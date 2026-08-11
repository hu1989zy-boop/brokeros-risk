#!/bin/sh

set -eu

REPOSITORY_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$REPOSITORY_ROOT"

if ! command -v kubectl >/dev/null 2>&1; then
    printf '%s\n' "kubectl is required for Kustomize verification." >&2
    exit 1
fi

RENDER_DIRECTORY=$(mktemp -d "${TMPDIR:-/tmp}/brokeros-risk-kustomize.XXXXXX")

cleanup() {
    rm -rf "$RENDER_DIRECTORY"
}

trap cleanup EXIT HUP INT TERM

render_environment() {
    environment_name=$1
    output_file="$RENDER_DIRECTORY/${environment_name}.yaml"

    kubectl kustomize "deploy/kubernetes/${environment_name}" > "$output_file"
    test -s "$output_file"
    grep -q '^kind: Deployment$' "$output_file"
    grep -q '^kind: Service$' "$output_file"
    grep -q '^kind: ConfigMap$' "$output_file"
    grep -q 'name: brokeros-risk-backend' "$output_file"
    grep -q 'name: brokeros-risk-config' "$output_file"
    grep -q 'app.kubernetes.io/name: brokeros-risk' "$output_file"
    grep -q 'name: brokeros-risk-secrets' "$output_file"

    printf '%s\n' "Kustomize ${environment_name} render PASS"
}

render_environment base
render_environment test
render_environment prod

grep -q 'SPRING_PROFILES_ACTIVE: test' "$RENDER_DIRECTORY/test.yaml"
grep -q 'SPRING_PROFILES_ACTIVE: prod' "$RENDER_DIRECTORY/prod.yaml"

printf '%s\n' "Kustomize contract verification PASS"
