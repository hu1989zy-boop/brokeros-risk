# Q-012 Security Review Evidence

## Authorization and actor integrity

- Both capabilities are explicit bounded values: `decision:record` and
  `decision:read`; no grants were invented.
- Every Decision use case calls the unchanged Q-009 `AuthorizationGuard` before
  a Decision lookup or mutation.
- Record requires `ActorType.HUMAN` immediately after authorization and before
  fingerprint replay, content validation, Q-010, Q-011, or mutation.
- Actor identity is taken only from `ActorContextProvider`; request DTOs cannot
  supply actor, source, record time, or Decision identity.
- Q-010 and Q-011 checks receive the caller's own `ActorContext`. Denial or
  unavailability prevents Decision creation.

## Confidentiality and disclosure

- `DecisionProvenanceView` is structurally unable to return conclusion text.
- The recording response also omits conclusion text.
- Full-detail read requires `decision:read` and completes the dedicated access
  audit before the service returns content.
- A forced audit-write failure raises a fail-closed Decision authority error and
  returns no content; a real-MySQL concurrency test proves the short dedicated
  audit transaction does not affect an unrelated recording.

## Data and persistence safety

- JDBC statements use parameters; no client value is concatenated into
  production SQL.
- Canonical references and single-value enums have database checks as defense in
  depth. Only Decision-internal foreign keys exist; Q-010/Q-011 ownership is
  enforced by live application contracts rather than cross-module SQL coupling.
- Recording transactionally commits Decision, Evidence-reference rows, and
  operation ledger. A test-only trigger proves rollback leaves no partial data.
- Generated DecisionRef collision handling retries only the named unique
  constraint, no more than three attempts, and never overwrites.
- There is no production update/delete SQL or mutation route.

## Logging, metrics, errors, and secrets

- Static and architecture scans found no log statement containing conclusion,
  subject, Evidence references, or actor identity.
- Metrics use bounded operation/outcome/capability/ResultCode tags only.
- Decision expected failures use stable ResultCodes through the existing global
  exception boundary; SQL details and stack traces are not returned to clients.
- No passwords, tokens, full authentication headers, or other credentials were
  added to repository files. Verification commands in this package redact the
  disposable database password.

## Threat findings

No authorization bypass, actor spoofing input, unaudited detail disclosure,
cross-module database ownership violation, raw-content logging, delete path, or
unbounded retry was found in the Q-012 changed scope.

The outstanding Q-011 regression is a verification/governance compatibility
blocker, not a discovered confidentiality or integrity defect in Q-012. The
overall stage nevertheless remains BLOCKED because the mandatory gate is not
green.
