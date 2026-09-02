# Q-008 Design Traceability

## Requirement acceptance criteria

| AC | Result | Implementation and verification evidence |
| --- | --- | --- |
| 19.1-1 Existing capability/gap evidence | PASS | Preserved approved Requirement analysis; new implementation is isolated under `riskcase` and `audit` rather than claiming a pre-existing case module. |
| 19.1-2 ADR-009 preservation | PASS | `RiskCase` stores typed Decision references only; `DecisionReferenceAdapter` delegates to Q-012; architecture tests forbid upstream persistence/execution coupling. Manual intake has no Decision prerequisite. |
| 19.1-3 Non-overlapping definitions | PASS | Separate Risk Case aggregate, independent Audit Record, Q-011 EvidenceRef, Q-012 DecisionRef, Q-013 ActionRef, Q-014 ActionOutcomeRef, and case-owned `InvestigationNote`; no Alert/Rule Hit implementation is fabricated. |
| 19.1-4 Explicit intake paths | PASS | `RiskCase.openManual`, `RiskCase.openDecisionDriven`, `RiskCaseCreationService.parse`, `manualAndDecisionDrivenCreationPreserveConditionalIntake`, and REST DTO contract tests. |
| 19.1-5 Explicit design decisions | PASS | V4 design is realized by the aggregate, V8 tables, application services, controller, audit boundary, provider ports, and verification suites mapped below. |
| 19.1-6 ADR need/ADR-010 | PASS | Existing Accepted ADR-010 is implemented; no new system-boundary/dependency/deployment decision was introduced. |
| 19.1-7 Former questions resolved/deferred | PASS WITH CONDITION | CaseNumber and normalized resolution history are implemented. Related cases, teams/queues, and detailed retention/redaction remain deferred. Active-assignee lookup is disclosed in `OutstandingItems.md`. |
| 19.1-8 V3 package | PASS / prior gate | Existing V3 Architecture Approved package remains untouched. This implementation package is new V5 evidence. |
| 19.1-9 No implementation in governance gate | PASS / prior gate | The earlier gate artifacts remain intact. Implementation occurred only after the authoritative §26/V5/Product Owner authorization. No commit or push occurred. |
| 19.2-10 Named transition enforcement | PASS | `RiskCase` named operations, no public `setStatus`, 15 domain tests, stale-version tests, and real-MySQL resolve/close/resume/reopen races. |
| 19.2-11 Association ownership/history | PASS | `RiskCaseAssociationService`, provider ports/adapters, append-only Evidence/Decision/Action tables, exact FKs/checks, duplicate-association race, and two-cycle immutable-history test. |
| 19.2-12 Actors/reasons/time/history | PASS | Assignment/notes/resolution/cancel/close/reopen records carry ActorRef/reason/UTC time; full-path MySQL test proves retained prior cycles and correction/supersession history. |
| 19.2-13 CaseNumber contract | PASS | `CaseNumber`, `CaseNumberGenerator`, `RiskCaseIdentifiers`, unique ASCII-binary `CHAR(39)`, bounded three-attempt collision handling, domain validation, and migration constraints. Internal ID is absent from APIs. |
| 19.2-14 Priority versus risk assessment | PASS | Only `RiskCasePriority` LOW/NORMAL/HIGH/CRITICAL exists. Migration column inventory rejects severity/risk-level columns. |
| 19.2-15 Atomic audit | PASS | `RiskCaseCreationService`, `RiskCaseCommandService`, `RiskCaseAssociationService`, `RiskCaseResolutionService`, `JdbcAuditRecordWriter`; real-MySQL history-failure and audit-failure rollback tests. |
| 19.2-16 No external operation | PASS | Action/outcome are reference events only. Architecture/static scans find no execution or vendor API. No Kafka/Redis behavior exists. |
| 19.2-17 platform standards/full review | PASS WITH CONDITIONS | 300 repository tests pass; package/build/static/Kustomize pass. Legacy Q-004 infrastructure verifier failure and governance/tooling conditions are documented without fabrication. |

