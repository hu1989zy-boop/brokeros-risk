#!/bin/sh

set -eu

REPOSITORY_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$REPOSITORY_ROOT"

log_info() {
    printf '%s\n' "INFO [$1] $2"
}

log_pass() {
    printf '%s\n' "PASS [$1] $2"
}

log_fail() {
    printf '%s\n' "FAIL [$1] $2" >&2
}

log_warn() {
    printf '%s\n' "WARN [$1] $2" >&2
}

if ! command -v docker >/dev/null 2>&1; then
    log_fail "preflight" "Docker with Compose v2 is required."
    exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
    log_fail "preflight" "openssl is required to generate ephemeral test credentials."
    exit 1
fi

if ! docker compose version >/dev/null; then
    log_fail "preflight" "Docker Compose v2 is unavailable."
    exit 1
fi
log_pass "preflight" "Required commands are available."

export COMPOSE_PROJECT_NAME="brokeros-risk-q004-$$"
export MYSQL_PASSWORD
export MYSQL_ROOT_PASSWORD
if ! MYSQL_PASSWORD=$(openssl rand -hex 24); then
    log_fail "credentials" "Could not generate the application database password."
    exit 1
fi
if ! MYSQL_ROOT_PASSWORD=$(openssl rand -hex 24); then
    log_fail "credentials" "Could not generate the MySQL root password."
    exit 1
fi
log_pass "credentials" "Ephemeral credentials generated without logging values."

if ! EVIDENCE_DIRECTORY=$(mktemp -d "${TMPDIR:-/tmp}/brokeros-risk-q004.XXXXXX"); then
    log_fail "evidence" "Could not create the temporary evidence directory."
    exit 1
fi

compose() {
    docker compose --project-name "$COMPOSE_PROJECT_NAME" --profile app "$@"
}

CURRENT_STAGE="compose-config"

cleanup() {
    verification_status=$?
    trap - EXIT HUP INT TERM
    set +e
    cleanup_status=0

    if [ "$verification_status" -ne 0 ]; then
        log_fail "infrastructure" "Stage '${CURRENT_STAGE}' exited with status ${verification_status}."
        if ! compose ps --all; then
            log_warn "diagnostics" "Could not list isolated Compose resources."
        fi
        if ! compose logs --no-color --tail=200; then
            log_warn "diagnostics" "Could not collect isolated Compose logs."
        fi
    fi

    if ! compose down --volumes --remove-orphans >/dev/null 2>&1; then
        log_fail "cleanup" "Could not remove all isolated Compose resources."
        cleanup_status=1
    fi

    if ! rm -rf -- "$EVIDENCE_DIRECTORY"; then
        log_fail "cleanup" "Could not remove the temporary evidence directory."
        cleanup_status=1
    fi

    if [ "$verification_status" -ne 0 ]; then
        log_warn "cleanup" "Cleanup attempted after failure; preserving original status ${verification_status}."
        exit "$verification_status"
    fi

    if [ "$cleanup_status" -ne 0 ]; then
        log_fail "infrastructure" "Verification passed, but isolated cleanup failed."
        exit "$cleanup_status"
    fi

    log_pass "cleanup" "Isolated Compose resources and evidence removed."
    log_pass "infrastructure" "All verification stages completed."
    exit 0
}

trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

wait_for_health() {
    service_name=$1
    attempt=0

    while [ "$attempt" -lt 90 ]; do
        if ! container_id=$(compose ps -q "$service_name"); then
            log_fail "health:${service_name}" "Could not resolve the service container."
            return 1
        fi

        if [ -n "$container_id" ]; then
            if ! container_status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id"); then
                log_fail "health:${service_name}" "Could not inspect container health."
                return 1
            fi

            case "$container_status" in
                healthy)
                    log_pass "health:${service_name}" "Container status is healthy."
                    return 0
                    ;;
                unhealthy|exited|dead)
                    log_fail "health:${service_name}" "Container status is ${container_status}."
                    if ! compose logs --no-color --tail=200 "$service_name" >&2; then
                        log_warn "diagnostics" "Could not collect ${service_name} logs."
                    fi
                    return 1
                    ;;
            esac
        fi

        attempt=$((attempt + 1))
        sleep 2
    done

    log_fail "health:${service_name}" "Timed out waiting for a healthy container."
    if ! compose logs --no-color --tail=200 "$service_name" >&2; then
        log_warn "diagnostics" "Could not collect ${service_name} logs."
    fi
    return 1
}

mysql_query() {
    query=$1
    compose exec -T -e MYSQL_PWD="$MYSQL_PASSWORD" mysql \
        mysql --user=brokeros --database=brokeros_risk \
        --batch --raw --skip-column-names --execute "$query"
}

log_info "compose-config" "Validating Docker Compose configuration."
if ! compose config >/dev/null; then
    log_fail "compose-config" "Docker Compose configuration is invalid."
    exit 1
fi
log_pass "compose-config" "Docker Compose configuration is valid."

CURRENT_STAGE="compose-startup"
log_info "compose-startup" "Building and starting the isolated stack."
if ! compose up --detach --build; then
    log_fail "compose-startup" "Could not build and start the isolated stack."
    exit 1
fi
log_pass "compose-startup" "Isolated stack started."

for service_name in mysql redis kafka backend; do
    CURRENT_STAGE="health:${service_name}"
    if ! wait_for_health "$service_name"; then
        exit 1
    fi
done

CURRENT_STAGE="mysql-flyway"
log_info "mysql-flyway" "Checking Flyway metadata and the foundation schema boundary."
if ! flyway_evidence=$(compose exec -T -e MYSQL_PWD="$MYSQL_PASSWORD" mysql \
    mysql --user=brokeros --database=brokeros_risk --batch --raw \
    --execute "SELECT version, description, type, script, checksum, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"); then
    log_fail "mysql-flyway" "Could not query Flyway history."
    exit 1
