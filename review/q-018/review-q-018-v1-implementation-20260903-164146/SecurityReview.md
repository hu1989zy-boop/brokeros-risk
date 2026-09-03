# Q-018 Security Review

## Assessment

Static implementation security is **PASS**; real Keycloak-to-backend enforcement
is unverified. The overall implementation gate remains **BLOCKED** by reference
and association-read contract defects described in the other package files.

## Authentication and identity

- Q-018 reuses the centralized Q-016 `ApiClient`: Bearer attachment, one
  coordinated 401 refresh/retry, typed 403, timeout, and `ApiResponse` parsing
  remain the only transport path.
- No Group C field accepts actor identity. All six exact-body tests assert that
  `actorRef` is absent. The verified JWT remains the backend actor source.
- No access token, client secret, password, signed JWT, refresh token, or full
  authorization header is logged or persisted by Q-018 code.

## Authorization and least privilege

- UI status/action availability is not presented as authorization. No token
  claim, role, or capability is parsed and no capability probe is introduced.
- Backend 403 responses pass through the typed error path; each of the six
  operations has a 403 component test.
- The existing console operator bootstrap receives exactly the five requested
  additions: `risk-case:associate`, `evidence:read`, `decision:read`,
  `action:read`, and `action-outcome:read`. Existing Q-016/Q-017 permissions are
  preserved; no create/admin/wildcard capability is added.
- Production identity provisioning and capability application remain an
  operational responsibility; this run did not apply the bootstrap to a live
  environment.

## Input and browser safety

- Exact lowercase UUIDv4 reference formats, required fields, source/reason
  lengths, and disposition enums are client validation for usability. Backend
  recognition and business invariants remain authoritative.
- External reference submit is disabled until a small typed GET response parses
  and its returned reference exactly matches the requested reference.
- Preview models deliberately omit evidence observation, decision conclusion,
  action intent, outcome text, source actor, and other content not needed for
  identity confirmation.
- React/Ant Design text rendering is used; there is no raw HTML, script
  execution, dynamic code evaluation, or URL constructed from an unencoded path
  segment.
- Pending mutation controls are disabled, and conflict recovery refetches
  authoritative state while preserving in-memory form data for review.

## Data-integrity limitation

The evidence disposition target's event ID is not in detail/history and has no
existing preview endpoint. The fallback field accepts only a canonical UUID and
shows an explicit warning that it cannot be previewed. This is weaker than the
approved on-case confirmation control and is not treated as PASS; it contributes
to the overall BLOCKED gate. No opaque-ID translation is attempted for the
approved-versus-backend prefix mismatch.

## Data and artifact handling

- Risk Case history remains bounded to 100 entries; the UI warns when more
  history exists and never claims the reconstructed view is complete.
- Query keys hold only the entered reference needed for the current in-memory
  preview. No reference/reason/entity content is logged or persisted by Q-018.
- Playwright trace, screenshot, and video remain disabled. The live test skipped
  before browser execution, so no authenticated artifact was produced.
- Test identifiers, reasons, and Bearer strings are synthetic placeholders, not
  real customer or credential data.

## Required dynamic evidence

After governance aligns the reference formats and association read contract,
apply the updated bootstrap and run the live Q-018 slice. Verify real principal
mapping, all five new capability checks, reference GET authorization, six POST
guards, version increments, and visible association/audit state with credential
capture still disabled.