## Design §5 transition operations

| Approved operation | Aggregate/service implementation | Primary tests |
| --- | --- | --- |
| `openManual` | `RiskCase.openManual`; `RiskCaseCreationService.createTransaction` | `manualAndDecisionDrivenCreationPreserveConditionalIntake`; `createAndExactReplayPersistOneRootTransitionAndAudit` |
| `openDecisionDriven` | `RiskCase.openDecisionDriven`; creation service plus Decision provider/association/selection | creation domain/application tests; `concurrentDecisionDrivenCreationElectsOnePrimaryCaseAndRollsBackLoser` |
| `beginReview` | `RiskCase.beginReview`; `RiskCaseCommandService.beginReview` | `openToReviewRequiresAssignmentAndUsesNamedOperations`; full-path MySQL test |
| `cancel` | `RiskCase.cancel`; `RiskCaseCommandService.cancel` | `cancellationIsTerminalFromEveryApprovedActiveSource` |
| `markActionRequired` | `RiskCase.markActionRequired`; command service | `actionRequiredPathRequiresDecisionActionAndOutcomeBeforeResolution`; full-path MySQL test |
| `resolve` | `RiskCase.resolve`; `RiskCaseResolutionService.resolve` | direct/action-required domain tests; `concurrentResolveOnOneVersionWritesOneResolutionHistoryAndAudit`; full-path test |
| `returnToReview` | `RiskCase.returnToReview`; command service | `actionRequiredCanReturnToReviewWithoutStartingNewCycle` |
| `close` | `RiskCase.close`; command service | close/reopen domain test; `concurrentCloseAndResumeOnResolvedVersionLeaveExactlyOneTerminalHistory` |
| `resumeResolvedCase` | `RiskCase.resumeResolvedCase`; command service | `resolvedCaseCanResumeAndSuppliedAssignmentIsCaptured`; close-versus-resume concurrency test |
| `reopenClosedCase` | `RiskCase.reopenClosedCase`; command service | `closeAndExceptionalReopenStartNewCycleAndClearDecision`; `concurrentReopenOnClosedVersionStartsOnlyOneNewCycle` |

## Design §5.1 non-transition operations

| Approved operation | Aggregate/service implementation | Primary tests |
| --- | --- | --- |
| `assign` / `reassign` | `RiskCase.assign`; `RiskCaseCommandService.changeAssignment` | trusted ActorContext application test; concurrent CAS MySQL test; full-path test |
| `unassign` | `RiskCase.unassign`; command service null-assignee path | `unassignIsAllowedOnlyWhileOpen` |
| `changePriority` | `RiskCase.changePriority`; command service | `priorityMustChangeAndClosedOrCancelledCasesRejectIt`; full-path test |
| `associateEvidence` | aggregate version reservation; `RiskCaseAssociationService.associateEvidence` | strict provider tests; duplicate-evidence concurrency; full-path test |
| `changeEvidenceDisposition` | aggregate operation; association service appends disposition event | immutable FK/metadata test; full-path supersession test |
| `associateDecision` | `RiskCase.associateDecision`; association service appends immutable association and selection | provider tests; global uniqueness migration test; full-path test |
| `selectCurrentDecision` | `RiskCase.selectCurrentDecision`; association service | domain version-ordering test; full-path second-cycle selection |
| `associateAction` | `RiskCase.associateAction`; association service with Q-013 recognition/origin check | `associateActionRejectsOriginatingDecisionNotAssociatedBeforeWrites`; provider and full-path tests |
| `recordActionOutcomeReference` | aggregate invariant; association service with Q-014 pertaining-Action check | `outcomePertainingToAnotherActionIsRejectedBeforeWrites`; provider and full-path tests |
| `addInvestigationNote` | aggregate operation; association service append | DTO contract and full-path immutable note test |
| `correctInvestigationNote` | aggregate operation; association service inserts a superseding note | full-path immutable correction test; self-FK metadata test |

