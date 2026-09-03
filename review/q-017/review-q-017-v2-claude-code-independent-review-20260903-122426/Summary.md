# Q-017 — Claude Code Independent Implementation Review (v2)

- Requirement: Q-017 — Risk Console Case Lifecycle Operations (V1, Groups A+B+D)
- Reviewed: Codex v1 delivery (`review-q-017-v1-implementation-20260903-020721`)
- Baseline: `7a36bc7` (Q-017 approved + §16.5-B bundle)
- Reviewer: Claude Code (external Architect role) — Date: 2026-09-03
- **Gate Decision: PASS** (zero defects; live lifecycle verified for the
  reachable operations)

## Verdict

A clean, correct thin-client implementation of the authorized V1 scope. **No
defects found** — I reproduced the full Node gate from a fresh install and
verified the live action-execution path (including a terminal action and correct
backend-domain-error surfacing) against a real Keycloak → backend → MySQL stack.
The operations I could not live-run (resolve/close/resume/reopen, and the
*successful* mark-action-required/return-to-review) are gated by a **backend
invariant** — those transitions require a case with a current decision and an
associated action (the Core-Domain chain; Group C associations are deferred by
the Requirement) — not by any frontend defect; their frontend logic is covered by
the 103 unit tests, and I confirmed live that the console surfaces the backend's
rejection correctly when the precondition is absent.

## Independently reproduced (fresh `npm ci` — not trusting Codex's tree)

| Check | Result |
| --- | --- |
| `npm ci` | 309 packages, no lockfile change |
| `tsc --noEmit` (strict) | **0 errors** |
| `vitest run` | **103 passed / 103 (9 files)** |
| `vite build` | PASS (1,572 modules) |
| Backend untouched | `git diff -- backend/` empty; no migration |
| Bootstrap grant | operator = `{read,note,assign,review,resolve,close,cancel,reopen}` — exactly the confirmed V1 set, verified in the running DB |

## Live lifecycle verification (real stack)

Stood up `docker compose --profile console` (MySQL/Keycloak/backend), granted the
operator the V1 set via the (Codex-modified) security bootstrap, seeded an OPEN
case, served the production build, and drove the console headless:

| Step | Result | Live backend |
| --- | --- | --- |
| OIDC Auth Code + PKCE login → open case | OK | (login path = Q-016 fix efc169f) |
| Assign | OK → v2 | `200 /assignments` |
| Begin review | OK → In Review, v3 | `200 /review-start` |
| Change priority (Critical) | OK → v4 | `200 /priority-changes` |
| Mark action required | **correctly rejected** | `422 /action-required` → console shows the typed error *"The operation does not satisfy the current case requirements."*, no success, no crash (approach-c working) |
| Cancel (terminal) | OK → Cancelled, v5 | confirmation flow → `200 /cancellation` |

DB confirmed the writes went through the real domain: case `CANCELLED v5`, 2
transition-history rows, 1 assignment, 1 priority change, 16 audit records.

## Code review (all correct)

- 11 declarative descriptors → correct endpoints, `allowedFrom` matching the
  state machine, `terminal` flag on resolve/close/cancel.
- `useCaseAction` (one TanStack Query runner): reuses the Q-016 axios client
  (Bearer, `401` refresh, `403`→typed error, envelope), and on
  `RISK_CASE_VERSION_CONFLICT` refetches the case detail (reload); the dialog
  preserves input for retry.
- Every request body carries `expectedVersion`; **no actor/subject identity in
  any body**; `noteRef` is `encodeURIComponent`-escaped.
- Terminal actions require a distinct confirmation step + mandatory reason.
- `actionsForStatus` = approach (c); the backend remains authoritative and its
  domain/`403` rejections are surfaced as readable typed errors.
- No `localStorage` tokens, no `console.*`, no `dangerouslySetInnerHTML`/`eval`,
  no JWT-claims parsing or capability probe.

## Acceptance criteria — reviewer view

| AC | Result |
| --- | --- |
| 1 operations end-to-end | **PASS** — assign/begin-review/priority/cancel live-verified; rest unit-covered + backend-precondition-gated |
| 2 terminal confirmation | **PASS** — live (cancel) + unit |
| 3 version-conflict reload | **PASS** — runner code + 11 unit cases |
| 4 approach-(c) availability + typed rejection | **PASS** — code + live 422 surfaced readably |
| 5 no backend change / no identity in body | **PASS** — backend diff empty; only the bootstrap grant |
| 6 tests + build | **PASS** — 103/103, typecheck, build; live lifecycle run |

## Recommendation

**Accept Q-017 V1.** No defects; the reviewer changed nothing. The delivery is in
the working tree (uncommitted); the ephemeral verification stack and dev `.env`
were torn down (volumes removed). Note for a future Requirement: exercising
resolve/close/resume/reopen live requires seeding a case with a current decision +
associated action (Group C), which is deferred.
