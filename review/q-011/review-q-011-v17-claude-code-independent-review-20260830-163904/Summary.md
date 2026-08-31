# Q-011 V17 Claude Code Independent Implementation Review

- Review ID: `Q-011-V17-CLAUDE-CODE-INDEPENDENT-REVIEW-20260830-163904`
- Subject: `review/q-011/review-q-011-v16-implementation-20260830-161236/`
  (Codex's implementation package) and the actual working-tree diff it
  produced.
- Reviewer: Claude Code, per the role assigned in
  `prompts/Claude-Code-BrokerOS-Risk-Project-Handover-Prompt.md` §7:
  independent review, not acceptance of Codex's own conclusions.
- Method: direct reading of the actual source diff (not only the review
  package's narrative) — the recording and correction services, the V4
  migration DDL, the REST controller, the provenance view type, and every
  file Codex modified outside the new `evidence` module — cross-checked
  line by line against Implementation Design V5 §11.1/§11.4/§8.5/§13 and
  Requirement V3.
- Environment update: this session's local Java is 11 and `mvn` is not on
  `PATH`, so code-level review below could not initially be backed by a
  re-executed test suite. That gap has since been closed — see
  `IndependentTestExecution.md` in this same directory — by running the
  full Maven test suite in a disposable `maven:3.9.9-eclipse-temurin-21-alpine`
  container against a disposable MySQL 8.4.11 container. Both the
  code-level findings below and the independently executed test results
  are now available; they agree.

## Verdict

**The implementation is a faithful, correct realization of Design V5.** I
found no defect in the authorized Q-011 scope. Codex's one reported FAIL
(Acceptance Criterion 15) is independently confirmed to be a genuine,
pre-existing defect in an *unchanged* Q-009 test, not a Q-011 defect — see
"AC15 Investigation" below.

**Q-011 implementation is NOT YET marked complete, approved, or ready for
commit.** That remains the Product Owner's decision. This package is my
independent recommendation, not a self-approval — consistent with every
prior gate in this task.

## What I Independently Verified (not just read in Codex's report)

1. **`EvidenceRecordingService.doRecord`** — traced execution line by line
   against Design §11.1's nine-step canonical order: authorize →
   `HUMAN` check → fingerprint from raw fields → operation-ID replay check
   (returns immediately on match, *before* content validation or the
   Q-010 call) → content validation → Q-010 call using the *same*
   `ActorContext` → accepts `ELIGIBLE_FOR_NEW_ASSOCIATION` and
   `RECOGNIZED_NOT_ELIGIBLE`, rejects only `NOT_RECOGNIZED` → mutation.
   Exact match. This is the precise ordering that took four governance
   rounds to specify correctly, and the code implements it correctly.
2. **`EvidenceCorrectionService.doCorrect`** — same authorize → `HUMAN` →
   fingerprint → replay-check-first order. Confirmed the replay check
   returns *before* the target-`ACTIVE` check is ever reached, and
   confirmed **no Q-010 import or call exists anywhere in this class** —
   correction never calls Q-010, for a new operation or a replay, exactly
   as decided. The new record's subject is not an input field on
   `CorrectEvidenceSpec` at all — subject-copying is structural, not just
   validated.
3. **`V4__create_evidence_provenance_foundation.sql`** — checked every row
   of Design §8.5 against the actual DDL: all four tables' primary keys,
   unique constraints (including the nullable-unique
   `uk_evidence_record_supersedes`), both self-FKs with `ON DELETE
   RESTRICT`, every enum/format `CHECK`, and critically the
   `before_status`/`after_status`/`reason` bidirectional checks all use an
   explicit `IS NOT NULL` guard before the value comparison — avoiding the
   exact MySQL three-valued-logic pitfall (`NULL = 'ACTIVE'` evaluates to
   `UNKNOWN`, which a bare `CHECK` does not reject) that Codex's own
   Verification.md reports finding and fixing. I confirmed the fix is
   actually present in the shipped DDL, not just claimed.
4. **`EvidenceProvenanceView`** — confirmed it has no `observationText` or
   `correctionReason` field at the type level (not just by convention),
   and its compact constructor enforces that a `NOT_FOUND` outcome cannot
   carry any subject/source/actor/time/status metadata. This makes the
   narrow-contract guarantee a structural property, not a habit future
   code could accidentally violate.
5. **`EvidenceController`** — exposes exactly three routes (record,
   correct, full-detail read); the narrow provenance contract is *not*
   exposed over HTTP, matching Design §10.3 ("not a REST endpoint").
   `@ConditionalOnWebApplication(type = SERVLET)` is present, addressing
   the regression Codex's own Verification.md reports finding against
   Q-010's non-Web bootstrap command. Actor identity comes only from
   `ActorContextProvider.currentContext()`; no request DTO field can
   supply an actor, status, timestamp, or generated reference.
6. **Files modified outside the new module** — `ResultCode.java` (diff
   reviewed): adds exactly the nine ResultCodes Design §13 specifies, no
   others, no changes to existing entries.
   `FlywayMigrationTests.java` (diff reviewed): adds one new test for V4's
   shape; does not alter existing V1–V3 assertions.
   `scripts/verify-static.sh` (diff reviewed): updates the expected
   migration count from 3 to 4 (mechanically necessary once V4 exists) and
   adds Q-011-specific static checks mirroring the existing Q-010 pattern.
   `docs/skills/development-standards.md` (diff reviewed): adds one
   genuinely reusable, non-Q-011-specific rule about MySQL `CHECK`
   three-valued logic — appropriate skill-file content, not scope creep.
   None of these four changes exceeds what implementing Q-011 correctly
   required.

## AC15 Investigation (the one reported FAIL)

I independently read `Q009MySqlIntegrationTests.java` and confirmed:
`git diff --stat` on that file is **empty** — Codex did not touch it, as
required. The failing assertion migrates a fresh schema to V1, then calls
an unrestricted `migrate()` and asserts exactly `1` further migration
executed. That assertion was already logically stale the moment Q-010's
V3 migration was added (the correct count became 2, not 1); with Q-011's
V4 now also present, the count is 3, which is what the failure message
reports (`expected: 1 but was: 3`). This is a **pre-existing Q-009 test
defect, unrelated to Q-011's own correctness**, that Q-011's hard boundary
(never modify Q-009) correctly prevented Codex from touching.

**Recommendation:** authorize a narrow, separate Q-009 test-maintenance
fix (updating the hard-coded expected count, or making it dynamic) as
routine test upkeep — not a new Q-009 business-behavior Requirement. This
does not block Q-011's own correctness and is not a defect introduced by
this implementation.

## Independently Executed (update)

Using Docker to run Java 21/Maven, the full test suite was actually
re-executed against a disposable MySQL 8.4.11 container — see
`IndependentTestExecution.md`. Result: **124 tests, 2 failures, 0 errors**,
matching Codex's own Q-011-owned test results (37/37) exactly, plus
confirming the known AC15 (`Q009MySqlIntegrationTests`) failure. It also
surfaced one **new** finding Codex's own report did not contain: a
pre-existing, environment/clock-dependent timestamp-precision mismatch in
`Q010BootstrapMySqlIntegrationTests`'s idempotent-replay assertion —
unrelated to Q-011, outside Q-011's change boundary, not something Codex's
macOS host exposed. Details, exact log output, and root-cause analysis are
in `IndependentTestExecution.md`.

## Recommendation to the Product Owner

1. Q-011's implementation, as authorized by Design V5, is sound —
   confirmed now by both code inspection and independently executed
   tests. I recommend accepting it.
2. Separately authorize a small Q-009 test-maintenance fix for the stale
   migration-count assertion (AC15) — this is unrelated to Q-011 and
   should not block Q-011's closure.
3. Separately decide how to handle the newly found Q010 bootstrap
   timestamp-precision replay mismatch (see `IndependentTestExecution.md`)
   — also unrelated to Q-011 and outside its change boundary, but a real,
   reproducible pre-existing gap worth a decision (fix, waive, or record
   as a lesson/follow-up ticket).
4. Final closure (Lessons Learned finalization, review-package completion
   per AGENTS.md's mandatory set, and the actual `git add`/commit) remains
   a separate, later step requiring your explicit direction — nothing in
   this review authorizes staging or committing.
