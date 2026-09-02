# Flutter Risk Console Development Skill

## When to Use

Use this guidance when implementing or reviewing the BrokerOS Risk Flutter web
console, its OIDC session, typed API client, Riverpod state, Risk Case screens,
or a browser-facing additive query endpoint. Read the governing Requirement,
ADR-017, approved frontend architecture/design, `AGENTS.md`,
`development-standards.md`, `configuration-management.md`, and
`trusted-actor-authorization.md` first.

## Thin-client Boundary

- Keep business lifecycle rules, authorization, actor identity, and reference
  recognition in the backend. Flutter may validate form shape for usability,
  but backend contracts remain authoritative.
- Treat all Evidence, Decision, Action, ActionOutcome, Actor, and Trading
  Account references as opaque strings. Never join external systems or infer
  broker/platform meaning in the UI.
- Send `expectedVersion` for mutations and map the backend's stable
  `ResultCode`; never emulate aggregate transitions in Dart.
- Model `ApiResponse<T>`, bounded page DTOs, and feature DTOs explicitly. An
  unknown result code is an error, not success.

## Browser OIDC Boundary

- Browser clients use Authorization Code + PKCE only. Do not assume a package's
  `Authenticator` uses PKCE: inspect the selected version. The
  `openid_client` browser convenience authenticator used during Q-016 still
  constructs an implicit flow, so Q-016 uses `Flow.authorizationCodeWithPKCE`
  through a small browser adapter instead.
- Generate fresh cryptographic `state` and PKCE verifier for each redirect;
  keep only those one-time correlation values in session storage and validate
  callback state through the OIDC library.
- Keep the access token in memory. Persist only the refresh token through the
  platform secure-storage adapter. Clear memory and secure storage on refresh
  failure and logout.
- Attach Bearer credentials in one HTTP interceptor. On `401`, attempt one
  refresh and one replay; prevent refresh loops. Treat `403` as authorization
  denial. Never put actor identity, credentials, or tokens in request bodies,
  URLs, logs, exceptions, or Review artifacts.
- A seeded local Keycloak realm is development infrastructure, not a production
  IdP decision. Supply administrator and operator passwords from ignored
  environment input; do not commit even a development password.

## Query and UI Pattern

- Add only an authorized application-service query and a repository projection
  when a screen needs a new read shape. Return only screen summary fields.
- Apply a server maximum even when the client requests more, fetch at most one
  extra row to determine `hasNext`, and use deterministic ordering. The client
  also requests a fixed bounded page size.
- Do not invent a synthetic per-case audit record for a multi-case list if the
  audit contract requires one case target. Record the decision in the Review;
  keep individual detail/history disclosure auditing unchanged.
- Riverpod notifiers own loading, empty, error, pagination, detail, and
  operation states behind an injected repository. Widgets render those states
  and contain no transport or authorization logic.
- On version conflict, show a precise stale-version message and reload detail.
  On success, reload authoritative backend state rather than patching a local
  case aggregate.

## Verification Pattern

- Frontend: run code generation, analyzer, unit tests for list/detail/operation
  notifiers, widget tests for loading/empty/error/success/dialog states, typed
  contract parsing tests, and a web build.
- Backend: test authorization-before-query, filters, server page cap, summary
  projection, deterministic pagination, and real MySQL behavior. Do not add a
  migration just to optimize a Foundation endpoint without authorization.
- Configuration: validate Compose rendering, exact CORS origin, Keycloak public
  client with standard flow + S256 PKCE and disabled implicit/password grants,
  audience mapping, and least-capability actor provisioning.
- When Flutter is unavailable, do not fabricate generated sources, analyzer,
  tests, build, or end-to-end results. Deliver the source/tests and mark those
  commands `NOT EXECUTED`; this is a review condition, not a PASS.
- Every new environment alias must be added to the authoritative configuration
  catalog because the repository contract test checks deployment aliases.

## Review Checklist

- The app is web-first, broker-neutral, and uses only backend contracts.
- Authorization Code + PKCE is evident in code; no implicit/password flow.
- Access token is memory-only; refresh token uses secure storage.
- Bearer retry is bounded and no sensitive value is logged.
- List queries are authorized, projected, stable, and capped at 100.
- Mutations include `expectedVersion` and reload after success/conflict.
- Local passwords come only from ignored environment input.
- No Q-008 aggregate rule or historical migration changed.
- Frontend and backend evidence is honest about tool availability.
