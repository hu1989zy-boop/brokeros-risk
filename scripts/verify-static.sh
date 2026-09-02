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
test "$migration_count" = "8"

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

q011_create_count=$(grep -Eic '^[[:space:]]*create[[:space:]]+table' \
    backend/src/main/resources/db/migration/V4__create_evidence_provenance_foundation.sql)
test "$q011_create_count" = "4"

if grep -Eiq '^[[:space:]]*(drop|truncate|alter|delete|update|insert)[[:space:]]' \
    backend/src/main/resources/db/migration/V4__create_evidence_provenance_foundation.sql; then
    printf '%s\n' "Q-011 migration must remain forward-only, additive, and schema-only." >&2
    exit 1
fi

for q011_table in evidence_record evidence_operation \
    evidence_operation_history evidence_access_log; do
    grep -Eq "^[[:space:]]*CREATE TABLE ${q011_table}[[:space:]]*\\(" \
        backend/src/main/resources/db/migration/V4__create_evidence_provenance_foundation.sql
done

q012_create_count=$(grep -Eic '^[[:space:]]*create[[:space:]]+table' \
    backend/src/main/resources/db/migration/V5__create_decision_provenance_foundation.sql)
test "$q012_create_count" = "4"

if grep -Eiq '^[[:space:]]*(drop|truncate|alter|delete|update|insert)[[:space:]]' \
    backend/src/main/resources/db/migration/V5__create_decision_provenance_foundation.sql; then
    printf '%s\n' "Q-012 migration must remain forward-only, additive, and schema-only." >&2
    exit 1
fi

for q012_table in decision_record decision_evidence_reference \
    decision_operation decision_access_log; do
    grep -Eq "^[[:space:]]*CREATE TABLE ${q012_table}[[:space:]]*\\(" \
        backend/src/main/resources/db/migration/V5__create_decision_provenance_foundation.sql
done

q013_create_count=$(grep -Eic '^[[:space:]]*create[[:space:]]+table' \
    backend/src/main/resources/db/migration/V6__create_action_provenance_foundation.sql)
test "$q013_create_count" = "3"

if grep -Eiq '^[[:space:]]*(drop|truncate|alter|delete|update|insert)[[:space:]]' \
    backend/src/main/resources/db/migration/V6__create_action_provenance_foundation.sql; then
    printf '%s\n' "Q-013 migration must remain forward-only, additive, and schema-only." >&2
    exit 1
fi

for q013_table in action_record action_operation action_access_log; do
    grep -Eq "^[[:space:]]*CREATE TABLE ${q013_table}[[:space:]]*\\(" \
        backend/src/main/resources/db/migration/V6__create_action_provenance_foundation.sql
done

q013_migration_test=backend/src/test/java/com/brokeros/risk/action/infrastructure/persistence/Q013MySqlMigrationTests.java
grep -Fq 'flyway.info().pending().length' "$q013_migration_test"
if grep -Eq 'flyway\.migrate\(\)\.migrationsExecuted\)\.isEqualTo\([0-9]+\)' \
    "$q013_migration_test"; then
    printf '%s\n' "Q-013 migration test must derive unrestricted migration counts dynamically." >&2
    exit 1
fi

q014_create_count=$(grep -Eic '^[[:space:]]*create[[:space:]]+table' \
    backend/src/main/resources/db/migration/V7__create_action_outcome_provenance_foundation.sql)
test "$q014_create_count" = "3"

if grep -Eiq '^[[:space:]]*(drop|truncate|alter|delete|update|insert)[[:space:]]' \
    backend/src/main/resources/db/migration/V7__create_action_outcome_provenance_foundation.sql; then
    printf '%s\n' "Q-014 migration must remain forward-only, additive, and schema-only." >&2
    exit 1
fi

for q014_table in action_outcome_record action_outcome_operation \
    action_outcome_access_log; do
    grep -Eq "^[[:space:]]*CREATE TABLE ${q014_table}[[:space:]]*\\(" \
        backend/src/main/resources/db/migration/V7__create_action_outcome_provenance_foundation.sql
done

if grep -Eiq '^[[:space:]]*status[[:space:]]' \
    backend/src/main/resources/db/migration/V7__create_action_outcome_provenance_foundation.sql; then
    printf '%s\n' "Q-014 must not introduce an action-outcome status column." >&2
    exit 1
fi

if grep -Eiq 'unique[[:space:]]*\\([[:space:]]*action_ref[[:space:]]*\\)' \
    backend/src/main/resources/db/migration/V7__create_action_outcome_provenance_foundation.sql; then
    printf '%s\n' "Q-014 action_ref must remain many-to-one." >&2
    exit 1
fi

q014_migration_test=backend/src/test/java/com/brokeros/risk/actionoutcome/infrastructure/persistence/Q014MySqlMigrationTests.java
grep -Fq 'flyway.info().pending().length' "$q014_migration_test"
if grep -Eq 'flyway\.migrate\(\)\.migrationsExecuted\)\.isEqualTo\([0-9]+\)' \
    "$q014_migration_test"; then
    printf '%s\n' "Q-014 migration test must derive unrestricted migration counts dynamically." >&2
    exit 1
fi

q008_create_count=$(grep -Eic '^[[:space:]]*create[[:space:]]+table' \
    backend/src/main/resources/db/migration/V8__create_risk_case_foundation.sql)
test "$q008_create_count" = "13"

if grep -Eiq '^[[:space:]]*(drop|truncate|alter|delete|update|insert)[[:space:]]' \
    backend/src/main/resources/db/migration/V8__create_risk_case_foundation.sql; then
    printf '%s\n' "Q-008 migration must remain forward-only, additive, and schema-only." >&2
    exit 1
fi

for q008_table in risk_case risk_case_transition_history \
    risk_case_assignment_history risk_case_priority_history \
    risk_case_evidence_association_history risk_case_decision_association \
    risk_case_decision_selection_history risk_case_action_association_history \
    risk_case_resolution_history risk_case_resolution_evidence_reference \
    risk_case_resolution_action_reference risk_case_note audit_record; do
    grep -Eq "^[[:space:]]*CREATE TABLE ${q008_table}[[:space:]]*\\(" \
        backend/src/main/resources/db/migration/V8__create_risk_case_foundation.sql
done

if grep -Eiq 'execution_payload|vendor_result|mt4|mt5|crm|bridge|liquidity_provider' \
    backend/src/main/resources/db/migration/V8__create_risk_case_foundation.sql; then
    printf '%s\n' "Q-008 migration must not contain execution or vendor-specific storage." >&2
    exit 1
fi

q008_migration_test=backend/src/test/java/com/brokeros/risk/riskcase/infrastructure/persistence/Q008MySqlMigrationTests.java
grep -Fq 'flyway.info().pending().length' "$q008_migration_test"
if grep -Eq 'flyway\.migrate\(\)\.migrationsExecuted\)\.isEqualTo\([0-9]+\)' \
    "$q008_migration_test"; then
    printf '%s\n' "Q-008 migration test must derive unrestricted migration counts dynamically." >&2
    exit 1
fi

if grep -R -Eiq 'ddl-auto|hbm2ddl' backend/src/main/resources; then
    printf '%s\n' "Hibernate schema generation must remain disabled." >&2
    exit 1
fi

printf '%s\n' "Static verification PASS"
