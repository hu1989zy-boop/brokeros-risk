# Q-014 Action Outcome Provenance Foundation Implementation Prompt

**CLEARED FOR USE.** The Product Owner approved Q-014's full governance
chain in chat: Requirement V1 (2026-09-01, with all three §5.3
business-scope questions confirmed), and — as one bundle at the
implementation-authorization gate per Decision Authority §16.5-B —
Architecture V1, ADR-016 (Accepted), and Implementation Design V1, with
implementation explicitly authorized. Recorded in
`docs/requirements/Q-014-Action-Outcome-Provenance-Foundation.md` §17 and
in each governing document's own status section. This work is governed by
`docs/engineering/AI-Engineering-Execution-Protocol.md` and
`docs/engineering/Architecture-and-Design-Decision-Principles.md` — read
both first.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-014-Action-Outcome-Provenance-Foundation.md` (V1,
   APPROVED) — §5.3 records the confirmed scope: a HUMAN-recorded outcome
   *fact* (not real execution), free-text outcome (no result taxonomy),
   many-to-one to Action (no one-per-Action constraint), immutable.
2. `docs/architecture/q-014-action-outcome-provenance-foundation-architecture.md`
   (V1, APPROVED)
3. `docs/adr/ADR-016-action-outcome-provenance-foundation.md` (Accepted)
4. `docs/architecture/q-014-action-outcome-provenance-foundation-implementation-design.md`
   (V1, APPROVED) — §11.1 is the single authoritative execution-order
   statement; §8.4 is the single authoritative constraint-to-test list.

Also read for context (do not modify): `com.brokeros.risk.security.*`
(Q-009) and `com.brokeros.risk.action.*` (Q-013). Reuse
`AuthorizationGuard`/`ActorContext`/`Capability` (Q-009) and Q-013's
narrow `confirmProvenance` provenance-read contract unchanged. Reuse the
single shared `Clock` bean (`SecurityModuleConfiguration.securityClock()`)
unchanged.

## The confirmed behavior — summary, not a substitute for the four documents

- `ActionOutcomeRef`: server-generated `aoc-<canonical-lowercase-UUIDv4>`,
  `CHAR(40)`.
- Recording is the **only** mutating use case. No correction, delete, or
  status transition — an ActionOutcome, once recorded, never changes.
  **There is no status column at all** (unlike Q-013's `PROPOSED`; an
  outcome fact has no lifecycle — this follows Decision's shape, not
  Action's).
- Recording requires `ActorType.HUMAN` and `action-outcome:record`,
  checked immediately after authorization and before the replay check.
- Recording requires exactly **one** pertaining `ActionRef` (a single
  reference, not a set), validated via Q-013's narrow provenance-read
  contract, accepting `RECOGNIZED` and rejecting only `NOT_FOUND`.
- Many-to-one: **do not** put a uniqueness constraint on `action_ref`. The
  same Action may have more than one recorded outcome. A real MySQL test
  must prove recording the same `action_ref` twice (different outcome
  refs) succeeds.
- Idempotency: `operationId` + SHA-256 fingerprint over raw request fields
  (`actionRef`, `outcomeText`). Replay check strictly precedes content
  validation and the Q-013 call; itself preceded only by authorization and
  the `HUMAN` check.
- `action_ref` on `action_outcome_record` is a validated `CHAR(40)`
  column, not a SQL foreign key to Q-013's table — the only real FKs are
  intra-module (`action_outcome_operation.action_outcome_id`,
  `action_outcome_access_log.action_outcome_id` → `action_outcome_record.id`).
- Two-tier reads: a narrow in-process `confirmProvenance(ActorContext,
  ActionOutcomeRef)` that never returns `outcomeText` (not on HTTP), and a
  separate full-detail HTTP read that commits an `action_outcome_access_log`
  row **before** returning content, in a short dedicated (not read-only)
  transaction. Neither read requires `HUMAN`.
- Outcome content is free text only — **no execution semantics, no
  external adapter, no structured result taxonomy** anywhere.

## Task

Implement Q-014 Action Outcome Provenance Foundation exactly as specified
in Implementation Design V1, and only that.

Required deliverable set:

1. `com.brokeros.risk.actionoutcome` module: domain, application,
   application.port, infrastructure.persistence, infrastructure.configuration,
   infrastructure.observability, interfaces.rest packages per Design §3.
2. One additive Flyway migration creating exactly the three tables in
   Design §8, with every constraint traced in §8.4 — including the shape
   checks, `source IN ('MANUAL')` and the operation `RECORD`/`CREATED`
   checks, and the `outcome_text` byte bound. **No status column, no join
   table, no history table, and no uniqueness constraint on `action_ref`.**
   Confirm the actual next unused migration version number against the
   current repository state before naming the file. Do not edit any
   existing migration. **This module's own migration test must use a
   fixed-baseline hard-coded count for the `target("N")` transition and a
   dynamic `flyway.info().pending().length` for the unrestricted
   post-baseline migrate — per Design §16.4 and
   `docs/lessons/2026-08-31-q011-migration-count-test-fix.md`. This has
   recurred three times; do not create a fourth occurrence.**
3. The recording application service implementing Design §11.1 exactly,
   including the single-`ActionRef` validation and ActorType behavior.
4. The narrow, in-process `confirmProvenance(ActorContext, ActionOutcomeRef)`
   contract for future Q-008 consumption (Design §6.2) — implement it but
   do not wire it into Q-008 and do not modify any Q-008 file.
5. The full-detail HTTP read and its access-audit-before-disclosure
   behavior (Design §6.3).
6. Protected HTTP endpoints per Design §10 — `POST /api/action-outcomes`
   and `GET /api/action-outcomes/{actionOutcomeRef}` only, no `PATCH`/`PUT`
   — returning `ApiResponse`, using Bean Validation at the controller
   boundary, adding only the eight ResultCodes in Design §13.
7. The complete test suite in Design §16, including real disposable MySQL
   8.4 integration tests (no H2 substitution, no skipped mandatory test),
   every test named in §8.4's traceability table, and explicit tests
   proving: (a) a replayed recording request does not call the Q-013 port
   a second time; (b) a `SERVICE`-actor context is rejected before the
   replay check ever runs; (c) an unrecognized `ActionRef` is rejected
   before any database write; (d) recording the **same** `ActionRef` twice
   with different outcome content both succeed (many-to-one); and (e)
   `confirmProvenance`/full-detail read succeed under a non-`HUMAN`
   authorized actor.

## Hard boundaries — do not do these

- Do not modify any file under `com.brokeros.risk.security`,
  `com.brokeros.risk.tradingaccount`, `com.brokeros.risk.evidence`,
  `com.brokeros.risk.decision`, or `com.brokeros.risk.action`, or any
  existing Flyway migration.
- Do not implement Risk Case, real execution, execution attempt/retry,
  Account Control adapter, MT4/MT5/CRM/bridge/LP vocabulary, a result
  taxonomy, Alert, Rule Hit, Rule Engine, or any Q-008 wiring.
- Do not add any `ActionOutcomeSource` other than `MANUAL`, any status
  column, any correction/withdrawal/delete use case, any eligibility
  service, any uniqueness constraint on `action_ref`, or any cross-module
  SQL foreign key.
- Do not silently reinterpret, narrow, or broaden anything in the
  governing documents; if you find a contradiction, stop and report it
  precisely (Execution Protocol §3/§6).
- Do not invent capability grants, deployment manifests, or credentials.
- Do not stage, commit, or push. Do not touch any existing timestamped
  review package.

## Required output

After implementation and full verification, create ONE new,
non-overwriting, timestamped review package at
`review/q-014/review-q-014-v3-implementation-<YYYYMMDD-HHMMSS>/` (check
`review/q-014/` first for the actual next unused version number)
containing at minimum: `Summary.md`, `ArchitectureReview.md`,
`DesignTraceability.md` (map each Q014-FR-XXX to the implementing
class/test, and confirm every §8.4 row has a corresponding test),
`ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`, `Verification.md`
(exact commands, environment versions, pass/fail/skip counts — honest,
never fabricated), `SecurityReview.md`, `TestInventory.txt`, and
`OutstandingItems.md`. Also add a
`docs/lessons/<date>-q-014-implementation.md` entry and update
`docs/skills/` if a reusable pattern emerged.

Also run the full repository-wide real-MySQL gate with
`Q009_MYSQL_TEST_URL`…`Q014_MYSQL_TEST_URL` all enabled, and report the
result honestly even if it fails for a reason outside this task's boundary
— do not silently work around such a failure.

This review package is for Claude Code's independent implementation review,
not your own sign-off — do not mark Q-014 "complete" or "approved" in any
document; state PASS/FAIL against each Q-014 Acceptance Criterion honestly
and list every open question.

If Maven, MySQL 8.4, or any required tool is unavailable, say so in
`Verification.md` rather than claiming a check passed.

Stop after producing the review package. Do not begin any other
Requirement.
