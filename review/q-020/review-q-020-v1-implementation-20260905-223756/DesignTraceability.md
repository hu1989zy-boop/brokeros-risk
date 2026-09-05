# Q-020 Design Traceability

| Requirement | Implementation evidence | Verification evidence | Result |
| --- | --- | --- | --- |
| Q020-FR-01 | Four controller query-param mappings; four list services and response DTOs; four query-port/JDBC methods; shared cap | Four `Q020*ReferenceListMySqlTests`; endpoint contract tests | PASS |
| Q020-FR-02 | `EvidenceCapabilities.READ`, `DecisionCapabilities.READ`, `ActionCapabilities.READ`, and `ActionOutcomeCapabilities.READ` are required before parsing/querying; no capability class changed | Each real-MySQL endpoint test checks denied `AUTHORIZATION_DENIED`, denial metric, and valid unknown-key `items: []` | PASS |
| Q020-FR-03 | JDBC list statements are parameterized `SELECT`s; no domain/write/association runner or migration files changed | Explicit Git boundary commands produced no output; V1-V8 migration inventory unchanged | PASS |
| Q020-FR-04 | List SQL and response records contain only refs, scope ref, optional status, and `recordedAt`; cap is 200 | MySQL fixtures seed 202 rows, include timestamp ties, assert 200 newest rows, ISO timestamp, and absence of each content property | PASS |
| Q020-FR-05 | `ReferenceListRepository`, provider, four enabled hooks, `ReferenceInput` browse/manual toggle, and case/on-case scope projection | `referenceListRepository.test.ts`, `ReferenceInput.test.tsx`, and `AssociationsPanel.test.tsx`; focused and full frontend suites pass | PASS |
| Q020-FR-06 | Candidate selection calls existing `onChange`; `CaseActionDialog` and `useCaseAction` association mechanics remain; descriptors and request repository are untouched | Q-020 case in `Q018AssociationActions.test.tsx` selects a browsed evidence ref, confirms preview, and asserts the exact pre-existing request body | PASS |

## Endpoint-to-code mapping

| Endpoint | Service / port / adapter | Response | Real-MySQL test |
| --- | --- | --- | --- |
| `GET /api/evidence?subjectRef=...` | `EvidenceReferenceListService`, `EvidenceQueryPort.findSummariesBySubject`, `JdbcEvidenceQueryAdapter` | `EvidenceReferenceListResponse` | `Q020EvidenceReferenceListMySqlTests` |
| `GET /api/decisions?subjectRef=...` | `DecisionReferenceListService`, `DecisionQueryPort.findSummariesBySubject`, `JdbcDecisionQueryAdapter` | `DecisionReferenceListResponse` | `Q020DecisionReferenceListMySqlTests` |
| `GET /api/actions?decisionRef=...` | `ActionReferenceListService`, `ActionQueryPort.findSummariesByDecision`, `JdbcActionQueryAdapter` | `ActionReferenceListResponse` | `Q020ActionReferenceListMySqlTests` |
| `GET /api/action-outcomes?actionRef=...` | `ActionOutcomeReferenceListService`, `ActionOutcomeQueryPort.findSummariesByAction`, `JdbcActionOutcomeQueryAdapter` | `ActionOutcomeReferenceListResponse` | `Q020ActionOutcomeReferenceListMySqlTests` |

## Confirmed exclusions

No pagination, free-text/content search, cross-module endpoint, cross-subject browse,
new status field, generated label, new capability, access-log write, aggregate rule,
write operation, table/column/index change, or migration is present.
