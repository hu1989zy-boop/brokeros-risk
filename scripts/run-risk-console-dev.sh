#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repository_root=$(dirname -- "$script_dir")
cd "$repository_root"

if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
  echo "Node.js and npm are required to run the Q-016 console." >&2
  exit 1
fi

if [ ! -f .env ]; then
  echo "Copy .env.example to .env and set the local-only credentials first." >&2
  exit 1
fi

compose() {
  docker compose --profile console "$@"
}

cleanup() {
  compose stop console-backend keycloak >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

compose up --build --wait -d mysql redis kafka keycloak
compose exec -T keycloak /bin/bash -c \
  '/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password "$KC_BOOTSTRAP_ADMIN_PASSWORD"'
compose exec -T keycloak /bin/bash -c \
  '/opt/keycloak/bin/kcadm.sh set-password --realm brokeros --username q016-operator --new-password "$KEYCLOAK_OPERATOR_PASSWORD"'
compose run --build --rm security-bootstrap
compose up --build --wait -d console-backend

cd frontend
npm ci
npm run dev
