# Q-013 Security Review

## Conclusion

The inspected Q-013 security design and implementation match Requirement §8
and Implementation Design §5/§15. No in-scope security finding was identified.
The overall lifecycle gate is blocked only by the separate Q-012 regression
assertion, not by a Q-013 security failure.

## Authorization and actor restrictions

- Recording calls AuthorizationGuard.requireAllowed with action:record before
  every Action query or mutation.
- HUMAN is checked immediately after authorization and before fingerprint
  replay, content parsing, Q-012 confirmation, or database access.
- confirmProvenance and full-detail reads require action:read but impose no
  ActorType restriction; real MySQL tests prove authorized SERVICE access.
- Q-012 confirmation uses the recording actor's own ActorContext and therefore
  also requires the actor's real decision:read grant.
- Denied and unavailable Action reads perform zero Action queries. Denied
  action:record and decision:read paths perform zero Action writes.
- No capability grant or permissive provider was added.

## Data disclosure and audit

- ActionProvenanceView structurally has no intentText field.
- The recording response also omits intentText.
- Full-detail content is returned only after action_access_log commits through
  a short REQUIRES_NEW, non-read-only transaction.
- A forced audit failure propagates ACTION_AUTHORITY_UNAVAILABLE and no content
  is returned.
- Access auditing records Action, accessing actor, and UTC access time.

## Integrity and abuse resistance

- operationId plus length-framed SHA-256 over raw decisionRef and intentText
  gives deterministic replay semantics.
- Same-operation/same-content concurrency produces one record and one replay.
- Changed content under the same operationId is rejected.
- Generated ActionRef collision retry is limited to three and classifies only
  the named unique constraint.
- DecisionRef validity is checked through Q-012 before any Action write.
- The database enforces canonical identifiers, single-value source/status/type/
  outcome enums, byte bounds, uniqueness, and internal referential integrity.

## Injection, logging, metrics, and credentials

- JDBC values use placeholders; no request content is concatenated into
  production SQL.
- Main Action source contains no logger calls and Action metrics use only
  bounded operation/outcome/capability/ResultCode tags.
- Intent text, DecisionRef, and actor identity are absent from metric tags.
- No application credential, token, authentication header, or secret was
  added. Disposable test credentials were supplied only at command runtime and
  are redacted in this package.

## Deferred security surfaces

Approval/rejection transitions, ActionOutcome, Execution, Account Control, and
vendor adapters remain outside Q-013. No placeholder capability or interface
for them was introduced.
