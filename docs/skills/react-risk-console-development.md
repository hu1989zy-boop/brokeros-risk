# React Risk Console Development Skill

## When to Use

Use this guidance when implementing or reviewing the BrokerOS Risk React web
console, its OIDC session, typed API client, TanStack Query state, Risk Case
screens, or browser tests. Read the governing Requirement, ADR-018, the React
pivot addendum, `AGENTS.md`, `development-standards.md`,
`configuration-management.md`, and `trusted-actor-authorization.md` first.

## Thin-client Boundary

- Keep lifecycle rules, authorization, actor identity, and reference recognition
  in the backend. Client validation may improve form usability, but the backend
  contract is authoritative.
- Treat domain references as opaque strings. Never join an external system or
  infer broker/platform meaning in the browser.
- Send `expectedVersion` for mutations. On version conflict, keep the user's
  input visible, explain the stale version, and reload authoritative detail.
- Model and runtime-parse `ApiResponse<T>`, bounded page DTOs, consumed
  `ResultCode`s, and feature DTOs. Unknown or malformed envelopes fail closed.

## Browser OIDC and Cache Boundary

- Use OIDC Authorization Code + PKCE through `react-oidc-context` /
  `oidc-client-ts`; never add implicit flow, password grant, a client secret, or
  in-app credential fields.
- Configure an in-memory OIDC user store so the access token is not persisted.
  Session storage may hold only short-lived protocol state required across the
  authorization redirect.
- Attach Bearer credentials in one axios interceptor. On `401`, coordinate one
  silent refresh and retry the request once. Preserve the newly refreshed
  Authorization header; a normal request interceptor must not overwrite it with
  the stale pre-refresh value. Map `403` to a typed authorization error.
- Clear the TanStack Query cache whenever the authenticated subject changes or
  authentication expires. Cached Risk Case content must never cross an operator
  session boundary.
- Never log tokens, credentials, authorization headers, or sensitive case
  content. Disable Playwright trace, screenshot, and video capture for live
  identity flows unless a separately approved sanitizer proves that artifacts
  contain no secrets.

## Query and UI Pattern

- Use an injected feature repository for transport and runtime parsing;
  TanStack Query hooks own remote loading/error/cache/invalidation state; UI
  components render state without making authorization decisions.
- Request a fixed bounded page size and display backend-projected summaries.
  Client-side table sorting sorts only the current bounded page and must be
  labelled accordingly.
- Fetch detail and the bounded history endpoint using their documented DTOs.
  Do not describe historical association references as a complete current-state
  projection when the backend contract does not provide one.
- On mutation success, invalidate/refetch detail and list data instead of
  patching a client-owned aggregate.
- Keep runtime configuration to non-secret endpoints and public client IDs.
  Validate absolute URLs at startup and document restart/rebuild semantics.

## Verification Pattern

- Run strict TypeScript checking independently of bundling.
- Use Vitest + React Testing Library + MSW for contract parsing, repository
  headers/errors, query-backed loading/empty/error/success states, mutation
  success, and version-conflict recovery.
- Run a production Vite build. A successful bundle is evidence, not proof of
  requirement compliance; record warnings honestly.
- Install and launch Playwright Chromium to distinguish browser-tool
  availability from live Keycloak/backend/data availability. A live spec may be
  marked not executed only with its exact missing prerequisite recorded.
- Do not put a live password on a command line or into test output. Supply it
  through the process environment and keep the value out of review artifacts.

## Review Checklist

- React/TypeScript/Vite and all libraries match ADR-018 and the pivot addendum.
- Authorization Code + PKCE is explicit; token user state is memory-only.
- Bearer retry is bounded, refreshed headers are preserved, and 403 is typed.
- Query cache is cleared at identity boundaries.
- Lists and history are bounded; no business rule or actor identity is sent.
- Mutations include `expectedVersion` and reload after success/conflict.
- Frontend tests and build ran on Node; Playwright evidence distinguishes
  browser smoke from the live authenticated slice.
- Backend, migrations, Keycloak, CORS, and historical review packages remain
  unchanged unless separately authorized.
