#!/usr/bin/env bash
set -Eeuo pipefail

# 部署脚本只更新 Compose 中的 Gateway 服务；失败时最多执行一次回滚。
readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
readonly DEFAULT_DEPLOY_DIR="${REPOSITORY_ROOT}/deploy/docker/gateway"

TARGET_REPOSITORY=""
TARGET_TAG=""
ENV_FILE="${DEFAULT_DEPLOY_DIR}/.env"
COMPOSE_FILE="${DEFAULT_DEPLOY_DIR}/docker-compose.yml"
RELEASE_DIR="${DEFAULT_DEPLOY_DIR}/.release"
HEALTH_TIMEOUT_SECONDS="180"
AUTO_ROLLBACK=true
SKIP_PULL=false
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
  deploy-gateway.sh --repository <repository> --tag <tag> --env-file <file> [选项]

必填参数:
  --repository <repository>  目标镜像 repository
  --tag <tag>                目标镜像 tag，禁止 latest
  --env-file <file>          服务器环境文件

可选参数:
  --compose-file <file>      Compose 文件，默认 deploy/docker/gateway/docker-compose.yml
  --timeout <seconds>        等待 healthy 的超时秒数，默认 180
  --no-auto-rollback         新版本失败时不自动回滚
  --skip-pull                仅离线/本地演练使用，不拉取 Registry 镜像
  -h, --help                 显示帮助
USAGE
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "缺少命令: $1"
}

validate_repository() {
    [[ -n $1 && $1 != *://* && $1 != *@* && $1 != *[[:space:]]* ]] \
        || die "image repository 格式非法"
    [[ $1 =~ ^[a-z0-9]+([._-][a-z0-9]+)*([:/][a-z0-9]+([._-][a-z0-9]+)*)*$ ]] \
        || die "image repository 只能使用标准小写 OCI repository 格式"
}

validate_tag() {
    [[ $1 =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] || die "image tag 格式非法"
    [[ $(printf '%s' "$1" | tr '[:upper:]' '[:lower:]') != latest ]] || die "禁止部署 latest"
}

# 读取 dotenv 单值但不执行文件，避免注入命令或打印 secret。
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

require_env_value() {
    [[ -n $(read_env_value "$1") ]] || die "环境文件缺少必要变量: $1"
}

# 状态文件只保存完整镜像引用，并通过同目录 rename 原子替换。
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

current_running_image() {
    local container_id
    container_id="$(gateway_container_id)"
    if [[ -n ${container_id} ]]; then
        docker inspect --format '{{.Config.Image}}' "${container_id}" 2>/dev/null || true
    elif [[ -s ${RELEASE_DIR}/current ]]; then
        head -n 1 "${RELEASE_DIR}/current"
    fi
}

# 轮询容器健康状态；unhealthy、exited、dead 和 missing 立即失败。
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
                log_warn "Gateway 健康等待提前失败，状态: ${status}"
                return 1
                ;;
        esac
        sleep 3
    done
    log_warn "Gateway 未在 ${HEALTH_TIMEOUT_SECONDS}s 内进入 healthy，当前状态: ${status}"
    return 1
}

print_failure_diagnostics() {
    log_warn "Gateway 部署诊断（不包含 inspect 环境变量）"
    compose ps gateway >&2 || true
    compose logs --no-color --tail 80 gateway >&2 || true
}

main() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
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
            --no-auto-rollback)
                AUTO_ROLLBACK=false
                shift
                ;;
            --skip-pull)
                SKIP_PULL=true
                EFFECTIVE_PULL_POLICY=never
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                die "未知参数: $1"
                ;;
        esac
    done

    validate_repository "${TARGET_REPOSITORY}"
    validate_tag "${TARGET_TAG}"
    [[ ${HEALTH_TIMEOUT_SECONDS} =~ ^[1-9][0-9]*$ ]] || die "--timeout 必须为正整数"
    [[ -f ${ENV_FILE} ]] || die "未找到环境文件: ${ENV_FILE}"
    [[ -f ${COMPOSE_FILE} ]] || die "未找到 Compose 文件: ${COMPOSE_FILE}"
    ENV_FILE="$(cd -- "$(dirname -- "${ENV_FILE}")" && pwd)/$(basename -- "${ENV_FILE}")"
    COMPOSE_FILE="$(cd -- "$(dirname -- "${COMPOSE_FILE}")" && pwd)/$(basename -- "${COMPOSE_FILE}")"
    RELEASE_DIR="$(cd -- "$(dirname -- "${COMPOSE_FILE}")" && pwd)/.release"

    require_command docker
    docker info >/dev/null 2>&1 || die "Docker daemon 不可用"
    docker compose version >/dev/null 2>&1 || die "需要 Docker Compose v2"
    for key in SPRING_PROFILES_ACTIVE SERVER_PORT NACOS_SERVER_ADDR IAM_ISSUER_URI \
        IAM_JWK_SET_URI SYNAPSE_GATEWAY_AUDIENCE GATEWAY_PROOF_ENABLED GATEWAY_ID; do
        require_env_value "${key}"
    done
    if [[ $(printf '%s' "$(read_env_value GATEWAY_PROOF_ENABLED)" | tr '[:upper:]' '[:lower:]') == true ]]; then
        require_env_value GATEWAY_PROOF_SECRET
    fi
    compose config --quiet >/dev/null || die "Compose 配置校验失败"

    local target_image previous_image=""
    target_image="${TARGET_REPOSITORY}:${TARGET_TAG}"
    previous_image="$(current_running_image)"
    if [[ -n ${previous_image} && ${previous_image} != "${target_image}" ]]; then
        write_release_state previous "${previous_image}"
    fi

    if [[ ${SKIP_PULL} == true ]]; then
        log_warn "已启用 --skip-pull，仅适用于本地或离线演练"
        docker image inspect "${target_image}" >/dev/null 2>&1 \
            || die "本地不存在目标镜像: ${target_image}"
    else
        compose pull gateway
    fi
    compose up --detach --no-deps gateway

    if ! wait_for_health; then
        print_failure_diagnostics
        if [[ ${AUTO_ROLLBACK} == true && -n ${previous_image} && ${previous_image} != "${target_image}" ]]; then
            log_warn "新版本部署失败，开始单次自动回滚到上一版本"
            local -a rollback_args=(
                --image "${previous_image}"
                --env-file "${ENV_FILE}"
                --compose-file "${COMPOSE_FILE}"
                --timeout "${HEALTH_TIMEOUT_SECONDS}"
                --preserve-previous
            )
            [[ ${SKIP_PULL} == true ]] && rollback_args+=(--skip-pull)
            if "${SCRIPT_DIR}/rollback-gateway.sh" "${rollback_args[@]}"; then
                log_warn "上一版本已恢复 healthy；新版本发布仍判定失败"
            else
                log_warn "自动回滚失败，保留当前容器和镜像用于排查"
            fi
        elif [[ ${AUTO_ROLLBACK} != true ]]; then
            log_warn "调用者已禁用自动回滚"
        else
            log_warn "不存在可用的上一版本，无法自动回滚"
        fi
        exit 1
    fi

    write_release_state current "${target_image}"
    log_info "Gateway 部署成功"
    log_info "镜像: ${target_image}"
    log_info "健康状态: healthy"
    log_info "部署时间: $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
}

main "$@"
