# Q-015 Phase B portable consumer implementation lessons

Scope: the approved Kafka-direct Java consumer, canonical routing projection,
unchanged V9 storage/reliability, replay tests and review handoff. No gateway,
SDK, schema, capability or other business-module implementation belongs here.

## What worked

- Extracting a shared local persistence operation from the existing service made
  the Kafka trigger reuse de-duplication, event/gap transaction and metrics while
  avoiding publication back to its input topic. Existing HTTP authorization and
  Phase A tests remain valid for the disabled-by-default legacy test aid.
- Slicing the original nested payload object avoids a subtle loss of fidelity
  from JSON tree/DTO reserialization. Recorded golden payloads, long decimal
  lexemes, Unicode and whitespace are compared as bytes.
- Tests using an embedded broker and a disposable MySQL 8.4.11 instance observe
  committed consumer-group offsets, not merely listener calls. A failing gap
  trigger demonstrates that both the event and gap roll back; a failed quarantine
  send demonstrates that input offsets remain replayable. Restarting the listener
  then proves recovery in each case.
- Native Spring Kafka security properties are copied into module factories;
  safe acknowledgement and serialization settings are fixed at the adapter.
  Disabling automatic listener startup only on the **test classpath** preserves
  ingestion in deployed test environments as well as production.

## Problems encountered and repairs

- Disabling Jackson's general scalar coercion did not prevent a numeric account
  reference from becoming a string. A negative contract test caught it. Explicit
  textual coercion rules now reject integer, float and boolean inputs; float-to-int
  coercion is separately disabled. Tests were retained, not relaxed.
- The first Docker focused run failed to compile because a raw factory reference
  made `createContainer` expose the abstract container type. The test now inspects
  the concrete concurrent container with wildcard generic types.
- Review identified JSON encoding auto-detection as an unnecessary ambiguity for
  the UTF-8 wire boundary. Strict UTF-8 decoding and raw-NUL rejection exclude
  UTF-16/32 messages before byte offsets are used to extract the payload.
- Maven `clean` could remove generated files but could not remove the target bind
  mount under a read-only repository mount. The final `package` run rebuilt the
  cleared output and reran all tests without making source mounts writable.
- The provided golden fixture contains monetary observations without currency.
  No currency was fabricated and no source fixture was rewritten. The typed model
  is explicitly a routing projection; financial interpretation remains disallowed
  until authoritative currency is supplied and checked downstream.

## Limits to keep visible

- The actual V9 table is the accepted non-partitioned fallback, not a physically
  partitioned store. The global source-sequence unique key remains intact.
- Per-account Kafka order does not impose order on a server's sequence distributed
  across multiple account partitions. One consumer thread does not solve this.
  Existing Phase A global-sequence/concurrency limits still require an architecture
  decision before concurrent live production.
- Gateway reconnect epochs and account attribution for raw quotes lack a complete
  wire contract. The portable consumer must not invent new identity fields, fake
  accounts, event kinds or idempotency keys to conceal that gap.
- Kafka offsets and MySQL commits are not atomic. Database duplicates and
  quarantine duplicates remain possible; input replay is at-least-once. A stopped
  consumer is visible through failure counters/logs and lag but does not change
  the platform's existing HTTP health endpoint.
- Production SASL/mTLS, ACLs, replication, retention, gateway translation, real
  reconnect and a Windows live slice were not verified by this Java task.

## Reusable guidance

`docs/skills/development-standards.md` now records original-byte JSON retention,
strict coercion, consume-without-republish, commit-before-ack and failure/replay
verification. These patterns are reusable; the Q-015 sequence limitations remain
in this lesson and the operations/review documents instead of becoming a generic
business rule. Test counts and exact executed commands are recorded in the new
Phase B consumer review package's `Verification.md`.
