# Q-010 V7 Implementation Design Traceability

| Design section | Implementation | Tests / evidence |
| --- | --- | --- |
| 4–5 module/domain model | `tradingaccount` domain/application/infrastructure/interfaces packages and typed value objects | Domain and architecture tests |
| 6 application use cases | four mutation services, external resolution, eligibility service | Application authorization/order and facade tests |
| 7 ports/generators | bounded query/mutation ports and deterministic production/test generators | Fake-port tests and generated collision tests |
| 8 persistence | Flyway V3 four-table model and JDBC adapters | Migration static test; MySQL shape/collation/constraint tests |
| 9 canonicalization | exact regex/code-point/UTF-8 validation without normalization | Domain boundary tests |
| 10 manifest | typed envelope/field matrix, strict Jackson, 64-KiB/file/symlink controls | Bootstrap unit and real command tests |
| 11 non-Web boundary | `TradingAccountAuthorityBootstrapCommand` and service descriptor | Real command success/replay/revocation-denial test |
| 12 fingerprint/idempotency | `ManifestFingerprintFactory`, operation table replay/conflict | golden/sensitivity, conflict, duplicate delivery tests |
| 13 transaction/concurrency | TransactionTemplate, named constraint classifier, CAS | same-identity race, operation race, collision, CAS, rollback tests |
| 14 Q-008 facade | `TradingAccountReferenceEligibilityService` | active/ineligible/not-found/unauthorized/bounded output tests |
| 15 ResultCodes/errors | 15 approved Q-010 ResultCodes and bounded BusinessException types | command exit/result and persistence conflict assertions |
| 16 observability | redacted key, safe command report, low-cardinality Micrometer counters/timers | output/redaction/source tests; dependency check |
| 17 security | exact Q-009 capabilities, descriptor registry, authorization-first services | zero-port denial and real authoritative service actor tests |
| 18 test design | unit/application/static/MySQL/concurrency/command suite | 27 Q-010 tests; all 11 MySQL tests executed |

No material design deviation or silent omission was identified.