fi
printf '%s\n' "$flyway_evidence"

if ! flyway_v1_count=$(mysql_query "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND script = 'V1__initial_schema.sql' AND type = 'SQL' AND checksum IS NOT NULL AND success = 1;"); then
    log_fail "mysql-flyway" "Could not validate the V1 Flyway row."
    exit 1
fi
if [ "$flyway_v1_count" != "1" ]; then
    log_fail "mysql-flyway" "Expected exactly one successful V1 Flyway row; found ${flyway_v1_count}."
    exit 1
fi

if ! application_table_count=$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'brokeros_risk' AND table_name <> 'flyway_schema_history';"); then
    log_fail "mysql-flyway" "Could not inspect application-owned tables."
    exit 1
fi
if [ "$application_table_count" != "0" ]; then
    log_fail "mysql-flyway" "Expected no business tables; found ${application_table_count}."
    exit 1
fi
log_pass "mysql-flyway" "V1 is successful and no business table exists."

CURRENT_STAGE="flyway-restart"
log_info "flyway-restart" "Restarting the backend to verify Flyway idempotence."
if ! compose restart backend >/dev/null; then
    log_fail "flyway-restart" "Backend restart failed."
    exit 1
fi
if ! wait_for_health backend; then
    exit 1
fi

if ! flyway_v1_count_after_restart=$(mysql_query "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1;"); then
    log_fail "flyway-restart" "Could not query Flyway history after restart."
    exit 1
fi
if [ "$flyway_v1_count_after_restart" != "1" ]; then
    log_fail "flyway-restart" "Expected one V1 row after restart; found ${flyway_v1_count_after_restart}."
    exit 1
fi
log_pass "flyway-restart" "Flyway history remains idempotent after restart."

CURRENT_STAGE="redis"
log_info "redis" "Checking connectivity and empty keyspace."
if ! redis_ping=$(compose exec -T redis redis-cli --raw PING); then
    log_fail "redis" "PING command failed."
    exit 1
fi
if [ "$redis_ping" != "PONG" ]; then
    log_fail "redis" "Expected PONG; received '${redis_ping}'."
    exit 1
fi

if ! redis_key_count=$(compose exec -T redis redis-cli --raw DBSIZE); then
    log_fail "redis" "DBSIZE command failed."
    exit 1
fi
if [ "$redis_key_count" != "0" ]; then
    log_fail "redis" "Expected an empty keyspace; found ${redis_key_count} keys."
    exit 1
fi
log_pass "redis" "PING returned PONG and the keyspace is empty."

CURRENT_STAGE="kafka"
log_info "kafka" "Checking broker API connectivity without creating a topic."
if ! compose exec -T kafka /opt/kafka/bin/kafka-broker-api-versions.sh \
    --bootstrap-server localhost:9092 > "$EVIDENCE_DIRECTORY/kafka-api.txt"; then
    log_fail "kafka" "Broker API versions command failed."
    exit 1
fi
if [ ! -s "$EVIDENCE_DIRECTORY/kafka-api.txt" ]; then
    log_fail "kafka" "Broker API versions command returned no evidence."
    exit 1
fi
log_pass "kafka" "Broker API connectivity succeeded."

CURRENT_STAGE="backend-health"
log_info "backend-health" "Checking Actuator and application health contracts."
if ! compose exec -T backend wget -q -O - \
    http://localhost:8080/actuator/health > "$EVIDENCE_DIRECTORY/actuator-health.json"; then
    log_fail "backend-health" "Actuator health request failed."
    exit 1
fi
if ! grep -q '"status":"UP"' "$EVIDENCE_DIRECTORY/actuator-health.json"; then
    log_fail "backend-health" "Actuator response does not report UP."
    exit 1
fi

if ! compose exec -T backend wget -q -O - \
    http://localhost:8080/api/health > "$EVIDENCE_DIRECTORY/api-health.json"; then
    log_fail "backend-health" "Application health request failed."
    exit 1
fi
if ! grep -q '"code":"SUCCESS"' "$EVIDENCE_DIRECTORY/api-health.json"; then
    log_fail "backend-health" "Application health response does not use the SUCCESS ApiResponse code."
    exit 1
fi
if ! grep -q '"status":"UP"' "$EVIDENCE_DIRECTORY/api-health.json"; then
    log_fail "backend-health" "Application health response does not report data.status UP."
    exit 1
fi
log_pass "backend-health" "Actuator and application health contracts are valid."

CURRENT_STAGE="log-scan"
log_info "log-scan" "Checking scoped Compose logs for fatal runtime patterns."
if ! compose logs --no-color > "$EVIDENCE_DIRECTORY/compose.log"; then
    log_fail "log-scan" "Could not collect Compose logs."
    exit 1
fi

if grep -E 'FATAL|OutOfMemoryError|Exception in thread' \
    "$EVIDENCE_DIRECTORY/compose.log" > "$EVIDENCE_DIRECTORY/fatal-patterns.log"; then
    log_fail "log-scan" "Fatal runtime pattern detected."
    sed -n '1,40p' "$EVIDENCE_DIRECTORY/fatal-patterns.log" >&2
    exit 1
else
    grep_status=$?
    if [ "$grep_status" -ne 1 ]; then
        log_fail "log-scan" "Fatal-pattern scan failed with status ${grep_status}."
        exit 1
    fi
fi
log_pass "log-scan" "No fatal runtime pattern detected."

CURRENT_STAGE="complete"
