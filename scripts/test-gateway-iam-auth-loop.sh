#!/usr/bin/env bash
set -Eeuo pipefail

export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:${PATH:-}"

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
readonly DEPLOY_DIR="${REPOSITORY_ROOT}/deploy/docker/local-auth-loop"
readonly ENV_FILE_DEFAULT="${DEPLOY_DIR}/.env"
readonly COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.yml"
readonly MVN_BIN="${MVN_BIN:-/Users/sxc/Documents/tool/apache-maven-3.9.0/bin/mvn}"
readonly PG_JDBC_JAR_DEFAULT="/Users/sxc/Documents/tool/m2/report/org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar"

MODE="compose"
ENV_FILE="${ENV_FILE_DEFAULT}"
TEST_DIR="/tmp/synapse-auth-test"
FAILED=0

ACCESS_TOKEN=""
REFRESH_TOKEN=""
SECOND_ACCESS_TOKEN=""
SECOND_REFRESH_TOKEN=""
CURRENT_DIGEST=""
REDIS_KEYS_CREATED=()
DIGEST_FILES=()

log_info() {
    printf '[INFO] %s\n' "$*"
}

pass() {
    printf '[Passed] %s\n' "$*"
}

fail() {
    printf '[Failed] %s\n' "$*" >&2
    FAILED=1
}

die() {
    printf '[ERROR] %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<'USAGE'
Usage: scripts/test-gateway-iam-auth-loop.sh [--mode compose|local] [--env-file PATH]

compose: 使用 deploy/docker/local-auth-loop/docker-compose.yml 启动完整本地闭环。
local:   连接本机已运行 PostgreSQL、Redis、Nacos，只启动本次 IAM/Gateway Java 进程。
USAGE
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --mode)
                MODE="${2:-}"
                shift 2
                ;;
            --env-file)
                ENV_FILE="${2:-}"
                shift 2
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
    [[ ${MODE} == "compose" || ${MODE} == "local" ]] || die "--mode 只能是 compose 或 local"
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "缺少命令: $1"
}

