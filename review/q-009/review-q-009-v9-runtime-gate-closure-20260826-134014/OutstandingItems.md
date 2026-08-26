# Outstanding Items

## Blocking Items

None.

All V8 blockers are closed:

- disposable MySQL 8.4 runtime: closed;
- mandatory MySQL test execution without skips: closed;
- Docker Compose runtime: closed;
- MySQL-backed Security Review evidence: closed;
- full zero-skip regression: closed.

## Required Governance Action

Final Architect review and explicit operator approval are still required before
any staging or commit. This is a workflow authorization step, not an unresolved
technical defect.

## Non-blocking Advisories

1. Flyway 11.7.2 warns that its latest formally tested MySQL version is 8.1,
   although all exercised MySQL 8.4.11 migration/runtime behavior passed. Track
   through normal dependency maintenance rather than changing the approved
   baseline during closure.
2. The pre-existing untracked historical V6 Prompt contains whitespace findings
   at lines 67, 68, and EOF. It is unrelated to Q-009, remains untouched, and
   must not be included in Q-009 staging.
3. The original Compose host ports collide with pre-existing developer MySQL
   and Redis services on this workstation. V9 verified the unchanged service
   topology using a temporary no-host-port override.
