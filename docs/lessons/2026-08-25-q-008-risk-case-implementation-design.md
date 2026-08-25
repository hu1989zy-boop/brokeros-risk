# Q-008 Risk Case Foundation Implementation Design Lessons Learned

## What Was Designed

Q-008 Implementation Design translated the approved Risk Case architecture
into a concrete, still non-executable Phase 1 plan. It selected UUIDv4-backed
CaseNumber values, normalized immutable Resolution History, explicit lifecycle
operations, Spring JDBC persistence, optimistic concurrency, named API/use-case
contracts, and same-database case/audit transactions.

No Java, SQL, Flyway migration, DTO, controller, service, repository, entity,
test, Kafka event, Redis key, adapter, frontend, or deployment change was made.

## Why These Choices

UUIDv4 is already available in Java, contains no time/node/sequence signal, and
does not require a custom codec or dependency. The database still enforces a
unique business key and uses a separate `BIGINT` internal primary key.

Resolution uses an immutable header per cycle plus normalized reference
snapshots because one mutable resolution value would erase earlier outcomes.
The root keeps only current bounded state; append-only history is queried
separately so the aggregate does not grow without bound.

One root optimistic version orders all material case commands and prevents
same-case last-write-wins behavior. Audit remains independently owned but
shares the same Spring/MySQL transaction, so Audit failure rolls back the case
mutation without requiring Kafka, 2PC, Saga, or Event Sourcing.

## Alternatives Considered

- UUIDv7 and ULID were not selected because their time/order signal is not
  needed in an externally visible opaque case identifier.
- Snowflake and database sequences were rejected because they expose ordering
  or volume and require allocator coordination.
- JPA was not selected because the repository already uses Spring JDBC and the
  design needs no ORM dependency.
- A generic status-update API was rejected because named domain operations must
  own lifecycle legality.
- Loading complete history into RiskCase was rejected because history is
  unbounded and not required for every invariant.
- Kafka-only Audit was rejected because it cannot guarantee atomic durability
  with the case mutation.

## Problems Encountered

The repository has no implemented Evidence, Decision, Action, or Action-outcome
owning capability, even though Q-008 decision-driven and resolution commands
need authoritative reference validation. The design therefore defines read-only
ports and reports the missing real providers as an implementation blocker
instead of accepting unchecked strings or creating fake domain objects.

The repository also has no authenticated Actor/authorization provider. A
caller-supplied actor header would make access control and audit attribution
spoofable. The design reports this as a second implementation blocker and does
not invent IAM/RBAC.

No runtime, migration, or compilation problem occurred because executable work
was prohibited in this phase.

## Reusable Lessons

- Resolve implementation detail deferrals without reopening accepted domain
  ownership or inventing new architecture.
- Use one aggregate version as both optimistic-lock token and case-local history
  ordering key when every material command updates the root.
- Separate bounded current state from append-only query history; aggregate
  ownership does not require loading every owned record for every command.
- Treat missing authoritative reference and actor providers as blockers, not as
  permission to trust opaque caller strings.
- A self-contained Design Review should include the approved Requirement,
  Accepted ADR, formal Design, Review evidence, and phase Lessons Learned.

## Future Risks

- Implementers may bypass reference providers to make decision-driven flows
  appear complete.
- HTTP endpoints may be exposed before a trustworthy ActorContext exists.
- A persistence implementation may update history rows or use wall-clock order
  instead of case version.
- Audit might be moved to asynchronous publication and lose atomicity.
- A generic workflow, association, entity, or permissions framework may be
  introduced despite the bounded Q-008 scope.
