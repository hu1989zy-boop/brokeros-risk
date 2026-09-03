# Q-017 Design Traceability

## Functional requirements

| Requirement | Implementation evidence | Test evidence | Result |
| --- | --- | --- | --- |
| Q017-FR-01 assignment/priority | Typed repository commands + `assign`/`changePriority` descriptors | Exact request/success/pending/validation/error/403/conflict rows | PASS (mocked backend) |
| Q017-FR-02 review workflow | `beginReview`, `markActionRequired`, `returnToReview` descriptors | Same per-operation matrix; state availability matrix | PASS (mocked backend) |
| Q017-FR-03 terminal operations | Resolve/close/cancel fields, validation, distinct confirmation state | Per-operation matrix reaches a second confirmation button before POST | PASS (mocked backend) |
| Q017-FR-04 resume/reopen | Reason + optional assignee descriptors/repository commands | Exact request and all error/concurrency rows | PASS (mocked backend) |
| Q017-FR-05 note correction | History-backed `NotesPanel` binds `noteRef` to correction path | Descriptor matrix + detail-page selected-note test | PASS (mocked backend) |
| Q017-FR-06 optimistic concurrency | Every repository method serializes `expectedVersion`; shared runner refetches conflict | 11 rows preserve values and retry with version 8 after version 7 conflict | PASS |
| Q017-FR-07 availability/errors | Descriptor `allowedFrom`, status-filtered action bar, typed messages | Six-state button matrix; 11 ordinary errors + 11 typed 403s | PASS |
| Q017-FR-08 Bearer identity only | Existing Q-016 client reused; no actor input/body field | All 11 exact bodies assert absence of `actorRef` | PASS |

## Operation map

All rows are declared in `frontend/src/features/riskcase/actions/actionDescriptors.ts`,
executed by `useCaseAction.ts`, transported by `riskCaseRepository.ts`, and
parameterized in `frontend/tests/CaseActions.test.tsx`.

| Operation | POST path below `/api/risk-cases` | Body besides `expectedVersion` | Allowed status | Test result |
| --- | --- | --- | --- | --- |
| assign | `/{case}/assignments` | assigneeRef, reason | OPEN, IN_REVIEW, ACTION_REQUIRED | PASS |
| changePriority | `/{case}/priority-changes` | priority, reason | OPEN, IN_REVIEW, ACTION_REQUIRED | PASS |
| beginReview | `/{case}/review-start` | reason | OPEN | PASS |
| markActionRequired | `/{case}/action-required` | reason | IN_REVIEW | PASS |
| returnToReview | `/{case}/review-return` | reason | ACTION_REQUIRED | PASS |
| resolve | `/{case}/resolutions` | outcome, resolutionSummary, evidenceRefs, actionRefs | IN_REVIEW, ACTION_REQUIRED | PASS |
| close | `/{case}/closure` | reason | RESOLVED | PASS |
| cancel | `/{case}/cancellation` | reason, optional duplicateCaseNumber | OPEN, IN_REVIEW, ACTION_REQUIRED | PASS |
| resume | `/{case}/resume` | reason, optional assigneeRef | RESOLVED | PASS |
| reopen | `/{case}/reopen` | reason, optional assigneeRef | CLOSED | PASS |
| correctNote | `/{case}/notes/{noteRef}/corrections` | content | selected existing note, any status | PASS |

## Design section map

| Design section | Evidence | Result |
| --- | --- | --- |
| §2 component layout | `features/riskcase/actions`, generic UI components, detail integration | PASS |
| §3 shared runner | TanStack mutation, invalidation, typed errors, conflict refetch | PASS |
| §4 exact operations | 11-row registry/repository/body test matrix | PASS |
| §5 terminal confirmation | Resolve/close/cancel two-step modal and required fields | PASS |
| §6 detail integration | Action bar + dialog + per-note correction | PASS |
| §7 typed contract | Request types, resolution parser, unknown ResultCode failure | PASS |
| §8 capability grant | Exact eight capabilities in bootstrap and documentation | PASS |
| §9 tests | 103 Vitest tests pass; live Playwright code delivered but skipped | PASS WITH CONDITION |

## Live vertical slice

`frontend/tests/e2e/q017CaseLifecycle.spec.ts` implements login → locate seeded
OPEN case → assign → begin review → change priority → resolve → close. It asserts
each displayed version and corresponding history event. The test was discovered
but not executed because its external password and seeded case number were not
available; it is not counted as passed.
