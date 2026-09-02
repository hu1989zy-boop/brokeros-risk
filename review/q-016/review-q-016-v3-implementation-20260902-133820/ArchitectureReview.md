# Q-016 Architecture Review

## Gate Decision

**PASS WITH CONDITIONS** — the inspected source respects the approved Q-016
architecture boundary, but the Flutter toolchain and live browser/Keycloak slice were
not available for runtime verification.

## Boundary assessment

The implementation remains a Phase 1 modular-monolith extension. The browser console
is a separate delivery artifact in the same repository, while all Risk Case rules and
authorization remain in the backend. The only backend product API addition is a query
path inside the existing `riskcase` module. Keycloak is confined to the approved
development profile; no Kafka/Redis behavior, production topology, or external-system
adapter boundary changed.

The frontend dependency direction is `presentation -> application -> data/core`.
Notifiers orchestrate loading and operations through an injected repository. DTOs
mirror backend contracts and do not become backend domain models. The UI does not
decide whether a command is allowed; it displays and submits a bounded request and
honors the backend result.

## Approved decision evidence

- ADR-017 thin-client decision: implemented under `frontend/lib`; no backend rule was
  copied into UI state.
- ADR-017 identity decision: custom browser flow calls
  `Flow.authorizationCodeWithPKCE`; implicit and password grants are not configured.
- ADR-017 stack decision: exact versions are pinned for Riverpod, go_router, dio,
  openid_client, secure storage, Freezed, and JSON serialization.
- Additive list decision: `GET /api/risk-cases` uses the existing read capability and
  `ApiResponse`, returns summaries only, and is capped at 100.
- Vertical-slice decision: login/list/detail/history/associations/add-note is the only
  application slice delivered.

## Development Standards Compliance

### AGENTS.md compliance

The inspected scope is limited to Q-016 frontend/dev infrastructure and the approved
additive Risk Case query. No new Requirement, speculative module, direct external
database integration, or vendor-specific domain coupling was introduced. The change
uses the approved Java/Spring Boot/MySQL stack and does not add Python or Flink.

### Architecture compliance

Controllers translate query parameters and return `ApiResponse`; query orchestration
stays in `RiskCaseQueryService`, and SQL stays in `JdbcRiskCaseRepository`. Frontend
HTTP, auth, application state, and presentation responsibilities are separated. The
console remains untrusted and backend authorization is called before query parsing or
repository access.

### ADR compliance

ADR-017 is Accepted and is implemented without changing its selected stack, identity
flow, repository placement, or backend boundary. Existing accepted decisions for
Q-008–Q-014 remain intact; the full real-MySQL regression suite passed.

### API standard compliance

The endpoint is additive at `/api/risk-cases`, returns the existing
`ApiResponse<RiskCaseListResponse>`, uses existing result codes and global exception
handling, and does not expose persistence entities. Query enum/reference validation is
translated to an existing Risk Case invariant result; malformed framework parameters
continue through the existing handler.

### Database standard compliance

No schema or migration file was added or edited. The SQL selects only summary fields,
binds all filters, orders stably by `updated_at DESC, id DESC`, fetches at most 101
rows to calculate `hasNext`, and uses the approved existing schema. Real MySQL 8.4
tests exercised filtering, projection, capping, and ordering. A future performance
measurement is recorded because Q-016 did not authorize a new covering index.

### Security standard compliance

The backend derives `ActorContext` from verified Bearer identity and requires the
existing Risk Case read capability. Operation bodies contain no identity. No token is
logged; access tokens are memory-only and refresh tokens use secure storage. The
Keycloak client is public, requires PKCE S256, disables implicit/direct grants, uses
exact local redirects/origins, and the seeded user receives only read/note
capabilities. Dev CORS is profile-scoped and exact-origin.

### Auditability compliance

Existing case detail and history reads remain individually audited. The list endpoint
does not invent a case-specific audit record because one response can contain several
cases and the approved audit factory requires one concrete Risk Case target. The
authorization decision remains centrally enforceable and the deliberate list-audit
choice is documented in Lessons Learned and Outstanding Items.

### Skill compliance

`docs/skills/flutter-risk-console-development.md` captures reusable PKCE, token,
thin-client, bounded-query, Keycloak, code-generation, and verification practices;
the skills index references it. This review package follows the personal
`brokeros-review-package` skill and is new/non-overwriting.

## Violations and conditions

No unresolved standards violation was found in the inspected source. Runtime evidence
is incomplete because Flutter/Dart and a browser-driven Keycloak session were not
available. Those gaps prevent a full PASS and are enumerated in `OutstandingItems.md`.
