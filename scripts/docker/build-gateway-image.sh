#!/usr/bin/env bash
set -Eeuo pipefail

# 构建脚本只负责生成不可变 Gateway 镜像；Registry 登录由调用环境预先完成。
readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
readonly GATEWAY_POM="${REPOSITORY_ROOT}/synapse-gateway-platform/pom.xml"
readonly DOCKERFILE="${REPOSITORY_ROOT}/synapse-gateway-platform/Dockerfile"

usage() {
    echo "用法: $0 <image-repository> <image-tag> [--push]" >&2
}

# 在执行外部命令前给出明确缺失项，避免构建到一半才失败。
require_command() {
    local command_name="$1"
    command -v "${command_name}" >/dev/null 2>&1 || {
        echo "错误: 缺少命令 ${command_name}" >&2
        exit 1
    }
}

# 过滤 original、sources、javadoc、tests，仅接受唯一 Spring Boot 可执行 JAR。
find_executable_jar() {
    local target_dir="${REPOSITORY_ROOT}/synapse-gateway-platform/target"
    local -a candidates=()
    while IFS= read -r -d '' candidate; do
        if jar tf "${candidate}" | grep -q '^BOOT-INF/' \
            && jar tf "${candidate}" | grep -q 'org/springframework/boot/loader/launch/JarLauncher.class'; then
            candidates+=("${candidate}")
        fi
    done < <(find "${target_dir}" -maxdepth 1 -type f -name '*.jar' \
        ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-tests.jar' -print0)

    if [[ ${#candidates[@]} -ne 1 ]]; then
        echo "错误: 预期找到 1 个 Spring Boot 可执行 JAR，实际为 ${#candidates[@]} 个" >&2
        exit 1
    fi
    printf '%s\n' "${candidates[0]}"
}

main() {
    if [[ $# -lt 2 || $# -gt 3 ]]; then
        usage
        exit 2
    fi

    local repository="$1"
    local tag="$2"
    local push=false
    if [[ ${3:-} == "--push" ]]; then
        push=true
    elif [[ -n ${3:-} ]]; then
        usage
        exit 2
    fi
    if [[ -z ${repository} || -z ${tag} ]]; then
        echo "错误: image repository 和 tag 均不能为空" >&2
        exit 2
    fi
    if [[ ${tag} == "latest" ]]; then
        echo "错误: 禁止使用 latest；生产发布必须使用明确且不可变的 tag" >&2
        exit 2
    fi

    require_command java
    require_command mvn
    require_command docker
    require_command jar
    java -version 2>&1 | grep -Eq 'version "(21|2[1-9]|[3-9][0-9])' || {
        echo "错误: 需要 Java 21 或更高版本执行构建" >&2
        exit 1
    }
    docker info >/dev/null

    # 使用 Gateway 子 POM，避免其他平台模块的已知历史依赖阻断定向安全构建。
    mvn -f "${GATEWAY_POM}" clean package

    local jar_file jar_relative image revision created source platform
    jar_file="$(find_executable_jar)"
    jar_relative="${jar_file#"${REPOSITORY_ROOT}/"}"
    image="${repository}:${tag}"
    revision="$(git -C "${REPOSITORY_ROOT}" rev-parse HEAD 2>/dev/null || printf 'unknown')"
    created="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
    source="${OCI_SOURCE:-}"
    platform="${DOCKER_PLATFORM:-linux/amd64}"

    docker build \
        --platform "${platform}" \
        --build-arg "JAR_FILE=${jar_relative}" \
        --build-arg "OCI_SOURCE=${source}" \
        --build-arg "OCI_REVISION=${revision}" \
        --build-arg "OCI_VERSION=${tag}" \
        --build-arg "OCI_CREATED=${created}" \
        --file "${DOCKERFILE}" \
        --tag "${image}" \
        "${REPOSITORY_ROOT}"

    if [[ ${push} == true ]]; then
        docker push "${image}"
    fi
    echo "Gateway 镜像构建完成: ${image} (${platform})"
}

main "$@"
