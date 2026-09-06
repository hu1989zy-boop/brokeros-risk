# Q-015 Phase A Outstanding Items

## Blocking items for this implementation package

None found in the authorized Phase A implementation and verification scope.

## Required next lifecycle action

Claude Code's independent implementation review and the Product Owner's explicit
Gate Decision remain pending. This package deliberately does not self-accept the
implementation, update the Requirement gate, or authorize Phase B.

## PASS WITH CONDITIONS items

1. **Time partitioning target.** V1 is non-partitioned to keep the plain global
   `(source_server_id, source_sequence)` unique key. Independent review should
   confirm use of the cleared Design §5 fallback. A future approved design must
   reconcile partition pruning with global idempotency before claiming the
   addendum's partitioned target.
2. **MySQL/Kafka atomicity.** The local transaction rolls back event/gap rows when
   publish fails, but cannot atomically commit Kafka and MySQL. Decide an outbox,
   transactional producer, reconciliation, or explicit at-least-once contract in a
   future authorized architecture change; do not infer exactly-once delivery from
   Phase A.
3. **Concurrent sequence ownership.** V1 reads the highest stored source sequence
   without a per-server lock/cursor. Define same-server concurrency, late-arrival,
   gap overlap, and gap-resolution semantics before concurrent production ingress.
4. **Kafka deployment validation.** Provision and verify topic partitions,
   replication/min-ISR, producer idempotence/acks, ACL/TLS, maximum message size,
   timeout policy, and real broker backpressure in the deployment environment.

## Phase B remains parked

Do not begin the canonical field model/ADR, Manager SDK interface, MT4/MT5 gateways,
native adapters, native-to-canonical translation, markout tick-window capture, or
x64 Windows integration until the real SDK and Windows prerequisite arrive and the
required Product Owner/Architect gates are passed. Actual snapshot/resync belongs
there.

## Open SDK research questions

The A5 note lists the exact questions to validate against the real SDK, including
identity/lifecycle differences, event ordering/sequence sources, snapshots,
timestamps/time zones, numeric precision, reconnect/backfill behavior, and
netting/hedging semantics. Public terminal-language documentation is not accepted as
Manager SDK proof.

## Assumptions made in Phase A

- `sourceSequence` is non-negative and monotonically meaningful within one
  `sourceServerId`; equality identifies a re-delivery regardless of payload.
- Operational producers serialize a single server's Phase A calls until a stronger
  concurrency contract is approved.
- The envelope payload maximum is 65,535 bytes; Kafka/environment message limits
  will be configured above the encoded envelope size before production use.
- Five seconds is the V1 bounded send wait. It can be made configurable only under
  an approved operational requirement/design.
- Full retention means no TTL or deletion job in this stage.

## Environment advisories

- Flyway reports MySQL 8.4 above its tested ceiling of 8.1, although all V1-to-V9
  tests passed.
- Docker default-bridge links are deprecated; a custom network is preferable for a
  durable CI harness.

## Repository state note

Five sibling ZIP files for Q-016 through Q-020 already existed untracked before
Q-015 work. They were preserved untouched and are excluded from this package.
