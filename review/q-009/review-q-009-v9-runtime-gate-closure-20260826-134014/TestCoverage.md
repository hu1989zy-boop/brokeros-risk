# Test Coverage

## Q-009 Security Coverage

- domain invariants and exact capability syntax;
- verified principal and ActorContext invariants;
- application authorization allow/deny/unavailable paths;
- explicit service identity and arbitrary SYSTEM rejection;
- issuer/audience/JWK configuration contracts;
- signed JWT validation and filter-chain behavior;
- spoofed header/role/scope rejection;
- unknown/disabled mapping and dependency failure behavior;
- safe 401/403/503 payloads and context cleanup;
- real MySQL 8.4 Flyway/constraint/query-plan behavior;
- mapping, capability, actor lifecycle and stale-version conflict;
- controlled provisioning idempotence/conflict; and
- application/configuration/correlation/error/Flyway regressions.

## Final Count

- Total: 58
- Passed: 58
- Failed: 0
- Errors: 0
- Skipped: 0

The mandatory MySQL test is included in the total and executed against MySQL
8.4.11. H2 and mocks were not substituted for database runtime evidence.
