# Outstanding items and assumptions

## Conditions retained for independent review

1. **Physical partitioning (pre-existing accepted Phase A condition).** V9 is
   non-partitioned to preserve global (server,seq) uniqueness. No migration or
   additive index is needed for this trigger change. Partitioning remains a
   separate architecture/data task; do not describe the current table as partitioned.
2. **Global sequence vs account partitions (pre-existing Phase A condition,
   now exposed by Kafka).** A server sequence distributed across account partitions
   has no global Kafka order, even with one consumer thread. Existing MAX(sequence)
   gaps can be false/incomplete with late arrivals or concurrent consumers. Tests
   serialize cross-account fixture delivery and verify same-account bursts.
   Resolve cursor ownership/reconciliation before concurrent production; no
   reliability rule or partition key was silently changed.
3. **Reconnect epoch wire contract (gateway integration prerequisite).** V2
   requires a new epoch on reconnect but supplies no encoding in the existing
   (server,seq) key/envelope. A same-server sequence reset can collide with old
   rows. No epoch field/marker type/fake identity was invented. The gateway-side
   architecture must settle durable uniqueness and discontinuity representation
   before live reconnect acceptance.
4. **Raw QUOTE account attribution (gateway contract prerequisite).** The approved
   envelope and Kafka key require an accountRef. A raw symbol quote without one is
   rejected visibly. This Java task does not invent a symbol key or synthetic account.
5. **Golden monetary completeness.** All 12 supplied records omit currency while
   carrying amounts. They are retained unchanged as opaque observations. The typed
   projection validates routing only and must not be treated as a complete financial
   validator. The gateway must supply authoritative currency before monetary use;
   independent review should confirm the projection matches the requested seam.
6. **Production operations.** Provision canonical/quarantine topics with approved
   security, size, retention and replication/min-ISR. Validate SASL/mTLS/ACLs and
   consumer lag/failure alerting. Unit config tests and an embedded broker are not
   production trust/topology evidence. Stopped listener status is not included in
   the existing HTTP health endpoint.
7. **At-least-once and quarantine recovery.** A post-DB-commit/pre-offset-commit
   crash replays into de-duplication. Quarantine can duplicate after its send ACK
   but before offset commit; identify by original broker coordinates. Retention
   must cover operator recovery. No atomic DB/Kafka transaction was introduced.
8. **Legacy HTTP opt-in.** The old authenticated test-aid path is default-off and
   retains payloadBase64 publication for Phase A compatibility. Such messages are
   not V2 canonical input and are quarantined by the new listener. Leave the aid
   off in production and isolate legacy tests.

## Authority reconciliation

- Parent §17 explicitly approves Kafka-direct transport, Kafka trust and raw ticks,
  revising old addendum/ADR-023 HTTP-first statements for Phase B. No governance
  source was edited; the parent revision is followed.
- The task says to reuse the Phase A partitioned store, but addendum §17 explicitly
  records its accepted non-partitioned fallback. Existing V9 is authoritative;
  maintaining it is required by this task, not an unreported schema deviation.
- The approved JSON payload and typed routing model coexist: parsing occurs only
  at the Kafka boundary; the service and database store original opaque bytes.
- The task explicitly requires a dead-letter path. Its implementation topic name
  follows the standing brokeros.risk.<domain>.<past-tense-event> convention. No
  topic is auto-created by this application and no business capability is added.

## Out of scope / next boundary

C++/Windows gateway, SDK work, MT5 gateway, native mapper verification, live demo
server reconnect, Flink, markout, position reconstruction, Evidence generation,
financial calculations, new schema, other requirements, production deployment and
Git commit/push remain outside this task. No unrelated fix was included.

Next step is Claude Code's independent review of the actual workspace and this
package. Implementation acceptance and subsequent gate advancement remain with
the Product Owner. This package does not self-approve those gates.
