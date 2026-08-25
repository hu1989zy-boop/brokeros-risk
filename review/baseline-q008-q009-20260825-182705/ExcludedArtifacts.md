# Excluded Artifacts

## Generated ZIPs — DO NOT COMMIT

| Path | Classification | Reason |
| --- | --- | --- |
| `review/q-007/review-final-v1-20260824-055408.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |
| `review/q-008/review-q-008-v1-requirement-20260824-164535.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |
| `review/q-008/review-q-008-v2-architecture-20260824-170858.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |
| `review/q-008/review-q-008-v3-architecture-approved-20260825-132359.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |
| `review/q-008/review-q-008-v4-implementation-design-20260825-142122.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |
| `review/q-008/review-q-008-architect-approval-prerequisite-analysis-20260825-170517.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |
| `review/q-009/review-q-009-v1-requirement-20260825-180019.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |
| `review/review-history/review-202608121713.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |
| `review/review-history/review-202608181643.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |
| `review/review-baseline-q008-q009-20260825-182705.zip` | GENERATED | Review delivery archive; no ZIP is tracked by Git |

## Unrelated untracked Q-007 package — MUST NOT COMMIT

- `review/q-007/review-final-v1-20260824-055408/**`
- `review/q-007/review-final-v1-20260824-055408.zip`

Reason: Q-007 is already the current committed HEAD baseline, and this extra
untracked final-delivery package is outside the approved Q-008/Q-009 scope.
It is preserved on disk and not classified as safe for this commit.

## Generated/ignored local artifacts — MUST NOT COMMIT

- `review/.DS_Store`
- `review/q-008/.DS_Store`
- `backend/target/**`

These are ignored personal metadata or Maven build artifacts.

## Tracked unrelated paths

Backend, frontend, adapters, deployment, scripts, README, AGENTS.md, existing
requirements/architecture/ADRs/Skills, and root/archive Reviews outside the
explicit Include inventory are unchanged and must not be added to this commit.

## Unknown

None.

## Policy recommendation not applied

Because no ZIP is tracked but `.gitignore` lacks a ZIP rule, a later scoped
governance change may consider `review/**/*.zip`. This task deliberately does
not modify `.gitignore`.
