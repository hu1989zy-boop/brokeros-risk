# Q-020 Architecture Review

## Governing decisions inspected

- `docs/requirements/Q-020-Risk-Console-External-Reference-Search.md` V1.
- `docs/adr/ADR-022-external-reference-scoped-search.md` (Accepted).
- `docs/architecture/q-020-external-reference-search-architecture.md` V1.
- `docs/architecture/q-020-external-reference-search-implementation-design.md` V1.
- Root `AGENTS.md`, both engineering authority documents, applicable repository
  skills, and recent Q-018/Q-019 lessons.

## Architecture findings

The change stays inside the Phase 1 modular monolith. Each provenance module owns
its controller, application list service/summary, query-port extension, JDBC query,
and response DTO. No external schema is accessed, no adapter boundary is crossed,
and no domain object is exposed through REST. The console remains a thin client:
it requests bounded lists through the existing authenticated API client and sends
the selected opaque reference through the unchanged Q-018 association operation.

ADR-022's natural scopes are followed exactly: evidence and decisions by trading
account subject, actions by decision, and outcomes by action. The shared server cap
is 200; ordering is `recorded_at DESC, id DESC`; there is no pagination, cross-module
endpoint, free-text search, label derivation, or global browse.

The four V4-V7 natural-scope indexes were confirmed in real MySQL. No measured need
for an additional index was found, so no migration was added.

## Design alignment

- Authorization occurs first and uses the existing module `READ` capability. A
  denial increments the established authorization-denial metric.
- Scope values are parsed through `TradingAccountRef`, `DecisionRef`, or `ActionRef`;
  malformed values become the owning module's request-invalid `ResultCode`.
- JDBC projections name only the approved minimal columns. Content columns are not
  loaded by list paths, and list services do not receive access-log ports.
- List DTOs are immutable, content-free, and returned in `ApiResponse` envelopes.
- The frontend hooks are disabled when their scope is absent. The selected kind
  activates only its natural-scope request.
- Browse candidates show opaque reference, recorded time, and status where the
  backing record has a status. Selection continues through the existing preview.
- Scope projection prefers the current on-case decision and otherwise uses an
  associated decision; the action scope comes only from an on-case action.

## Development Standards Compliance

### AGENTS.md compliance

The inspected Git scope contains only Q-020 read-side/backend wiring, frontend
selection changes, tests, Lessons Learned, and this new package. There are no
changes under the five relevant domain directories, no record/correct/association
write-service changes, no existing timestamped review-package edits, and no Git
stage/commit/push operation.

### Architecture compliance

The modular-monolith module boundaries remain intact. Controllers translate HTTP
and call one application service; application services authorize and orchestrate;
JDBC details stay in infrastructure adapters. No new framework, database, cache,
message bus, external integration, or deployment boundary was introduced.

### ADR compliance

ADR-022 is implemented without deviation in endpoint coverage, natural scopes,
metadata-only projections, cap, deterministic ordering, existing capabilities,
absence of pagination, and case-restricted frontend discovery. No new ADR is
needed because this stage implements the already accepted decision.

### API standard compliance

All four list operations are additive `GET /api/<resource>` methods selected by a
required natural-scope query parameter. Responses use `ApiResponse`; malformed
scope values route through existing module exceptions and `GlobalExceptionHandler`;
unknown valid keys return successful empty lists. Existing detail and write routes
are unchanged.

### Database standard compliance

Every query is parameterized, read-only, explicitly projected, bounded, and ordered.
The V4-V7 indexes were verified by `information_schema.statistics`. No table,
column, data, index, or Flyway file changed, so migration compatibility and locking
risk are absent for this delta. `Instant` values originate from UTC timestamps and
serialize through the application's existing ISO-8601 convention.

### Security standard compliance

The server authorizes before scope parsing/query execution, reuses only existing
`*:read` capabilities, exposes no recorded content, writes no full-detail access
log, and adds no identity fields. The frontend obtains scopes only from the loaded
case and authoritative on-case projection and continues to rely on Bearer JWT
handling in `ApiClient`. No secrets, authentication headers, or customer data are
introduced in source or package evidence.

### Auditability compliance

Q-020 is a discovery read, not a critical state-changing action. It deliberately
does not create full-detail content-access audit records, as required by the design.
All existing association mutations and their audit/version behavior remain intact;
the unchanged request-body test provides evidence that browse does not bypass them.

### Skill compliance

`docs/skills/development-standards.md`,
`docs/skills/react-risk-console-development.md`, and
`docs/skills/trusted-actor-authorization.md` were applied. A reusable-skill
evaluation is recorded in the new Q-020 lesson; no new skill was warranted. The
personal review-package skill governs the non-overwriting package, sensitive-data
scan, ZIP validation, checksum, and cleanup handoff.

## Review conclusion

No unresolved architecture or development-standards violation was found in the
implementation scope. Independent reviewer acceptance remains pending by lifecycle
policy and is not implied by this implementation-stage review.
