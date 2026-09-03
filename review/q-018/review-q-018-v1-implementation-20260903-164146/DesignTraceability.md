# Q-018 Design Traceability

## Functional requirements

| Requirement | Implementation evidence | Test evidence | Result |
| --- | --- | --- | --- |
| Q018-FR-01 associate evidence | `associateEvidence` descriptor/repository; evidence preview; panel history projection | Exact request/success/pending/validation/error/403/conflict matrix | PASS with mocked backend; live not verified |
| Q018-FR-02 evidence disposition | Typed disposition path/body, enum/source/reason, optional replacement preview; manual warned event UUID fallback | Same six-state operation matrix; replacement preview exercised | FAIL: association event ID is absent from detail/history and cannot be picked or previewed |
| Q018-FR-03 decision associate/select | External decision preview + history/detail decision options | Two operation matrices; picker candidate test | BLOCKED live by `dc-` vs backend `dec-` |
| Q018-FR-04 action associate/outcome | External action/outcome previews + history-derived action picker | Two operation matrices; picker candidate test | BLOCKED live by `ac-/ao-` vs backend `act-/aoc-` |
| Q018-FR-05 pre-send external validation | Exact per-kind UUIDv4 checks, debounced GET, response-ref equality, submit confirmation gate | Valid, invalid-format/no-GET, 404, 403 tests; external refs used per operation | PASS against approved client contract; incompatible with three backend prefixes |
| Q018-FR-06 optimistic concurrency | Every request carries expectedVersion; shared Q-017 runner refetches conflict | Six conflict rows preserve input and retry 7→8 | PASS |
| Q018-FR-07 associations/errors | Labelled bounded projection, current decision marker, typed error messages | Panel/projection tests; six ordinary errors + six 403s | FAIL: backend read shape cannot expose complete current evidence/outcomes |
| Q018-FR-08 Bearer identity only | Existing Q-016 `ApiClient`; no actor input/body property | Six exact-body rows assert no `actorRef` | PASS |

## Operation map

All rows are declared in
`frontend/src/features/riskcase/actions/actionDescriptors.ts`, executed by the
unchanged `useCaseAction.ts`, transported by `riskCaseRepository.ts`, and
parameterized in `frontend/tests/Q018AssociationActions.test.tsx`.

| Operation | POST path below `/api/risk-cases` | Body besides `expectedVersion` | Reference handling | Automated result |
| --- | --- | --- | --- | --- |
| associateEvidence | `/{case}/evidence-associations` | evidenceRef, source, reason | external evidence preview | PASS (MSW) |
| changeEvidenceDisposition | `/{case}/evidence-associations/{eventRef}/dispositions` | disposition, replacementEvidenceRef?, source, reason | eventRef manual warned UUID; replacement external preview | PASS (MSW); picker/preview blocker |
| associateDecision | `/{case}/decision-associations` | decisionRef, reason | external decision preview | PASS (MSW); live prefix blocker |
| selectDecision | `/{case}/decision-selection` | decisionRef, reason | on-case detail/history select | PASS (MSW) |
| associateAction | `/{case}/action-associations` | actionRef, reason | external action preview | PASS (MSW); live prefix blocker |
| referenceActionOutcome | `/{case}/action-associations/{actionRef}/outcomes` | outcomeRef, reason | on-case action select + external outcome preview | PASS (MSW); live prefix blocker |

For each row the test matrix covers: success/exact body/no actor, pending-disabled
control, client validation, ordinary backend `ResultCode`, typed HTTP 403, and
version conflict reload/preserved input/retry.

## Preview map

| Kind | Approved client format | Existing GET | Small parsed fields | Result |
| --- | --- | --- | --- | --- |
| Evidence | `ev-<UUIDv4>` | `/api/evidence/{ref}` | reference, subject, source, status, recordedAt | Aligned |
| Decision | `dc-<UUIDv4>` | `/api/decisions/{ref}` | reference, subject, source, recordedAt | Backend domain expects `dec-`; blocked |
| Action | `ac-<UUIDv4>` | `/api/actions/{ref}` | reference, decision, source, status, recordedAt | Backend domain expects `act-`; blocked |
| Action outcome | `ao-<UUIDv4>` | `/api/action-outcomes/{ref}` | reference, action, source, recordedAt | Backend domain expects `aoc-`; blocked |

No observation, conclusion, intent, outcome text, actor identity, or other full
entity content is parsed into the preview model.

## On-case availability verification

| Required target/state | Actual detail/history availability | Implemented behavior | Result |
| --- | --- | --- | --- |
| Evidence association event ID | Not exposed; history `affectedRef` is evidence ref | Canonical UUID manual field with explicit no-preview warning | FAIL against picker + preview design |
| Associated decisions | `DECISION_ASSOCIATED.affectedRef`; current also in detail | Deduplicated select; current labelled | PASS within loaded page |
| Associated actions | `ACTION_ASSOCIATED/OUTCOME_REFERENCED.affectedRef` | Deduplicated active action select | PASS within loaded page |
| Effective evidence replacement | Disposition history omits replacement ref/event link | Panel shows latest visible evidence event only and warns | PARTIAL / BLOCKER |
| Referenced outcome ref | History exposes action ref, not outcome ref | Panel shows outcome presence, explicitly says ref unavailable | PARTIAL / BLOCKER |

History is limited to the first 100 entries. A non-null `nextCursor` triggers an
additional incompleteness warning.

## Design section map

| Design section | Evidence | Result |
| --- | --- | --- |
| §2 component layout | New preview API/model/UI and panel files under riskcase feature | PASS |
| §3 exact operations | Six typed descriptor/repository/body rows | PASS (mocked) |
| §4 Reference preview | Four existing GET paths, debounce, format/state/confirmation tests | PASS client-side; three prefix blockers |
| §5 On-case pickers | Decision/action selectors; disposition target missing | FAIL |
| §6 Detail integration | Panel hosts six actions; success/conflict query refresh | PASS with bounded-state qualification |
| §7 Capability grant | Exact five additive capabilities in bootstrap | PASS statically |
| §8 Typed contract | Six request types, three response parsers, four preview parsers; unknown code remains failure | PASS |
| §9 Testing | 148 Vitest pass; Q-018 Playwright discovered but skipped | FAIL live criterion |

## Live vertical slice

`frontend/tests/e2e/q018AssociationLifecycle.spec.ts` implements login → locate a
seeded case → associate a preview-confirmed decision → select it current →
associate a preview-confirmed action → assert versions/associations and that
Q-017 resolve remains enabled. It was discovered but skipped because the four
required external environment inputs were absent. The current backend prefix
contract must also be aligned before the approved short-ref slice can pass.
