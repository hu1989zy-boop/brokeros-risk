# V9 Verified Runtime Fix

## Changed File

`backend/src/test/java/com/brokeros/risk/security/infrastructure/persistence/Q009MySqlIntegrationTests.java`

## Runtime Defect

The MySQL 8.4 CHECK constraints correctly rejected invalid `SYSTEM` actor type
and wildcard capability rows with MySQL error 3819 / SQL state HY000. Spring
JDBC translated that vendor result as `UncategorizedSQLException`, which is a
`DataAccessException`. The test had assumed every constraint rejection would be
translated as `DataIntegrityViolationException`.

V8 could not reveal the mismatch because the mandatory test was skipped when no
disposable MySQL 8.4 runtime was available.

## Smallest Correction

For the two CHECK-constraint assertions only, V9 now requires:

1. a Spring `DataAccessException`;
2. an SQL exception root cause;
3. MySQL error code 3819; and
4. SQL state HY000.

Duplicate-key and FK assertions remain specifically
`DataIntegrityViolationException`. The change is more faithful to the approved
constraint behavior and does not accept a successful invalid write, connection
failure, syntax failure, or unrelated SQL error.

## Scope

- Production Java changed: NO
- Configuration changed: NO
- Migration changed: NO
- Requirement/Architecture/ADR/Design changed: NO
- Test changed: YES — one file
- Business behavior changed: NO
- Test/gate weakened: NO

Targeted and full zero-skip MySQL 8.4 verification passed after the correction.
