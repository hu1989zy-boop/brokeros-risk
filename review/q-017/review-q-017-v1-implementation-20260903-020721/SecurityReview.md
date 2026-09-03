# Q-017 Security Review

## Decision

**PASS WITH CONDITIONS** — static and automated authorization/identity controls
pass; live Keycloak-to-backend enforcement remains unexecuted.

## Authentication and identity

- Q-017 reuses the existing centralized Q-016 axios client without modification:
  Bearer attachment, one coordinated refresh/retry, typed `403`, and envelope
  parsing remain the only transport path.
- No action accepts actor identity. Every one of the 11 exact-body tests asserts
  that `actorRef` is absent. The verified JWT remains the backend actor source.
- No access token, client secret, password, signed JWT, refresh token, or
  authorization header is logged or persisted by Q-017 code.

## Authorization

- UI action availability uses only the accepted case-status map. It is not
  presented or implemented as an authorization decision.
- No JWT roles/claims are parsed and no capability probe is added.
- All operations surface backend ResultCodes. All 11 paths test a real typed
  `AuthorizationError` produced from HTTP 403.
- The local operator bootstrap holds exactly `read`, `note`, `assign`, `review`,
  `resolve`, `close`, `cancel`, and `reopen`; it does not hold `associate` or
  `create`.
- The same exact set is the documented production authorization expectation;
  production identity provisioning remains an operational responsibility and
  is not changed by this local bootstrap.

## Input and browser safety

- Ant Design controlled fields and React text rendering are used. No raw HTML,
  dynamic code evaluation, or shell/process call is introduced.
- Required, length, enum, canonical reference, and multi-reference validation
  improve usability; the backend remains authoritative for all invariants.
- Terminal actions require validation followed by an explicit second
  confirmation. Pending requests disable form and modal controls.
- Conflict recovery preserves in-memory form values, refetches authoritative
  server state, and retries with the new version; it does not silently overwrite
  the user's input or client-patch the aggregate.

## Data and artifact handling

- Case/note/history reads remain bounded by the inherited repository behavior.
- Notes are shown using metadata/ref fields returned by history; unavailable
  note content is not guessed or duplicated into client state.
- Playwright configuration keeps trace, screenshot, and video off for live
  authentication and case flows. No live browser artifact was produced because
  the spec was skipped before execution.
- Source fixtures contain only synthetic opaque identifiers and test strings.

## Outstanding dynamic evidence

Run the live Q-017 lifecycle test with the local bootstrap applied to verify the
real Keycloak principal mapping, all capability guards, version increments,
audit history, and backend invariant responses. Keep credentials external and
artifact capture disabled.
