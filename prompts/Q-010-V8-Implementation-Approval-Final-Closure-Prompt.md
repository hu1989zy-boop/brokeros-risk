# Q-010 V8 — Implementation Approval + Final Closure Prompt

## Role

You are Codex working inside the BrokerOS Risk repository.

This task is **Q-010 V8 — Implementation Approval + Final Closure**.

An independent external Architect has reviewed the Q-010 V7 implementation and issued:

**PASS — Q-010 V7 IMPLEMENTATION APPROVED**

This round has two purposes only:

1. formally record the external Architect's implementation approval;
2. perform Q-010 final closure and determine whether the complete Q-010 change set is ready for Git commit.

Do not add new product functionality.
Do not redesign Q-010.
Do not begin Q-008 implementation.
Do not commit or push.

---

# 1. Authoritative Gate State

Treat the following as authoritative:

- Q-010 Requirement: APPROVED
- Q-010 Architecture: APPROVED
- ADR-012: ACCEPTED
- Q-010 Implementation Design: APPROVED
- Q-010 V6 Design Approval Recording: ACCEPTED
- Q-010 V7 Implementation: EXTERNAL ARCHITECT APPROVED
- Final Closure: NOT YET RECORDED
- Git Commit: NOT YET AUTHORIZED
- Git Push: NOT AUTHORIZED

The V8 task is not permission to reopen any approved decision.

---

# 2. Mandatory Preflight

Before editing:

1. Read and obey repository root `AGENTS.md`.
2. Inspect:
   - current branch
   - HEAD
   - working tree
   - staged files
   - untracked files
   - Q-010 review history V1–V7
3. Read the authoritative:
   - Q-010 Requirement
   - Q-010 Architecture
   - ADR-012
   - Q-010 approved Implementation Design
   - Q-010 V7 implementation
   - V7 Review Package/evidence
4. Confirm the implementation currently in the working tree is the same implementation that the external Architect approved.
5. Detect any post-V7 code/schema/test drift.
6. Preserve unrelated pre-existing changes.
7. Do not commit or push.

If material implementation drift exists after V7 review, V8 must be BLOCKED until that drift is independently reviewed.

---

# 3. External Architect Implementation Decision to Record

Record the supplied external Architect decision:

**Q-010 Implementation V7 — APPROVED**

Approval specifically recognizes that the implementation preserves the approved design, including:

- BrokerOS-owned opaque `TradingAccountRef`
- external identity tuple:
  - `AccountAuthorityScopeRef`
  - `SourceNamespace`
  - `ExternalAccountKey`
- DB-level bidirectional uniqueness
- exact external-key comparison/storage semantics
- immutable/non-reassignable identity mapping
- lifecycle and CAS semantics
- durable idempotency
- transaction atomicity
- immutable history
- controlled non-Web provisioning
- Q-009 authorization integration
- authorization-before-sensitive-lookup
- Q-008 bounded read-only eligibility facade
- sensitive-data/non-enumeration protections
- Flyway persistence implementation
- unit/application/persistence/transaction/concurrency/security verification

Codex is recording an external decision, not self-approving.

---

# 4. Implementation Status Recording

Update Q-010 governance/status artifacts so the implementation is formally marked as the repository-equivalent of:

**IMPLEMENTATION APPROVED — External Architect**

Record approval provenance and date according to repository conventions.

Do not materially alter approved production implementation during this status-recording step.

If a defect is discovered during closure verification, do not silently patch it and still call V8 closed. Record the defect and set closure BLOCKED unless the repository process explicitly supports a separately reviewed closure patch.

---

# 5. Final Closure Objective

After recording approval, perform a complete Q-010 lifecycle closure review.

Verify the full chain:

Requirement
→ Architecture
→ ADR-012
→ Implementation Design
→ Implementation
→ Verification
→ Review Evidence
→ Final Closure

Confirm that every approved MUST has an implemented and verified counterpart.

Confirm no unapproved scope was introduced.

---

# 6. Requirement Traceability Freeze

Produce/finalize a closure-level Requirement Traceability Matrix.

