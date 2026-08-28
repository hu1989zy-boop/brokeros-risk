# ADR-012: Trading Account Reference Authority Foundation

- Status: **Accepted**
- Date: 2026-08-27
- Architect approval date: 2026-08-27
- Approval origin: Explicit external Architect Review decision
- Requirement: Q-010 — Trading Account Reference Authority Foundation
- Architecture: Approved Q-010 Architecture V1 / Approval Review V4
- Depends on: ADR-002, ADR-009, ADR-010, ADR-011
- Supersedes: None

This ADR was accepted from the explicit external Architect Review decision
recorded on 2026-08-27. Acceptance is not Codex self-approval. The external
Architect subsequently approved Q-010 Implementation Design V1 on 2026-08-27;
Q-010 V6 records that separate decision. The later explicit V7 instruction
authorized implementation of that exact approved design. The external
Architect approved the exact V7 implementation on 2026-08-27, and Q-010 V8
records that decision plus the verification-backed Final Closure on 2026-08-28.

## Context

Q-008 requires an authoritative `TRADING_ACCOUNT` subject reference, but the
repository has no BrokerOS Trading Account identity or mapping authority. A raw
MT4/MT5 login, CRM ID, vendor database key, or caller string has no stable
BrokerOS meaning and can collide across brokers, source instances, servers,
and environments or be reassigned externally.

Q-010 Requirement V1 approves an exact scoped identity tuple, one-to-one
immutable cardinality, historical resolution, controlled non-web registration,
Q-009 protection, and atomic mutation provenance. Architecture must decide who
owns that authority, how identity is represented and compared, where durable
truth lives, and how Q-008 consumes it without creating Trading Account master
data or vendor coupling.

## Decision

### Bounded capability and identity ownership

Create a logical Trading Account Reference Authority capability inside the
existing Phase 1 modular monolith. It owns stable TradingAccountRef identity,
a minimal AccountAuthorityScopeRef registry, the immutable scoped external
identity association, lifecycle, protected registration/resolution, and
append-only authority-operation history.

It owns no customer/account master data, trading data, risk reasoning,
execution, broker/tenant directory, vendor payload, or external adapter.

### Opaque references

Use server-generated prefixed canonical lowercase UUIDv4 business references:

- `ta-<uuidv4>` for TradingAccountRef;
- `aas-<uuidv4>` for AccountAuthorityScopeRef.

They are separate from internal `BIGINT id`, globally safe for independent
deployments, immutable, non-sequential, and contain no broker/source semantics.

### Authoritative identity and comparison

The external identity is exactly AccountAuthorityScopeRef plus an immutable
structured SourceNamespace plus ExternalAccountKey.

SourceNamespace contains governed source-family, source-instance, server, and
environment codes. Controlled codes use exact lowercase ASCII/binary equality.
ExternalAccountKey is a bounded exact UTF-8 string compared by canonical bytes;
generic trim, numeric conversion, case folding, and Unicode normalization are
prohibited. Leading zeros and exact case are preserved.

The complete tuple and TradingAccountRef are immutable and uniquely constrained
in both directions. There are no aliases, merges, reassignment, or physical
deletion.

### Lifecycle

Use ACTIVE, INACTIVE, and RETIRED semantics. All remain historically
resolvable. Only an active account in an active authority scope is eligible for
new associations. Controlled reactivation restores only the same inactive
mapping; RETIRED is terminal.

### Registration authority and security

Registration and lifecycle changes occur only through a controlled non-web
application command. A purpose-specific, pre-provisioned Q-009 SERVICE actor
obtains a fresh ActorContext through the registered service boundary and must
receive the exact Q-010 capability. The manifest carries a complete canonical
identity, operation/idempotency ID, bounded reason, and broker/source-owner-
approved attestation reference.

Authorization permits the operation but does not prove the mapping. The
separate deployment-approved attestation is required. HTTP/public registration,
auto-enrollment, direct external database discovery, caller ActorRef, and a
generic SYSTEM bypass are prohibited.

All protected reads/mutations authorize before Q-010 data access using exactly:

- `trading-account-reference:read`;
- `trading-account-reference:register`; and
- `trading-account-reference:change-lifecycle`.

### Durable authority and atomic history

Application-owned MySQL/InnoDB is the durable source of truth, accessed through
the existing Spring JDBC/local transaction and changed only through a future
additive Flyway migration. Current scope/reference state, durable idempotency
outcome, and immutable mutation history commit in one local transaction.
History failure rolls back state.

Database constraints must enforce unique TradingAccountRef, unique complete
tuple, scope referential integrity, allowed lifecycle/version values, unique
operation identity, exact binary comparison, and delete restriction.

Redis, Kafka, event sourcing, a second database, and read-cache authority are
not selected. No new framework or dependency is required.

### Consumer boundary

Q-008 may call only a protected read-only application contract that validates a
TradingAccountRef for a new association and returns bounded recognized/
eligibility plus opaque authority version/provenance. It cannot see external
account keys, source details, persistence IDs, customer data, or vendor DTOs.

A separate protected internal tuple-resolution contract may support Q-010
duplicate handling and a future approved adapter. It is not public, not
available to Q-008, and never auto-registers.

## Alternatives Considered

### Raw MT4/MT5/CRM account ID as BrokerOS identity

Rejected. It is vendor-coupled, can collide across server/environment, can be
reassigned, and exposes external persistence as domain authority.

### Direct external-system lookup as runtime authority

