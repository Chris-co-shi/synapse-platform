#!/usr/bin/env bash
set -Eeuo pipefail

# 回滚脚本只执行一次明确版本切换，不递归调用部署脚本或再次自动回滚。
readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
readonly DEFAULT_DEPLOY_DIR="${REPOSITORY_ROOT}/deploy/docker/gateway"

TARGET_REPOSITORY=""
TARGET_TAG=""
TARGET_IMAGE=""
ENV_FILE="${DEFAULT_DEPLOY_DIR}/.env"
COMPOSE_FILE="${DEFAULT_DEPLOY_DIR}/docker-compose.yml"
RELEASE_DIR="${DEFAULT_DEPLOY_DIR}/.release"
HEALTH_TIMEOUT_SECONDS="180"
SKIP_PULL=false
PRESERVE_PREVIOUS=false
EFFECTIVE_PULL_POLICY=always

log_info() {
    printf '[INFO] %s\n' "$*"
}

log_warn() {
    printf '[WARN] %s\n' "$*" >&2
}

die() {
    printf '[ERROR] %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<'USAGE'
用法:
  rollback-gateway.sh [--image <full-image> | --repository <repository> --tag <tag>] [选项]
  rollback-gateway.sh [tag-or-full-image] [选项]

未指定目标时读取 Compose 目录下的 .release/previous。

可选参数:
  --env-file <file>       环境文件，默认 deploy/docker/gateway/.env
  --compose-file <file>   Compose 文件
  --timeout <seconds>     等待 healthy 的超时秒数，默认 180
  --skip-pull             仅离线/本地演练使用
  -h, --help              显示帮助
USAGE
}

validate_repository() {
    [[ -n $1 && $1 != *://* && $1 != *@* && $1 != *[[:space:]]* ]] \
        || die "image repository 格式非法"
    [[ $1 =~ ^[a-z0-9]+([._-][a-z0-9]+)*([:/][a-z0-9]+([._-][a-z0-9]+)*)*$ ]] \
        || die "image repository 只能使用标准小写 OCI repository 格式"
}

validate_tag() {
    [[ $1 =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] || die "image tag 格式非法"
    [[ $(printf '%s' "$1" | tr '[:upper:]' '[:lower:]') != latest ]] || die "禁止回滚到 latest"
}

read_env_value() {
    local key="$1"
    awk -v key="${key}" '
        $0 ~ "^[[:space:]]*(export[[:space:]]+)?" key "[[:space:]]*=" {
            line=$0
            sub("^[[:space:]]*(export[[:space:]]+)?" key "[[:space:]]*=[[:space:]]*", "", line)
            sub("[[:space:]]*$", "", line)
            if ((substr(line,1,1) == "\"" && substr(line,length(line),1) == "\"") ||
                (substr(line,1,1) == "\047" && substr(line,length(line),1) == "\047")) {
                line=substr(line,2,length(line)-2)
            }
            print line
            exit
        }
    ' "${ENV_FILE}"
}

write_release_state() {
    local name="$1"
    local value="$2"
    local temporary
    mkdir -p "${RELEASE_DIR}"
    temporary="$(mktemp "${RELEASE_DIR}/.${name}.XXXXXX")"
    printf '%s\n' "${value}" >"${temporary}"
    mv -f "${temporary}" "${RELEASE_DIR}/${name}"
}

compose() {
    GATEWAY_IMAGE_REPOSITORY="${TARGET_REPOSITORY}" \
    GATEWAY_IMAGE_TAG="${TARGET_TAG}" \
    GATEWAY_PULL_POLICY="${EFFECTIVE_PULL_POLICY}" \
        docker compose --env-file "${ENV_FILE}" --file "${COMPOSE_FILE}" "$@"
}

gateway_container_id() {
    compose ps --quiet gateway 2>/dev/null || true
}

wait_for_health() {
    local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
    local status="missing"
    local container_id=""
    while (( SECONDS < deadline )); do
        container_id="$(gateway_container_id)"
        if [[ -n ${container_id} ]]; then
            status="$(docker inspect --format \
                '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
                "${container_id}" 2>/dev/null || printf 'missing')"
        else
            status="missing"
        fi
        case "${status}" in
            healthy)
                return 0
                ;;
            unhealthy|exited|dead)
                log_warn "回滚健康等待提前失败，状态: ${status}"
                return 1
                ;;
        esac
        sleep 3
    done
    log_warn "回滚版本未在 ${HEALTH_TIMEOUT_SECONDS}s 内进入 healthy，当前状态: ${status}"
    return 1
}

parse_full_image() {
    local image="$1"
    [[ ${image} != *@* ]] || die "当前回滚脚本要求 repository:tag，不接受 digest 引用"
    local last_component="${image##*/}"
    [[ ${last_component} == *:* ]] || die "完整镜像必须包含明确 tag"
    TARGET_REPOSITORY="${image%:*}"
    TARGET_TAG="${image##*:}"
}

