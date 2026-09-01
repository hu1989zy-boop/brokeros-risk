# Q-013 Design Traceability

## Functional requirements

| Requirement | Implementation | Tests |
| --- | --- | --- |
| Q013-FR-001 | ActionRecordingService, RecordActionCommand, RecordActionSpec, ActionRecord, JdbcActionMutationAdapter | ActionApplicationTests.recordingUsesCanonicalOrderAndPassesOwnActorToDecisionAuthority; Q013MySqlPersistenceTests.recordReplayQueryAndAccessLogAreDurableAndConsistent |
| Q013-FR-002 | ActionCapabilities.RECORD and requireHuman in ActionRecordingService | actionAuthorizationDenialPrecedesEveryActionPort; serviceActorIsRejectedBeforeReplayContentAndDecisionChecks; real security denial/SERVICE tests |
| Q013-FR-003 | DecisionProvenanceQueryService call in ActionRecordingService | unrecognizedDecisionIsRejectedBeforeAnyActionWrite; decisionAuthorityFailureIsMappedAndDecisionReadDenialPreventsWrites; realDecisionReadDenialPreventsActionCreation |
| Q013-FR-004 | ActionFingerprintFactory, ActionOperationId, action_operation, replay paths | exactReplayDoesNotCallDecisionPortSecondTimeAndChangedReplayConflicts; concurrentSameOperationReturnsOneCommitAndOneReplay |
| Q013-FR-005 | ActionStatus.PROPOSED, ActionRecord invariant, V6 status CHECK | actionIsManualProposedAndReferencesExactlyOneDecision; action_record constraint test |
| Q013-FR-006 | Recording is the only mutation port/service; no update/delete SQL or route | ActionArchitectureTests.moduleHasNoTransitionDeleteExecutionVendorOrRawContentBehavior; REST route test |
| Q013-FR-007 | ActionProvenanceQueryService and ActionProvenanceView without intentText | narrowProvenanceContractCannotExposeIntentText; authorized SERVICE read tests |
| Q013-FR-008 | ActionDetailReadService and JdbcActionAccessLogAdapter | readsAuthorizeBeforeLookupPermitServiceAndAuditBeforeDisclosure; audit-failure unit/persistence tests; authorizedServiceActorCanUseNarrowAndAuditedFullDetailReads |
| Q013-FR-009 | IntentText free text; no operation taxonomy types | architecture prohibited-vocabulary scan |

## Design §11.1 canonical recording order

| Step | Implementation evidence | Test evidence |
| --- | --- | --- |
| 1 authorize action:record | ActionRecordingService.doRecord | actionAuthorizationDenialPrecedesEveryActionPort |
| 2 require HUMAN | ActionRecordingService.requireHuman | serviceActorIsRejectedBeforeReplayContentAndDecisionChecks |
| 3 raw-field fingerprint | ActionFingerprintFactory.forRecord before parsing | fingerprint golden-vector and replay-invalid-content tests |
| 4 replay check | ActionQueryPort.findOperation before content/Q-012 | exactReplayDoesNotCallDecisionPortSecondTimeAndChangedReplayConflicts |
| 5 content parsing | IntentText then DecisionRef construction | domain boundary tests and ACTION_CONTENT_INVALID mapping |
| 6 Q-012 confirmation | DecisionProvenanceQueryService.confirmProvenance with same ActorContext | canonical-order test; unrecognized/authority/denial tests |
| 7 authorized context | AuthorizedMutationFactory using shared Clock | canonical-order test and configuration inspection |
| 8 atomic mutation | ActionMutationPort.record and JdbcActionMutationAdapter transaction | operationFailureRollsBackActionAndLedgerAtomically |

## Design §8.4 constraint-to-test traceability

### action_record

| Invariant | V6 mechanism | Corresponding test |
| --- | --- | --- |
| id primary key | pk_action_record | metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex |
| action_ref unique | uk_action_record_ref | actionRecordPrimaryUniqueReferenceDecisionActorSourceStatusAndIntentChecksAreEnforced |
| action_ref canonical | ck_action_record_ref | ActionDomainTests actionRef test plus action_record constraint test |
| decision_ref canonical | ck_action_record_decision_ref | action_record constraint test |
| recorded_by_actor_ref canonical | ck_action_record_actor_ref | action_record constraint test |
| source MANUAL only | ck_action_record_source | actionIsManualProposedAndReferencesExactlyOneDecision plus action_record constraint test |
| status PROPOSED only | ck_action_record_status | same domain test plus action_record constraint test |
| intent_text 1-4000 bytes | ck_action_record_intent | IntentText domain boundary test plus action_record constraint test |

### action_operation

| Invariant | V6 mechanism | Corresponding test |
| --- | --- | --- |
| id primary key | pk_action_operation | metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex |
| operation_id unique | uk_action_operation_id | operationPrimaryUniqueIdSingleValueEnumsAndRecordForeignKeyAreEnforced |
| operation_id canonical | ck_action_operation_id | ActionDomainTests operation-id test plus operation constraint test |
| operation_type RECORD only | ck_action_operation_type | operation constraint test |
| outcome CREATED only | ck_action_operation_outcome | operation constraint test |
| action_id Action-internal FK and restrict | fk_action_operation_record | operation constraint/FK-restrict test |

### action_access_log

| Invariant | V6 mechanism | Corresponding test |
| --- | --- | --- |
| id primary key | pk_action_access_log | metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex |
| log cannot outlive Action | fk_action_access_log_record with ON DELETE RESTRICT | accessLogPrimaryAndRecordForeignKeyRestrictAreEnforced |

Every row in Design §8.4 has a named test. The migration suite itself derives
the unrestricted migration count from flyway.info().pending().length.
