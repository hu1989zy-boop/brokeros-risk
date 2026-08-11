#!/bin/sh

set -eu

REPOSITORY_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$REPOSITORY_ROOT"

if ! command -v docker >/dev/null 2>&1; then
    printf '%s\n' "Docker with Compose v2 is required." >&2
    exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
    printf '%s\n' "openssl is required to generate ephemeral test credentials." >&2
    exit 1
fi

docker compose version >/dev/null

export COMPOSE_PROJECT_NAME="brokeros-risk-q004-$$"
export MYSQL_PASSWORD
export MYSQL_ROOT_PASSWORD
MYSQL_PASSWORD=$(openssl rand -hex 24)
MYSQL_ROOT_PASSWORD=$(openssl rand -hex 24)

EVIDENCE_DIRECTORY=$(mktemp -d "${TMPDIR:-/tmp}/brokeros-risk-q004.XXXXXX")

compose() {
    docker compose --project-name "$COMPOSE_PROJECT_NAME" --profile app "$@"
}

cleanup() {
    verification_status=$?
    set +e

    if [ "$verification_status" -ne 0 ]; then
        compose ps --all
        compose logs --no-color --tail=200
    fi

    compose down --volumes --remove-orphans >/dev/null 2>&1
    rm -rf "$EVIDENCE_DIRECTORY"
    trap - EXIT HUP INT TERM
    exit "$verification_status"
}

trap cleanup EXIT HUP INT TERM

wait_for_health() {
    service_name=$1
    attempt=0

    while [ "$attempt" -lt 90 ]; do
        container_id=$(compose ps -q "$service_name")

        if [ -n "$container_id" ]; then
            container_status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")

            case "$container_status" in
                healthy)
                    printf '%s\n' "${service_name} status: ${container_status}"
                    return 0
                    ;;
                unhealthy|exited|dead)
                    printf '%s\n' "${service_name} status: ${container_status}" >&2
                    compose logs --no-color --tail=200 "$service_name" >&2
                    return 1
                    ;;
            esac
        fi

        attempt=$((attempt + 1))
        sleep 2
    done

    printf '%s\n' "Timed out waiting for ${service_name}." >&2
    compose logs --no-color --tail=200 "$service_name" >&2
    return 1
}

mysql_query() {
    query=$1
    compose exec -T -e MYSQL_PWD="$MYSQL_PASSWORD" mysql \
        mysql --user=brokeros --database=brokeros_risk \
        --batch --raw --skip-column-names --execute "$query"
}

compose config >/dev/null
printf '%s\n' "Docker Compose config PASS"

compose up --detach --build

wait_for_health mysql
wait_for_health redis
wait_for_health kafka
wait_for_health backend

flyway_evidence=$(compose exec -T -e MYSQL_PWD="$MYSQL_PASSWORD" mysql \
    mysql --user=brokeros --database=brokeros_risk --batch --raw \
    --execute "SELECT version, description, type, script, checksum, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;")
printf '%s\n' "$flyway_evidence"

flyway_v1_count=$(mysql_query "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND script = 'V1__initial_schema.sql' AND type = 'SQL' AND checksum IS NOT NULL AND success = 1;")
test "$flyway_v1_count" = "1"

application_table_count=$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'brokeros_risk' AND table_name <> 'flyway_schema_history';")
test "$application_table_count" = "0"

compose restart backend >/dev/null
wait_for_health backend

flyway_v1_count_after_restart=$(mysql_query "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1;")
test "$flyway_v1_count_after_restart" = "1"

redis_ping=$(compose exec -T redis redis-cli --raw PING)
test "$redis_ping" = "PONG"

redis_key_count=$(compose exec -T redis redis-cli --raw DBSIZE)
test "$redis_key_count" = "0"

compose exec -T kafka /opt/kafka/bin/kafka-broker-api-versions.sh \
    --bootstrap-server localhost:9092 > "$EVIDENCE_DIRECTORY/kafka-api.txt"
test -s "$EVIDENCE_DIRECTORY/kafka-api.txt"

compose exec -T backend wget -q -O - \
    http://localhost:8080/actuator/health | grep -q '"status":"UP"'
compose exec -T backend wget -q -O - \
    http://localhost:8080/api/health | grep -q '"success":true'

compose logs --no-color > "$EVIDENCE_DIRECTORY/compose.log"
if grep -E 'FATAL|OutOfMemoryError|Exception in thread' "$EVIDENCE_DIRECTORY/compose.log"; then
    printf '%s\n' "Fatal log pattern detected." >&2
    exit 1
fi

printf '%s\n' "MySQL and Flyway verification PASS"
printf '%s\n' "Redis infrastructure verification PASS"
printf '%s\n' "Kafka infrastructure verification PASS"
printf '%s\n' "Backend health verification PASS"
printf '%s\n' "Infrastructure verification PASS"
