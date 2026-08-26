# Regression Verification

- Gate: PASS
- Environment: disposable MySQL 8.4.11 active
- Command: `cd backend && mvn verify` with documented Q-009 MySQL environment variables
- Build: SUCCESS
- Tests run: 58
- Failures: 0
- Errors: 0
- Skipped: 0

## Suite Counts

- Configuration contract: 7
- Security architecture: 2
- Security application: 6
- Security configuration: 3
- Q-009 MySQL integration: 1
- Security HTTP boundary: 10
- Security domain: 7
- Security bootstrap: 2
- Request correlation: 7
- Application context/API: 7
- Flyway contract: 2
- Global exception handling: 4

All mandatory Q-009 tests executed. No test was disabled, deleted, weakened, or
skipped.

## Dependency Evidence

The dependency tree resolved successfully:

- Spring Boot security/resource-server starters: 3.5.16
- Spring Security: 6.5.11
- Nimbus JOSE JWT: 9.37.4
- MySQL Connector/J: 9.7.0
- Flyway core/MySQL: 11.7.2

## Static and Configuration Checks

- `git diff --check`: PASS
- `git diff --cached --check`: PASS; staging empty
- shell syntax: PASS
- Q-009 migration count/schema/additive/DDL checks: PASS
- Hibernate schema-generation prohibition: PASS
- high-confidence changed-content secret scan: PASS

The repository-wide `scripts/verify-static.sh` reports only whitespace in the
pre-existing untracked historical file
`review/q-006-design/Q-009-V6-Approved-Design-Git-Baseline-Prompt.md` at lines
67, 68, and EOF. That file is outside the Q-009 implementation/commit scope and
will not exist in a clean checkout unless separately added. It was preserved
unchanged and is not a Q-009 gate blocker.
