# Q-010 V6 — Implementation Design Approval Recording Prompt

## Role

You are Codex working inside the BrokerOS Risk repository.

This task is **Q-010 V6 — Implementation Design Approval Recording**.

An independent external Architect has reviewed the Q-010 V5 Implementation Design and issued:

**PASS — Q-010 Implementation Design APPROVED**

This round exists only to record that approval into the repository and produce a new review package.

This is NOT an implementation task.

---

# 1. Authoritative Gate State

Treat these decisions as authoritative:

- Q-010 Requirement: APPROVED
- Q-010 Architecture: APPROVED
- ADR-012: ACCEPTED
- Q-010 V4 Architecture/ADR Approval Recording: APPROVED
- Q-010 V5 Implementation Design: EXTERNAL ARCHITECT APPROVED
- Implementation: NOT STARTED
- Implementation Allowed: NO until this approval recording is independently reviewed

Do not re-open already approved Requirement, Architecture, ADR, or Implementation Design decisions.

---

# 2. Mandatory Preflight

Before editing:

1. Read and obey repository root `AGENTS.md`.
2. Inspect branch, HEAD, working tree, and current Q-010 artifacts.
3. Read:
   - Q-010 Requirement
   - Q-010 Architecture
   - ADR-012
   - Q-010 Implementation Design
   - Q-010 V4 approval recording
   - Q-010 V5 review artifacts
   - repository review/index/lessons conventions
4. Confirm the V5 design being approved is the same authoritative design reviewed by the Architect.
5. Preserve unrelated existing changes.
6. Do not overwrite prior Q-010 review directories or ZIPs.
7. Do not commit or push.

If repository state materially contradicts the supplied approval, stop and report BLOCKED rather than silently repairing or redesigning it.

---

# 3. Architect Decision to Record

Record the external Architect approval of the existing Q-010 Implementation Design.

The approval covers the V5 design as written, including its established:

- BrokerOS-owned `TradingAccountRef`
- external identity tuple
- immutable one-to-one mapping
- lifecycle semantics
- database-level uniqueness
- canonicalization rules
- concurrency handling
- idempotency semantics
- local transaction/rollback rules
- controlled non-Web provisioning
- Q-009 authorization integration
- narrow Q-008 read-only consumer contract
- error/result model
- sensitive logging rules
- immutable history
- Flyway implementation plan
- security and test matrix

Do not redesign these in V6.

---

# 4. Implementation Design Status Update

Update the authoritative Q-010 Implementation Design status from its current draft/awaiting-approval state to the repository-equivalent of:

**APPROVED — External Architect**

Record approval provenance/date according to repository convention.

Do not claim Codex approved its own design.

Preserve the substantive design.

Only make minimal metadata/gate/provenance edits necessary to record approval.

---

# 5. Gate Synchronization

Synchronize relevant governance documents so they consistently show:

- Requirement: APPROVED
- Architecture: APPROVED
- ADR-012: ACCEPTED
- Implementation Design: APPROVED
- Implementation: NOT STARTED
- Implementation Allowed: NO pending independent review of this V6 approval-recording package

Do not mark implementation complete.

Do not start implementation.

Do not mark Q-008 implementation allowed merely because Q-010 Design is approved.

---

# 6. Explicit Prohibitions

You MUST NOT:

- add production Java code
- add domain/application/infrastructure implementation classes
- add repositories/services/controllers
- create Flyway migrations
- modify runtime DB schema
- create REST endpoints
- implement provisioning tooling
- implement manifest parser/runtime
- implement Redis/Kafka behavior
- implement Q-008
- redesign Q-009
- change approved Q-010 semantics
- introduce new dependencies
- change Docker/Kubernetes runtime behavior
- perform unrelated cleanup
- fix historical Q-009 whitespace solely to green the gate
- git commit
- git push

If an unrelated problem is observed, record it in OutstandingItems.

---

# 7. Review Package Self-Containment Improvement

The V5 Architect review noted that the review ZIP referenced the authoritative ~1217-line Implementation Design by repository path rather than including the full authoritative artifact.

