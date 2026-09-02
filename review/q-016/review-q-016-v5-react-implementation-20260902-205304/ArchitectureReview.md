# Q-016 React Architecture Review

## Gate Decision

**PASS WITH CONDITIONS** — the implementation conforms to ADR-018 and the React
pivot addendum; live Keycloak/backend end-to-end behavior remains unexecuted.

## Requirement and boundary review

- Scope is one thin-client Risk Case slice: authentication, bounded list,
  detail/history/association references, and add investigation note.
- No new frontend business transition, risk rule, authorization decision,
  reference-resolution logic, backend endpoint, database object, message,
  cache, adapter, or production infrastructure was introduced.
- Add-note UX checks blank/length shape, but the command is still sent to and
  governed by the backend. Version conflict is a backend `ResultCode`, not a
  client-side concurrency decision.
- The current authenticated actor comes only from the Bearer token. Command
  bodies contain no actor/principal/role/scope field.

## ADR-018 and addendum compliance

| Decision | Evidence | Result |
| --- | --- | --- |
| React 18 + strict TypeScript, Vite SPA | Exact locked React 18.3.1; `strict: true`; Vite 7.3.6; build passed | PASS |
| React Router + auth guard | `src/app/App.tsx` protected route and login redirect | PASS |
| TanStack Query | Feature query keys/hooks, invalidation after mutation, cache cleared at identity boundary | PASS |
| react-oidc-context / oidc-client-ts | `authConfig.ts`, Authorization Code response type, public client, memory user store | PASS |
| axios Bearer/ResultCode client | `apiClient.ts`, request/response interceptors and runtime envelope parser | PASS |
| TanStack Table | Headless column/row/sort model in `RiskCaseTable.tsx` | PASS |
| Ant Design | Layout, form, modal, table shell, async/error/detail components | PASS |
| Vitest + RTL + MSW | 27 tests passed across 7 files | PASS |
| Playwright | Live spec delivered; Chromium installed/launched; authenticated spec skipped for missing inputs | CONDITION |
| No backend pivot change | Empty backend task diff | PASS |

## Layer and dependency review

`core/config` validates non-secret runtime inputs. `core/auth` owns OIDC
settings. `core/api` owns transport, envelope parsing, and typed failures.
`features/riskcase/api` owns consumed DTO parsers and the repository;
`model` owns TanStack Query hooks/context; `ui` only renders remote state and
submits backend commands. The direction remains UI → model → API → core API;
no backend or infrastructure implementation leaks into the client.

## Compatibility and operations

- Vite uses port 4173, so delivered Keycloak redirects and exact dev CORS are
  reused without configuration change.
- `runtime-config.js` supports deployment repointing of endpoints/public client
  ID without rebuilding. Values are validated as absolute HTTP(S) URLs/nonblank
  ID and contain no secret.
- `scripts/run-risk-console-dev.sh` preserves the existing local workflow while
  replacing Flutter commands with `npm ci` and `npm run dev`.
- Static assets are the deployment product. Route-level lazy loading separates
  feature chunks; Vite still reports a non-blocking 766.04 kB base chunk warning.

## Development Standards Compliance

### AGENTS.md compliance

The approved Q-016 Requirement §17, ADR-018, React addendum, implementation
prompt, root `AGENTS.md`, both engineering governance documents, mandatory
development standards, applicable auth/configuration skills, and recent Q-016/
Q-009 lessons were inspected before implementation. Work stayed in the
authorized implementation stage. No staging, commit, push, older review-package
edit, or next Requirement occurred.

### Architecture compliance

The product remains broker-, CRM-, and trading-platform-neutral. The frontend
handles opaque references and backend DTOs only. It adds no service boundary,
database dependency, Kafka/Redis use, vendor adapter, or business-rule copy.
Frontend dependencies follow the React addendum layout and are pinned in the
lockfile.

### ADR compliance

ADR-018's exact stack is implemented. ADR-017 is used only for reaffirmed
framework-neutral rules; its Flutter stack is removed. The Ant Design choice is
honored. No new architecture decision was introduced that requires another ADR.

### API standard compliance

No backend API was added/changed/removed. TypeScript runtime parsers mirror the
real `ApiResponse`, Risk Case list/detail/history/note DTOs, and consumed stable
ResultCodes. List size is fixed at 20; history limit is 100. Malformed envelopes
fail as `INVALID_API_RESPONSE`. Actor identity is absent from request bodies.

### Database standard compliance

No backend or Flyway path is changed. `git diff --stat -- backend/` is empty.
No SQL, schema, index, table, money representation, transaction, or data
migration decision is present in this task.

### Security standard compliance

OIDC uses Authorization Code + PKCE settings for a public client. Signed-in
user/token state uses an in-memory store; only redirect protocol state uses
session storage. The centralized Bearer path refreshes once, retries once,
preserves the refreshed header, and fails authentication closed. `403` is typed.
TanStack Query cache clears when the authenticated subject changes or refresh
fails. No dangerous HTML, token logging, hard-coded credential, client secret,
implicit flow, or password grant was found. Live Playwright artifacts are
disabled.

### Auditability compliance

The client neither invents nor suppresses backend audit behavior. Detail/history
and add-note calls keep the verified backend actor boundary; note operations
carry `expectedVersion`. The UI displays the actor/timestamp/version fields the
backend returns. No new audit record contract is introduced.

### Skill compliance

`development-standards`, `trusted-actor-authorization`, and
`configuration-management` were applied. Non-secret React settings were added
to the configuration catalog. Reusable React lessons were captured in
`react-risk-console-development.md`; the superseded Flutter skill is explicitly
historical. The mandatory lesson and review package are present.

## Cross-system impact

Risk Case is consumed through existing REST contracts. Rule Engine, Account
Control, Audit implementation, Kafka, Redis, MT4, MT5, BrokerPilot, oneZero,
CRM, MySQL schema, Keycloak realm, CORS Java configuration, and backend runtime
have no implementation change in this task.