main() {
    local positional=""
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --image)
                [[ $# -ge 2 ]] || die "--image 缺少值"
                TARGET_IMAGE="$2"
                shift 2
                ;;
            --repository)
                [[ $# -ge 2 ]] || die "--repository 缺少值"
                TARGET_REPOSITORY="$2"
                shift 2
                ;;
            --tag)
                [[ $# -ge 2 ]] || die "--tag 缺少值"
                TARGET_TAG="$2"
                shift 2
                ;;
            --env-file)
                [[ $# -ge 2 ]] || die "--env-file 缺少值"
                ENV_FILE="$2"
                shift 2
                ;;
            --compose-file)
                [[ $# -ge 2 ]] || die "--compose-file 缺少值"
                COMPOSE_FILE="$2"
                shift 2
                ;;
            --timeout)
                [[ $# -ge 2 ]] || die "--timeout 缺少值"
                HEALTH_TIMEOUT_SECONDS="$2"
                shift 2
                ;;
            --skip-pull)
                SKIP_PULL=true
                EFFECTIVE_PULL_POLICY=never
                shift
                ;;
            --preserve-previous)
                PRESERVE_PREVIOUS=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            --*)
                die "未知参数: $1"
                ;;
            *)
                [[ -z ${positional} ]] || die "只能提供一个位置目标参数"
                positional="$1"
                shift
                ;;
        esac
    done

    [[ ${HEALTH_TIMEOUT_SECONDS} =~ ^[1-9][0-9]*$ ]] || die "--timeout 必须为正整数"
    [[ -f ${ENV_FILE} ]] || die "未找到环境文件: ${ENV_FILE}"
    [[ -f ${COMPOSE_FILE} ]] || die "未找到 Compose 文件: ${COMPOSE_FILE}"
    ENV_FILE="$(cd -- "$(dirname -- "${ENV_FILE}")" && pwd)/$(basename -- "${ENV_FILE}")"
    COMPOSE_FILE="$(cd -- "$(dirname -- "${COMPOSE_FILE}")" && pwd)/$(basename -- "${COMPOSE_FILE}")"
    RELEASE_DIR="$(cd -- "$(dirname -- "${COMPOSE_FILE}")" && pwd)/.release"

    if [[ -n ${TARGET_IMAGE} ]]; then
        [[ -z ${TARGET_REPOSITORY} && -z ${TARGET_TAG} && -z ${positional} ]] \
            || die "--image 不能与 repository/tag/位置参数同时使用"
        parse_full_image "${TARGET_IMAGE}"
    elif [[ -n ${positional} ]]; then
        [[ -z ${TARGET_REPOSITORY} && -z ${TARGET_TAG} ]] \
            || die "位置参数不能与 --repository/--tag 同时使用"
        if [[ ${positional##*/} == *:* ]]; then
            parse_full_image "${positional}"
        else
            TARGET_REPOSITORY="$(read_env_value GATEWAY_IMAGE_REPOSITORY)"
            TARGET_TAG="${positional}"
        fi
    elif [[ -n ${TARGET_REPOSITORY} || -n ${TARGET_TAG} ]]; then
        [[ -n ${TARGET_REPOSITORY} && -n ${TARGET_TAG} ]] \
            || die "--repository 和 --tag 必须同时提供"
    else
        [[ -s ${RELEASE_DIR}/previous ]] || die "没有记录上一版本，请显式提供回滚目标"
        parse_full_image "$(head -n 1 "${RELEASE_DIR}/previous")"
    fi

    validate_repository "${TARGET_REPOSITORY}"
    validate_tag "${TARGET_TAG}"
    TARGET_IMAGE="${TARGET_REPOSITORY}:${TARGET_TAG}"
    command -v docker >/dev/null 2>&1 || die "缺少命令: docker"
    docker info >/dev/null 2>&1 || die "Docker daemon 不可用"
    docker compose version >/dev/null 2>&1 || die "需要 Docker Compose v2"
    compose config --quiet >/dev/null || die "Compose 配置校验失败"

    local current_image=""
    local container_id
    container_id="$(gateway_container_id)"
    if [[ -n ${container_id} ]]; then
        current_image="$(docker inspect --format '{{.Config.Image}}' "${container_id}" 2>/dev/null || true)"
    fi

    if [[ ${SKIP_PULL} == true ]]; then
        log_warn "已启用 --skip-pull，仅适用于本地或离线演练"
        docker image inspect "${TARGET_IMAGE}" >/dev/null 2>&1 \
            || die "本地不存在回滚镜像: ${TARGET_IMAGE}"
    else
        compose pull gateway
    fi
    compose up --detach --no-deps gateway
    if ! wait_for_health; then
        compose ps gateway >&2 || true
        compose logs --no-color --tail 80 gateway >&2 || true
        die "回滚失败；不会递归回滚，已保留容器和镜像"
    fi

    if [[ ${PRESERVE_PREVIOUS} != true && -n ${current_image} && ${current_image} != "${TARGET_IMAGE}" ]]; then
        write_release_state previous "${current_image}"
    fi
    write_release_state current "${TARGET_IMAGE}"
    log_info "Gateway 回滚成功: ${TARGET_IMAGE}"
    log_info "健康状态: healthy"
}

main "$@"
