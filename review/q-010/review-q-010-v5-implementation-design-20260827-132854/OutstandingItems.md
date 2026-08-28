# Q-010 V5 Outstanding Items

## Blocking Governance Item

- Independent Architect review of Implementation Design V1 is required.
- Implementation Design Approved: **NO**
- Implementation Allowed: **NO**

No implementation may begin from this package alone.

## Deployment Inputs, Not Design Gaps

- actual deployment authority scopes;
- real broker/source-owner-approved attestation records/references;
- target manifests, reasons, and change tickets;
- the target Q-009 SERVICE ActorRef/mapping and exact grants;
- target database credentials, command access control, and change window.

These values are deliberately not invented, defaulted, or committed. They are
needed before rollout, but their absence does not prevent Architect review of
the bounded design.

## Future Requirements

- alias, merge, split, reassignment, external identity migration, or multiple
  keys per TradingAccountRef;
- automated discovery/synchronization and MT4/MT5/CRM/vendor adapters;
- public/online administration, bulk operations, or master data;
- Redis/Kafka projections, cache/events, cross-deployment federation;
- cryptographic attestation protocol and verifier;
- retention/redaction/legal hold and general Audit administration; and
- direct identity repair or destructive removal.

## Q-008 Prerequisites

Q-010 completion would satisfy only the authoritative TRADING_ACCOUNT subject
reference provider. Evidence, Decision, Action, and ActionOutcome providers
and Q-008 implementation authorization remain separate prerequisites. Q-008
implementation did not start in V5.

## Verification Limitation

No Q-010 production code, migration, command, table, or test exists. The future
implementation must pass the complete Maven, mandatory MySQL 8.4.11/Flyway,
authorization, transaction/concurrency, disclosure, logging, Compose,
Kustomize, static, and Review gates with no mandatory skip.

## Unresolved Design Blockers

None. The remaining blocking item is governance approval, not an unresolved
architecture decision.
