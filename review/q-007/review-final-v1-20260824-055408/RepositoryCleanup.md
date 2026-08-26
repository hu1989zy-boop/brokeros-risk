# Q-007 Repository and Git Candidate Review

## Candidate Scope

The complete staged candidate contains 38 files: 23 additions and 15
modifications. It contains Q-007 requirement, architecture, ADR, skill, lessons,
archived design review, root current-review updates, and small documentation
index/README updates.

No staged file is under:

- `backend/`
- `frontend/`
- `adapters/`
- `deploy/`
- `scripts/`
- `.github/`
- `docker-compose.yml`
- `review/review-history/`

## Deletion Accounting

`git diff --cached --stat` reports 747 line deletions but `git diff --cached
--name-status` reports no `D` entries. The removals primarily represent the
intentional replacement of the mutable root Review package from Q-006 content
to Q-007 content, plus regenerated status/tree evidence. The largest replaced
files are ArchitectureReview, Verification, ProjectTree, Summary, and
InitialBaselineCheck.

This does not delete tracked historical files. The Q-006 versions remain in Git
history at the current baseline commit. Q-007's earlier design review is added
under `review/archive/q-007/review-v1-design/`, so Q-007 history is preserved.

## Unrelated Work Isolation

- No unrelated Q-006 or older implementation file is staged.
- Root Review files necessarily replace the prior current-package snapshot; this
  is review rotation, not inclusion of unrelated implementation.
- `review/review-history/` was not inspected, modified, staged, or included.
- The existing `pre-q-007 unrelated review work` stash was not inspected,
  restored, modified, or included.

## Prohibited-content Review

The candidate contains none of the following:

- Java or runtime implementation
- Flyway changes or business database tables
- Redis/Kafka behavior or topics
- Docker/Kubernetes or CI changes
- MT4/MT5 adapter implementation
- Rule Engine, Evidence, Decision, Action, or Risk Case implementation
- Workflow, Audit, RBAC, or AI implementation
- Q-008 implementation
- `.DS_Store`, ZIP, `target/`, `.idea/`, `.env`, or secret material

## Local Artifact Handling

The locally generated `backend/target/` and a root `.DS_Store` were moved—not
deleted—to `/private/tmp/brokeros-q007-final-review-artifacts/`. The new package
directory and its ZIP remain untracked external review evidence by design.
