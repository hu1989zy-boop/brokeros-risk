# ADR-020: Risk Console — Association Management (Group C, reference-preview)

- Status: **Accepted — 2026-09-03 — Product Owner** — at the Q-018
  implementation-authorization gate (Decision Authority §16.5-B).
- Date: 2026-09-03
- Requirement: Q-018 — V1, APPROVED — 2026-09-03.
- Builds on: **ADR-018** (React SPA) + **ADR-019** (action registry + runner). No
  stack change. Consumes committed Q-008 association endpoints, the Q-011…Q-014
  `GET /{ref}` reads, and Q-009.
- Supersedes: None.

## Context

Q-017 gave the console the case lifecycle but deferred **Group C** — managing a
case's evidence/decision/action associations — because associating an entity needs
its reference (`ev-/dc-/ac-/ao-`, opaque UUIDs) and the source modules expose only
`POST` and `GET /{ref}` (no list/search). The Product Owner approved Q-018 V1 with
**Option A**: manual reference entry validated by a **fetch-by-ref preview** using
the existing `GET /{ref}`, keeping the console thin-client with no new backend
endpoint. Group C also **unblocks** Q-017's `resolve`/`close`, which require a
current decision with an associated action.

## Decision

### 1. Reuse the Q-017 action framework for the six association operations

The six operations (evidence associate + disposition; decision associate +
selection; action associate + outcome) are added as **action descriptors** and
run through the existing **`useCaseAction`** runner (Bearer, `expectedVersion`,
`403`/`ResultCode` typed errors, `RISK_CASE_VERSION_CONFLICT` reload). No new
execution machinery.

### 2. Reference sourcing = manual entry + fetch-by-ref preview (Option A)

External references (new `evidenceRef`/`decisionRef`/`actionRef`, `outcomeRef`,
replacement evidence) are entered through a **`ReferenceInput`** control that,
after client-side format validation, calls the module's existing `GET /{ref}` and
shows a **preview** of the entity; the action cannot submit until a valid
reference is confirmed. This catches typos/wrong refs before the association and
needs **no new backend endpoint**. Browse/search pickers (Option B, new list
endpoints) are deferred to a future Requirement.

### 3. On-case references are picked from existing state

The disposition target event, the current-decision candidates, and the action for
an outcome are selected from the case's **existing detail/history** — no manual
UUID for those.

### 4. Capability grant = `associate` + four module reads

The console-operator role is granted `risk-case:associate` (the six operations)
plus `evidence:read`, `decision:read`, `action:read`, `action-outcome:read` (the
preview `GET /{ref}` calls) in the security bootstrap — verified against the
services. Minimal reads, scoped to preview; **no backend business/aggregate/
migration change**.

## Alternatives Considered

- **Option B — list/search pickers with new endpoints** — better discovery UX, but
  a real backend scope increase across four modules; deferred by the Requirement
  gate to a later Requirement.
- **Manual entry without preview** — simplest, but ships blind UUID entry with no
  guard against typos/wrong refs; kept only as the fallback if a source `:read`
  grant is undesirable.
- **A bespoke association subsystem** — rejected: the Q-017 registry+runner already
  provides version-safe, typed-error, conflict-handling execution; reuse it.
- **Client-side association-rule enforcement** (e.g. deciding evidence supersession
  validity) — rejected: the backend owns those rules; the console surfaces its
  `ResultCode`s.

## Consequences

Positive: operators can manage a case's substance (evidence/decisions/actions) and
thereby reach the full lifecycle (`resolve`/`close`); preview prevents wrong-ref
associations; the action framework and thin-client model are reused unchanged;
no new backend endpoint.

Costs/constraints: manual UUID entry (mitigated by preview) is less convenient than
a picker — a future Requirement can add Option B. The operator gains four
cross-module read capabilities (least-privilege reads). If the detail/history
payloads lack an on-case ref a picker needs, that ref falls back to manual entry
(Architecture verification point) — still no new endpoint.

## Security Implications

Authorization stays server-side (Q-009 + guards). New grants are reads +
`associate`, minimal. Backend authoritative; its `403`/`ResultCode` surfaced as
typed errors. No identity in bodies; no references/entity content logged.

## Data / Contract Implications

Consumes existing association request/response DTOs and `GET /{ref}` responses via
typed TS models. No new backend endpoint or field.

## Operational Implications

The dev security bootstrap grants the five capabilities; production carries the
same expectation. Keycloak and all endpoints unchanged.

## Deferred Decisions

Option B browse/search pickers (new list endpoints); creating evidence/decisions/
actions from the console; Group E (case creation). Each a future Requirement.

## Approval Boundary

**Accepted by explicit Product Owner decision on 2026-09-03** at the Q-018
implementation-authorization gate, together with the Q-018 Architecture and
Implementation Design (§16.5-B). Codex is authorized to implement Q-018 V1 (the
six Group C operations, Option A reference preview) as specified.
