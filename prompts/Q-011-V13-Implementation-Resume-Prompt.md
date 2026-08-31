# Q-011 V13 Implementation Resume Prompt

**CLEARED FOR USE.** The Product Owner confirmed, in chat, on 2026-08-28,
all four items this Prompt originally required: the Requirement V3
status/reference wording fix is acceptable; the ADR-013 status wording fix
is acceptable; the Requirement V3 / Architecture V4 / ADR-013 amendment /
Implementation Design V5 approvals remain in force; and Codex is
explicitly authorized to begin implementing Q-011 per Implementation
Design V5. This confirmation is recorded in
`review/q-011/review-q-011-v13-fifth-governance-status-correction-20260828-200817/`
and in the governing documents' own Document Status sections.

---

This is the fifth resume prompt for Q-011 Evidence Provenance Foundation.
You have halted correctly four times before writing any code: six defects,
then five, then four via a written blocker report, then three formal
status/reference contradictions caught on your most recent read of the
round-four-approved documents. Every one of the four rounds was real.
Thank you for continuing to stop rather than guessing.

Once the confirmations above are given, read in this exact order and treat
each as authoritative over anything below it (Design §1.1's priority rule):

1. `docs/requirements/Q-011-Evidence-Provenance-Foundation.md` (V3,
   APPROVED — §19 is now a historical drafting note, not a live "draft
   candidate" claim; §17 is the authoritative current gate)
2. `docs/architecture/q-011-evidence-provenance-foundation-architecture.md`
   (V4, APPROVED)
3. `docs/adr/ADR-013-evidence-provenance-foundation.md` (accepted original
   + amendment RE-ACCEPTED — the introduction now correctly states this;
   the Status line and Approval Boundary are authoritative if anything
   ever appears to disagree)
4. `docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`
   (V5, APPROVED) — §11.1 and §11.4 remain the single authoritative
   statement of execution order; §8.5 remains the single authoritative
   constraint-to-test list. §20.10 is the complete four-document,
   round-four finding-by-finding record if you want the full history.

Also read for context (do not modify): the existing `security` module
(`com.brokeros.risk.security.*`) and `tradingaccount` module
(`com.brokeros.risk.tradingaccount.*`). Reuse
`TradingAccountReferenceEligibilityService`, `TradingAccountCapabilities`,
`AuthorizationGuard`, `ActorContext`, and `Capability` unchanged.

## The confirmed behavior — unchanged in substance since round four; only
## how it was described in prose was corrected this round

- Evidence subject: accept `ELIGIBLE_FOR_NEW_ASSOCIATION` **or**
  `RECOGNIZED_NOT_ELIGIBLE`; reject only `NOT_RECOGNIZED`. ResultCode
  `EVIDENCE_SUBJECT_NOT_RECOGNIZED`.
- `ActorType.HUMAN` required for Record and Correct only. Provenance-read
  and Full-detail-read require `evidence:read` under Q-009 authorization
  but no additional actor-type restriction — any authorized `ActorType`,
  including `SERVICE`, may call them.
- Full-detail read commits `evidence_access_log` before returning content,
  in a short dedicated (**not** database-read-only) transaction.
- Q-008 may consume only the narrow provenance contract, never full-detail
  — a consumer-contract limitation, not an actor-type rule.
- Correction copies the target's subject, requires a mandatory reason,
  never calls Q-010 (new operation or replay), and its replay check
  precedes the target-`ACTIVE` check.
- Authorization and the `HUMAN` check (where required) always precede the
  replay check and are never skipped, including on replay.

## Task

Implement Q-011 Evidence Provenance Foundation exactly as specified in
Implementation Design V5, and only that.

Required deliverable set:

1. `com.brokeros.risk.evidence` module: domain, application,
   application.port, infrastructure.persistence, infrastructure.configuration,
   interfaces.rest packages per Design §3.
2. Flyway migration `V4__create_evidence_provenance_foundation.sql`
   creating exactly the four tables in Design §8, with every constraint
   traced in §8.5 — including the `source`/`status`/`operation_type` enum
   checks, all UUIDv4-shape checks, and the `before_status`/`after_status`
   bidirectional checks. Do not edit V1–V3.
3. Recording and correction application services implementing §11.1/§11.4
   exactly, including the confirmed subject-bar and ActorType behavior
   above.
4. The narrow, in-process `confirmProvenance(ActorContext, EvidenceRef)`
   contract for future Q-008 consumption (Design §10.3) — implement it but
   do not wire it into Q-008 and do not modify any Q-008 file.
5. The full-detail HTTP read and the provenance-read use case, neither
   requiring `HUMAN`.
6. Protected HTTP endpoints per Design §10.1–§10.2, returning `ApiResponse`,
   using Bean Validation at the controller boundary, adding only the
   ResultCodes in Design §13.
7. The complete test suite in Design §16, including real disposable MySQL
   8.4 integration tests (no H2 substitution, no skipped mandatory test),
   every test named in §8.5's traceability tables, the forced-failure
   rollback tests, and explicit tests proving: (a) a replayed recording
   request does not call the Q-010 port a second time; (b) a replayed
   correction request succeeds even when the target is already
   `SUPERSEDED`; (c) a `SERVICE`-actor context is rejected before the
   replay check ever runs for Record/Correct; (d) recording a
   `RECOGNIZED_NOT_ELIGIBLE` subject succeeds and only `NOT_RECOGNIZED` is
   rejected; and (e) a provenance-read or full-detail-read use case
   succeeds under a non-`HUMAN` authorized actor.

## Hard boundaries — do not do these

- Do not modify any file under `com.brokeros.risk.security` or
  `com.brokeros.risk.tradingaccount`, or any existing Flyway migration
  (V1–V3).
- Do not implement Decision, Action, ActionOutcome, Rule Engine, Risk Case,
  or any Q-008 code or wiring.
- Do not add any Evidence source other than `MANUAL`, any `SERVICE`-actor
  authoring path, any subject type other than `TRADING_ACCOUNT`, any
  file/blob/document storage, or any Kafka topic or Redis key.
- Do not silently reinterpret, narrow, or broaden anything in the governing
  documents, and do not resolve an apparent contradiction yourself — if you
  find one, including between this prompt and the Design, stop and report
  it precisely, exactly as you have done four times already.
- Do not invent capability grants, deployment manifests, or environment
  credentials.
- Do not stage, commit, or push. Do not touch `review/review-history/` or
  any existing timestamped review package under `review/q-007/` through
  `review/q-010/`, or any `review/q-011/` package already present.

## Required output

After implementation and full verification, create ONE new, non-overwriting,
timestamped review package at
`review/q-011/review-q-011-v14-implementation-<YYYYMMDD-HHMMSS>/` (check
`review/q-011/` first for the actual next unused version — do not assume
this number is still correct by the time you run) containing at minimum:
`Summary.md`, `ArchitectureReview.md`, `DesignTraceability.md` (map each
Q011-FR-XXX to the implementing class/test, and separately confirm every
row of Design §8.5 has a corresponding test), `ProjectTree.txt`,
`GitStatus.txt`, `GitDiffStat.txt`, `Verification.md` (exact commands run,
environment versions, pass/fail/skip counts — record honestly, never
fabricate a result), `SecurityReview.md`, `TestInventory.txt`, and
`OutstandingItems.md`. Also add a
`docs/lessons/<date>-q-011-implementation.md` entry and update
`docs/skills/` if a reusable pattern emerged, per AGENTS.md.

This review package is for Claude Code's independent implementation review,
not your own sign-off — do not mark Q-011 "complete" or "approved" in any
document; state PASS/FAIL against each Acceptance Criterion honestly and
list every open question.

If Maven, MySQL 8.4, or any other required verification tool is unavailable
in your environment, say so explicitly in Verification.md rather than
claiming a check passed. If you get blocked by an environment limitation,
report the exact blocker instead of working around it with a weaker check.

Stop after producing the review package. Do not begin any other Requirement.
