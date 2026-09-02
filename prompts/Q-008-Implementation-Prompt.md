# Q-008 Risk Case Foundation Implementation Prompt

**CLEARED FOR USE — Product Owner authorized implementation 2026-09-02.**
Q-008's Requirement, Architecture, ADR-010, and Implementation Design V4
were approved earlier (2026-08-25/28). Its Implementation Gate (§26) was
blocked solely by two prerequisites — real reference providers and an
authenticated Actor/authorization provider — both of which now exist
(Q-009 authorization; Q-010 Trading Account; Q-011 Evidence; Q-012
Decision; Q-013 Action; Q-014 ActionOutcome). The Product Owner confirmed
readiness, chose the strict subject-eligibility bar, and explicitly
authorized implementation on 2026-09-02. This work is governed by
`docs/engineering/AI-Engineering-Execution-Protocol.md` and
`docs/engineering/Architecture-and-Design-Decision-Principles.md` — read
both first.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-008-Requirement.md` (APPROVED) — the business
   scope, lifecycle, and acceptance criteria.
2. `docs/adr/ADR-010-risk-case-foundation.md` (Accepted).
3. `docs/architecture/q-008-risk-case-foundation-implementation-design.md`
   (V4, APPROVED) — the authoritative implementation spec: aggregate
   boundary (§4), lifecycle operations (§5), CaseNumber (§6), resolution
   cycles (§7), persistence/table catalog (§8), transaction/audit (§9),
   use cases (§10), API (§11), security (§12), concurrency (§13),
   verification (§16).
4. `docs/architecture/q-008-risk-case-foundation-provider-binding-addendum.md`
   (V5 binding addendum, APPROVED) — the single authoritative binding of
   Design §14's abstract ports to the shipped provider services, the
   strict subject-eligibility decision, and the one new ResultCode.

Also read for context and **reuse unchanged** (do not modify any file in
these modules): `com.brokeros.risk.security.*` (Q-009),
`com.brokeros.risk.tradingaccount.*` (Q-010), `com.brokeros.risk.evidence.*`
(Q-011), `com.brokeros.risk.decision.*` (Q-012), `com.brokeros.risk.action.*`
(Q-013), `com.brokeros.risk.actionoutcome.*` (Q-014). Reuse the single
shared `Clock` bean (`SecurityModuleConfiguration.securityClock()`)
unchanged.

## Provider binding (from the V5 addendum — authoritative)

Implement Q-008's own read-only reference-query ports as thin adapters
delegating to these shipped in-process services, each called with the
caller's own `ActorContext`; never accept an unchecked string as proof a
reference exists:

- `TradingAccountReferenceEligibilityService.validateForNewRiskCaseAssociation(ActorContext, TradingAccountRef)`
  — **STRICT bar**: accept only `ELIGIBLE_FOR_NEW_ASSOCIATION`; reject
  `RECOGNIZED_NOT_ELIGIBLE` with the new `RISK_CASE_SUBJECT_NOT_ELIGIBLE`
  (422); reject `NOT_RECOGNIZED` with `RISK_CASE_REFERENCE_NOT_FOUND`.
- `EvidenceProvenanceQueryService.confirmProvenance(ActorContext, EvidenceRef)`
  — accept `RECOGNIZED` (incl. superseded Evidence); reject `NOT_FOUND`
  with `RISK_CASE_REFERENCE_NOT_FOUND`.
- `DecisionProvenanceQueryService.confirmProvenance(...)` — same; the view
  carries `evidenceRefs`.
- `ActionProvenanceQueryService.confirmProvenance(...)` — same; the view
  carries the originating `decisionRef`, which `associateAction` must use
  to enforce "Action originates from a Decision associated to this case".
- `ActionOutcomeProvenanceQueryService.confirmProvenance(...)` — same; the
  view carries the pertaining `actionRef`, which
  `recordActionOutcomeReference` must use to enforce the outcome pertains
  to an associated Action.
- Any provider-unavailability exception → `RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE`
  (503), fail-closed.
- Authorization: `AuthorizationGuard.requireAllowed(ActorContext, Capability)`
  before any case data is loaded or mutated; `ActorRef` only from
  `ActorContext`, never from request body/header; capabilities per Design
  §12 (create, read, assign, associate, review, resolve, close, cancel,
  reopen, note).

## Task

Implement Q-008 Risk Case Foundation exactly as specified in Implementation
Design V4 plus the V5 binding addendum, and only that. This is the
aggregate-root case-management module — larger than the provenance
foundations; implement the full approved design:

1. `com.brokeros.risk.riskcase` module (domain / application /
   application.port / infrastructure.persistence / infrastructure.configuration
   / infrastructure.observability / interfaces.rest) per Design §3.
2. The `RiskCase` aggregate root and all domain types, enums, immutable
   history entities, and value objects in Design §4; every named lifecycle
   operation and non-transition operation in Design §5 (no public
   `setStatus`); CaseNumber per §6; resolution cycles/records per §7.
3. The additive Flyway migration for the full Q-008 table catalog in
   Design §8 (root + append-only history/association/resolution/audit
   tables), with every constraint, the optimistic `version`, the unique
   `(case_id, cycle_no)` resolution constraint, the global unique
   `decision_ref` primary-association constraint, and the create
   idempotency-key/request-hash columns. Confirm the actual next unused
   migration version number first. Do not edit any existing migration.
4. The application services as transaction owners implementing the exact
   material-mutation sequence in Design §9.2 (authorize → load snapshot →
   validate references via the bound ports → domain operation with
   `expectedVersion` → optimistic CAS `UPDATE ... WHERE id=? AND version=?`
   → require exactly one row → append history → append one Audit Record →
   commit), with the §9.3 fail-closed rollback behavior and §9.5 read
   audit (`RISK_CASE_VIEWED` before returning content; fail closed).
5. The reference-query port adapters per the binding above.
6. The HTTP endpoints per Design §11, returning `ApiResponse`, Bean
   Validation at the boundary, `ActorRef` only from `ActorContext`,
   adding only the Design §11.4 ResultCodes **plus the one new
   `RISK_CASE_SUBJECT_NOT_ELIGIBLE` (422)** from the addendum. Opaque
   `CaseNumber` (`RC-<UUIDv4>`) and opaque note/association event refs;
   internal `BIGINT id` never in the API.
7. The complete verification suite in Design §16: domain unit tests
   (both creation paths, every transition invariant, illegal transitions,
   resolution-cycle immutability, deterministic ordering); application
   tests; real disposable MySQL 8.4 integration tests (no H2, no skipped
   mandatory test); transaction/audit atomicity tests (history-write and
   audit-write failure both roll back the root); concurrency tests (two
   writers on one version → one conflict; concurrent resolve/close/reopen;
   duplicate decision-driven creation; duplicate association); immutable
   -history tests; API tests; and explicit tests proving the STRICT
   subject bar (`RECOGNIZED_NOT_ELIGIBLE` subject rejected on create) and
   the relational invariants (`associateAction` rejects an Action whose
   originating Decision is not associated to the case; `recordActionOutcomeReference`
   rejects an outcome whose pertaining Action is not associated).

## Hard boundaries — do not do these

- Do not modify any file under `com.brokeros.risk.security`,
  `tradingaccount`, `evidence`, `decision`, `action`, or `actionoutcome`,
  or any existing Flyway migration.
- Do not implement any real execution, Account Control adapter, MT4/MT5/
  CRM/bridge/LP vocabulary, or Kafka topic / Redis key. Risk Case NEVER
  executes an Action (Design §4 diagram) and records outcome references
  without interpreting vendor success.
- Do not invent IAM/RBAC, users, roles, teams, queues, authentication
  protocol, retention/legal-hold/redaction workflow (Design §12.2) — reuse
  Q-009's authorization only.
- Do not add any destructive delete or silent note/evidence replacement;
  history is append-only and immutable.
- Do not fabricate a provider or accept an unchecked reference string.
- **Test-ownership discipline (this session's lessons):** any migration
  test must derive the post-baseline migration count dynamically
  (`flyway.info().pending().length`), never hard-code it; any "exactly
  these objects" ownership assertion (tables, FKs, ResultCodes) must scope
  to Q-008's exact own object names, never a bare shared prefix like
  `LIKE 'risk_case_%'` / `startsWith("RISK_CASE_")` that a future sibling
  could collide with. See `docs/lessons/2026-08-31-q011-migration-count-test-fix.md`
  and the Q-014 v4 review.
- Do not silently reinterpret, narrow, or broaden anything in the
  governing documents. **Autonomy directive (Product Owner, 2026-09-02):
  complete the entire implementation and its review package in one
  autonomous run — do NOT stop to ask questions, request confirmation, or
  halt for check-ins.** The design has been independently reviewed and is
  complete and self-contained, so you should not need to halt. If you
  nonetheless hit something genuinely underspecified or an apparent
  contradiction between governing documents, do NOT stop and wait: choose
  the reading most consistent with the approved V4 design + V5 addendum
  and the directly-analogous established patterns in Q-011…Q-014, implement
  it, and record the exact assumption/finding in `OutstandingItems.md` for
  Claude Code's independent review. The only thing you must never do is
  fabricate: never invent a passing verification result, and if a real
  tool (Maven / MySQL 8.4 / Docker) is genuinely unavailable, record that
  honestly in `Verification.md` rather than claiming a check passed.
- Do not stage, commit, or push. Do not touch any existing timestamped
  review package.

## Required output

After implementation and full verification, create ONE new,
non-overwriting, timestamped review package at
`review/q-008/review-q-008-v<N>-implementation-<YYYYMMDD-HHMMSS>/` (check
`review/q-008/` for the next unused version) containing at minimum:
`Summary.md`, `ArchitectureReview.md`, `DesignTraceability.md` (map each
Q-008 acceptance criterion + each Design §5 operation and §8 table to the
implementing class/test), `ProjectTree.txt`, `GitStatus.txt`,
`GitDiffStat.txt`, `Verification.md` (exact commands, environment,
pass/fail/skip counts — honest, never fabricated), `SecurityReview.md`,
`TestInventory.txt`, and `OutstandingItems.md`. Add a
`docs/lessons/<date>-q-008-implementation.md` entry and update
`docs/skills/` if a reusable pattern emerged.

Run the full repository-wide real-MySQL gate with `Q009…Q014` **and** the
new Q-008 datasource variables all enabled, and report the result honestly
even if it fails for a reason outside this task's boundary — do not
silently work around such a failure.

This review package is for Claude Code's independent implementation review,
not your own sign-off — do not mark Q-008 "complete" or "approved"; state
PASS/FAIL against each acceptance criterion honestly and list every open
question and every assumption you made. Because Q-008 is large, favour
completing a coherent, fully-tested whole; if you genuinely cannot hold
the entire task reliably in one pass, complete and test as much as you can
as coherent vertical slices, and record precisely what remains in
`OutstandingItems.md` — degrade by documenting honestly, never by
fabricating verification.

Stop only after producing the review package (this is the end of the
authorized work, not a mid-task halt). Do not begin any other Requirement.