Every successful operation above reserves one new aggregate version before its
case-owned record and independent audit insert. `materialOperationsIncreaseVersionExactlyOnceForDeterministicOrder`
and the real-MySQL concurrency/rollback tests verify this cross-cutting rule.

## Design §8 table catalog

| Approved table | Persistence implementation | Primary verification |
| --- | --- | --- |
| `risk_case` | V8 root DDL; `JdbcRiskCaseRepository` insert/load/CAS | migration metadata/check tests; create/replay; all concurrency tests |
| `risk_case_transition_history` | V8 DDL; repository append/query | exact metadata; rollback; deterministic pagination; two-cycle full path |
| `risk_case_assignment_history` | V8 DDL; repository append/query | exact metadata; concurrent assignment; full path |
| `risk_case_priority_history` | V8 DDL; repository append/query | exact metadata; full path |
| `risk_case_evidence_association_history` | V8 DDL; append/prior-event/effective queries | FK restriction; duplicate race; full path |
| `risk_case_decision_association` | V8 DDL; immutable insert/lookups | global `decision_ref` uniqueness; concurrent decision creation; full path |
| `risk_case_decision_selection_history` | V8 DDL; selection append/query | exact metadata; reopen pointer clear; full path |
| `risk_case_action_association_history` | V8 DDL; action/outcome event queries | exact metadata; relational invariant app tests; full path |
| `risk_case_resolution_history` | V8 DDL; resolution append | unique `(case_id, cycle_no)`; resolve/reopen concurrency; two-cycle full path |
| `risk_case_resolution_evidence_reference` | V8 DDL; immutable snapshot insert | restrictive snapshot FKs; full-path resolution snapshot |
| `risk_case_resolution_action_reference` | V8 DDL; immutable action/outcome snapshot insert | restrictive snapshot FKs; ACTION_REQUIRED full path |
| `risk_case_note` | V8 DDL; append/find/correction check/query | restrictive self-FK; full-path immutable correction |
| `audit_record` | V8 DDL; `JdbcAuditRecordWriter` | audit-failure rollback; read-audit fail closed; full path/count assertions |

`Q008MySqlMigrationTests.migrationUpgradesV7UsingDynamicPendingCountAndCreatesExactOwnedTables`
proves the migration is the single dynamic post-V7 step and owns exactly this
explicit 13-table set without a broad prefix selector.

## V5 provider bindings

| Q-008 port | Shipped provider | Evidence |
| --- | --- | --- |
| `TradingAccountReferenceQuery` | Q-010 `TradingAccountReferenceEligibilityService` | `TradingAccountReferenceAdapter`; strict eligible/not-eligible/not-found/unavailable tests |
| `EvidenceReferenceQuery` | Q-011 `EvidenceProvenanceQueryService` | `EvidenceReferenceAdapter`; recognition/unavailable tests |
| `DecisionReferenceQuery` | Q-012 `DecisionProvenanceQueryService` | `DecisionReferenceAdapter`; recognition/unavailable tests |
| `ActionReferenceQuery` | Q-013 `ActionProvenanceQueryService` | `ActionReferenceAdapter`; originating-Decision invariant tests |
| `ActionOutcomeReferenceQuery` | Q-014 `ActionOutcomeProvenanceQueryService` | `ActionOutcomeReferenceAdapter`; pertaining-Action invariant tests |

## API surface

`RiskCaseController` exposes exactly the 21 Design §11 routes: create, detail,
history, assignment, review-start, Evidence association/disposition, Decision
association/selection, Action association/outcome reference, action-required,
review-return, priority change, note/correction, resolution, closure,
cancellation, resume, and reopen. `RiskCaseRestContractTests` verifies the exact
route set, absence of DELETE, request/response boundary fields, and exact nine
ResultCodes/statuses.
