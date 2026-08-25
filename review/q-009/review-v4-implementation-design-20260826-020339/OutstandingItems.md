# Q-009 Implementation Design Outstanding Items

## Blocking Governance Item

- Architect review and explicit Q-009 implementation authorization are still
  required.

Implementation Authorized: **NO**

## Open Deployment Inputs

- Concrete identity-provider vendor.
- Environment issuer, audience, and optional explicit JWK-set locations.
- Approved first human/service principal mappings and capability grants in each
  deployment bootstrap manifest.

These are required before runtime rollout but do not prevent the foundation
design from Architect review or later implementation.

## Deferred Capabilities

- External/distributed service JWT, mTLS, or workload identity.
- Online actor/policy administration and its Audit/approval controls.
- Roles/groups-to-capability mapping, resource-scoped/ABAC policy, delegation,
  impersonation, and break-glass.
- Security caching, metrics/alerting, SIEM, or Kafka events.
- Q-008 exact final capability-to-use-case integration and all Q-008 upstream
  provider Requirements.

## Known Verification Limitation

No proposed Q-009 runtime class, dependency, filter, decoder, table, migration,
or test exists in this design-only phase. The current 26-test backend suite
proves only that the unchanged approved baseline still passes; it does not
prove Q-009 behavior.

## Unresolved Design Blockers

None. Human/service runtimes, mapping/authorization persistence, provisioning,
dependencies, migration, failure, concurrency, and tests are decided.
