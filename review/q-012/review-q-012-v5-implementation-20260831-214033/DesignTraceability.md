# Q-012 Design Traceability

## Functional Requirements

| Requirement | Implementation | Primary proving tests | Result |
| --- | --- | --- | --- |
| Q012-FR-001 | `DecisionRecordingService`, `RecordDecisionSpec`, `JdbcDecisionMutationAdapter`, V5 | `recordingUsesCanonicalOrderOwnActorDeduplicatesAndAcceptsRecognizedNotEligibleAndSuperseded`; `recordReplayQueryAndAccessLogAreDurableAndConsistent` | PASS |
| Q012-FR-002 | `DecisionCapabilities.RECORD`, `AuthorizationGuard`, `requireHuman` | `serviceActorIsRejectedBeforeReplayCheckAndExternalCalls`; `realDecisionRecordDenialPreventsAllDecisionDataAccess`; `serviceActorRemainsRejectedDespiteAllRealRecordingGrants` | PASS |
| Q012-FR-003 | unchanged `TradingAccountReferenceEligibilityService` consumption | `recordingUsesCanonicalOrderOwnActorDeduplicatesAndAcceptsRecognizedNotEligibleAndSuperseded`; `subjectRejectsOnlyNotRecognizedAndMapsAuthorityUnavailability`; real Q-010 grant/denial tests | PASS |
| Q012-FR-004 | unchanged `EvidenceProvenanceQueryService` consumption; distinct sorted `EvidenceRef` set | canonical-order/dedup test; `evidenceRejectsOnlyNotFoundAndMapsAuthorityUnavailability`; empty-set test; real Q-011 grant/denial tests | PASS |
| Q012-FR-005 | `DecisionFingerprintFactory`, `decision_operation`, replay paths in service/adapter | golden-vector test; exact/changed replay test; `concurrentSameOperationReturnsOneCommitAndOneReplay` | PASS |
| Q012-FR-006 | record-only application/port/controller and immutable schema | `moduleHasNoDeleteCorrectionSupersessionOrRawContentLogging`; controller route inventory | PASS |
| Q012-FR-007 | `DecisionProvenanceQueryService`, `DecisionProvenanceView` | `narrowProvenanceContractCannotExposeConclusionText`; authorized service narrow read tests | PASS |
| Q012-FR-008 | `DecisionDetailReadService`, `JdbcDecisionAccessLogAdapter` | audit-before-disclosure application test; real authorized service detail read; failed-audit concurrency test | PASS |
| Q012-FR-009 | no provenance controller; only record/detail controller methods | `controllerExposesOnlyApprovedDecisionRoutes` | PASS |

## Implementation Design §8.5 constraint-to-test mapping

### `decision_record`

| §8.5 invariant | Corresponding real-MySQL test | Result |
| --- | --- | --- |
| `id` primary key | `metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex` | PASS |
| `decision_ref` globally unique | `decisionRecordPrimaryUniqueReferenceSubjectActorSourceAndConclusionChecksAreEnforced` | PASS |
| canonical `dec-<UUIDv4>` | same enforcement test plus domain canonical-form test | PASS |
| canonical actor UUIDv4 | same enforcement test | PASS |
| source exactly `MANUAL` | same enforcement test | PASS |
| conclusion 1–4,000 bytes | same enforcement test plus domain UTF-8 boundaries | PASS |

The approved DDL also contains the explicit `subject_ref` canonical-shape check
`ck_decision_record_subject_ref`; it is verified by
`decisionRecordPrimaryUniqueReferenceSubjectActorSourceAndConclusionChecksAreEnforced`
and included in metadata assertions even though §8.5 omitted a separate row for
it.

### `decision_evidence_reference`

| §8.5 invariant | Corresponding real-MySQL test | Result |
| --- | --- | --- |
| `id` primary key | metadata test | PASS |
| `decision_id` existing Decision + delete restrict | `evidenceReferencePrimaryForeignKeyUniquePairAndCanonicalShapeAreEnforced` | PASS |
| unique `(decision_id, evidence_ref)` | same enforcement test; application dedup test | PASS |
| canonical `ev-<UUIDv4>` | same enforcement test | PASS |

### `decision_operation`

| §8.5 invariant | Corresponding real-MySQL test | Result |
| --- | --- | --- |
| `id` primary key | metadata test | PASS |
| globally unique `operation_id` | `operationPrimaryUniqueIdSingleValueEnumsAndRecordForeignKeyAreEnforced` | PASS |
| canonical UUIDv4 operation id | same enforcement test plus domain test | PASS |
| operation type exactly `RECORD` | same enforcement test | PASS |
| outcome exactly `CREATED` | same enforcement test | PASS |
| `decision_id` existing Decision + delete restrict | same enforcement test | PASS |

### `decision_access_log`

| §8.5 invariant | Corresponding real-MySQL test | Result |
| --- | --- | --- |
| `id` primary key | metadata test | PASS |
| entry cannot outlive its Decision | `accessLogPrimaryAndRecordForeignKeyRestrictAreEnforced` | PASS |

## Other mandatory design tests

- Upgrade, exactly four tables, no seed, validate/restart/checksum:
  `migrationUpgradesV4CreatesExactlyFourTablesWithoutSeedsAndValidatesOnRestart`.
- Indexed query plans/no full scan:
  `approvedQueriesUseUniqueAndSecondaryIndexesWithoutFullScans`.
- Generated-reference collision bounded to three attempts:
  `generatedRefCollisionRetriesExactlyThreeTimesAndNeverOverwrites`.
- Atomic forced rollback:
  `evidenceReferenceFailureRollsBackDecisionAndOperationAtomically`.
- Concurrent same-operation replay:
  `concurrentSameOperationReturnsOneCommitAndOneReplay`.
- Failed access audit isolated from unrelated recording:
  `failedAccessLogIsIsolatedFromConcurrentUnrelatedRecording`.
- Malformed stored UTF-8 fails closed:
  `malformedStoredUtf8FailsClosedInsteadOfReturningReplacementContent`.

## Traceability limitation

All Q-012-specific traceability rows pass. Requirement AC11 additionally
requires the full unchanged Q-009/Q-010/Q-011/Q-012 database gate. That gate is
blocked by one unchanged Q-011 migration-count assertion; see
`Verification.md` and `OutstandingItems.md`.
