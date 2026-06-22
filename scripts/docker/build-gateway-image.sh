#!/usr/bin/env bash
set -Eeuo pipefail

# 构建脚本只负责测试、打包和生成不可变镜像；Registry 登录由调用环境预先完成。
readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
readonly GATEWAY_TARGET="${REPOSITORY_ROOT}/synapse-gateway-platform/target"
readonly DOCKERFILE="${REPOSITORY_ROOT}/synapse-gateway-platform/Dockerfile"

BUILD_CONTEXT=""

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
  build-gateway-image.sh --repository <repository> --tag <tag> [选项]

必填参数:
  --repository <repository>  OCI 镜像仓库，例如 registry.example.com/synapse/synapse-gateway
  --tag <tag>                明确且不可变的镜像 tag，禁止 latest

可选参数:
  --platform <platforms>     目标平台，默认 DOCKER_PLATFORM 或 linux/amd64
  --push                     构建后推送；脚本不会执行 docker login
  --skip-tests               明确跳过测试，默认会执行测试
  -h, --help                 显示帮助

环境变量:
  MVN_BIN                    Maven 命令或脚本路径，默认 mvn
  DOCKER_PLATFORM            默认目标平台
  BUILD_SOURCE               OCI source label；默认使用脱敏后的 Git origin URL
USAGE
}

# 在执行外部命令前给出明确缺失项。
require_command() {
    command -v "$1" >/dev/null 2>&1 || die "缺少命令: $1"
}

# Maven 脚本没有执行位时使用 /bin/sh，兼容任务指定的本机 Maven。
run_maven() {
    if command -v "${MVN_BIN}" >/dev/null 2>&1; then
        "${MVN_BIN}" "$@"
    elif [[ -f ${MVN_BIN} ]]; then
        /bin/sh "${MVN_BIN}" "$@"
    else
        die "无法执行 Maven: ${MVN_BIN}"
    fi
}

