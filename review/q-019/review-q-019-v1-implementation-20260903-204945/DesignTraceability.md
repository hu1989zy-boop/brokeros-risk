# Q-019 Design Traceability

## Requirement mapping

| Requirement | Implementation | Verification | Result |
| --- | --- | --- | --- |
| Q019-FR-01: authoritative association projection | `RiskCaseController.associations`; `RiskCaseQueryService.associations`; `RiskCaseAssociations`; `RiskCaseAssociationsResponse`; JDBC evidence/decision reads; existing effective action read | `Q019RiskCaseAssociationsMySqlTests.endpointReturnsAuthoritativeProjectionFromRealMysql` | PASS |
| Q019-FR-02: `risk-case:read`, envelope, bound, not-found | Existing `RiskCaseCapabilities.READ`; `ok(...)`; 500 per collection; existing ResultCodes/handler | Q-019 MySQL tests for 403-before-lookup, 404, and 501-item overflow; REST contract reflection test | PASS |
| Q019-FR-03: read-only, no aggregate/rule/migration change | Only repository port/JDBC reads and query/API mapping added | Empty targeted domain/migration/write-service/capability diff; full MySQL suite 309/309 | PASS |
| Q019-FR-04: authoritative panel and disposition picker | `getAssociations`; `useRiskCaseAssociations`; `AssociationsPanel`; `onCaseReferenceOptions`; `associationEventRef` changed to `on-case-select` | `AssociationsPanel.test.tsx`, repository/contract/MSW tests, Q-018 action refetch tests | PASS |
| Q019-FR-05: console reaches resolve using associated decision/action | `q019AssociationProjectionResolve.spec.ts` drives associate/select/associate/project/resolve/close | Spec discovery passed; execution reported 1 skipped because credential and seeded refs were unavailable | FAIL / NOT LIVE-VERIFIED |

## Implementation Design mapping

| Design section | Evidence |
| --- | --- |
| §2.1 endpoint | Existing controller GET plus query-service delegation; authorization precedes target resolution. |
| §2.2 evidence | `findAllEvidenceEvents` lists event rows in stable order; `findAllEffectiveEvidence` is reused and bounded. |
| §2.2 decisions | `findAllDecisionAssociations`; `current` compares each ref with snapshot `currentDecisionRef`. |
| §2.2 actions | Existing `findAllEffectiveActions`; nullable single persisted outcome is mapped to zero/one `outcomeRefs`. |
| §2.3 DTO | Exact case/version/evidence/decision/action field shape, with no internal IDs or external entity content. |
| §3 console | Typed model/parser/repository/hook; authoritative panel; all on-case pickers use projection; success and conflict refresh tested. |
| §4 backend | Real MySQL seed covers attached then superseded evidence, two decisions/one current, action/outcome, 403, 404, and bound. |
| §4 frontend | Component, parser, repository, invalidation/conflict, full Vitest/typecheck/build gates pass. |
| §4 live | Full UI spec delivered; current environment supplied none of the required live inputs, so one test skipped. |
| §5 boundaries | No aggregate, write path, migration, capability, other-module endpoint, Option B, or dependency change. |

## Technical choices made within design authority

1. The example cap of 500 became a constant applied independently to all four
   queried collections. Overflow fails closed with the existing
   `RISK_CASE_INVARIANT_VIOLATION` (HTTP 422) instead of truncating an authoritative
   response or adding a new ResultCode.
2. An immutable application projection separates domain/persistence objects from
   the REST DTO. This preserves the repository layering rule while keeping the
   external contract to the one approved response DTO.
3. Evidence returns every append-only event, as required by the two-event MySQL
   acceptance scenario and the disposition event-ref picker. Actions remain the
   existing effective projection. The frontend performs no state reconstruction.