read_env_value() {
    local key="$1"
    [[ -f ${ENV_FILE} ]] || return 0
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

config_value() {
    local key="$1"
    local default_value="${2:-}"
    local current="${!key:-}"
    if [[ -n ${current} ]]; then
        printf '%s' "${current}"
        return
    fi
    current="$(read_env_value "${key}")"
    if [[ -n ${current} && ${current} != "__GENERATED_BY_PREPARE_SCRIPT__" ]]; then
        printf '%s' "${current}"
        return
    fi
    printf '%s' "${default_value}"
}

compose() {
    docker compose --env-file "${ENV_FILE}" --file "${COMPOSE_FILE}" "$@"
}

curl_bin() {
    if command -v curl >/dev/null 2>&1; then
        command -v curl
    elif [[ -x /usr/bin/curl ]]; then
        printf '/usr/bin/curl'
    else
        die "缺少命令: curl"
    fi
}

http_request() {
    local method="$1"
    local url="$2"
    local body_file="$3"
    local output_file="$4"
    shift 4
    local curl
    curl="$(curl_bin)"
    local -a args=(-sS -o "${output_file}" -w "%{http_code}" -X "${method}" "${url}" "$@")
    if [[ -n ${body_file} ]]; then
        args+=(-H "Content-Type: application/json" --data-binary "@${body_file}")
    fi
    "${curl}" "${args[@]}"
}

json_body() {
    local file="$1"
    shift
    jq -n "$@" >"${file}"
    chmod 600 "${file}"
}

sha256_hex() {
    printf '%s' "$1" | shasum -a 256 | awk '{print $1}'
}

write_secret_file() {
    local name="$1"
    local value="$2"
    local file="${TEST_DIR}/${name}"
    printf '%s' "${value}" >"${file}"
    chmod 600 "${file}"
    DIGEST_FILES+=("${file}")
    printf '%s' "${file}"
}

redis_cli() {
    if [[ ${MODE} == "compose" ]]; then
        compose exec -T redis redis-cli "$@"
        return
    fi
    local -a args=(-h "${REDIS_HOST}" -p "${REDIS_PORT}" -n "${REDIS_DATABASE}")
    if [[ -n ${REDIS_PASSWORD} ]]; then
        REDISCLI_AUTH="${REDIS_PASSWORD}" redis-cli "${args[@]}" "$@"
    else
        redis-cli "${args[@]}" "$@"
    fi
}

record_redis_key() {
    local key="$1"
    REDIS_KEYS_CREATED+=("${key}")
}

wait_http() {
    local name="$1"
    local url="$2"
    local deadline=$((SECONDS + 180))
    local curl
    curl="$(curl_bin)"
    until "${curl}" -fsS "${url}" >/dev/null 2>&1; do
        if (( SECONDS >= deadline )); then
            fail "${name} health timeout"
            return 1
        fi
        sleep 3
    done
    pass "${name} health"
}

wait_http_status() {
    local name="$1"
    local url="$2"
    local expected="$3"
    local deadline=$((SECONDS + 90))
    local output="${TEST_DIR}/wait-${name}.json"
    local status
    until [[ ${status:-} == "${expected}" ]]; do
        status="$(http_request GET "${url}" "" "${output}" || true)"
        if (( SECONDS >= deadline )); then
            fail "${name} status timeout: expected ${expected}, actual ${status:-none}"
            return 1
        fi
        sleep 2
    done
    pass "${name}"
}

assert_status() {
    local actual="$1"
    local expected="$2"
    local name="$3"
    if [[ ${actual} == "${expected}" ]]; then
        pass "${name}"
    else
        fail "${name}: expected ${expected}, actual ${actual}"
    fi
}

assert_non_200() {
    local actual="$1"
    local name="$2"
    if [[ ${actual} != "200" ]]; then
        pass "${name}"
    else
        fail "${name}: expected non-200, actual 200"
    fi
}

kv() {
    local text="$1"
    local key="$2"
    awk -F= -v key="${key}" '$1 == key {print substr($0, index($0, "=") + 1); exit}' <<<"${text}"
}

build_db_helper() {
    [[ ${MODE} == "local" ]] || return 0
    [[ -f ${PG_JDBC_JAR} ]] || die "未找到 PostgreSQL JDBC 驱动: ${PG_JDBC_JAR}"
    cat >"${TEST_DIR}/DbAssert.java" <<'JAVA'
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;

public class DbAssert {
    record Session(String id, String familyId, String status, int revision, String replacedById, String refreshDigest) {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("command required");
        }
        try (var connection = DriverManager.getConnection(url(), props())) {
            switch (args[0]) {
                case "probe" -> probe(connection);
                case "rotation" -> rotation(connection, read(args[1]), read(args[2]));
                case "reuse" -> reuse(connection, read(args[1]), read(args[2]));
                case "concurrent" -> concurrent(connection, read(args[1]));
                case "logout" -> logout(connection, read(args[1]));
                case "count" -> count(connection);
                default -> throw new IllegalArgumentException("unknown command: " + args[0]);
            }
        }
    }

    private static String url() {
        return "jdbc:postgresql://" + env("DB_HOST", "127.0.0.1") + ":" + env("DB_PORT", "5432")
                + "/" + env("DB_NAME", "synapse_iam");
    }

    private static Properties props() {
        Properties props = new Properties();
        props.setProperty("user", env("DB_USERNAME", "postgres"));
        String password = env("DB_PASSWORD", "");
        if (!password.isBlank()) {
            props.setProperty("password", password);
        }
        return props;
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String read(String file) throws Exception {
        return Files.readString(Path.of(file)).trim();
    }

    private static void probe(java.sql.Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("""
                     select version(), current_database(), current_schema(), current_user,
                            to_regclass('public.iam_refresh_session') is not null
                     """)) {
            rs.next();
            System.out.println("postgres_major17=" + rs.getString(1).contains("PostgreSQL 17"));
            System.out.println("database=" + rs.getString(2));
            System.out.println("schema=" + rs.getString(3));
            System.out.println("db_user=" + rs.getString(4));
            System.out.println("refresh_table_exists=" + rs.getBoolean(5));
        }
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("select coalesce(max(version), 'none'), count(*) from flyway_schema_history where success")) {
            rs.next();
            System.out.println("flyway_max_version=" + rs.getString(1));
            System.out.println("flyway_success_count=" + rs.getInt(2));
        }
    }

    private static void count(java.sql.Connection connection) throws Exception {
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("select count(*) from iam_refresh_session")) {
            rs.next();
            System.out.println("refresh_session_count=" + rs.getLong(1));
        }
    }

    private static Session byRefresh(java.sql.Connection connection, String digest) throws Exception {
        try (var statement = connection.prepareStatement("""
                select id, family_id, status, revision, replaced_by_id, refresh_token_digest
                  from iam_refresh_session
                 where refresh_token_digest = ?
                """)) {
            statement.setString(1, digest);
            try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Session(
                        rs.getString("id"),
                        rs.getString("family_id"),
                        rs.getString("status"),
                        rs.getInt("revision"),
                        rs.getString("replaced_by_id"),
                        rs.getString("refresh_token_digest")
                );
            }
        }
    }

    private static long activeCount(java.sql.Connection connection, String familyId) throws Exception {
        try (var statement = connection.prepareStatement(
                "select count(*) from iam_refresh_session where family_id = ? and status = 'ACTIVE'")) {
            statement.setString(1, familyId);
            try (var rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static long totalCount(java.sql.Connection connection, String familyId) throws Exception {
        try (var statement = connection.prepareStatement(
                "select count(*) from iam_refresh_session where family_id = ?")) {
            statement.setString(1, familyId);
            try (var rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void rotation(java.sql.Connection connection, String oldDigest, String newDigest) throws Exception {
        Session oldSession = byRefresh(connection, oldDigest);
        Session newSession = byRefresh(connection, newDigest);
        boolean oldFound = oldSession != null;
        boolean newFound = newSession != null;
        System.out.println("rotation_old_found=" + oldFound);
        System.out.println("rotation_new_found=" + newFound);
        if (!oldFound || !newFound) {
            return;
        }
        System.out.println("rotation_old_status=" + oldSession.status());
        System.out.println("rotation_new_status=" + newSession.status());
        System.out.println("rotation_same_family=" + oldSession.familyId().equals(newSession.familyId()));
        System.out.println("rotation_replacement_relation_valid=" + newSession.id().equals(oldSession.replacedById()));
        System.out.println("rotation_active_count=" + activeCount(connection, oldSession.familyId()));
        System.out.println("rotation_total_count=" + totalCount(connection, oldSession.familyId()));
        System.out.println("rotation_old_digest_preserved=" + oldDigest.equals(oldSession.refreshDigest()));
        System.out.println("rotation_revision_incremented=" + (oldSession.revision() > 0));
    }

    private static void reuse(java.sql.Connection connection, String oldDigest, String successorDigest) throws Exception {
        Session oldSession = byRefresh(connection, oldDigest);
        Session successor = byRefresh(connection, successorDigest);
        boolean oldFound = oldSession != null;
        boolean successorFound = successor != null;
        System.out.println("reuse_old_found=" + oldFound);
        System.out.println("reuse_successor_found=" + successorFound);
        if (!oldFound) {
            return;
        }
        System.out.println("reuse_active_count=" + activeCount(connection, oldSession.familyId()));
        try (var statement = connection.prepareStatement("""
                select count(*) = 0
                  from iam_refresh_session
                 where family_id = ? and status <> 'REUSE_DETECTED'
                """)) {
            statement.setString(1, oldSession.familyId());
            try (var rs = statement.executeQuery()) {
                rs.next();
                System.out.println("reuse_all_reuse_detected=" + rs.getBoolean(1));
            }
        }
        if (successorFound) {
            System.out.println("reuse_successor_status=" + successor.status());
        }
    }

    private static void concurrent(java.sql.Connection connection, String originalDigest) throws Exception {
        Session original = byRefresh(connection, originalDigest);
        boolean originalFound = original != null;
        System.out.println("concurrent_original_found=" + originalFound);
        if (!originalFound) {
            return;
        }
        System.out.println("concurrent_old_status=" + original.status());
        System.out.println("concurrent_total_count=" + totalCount(connection, original.familyId()));
        System.out.println("concurrent_active_count=" + activeCount(connection, original.familyId()));
        try (var statement = connection.prepareStatement("""
                select count(*)
                  from iam_refresh_session
                 where family_id = ? and status = 'ACTIVE' and id <> ?
                """)) {
            statement.setString(1, original.familyId());
            statement.setString(2, original.id());
            try (var rs = statement.executeQuery()) {
                rs.next();
                System.out.println("concurrent_active_successor_count=" + rs.getLong(1));
            }
        }
    }

    private static void logout(java.sql.Connection connection, String digest) throws Exception {
        Session session = byRefresh(connection, digest);
        boolean found = session != null;
        System.out.println("logout_session_found=" + found);
        if (!found) {
            return;
        }
        System.out.println("logout_status=" + session.status());
        System.out.println("logout_active_count=" + activeCount(connection, session.familyId()));
    }
}
JAVA
    javac -cp "${PG_JDBC_JAR}" "${TEST_DIR}/DbAssert.java"
}

db_assert() {
    [[ ${MODE} == "local" ]] || return 0
    DB_HOST="${DB_HOST}" DB_PORT="${DB_PORT}" DB_NAME="${DB_NAME}" \
    DB_USERNAME="${DB_USERNAME}" DB_PASSWORD="${DB_PASSWORD}" \
        java -cp "${TEST_DIR}:${PG_JDBC_JAR}" DbAssert "$@"
}

login_success() {
    local username="$1"
    local password="$2"
    local body="${TEST_DIR}/login-request.json"
    local response="${TEST_DIR}/login-response.json"
    json_body "${body}" --arg username "${username}" --arg password "${password}" \
        '{username:$username,password:$password}'
    local status
    status="$(http_request POST "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/login" "${body}" "${response}")"
    [[ ${status} == "200" ]] || return 1
    ACCESS_TOKEN="$(jq -r '.data.accessToken // empty' "${response}")"
    REFRESH_TOKEN="$(jq -r '.data.refreshToken // empty' "${response}")"
    [[ -n ${ACCESS_TOKEN} && -n ${REFRESH_TOKEN} ]]
}

start_java_service() {
    local name="$1"
    local jar="$2"
    local log_file="$3"
    shift 3
    (
        cd "${REPOSITORY_ROOT}"
        nohup env "$@" java -jar "${jar}" >"${log_file}" 2>&1 &
        echo $!
    )
}

stop_pid_file() {
    local pid_file="$1"
    [[ -f ${pid_file} ]] || return 0
    local pid
    pid="$(cat "${pid_file}")"
    if [[ -n ${pid} ]] && kill -0 "${pid}" >/dev/null 2>&1; then
        kill "${pid}" >/dev/null 2>&1 || true
        for _ in {1..30}; do
            kill -0 "${pid}" >/dev/null 2>&1 || break
            sleep 1
        done
        kill -0 "${pid}" >/dev/null 2>&1 && kill -9 "${pid}" >/dev/null 2>&1 || true
    fi
}

stop_local_services() {
    stop_pid_file "${TEST_DIR}/gateway.pid"
    stop_pid_file "${TEST_DIR}/iam.pid"
}

start_iam_local() {
    local redis_port="${1:-${REDIS_PORT}}"
    local redis_host="${2:-${REDIS_HOST}}"
    stop_pid_file "${TEST_DIR}/iam.pid"
    local -a envs=(
        "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"
        "SERVER_PORT=${IAM_HOST_PORT}"
        "DB_HOST=${DB_HOST}"
        "DB_PORT=${DB_PORT}"
        "DB_NAME=${DB_NAME}"
        "DB_USERNAME=${DB_USERNAME}"
        "SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
        "SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=${DB_USERNAME}"
        "SPRING_DATA_REDIS_HOST=${redis_host}"
        "SPRING_DATA_REDIS_PORT=${redis_port}"
        "SPRING_DATA_REDIS_DATABASE=${REDIS_DATABASE}"
        "NACOS_SERVER_ADDR=${NACOS_SERVER_ADDR}"
        "NACOS_NAMESPACE=${NACOS_NAMESPACE}"
        "NACOS_GROUP=${NACOS_GROUP}"
        "NACOS_USERNAME=${NACOS_USERNAME}"
        "NACOS_PASSWORD=${NACOS_PASSWORD}"
        "IAM_ISSUER_URI=http://127.0.0.1:${IAM_HOST_PORT}"
        "IAM_JWK_SET_URI=http://127.0.0.1:${IAM_HOST_PORT}/oauth2/jwks"
        "SYNAPSE_PLATFORM_AUDIENCE=${SYNAPSE_PLATFORM_AUDIENCE}"
        "SYNAPSE_CONSOLE_CLIENT_ID=${SYNAPSE_CONSOLE_CLIENT_ID}"
        "IAM_GATEWAY_PROOF_ENABLED=true"
        "IAM_GATEWAY_PROOF_REPLAY_ENABLED=true"
        "IAM_TRUSTED_GATEWAY_ID=${GATEWAY_ID}:synapse-iam-server"
        "GATEWAY_PROOF_SECRET=${GATEWAY_PROOF_SECRET}"
        "IAM_BOOTSTRAP_ENABLED=true"
        "IAM_BOOTSTRAP_USERNAME=${IAM_BOOTSTRAP_USERNAME}"
        "IAM_BOOTSTRAP_PASSWORD=${IAM_BOOTSTRAP_PASSWORD}"
        "MANAGEMENT_HEALTH_REDIS_ENABLED=false"
        "JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS}"
        "TZ=UTC"
    )
    [[ -n ${DB_PASSWORD} ]] && envs+=("DB_PASSWORD=${DB_PASSWORD}" "SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=${DB_PASSWORD}")
    [[ -n ${REDIS_PASSWORD} ]] && envs+=("SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}")
    local pid
    pid="$(start_java_service iam "${IAM_JAR}" "${TEST_DIR}/iam.log" "${envs[@]}")"
    printf '%s' "${pid}" >"${TEST_DIR}/iam.pid"
}

start_gateway_local() {
    local redis_port="${1:-${REDIS_PORT}}"
    local redis_host="${2:-${REDIS_HOST}}"
    stop_pid_file "${TEST_DIR}/gateway.pid"
    local -a envs=(
        "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"
        "SERVER_PORT=${GATEWAY_HOST_PORT}"
        "SPRING_DATA_REDIS_HOST=${redis_host}"
        "SPRING_DATA_REDIS_PORT=${redis_port}"
        "SPRING_DATA_REDIS_DATABASE=${REDIS_DATABASE}"
        "NACOS_SERVER_ADDR=${NACOS_SERVER_ADDR}"
        "NACOS_NAMESPACE=${NACOS_NAMESPACE}"
        "NACOS_GROUP=${NACOS_GROUP}"
        "NACOS_USERNAME=${NACOS_USERNAME}"
        "NACOS_PASSWORD=${NACOS_PASSWORD}"
        "IAM_ISSUER_URI=http://127.0.0.1:${IAM_HOST_PORT}"
        "IAM_JWK_SET_URI=http://127.0.0.1:${IAM_HOST_PORT}/oauth2/jwks"
        "SYNAPSE_GATEWAY_AUDIENCE=${SYNAPSE_GATEWAY_AUDIENCE}"
        "SYNAPSE_PLATFORM_AUDIENCE=${SYNAPSE_PLATFORM_AUDIENCE}"
        "GATEWAY_PROOF_ENABLED=true"
        "GATEWAY_ID=${GATEWAY_ID}"
        "GATEWAY_PROOF_SECRET=${GATEWAY_PROOF_SECRET}"
        "MANAGEMENT_HEALTH_REDIS_ENABLED=false"
        "JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS}"
        "TZ=UTC"
    )
    [[ -n ${REDIS_PASSWORD} ]] && envs+=("SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}")
    local pid
    pid="$(start_java_service gateway "${GATEWAY_JAR}" "${TEST_DIR}/gateway.log" "${envs[@]}")"
    printf '%s' "${pid}" >"${TEST_DIR}/gateway.pid"
}

configure_local() {
    TEST_DIR="/tmp/synapse-auth-test-local"
    SPRING_PROFILES_ACTIVE="$(config_value SPRING_PROFILES_ACTIVE dev)"
    JAVA_TOOL_OPTIONS="$(config_value JAVA_TOOL_OPTIONS "-XX:MaxRAMPercentage=75.0")"
    DB_HOST="$(config_value DB_HOST 127.0.0.1)"
    DB_PORT="$(config_value DB_PORT 5432)"
    DB_NAME="$(config_value DB_NAME "$(config_value POSTGRES_DB synapse_iam)")"
    DB_USERNAME="$(config_value DB_USERNAME postgres)"
    DB_PASSWORD="$(config_value DB_PASSWORD "")"
    REDIS_HOST="$(config_value REDIS_HOST 127.0.0.1)"
    REDIS_PORT="$(config_value REDIS_PORT 6379)"
    REDIS_DATABASE="$(config_value REDIS_DATABASE 0)"
    REDIS_PASSWORD="$(config_value REDIS_PASSWORD "")"
    NACOS_SERVER_ADDR="$(config_value NACOS_SERVER_ADDR 127.0.0.1:8848)"
    [[ ${NACOS_SERVER_ADDR} == "nacos:8848" ]] && NACOS_SERVER_ADDR="127.0.0.1:8848"
    NACOS_NAMESPACE="$(config_value NACOS_NAMESPACE "")"
    NACOS_GROUP="$(config_value NACOS_GROUP SYNAPSE_PLATFORM_DEV)"
    NACOS_USERNAME="$(config_value NACOS_USERNAME "")"
    NACOS_PASSWORD="$(config_value NACOS_PASSWORD "")"
    IAM_HOST_PORT="$(config_value IAM_TEST_PORT "$(config_value IAM_HOST_PORT 18100)")"
    GATEWAY_HOST_PORT="$(config_value GATEWAY_TEST_PORT "$(config_value GATEWAY_HOST_PORT 18080)")"
    GATEWAY_ID="$(config_value GATEWAY_ID synapse-gateway)"
    GATEWAY_PROOF_SECRET="$(config_value GATEWAY_PROOF_SECRET "")"
    IAM_BOOTSTRAP_USERNAME="$(config_value IAM_BOOTSTRAP_USERNAME "")"
    IAM_BOOTSTRAP_PASSWORD="$(config_value IAM_BOOTSTRAP_PASSWORD "")"
    SYNAPSE_PLATFORM_AUDIENCE="$(config_value SYNAPSE_PLATFORM_AUDIENCE synapse-platform)"
    SYNAPSE_GATEWAY_AUDIENCE="$(config_value SYNAPSE_GATEWAY_AUDIENCE synapse-platform)"
    SYNAPSE_CONSOLE_CLIENT_ID="$(config_value SYNAPSE_CONSOLE_CLIENT_ID synapse-console)"
    BAD_REDIS_PORT="$(config_value BAD_REDIS_PORT 16380)"
    PG_JDBC_JAR="$(config_value PG_JDBC_JAR "${PG_JDBC_JAR_DEFAULT}")"
    IAM_JAR="${REPOSITORY_ROOT}/synapse-iam-platform/synapse-iam-server/target/synapse-iam-server-0.1.0-SNAPSHOT.jar"
    GATEWAY_JAR="${REPOSITORY_ROOT}/synapse-gateway-platform/target/synapse-gateway-platform-0.1.0-SNAPSHOT.jar"

    [[ -n ${GATEWAY_PROOF_SECRET} ]] || die "缺少 GATEWAY_PROOF_SECRET，请在环境变量或 ${ENV_FILE} 中提供"
    [[ -n ${IAM_BOOTSTRAP_USERNAME} ]] || die "缺少 IAM_BOOTSTRAP_USERNAME，请在环境变量或 ${ENV_FILE} 中提供"
    [[ -n ${IAM_BOOTSTRAP_PASSWORD} ]] || die "缺少 IAM_BOOTSTRAP_PASSWORD，请在环境变量或 ${ENV_FILE} 中提供"
}

check_local_ports() {
    for port in "${IAM_HOST_PORT}" "${GATEWAY_HOST_PORT}"; do
        if lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
            die "本次测试端口已被占用: ${port}"
        fi
    done
    if lsof -nP -iTCP:"${BAD_REDIS_PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
        die "Redis 故障模拟端口已被占用: ${BAD_REDIS_PORT}"
    fi
}

probe_local_middleware() {
    log_info "本地中间件只读探测"
    nc -z "${DB_HOST}" "${DB_PORT}" || die "PostgreSQL 端口不可达"
    nc -z "${REDIS_HOST}" "${REDIS_PORT}" || die "Redis 端口不可达"
    nc -z "${NACOS_SERVER_ADDR%:*}" "${NACOS_SERVER_ADDR##*:}" || die "Nacos 端口不可达"
    if redis_cli PING | grep -q PONG; then
        pass "Redis PING"
    else
        die "Redis PING 失败"
    fi
    local key_count
    key_count="$(redis_cli --scan --pattern 'synapse:iam:authorization-snapshot:*' | wc -l | tr -d ' ')"
    log_info "测试前 Redis 授权快照 Key 数量: ${key_count}"
}

cleanup() {
    ACCESS_TOKEN=""
    REFRESH_TOKEN=""
    SECOND_ACCESS_TOKEN=""
    SECOND_REFRESH_TOKEN=""
    CURRENT_DIGEST=""
    if [[ ${MODE} == "local" ]]; then
        stop_local_services
        if [[ ${#REDIS_KEYS_CREATED[@]} -gt 0 ]]; then
            for key in "${REDIS_KEYS_CREATED[@]}"; do
                [[ -n ${key} ]] && redis_cli DEL "${key}" >/dev/null 2>&1 || true
            done
        fi
        if [[ -f ${TEST_DIR}/nonce-before.txt && -f ${TEST_DIR}/nonce-after.txt ]]; then
            comm -13 "${TEST_DIR}/nonce-before.txt" "${TEST_DIR}/nonce-after.txt" 2>/dev/null \
                | while IFS= read -r key; do [[ -n ${key} ]] && redis_cli DEL "${key}" >/dev/null 2>&1 || true; done
        fi
    fi
    if [[ ${#DIGEST_FILES[@]} -gt 0 ]]; then
        for file in "${DIGEST_FILES[@]}"; do
            [[ -f ${file} ]] && : >"${file}" && rm -f "${file}"
        done
    fi
    if [[ -d ${TEST_DIR} ]]; then
        find "${TEST_DIR}" -type f \
            ! -name 'iam.log' ! -name 'gateway.log' \
            -delete 2>/dev/null || true
    fi
}

setup() {
    require_command jq
    require_command shasum
    require_command redis-cli
    require_command nc
    require_command lsof
    curl_bin >/dev/null
    [[ -f ${ENV_FILE} ]] || die "未找到环境文件: ${ENV_FILE}"

    if [[ ${MODE} == "compose" ]]; then
        require_command docker
        docker compose version >/dev/null 2>&1 || die "需要 Docker Compose v2"
        TEST_DIR="/tmp/synapse-auth-test"
        GATEWAY_HOST_PORT="$(config_value GATEWAY_HOST_PORT 8080)"
        IAM_HOST_PORT="$(config_value IAM_HOST_PORT 8100)"
        IAM_BOOTSTRAP_USERNAME="$(config_value IAM_BOOTSTRAP_USERNAME "")"
        IAM_BOOTSTRAP_PASSWORD="$(config_value IAM_BOOTSTRAP_PASSWORD "")"
        GATEWAY_PROOF_SECRET="$(config_value GATEWAY_PROOF_SECRET "")"
        return
    fi

    require_command java
    require_command javac
    configure_local
    check_local_ports
}

start_environment() {
    if [[ ${MODE} == "compose" ]]; then
        log_info "Case 01: Compose config 校验"
        compose config >/dev/null
        pass "Compose config"
        log_info "打包 IAM 和 Gateway 本地镜像输入 JAR"
        "${MVN_BIN}" -pl :synapse-gateway-platform,:synapse-iam-api,:synapse-iam-client,:synapse-iam-server -am -DskipTests package >/dev/null
        pass "Maven package"
        log_info "启动本地认证闭环 Compose"
        compose up -d --build
        pass "Compose up"
        wait_http "IAM" "http://127.0.0.1:${IAM_HOST_PORT}/actuator/health" || true
        wait_http "Gateway" "http://127.0.0.1:${GATEWAY_HOST_PORT}/actuator/health" || true
        return
    fi

    probe_local_middleware
    log_info "打包 IAM 和 Gateway 可执行 JAR"
    "${MVN_BIN}" -pl :synapse-gateway-platform,:synapse-iam-api,:synapse-iam-client,:synapse-iam-server -am -DskipTests package >/dev/null
    pass "Maven package"
    build_db_helper
    redis_cli --scan --pattern 'synapse:iam:gateway-proof-nonce:*' | sort >"${TEST_DIR}/nonce-before.txt"
    log_info "启动本地 IAM 进程"
    start_iam_local
    wait_http "IAM" "http://127.0.0.1:${IAM_HOST_PORT}/actuator/health"
    log_info "启动本地 Gateway 进程"
    start_gateway_local
    wait_http "Gateway" "http://127.0.0.1:${GATEWAY_HOST_PORT}/actuator/health"
    pass "Case 01 本地 IAM/Gateway 健康检查"
    db_assert probe >"${TEST_DIR}/db-probe.out"
    if [[ $(kv "$(cat "${TEST_DIR}/db-probe.out")" refresh_table_exists) == "true" ]]; then
        pass "PostgreSQL Flyway V2 表存在"
    else
        fail "PostgreSQL Flyway V2 表不存在"
    fi
}

assert_snapshot() {
    local access_token="$1"
    local name="$2"
    local digest key value ttl subject_id
    digest="$(sha256_hex "${access_token}")"
    CURRENT_DIGEST="${digest}"
    key="synapse:iam:authorization-snapshot:${digest}"
    record_redis_key "${key}"
    value="$(redis_cli GET "${key}")"
    ttl="$(redis_cli TTL "${key}")"
    if [[ -n ${value} ]]; then pass "${name} Redis 快照存在"; else fail "${name} Redis 快照不存在"; fi
    if [[ ${key} != *"${access_token}"* && ${value} != *"${access_token}"* ]]; then
        pass "${name} Key/Value 不包含原始 Access Token"
    else
        fail "${name} Redis Key/Value 泄露原始 Access Token"
    fi
    if [[ ${ttl} =~ ^[0-9]+$ && ${ttl} -gt 0 ]]; then pass "${name} TTL 大于零"; else fail "${name} TTL 非正数"; fi
    subject_id="$(jq -r '.subjectId // empty' <<<"${value}")"
    if [[ -n ${subject_id} ]]; then pass "${name} 快照字段包含测试账号主体"; else fail "${name} 快照缺少测试账号主体"; fi
}

run_cases() {
    local response status body digest key value ttl
    response="${TEST_DIR}/response.json"

    status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}")"
    assert_status "${status}" "401" "Case 02 未认证 /auth/me 返回 401"

    body="${TEST_DIR}/bad-login.json"
    json_body "${body}" '{username:"missing-user",password:"wrong-password"}'
    status="$(http_request POST "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/login" "${body}" "${response}")"
    if [[ ${status} == "401" && $(jq -r '.code // empty' "${response}") == "IAM_AUTHENTICATION_FAILED" ]]; then
        pass "Case 03 错误账号登录失败且错误码统一"
    else
        fail "Case 03 登录失败状态或错误码异常"
    fi

    if login_success "${IAM_BOOTSTRAP_USERNAME}" "${IAM_BOOTSTRAP_PASSWORD}"; then
        pass "Case 04 登录成功"
    else
        fail "Case 04 登录失败"
    fi

    if [[ $(awk -F. '{print NF-1}' <<<"${ACCESS_TOKEN}") -ne 2 ]]; then
        pass "Case 04 Access Token 不是 JWT 三段式"
    else
        fail "Case 04 Access Token 不应是 JWT 三段式"
    fi

    assert_snapshot "${ACCESS_TOKEN}" "Case 05"

    status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}")"
    assert_status "${status}" "200" "Case 06 Gateway -> IAM /auth/me 返回 200"

    status="$(http_request GET "http://127.0.0.1:${IAM_HOST_PORT}/auth/me" "" "${response}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}")"
    assert_non_200 "${status}" "Case 07 直连 IAM 因缺 GatewayProof 被拒绝"

    status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -H "X-Synapse-Gateway-Proof-Version: forged" \
        -H "X-Synapse-Gateway-Id: forged" \
        -H "X-Synapse-Gateway-Signature: forged")"
    assert_status "${status}" "200" "Case 08 伪造 Gateway Header 不能绕过"

    body="${TEST_DIR}/refresh.json"
    local old_refresh_file new_refresh_file old_access_digest new_access_digest old_refresh_token old_access_token
    old_refresh_token="${REFRESH_TOKEN}"
    old_access_token="${ACCESS_TOKEN}"
    old_refresh_file="$(write_secret_file old-refresh.digest "$(sha256_hex "${REFRESH_TOKEN}")")"
    old_access_digest="$(sha256_hex "${ACCESS_TOKEN}")"
    json_body "${body}" --arg refreshToken "${REFRESH_TOKEN}" '{refreshToken:$refreshToken}'
    status="$(http_request POST "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/refresh" "${body}" "${response}")"
    if [[ ${status} == "200" ]]; then
        SECOND_ACCESS_TOKEN="$(jq -r '.data.accessToken // empty' "${response}")"
        SECOND_REFRESH_TOKEN="$(jq -r '.data.refreshToken // empty' "${response}")"
        pass "Case 09 Refresh rotation 成功"
    else
        fail "Case 09 Refresh rotation 失败"
    fi
    new_refresh_file="$(write_secret_file new-refresh.digest "$(sha256_hex "${SECOND_REFRESH_TOKEN}")")"
    new_access_digest="$(sha256_hex "${SECOND_ACCESS_TOKEN}")"
    record_redis_key "synapse:iam:authorization-snapshot:${new_access_digest}"

    status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}" \
        -H "Authorization: Bearer ${old_access_token}")"
    assert_status "${status}" "401" "Case 09 旧 Access Token 返回 401"
    status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}" \
        -H "Authorization: Bearer ${SECOND_ACCESS_TOKEN}")"
    assert_status "${status}" "200" "Case 09 新 Access Token 返回 200"
    if [[ ${MODE} == "local" ]]; then
        local rotation_out
        rotation_out="$(db_assert rotation "${old_refresh_file}" "${new_refresh_file}")"
        [[ $(kv "${rotation_out}" rotation_old_status) == "ROTATED" ]] && pass "Case 09 DB 旧 session 状态 ROTATED" || fail "Case 09 DB 旧 session 状态异常"
        [[ $(kv "${rotation_out}" rotation_new_status) == "ACTIVE" ]] && pass "Case 09 DB 新 session 状态 ACTIVE" || fail "Case 09 DB 新 session 状态异常"
        [[ $(kv "${rotation_out}" rotation_replacement_relation_valid) == "true" ]] && pass "Case 09 DB replaced_by_id 指向新 session" || fail "Case 09 DB replaced_by_id 异常"
        [[ $(kv "${rotation_out}" rotation_active_count) == "1" ]] && pass "Case 09 DB 同 family ACTIVE 数量为 1" || fail "Case 09 DB ACTIVE 数量异常"
        [[ $(kv "${rotation_out}" rotation_old_digest_preserved) == "true" ]] && pass "Case 09 DB 旧 refresh digest 保留" || fail "Case 09 DB 旧 refresh digest 未保留"
        [[ $(kv "${rotation_out}" rotation_revision_incremented) == "true" ]] && pass "Case 09 DB revision 已变化" || fail "Case 09 DB revision 未变化"
    fi

    status="$(http_request POST "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/refresh" "${body}" "${response}")"
    assert_status "${status}" "401" "Case 10 旧 Refresh Token reuse detection"

    if [[ ${MODE} == "local" ]]; then
        body="${TEST_DIR}/successor-refresh.json"
        json_body "${body}" --arg refreshToken "${SECOND_REFRESH_TOKEN}" '{refreshToken:$refreshToken}'
        status="$(http_request POST "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/refresh" "${body}" "${response}")"
        assert_status "${status}" "401" "Case 10 successor Refresh Token 失效"
        status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}" \
            -H "Authorization: Bearer ${SECOND_ACCESS_TOKEN}")"
        assert_status "${status}" "401" "Case 10 successor Access Token 快照失效"
        local reuse_out
        reuse_out="$(db_assert reuse "${old_refresh_file}" "${new_refresh_file}")"
        [[ $(kv "${reuse_out}" reuse_active_count) == "0" ]] && pass "Case 10 DB 同 family ACTIVE 数量为 0" || fail "Case 10 DB ACTIVE 数量异常"
        [[ $(kv "${reuse_out}" reuse_all_reuse_detected) == "true" ]] && pass "Case 10 DB family 状态 REUSE_DETECTED" || fail "Case 10 DB family 状态异常"
    fi

    login_success "${IAM_BOOTSTRAP_USERNAME}" "${IAM_BOOTSTRAP_PASSWORD}" || fail "并发 refresh 前登录失败"
    local concurrent_refresh_file
    concurrent_refresh_file="$(write_secret_file concurrent-refresh.digest "$(sha256_hex "${REFRESH_TOKEN}")")"
    body="${TEST_DIR}/concurrent-refresh.json"
    json_body "${body}" --arg refreshToken "${REFRESH_TOKEN}" '{refreshToken:$refreshToken}'
    http_request POST "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/refresh" "${body}" \
        "${TEST_DIR}/concurrent-a.json" >"${TEST_DIR}/concurrent-a.status" &
    local pid_a=$!
    http_request POST "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/refresh" "${body}" \
        "${TEST_DIR}/concurrent-b.json" >"${TEST_DIR}/concurrent-b.status" &
    local pid_b=$!
    wait "${pid_a}" || true
    wait "${pid_b}" || true
    local success_count failure_count
    success_count="$(grep -h '^200$' "${TEST_DIR}/concurrent-"*.status | wc -l | tr -d ' ')"
    failure_count="$(grep -hv '^200$' "${TEST_DIR}/concurrent-"*.status | wc -l | tr -d ' ')"
    [[ ${success_count} == "1" ]] && pass "Case 11 并发 refresh 只有一次成功" || fail "Case 11 并发 refresh 成功次数异常"
    [[ ${failure_count} == "1" ]] && pass "Case 11 并发 refresh 只有一次失败" || fail "Case 11 并发 refresh 失败次数异常"
    if [[ ${MODE} == "local" ]]; then
        local concurrent_out
        concurrent_out="$(db_assert concurrent "${concurrent_refresh_file}")"
        [[ $(kv "${concurrent_out}" concurrent_active_successor_count) == "1" ]] && pass "Case 11 DB active successor 数量为 1" || fail "Case 11 DB active successor 数量异常"
        [[ $(kv "${concurrent_out}" concurrent_active_count) == "1" ]] && pass "Case 11 DB 不存在两个 ACTIVE successor" || fail "Case 11 DB ACTIVE 数量异常"
    fi

    login_success "${IAM_BOOTSTRAP_USERNAME}" "${IAM_BOOTSTRAP_PASSWORD}" || fail "logout 前登录失败"
    local logout_refresh_file logout_access_key
    logout_refresh_file="$(write_secret_file logout-refresh.digest "$(sha256_hex "${REFRESH_TOKEN}")")"
    logout_access_key="synapse:iam:authorization-snapshot:$(sha256_hex "${ACCESS_TOKEN}")"
    record_redis_key "${logout_access_key}"
    body="${TEST_DIR}/logout.json"
    json_body "${body}" --arg refreshToken "${REFRESH_TOKEN}" '{refreshToken:$refreshToken}'
    status="$(http_request POST "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/logout" "${body}" "${response}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}")"
    assert_status "${status}" "200" "Case 12 Logout 成功"
    ttl="$(redis_cli TTL "${logout_access_key}")"
    [[ ${ttl} == "-2" ]] && pass "Case 12 Redis 快照删除" || fail "Case 12 Redis 快照未删除"
    status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}")"
    assert_status "${status}" "401" "Case 12 Logout 后 Access Token 失效"
    body="${TEST_DIR}/logout-refresh-request.json"
    json_body "${body}" --arg refreshToken "${REFRESH_TOKEN}" '{refreshToken:$refreshToken}'
    status="$(http_request POST "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/refresh" "${body}" "${response}")"
    assert_status "${status}" "401" "Case 12 Logout 后 Refresh Token 失效"
    if [[ ${MODE} == "local" ]]; then
        local logout_out
        logout_out="$(db_assert logout "${logout_refresh_file}")"
        [[ $(kv "${logout_out}" logout_status) == "REVOKED" ]] && pass "Case 12 DB 当前 session 状态 REVOKED" || fail "Case 12 DB 当前 session 状态异常"
        [[ $(kv "${logout_out}" logout_active_count) == "0" ]] && pass "Case 12 DB active session 数量为 0" || fail "Case 12 DB active session 数量异常"
    fi

    login_success "${IAM_BOOTSTRAP_USERNAME}" "${IAM_BOOTSTRAP_PASSWORD}" || fail "删除 Redis 快照前登录失败"
    digest="$(sha256_hex "${ACCESS_TOKEN}")"
    key="synapse:iam:authorization-snapshot:${digest}"
    record_redis_key "${key}"
    redis_cli DEL "${key}" >/dev/null
    status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}")"
    assert_status "${status}" "401" "Case 13 删除 Redis 快照后返回 401"

    login_success "${IAM_BOOTSTRAP_USERNAME}" "${IAM_BOOTSTRAP_PASSWORD}" || fail "Redis 故障前登录失败"
    if [[ ${MODE} == "compose" ]]; then
        compose stop redis >/dev/null
        status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" || true)"
        assert_status "${status}" "503" "Case 14 Redis 不可用时返回 503"
        compose start redis >/dev/null
        sleep 5
        if redis_cli PING | grep -q PONG; then pass "Case 14 Redis 恢复 PING"; else fail "Case 14 Redis 未恢复"; fi
    else
        stop_pid_file "${TEST_DIR}/gateway.pid"
        start_gateway_local "${BAD_REDIS_PORT}" "${REDIS_HOST}"
        local redis_down_deadline=$((SECONDS + 90))
        until [[ ${status:-} == "503" ]]; do
            status="$(http_request GET "http://127.0.0.1:${GATEWAY_HOST_PORT}/iam/auth/me" "" "${response}" \
                -H "Authorization: Bearer ${ACCESS_TOKEN}" || true)"
            if (( SECONDS >= redis_down_deadline )); then
                fail "Case 14 Redis 不可用时返回 503 status timeout: expected 503, actual ${status:-none}"
                break
            fi
            sleep 2
        done
        [[ ${status:-} == "503" ]] && pass "Case 14 Redis 不可用时返回 503"
        stop_pid_file "${TEST_DIR}/gateway.pid"
        start_gateway_local "${REDIS_PORT}" "${REDIS_HOST}"
        wait_http "Gateway Redis 恢复后健康检查" "http://127.0.0.1:${GATEWAY_HOST_PORT}/actuator/health" || true
        if redis_cli PING | grep -q PONG; then pass "Case 14 Redis 恢复 PING"; else fail "Case 14 Redis 未恢复"; fi
    fi

    status="$(http_request GET "http://127.0.0.1:${IAM_HOST_PORT}/auth/me" "" "${response}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -H "X-Synapse-Gateway-Proof-Version: v1" \
        -H "X-Synapse-Gateway-Id: ${GATEWAY_ID}:synapse-iam-server" \
        -H "X-Synapse-Gateway-Timestamp: 2000-01-01T00:00:00Z" \
        -H "X-Synapse-Gateway-Nonce: forged" \
        -H "X-Synapse-Gateway-Signature: forged")"
    assert_non_200 "${status}" "Case 15 IAM 拒绝错误 GatewayProof"

    if [[ ${MODE} == "local" ]]; then
        redis_cli --scan --pattern 'synapse:iam:gateway-proof-nonce:*' | sort >"${TEST_DIR}/nonce-after.txt"
    fi

    local service_log="${TEST_DIR}/service.log"
    if [[ ${MODE} == "compose" ]]; then
        compose logs --no-color gateway iam >"${service_log}"
    else
        cat "${TEST_DIR}/gateway.log" "${TEST_DIR}/iam.log" >"${service_log}"
    fi
    local leaked=false
    for secret in "${ACCESS_TOKEN}" "${REFRESH_TOKEN}" "${SECOND_ACCESS_TOKEN}" "${SECOND_REFRESH_TOKEN}" \
        "${IAM_BOOTSTRAP_PASSWORD}" "${GATEWAY_PROOF_SECRET}" "${CURRENT_DIGEST}"; do
        if [[ -n ${secret} ]] && grep -Fq -- "${secret}" "${service_log}"; then
            leaked=true
        fi
    done
    if grep -Fiq -- "canonical" "${service_log}"; then
        leaked=true
    fi
    if [[ ${leaked} == false ]]; then pass "Case 16 日志不包含敏感材料"; else fail "Case 16 日志包含敏感材料"; fi

    if [[ ${FAILED} -ne 0 ]]; then
        exit 1
    fi
}

main() {
    parse_args "$@"
    setup
    mkdir -p "${TEST_DIR}"
    chmod 700 "${TEST_DIR}"
    trap cleanup EXIT
    start_environment
    run_cases
}

main "$@"
