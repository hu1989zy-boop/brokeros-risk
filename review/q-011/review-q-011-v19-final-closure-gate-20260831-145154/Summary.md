# Final Closure Gate Report

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12, at the Product Owner's explicit request to consolidate the three
review threads below into one report before the Git Commit stage.

## Task ID

**Q-011 — Evidence Provenance Foundation**, plus two prerequisite/follow-on
corrections discovered and resolved during its closure:

1. **AC15 fix** — `Q009MySqlIntegrationTests.java` stale migration-count
   assertion (Q-011's own Acceptance Criterion 15).
2. **Shared Clock microsecond-precision fix** —
   `SecurityModuleConfiguration.securityClock()`, which benefits Q-009,
   Q-010, and Q-011 identically (originally observed as a Q-010 test
   failure, root-caused during Q-011 review).

These are reported together because none of the three is independently
committable without the other two: AC15 is Q-011's own acceptance
criterion, and the Clock fix was discovered only while independently
re-verifying Q-011.

## Stage

**Final Closure** (Implementation Verification and Independent Review are
both complete for all three items below). This report is the Final
Closure output. **Git Commit is the next stage and has not begun** — no
file has been staged, committed, or pushed as part of this report.

## Scope Reviewed

- Q-011 implementation: `com.brokeros.risk.evidence` module (domain,
  application, application.port, infrastructure.persistence,
  infrastructure.configuration, infrastructure.observability,
  interfaces.rest — ~50 files), Flyway
  `V4__create_evidence_provenance_foundation.sql`, and the additive
  ResultCode/test/skill/script changes listed in `GitDiffStat.txt`.
- AC15 fix: the two-line dynamic-migration-count correction in
  `Q009MySqlIntegrationTests.java`.
- Shared Clock fix: the two-line change in `SecurityModuleConfiguration.java`
  (`Clock.systemUTC()` → `Clock.tick(Clock.systemUTC(), Duration.ofNanos(1000))`).

## Files Inspected (direct reading, not report-only)

- `EvidenceRecordingService.java`, `EvidenceCorrectionService.java`,
  `EvidenceDetailReadService.java`, `EvidenceProvenanceQueryService.java`,
  `EvidenceProvenanceView.java`, `EvidenceController.java` — read in full.
- `V4__create_evidence_provenance_foundation.sql` — read in full, every
  constraint cross-checked against Design §8.5.
- `ResultCode.java`, `FlywayMigrationTests.java`, `scripts/verify-static.sh`,
  `docs/skills/development-standards.md` diffs — read in full.
- `Q009MySqlIntegrationTests.java` — read in full both before and after
  the AC15 fix; `git diff` independently confirmed the exact 2-line change.
- `AuthorizedMutationFactory.java`, `AuthorizedMutationContext.java`,
  `JdbcTradingAccountAuthorityMutationAdapter.java`,
  `SecurityModuleConfiguration.java` — read in full to root-cause the
  Clock precision defect and confirm the single-bean fix location; `git
  diff` independently confirmed the exact 2-line change plus import.
- All Flyway migrations V1–V4 — grepped for every `DATETIME`/`TIMESTAMP`
  column to confirm `DATETIME(6)` is used consistently everywhere the
  shared Clock's output is persisted.
- Every `clock.instant()` call site in `src/main/java` (8 total, across
  `security`, `tradingaccount`, `evidence`) — grepped and traced to
  confirm all 8 share the single `securityClock` bean.

## Verification Executed

All verification below used disposable MySQL 8.4.11 containers (never a
shared/persistent database) and was independently re-executed by Claude
Code, not merely read from Codex's reports:

| Run | Environment | Result |
| --- | --- | --- |
| Codex v16 (initial implementation) | macOS/JBR21 | 124 tests, 1 failure (AC15) |
| Claude Code v17 (independent code review + first test run, mount artifact) | Linux/Docker/Java21 | 124 tests, 2 failures + 7 errors (errors = own mount mistake, diagnosed and corrected) |
| Claude Code v17 (corrected mount) | Linux/Docker/Java21 | 124 tests, 2 failures: AC15 (known) + Q010 timestamp-precision (new finding) |
| Codex v18 (AC15 fix) | macOS/JBR21 | 124 tests, 0 failures |
| Claude Code v18 (independent re-verification of AC15 fix) | Linux/Docker/Java21 | 124 tests, 1 failure: Q010 timestamp-precision only — confirms AC15 fix works across environments |
| Codex v9/Q-010 (shared Clock fix) | macOS/JBR21 | 124 tests, 0 failures |
| Claude Code (independent re-verification of Clock fix) | Linux/Docker/Java21 — the environment that twice guaranteed the original failure | **124 tests, 0 failures, 0 errors, 0 skipped** |

The last row is the decisive result: every known failure is now resolved,
confirmed in the one environment where the two defects were provably
reproducible, not just on a host where they happened not to manifest.

Static/diff gates (`scripts/verify-static.sh`, `git diff --check`) passed
in every closure round.

## Requirement Status

Q-011 Requirement V3 — **APPROVED** (2026-08-28). All 15 Acceptance
Criteria now PASS, including AC15 (previously FAIL, now fixed and
independently confirmed). No requirement text was changed as part of
either fix.

## Architecture Status

Architecture V4 — **APPROVED**; ADR-013 — **ACCEPTED, amendment
RE-ACCEPTED** (2026-08-28). Implementation independently confirmed
compliant: Q-010 is reused, not extended, for subject eligibility; module
boundaries (`evidence` package structure) match Design §3; the
`EvidenceProvenanceView` narrow-read contract is a structural guarantee
(no `observationText`/`correctionReason` field exists), matching ADR-013's
consumer-boundary decision. Neither fix touched architecture, ADRs, or
module boundaries — the Clock fix is a single-bean change with no new
dependency direction or module coupling.

## Design Compliance Status

Implementation Design V5 — **APPROVED**. Independently confirmed via
direct code tracing (not test results alone):
- §11.1/§11.4 execution ordering: exact match in both
  `EvidenceRecordingService` and `EvidenceCorrectionService` (authorize →
  HUMAN check → fingerprint → replay check → content validation → Q-010
  call [Record only] → mutation).
- §8.5 constraint-to-test table: every constraint present and correctly
  guarded (including the `IS NOT NULL`-guarded bidirectional CHECKs that
  avoid MySQL's three-valued-logic pitfall).
- §10.3: the narrow provenance contract is in-process only, no REST
  endpoint — confirmed via `EvidenceController`'s exact 3-route surface.

No design deviation was found or introduced by either fix.

## Test Status

124/124 tests passing, 0 errors, 0 skips, confirmed independently in two
different host/JVM/OS combinations (macOS/JBR21 by Codex, Linux/Docker/
Temurin21 by Claude Code) for every one of the three closure items.

## Findings

1. Q-011's own implementation: **no defect found**, in either code review
   or independently executed tests.
2. AC15: a genuine, pre-existing, Q-011-unrelated `Q009MySqlIntegrationTests`
   staleness (hard-coded post-V1 migration count), correctly left untouched
   by the original implementation per the "never modify Q-009" boundary,
   then fixed under separate narrow authorization using a dynamically
   derived count (`flyway.info().pending().length`) instead of a literal,
   so it will not go stale again with future migrations.
3. Shared Clock precision: a genuine, pre-existing, environment/clock-
   resolution-dependent latent defect (not fabricated or environment
   noise — reproduced deterministically twice in Linux/Docker before the
   fix, and resolved deterministically after it), root-caused to the
   single shared `Clock` bean minting instants at full JVM precision while
   every consuming `DATETIME(6)` column caps at microseconds. Fixed at the
   single source (the bean itself), which also protects Q-009 and Q-011
   from the identical latent risk without touching either module's own
   code.

## Remaining Risks

- Flyway 11.7.2 emits a warning that MySQL 8.4 exceeds its formally listed
  tested-support ceiling (8.1). Pre-existing since Q-009/Q-010, not
  introduced or worsened by Q-011 or either fix, not acted on (no
  dependency change was in scope for any of the three tasks). Tracked as
  an existing, monitored risk, not a defect.
- No commit has been made. All three items remain only in the working
  tree.

## Out-of-Scope Issues (tracked, not acted on)

- Q-008 (Risk Case) implementation remains blocked: Trusted Actor (Q-009)
  and Trading Account (Q-010) providers exist; Evidence (Q-011) is now
  ready pending commit; Decision/Action/ActionOutcome authoritative
  providers have no Requirement drafted yet.
- The previously approved "AI Context Bootstrap" documentation task
  (`docs/context/*`, `CLAUDE.md`) was authorized earlier but never
  executed; still pending, unrelated to Q-011.
- Frontend work (planned: Flutter) has not started.

## Recommendation

Accept all three items (Q-011 implementation, AC15 fix, shared Clock fix)
as verified and ready to proceed to the **Git Commit** stage. Per protocol
§14, Git Commit itself requires a separate, explicit Product Owner
instruction — this report does not authorize it. One open administrative
question remains before that stage begins: whether to bundle everything
into one commit (matching the precedent set by the original Q-009/Q-010
commits) or split the shared-Clock fix into its own `fix:` commit separate
from Q-011's `feat:` commit (matching this repository's existing use of
both commit types). That decision belongs to the Product Owner.

## Gate Decision

**PASS.**

This Final Closure gate is satisfied: all stage exit criteria (independent
review completed, all findings resolved, all verification independently
reproduced, no open defect, no undocumented architecture/design deviation)
are met with evidence, not assumption. The Git Commit stage has not begun
and awaits separate explicit instruction per protocol §14.
