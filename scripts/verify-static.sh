#!/bin/sh

set -eu

REPOSITORY_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$REPOSITORY_ROOT"

git diff --check
git diff --cached --check

git ls-files --others --exclude-standard | while IFS= read -r untracked_file; do
    untracked_check=

    if untracked_check=$(git diff --no-index --check -- /dev/null \
        "$untracked_file" 2>&1); then
        :
    fi

    if [ -n "$untracked_check" ]; then
        printf '%s\n' "$untracked_check" >&2
        exit 1
    fi
done

BASE_SHA=${1:-}
ZERO_SHA=0000000000000000000000000000000000000000

if [ -n "$BASE_SHA" ] && [ "$BASE_SHA" != "$ZERO_SHA" ]; then
    case "$BASE_SHA" in
        *[!0-9a-fA-F]*)
            printf '%s\n' "Invalid base commit SHA." >&2
            exit 1
            ;;
    esac

    if ! git cat-file -e "${BASE_SHA}^{commit}" 2>/dev/null; then
        printf '%s\n' "Base commit is unavailable: $BASE_SHA" >&2
        exit 1
    fi

    git diff --check "${BASE_SHA}...HEAD"
else
    git show --check --format= HEAD
fi

find scripts -type f -name '*.sh' -exec sh -n {} \;

migration_count=$(find backend/src/main/resources/db/migration -maxdepth 1 \
    -type f -name 'V*__*.sql' | wc -l | tr -d '[:space:]')
test "$migration_count" = "3"

if grep -Eiq 'create[[:space:]]+table|alter[[:space:]]+table|drop[[:space:]]+table|truncate[[:space:]]+table' \
    backend/src/main/resources/db/migration/V1__initial_schema.sql; then
    printf '%s\n' "Business DDL is not allowed in the foundation migration." >&2
    exit 1
fi

q009_create_count=$(grep -Eic '^[[:space:]]*create[[:space:]]+table' \
    backend/src/main/resources/db/migration/V2__create_security_actor_foundation.sql)
test "$q009_create_count" = "3"

if grep -Eiq '^[[:space:]]*(drop|truncate|alter|delete|update)[[:space:]]' \
    backend/src/main/resources/db/migration/V2__create_security_actor_foundation.sql; then
    printf '%s\n' "Q-009 migration must remain forward-only and additive." >&2
    exit 1
fi

q010_create_count=$(grep -Eic '^[[:space:]]*create[[:space:]]+table' \
    backend/src/main/resources/db/migration/V3__create_trading_account_reference_authority.sql)
test "$q010_create_count" = "4"

if grep -Eiq '^[[:space:]]*(drop|truncate|alter|delete|update|insert)[[:space:]]' \
    backend/src/main/resources/db/migration/V3__create_trading_account_reference_authority.sql; then
    printf '%s\n' "Q-010 migration must remain forward-only, additive, and schema-only." >&2
    exit 1
fi

for q010_table in trading_account_authority_scope trading_account_reference \
    trading_account_authority_operation trading_account_authority_history; do
    grep -Eq "^[[:space:]]*CREATE TABLE ${q010_table}[[:space:]]*\\(" \
        backend/src/main/resources/db/migration/V3__create_trading_account_reference_authority.sql
done

if grep -R -Eiq 'ddl-auto|hbm2ddl' backend/src/main/resources; then
    printf '%s\n' "Hibernate schema generation must remain disabled." >&2
    exit 1
fi

printf '%s\n' "Static verification PASS"
