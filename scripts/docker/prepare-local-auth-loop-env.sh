#!/usr/bin/env bash
set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
readonly DEPLOY_DIR="${REPOSITORY_ROOT}/deploy/docker/local-auth-loop"
readonly EXAMPLE_ENV="${DEPLOY_DIR}/.env.example"
readonly ENV_FILE="${DEPLOY_DIR}/.env"

log_info() {
    printf '[INFO] %s\n' "$*"
}

die() {
    printf '[ERROR] %s\n' "$*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "缺少命令: $1"
}

random_secret() {
    if command -v openssl >/dev/null 2>&1; then
        openssl rand -base64 48 | tr -d '\n=' | cut -c 1-48
        return
    fi
    set +o pipefail
    LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 48
    local status=$?
    set -o pipefail
    [[ ${status} -eq 0 ]] || die "无法生成本地随机 secret"
}

random_base64_secret() {
    if command -v openssl >/dev/null 2>&1; then
        openssl rand -base64 48 | tr -d '\n'
        return
    fi
    random_secret
}

read_env_value() {
    local key="$1"
    awk -v key="${key}" '
        $0 ~ "^[[:space:]]*" key "[[:space:]]*=" {
            line=$0
            sub("^[[:space:]]*" key "[[:space:]]*=[[:space:]]*", "", line)
            sub("[[:space:]]*$", "", line)
            print line
            exit
        }
    ' "${ENV_FILE}"
}

set_env_value() {
    local key="$1"
    local value="$2"
    local temporary
    temporary="$(mktemp "${ENV_FILE}.XXXXXX")"
    awk -v key="${key}" -v value="${value}" '
        BEGIN { updated = 0 }
        $0 ~ "^" key "=" {
            print key "=" value
            updated = 1
            next
        }
        { print }
        END {
            if (updated == 0) {
                print key "=" value
            }
        }
    ' "${ENV_FILE}" >"${temporary}"
    mv -f "${temporary}" "${ENV_FILE}"
}

ensure_env_secret() {
    local key="$1"
    local generator="$2"
    local current
    current="$(read_env_value "${key}")"
    if [[ -z ${current} || ${current} == "__GENERATED_BY_PREPARE_SCRIPT__" ]]; then
        set_env_value "${key}" "$("${generator}")"
    fi
}

replace_env_default() {
    local key="$1"
    local old_value="$2"
    local new_value="$3"
    local current
    current="$(read_env_value "${key}")"
    if [[ ${current} == "${old_value}" ]]; then
        set_env_value "${key}" "${new_value}"
    fi
}

main() {
    require_command docker
    docker compose version >/dev/null 2>&1 || die "需要 Docker Compose v2"
    require_command curl
    require_command jq

    [[ -f ${EXAMPLE_ENV} ]] || die "未找到示例环境文件: ${EXAMPLE_ENV}"
    if [[ ! -e ${ENV_FILE} ]]; then
        cp "${EXAMPLE_ENV}" "${ENV_FILE}"
        log_info "已生成本地联调环境文件: ${ENV_FILE}"
    else
        log_info "已存在本地环境文件，保留已有真实值并补齐缺失项: ${ENV_FILE}"
    fi
    chmod 600 "${ENV_FILE}"

    ensure_env_secret POSTGRES_PASSWORD random_secret
    ensure_env_secret GATEWAY_PROOF_SECRET random_secret
    ensure_env_secret IAM_BOOTSTRAP_PASSWORD random_secret
    ensure_env_secret NACOS_AUTH_TOKEN random_base64_secret
    ensure_env_secret NACOS_AUTH_IDENTITY_KEY random_secret
    ensure_env_secret NACOS_AUTH_IDENTITY_VALUE random_secret
    replace_env_default GATEWAY_HOST_PORT 8080 18080
    replace_env_default IAM_HOST_PORT 8100 18100

    log_info "文件权限已设置为 600；其中包含本地开发密码和 GatewayProof Secret，不会提交到 Git。"
}

main "$@"