For every Q-010 acceptance criterion / MUST requirement, identify:

- Requirement reference
- Architecture/design reference
- implementation component(s)
- persistence/migration component if applicable
- test(s)
- verification evidence
- final status: PASS / BLOCKED

No required item may be silently omitted.

If any required item lacks implementation or verification evidence, Final Closure must not PASS.

---

# 7. Design Traceability Freeze

Produce/finalize a Design Traceability Matrix covering the approved Implementation Design.

At minimum verify:

- domain/value types
- application use cases
- ports
- persistence adapters
- relational schema
- uniqueness constraints
- canonicalization
- concurrency behavior
- idempotency
- transaction boundaries
- immutable history
- provisioning manifest
- controlled execution boundary
- Q-009 authorization
- Q-008 bounded facade
- result/error handling
- sensitive logging
- observability
- security requirements
- tests

Classify each as:

- IMPLEMENTED + VERIFIED
- NOT APPLICABLE with justification
- BLOCKED

Do not accept unexplained omissions.

---

# 8. Implementation Drift Check

Compare the current authoritative implementation against the exact V7 implementation reviewed by the Architect.

Use hashes/diffs where practical.

The V8 review package must state:

- whether production code changed after V7 review
- whether Flyway changed after V7 review
- whether tests changed after V7 review
- whether authoritative design changed after V7 review

Approval-recording/governance-only changes are expected.

Any material unreviewed runtime change blocks closure.

---

# 9. Flyway / Database Final Verification

Re-verify the actual Q-010 migration and schema behavior.

Confirm:

- migration version is correct in repository sequence
- clean migration path succeeds where repository verification supports it
- restart/re-run behavior succeeds
- all Q-010 tables exist
- PK/FK/unique constraints/indexes match approved design
- `TradingAccountRef` uniqueness exists
- external identity tuple uniqueness exists
- exact `ExternalAccountKey` semantics remain correct
- lifecycle/version columns remain correct
- operation/idempotency persistence remains correct
- immutable history remains append-only by application contract
- no destructive migration behavior was introduced

Do not alter schema merely to make closure evidence prettier.

---

# 10. Concurrency Final Verification

Re-run or preserve fresh reproducible evidence for the approved concurrency guarantees.

Verify at minimum:

- same external identity concurrent provisioning -> one authoritative mapping
- same TradingAccountRef competing mapping -> rejected
- same external identity competing TradingAccountRef -> rejected
- concurrent duplicate operation -> deterministic replay/conflict
- lifecycle CAS -> no lost update

Database constraints must remain the final uniqueness arbiter.

Do not substitute mocked tests for DB concurrency evidence if the repository's V7 gate used real MySQL tests.

---

# 11. Transaction / Rollback Final Verification

Verify:

- registry mutation + required operation outcome + history are atomic where approved
- history persistence failure rolls back business mutation
- operation persistence failure rolls back business mutation where approved
- no partial durable state survives tested failure paths

Capture evidence in the final review.

---

# 12. Security Final Verification

Re-confirm:

- Q-009 authorization is reused
- no parallel authorization model exists
- authorization-before-sensitive-lookup remains true
- unauthorized callers cannot enumerate account existence beyond approved semantics
- Q-008 facade does not disclose forbidden identity fields
- no public/admin provisioning REST endpoint exists
- manifest handling rejects unsafe/invalid inputs according to design
- raw sensitive identity data is not leaked in normal errors/logging
- no secrets are present in source/review ZIP

Include a final security checklist.

---

# 13. Q-008 Boundary Check

Explicitly verify that V7/V8 did NOT implement Q-008 Risk Case business functionality.

Q-010 may provide only the approved bounded read-only facade/prerequisite.

Final closure must not silently mark Q-008 itself complete.

Record the exact prerequisite status Q-010 now provides to Q-008.

---

# 14. Full Verification

Run the strongest repository-appropriate verification available.

Use actual repository commands and conventions.

Where applicable include:

- full Maven verification/tests
- Q-010 focused tests
- MySQL integration tests
- Flyway migration verification
- concurrency tests
- architecture/package-boundary tests
- static verification
- Docker/runtime gate if required by repository policy

