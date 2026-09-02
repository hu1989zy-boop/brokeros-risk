# ADR-019: Risk Console — Case Lifecycle Operations (thin-client expansion)

- Status: **Accepted — 2026-09-03 — Product Owner** — at the Q-017
  implementation-authorization gate (Decision Authority §16.5-B).
- Date: 2026-09-03
- Requirement: Q-017 — V1, APPROVED — 2026-09-03.
- Builds on: **ADR-018** (React + TypeScript SPA) — no stack change. Consumes the
  committed Q-008 Risk Case operations and Q-009 JWT verification.
- Supersedes: None.

## Context

Q-016 delivered the React Risk Console with one write operation (add note). The
Q-008 aggregate already exposes the full operator workflow over HTTP
(assignment, review lifecycle, resolution/closure/cancel, resume/reopen, note
correction), all authorized (per-capability), version-checked (`expectedVersion`
→ `RISK_CASE_VERSION_CONFLICT`), and audited server-side. Q-017 (approved V1)
surfaces Groups **A + B + D** of these in the console as a **thin-client**
expansion — no backend business logic, aggregate, or migration change. The
Product Owner confirmed: a single operator role with the full V1 capability set,
mandatory confirmation on terminal actions, and status-only action availability
(approach c).

## Decision

### 1. A declarative action registry + a shared action runner

Each V1 operation is described once by an **action descriptor** (endpoint,
method, typed inputs + validation, `allowedFrom` statuses, `terminal` flag,
notable `ResultCode`s). A single **`useCaseAction`** runner (TanStack Query
`useMutation` over the typed repository) executes any operation: it reuses the
Q-016 axios client (Bearer attach, one silent `401` refresh, `403`→typed
authorization error, envelope/`ResultCode` parsing), always sends
`expectedVersion`, and on success refetches the case detail + history. This keeps
eleven operations consistent, testable, and free of copy-pasted mutation logic,
rather than a bespoke component per operation.

### 2. Status-only action availability (approach c); backend is the authority

`CaseActionsBar` offers the actions whose descriptor `allowedFrom` includes the
case's current status. It performs **no** authorization logic: an operation the
operator is not permitted to run returns `403`/a typed `ResultCode`, which is
surfaced as a readable error. V1 adds **no** JWT-claims parsing and **no**
capability probe (both would be a larger change and, for the probe, a backend
addition) — the state map is a UX convenience, not a security control.

### 3. Terminal actions require confirmation + reason

`close`, `cancel`, and `resolve` move a case to a terminal/near-terminal state.
Each is gated by an explicit confirmation dialog with a mandatory reason before
the request is sent; the backend remains the final validator.

### 4. Single console-operator role granted the full V1 capability set

The operator role is granted `{read, note, assign, review, resolve, close,
cancel, reopen}` in the security bootstrap — verified to cover exactly the V1
operations (assign→ASSIGN; priority/begin-review/mark-action-required/return-to-
review→REVIEW; resolve→RESOLVE; close→CLOSE; cancel→CANCEL; resume/reopen→REOPEN;
correct-note→NOTE). No `associate`/`create` (Groups C/E deferred). This is
authorization configuration; **no business-logic/aggregate/migration change**.

## Alternatives Considered

- **Bespoke component per operation** — rejected: eleven near-identical
  mutation+dialog flows would duplicate concurrency/error handling and be harder
  to test than one registry + runner.
- **Capability-driven availability (JWT claims or a `my-capabilities` probe)** —
  deferred: it pre-empts unauthorized actions more cleanly but needs either
  reliable role claims or a new backend read; approach (c) is safe (backend
  enforces) and needs no backend change. A future Requirement may add it.
- **Client-side state-machine enforcement** — rejected: the backend owns the
  transition rules; the UI gate is convenience only.
- **Role split (e.g. senior-only close/cancel/reopen)** — deferred by the
  Requirement gate; V1 is a single role.
- **Group C/E in V1 (associations, creation)** — deferred: both need
  cross-module reference/subject pickers that do not exist yet.

## Consequences

Positive: operators can run the full case lifecycle from the console; one
consistent, tested action framework; the security model and thin-client
discipline are preserved; no backend change beyond the capability grant.

Costs/constraints: approach (c) may occasionally offer an action the backend then
rejects (shown as a typed error) — acceptable and safe. The operator role is
broad (full V1 set); a future Requirement can split roles or add capability-aware
availability. Association/creation remain unavailable until a later Requirement.

## Security Implications

Server-side authorization unchanged (Q-009 + guards). Least privilege: exactly
the V1 capability set, no more. Confirmation + reason on terminal actions. No
identity in request bodies; no sensitive content logged.

## Data / Contract Implications

Consumes existing operation request/response DTOs via typed TypeScript models; a
contract drift is a compile-time signal. No new backend endpoint or field for
A+B+D.

## Operational Implications

The dev security bootstrap grants the operator the V1 set; production carries the
same authorization expectation. Keycloak and all endpoints are otherwise
unchanged.

## Deferred Decisions

Groups C (associations) and E (case creation); capability-aware action
availability; role differentiation. Each requires its own Requirement.

## Approval Boundary

**Accepted by explicit Product Owner decision on 2026-09-03** at the Q-017
implementation-authorization gate, together with the Q-017 Architecture and
Implementation Design (§16.5-B). Codex is authorized to implement Q-017 V1
(Groups A+B+D) as specified.