Rejected. It couples availability and semantics to an independently owned
system, encourages direct database access, cannot guarantee immutable
historical meaning, and prevents atomic Q-010 history/idempotency.

### Full Trading Account master-data module

Rejected. Customer, broker, balances, positions, leverage, KYC, and trading
state exceed Q-010 and would duplicate external systems before their contracts
exist.

### Auto-registration on first observation

Rejected. Observation proves neither source-owner attestation nor registration
authorization, makes adapters confused deputies, and turns attacker input into
durable identity.

### Public/admin HTTP provisioning

Rejected for the Foundation. It creates an online administration/security/
audit surface without a Requirement. Controlled non-web provisioning is the
smallest accountable boundary.

### Alias/migration-capable mapping

Rejected under Q-010. It needs merge authority, conflict resolution, account-
number reuse policy, and cross-source history that the approved one-to-one
Requirement explicitly excludes.

### Database ID as business reference

Rejected because it leaks persistence/order, is not globally safe, and makes
schema identity the domain contract. Independent prefixed UUIDv4 is selected.

### Cache/event-only authority

Rejected. Redis and Kafka cannot atomically enforce bidirectional uniqueness,
idempotency, current lifecycle, and required immutable history. MySQL is
selected.

### Mutable external mapping

Rejected. Updating the tuple silently changes historical meaning. Immutable
identity plus mutable lifecycle preserves explainability and safe deactivation.

### Separate microservice/database

Rejected. Phase 1 requires a modular monolith, and one application database
transaction supplies the required integrity without distributed consistency.

## Consequences

### Positive

- BrokerOS receives stable broker/vendor-neutral Trading Account references.
- Server/environment collisions and external identifier reuse cannot silently
  change historical meaning.
- Q-008 obtains a narrow provider without owning upstream identity.
- Q-009 authorization and service identities are reused without a new IAM
  model or SYSTEM bypass.
- MySQL uniqueness, optimistic concurrency, idempotency, and atomic history are
  testable on the existing runtime stack.
- No new framework, cache, messaging topology, service, or external dependency
  is introduced.

### Costs and constraints

- Deployments must govern authority scopes, namespace codes, source-owner
  attestation, manifests, and purpose-specific service grants.
- Exact binary comparison requires strict canonical input and MySQL collation/
  index verification.
- One-to-one immutable mapping deliberately rejects source migrations and
  aliases until a later Requirement.
- MySQL unavailability stops registration, mutation, and authoritative reads;
  there is no stale fallback.
- Append-only operation history grows and requires future retention analysis.

## Security Implications

- Authorization occurs before lookup, limiting existence disclosure.
- Registration requires both a trusted Q-009 actor/grant and independent
  broker/source-owner attestation.
- Caller actor fields, roles, claims, source fields, raw account keys, and
  external IDs cannot bypass the authority.
- Full external keys, credentials, tokens, vendor payloads, and principal
  identifiers are not logged or persisted in history beyond the required
  canonical identity store.
- Ambiguous/corrupt state and security/database unavailability fail closed.

## Data and Integrity Implications

- MySQL/Flyway owns additive application schema only; no external database is
  read or modified.
- Identity components use exact binary equality and two-direction uniqueness.
- Internal BIGINT primary keys remain separate from opaque business refs.
- Identity is immutable; lifecycle uses stable codes and optimistic versions.
- State, idempotency outcome, and history share one transaction; no delete or
  cascade delete is permitted.
- Real disposable MySQL 8.4 verification is mandatory before implementation
  completion can be claimed.

## Operational Implications

- Provisioning is explicit, non-web, deployment-controlled, and manifest-
  driven.
- The Q-010 SERVICE identity and direct capabilities must be pre-provisioned in
  Q-009 before the command can run.
- Operators retry uncertain outcomes only with the same operation ID and
  semantic manifest.
- Safe logs report event/outcome and BrokerOS refs only; external keys are not
  printed.
- Corruption requires separately controlled diagnosis/repair; runtime never
  chooses a winner.

## Dependencies

Q-010 reuses Java 21, Spring Boot, Spring JDBC, the local transaction manager,
MySQL, Flyway, and the implemented Q-009 ActorContext/service actor/
authorization contracts. It depends on ADR-002 isolation, ADR-009 domain
ownership, ADR-010 Q-008 consumer boundaries, and ADR-011 trust semantics.

No new Maven dependency, framework, vendor SDK, service, database, Kafka topic,
Redis key, deployment object, or external integration is required.

## Deferred Decisions

Implementation Design will define exact Java/SQL/manifest/CLI/transaction/
exception/query/test mechanics without reopening this decision.

A future approved Requirement is required for aliases/merges/migration,
multiple external keys, automatic discovery/synchronization, source adapters,
online administration, master data, cache/events, cryptographic attestation,
retention/redaction, or cross-deployment federation.

## Approval Boundary

ADR-012 is **Accepted** through the explicit external Architect Review decision
dated 2026-08-27. Q-010 Architecture V1 is approved. Implementation Design V1
is **APPROVED — EXTERNAL ARCHITECT — 2026-08-27**. Q-010 V6 records that
approval. The explicit V7 instruction authorized implementation of that exact
baseline, and the external Architect approved the exact V7 implementation on
2026-08-27. Q-010 V8 records Verification PASS and Final Closure PASS / CLOSED
on 2026-08-28. Ready for Git Commit is YES as a closure assessment only; no
commit or push was performed, and Q-008 implementation remains separate.