Record:

- exact command
- exit code
- test counts
- failures/errors/skips
- environment/version information relevant to reproducibility

Do not fabricate PASS for unavailable tooling.

---

# 15. Known Pre-Existing Static Issue

The external Architect review observed the historical static issue in:

`review/q-006-design/Q-009-V6-Approved-Design-Git-Baseline-Prompt.md`

including whitespace around lines 67/68 and EOF.

If unchanged:

- classify it as PRE-EXISTING
- prove Q-010 did not introduce it
- do not fix it inside Q-010 V8 solely to make the gate green

If static verification reveals any NEW Q-010 issue, do not hide it under this exception.

---

# 16. MySQL / Flyway Environment Note

V7 review evidence used MySQL 8.4 and noted Flyway 11.7.2 reports a latest-tested MySQL target around 8.1.

During V8:

1. record the repository's actual intended DB compatibility baseline;
2. record the DB version used for final verification;
3. preserve the successful migration/test evidence;
4. classify version-support mismatch accurately if it remains only a tooling warning;
5. do not change project DB strategy in Final Closure.

If repository policy requires a specific DB version and V7 was verified against the wrong mandatory target, Closure must be BLOCKED pending correct verification.

---

# 17. Final Outstanding Items

Create a final Q-010 outstanding-items record.

Separate:

## Blocking

Anything preventing Final Closure or Git commit.

## Non-blocking

Examples may include:

- historical unrelated static whitespace
- documented tooling compatibility warning
- future Q-008 work
- operational/deployment follow-ups outside Q-010 scope

Do not call a real Q-010 correctness/security defect “non-blocking” merely to close the phase.

---

# 18. Lessons Learned

Finalize Q-010 Lessons Learned with only concrete lessons supported by the work.

Potential areas:

- identity authority must not be confused with platform login
- database constraints are essential for concurrency correctness
- durable idempotency must survive timeout/retry
- authorization ordering matters for enumeration resistance
- immutable review snapshots/hashes improve approval provenance
- integration/concurrency tests must validate architecture guarantees, not merely code coverage

Avoid generic filler.

---

# 19. Final Closure Status

If and only if every blocking gate passes, record Q-010 as the repository-equivalent of:

- Requirement: APPROVED
- Architecture: APPROVED
- ADR-012: ACCEPTED
- Implementation Design: APPROVED
- Implementation: APPROVED
- Verification: PASS, with clearly isolated pre-existing exceptions if applicable
- Final Closure: PASS / CLOSED
- Ready for Git Commit: YES

Important:

`Ready for Git Commit: YES` is a closure assessment only.

**DO NOT actually run git commit.**
**DO NOT run git push.**

The user will perform Git commit manually after external review of V8.

If any blocker exists:

- Final Closure: BLOCKED
- Ready for Git Commit: NO

---

# 20. Self-Contained Final Review Package

V8 is the final Q-010 closure package and should be independently reviewable.

Create a new review directory, preferred semantic label:

`q-010-v8-final-closure`

Do not overwrite V1–V7.

Include, following repository convention, at least:

- `Summary.md`
- `FinalClosureReview.md`
- `ImplementationApproval.md`
- `RequirementTraceability.md`
- `DesignTraceability.md`
- `ArchitectureReview.md`
- `ImplementationReview.md`
- `PersistenceReview.md`
- `ConcurrencyReview.md`
- `SecurityReview.md`
- `Verification.md`
- `OutstandingItems.md`
- `LessonsLearned.md`
- `PhaseReviewIndex.md`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `ProjectTree.txt`
- artifact hash manifest
- implementation file inventory
- migration snapshot
- test inventory/result evidence

Where practical, include review snapshots of authoritative:

- Requirement
- Architecture
- ADR-012
- approved Implementation Design
- migration
- important implementation inventory

Do not create duplicate authoritative documents; packaged copies are evidence only.

Do not include secrets, credentials, tokens, private keys, production data, database dumps, `.env` secrets, or other sensitive material.

