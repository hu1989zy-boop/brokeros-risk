# Approved Artifact Inventory

## Classification result

- A — Approved baseline artifact / SHOULD COMMIT: approved source documents
  and authoritative approval evidence.
- B — Review governance artifact / COMMIT BY CONVENTION: selected Markdown/TXT
  Review history required for auditability.
- C — Generated archive / SHOULD NOT COMMIT: all ZIP delivery packages.
- D — Unapproved or unrelated / MUST NOT COMMIT: Q-007 extra untracked review,
  personal metadata, build output, and anything outside the exact scope.
- E — Unknown / BLOCK: **none**.

Review text is PARTIAL policy rather than all-or-nothing: repository HEAD and
history track Review Markdown/TXT and archived Review evidence, while
`git ls-files '*.zip'` returns zero.

## Complete included inventory

| Path | Artifact classification | Reason | Approval/policy source |
| --- | --- | --- | --- |
| `docs/requirements/Q-008-Requirement.md` | APPROVED | Approved Q-008 Requirement and synchronized Gate state | Q-008 external Architect approval recorded in the document |
| `docs/adr/ADR-010-risk-case-foundation.md` | APPROVED | Accepted Risk Case architecture decision | ADR-010 external Architect acceptance |
| `docs/architecture/q-008-risk-case-foundation-implementation-design.md` | APPROVED | Final approved Q-008 Implementation Design V4; exact approved SHA preserved | Q-008 ArchitectApproval.md, decision dated 2026-08-25 |
| `docs/lessons/2026-08-25-q-008-risk-case-implementation-design.md` | APPROVED | Required honest Design-phase lesson | Approved Q-008 V4 phase and AGENTS.md |
| `docs/lessons/2026-08-25-q-008-architect-approval-prerequisite-analysis.md` | APPROVED | Required prerequisite-analysis lesson | Approved Q-008 prerequisite analysis and AGENTS.md |
| `docs/requirements/Q-009-Requirement.md` | APPROVED | Approved Q-009 Requirement V1 with metadata-only approval synchronization | External Architect approval confirmed by Product Owner 2026-08-25 |
| `docs/lessons/2026-08-25-q-009-trusted-actor-authorization-requirement.md` | APPROVED | Required honest Q-009 Requirement Discovery lesson | Approved Q-009 Requirement phase and AGENTS.md |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/ArchitectApproval.md` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/ArchitectureReview.md` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/GitDiffStat.txt` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/GitStatus.txt` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/OutstandingItems.md` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/PhaseReviewIndex.md` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/PrerequisiteAnalysis.md` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/ProjectTree.txt` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/Summary.md` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-architect-approval-prerequisite-analysis-20260825-170517/Verification.md` | APPROVED | Approved Q-008 V4 decision and prerequisite governance evidence | Explicit Q-008 Architect Design approval/prerequisite decision |
| `review/q-008/review-v1-requirement/ArchitectureReview.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v1-requirement/GitDiffStat.txt` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v1-requirement/GitStatus.txt` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v1-requirement/OutstandingItems.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v1-requirement/PhaseReviewIndex.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v1-requirement/ProjectTree.txt` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v1-requirement/Summary.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v1-requirement/Verification.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v2-architecture/ArchitectureReview.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v2-architecture/GitDiffStat.txt` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v2-architecture/GitStatus.txt` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v2-architecture/OutstandingItems.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v2-architecture/PhaseReviewIndex.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v2-architecture/ProjectTree.txt` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v2-architecture/Summary.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v2-architecture/Verification.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v3-architecture-approved/ArchitectureReview.md` | APPROVED | Approved Q-008 Architecture governance evidence | Explicit Q-008 Architect Architecture approval |
| `review/q-008/review-v3-architecture-approved/GitDiffStat.txt` | APPROVED | Approved Q-008 Architecture governance evidence | Explicit Q-008 Architect Architecture approval |
| `review/q-008/review-v3-architecture-approved/GitStatus.txt` | APPROVED | Approved Q-008 Architecture governance evidence | Explicit Q-008 Architect Architecture approval |
| `review/q-008/review-v3-architecture-approved/OutstandingItems.md` | APPROVED | Approved Q-008 Architecture governance evidence | Explicit Q-008 Architect Architecture approval |
| `review/q-008/review-v3-architecture-approved/PhaseReviewIndex.md` | APPROVED | Approved Q-008 Architecture governance evidence | Explicit Q-008 Architect Architecture approval |
| `review/q-008/review-v3-architecture-approved/ProjectTree.txt` | APPROVED | Approved Q-008 Architecture governance evidence | Explicit Q-008 Architect Architecture approval |
| `review/q-008/review-v3-architecture-approved/Summary.md` | APPROVED | Approved Q-008 Architecture governance evidence | Explicit Q-008 Architect Architecture approval |
| `review/q-008/review-v3-architecture-approved/Verification.md` | APPROVED | Approved Q-008 Architecture governance evidence | Explicit Q-008 Architect Architecture approval |
| `review/q-008/review-v4-implementation-design/ArchitectureReview.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v4-implementation-design/GitDiffStat.txt` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v4-implementation-design/GitStatus.txt` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v4-implementation-design/OutstandingItems.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v4-implementation-design/PhaseReviewIndex.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v4-implementation-design/ProjectTree.txt` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v4-implementation-design/Summary.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-008/review-v4-implementation-design/Verification.md` | HISTORICAL REVIEW | Immutable Q-008 submission/history snapshot | Tracked Review Markdown/TXT convention in HEAD and Git history |
| `review/q-009/review-v1-requirement-20260825-180019/ArchitectureReview.md` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/q-009/review-v1-requirement-20260825-180019/GapAnalysis.md` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/q-009/review-v1-requirement-20260825-180019/GitDiffStat.txt` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/q-009/review-v1-requirement-20260825-180019/GitStatus.txt` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/q-009/review-v1-requirement-20260825-180019/OutstandingItems.md` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/q-009/review-v1-requirement-20260825-180019/PhaseReviewIndex.md` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/q-009/review-v1-requirement-20260825-180019/ProjectTree.txt` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/q-009/review-v1-requirement-20260825-180019/RequirementReview.md` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/q-009/review-v1-requirement-20260825-180019/Summary.md` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/q-009/review-v1-requirement-20260825-180019/Verification.md` | HISTORICAL REVIEW | Immutable pre-approval Q-009 V1 Review snapshot | Tracked Review convention; later approval in Requirement/current baseline |
| `review/baseline-q008-q009-20260825-182705/Summary.md` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/ApprovedArtifactInventory.md` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/CommitScope.md` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/ExcludedArtifacts.md` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/ArchitectureReview.md` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/Verification.md` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/OutstandingItems.md` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/GitStatus.txt` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/GitDiffStat.txt` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/ProposedStagingCommands.txt` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/ProposedCommitMessage.txt` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/PhaseReviewIndex.md` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |
| `review/baseline-q008-q009-20260825-182705/ProjectTree.txt` | REVIEW GOVERNANCE | Mandatory current baseline-preparation governance record | User instruction and AGENTS.md Review Package requirement |

## Q-008 version classification

- The only formal Q-008 Implementation Design document is
  `docs/architecture/q-008-risk-case-foundation-implementation-design.md`.
  It is final V4 and **APPROVED**; its current SHA-256 exactly matches the hash
  recorded by the Architect.
- No standalone Q-008 Design V1, V2, or V3 source document exists.
- `review-v1-requirement` and `review-v2-architecture` are
  **HISTORICAL REVIEW** evidence.
- `review-v3-architecture-approved` is **APPROVED** Architecture evidence.
- `review-v4-implementation-design` is the immutable
  **HISTORICAL REVIEW** submission snapshot. Its pre-approval wording remains
  true for submission time.
- `review-architect-approval-prerequisite-analysis-20260825-170517` is the
  later authoritative **APPROVED** V4 decision and prerequisite record.
- No artifact is deleted, silently superseded, or rewritten into a V5.

## Q-009 classification

- `docs/requirements/Q-009-Requirement.md` is **APPROVED V1** after
  governance-metadata-only synchronization from the explicit approval.
- Its substantive requirements, Identity Authority OPEN decision, and
  authorization direction are unchanged.
- Its V1 Review directory is **HISTORICAL REVIEW** submission evidence and
  preserves its original pre-approval wording.
- Q-009 Architecture, ADR, Design, and Implementation artifacts do not exist.

## Generated ZIP inventory

| Path | Classification | Exclusion reason |
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

## Unknown

None. No file requires an uncertainty-based block.