For V6, improve review-package independence.

Where repository conventions permit, include a snapshot/copy of the authoritative approved Implementation Design in the V6 review directory, or otherwise include sufficient immutable evidence (for example exact file hash plus a packaged copy/reference artifact) so an independent reviewer can verify which exact design was approved without relying only on summaries.

Do NOT create a second authoritative design document.

The repository document remains authoritative; the packaged copy is review evidence only.

Also include hashes for key Q-010 authoritative artifacts if repository conventions allow, especially:

- Requirement
- Architecture
- ADR-012
- Implementation Design

---

# 8. Verification

Run appropriate documentation/static verification.

Record:

- branch
- HEAD
- git status before/after where available
- diff stat
- relevant static/doc checks
- review package integrity
- artifact hashes if generated
- confirmation that no production/runtime implementation was added

The known historical Q-009 whitespace/static issue must remain classified as PRE-EXISTING if unchanged.

Clearly distinguish:

- V6-introduced failures
- pre-existing unrelated failures

Do not modify unrelated historical files just to make a check pass.

---

# 9. Governance Artifacts

Update only what repository convention requires for Design Approval Recording, such as:

- Q-010 Implementation Design approval metadata
- Q-010 phase/review index
- architecture/design review record
- lessons learned
- outstanding items
- summary/status references
- relevant skill status/reference synchronization if required

Do not add implementation instructions beyond the already approved design.

---

# 10. V6 Review Directory

Create a new review directory without overwriting V1–V5.

Preferred semantic label:

`q-010-v6-implementation-design-approved`

Include, following repository convention:

- `Summary.md`
- `DesignReview.md` and/or appropriate approval-recording review
- `Verification.md`
- `OutstandingItems.md`
- `LessonsLearned.md`
- `PhaseReviewIndex.md`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `ProjectTree.txt`
- authoritative Implementation Design review snapshot/evidence
- artifact hash manifest where appropriate

The package must explicitly state that the Architect approval was supplied externally and Codex is only recording it.

---

# 11. V6 Review ZIP

Create a new ZIP:

`review-q-010-v6-implementation-design-approved-<timestamp>.zip`

Requirements:

- do not overwrite previous ZIPs
- verify ZIP integrity
- verify intended files are present
- verify packaged authoritative-design evidence corresponds to the repository design
- record verification results

---

# 12. Git Rules

Do not commit.
Do not push.

Preserve unrelated pre-existing changes.

At the end, report untracked/modified/staged state accurately.

Do not say Ready for Git Commit.

---

# 13. Acceptance Criteria

V6 is complete only if:

1. The authoritative Q-010 Implementation Design is marked APPROVED with external Architect provenance.
2. Requirement remains APPROVED.
3. Architecture remains APPROVED.
4. ADR-012 remains ACCEPTED.
5. No substantive design drift occurred.
6. Implementation remains NOT STARTED.
7. Implementation Allowed remains NO pending independent V6 review.
8. No production Java code was added.
9. No Flyway migration/runtime schema change was added.
10. No REST/provisioning runtime implementation was added.
11. Q-008 implementation was not started.
12. Q-009 was not redesigned.
13. Governance/index artifacts are synchronized.
14. V6 review evidence is more self-contained than V5 and identifies the exact approved design.
15. A new V6 review directory exists.
16. A new V6 review ZIP exists and passes integrity verification.
17. Pre-existing static failures are distinguished from new regressions.
18. No Git commit or push occurred.

---

# 14. Final Codex Response Format

Return a concise report with:

1. `Q-010 V6 Result`
2. Requirement status
3. Architecture status
4. ADR-012 status
5. Implementation Design status
6. Implementation status
7. Implementation Allowed status
8. files changed
9. verification result
10. pre-existing issues
11. review-package self-containment evidence
12. exact V6 review directory path
13. exact V6 review ZIP path
14. Git status summary
15. explicit statement:

`Ready for Architect Review: YES`

Do NOT say:

`Ready for Implementation: YES`

Do NOT say:

`Ready for Git Commit: YES`

The next action is independent Architect review of this V6 approval-recording package.
