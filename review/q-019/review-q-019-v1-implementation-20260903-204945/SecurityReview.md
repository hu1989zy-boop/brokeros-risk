# Q-019 Security Review

## Result

**PASS** for the implemented scope. No unresolved security-standard violation was
found. AC4's unexecuted live flow is a verification condition, not evidence of a
security bypass.

## Authorization and information disclosure

- `RiskCaseQueryService.associations` calls the existing
  `RiskCaseCapabilities.READ` authorization guard before parsing/looking up the
  case target.
- A real-MySQL HTTP test requests a nonexistent case without the capability and
  receives `403 AUTHORIZATION_DENIED`, demonstrating that case existence is not
  disclosed first.
- An authorized nonexistent case uses the established `404 RISK_CASE_NOT_FOUND`
  contract.
- No new capability, bootstrap grant, role mapping, JWT claim parser, or client
  capability inference was introduced.

## Response minimization

The response is limited to case number/version and case-owned association refs,
enum/state markers, source, replacement refs, and occurrence timestamps. It does
not expose internal database IDs, actor refs, reasons, external entity bodies,
authentication data, or persistence entities. The existing REST contract test
checks nested Q-019 response records for internal-ID field names.

## Input and query safety

- The path value is parsed by the existing `CaseNumber` value object and maps an
  invalid/missing target to the established not-found behavior.
- Both new JDBC queries use positional parameters; no user value is concatenated
  into SQL.
- Deterministic ordering and the 500-per-collection cap prevent unbounded response
  emission. Over-cap data fails closed; it is not partially returned as an
  apparently authoritative projection.

## Frontend security

- The existing Bearer-aware `ApiClient` is reused. No token or actor identity is
  added to a URL, request body, fixture, log, or browser storage.
- Network data is validated by strict typed parsers before UI use.
- React text rendering is used for all refs/source fields; no HTML injection API,
  dynamic evaluation, or new browser persistence was introduced.
- Reference selection is usability guidance only; the backend remains the
  authority for every mutation.

## Credential handling

MySQL and Keycloak credential values are not included in this package. Verification
commands name the environment variables but redact their values. The package is
scanned before archiving for likely credential/private-key material.

## Deferred live verification

The full Keycloak/browser path was not executed because credentials and real seeded
references were unavailable. No security behavior from that live path is claimed;
the 403 authorization behavior is covered at the backend HTTP boundary against
real MySQL.
