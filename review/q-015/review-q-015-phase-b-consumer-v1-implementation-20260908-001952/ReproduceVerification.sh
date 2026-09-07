#!/bin/sh
# Run from any directory inside the full repository. Uses disposable Docker resources.
set -eu
umask 077
q015_repo=$(git rev-parse --show-toplevel)
q015_tmp=$(mktemp -d "${TMPDIR:-/tmp}/q015-consumer-verify.XXXXXX")
q015_tag=$(basename "$q015_tmp" | tr '[:upper:]' '[:lower:]')
q015_network=false
q015_mysql=false
q015_cache=false
cleanup() {
  q015_exit=$?
  trap - EXIT HUP INT TERM
  if [ "$q015_mysql" = true ]; then docker rm -f -v "$q015_tag-mysql" >/dev/null; fi
  if [ "$q015_network" = true ]; then docker network rm "$q015_tag" >/dev/null; fi
  if [ "$q015_cache" = true ]; then docker volume rm "$q015_tag-m2" >/dev/null; fi
  rm -f "$q015_tmp/test.env"
  rmdir "$q015_tmp"
  exit "$q015_exit"
}
trap cleanup EXIT HUP INT TERM
q015_password=$(openssl rand -hex 24)
{
  printf 'MYSQL_ROOT_PASSWORD=%s\nMYSQL_PASSWORD=%s\n' "$q015_password" "$q015_password"
  printf 'MYSQL_DATABASE=q015_consumer_test\nMYSQL_USER=q015_test\n'
  for q015_id in Q008 Q009 Q010 Q011 Q012 Q013 Q014 Q015; do
    printf '%s_MYSQL_TEST_URL=jdbc:mysql://mysql:3306/q015_consumer_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC\n' "$q015_id"
    printf '%s_MYSQL_TEST_USERNAME=q015_test\n%s_MYSQL_TEST_PASSWORD=%s\n' "$q015_id" "$q015_id" "$q015_password"
  done
} > "$q015_tmp/test.env"
unset q015_password
docker network create "$q015_tag" >/dev/null
q015_network=true
docker volume create "$q015_tag-m2" >/dev/null
q015_cache=true
docker run -d --name "$q015_tag-mysql" --network "$q015_tag" --network-alias mysql \
  --env-file "$q015_tmp/test.env" mysql:8.4.11 --log-bin-trust-function-creators=1 >/dev/null
q015_mysql=true
q015_attempt=0
until docker exec "$q015_tag-mysql" sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -Nse "SELECT VERSION()"' 2>/dev/null; do
  q015_attempt=$((q015_attempt+1))
  test "$q015_attempt" -lt 60
  sleep 1
done
mkdir -p "$q015_repo/backend/target"
for q015_goal in test package; do
  docker run --rm --network "$q015_tag" --env-file "$q015_tmp/test.env" \
    -v "$q015_repo:/workspace:ro" -v "$q015_repo/backend/target:/workspace/backend/target" \
    -v "$q015_tag-m2:/root/.m2" -w /workspace/backend \
    maven:3.9.9-eclipse-temurin-21-alpine mvn --batch-mode --no-transfer-progress "$q015_goal"
done
