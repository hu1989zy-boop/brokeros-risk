# Q-011 Security Review

## Identity and authorization

- The REST adapter obtains identity only from the existing trusted
  `ActorContextProvider`; request DTOs accept no actor identity, owner, status,
  timestamp, or generated reference.
- Every use case calls existing Q-009 `AuthorizationGuard` before any Q-011
  lookup, mutation, or replay read.
- Capabilities are exactly `evidence:record`, `evidence:correct`, and
  `evidence:read`; no implicit grant or always-allow provider was added.
- Record/Correct require `ActorType.HUMAN` immediately after authorization and
  before replay/content/Q-010. Authorized SERVICE actors are tested as rejected
  even for replay.
- Provenance/full-detail reads require `evidence:read` but correctly impose no
  additional actor-type restriction; authorized SERVICE reads are tested.

## Subject authority and mutation safety

- New recording passes the caller's own trusted context to the unchanged Q-010
  authority. `ELIGIBLE_FOR_NEW_ASSOCIATION` and
  `RECOGNIZED_NOT_ELIGIBLE` are accepted; only `NOT_RECOGNIZED` rejects.
- Correction never calls Q-010, never accepts a replacement subject, and copies
  the target's persisted subject.
- Exact semantic fingerprints and unique operation IDs prevent materially
  different replay. Constraint classification retries only the named generated
  EvidenceRef uniqueness violation, at most three attempts.
- Correction winner election relies on database uniqueness/CAS. Forced history
  failures roll back record, operation, history, and target transition.

## Disclosure and sensitive content

- `EvidenceProvenanceView` has no observation or correction-reason component;
  reflective architecture tests enforce this narrow contract.
- Full-detail read persists its access record in a short dedicated,
  non-read-only `REQUIRES_NEW` transaction before returning content. Audit
  failure returns no content and does not roll back an unrelated concurrent
  recording.
- JDBC VARBINARY is decoded with a strict UTF-8 decoder; malformed stored bytes
  fail authority access closed rather than returning replacement text.
- Q-011 has no logger/System output and metrics use only bounded operation,
  outcome, capability, and ResultCode tags. Observation text, reason, subject,
  actor, and raw references are absent from logs/metric tags.
- Expected errors use typed ResultCodes through the shared
  `GlobalExceptionHandler`; no SQL or stack trace is placed in API responses.

## Database and availability controls

- All FKs use `ON DELETE RESTRICT`; no delete SQL or delete use case exists.
- Named enum/format/byte/bidirectional constraints fail malformed writes at
  the database layer as well as the domain layer.
- Unknown integrity failures, exhausted reference generation, database
  failures, Q-010 unavailability, and access-log failures map to fail-closed
  availability errors.
- Real Q-009/Q-010 grant/deny/unavailability paths were exercised against
  MySQL 8.4.11 with zero Q-011 access on denial.

## Scope and result

No Q-008 wiring, Q-009/Q-010 module change, Kafka/Redis channel, blob/file
storage, production secret, or deployment grant was introduced. No security
defect was found in the authorized Q-011 scope. Repository-wide sign-off is
still withheld because of the separate unchanged Q-009 migration-count test
failure documented in `OutstandingItems.md`.