validate_repository() {
    local repository="$1"
    [[ -n ${repository} ]] || die "image repository 不能为空"
    [[ ${repository} != *://* && ${repository} != *@* && ${repository} != *[[:space:]]* ]] \
        || die "image repository 格式非法"
    [[ ${repository} =~ ^[a-z0-9]+([._-][a-z0-9]+)*([:/][a-z0-9]+([._-][a-z0-9]+)*)*$ ]] \
        || die "image repository 只能使用标准小写 OCI repository 格式"
}

validate_tag() {
    local tag="$1"
    [[ ${tag} =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] || die "image tag 格式非法"
    [[ $(printf '%s' "${tag}" | tr '[:upper:]' '[:lower:]') != latest ]] \
        || die "禁止使用 latest；生产发布必须使用明确且不可变的 tag"
}

# 过滤 original、sources、javadoc、tests，仅接受唯一 Spring Boot 可执行 JAR。
find_executable_jar() {
    local candidate
    local jar_entries
    local -a candidates=()
    while IFS= read -r -d '' candidate; do
        jar_entries="$(jar tf "${candidate}")"
        if [[ ${jar_entries} == *'BOOT-INF/'* \
            && ${jar_entries} == *'org/springframework/boot/loader/launch/JarLauncher.class'* ]]; then
            candidates+=("${candidate}")
        fi
    done < <(find "${GATEWAY_TARGET}" -maxdepth 1 -type f -name '*.jar' \
        ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' \
        ! -name '*-tests.jar' -print0)

    [[ ${#candidates[@]} -eq 1 ]] \
        || die "预期找到 1 个 Spring Boot 可执行 JAR，实际为 ${#candidates[@]} 个"
    printf '%s\n' "${candidates[0]}"
}

# 防止带用户信息的 Git URL 被写入 OCI label。
sanitize_source_url() {
    local source="$1"
    if [[ ${source} == *://* && ${source} == *@* ]]; then
        local scheme rest
        scheme="${source%%://*}"
        rest="${source#*://}"
        source="${scheme}://${rest#*@}"
    fi
    printf '%s' "${source}"
}

sanitized_git_source() {
    sanitize_source_url "$(git -C "${REPOSITORY_ROOT}" config --get remote.origin.url 2>/dev/null || true)"
}

cleanup() {
    if [[ -n ${BUILD_CONTEXT} && -d ${BUILD_CONTEXT} ]]; then
        rm -rf -- "${BUILD_CONTEXT}"
    fi
}

main() {
    local repository=""
    local tag=""
    local platform="${DOCKER_PLATFORM:-linux/amd64}"
    local push=false
    local skip_tests=false

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --repository)
                [[ $# -ge 2 ]] || die "--repository 缺少值"
                repository="$2"
                shift 2
                ;;
            --tag)
                [[ $# -ge 2 ]] || die "--tag 缺少值"
                tag="$2"
                shift 2
                ;;
            --platform)
                [[ $# -ge 2 ]] || die "--platform 缺少值"
                platform="$2"
                shift 2
                ;;
            --push)
                push=true
                shift
                ;;
            --skip-tests)
                skip_tests=true
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

    validate_repository "${repository}"
    validate_tag "${tag}"
    [[ -n ${platform} && ${platform} != *[[:space:]]* ]] || die "目标平台格式非法"
    if [[ ${platform} == *,* && ${push} != true ]]; then
        die "多平台构建不能使用 --load；请显式传入 --push"
    fi

    readonly MVN_BIN="${MVN_BIN:-mvn}"
    require_command java
    require_command jar
    require_command git
    require_command docker
    [[ -f ${REPOSITORY_ROOT}/pom.xml && -d ${REPOSITORY_ROOT}/.git ]] \
        || die "脚本未解析到正确的 synapse-platform 仓库根目录"
    [[ $(git -C "${REPOSITORY_ROOT}" rev-parse --show-toplevel) == "${REPOSITORY_ROOT}" ]] \
        || die "当前目录不是预期 Git 仓库"
    java -version 2>&1 | grep -Eq 'version "21([.]|\")' || die "构建必须使用 Java 21"
    docker info >/dev/null 2>&1 || die "Docker daemon 不可用"
    docker buildx version >/dev/null 2>&1 || die "Docker buildx 不可用"

    if [[ -n $(git -C "${REPOSITORY_ROOT}" status --porcelain) ]]; then
        log_warn "当前工作区为 dirty；镜像仍记录当前 commit revision，不会自动提交"
    fi
    if [[ ${skip_tests} == true ]]; then
        log_warn "调用者明确要求 --skip-tests，本次 Maven 构建不会执行测试"
        run_maven -pl synapse-gateway-platform -am -DskipTests clean package
    else
        run_maven -pl synapse-gateway-platform -am clean package
    fi

    local jar_file image revision created source digest=""
    jar_file="$(find_executable_jar)"
    image="${repository}:${tag}"
    revision="$(git -C "${REPOSITORY_ROOT}" rev-parse HEAD)"
    created="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
    source="$(sanitize_source_url "${BUILD_SOURCE:-$(sanitized_git_source)}")"

    BUILD_CONTEXT="$(mktemp -d "${TMPDIR:-/tmp}/synapse-gateway-context.XXXXXX")"
    trap cleanup EXIT INT TERM
    cp "${jar_file}" "${BUILD_CONTEXT}/application.jar"

    local -a build_args=(
        --platform "${platform}"
        --build-arg JAR_FILE=application.jar
        --build-arg "BUILD_VERSION=${tag}"
        --build-arg "BUILD_REVISION=${revision}"
        --build-arg "BUILD_CREATED=${created}"
        --build-arg "BUILD_SOURCE=${source}"
        --file "${DOCKERFILE}"
        --tag "${image}"
    )
    if [[ ${push} == true ]]; then
        build_args+=(--push)
    else
        build_args+=(--load)
    fi
    docker buildx build "${build_args[@]}" "${BUILD_CONTEXT}"

    if [[ ${push} == true ]]; then
        digest="$(docker buildx imagetools inspect "${image}" 2>/dev/null \
            | awk '/^Digest:/ {print $2; exit}' || true)"
    else
        digest="$(docker image inspect --format '{{index .RepoDigests 0}}' "${image}" 2>/dev/null || true)"
    fi

    log_info "Gateway 镜像构建完成: ${image}"
    log_info "目标平台: ${platform}"
    log_info "Git revision: ${revision}"
    log_info "已推送: ${push}"
    [[ -n ${digest} ]] && log_info "镜像 digest: ${digest}"
    log_info "下一步: ./scripts/docker/deploy-gateway.sh --repository ${repository} --tag ${tag} --env-file /path/to/.env"
}

main "$@"
