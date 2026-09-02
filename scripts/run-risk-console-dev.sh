#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repository_root=$(dirname -- "$script_dir")
cd "$repository_root"

if ! command -v flutter >/dev/null 2>&1; then
  echo "Flutter SDK is required to run the Q-016 console." >&2
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
flutter pub get
dart run build_runner build --delete-conflicting-outputs
flutter run -d chrome \
  --web-port 4173 \
  --dart-define="BROKEROS_API_BASE_URL=http://localhost:8080" \
  --dart-define="BROKEROS_OIDC_ISSUER=http://localhost:8180/realms/brokeros" \
  --dart-define="BROKEROS_OIDC_CLIENT_ID=brokeros-risk-console"
