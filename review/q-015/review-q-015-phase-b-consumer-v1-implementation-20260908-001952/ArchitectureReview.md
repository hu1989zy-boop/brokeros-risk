# Q-015 Phase B consumer implementation architecture review

Stage: Implementation + Implementation Verification, prepared for Claude Code's
independent review. This is an implementation-side compliance assessment, not
independent acceptance or authorization for Final Closure / Git.

## Authority and inspected scope

Read the engineering Execution Protocol, Architecture and Design Decision
Principles, root AGENTS.md, Q-015 parent and Phase A addendum, ADR-024, Phase B
Architecture V2 and Gateway Design V2, Phase A Architecture/Design and ADR-023.
Related accepted ADRs inspected: 001 (Java/monolith), 003/005 (foundation and
standards), 008 (configuration), 009 (domain boundaries). Inspected the existing
Q-015 service, envelope, JDBC store, publisher, HTTP controller, metrics, tests,
V9 SQL, bootstrap contract, Maven and configuration. New/changed source files
are listed and hashed in SourceManifest.sha256; TestInventory.txt identifies
executed tests. Existing Phase A evidence is background, not proof of this work.

Parent §17 Transport V2 is the explicit higher-authority revision to the old
Phase A HTTP-auth and publisher topology. It authorizes Kafka-native trust and
re-triggering the existing V9 store. The old addendum/ADR-023 transport language
is historical Phase A scope; no governance status was silently changed.

## Trace and design decisions

1. **Reuse the service without a publish loop.** The legacy authenticated method
   and new Kafka method share the identical local append/gap transaction.
   Kafka calls persist without publication because the event already exists on
   the input topic. A fake SERVICE actor or re-publication would obscure the new
   trust boundary or create a loop; both are rejected. The integration test's
   actor guard and publisher port deliberately throw if called by Kafka.
2. **Typed projection, opaque retention.** The ADR-024 routing projection contains
   kinds, lifecycle, optional string refs, provenance, UTC time, side/reason and
   raw codes. It does not claim to validate every financial payload field.
   Jackson is confined to the messaging boundary; the original payload object is
   sliced from UTF-8 input. JDBC/application code remains payload-opaque. No
   currency is fabricated for the provided records that lack it.
3. **Transaction-before-offset.** RECORD acknowledgement and synchronous commits
   follow completion of the local transaction. Consumption is synchronous with
   one thread and a 100-record pull bound. A post-commit crash may replay; the
   unchanged unique key prevents another row. No exactly-once guarantee is made.
4. **Visible quarantine and fail-stop.** The task explicitly requires malformed
   messages to reach a dead-letter/error path. The adapter publishes original
   bytes to brokeros.risk.tradingdata.ingestion-rejected with safe provenance
   headers; it acknowledges input only after confirmed publication. Database,
   quarantine or unexpected failures stop the listener, retaining its offset.
   No retry-topic topology, automatic offset reset, unbounded buffering or
   catch-all retry was added. A dead letter can be duplicated after a crash.
5. **Legacy HTTP test aid.** The existing route is disabled by default and marked
   deprecated/test-only. Explicit opt-in preserves its original API, guard,
   capability and opaque legacy wire contract; it is not a production canonical
   path. Test-only listener suppression lives in test resources, so every
   packaged deployment profile retains automatic Kafka ingestion.

ADR required for these implementation choices: **NO new ADR**. They implement
the already approved V2 consumer/error-path boundary and preserve the database,
capabilities, module layout and Phase A reliability rules. Production concurrency,
reconnect epoch encoding, financial completeness or schema partitioning would
require separately reviewed decisions; they were not implemented here.

## Development Standards Compliance

| Area | Inspected evidence and result |
| --- | --- |
| AGENTS.md | Stage/scope recorded; only Q-015 production Java plus its tests, Maven test dependency and supporting docs changed. No gateway, other domain module, Git staging/commit/push, SDK access, or historical package edits. |
| Architecture | Kafka listener invokes existing service; input is never re-published on the consumer path. One modular-monolith deployable; interfaces/application/domain/persistence dependencies remain within tradingdata. Core Domain tables are excluded by architecture tests. |
| ADR | ADR-024's neutral kind/side/reason/ref/time routing subset is implemented; ADR-023 payload opacity and unique key persist under parent §17's V2 transport revision. ADR-008 native configuration/security binding retained; no framework wrapper or dynamic configuration introduced. |
| API | HTTP remains ApiResponse + Bean Validation + SERVICE/capability guard + GlobalExceptionHandler when explicitly enabled; default absence and deprecation are deliberate V2 compatibility changes. Existing ResultCodes/capabilities/bootstrap remain unchanged. No application REST endpoint added. |
| Database | All migration bytes and JdbcTradingDataEventStore/TradingDataEnvelope are unchanged. V9 BLOB/65,535-byte limit and global unique key preserved. Event+gap rollback observed against MySQL 8.4.11. Actual V9 is the accepted non-partitioned fallback, not physical partitioning. No DDL/index migration added. |
| Security | Strict UTF-8/JSON/version/key/provenance/enum/coercion checks; bounded parsing; safe parser exceptions without input causes. Producer default record logging suppressed; listener emits fixed coordinates only. Native SASL/TLS config survives adapter construction in tests. Real broker ACL/mTLS verification remains an operational condition. |
| Auditability | Existing source, sequence, account, event/receive time and durable gaps retained. Quarantine carries original topic/partition/offset and a fixed rejection code; no caller headers or SQL/parser cause leaked. No Audit module or action execution introduced. |
| Skill | Applied repository development/configuration/CI guidance and brokeros-review-package skill. Reusable byte-retention, strict-coercion and offset/replay lessons added to development-standards.md; honest Q-015 lesson added. |

No newly introduced standards violation was identified in the inspected portable
scope. The routing projection deliberately leaves unvalidated monetary content
opaque; it must not be described as a complete financial model. Existing global
sequence, reconnect, physical partitioning and production trust limits remain
explicit in OutstandingItems.md; they prevent a claim of live end-to-end readiness.
