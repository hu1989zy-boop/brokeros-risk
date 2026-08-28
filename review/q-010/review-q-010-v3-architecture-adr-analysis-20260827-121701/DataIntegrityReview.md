# Q-010 Data and Integrity Review

## Durable Authority

Application-owned MySQL/InnoDB is selected as source of truth using the
existing Spring JDBC/local transaction and Flyway foundation. No migration or
SQL is created in V3.

## Required Future Integrity Categories

- separate internal `BIGINT id` and opaque business refs;
- unique TradingAccountRef;
- unique full authority-scope/namespace/external-key tuple;
- restrict-delete authority-scope relationship;
- immutable tuple/ref update contract;
- exact binary comparisons for all identity components;
- stable lifecycle checks and nonnegative optimistic versions;
- unique operation/idempotency identity and semantic fingerprint;
- append-only history related to immutable target/version; and
- indexes for ref validation, tuple resolution, CAS mutations, and ordered
  history.

The proposed model keeps one immutable tuple and TradingAccountRef in one
current-state authority row. It therefore does not create a generic alias
mapping model. Mutable lifecycle and version are separate from immutable
identity.

## Concurrency and Atomicity

- Database uniqueness is final authority for concurrent registration.
- Optimistic compare-and-set rejects stale lifecycle mutations.
- Exact replay uses operation ID plus semantic fingerprint.
- Current state, durable operation result, and history commit atomically.
- History failure or DB unavailability rolls back/fails closed.
- Corrupt/ambiguous state never selects a winner or auto-repairs.

## Collation and Runtime

Controlled codes/refs require ASCII binary comparison. ExternalAccountKey
requires exact UTF-8 byte semantics, with no case folding or lossy
normalization. Implementation Design must choose and verify the physical form
on disposable MySQL 8.4, including composite index size, constraints,
collations, query plans, Flyway restart/validation, and concurrency.

No Redis, Kafka, external DB, read replica, or cache is an authority.
