# Q-016 (React) — Claude Code Independent Implementation Review (v6)

- Requirement: Q-016 — Frontend Foundation (Risk Console), React pivot (ADR-018)
- Reviewed: Codex v5 delivery (`review-q-016-v5-react-implementation-20260902-205304`)
- Baseline: `275ef06` (React pivot governance bundle)
- Reviewer: Claude Code (external Architect role) — Date: 2026-09-02
- **Gate Decision: PASS WITH CONDITIONS** (one condition: live E2E)

## Verdict

The React Risk Console is a clean, correct implementation of the authorized
pivot. Unlike the Flutter delivery (which I had to fix for four defects), **this
delivery has no defects I needed to fix** — everything executable is green, and
I reproduced it independently from a fresh install. The single outstanding item
is the live Keycloak→backend browser slice, which legitimately needs a running
dev stack + dev credentials (condition C2 — the same class of live-E2E condition
as before). Critically, the React pivot **closed the C1 gap**: the unit/
component/contract tests now actually run and pass on the Product Owner's
Apple-Silicon (arm64) machine, which Flutter could not do.

## Independently reproduced (not read from Codex's report)

From a **fresh `npm ci`** (I did not trust Codex's `node_modules`):

| Check | Command | Result |
| --- | --- | --- |
| Install | `npm ci` | 309 packages, clean |
| Type-check | `tsc --noEmit` (strict) | **0 errors** |
| Unit/component/contract | `vitest run` | **27 passed / 27 (7 files)** |
| Production build | `vite build` | **PASS** (1566 modules, ~1.8s) |
| Backend untouched | `git diff --stat/-name-only -- backend/` | **empty** (no code, no migration) |

Log: `logs/01-fresh-npmci-typecheck-vitest-build.txt`.

## Code review — security & thin-client discipline (all PASS)

- **Thin client:** no business rules/authorization client-side; the axios client
  parses the backend `ApiResponse`/`ResultCode` envelope and throws the backend's
  code — it never re-decides. No actor/subject/user identity is placed in any
  request body (`addNote` sends only `{ content, expectedVersion }`; the case
  number travels in the path, `encodeURIComponent`-escaped).
- **OIDC:** `response_type: 'code'` (Authorization Code + PKCE, public client) —
  **no `client_secret`, no implicit/password grant**. Access token + user held in
  an in-memory `MemoryStorage`; only PKCE/protocol state uses `sessionStorage`
  (needed across the redirect). `loadUserInfo:false`, `monitorSession:false`.
- **Bearer/refresh:** request interceptor attaches `Bearer` only when a token is
  present and not already set; on `401`, exactly **one** de-duplicated silent
  refresh-and-retry that **preserves the refreshed header**, else raises
  authentication-required; `403` maps to a typed `AuthorizationError` carrying the
  backend code. (Codex's own notes show it caught and fixed a refreshed-header
  overwrite bug before delivery — verified correct in the shipped code.)
- **No leakage:** no `localStorage` token storage; **no `console.*` in `src`**; no
  `dangerouslySetInnerHTML`/`eval`. UI copy states the password is entered only
  at the IdP.

## Acceptance criteria — reviewer view

| AC | Result | Basis |
| --- | --- | --- |
| 1 build/run | **PASS** | fresh `npm ci` + strict typecheck + `vite build` reproduced green |
| 2 OIDC/PKCE E2E | **PASS (static/unit) — live outstanding (C2)** | code + tests correct; live Keycloak login not run |
| 3 list/detail/history | **PASS** | MSW-backed tests reproduced; repository/queries correct |
| 4 add-note + conflict | **PASS** | sends only `{content,expectedVersion}`; conflict-reload path tested |
| 5 additive list endpoint | **PASS** | consumed unchanged; backend diff empty |
| 6 local slice E2E | **live outstanding (C2)** | launcher/config present; full stack not started |
| 7 tests run | **PASS (unit) — 1 live E2E skipped (C2)** | 27/27 reproduced; Playwright spec present, skipped for missing `.env` |
| 8 no backend change | **PASS** | backend diff empty, no migration |

## Recommendation

Accept Q-016 (React) as **PASS WITH CONDITIONS**. The foundation is correct,
secure, and — unlike Flutter — fully test-executable on the target machine. The
only remaining condition is **C2**: run the live Playwright slice (login → list →
detail → add-note) against a running dev Keycloak + backend + MySQL with a seeded
case and dev operator password. See `OutstandingConditions.md`. No defects to
fix; nothing was changed by the reviewer.
