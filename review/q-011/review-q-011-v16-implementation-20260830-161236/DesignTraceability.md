# Q-011 Design Traceability

## Functional requirements

| Requirement | Implementation evidence | Test evidence |
| --- | --- | --- |
| Q011-FR-001 | `EvidenceRef`; `evidence_record.evidence_ref` separate from BIGINT `id` and globally unique | `EvidenceDomainTests.evidenceRefAndOperationIdRequireCanonicalLowercaseUuidV4`; MySQL metadata/record constraint tests |
| Q011-FR-002 | `EvidenceRecordingService` calls unchanged `TradingAccountReferenceEligibilityService` and rejects only `NOT_RECOGNIZED` | `recordingUsesExactOrderOwnActorAndAcceptsRecognizedNotEligible`; `recordingRejectsOnlyNotRecognizedAndMapsQ010Unavailability`; real Q-010 integration tests |
| Q011-FR-003 | `ObservationText`, `EvidenceRecord`, `MANUAL`, trusted ActorRef, UTC Clock; no attachment type/storage | domain boundary tests; architecture forbidden-scope scan; durable record test |
| Q011-FR-004 | `EvidenceSource.MANUAL`; `ck_evidence_record_source` | domain/REST contract tests; `evidenceRecordChecksUniqueSupersessionAndSelfForeignKeysAreEnforced` |
| Q011-FR-005 | Record/Correct authorize then require `ActorType.HUMAN`; read services do not impose HUMAN | SERVICE replay-denial, SERVICE read-success, and real-grant security tests |
| Q011-FR-006 | No edit/delete use case or content update SQL; correction appends a new record | `moduleHasNoDeleteSqlBypassProviderOrRawContentLogging`; correction durability/query test |
| Q011-FR-007 | `EvidenceCorrectionService` copies target subject; `JdbcEvidenceMutationAdapter.correct` atomically inserts replacement/history and supersedes target | correction order/copy/not-found/status tests; durable correction; forced rollback |
| Q011-FR-008 | nullable-unique `supersedes_id`, target CAS, exact constraint classification | duplicate supersession constraint test; concurrent different-correction winner test |
| Q011-FR-009 | `EvidenceOperationId`, SHA-256 semantic fingerprint, unique operation ledger, replay/conflict handling | fingerprint vectors; exact/conflicting replay application tests; concurrent record/correction replay tests |
| Q011-FR-010 | `EvidenceProvenanceQueryService` + narrow `EvidenceProvenanceView`; separate `EvidenceDetailReadService` and REST detail route | reflective no-sensitive-field test; read authorization/SERVICE tests; REST route test |
| Q011-FR-011 | typed expected exceptions, exact MySQL constraint classifier, strict UTF-8 decoder, dependency failure mapping | denial/no-port tests; malformed UTF-8 fail-closed; unknown integrity/access-log failure tests |
| Q011-FR-012 | `AuthorizedMutationContext`; operation/history rows in the mutation transaction retain actor/capability/time/reason/statuses | durable consistency test; history-trigger rollback test; history constraint tests |
| Q011-FR-013 | no Kafka/Redis/blob/provider implementation or dependency change | architecture scan; dependency-tree/POM check; protected-boundary diff scan |
| Q011-FR-014 | `JdbcEvidenceAccessLogAdapter` uses non-read-only `REQUIRES_NEW`; detail service logs before return | log-before-return application test; access-log failure/no-content; isolation/concurrency test |

## Design §8.5 constraint-to-test mapping

### `evidence_record`

| Invariant | Corresponding test |
| --- | --- |
| BIGINT primary key | `metadataContainsEveryNamedPrimaryUniqueForeignKeyCheckAndIndex` |
| globally unique `evidence_ref` | metadata test + `evidenceRecordChecksUniqueSupersessionAndSelfForeignKeysAreEnforced` |
| canonical `ev-<UUIDv4>` | domain reference-format test + MySQL record rejection test |
| canonical recording ActorRef UUIDv4 | MySQL record rejection test |
| source only `MANUAL` | MySQL record rejection test |
| status only `ACTIVE`/`SUPERSEDED` | domain transition test + MySQL record rejection test |
| observation 1–4,000 bytes | domain content boundary test + MySQL 0/4001 rejection test |
| at most one correction per target | MySQL nullable-unique test + concurrent different-correction test |
| both self-FKs restrict dangling/delete | MySQL record FK/restrict test |

### `evidence_operation`

| Invariant | Corresponding test |
| --- | --- |
| primary key | metadata test |
| globally unique operation ID | metadata test + duplicate operation rejection/concurrent replay tests |
| canonical operation UUIDv4 | domain format test + MySQL invalid operation test |
| type only RECORD/CORRECT | MySQL enum rejection test |
| outcome only CREATED/CORRECTED | MySQL rejects UNCHANGED test |
| Evidence FK restrict | MySQL dangling/delete-restrict test |

### `evidence_operation_history`

| Invariant | Corresponding test |
| --- | --- |
| primary key | metadata test |
| one history per operation | metadata test + duplicate history rejection test |
| operation FK restrict | dangling/delete-restrict test |
| type only RECORD/CORRECT | MySQL history enum rejection test |
| history type matches parent | durable mutation test asserts operation/history types match |
| before status bidirectional, including CORRECT NULL rejection | MySQL RECORD non-null, CORRECT null/wrong-value rejection test |
| after status bidirectional | MySQL RECORD/CORRECT wrong-value rejection test |
| reason bidirectional and 1–1,000 bytes for CORRECT | MySQL RECORD non-null, CORRECT null/0/1001 rejection test |

### `evidence_access_log`

| Invariant | Corresponding test |
| --- | --- |
| primary key | metadata test |
| Evidence FK restrict | MySQL dangling access-log and parent-delete rejection test |

### Index/query-plan and rollout evidence

`approvedQueriesUseUniqueAndSecondaryIndexesWithoutFullScans` verifies the
EvidenceRef, subject, operation ID, and access-log paths. The V3→V4 migration
test proves exactly four empty Evidence tables, validate/restart/checksum
stability, and zero additional migrations on restart; the complete suite also
proves clean V1→V4.
