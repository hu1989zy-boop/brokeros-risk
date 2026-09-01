# Q-013 Action Provenance Foundation Implementation Prompt

**CLEARED FOR USE.** The Product Owner approved Q-013's full governance
chain in chat: Requirement V1 (2026-08-31, including two design choices
at §5.3 explicitly confirmed against a stated principle of prioritizing
extensibility and stability), Architecture V1 (2026-09-01, including
authorizing ADR-015 to be drafted), ADR-015 (Accepted, 2026-09-01), and
Implementation Design V1 (approved 2026-09-01, with implementation
separately authorized in the same session). This is recorded in
`docs/requirements/Q-013-Action-Provenance-Foundation.md` §17 and in each
governing document's own Document Status / Gate section. This work is
governed by `docs/engineering/AI-Engineering-Execution-Protocol.md` —
read it first; it applies to you as much as to Claude Code.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-013-Action-Provenance-Foundation.md` (V1,
   APPROVED) — §5.3 records two design choices the Product Owner
   confirmed directly, one of which reverses this Requirement's own first
   draft (single read contract → two-tier). Both are final.
2. `docs/architecture/q-013-action-provenance-foundation-architecture.md`
   (V1, APPROVED)
3. `docs/adr/ADR-015-action-provenance-foundation.md` (Accepted)
4. `docs/architecture/q-013-action-provenance-foundation-implementation-design.md`
   (V1, APPROVED) — §11.1 is the single authoritative execution-order
   statement; §8.4 is the single authoritative constraint-to-test list.

Also read for context (do not modify): `com.brokeros.risk.security.*`
(Q-009), `com.brokeros.risk.decision.*` (Q-012). Reuse
`AuthorizationGuard`/`ActorContext`/`Capability` (Q-009) and Q-012's
existing narrow `confirmProvenance` provenance-read contract unchanged.
Also reuse the single shared `Clock` bean
(`SecurityModuleConfiguration.securityClock()`) unchanged.

## The confirmed behavior — read the four documents above for full detail; this is a summary, not a substitute

- `ActionRef`: server-generated `act-<canonical-lowercase-UUIDv4>`,
  `CHAR(40)`.
- Recording is the **only** mutating use case. There is no
  approval-workflow transition, correction, or delete use case, port, or
  SQL. An Action, once recorded, never changes — its `status` is always
  `PROPOSED`.
- Recording requires `ActorType.HUMAN` and `action:record`, checked
  immediately after authorization and before the replay check.
- Recording requires exactly **one** originating `DecisionRef` (a
  single reference, not a set — this differs from Decision's own
  at-least-one-`EvidenceRef` requirement; do not build a set/collection
  for this field), validated via Q-012's narrow provenance-read contract,
  accepting any `RECOGNIZED` outcome and rejecting only `NOT_FOUND`.
- Idempotency: `operationId` + SHA-256 semantic fingerprint over raw
  request fields (`decisionRef`, `intentText` strings). The replay check
  strictly precedes content validation and the Q-012 call, and is itself
  strictly preceded only by authorization and the `HUMAN` check.
- `decision_ref` on `action_record` is a validated `CHAR(40)` column, not
  a SQL foreign key to Q-012's table — the only real foreign keys in the
  new schema are intra-module (`action_operation.action_id`,
  `action_access_log.action_id`, both → `action_record.id`).
- Two-tier reads: a narrow, in-process `confirmProvenance(ActorContext,
  ActionRef)` contract that never returns `intentText` (not exposed over
  HTTP), and a separately protected full-detail HTTP read that commits an
  `action_access_log` row **before** returning content, in a short
  dedicated (not database-read-only) transaction. Neither read requires
  `HUMAN`.
- No eligibility-style service for Action itself exists.
- No vendor-specific operation type, taxonomy, or execution semantic may
  be introduced anywhere — Action's intent is free text only.

## Task

Implement Q-013 Action Provenance Foundation exactly as specified in
Implementation Design V1, and only that.

Required deliverable set:

1. `com.brokeros.risk.action` module: domain, application,
   application.port, infrastructure.persistence,
   infrastructure.configuration, infrastructure.observability,
   interfaces.rest packages per Design §3.
2. One additive Flyway migration creating exactly the three tables in
   Design §8, with every constraint traced in §8.4 — including the
   `action_ref`/`decision_ref`/`operation_id`/`recorded_by_actor_ref`
   shape checks, the `source`/`status`/`operation_type`/`outcome` enum
   checks (each a single-value `CHECK` — do not add values not authorized
   by this Requirement, and do not omit the `status` column even though
   it currently has only one value; Design §8.1 and Requirement §5.3
   explain why it must remain a real, extensible column), and the
   `intent_text` byte-bound check. Confirm the actual next unused
   migration version number against the current repository state before
   naming the file. Do not edit any existing migration. **Your own new
   migration test for this module must derive its expected migration
   count dynamically (`flyway.info().pending().length`), never hard-code
   it — see `docs/lessons/2026-08-31-q011-migration-count-test-fix.md`
   and Design §16.4; this has already recurred twice and must not become
   a third occurrence in this module's own test.**
3. The recording application service implementing Design §11.1 exactly,
   including the single-`DecisionRef` (not a set) validation and
   ActorType behavior above.
4. The narrow, in-process `confirmProvenance(ActorContext, ActionRef)`
   contract for future Q-008 consumption (Design §6.2) — implement it but
   do not wire it into Q-008 and do not modify any Q-008 file.
5. The full-detail HTTP read and its access-audit-before-disclosure
   behavior (Design §6.3).
6. Protected HTTP endpoints per Design §10 — `POST /api/actions` and
   `GET /api/actions/{actionRef}` only, no `PATCH`/`PUT` route — returning
   `ApiResponse`, using Bean Validation at the controller boundary, adding
   only the eight ResultCodes in Design §13.
7. The complete test suite in Design §16, including real disposable MySQL
   8.4 integration tests (no H2 substitution, no skipped mandatory test),
   every test named in §8.4's traceability table, and explicit tests
   proving: (a) a replayed recording request does not call the Q-012 port
   a second time; (b) a `SERVICE`-actor context is rejected before the
   replay check ever runs; (c) an unrecognized `DecisionRef` is rejected
   with `ACTION_DECISION_NOT_RECOGNIZED` before any database write; and
   (d) `confirmProvenance`/full-detail read succeed under a non-`HUMAN`
   authorized actor.

## Hard boundaries — do not do these

- Do not modify any file under `com.brokeros.risk.security`,
  `com.brokeros.risk.tradingaccount`, `com.brokeros.risk.evidence`, or
  `com.brokeros.risk.decision`, or any existing Flyway migration.
- Do not implement Risk Case, ActionOutcome, Execution, Account Control
  adapter, Alert, Rule Hit, or Rule Engine code, or any Q-008 wiring.
- Do not add any `ActionSource` other than `MANUAL`, any `ActionStatus`
  other than `PROPOSED`, any status-transition use case (no `APPROVED`,
  `REJECTED`, or any approval workflow), any correction/withdrawal/delete
  use case, any eligibility-style service, any vendor-specific operation
  type/taxonomy, or any cross-module SQL foreign key.
- Do not silently reinterpret, narrow, or broaden anything in the
  governing documents, and do not resolve an apparent contradiction
  yourself — if you find one, stop and report it precisely, per
  `docs/engineering/AI-Engineering-Execution-Protocol.md` §3/§6.
- Do not invent capability grants, deployment manifests, or environment
  credentials.
- Do not stage, commit, or push. Do not touch any existing timestamped
  review package under `review/q-009/` through `review/q-012/`, or any
  `review/q-013/` package already present.

## Required output

After implementation and full verification, create ONE new,
non-overwriting, timestamped review package at
`review/q-013/review-q-013-v5-implementation-<YYYYMMDD-HHMMSS>/` (check
`review/q-013/` first for the actual next unused version number)
containing at minimum: `Summary.md`, `ArchitectureReview.md`,
`DesignTraceability.md` (map each Q013-FR-XXX to the implementing
class/test, and separately confirm every row of Design §8.4 has a
corresponding test), `ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`,
`Verification.md` (exact commands run, environment versions,
pass/fail/skip counts — record honestly, never fabricate a result),
`SecurityReview.md`, `TestInventory.txt`, and `OutstandingItems.md`. Also
add a `docs/lessons/<date>-q-013-implementation.md` entry and update
`docs/skills/` if a reusable pattern emerged.

This review package is for Claude Code's independent implementation
review, not your own sign-off — do not mark Q-013 "complete" or
"approved" in any document; state PASS/FAIL against each Q-013 Acceptance
Criterion (Requirement §10) honestly and list every open question.

Also run the full repository-wide real-MySQL gate with
`Q009_MYSQL_TEST_URL`/`Q010_MYSQL_TEST_URL`/`Q011_MYSQL_TEST_URL`/
`Q012_MYSQL_TEST_URL`/`Q013_MYSQL_TEST_URL` all enabled, and report the
result honestly even if it fails for a reason outside this task's
boundary (as happened once already for Q-012's own closure) — do not
silently work around such a failure; report it precisely.

If Maven, MySQL 8.4, or any other required verification tool is
unavailable in your environment, say so explicitly in `Verification.md`
rather than claiming a check passed.

Stop after producing the review package. Do not begin any other
Requirement.
