# Q-007 Final Outstanding Items

## Blocking Items

None.

## Informational Only

- Q-007 implementation is Deferred; the PASS applies to the approved design
  baseline and closure documentation.
- The Q-007 candidate has not been committed or pushed, so no Q-007 commit SHA
  or GitHub Actions run exists yet.
- Pre-Q-007 unrelated changes are safely preserved in stash
  `pre-q-007 unrelated review work` and are outside the candidate commit.
- The user-owned `review/review-history/` directory remains protected,
  untracked, unread, unmodified, and unstaged.
- Local Java 21, Docker, kubectl, and kustomize are unavailable. Backend tests
  and package passed locally on Java 23; the unchanged committed baseline
  passed CI on required Java 21.
- Observation, Evidence Chain, Decision Metadata, Rule Engine implementation,
  AI integration, and all business capabilities remain future considerations
  requiring formal Requirements and architecture review.

## Remaining Risks

- Future work may collapse Action into Execution or let Risk Case own Decision.
- Rule Engine/AI Requirements may fail to preserve Evidence provenance.
- The recoverable stash must not be accidentally included in the Q-007 commit.

## Ready for Git Commit

YES. The next authorized action is a Q-007-only documentation commit and CI
verification. Do not start Q-008.

====================================
Codex Prompt
====================================

Commit and verify the approved Q-007 BrokerOS Risk Design Baseline in the
current `brokeros-risk` repository.

Requirements:

1. Read `AGENTS.md`, Q-007 Requirement, ADR-009, Q-007 Architecture Design,
   Q-007 Skill, Lessons Learned, and the final root Review Package.
2. Confirm the staged candidate contains only Q-007 documentation and Review
   files. Do not restore or include stash `pre-q-007 unrelated review work`.
3. Do not read, modify, stage, or commit `review/review-history/`.
4. Do not include `.DS_Store`, zip files, `target/`, IDE files, local environment
   files, secrets, or unrelated historical Review changes.
5. Verify with `git diff --cached --name-status`,
   `git diff --cached --check`, protected-archive-aware static verification,
   and `git status --short --branch`.
6. Confirm no Java, test logic, runtime configuration, API, Flyway, Redis,
   Kafka, CI, Docker, Kubernetes, adapter, Rule Engine, Evidence, Decision,
   Action, Risk Case, Workflow, Audit, RBAC, AI, or Q-008 implementation is
   staged.
7. Commit with `docs: establish q-007 domain design baseline`.
8. Push `main` and wait for GitHub Actions for the exact new commit.
9. If CI fails, do not modify code or create another commit. Report the failing
   stage and wait for Architect direction.
10. If CI passes, update Q-007 closure evidence only if a separately authorized
    closure task requests it; otherwise report commit SHA, run ID, job ID,
    stage results, and final Git status.
11. Stop. Do not begin Q-008 or any business implementation.
