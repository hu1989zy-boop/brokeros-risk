# Q-010 V5 Security Design Review

## Result

- Security Design: **PASS FOR EXTERNAL ARCHITECT REVIEW**
- Parallel authorization model introduced: **NO**
- Public/admin REST surface introduced: **NO**
- Implementation Allowed: **NO**

## Authorization Boundary

Q-010 reuses the committed Q-009 `ActorContext`, `Capability`,
`AuthorizationGuard`, and failure model. Every service calls the guard before
any Q-010 query/mutation port. Denied or unavailable authorization therefore
cannot reveal whether a scope, account, tuple, lifecycle, or provenance record
exists.

Exact capabilities are:

- `trading-account-reference:read`;
- `trading-account-reference:register`; and
- `trading-account-reference:change-lifecycle`.

The controlled command obtains a fresh context from one registered singleton
descriptor with service code `trading-account-reference-provisioner`. It
cannot accept an ActorRef, reuse a human context, fabricate a token, assert a
generic SYSTEM identity, or bypass active Q-009 mapping/grant checks.

## Manifest and Replay Controls

- one regular non-symlink UTF-8 file, maximum 64 KiB, one operation;
- strict schema V1, duplicate/unknown/trailing-token rejection;
- no actor/operator field, caller timestamp, generated ref, credential, token,
  arbitrary metadata, customer/vendor payload, or inline JSON secret channel;
- required bounded attestation, reason, and change reference;
- one canonical UUIDv4 operation ID bound to a typed SHA-256 semantic
  fingerprint and durable result; and
- changed replay conflicts, while exact replay returns the committed result.

Actual attestation truth remains deployment governance. Q-010 records and
validates a bounded reference but does not invent a cryptographic verifier or
external approval API without an approved protocol.

## Disclosure and Tamper Controls

- Q-008 receives only a TradingAccountRef, bounded decision, and optional
  opaque snapshot/provenance refs.
- Raw external keys, namespace, scope ref, lifecycle/version detail, internal
  ID, attestation, actor, customer/vendor data, and persistence records never
  cross the Q-008 boundary.
- External keys/tuples, manifest body, fingerprint, attestation, reason,
  ActorRef, SQL/constraint values, credentials, and tokens are excluded from
  ordinary logs and metric tags.
- Identity columns have no update/delete use case; database uniqueness and
  restricted FKs block remap/destruction through the application model.
- Current state, durable outcome, and immutable history are one transaction;
  forced history failure must roll everything back.

## Residual Security Inputs and Verification

Deployment must supply real approved scope/attestation/change records, Q-009
service mapping/grants, credentials, and command access controls. Future tests
must cover denial-before-lookup, missing/revoked service authority, DB outage,
replay/tampering, concurrency, rollback, logging/output leakage, direct table
coupling, and no HTTP listener.

No unresolved security-design violation blocks external Architect review.
