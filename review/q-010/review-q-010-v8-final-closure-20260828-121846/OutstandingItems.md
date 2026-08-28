# Q-010 V8 Outstanding Items

## Blocking

None. No Q-010 correctness, security, persistence, concurrency, transaction,
traceability, drift, or verification blocker remains.

## Non-blocking

1. `review/q-006-design/Q-009-V6-Approved-Design-Git-Baseline-Prompt.md`
   retains its pre-existing lines 67/68 trailing whitespace and final blank
   line. Q-010 did not modify it; the same finding existed in V7.
2. Flyway 11.7.2 warns that MySQL 8.4 is newer than its latest tested 8.1
   target. Q-010's approved compatibility baseline is disposable MySQL 8.4;
   V1/V2/V3, restart, constraints, concurrency, rollback, and command tests all
   passed on MySQL 8.4.11. Dependency strategy was not changed in closure.
3. Actual deployment authority scopes, Q-009 service mappings/grants,
   attestations, manifests, credentials, and change windows remain environment
   inputs and are intentionally absent from Git and Review evidence.
4. Q-008 Risk Case, Evidence, Decision, Action, and ActionOutcome implementation
   remains future separately authorized work. Q-010 closes only the Trading
   Account reference prerequisite.

Independent external Architect review of V8 is the next governance action. It
is not a technical closure blocker and no commit is authorized by this package
alone.
