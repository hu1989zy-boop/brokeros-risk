# Q-010 V1 — Requirement Candidate Analysis & Recommendation Prompt

## Role
Act as the implementation/repository analysis agent for the BrokerOS Risk project.

Q-009 has completed Final Architect Approval, was manually committed by the user, and pushed successfully.

Start **Q-010 planning** from the new Git baseline.

This round is strictly:

**Q-010 V1 — Requirement Candidate Analysis & Recommendation**

It is NOT authorization to implement Q-010.

## Objective
Determine the most appropriate next formal requirement after the completed Q-007/Q-008/Q-009 foundation work.

The decision must be derived from the actual repository baseline, approved architecture/ADRs, requirements, skills, lessons learned, review history, outstanding items, and project roadmap.

Do not invent a requirement merely to continue development.

## 1. Baseline inspection
Capture:
- current branch
- current HEAD
- `git status --short`
- `git log -n 10 --oneline`
- `git diff --stat`
- relevant project tree

Confirm Q-009 is present in the committed baseline.

Do not modify, reset, stash, commit, or push anything during baseline inspection.

## 2. Read authoritative repository context
Inspect at minimum:
- `AGENTS.md`
- Q-001 through Q-009 requirements/statuses that exist
- approved architecture documents
- all applicable ADRs, especially the latest foundation ADRs
- project architecture / roadmap documents
- relevant Skills
- Lessons Learned
- review/phase indexes
- Outstanding Items
- Q-007/Q-008/Q-009 closure records

Use repository evidence as authoritative.

Do not assume the next requirement from chat history alone.

## 3. Capability/gap map
Produce a concise map with at least:

### Already established
Identify capabilities already provided by Q-001–Q-009 and therefore not candidates for duplicate implementation.

### Approved but not implemented
Identify requirements/capabilities that already have approval but intentionally remain deferred.

### Missing prerequisites
Identify technical/domain prerequisites required before meaningful Risk business functionality can safely begin.

### Candidate business capabilities
Identify the next realistic BrokerOS Risk capabilities enabled by the current foundation.

### Explicitly premature items
Identify roadmap capabilities that should NOT be started yet (for example later-phase streaming/ML/trading-platform work if prerequisites are not ready).

## 4. Generate Q-010 candidates
Generate **2–4 evidence-backed candidates** for Q-010.

For each candidate provide:
- proposed title
- problem solved
- why now
- prerequisites
- dependencies on Q-001–Q-009
- architecture impact
- whether ADR analysis is likely required
- database impact
- security/audit implications
- expected implementation size
- risks
- what becomes possible afterward

Do not implement any candidate.

## 5. Recommend exactly one Q-010
Select exactly ONE preferred Q-010 candidate.

The recommendation must optimize for:
1. architectural sequence
2. smallest safe increment
3. reuse of existing foundations
4. avoiding speculative infrastructure
5. moving BrokerOS toward real Risk Case / Rule Engine / Account Control / Audit capability
6. testability and clear acceptance criteria
7. preserving future productization/multi-broker extensibility

Explain why the other candidates should wait.

## 6. Draft the proposed Q-010 requirement boundary
For the selected candidate, prepare a **requirement proposal**, not an approved requirement and not implementation.

Include:
- Background
- Problem Statement
- Goals
- In Scope
- Non-Goals
- Functional Requirements
- Acceptance Criteria
- Technical Constraints
- Security/Audit Constraints
- Data/Schema considerations
- Dependencies
- Verification Plan
- Risks/Open Questions
- Deliverables

Mark it clearly:

**Status: Proposed — awaiting Architect review/approval**

If repository evidence shows that Q-010 already exists or another numbered requirement is reserved, do not overwrite it. Report the conflict and recommend the correct next identifier.

## 7. No implementation
This round MUST NOT:
- add production Java implementation
- add Flyway migrations for the candidate
- change runtime configuration
- add dependencies
- implement Q-008 or any deferred requirement
- create speculative infrastructure
- modify approved architecture
- create/accept an ADR
- mark Q-010 approved
- commit or push

Only planning/governance artifacts required for this analysis may be created.

## 8. Review package
Create a new timestamped review package following repository conventions, preferably:

`review/q-010/review-q-010-v1-requirement-candidate-analysis-<timestamp>/`

Include equivalents of:
- `Summary.md`
- `RequirementCandidateAnalysis.md`
- `CapabilityGapMap.md`
- `ArchitectureReview.md`
- `OutstandingItems.md`
- `Verification.md`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `ProjectTree.txt`

If appropriate under repository conventions, create the proposed Q-010 requirement document, but it must remain explicitly **Proposed / awaiting Architect approval**.

Create a ZIP:

`review-q-010-v1-requirement-candidate-analysis-<timestamp>.zip`

Do not overwrite prior review packages.

## 9. Required final response
Report:
1. V1 result
2. repository baseline HEAD
3. whether Q-009 committed baseline was confirmed
4. candidate list
5. recommended Q-010
6. why it is the preferred next increment
7. whether ADR analysis is expected
8. whether production code changed (expected: NO)
9. proposed requirement path, if created
10. review directory
11. ZIP path
12. blockers/open questions

Do not claim Q-010 is approved.

## Decision principle
The purpose of V1 is to decide **what Q-010 should be**, based on repository evidence.

If the repository shows an unresolved prerequisite or lifecycle inconsistency, recommend resolving that first rather than forcing a new business feature.

Accuracy and architectural continuity are more important than development speed.