---

# 21. Artifact Hashes

Generate SHA-256 evidence for key authoritative artifacts where practical.

Include at minimum:

- Requirement
- Architecture
- ADR-012
- Implementation Design
- Flyway migration
- important Q-010 implementation inventory or deterministic manifest
- V8 ZIP

The purpose is to make approval provenance and post-review drift detectable.

---

# 22. Final Review ZIP

Create:

`review-q-010-v8-final-closure-<timestamp>.zip`

Then:

1. test ZIP integrity;
2. list/verify expected contents;
3. verify no secret-bearing files were included;
4. calculate ZIP SHA-256;
5. record results in `Verification.md`.

Do not overwrite older review archives.

---

# 23. Git Hygiene and Commit Assessment

Inspect final Git status carefully.

Classify every modified/untracked/staged file as:

- Q-010 required
- prior Q-010 lifecycle artifact
- unrelated pre-existing
- unexpected

There must be no unexplained implementation artifact.

Check for:

- build output
- IDE files
- temporary manifests
- local DB files
- logs
- secrets
- generated test junk
- accidental ZIP nesting
- staged files

If disposable runtime artifacts exist, remove only those clearly generated by this Q-010 execution and safe to remove under repository rules.

Do not remove unrelated user files.

The V8 review must explicitly justify the final:

`Ready for Git Commit: YES/NO`

---

# 24. No New Functional Work

V8 must not introduce new Q-010 functionality.

Permitted changes are limited to:

- implementation approval metadata
- final closure/governance records
- review evidence
- verification outputs
- necessary status synchronization

If verification finds a production defect requiring a code change, do not silently fix it as part of closure. Report the need for a separately reviewed patch unless repository governance explicitly permits and records such a patch cycle.

---

# 25. Git Rules

DO NOT COMMIT.
DO NOT PUSH.

Even if final assessment is:

`Ready for Git Commit: YES`

leave the changes uncommitted for the user.

---

# 26. Acceptance Criteria

V8 passes only if:

1. External V7 Implementation Approval is formally recorded.
2. No material post-V7 implementation drift exists.
3. Requirement traceability is complete.
4. Design traceability is complete.
5. Flyway/database verification passes.
6. DB uniqueness guarantees remain verified.
7. Concurrency verification passes.
8. Transaction rollback verification passes.
9. Durable idempotency remains verified.
10. Immutable history guarantees remain verified.
11. Q-009 authorization integration remains verified.
12. Non-enumeration behavior remains verified.
13. Q-008 disclosure boundary remains verified.
14. Controlled provisioning remains non-Web.
15. Full repository tests/required gates pass or only explicitly proven pre-existing unrelated exceptions remain.
16. No new Q-010 static regression exists.
17. No unapproved scope creep exists.
18. Q-008 business implementation has not started.
19. Final outstanding items are classified.
20. Final review package is self-contained enough for independent review.
21. Artifact hashes are recorded.
22. V8 ZIP exists and passes integrity/security-content checks.
23. Git working tree is fully explained.
24. No commit or push occurred.
25. Final Closure and Ready-for-Commit assessment are explicit and evidence-based.

---

# 27. Final Codex Response Format

Return:

1. `Q-010 V8 Result`
2. Requirement status
3. Architecture status
4. ADR-012 status
5. Implementation Design status
6. Implementation status
7. Final Closure status
8. full verification summary
9. Maven test count/result
10. MySQL/Flyway result
11. concurrency result
12. transaction rollback result
13. security/non-enumeration result
14. Q-008 boundary result
15. post-V7 drift result
16. blocking outstanding items
17. non-blocking outstanding items
18. exact V8 review directory path
19. exact V8 review ZIP path
20. V8 ZIP SHA-256
21. final Git status summary
22. explicit:

`Ready for Architect Final Review: YES`

23. and one of:

`Ready for Git Commit: YES`

or

`Ready for Git Commit: NO — <reason>`

Do not actually commit or push.

The next action is independent external Architect review of the V8 Final Closure package. Only after that review may the user manually commit.
