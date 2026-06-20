#!/usr/bin/env bash
set -Eeuo pipefail

# 部署脚本只更新 Gateway 服务，健康失败时回滚到部署前版本，不影响同机其他容器。
readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
readonly DEPLOY_DIR="${REPOSITORY_ROOT}/deploy/docker/gateway"
readonly COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.yml"
readonly ENV_FILE="${DEPLOY_DIR}/.env"
readonly RELEASE_DIR="${DEPLOY_DIR}/.release"
readonly CONTAINER_NAME="synapse-gateway"
readonly HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-180}"

usage() {
    echo "用法: $0 <image-repository> <image-tag>" >&2
}

require_command() {
    local command_name="$1"
    command -v "${command_name}" >/dev/null 2>&1 || {
        echo "错误: 缺少命令 ${command_name}" >&2
        exit 1
    }
}

# 只判断变量是否存在非空值，不输出值，避免日志泄漏密码或 GatewayProof secret。
require_env_value() {
    local key="$1"
    grep -Eq "^[[:space:]]*${key}=[^[:space:]].*$" "${ENV_FILE}" || {
        echo "错误: ${ENV_FILE} 缺少必要变量 ${key}" >&2
        exit 1
    }
}

read_env_value() {
    local key="$1"
    awk -F= -v key="${key}" '$1 == key {sub(/^[^=]*=/, ""); print; exit}' "${ENV_FILE}"
}

# 同目录临时文件加 rename，避免部署进程中断留下半写状态。
write_state_atomically() {
    local name="$1"
    local value="$2"
    local temporary
    mkdir -p "${RELEASE_DIR}"
    temporary="$(mktemp "${RELEASE_DIR}/.${name}.XXXXXX")"
    printf '%s\n' "${value}" >"${temporary}"
    mv -f "${temporary}" "${RELEASE_DIR}/${name}"
}

current_running_image() {
    if docker inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
        docker inspect --format '{{.Config.Image}}' "${CONTAINER_NAME}"
    elif [[ -s "${RELEASE_DIR}/current" ]]; then
        head -n 1 "${RELEASE_DIR}/current"
    fi
}

# 仅读取健康状态，不输出 docker inspect 的环境变量快照。
wait_until_healthy() {
    local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
    local status="unknown"
    while (( SECONDS < deadline )); do
        status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
            "${CONTAINER_NAME}" 2>/dev/null || printf 'missing')"
        if [[ ${status} == "healthy" ]]; then
            return 0
        fi
        if [[ ${status} == "exited" || ${status} == "dead" || ${status} == "missing" ]]; then
            break
        fi
        sleep 3
    done
    echo "错误: Gateway 未在 ${HEALTH_TIMEOUT_SECONDS}s 内进入 healthy，当前状态: ${status}" >&2
    return 1
}

compose() {
    GATEWAY_IMAGE_REPOSITORY="${TARGET_REPOSITORY}" \
    GATEWAY_IMAGE_TAG="${TARGET_TAG}" \
        docker compose --env-file "${ENV_FILE}" --file "${COMPOSE_FILE}" "$@"
}

main() {
    if [[ $# -ne 2 ]]; then
        usage
        exit 2
    fi
    TARGET_REPOSITORY="$1"
    TARGET_TAG="$2"
    export TARGET_REPOSITORY TARGET_TAG
    if [[ -z ${TARGET_REPOSITORY} || -z ${TARGET_TAG} || ${TARGET_TAG} == "latest" ]]; then
        echo "错误: repository/tag 不能为空，且禁止部署 latest" >&2
        exit 2
    fi

    require_command docker
    [[ -f ${ENV_FILE} ]] || {
        echo "错误: 未找到 ${ENV_FILE}；请先从 .env.example 创建" >&2
        exit 1
    }
    for key in SPRING_PROFILES_ACTIVE SERVER_PORT NACOS_SERVER_ADDR IAM_ISSUER_URI \
        IAM_JWK_SET_URI SYNAPSE_GATEWAY_AUDIENCE GATEWAY_PROOF_ENABLED GATEWAY_ID; do
        require_env_value "${key}"
    done
    if [[ $(read_env_value GATEWAY_PROOF_ENABLED) == "true" ]]; then
        require_env_value GATEWAY_PROOF_SECRET
    fi
    local container_port server_port
    container_port="$(read_env_value GATEWAY_CONTAINER_PORT)"
    server_port="$(read_env_value SERVER_PORT)"
    if [[ -n ${container_port} && ${container_port} != "${server_port}" ]]; then
        echo "错误: GATEWAY_CONTAINER_PORT 必须与 SERVER_PORT 一致" >&2
        exit 1
    fi

    local target_image previous_image=""
    target_image="${TARGET_REPOSITORY}:${TARGET_TAG}"
    previous_image="$(current_running_image || true)"
    compose pull gateway
    compose up --detach --no-deps gateway

    if ! wait_until_healthy; then
        compose ps gateway >&2 || true
        if [[ -n ${previous_image} && ${previous_image} != "${target_image}" ]]; then
            echo "部署失败，开始回滚到上一已知版本。" >&2
            "${SCRIPT_DIR}/rollback-gateway.sh" "${previous_image}" || true
        else
            echo "部署失败，且不存在可用的上一版本。" >&2
        fi
        exit 1
    fi

    if [[ -n ${previous_image} && ${previous_image} != "${target_image}" ]]; then
        write_state_atomically previous "${previous_image}"
    fi
    write_state_atomically current "${target_image}"
    echo "Gateway 部署成功"
    echo "容器: ${CONTAINER_NAME}"
    echo "镜像: ${target_image}"
    echo "健康状态: healthy"
    echo "部署时间: $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
}

main "$@"
