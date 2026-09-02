# Q-016 React Security Review

## Decision

**PASS WITH CONDITIONS** — static/automated security controls pass; live
Keycloak/browser/backend interoperability is not executed.

## Authentication and token handling

- `response_type` is `code`; Keycloak's existing client enforces S256 PKCE.
- No client secret, implicit flow, resource-owner password grant, in-app
  username/password form, or backend login endpoint exists.
- `WebStorageStateStore` over a private in-memory `Storage` holds signed-in user
  state/access token. `sessionStorage` is only the OIDC redirect state store.
- Silent renewal is library-managed. On `401`, axios coordinates one refresh
  and one request replay. A failed refresh clears query data and removes the
  authenticated user. `403` becomes `AuthorizationError`.
- Regression coverage caught and fixed stale-token header overwrite on replay.

## Authorization and identity

- UI state is not an authorization boundary. Controls call the backend and
  honor the returned ResultCode.
- Request bodies contain no actor, subject principal, role, capability, scope,
  authorization header, or identity override. The note command contains only
  content and expected version.
- The backend's Q-009 JWT verification and exact capability decisions are
  unchanged.

## Browser data and injection

- React text rendering and Ant Design components are used; no
  `dangerouslySetInnerHTML`, `eval`, script injection, or raw upstream payload
  rendering exists.
- DTOs runtime-validate object/string/enum/integer/instant fields. Unexpected
  envelopes fail without exposing implementation details.
- List/history requests are bounded. Opaque refs and investigation text are
  rendered as text.
- TanStack Query cache clears on authenticated-subject changes and refresh
  failure to prevent cross-operator browser-session disclosure.

## Configuration and secrets

- Runtime config accepts only non-secret endpoints, redirect URI, and public
  client ID; URLs must be absolute HTTP(S), client ID nonblank.
- No `.env`, password, token, private key, JWT literal, or live credential was
  added. The corrected high-entropy scan returned no match.
- `npm audit --audit-level=high` returned 0 vulnerabilities for the locked tree.
- Live Playwright trace, screenshot, and video capture are disabled. Passwords
  are supplied only through the process environment when a live test is run.

## Test artifact review

Fixtures contain synthetic opaque identifiers and a non-production Bearer
marker, not a signed JWT or usable credential. Generated Playwright reports,
test results, Vite dist, and stale Flutter build outputs were removed after
verification. `node_modules` remains ignored for local reproducibility.

## Outstanding dynamic security evidence

Run the live Playwright spec with the ignored local `.env` and an existing Risk
Case to verify Keycloak redirect/callback, PKCE exchange, backend JWT audience/
issuer verification, refresh interoperability, logout, and least-capability
behavior. Do not enable trace/video/screenshots for that run.
