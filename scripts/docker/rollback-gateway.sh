#!/usr/bin/env bash
set -Eeuo pipefail

# 回滚脚本执行单次、显式版本切换，不递归调用部署脚本，避免失败时形成无限回滚。
readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
readonly DEPLOY_DIR="${REPOSITORY_ROOT}/deploy/docker/gateway"
readonly COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.yml"
readonly ENV_FILE="${DEPLOY_DIR}/.env"
readonly RELEASE_DIR="${DEPLOY_DIR}/.release"
readonly CONTAINER_NAME="synapse-gateway"
readonly HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-180}"

read_env_value() {
    local key="$1"
    awk -F= -v key="${key}" '$1 == key {sub(/^[^=]*=/, ""); print; exit}' "${ENV_FILE}"
}

write_state_atomically() {
    local name="$1"
    local value="$2"
    local temporary
    mkdir -p "${RELEASE_DIR}"
    temporary="$(mktemp "${RELEASE_DIR}/.${name}.XXXXXX")"
    printf '%s\n' "${value}" >"${temporary}"
    mv -f "${temporary}" "${RELEASE_DIR}/${name}"
}

wait_until_healthy() {
    local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
    local status="unknown"
    while (( SECONDS < deadline )); do
        status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
            "${CONTAINER_NAME}" 2>/dev/null || printf 'missing')"
        [[ ${status} == "healthy" ]] && return 0
        [[ ${status} == "exited" || ${status} == "dead" || ${status} == "missing" ]] && break
        sleep 3
    done
    echo "错误: 回滚后的 Gateway 未进入 healthy，当前状态: ${status}" >&2
    return 1
}

main() {
    if [[ $# -gt 1 ]]; then
        echo "用法: $0 [target-tag-or-full-image]" >&2
        exit 2
    fi
    command -v docker >/dev/null 2>&1 || {
        echo "错误: 缺少 docker" >&2
        exit 1
    }
    [[ -f ${ENV_FILE} ]] || {
        echo "错误: 未找到 ${ENV_FILE}" >&2
        exit 1
    }

    local requested="${1:-}"
    if [[ -z ${requested} ]]; then
        [[ -s ${RELEASE_DIR}/previous ]] || {
            echo "错误: 没有记录上一版本，请显式传入 tag 或完整镜像" >&2
            exit 1
        }
        requested="$(head -n 1 "${RELEASE_DIR}/previous")"
    fi
    local repository target_image target_tag current_image=""
    repository="$(read_env_value GATEWAY_IMAGE_REPOSITORY)"
    if [[ ${requested} == *:* || ${requested} == *@* ]]; then
        target_image="${requested}"
        repository="${target_image%:*}"
        target_tag="${target_image##*:}"
    else
        [[ -n ${repository} ]] || {
            echo "错误: .env 缺少 GATEWAY_IMAGE_REPOSITORY" >&2
            exit 1
        }
        target_tag="${requested}"
        target_image="${repository}:${target_tag}"
    fi
    [[ ${target_tag} != "latest" ]] || {
        echo "错误: 禁止回滚到 latest" >&2
        exit 2
    }
    if docker inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
        current_image="$(docker inspect --format '{{.Config.Image}}' "${CONTAINER_NAME}")"
    fi

    GATEWAY_IMAGE_REPOSITORY="${repository}" GATEWAY_IMAGE_TAG="${target_tag}" \
        docker compose --env-file "${ENV_FILE}" --file "${COMPOSE_FILE}" pull gateway
    GATEWAY_IMAGE_REPOSITORY="${repository}" GATEWAY_IMAGE_TAG="${target_tag}" \
        docker compose --env-file "${ENV_FILE}" --file "${COMPOSE_FILE}" \
        up --detach --no-deps gateway
    wait_until_healthy || exit 1

    [[ -n ${current_image} && ${current_image} != "${target_image}" ]] \
        && write_state_atomically previous "${current_image}"
    write_state_atomically current "${target_image}"
    echo "Gateway 回滚成功: ${target_image}，健康状态: healthy"
}

main "$@"
