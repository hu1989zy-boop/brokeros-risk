# Q-010 Architecture Approval Record

## Decision Source

The Product Owner supplied the external Architect decision through the
authorized Q-010 V4 Approval Recording Prompt. This file records that decision;
it is not Codex self-approval.

## Decision

- Q-010 Requirement V1: **APPROVED**
- Q-010 Architecture V1: **APPROVED**
- ADR-012: **APPROVED FOR ACCEPTANCE RECORDING**
- Architect decision recorded: **2026-08-27**
- Implementation Design: **NOT STARTED**
- Implementation: **NOT STARTED**
- Git commit: **NOT YET ALLOWED BY THIS TASK**

## Approved Invariants Preserved

1. `TradingAccountRef` is a BrokerOS-owned opaque stable identity, never an
   MT4/MT5 login, CRM ID, vendor key, or database primary key.
2. External identity is exactly `AccountAuthorityScopeRef + SourceNamespace +
   ExternalAccountKey`; the full tuple prevents source/server/environment
   collisions.
3. The tuple-to-reference mapping is immutable, one-to-one, non-reassignable,
   non-reusable, and not physically deleted.
4. `ACTIVE`, `INACTIVE`, and `RETIRED` remain historically resolvable; only an
   active account inside an active scope is eligible for a new association.
5. Authoritative MySQL constraints and one local transaction enforce
   uniqueness, idempotency, current state, durable outcome, and immutable
   history.
6. Provisioning is controlled, attested, manifest-driven, and non-Web.
7. Q-009 `ActorContext` and exact capability authorization run before protected
   Q-010 lookup or mutation; authorization does not replace source attestation.
8. Q-008 sees only the bounded protected eligibility contract by
   `TradingAccountRef` and never raw external identity or persistence details.
9. Redis and Kafka are not identity authorities; no new dependency or external
   integration is approved.
10. History failure rolls back the corresponding mutation.

## Approval Boundary

Architecture approval does not approve Implementation Design or authorize
implementation. Q-008 remains blocked by runtime prerequisites and separate
authorization.
