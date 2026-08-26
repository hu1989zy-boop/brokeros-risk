# MySQL 8.4 Verification

- Status: FAIL
- Mandatory test executed against MySQL 8.4: NO
- Database changes performed: NO

## Required Evidence

Q-009 requires the Flyway migration and JDBC persistence behavior to be
verified against an actual disposable MySQL 8.4 runtime. The required runtime
was not available.

## Observed Host State

- `127.0.0.1:3306` accepted a TCP connection.
- The MySQL protocol greeting reported server version `5.7.11` and
  `mysql_native_password`.
- The host process command identified `/usr/local/mysql/bin/mysqld` with a
  `/usr/local/mysql/data` data directory.
- The matching local client reports MySQL 5.7.11.
- No MySQL 8.4 executable, client, server installation, environment file, or
  usable container runtime was found.

The host MySQL was not used: it is not MySQL 8.4, is not known to be disposable,
and `Q009MySqlIntegrationTests` invokes Flyway `clean()` before migration.

## Targeted Test Result

Command:

`cd backend && mvn -Dtest=Q009MySqlIntegrationTests test`

Result:

- Tests run: 1
- Failures: 0
- Errors: 0
- Skipped: 1
- Maven result: BUILD SUCCESS
- Activation variable `Q009_MYSQL_TEST_URL`: not configured

The Maven process succeeded only because the mandatory integration test was
skipped. This is not acceptable runtime evidence and the gate remains FAIL.

## Missing Evidence

- Flyway V1 + V2 migration on MySQL 8.4
- MySQL 8.4 schema constraints and rejection cases
- JDBC principal mapping behavior
- JDBC exact-capability authorization behavior
- Idempotent provisioning and conflict behavior on MySQL 8.4

No implementation defect was inferred from the missing environment, and no code
or migration was changed.
