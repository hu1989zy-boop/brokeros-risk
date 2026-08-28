# Q-010 V7 Outstanding Items

## Required next gate

Independent Architect Implementation Review of this exact V7 package is
required. Codex has not self-approved implementation. Final Closure, staging,
commit, and push remain unauthorized.

## Known non-Q-010 repository issue

`sh scripts/verify-static.sh` reports the unchanged historical Q-009 whitespace
issue in
`review/q-006-design/Q-009-V6-Approved-Design-Git-Baseline-Prompt.md` at lines
67, 68, and the final blank line. Q-010 did not modify that file. No new Q-010
whitespace issue exists.

## Runtime advisory

Flyway 11.7.2 logs that MySQL 8.4 is newer than Flyway's latest tested 8.1
target. All V1/V2/V3 migration, validation, restart, constraint, transaction,
and concurrency checks passed on MySQL 8.4. Dependency changes are outside this
task and are not proposed here.

## Deferred by approved scope

Q-008 business implementation, platform/CRM adapters, automatic discovery or
synchronization, alias/merge/remap/repair/delete, public administration,
cryptographic attestation, retention/redaction policy, Kafka/Redis authority,
and final operational deployment policy remain future authorized work.
