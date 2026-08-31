# Q-012 Decision Provenance Foundation Implementation Prompt

**CLEARED FOR USE.** The Product Owner approved Q-012's full governance
chain in chat: Requirement V1 (2026-08-31), Architecture V1 (2026-08-31,
including authorizing ADR-014 to be drafted), ADR-014 (Accepted,
2026-08-31), and Implementation Design V1 (2026-08-31, PASS, with
explicit implementation authorization in the same message: "PASS，批准
Implementation Design V1，授权开始实施"). This is recorded in
`docs/requirements/Q-012-Decision-Provenance-Foundation.md` §17 and in
each governing document's own Document Status / Gate section. This work
is governed by `docs/engineering/AI-Engineering-Execution-Protocol.md` —
read it first; it applies to you as much as to Claude Code.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-012-Decision-Provenance-Foundation.md` (V1,
   APPROVED)
2. `docs/architecture/q-012-decision-provenance-foundation-architecture.md`
   (V1, APPROVED) — note its Document Status section: it corrects a
   Requirement §9 technical aside about a cross-module foreign key; the
   Architecture's version is authoritative.
3. `docs/adr/ADR-014-decision-provenance-foundation.md` (Accepted)
4. `docs/architecture/q-012-decision-provenance-foundation-implementation-design.md`
   (V1, APPROVED) — §11.1 is the single authoritative execution-order
   statement; §8.5 is the single authoritative constraint-to-test list.
   Treat both as canonical over any other section's restatement.

Also read for context (do not modify): `com.brokeros.risk.security.*`
(Q-009), `com.brokeros.risk.tradingaccount.*` (Q-010), and
`com.brokeros.risk.evidence.*` (Q-011). Reuse
`AuthorizationGuard`/`ActorContext`/`Capability` (Q-009),
`TradingAccountReferenceEligibilityService.validateForNewRiskCaseAssociation`
(Q-010), and Evidence's existing narrow provenance-read contract (Q-011)
unchanged. Also reuse the single shared `Clock` bean
(`SecurityModuleConfiguration.securityClock()`) unchanged — do not
introduce a second `Clock` bean.

## The confirmed behavior — read the four documents above for full detail; this is a summary, not a substitute

- `DecisionRef`: server-generated `dec-<canonical-lowercase-UUIDv4>`,
  `CHAR(40)`.
- Recording is the **only** mutating use case. There is no correction,
  supersession, or delete use case, port, or SQL. A Decision, once
  recorded, never changes.
- Recording requires `ActorType.HUMAN` and `decision:record`, checked
  immediately after authorization and before the replay check (Design
  §11.1).
- Recording requires a subject (`TradingAccountRef`) validated via Q-010,
  accepting `ELIGIBLE_FOR_NEW_ASSOCIATION` or `RECOGNIZED_NOT_ELIGIBLE`,
  rejecting only `NOT_RECOGNIZED`.
- Recording requires at least one `EvidenceRef`, each validated via
  Q-011's narrow provenance-read contract, accepting `ACTIVE` or
  `SUPERSEDED`, rejecting only `NOT_FOUND`. Duplicate references in one
  request are de-duplicated, not rejected. No requirement that referenced
  Evidence share the Decision's own subject.
- Idempotency: `operationId` + SHA-256 semantic fingerprint over raw
  request fields. The replay check strictly precedes content validation
  and both external calls (Design §11.1 step 4) and is itself strictly
  preceded only by authorization and the `HUMAN` check.
- Cross-module references (`subject_ref`, and every `evidence_ref` in
  `decision_evidence_reference`) are validated `CHAR` columns, **not**
  SQL foreign keys to Q-010's or Q-011's tables (Architecture §8/ADR-014)
  — the only real foreign keys in the new schema are intra-module
  (`decision_evidence_reference.decision_id`,
  `decision_operation.decision_id`, `decision_access_log.decision_id`,
  all → `decision_record.id`).
- Two-tier reads: a narrow, in-process
  `confirmProvenance(ActorContext, DecisionRef)` contract that never
  returns `conclusionText` (not exposed over HTTP), and a separately
  protected full-detail HTTP read that commits a `decision_access_log`
  row **before** returning content, in a short dedicated (not
  database-read-only) transaction. Neither read requires `HUMAN`.
- No eligibility-style service for Decision itself exists — only
  "recognized or not."

## Task

Implement Q-012 Decision Provenance Foundation exactly as specified in
Implementation Design V1, and only that.

Required deliverable set:

1. `com.brokeros.risk.decision` module: domain, application,
   application.port, infrastructure.persistence,
   infrastructure.configuration, infrastructure.observability,
   interfaces.rest packages per Design §3.
2. One additive Flyway migration creating exactly the four tables in
   Design §8, with every constraint traced in §8.5 — including the
   `decision_ref`/`subject_ref`/`evidence_ref`/`operation_id`/
   `recorded_by_actor_ref` shape checks, the `source`/`operation_type`/
   `outcome` enum checks (each a single-value `CHECK` for now, per Design
   §8.3's explicit reasoning — do not add values not authorized by this
   Requirement), the `conclusion_text` byte-bound check, and the
   `decision_evidence_reference` uniqueness constraint. Confirm the
   actual next unused migration version number against the current
   repository state before naming the file — do not assume a specific
   number from the Design document. Do not edit any existing migration.
3. The recording application service implementing Design §11.1 exactly,
   including the confirmed subject/Evidence validation bars and ActorType
   behavior above.
4. The narrow, in-process `confirmProvenance(ActorContext, DecisionRef)`
   contract for future Q-008 consumption (Design §6.2) — implement it but
   do not wire it into Q-008 and do not modify any Q-008 file.
5. The full-detail HTTP read and its access-audit-before-disclosure
   behavior (Design §6.3).
6. Protected HTTP endpoints per Design §10 — `POST /api/decisions` and
   `GET /api/decisions/{decisionRef}` only, no `PATCH`/`PUT` route —
   returning `ApiResponse`, using Bean Validation at the controller
   boundary, adding only the ten ResultCodes in Design §13.
7. The complete test suite in Design §16, including real disposable MySQL
   8.4 integration tests (no H2 substitution, no skipped mandatory test),
   every test named in §8.5's traceability table, the forced-failure
   rollback test, and explicit tests proving: (a) a replayed recording
   request does not call the Q-010 or Q-011 ports a second time; (b) a
   `SERVICE`-actor context is rejected before the replay check ever runs;
   (c) recording with an evidence reference in `SUPERSEDED` status
   succeeds, and only a `NOT_FOUND` Evidence outcome is rejected; (d) an
   empty evidence-reference set is rejected before any Q-010/Q-011 call;
   and (e) `confirmProvenance`/full-detail read succeed under a
   non-`HUMAN` authorized actor.

## Hard boundaries — do not do these

- Do not modify any file under `com.brokeros.risk.security`,
  `com.brokeros.risk.tradingaccount`, or `com.brokeros.risk.evidence`, or
  any existing Flyway migration.
- Do not implement Risk Case, Action, ActionOutcome, Alert, Rule Hit, or
  Rule Engine code, or any Q-008 wiring.
- Do not add any `DecisionSource` other than `MANUAL`, any correction/
  supersession/delete use case, any eligibility-style service for
  Decision, any subject type other than what Q-010 already validates, or
  any cross-module SQL foreign key.
- Do not silently reinterpret, narrow, or broaden anything in the
  governing documents, and do not resolve an apparent contradiction
  yourself — if you find one, stop and report it precisely, per
  `docs/engineering/AI-Engineering-Execution-Protocol.md` §3/§6.
- Do not invent capability grants, deployment manifests, or environment
  credentials.
- Do not stage, commit, or push. Do not touch any existing timestamped
  review package under `review/q-009/` through `review/q-011/`, or any
  `review/q-012/` package already present.

## Required output

After implementation and full verification, create ONE new,
non-overwriting, timestamped review package at
`review/q-012/review-q-012-v5-implementation-<YYYYMMDD-HHMMSS>/` (check
`review/q-012/` first for the actual next unused version number — do not
assume this number is still correct by the time you run) containing at
minimum: `Summary.md`, `ArchitectureReview.md`, `DesignTraceability.md`
(map each Q012-FR-XXX to the implementing class/test, and separately
confirm every row of Design §8.5 has a corresponding test),
`ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`, `Verification.md`
(exact commands run, environment versions, pass/fail/skip counts — record
honestly, never fabricate a result), `SecurityReview.md`,
`TestInventory.txt`, and `OutstandingItems.md`. Also add a
`docs/lessons/<date>-q-012-implementation.md` entry and update
`docs/skills/` if a reusable pattern emerged.

This review package is for Claude Code's independent implementation
review, not your own sign-off — do not mark Q-012 "complete" or
"approved" in any document; state PASS/FAIL against each Q-012
Acceptance Criterion (Requirement §10) honestly and list every open
question.

If Maven, MySQL 8.4, or any other required verification tool is
unavailable in your environment, say so explicitly in `Verification.md`
rather than claiming a check passed. If you get blocked by an environment
limitation, report the exact blocker instead of working around it with a
weaker check.

Stop after producing the review package. Do not begin any other
Requirement.
