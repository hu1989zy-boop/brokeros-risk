# Test Coverage

## Maven Suite

Final command: `mvn verify`

Result: **BUILD SUCCESS**

- Tests run: 58
- Failures: 0
- Errors: 0
- Skipped: 1

The skipped test is
`Q009MySqlIntegrationTests.verifiesMigrationConstraintsQueryPlansAndPersistenceLifecycle`
because `Q009_MYSQL_TEST_URL` was not available and this host has no disposable
MySQL runtime.

## Coverage by Test Class

| Test class | Tests | Coverage |
| --- | ---: | --- |
| `SecurityDomainTests` | 7 | ActorRef, principal, context, capability, decision, and lifecycle invariants |
| `SecurityApplicationTests` | 6 | mapping, guard allow/deny, service registry trust, provisioning delegation |
| `SecurityConfigurationTests` | 3 | required issuer/audience and bounded clock skew |
| `SecurityBoundaryIntegrationTests` | 10 | real signed RSA JWT/filter chain, spoofing, safe 401/403/503, mapping, allow, context isolation |
| `SecurityBootstrapCommandTests` | 2 | explicit manifest, no SYSTEM/unknown fields |
| `SecurityArchitectureTests` | 2 | framework-neutral domain/application and no identity-header/role bypass |
| `Q009MySqlIntegrationTests` | 1 skipped | V1→V2, schema/plan, provisioning, grant/mapping/actor lifecycle on MySQL 8.4 |
| Existing configuration tests | 7 | profile and catalog regression |
| Existing application smoke tests | 7 | public health and authenticated defaults |
| Existing correlation tests | 7 | request/trace behavior under security defaults |
| Existing Flyway tests | 2 | V1 preservation and V2 static migration contract |
| Existing exception tests | 4 | standardized safe error behavior |

## Security Negative Matrix

Covered: forged signature, wrong issuer, wrong audience, expired token,
premature token, missing subject, malformed bearer value, missing credential,
unknown/disabled mapping, caller actor headers, roles/scopes, missing grant,
authorization dependency failure, mapping dependency failure, arbitrary service
descriptor, SYSTEM descriptor, and sequential context leakage.

## Missing Runtime Coverage

The real MySQL test code exists and includes collation, constraint, FK,
EXPLAIN, provisioning idempotence/conflict, revoke/regrant, mapping
disable/reactivate, actor disable/reactivate, and optimistic stale-version
checks. It has not executed in this environment and therefore supplies no
runtime evidence yet.
