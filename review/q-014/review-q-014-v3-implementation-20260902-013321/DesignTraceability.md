# Q-014 Design Traceability

## Functional requirements

| Requirement | Implementation evidence | Test evidence | Result |
| --- | --- | --- | --- |
| Q014-FR-001 | `RecordActionOutcomeCommand`, `ActionOutcomeRecordingService`, `ActionOutcomeRecord` | `recordingUsesCanonicalOrderAndPassesOwnActorToActionAuthority`; MySQL durable-record tests | PASS |
| Q014-FR-002 | Q-009 `AuthorizationGuard`, `ActionOutcomeCapabilities.RECORD`, HUMAN check | application authorization/order tests; `Q014SecurityMySqlIntegrationTests` | PASS |
| Q014-FR-003 | `ActionOutcomeProvenanceQueryService` delegates recognition to Q-013 using caller `ActorContext` | unrecognized-before-write and real Q-013 grant/denial tests | PASS |
| Q014-FR-004 | raw `ActionOutcomeFingerprintFactory`, operation lookup, conflict handling, atomic ledger | exact replay/no-second-Q013-call, changed replay, rollback, concurrency tests | PASS |
| Q014-FR-005 | immutable domain record; no update/delete application port, route, or SQL | domain and architecture forbidden-behavior tests | PASS |
| Q014-FR-006 | `ActionOutcomeProvenanceView` and query service omit outcome text | structural narrow-contract architecture test; authorized SERVICE read test | PASS |
| Q014-FR-007 | `ActionOutcomeDetailReadService`, full-detail endpoint, `REQUIRES_NEW` access-log adapter | read authorization/audit ordering, audit-failure nondisclosure, real-MySQL access-log tests | PASS |
| Q014-FR-008 | free-text `OutcomeText`; no execution adapter/status/taxonomy/Q-008 consumer | architecture and repository static checks | PASS |

## Implementation Design §8.4 constraint matrix

| Table / invariant | Proving test | Result |
| --- | --- | --- |
| record `id` primary key | `metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex` | PASS |
| record `action_outcome_ref` globally unique | metadata test plus `recordPrimaryUniqueReferenceActionActorSourceAndOutcomeTextChecksAreEnforced` | PASS |
| record canonical `aoc-<UUIDv4>` | domain ref test and record constraint test | PASS |
| record canonical `act-<UUIDv4>` | domain ref test and record constraint test | PASS |
| record canonical actor UUIDv4 | record constraint test | PASS |
| record source exactly `MANUAL` | domain enum/immutability test and record constraint test | PASS |
| record outcome text 1–4,000 UTF-8 bytes | `outcomeTextUsesUtf8ByteBoundsAndRejectsBlankNulAndControls`; record constraint test | PASS |
| operation `id` primary key | metadata test | PASS |
| operation `operation_id` globally unique | metadata test and operation constraint test | PASS |
| operation canonical UUIDv4 | domain operation-id test and operation constraint test | PASS |
| operation type exactly `RECORD` | operation constraint test | PASS |
| operation outcome exactly `CREATED` | operation constraint test | PASS |
| operation references existing record, delete restricted | operation constraint test and FK metadata test | PASS |
| access-log `id` primary key | metadata test | PASS |
| access rows cannot outlive record, delete restricted | `accessLogPrimaryAndRecordForeignKeyRestrictAreEnforced` | PASS |

## Additional approved design proofs

- Multiple outcomes for one action: `sameActionRefCanHaveMultipleOutcomeRows` and `sameActionRefCanBeRecordedTwiceWithDifferentOutcomeFacts`.
- Exact replay does not repeat the Q-013 call: `exactReplayDoesNotCallActionPortSecondTimeAndChangedReplayConflicts`.
- SERVICE recording fails before replay/content/action checks: `serviceActorIsRejectedBeforeReplayContentAndActionChecks`.
- SERVICE reads are allowed when authorized: application read-order test and `authorizedServiceActorCanUseNarrowAndAuditedFullDetailReads`.
- Generated reference collisions are bounded to three retries and never overwrite.
- Full-detail audit failure never returns outcome content.

## Acceptance decision

AC1–AC9 are proven by the dedicated Q-014 suites. AC10 is not met because the complete Q-009–Q-014 gate has three Q-013 failures; therefore overall traceability status is **BLOCKED**.
