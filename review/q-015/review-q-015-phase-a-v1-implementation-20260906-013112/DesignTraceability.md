# Q-015 Phase A Design Traceability

| Requirement / design item | Production evidence | Test evidence | Result |
| --- | --- | --- | --- |
| A1 Q-009 authorization entry | `TradingDataIngestionController`, `TradingDataIngestionService`, `TradingDataCapabilities`, `q015-ingestion-bootstrap.json` | `TradingDataApplicationTests`, `TradingDataBootstrapContractTests`, `TradingDataRestContractTests` | PASS |
| A1 `SERVICE` only / default deny | Capability checked before service-type gate; `requireService` rejects every non-service actor | missing-capability and non-service cases in `TradingDataApplicationTests` | PASS |
| Provisional versioned opaque envelope | `TradingDataEnvelope`, request/command/response records; defensive byte copies; no payload parser | `TradingDataDomainTests`, `TradingDataRestContractTests`, architecture scan | PASS |
| A2 idempotency `(server, sequence)` | global V9 unique key; duplicate constraint classifier; duplicate no-publish result | application duplicate test; real-MySQL unique/endpoint tests | PASS |
| A2 gap signal | prior sequence lookup and durable `trading_data_ingestion_gap` marker; metric | application gap test; real-MySQL marker and publisher-rollback tests | PASS; actual resync is Phase B |
| A2 ordering | Kafka send key is `envelope.tradingAccountId()` | `KafkaTradingDataEventPublisherTests` | PASS |
| A2 backpressure | synchronous future wait bounded at five seconds; timeout→429; other authority failures→503 | publisher timeout/failure tests and application metric test | PASS; no live broker test |
| A2 persist/publish failure behavior | local `TransactionTemplate` encloses insert, gap marker, and publish call | real-MySQL publisher-failure rollback test | PASS with cross-resource atomicity condition |
| A3 full-retention event store | V9 event table contains only versioned metadata, timestamps, and opaque `BLOB`; no expiry | migration contract plus real-MySQL round trip | PASS |
| A3 historical lookup | index `(trading_account_id, occurred_at)` | real-MySQL range query and `EXPLAIN` key assertion | PASS |
| A3 partition target | cleared Design §5 fallback used: non-partitioned table preserves plain global unique key | `SHOW CREATE TABLE` asserts and documents no partition | PASS using authorized fallback; follow-up condition |
| A4 synthetic replay harness | application ports/adapters permit deterministic synthetic envelope flow | 24 Q-015 tests plus shared V9 assertion; 5 tests use real MySQL | PASS |
| A5 public-doc research | `docs/2026-09-06-mt4-mt5-neutrality-pre-sdk-notes.md` | source and boundary review | PASS |
| Kafka first use | `KafkaTradingDataEventPublisher`; exact topic `trading-data.canonical`; Base64 only as transport encoding | topic/key/opaque byte assertions via hand-written sender double | PASS without live broker |
| Observability | `MicrometerTradingDataMetrics` for outcome, denial, gap, backpressure, duration | `TradingDataMetricsTests`, application metric assertions | PASS |
| V9 migration discipline | V9 only; shared migration test and static gate updated from eight to nine | `FlywayMigrationTests`, `Q015MySqlTests`, `verify-static.sh` | PASS |
| Phase A exclusions | no gateway directory, SDK interface, native/canonical field, external DB, frontend, or older module edit | `TradingDataArchitectureTests` and source-boundary commands | PASS |

## Design choices requiring independent-review attention

1. The non-partitioned V1 choice follows the exact fallback in Implementation
   Design §5 and the cleared prompt. It is not claimed to meet the addendum's final
   partitioned target.
2. `BLOB` is the Design-authorized blob alternative and safely accommodates the
   exact 65,535-byte payload maximum without the inline-row pressure of
   `VARBINARY(65535)`.
3. Prior sequence is queried before insertion so the newly inserted row cannot mask
   its own gap. Gap markers remain historical signals; no resolved state was
   invented.
4. The synchronous local transaction is the smallest coherent failure handling in
   the authorized two-table design. It cannot provide atomic MySQL/Kafka commit.
5. Kafka tests use a narrow sender future, not Mockito, embedded Kafka, or a live
   broker. This directly verifies topic, key, bytes, timeout, and failure mapping.
