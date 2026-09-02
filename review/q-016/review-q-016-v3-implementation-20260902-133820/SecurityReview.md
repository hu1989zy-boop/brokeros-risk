# Q-016 Security Review

## Decision

**PASS WITH CONDITIONS** — source and configuration controls are aligned with the
approved model; dynamic OIDC/browser verification remains outstanding.

## Identity and authorization

- The console uses OIDC Authorization Code + PKCE with random state/verifier values.
  The transient values live only in browser session storage, are cleared after the
  callback, and authorization query parameters are removed from browser history.
- The Keycloak public client enables standard flow, requires PKCE S256, disables
  implicit and direct-access grants, and restricts redirects/origins to the local
  Flutter web origin.
- The console sends identity only in `Authorization: Bearer`; the add-note body has
  only `content` and `expectedVersion`.
- Q-009 remains the backend trust boundary. `RiskCaseQueryService.listCases` invokes
  the existing `risk-case:read` authorization before parsing filters or accessing SQL.
- The seeded operator receives only `risk-case:read` and `risk-case:note`; its password
  is supplied at local runtime and is absent from the realm export.

## Token handling

- Access token: memory only.
- Refresh token: `flutter_secure_storage` abstraction.
- `401`: at most one refresh-and-retry per request.
- `403`: typed authorization failure; no retry/bypass.
- Logout cancels token updates, clears memory and secure storage, and uses provider
  end-session when available.
- No token, authorization header, password, or case content is logged by Q-016 code.

## API, data, and browser boundary

- List responses contain only case number, subject reference, status, priority,
  assignee reference, timestamps, and version.
- Client page size is 20; server size is capped at 100; the repository fetches at most
  101 summary rows. History is requested with its existing maximum of 100.
- SQL parameters are bound through `JdbcTemplate`; optional filters are not
  concatenated from raw input.
- Dev CORS is enabled only in the `dev` profile and permits exactly
  `http://localhost:4173` by default.
- Keycloak/console services are profile-scoped and bind local development ports to
  `127.0.0.1`.

## Sensitive material scan

The implementation and review package were inspected for populated passwords,
Bearer tokens, private keys, and client secrets. `.env.example` contains names only;
the realm contains no user credential; the public client has no secret. Test password
values are redacted from this review package.

## Outstanding dynamic checks

- Confirm discovery, redirect state validation, code exchange, audience, Bearer
  validation, silent refresh, logout, and URL cleanup against the running Keycloak.
- Confirm `401`, `403`, expired refresh token, and version-conflict UI behavior in a
  real browser.
- Re-scan generated dependency lock/code-generation outputs after Flutter is
  available.

These conditions are verification gaps, not permission to weaken any control.
