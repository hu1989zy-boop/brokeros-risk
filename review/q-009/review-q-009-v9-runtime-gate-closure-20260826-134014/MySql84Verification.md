# MySQL 8.4 Runtime Verification

- Gate: PASS
- Actual server version: MySQL 8.4.11
- Architecture: arm64
- Host MySQL 5.7 used: NO
- Mandatory test skipped: NO
- Cleanup: PASS

## Isolation Procedure

Fresh `mysql:8.4` containers were created with unique names, ephemeral
credentials, unique databases, and random loopback host ports. Readiness was
verified from inside each container using `mysqladmin ping`. Server version was
queried from the running server using `SELECT VERSION()` before Maven execution.

The first procedural attempt waited on Docker health status, but the official
image has no built-in Docker `HEALTHCHECK`; it therefore remained `running`
rather than becoming `healthy`. The bounded attempt exited and its cleanup trap
removed the container. The corrected procedure used `mysqladmin ping`. This was
an orchestration finding, not a repository implementation failure.

## Initial Runtime Test Finding

The first actual test execution reached MySQL 8.4.11, applied and validated V1
and V2, and then failed because the test expected
`DataIntegrityViolationException` for a MySQL CHECK violation. Actual evidence:

- MySQL error code: 3819
- SQL state: HY000
- Spring exception family: `DataAccessException`

The database correctly rejected the invalid row. The test assertion, rather
than the schema or production adapter, was defective.

## Corrected Targeted Test

Command form:

`Q009_MYSQL_TEST_URL=<disposable-8.4-url> Q009_MYSQL_TEST_USERNAME=<ephemeral-user> Q009_MYSQL_TEST_PASSWORD=<ephemeral-secret> mvn -Dtest=Q009MySqlIntegrationTests test`

Result after the test-only correction:

- Tests run: 1
- Failures: 0
- Errors: 0
- Skipped: 0
- Build: SUCCESS

The executed test proved:

- clean Flyway migration to V1 and upgrade to V2;
- Flyway validation and no-op repeated migration;
- binary/exact principal mapping behavior;
- mapping and grant uniqueness;
- actor-type and exact-capability CHECK enforcement;
- FK delete restriction;
- indexed mapping and authorization query plans;
- provisioning idempotence and deterministic conflict;
- mapping, capability, and actor lifecycle transitions; and
- fail-closed persistence adapter behavior.

Flyway 11.7.2 emitted its advisory that its latest formally tested MySQL version
is 8.1. The complete 8.4.11 execution passed; the advisory is recorded for
future dependency maintenance and is not a current runtime failure.
