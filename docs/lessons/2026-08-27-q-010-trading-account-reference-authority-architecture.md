# Q-010 Trading Account Reference Authority Architecture Lessons Learned

## Stable Identity Is Not an External Key

A vendor account key can identify a row inside one source but cannot safely
identify a BrokerOS domain subject. Stable authority requires an independent
BrokerOS reference and a complete immutable scope/source/server/environment/
key tuple. Otherwise identical account numbers or external reassignment can
silently rewrite historical meaning.

## Reject Lossy Generic Canonicalization

External account identifiers are not universally numeric or case-insensitive.
Parsing them as numbers drops leading zeros; trimming, case folding, or Unicode
normalization can collapse values that a source considers distinct. The safe
generic boundary preserves exact canonical UTF-8 bytes and rejects malformed
edge whitespace/control characters. Any additional normalization belongs to a
real approved source contract, not to Q-010 guesses.

## Authorization and Attestation Are Different Proofs

Q-009 proves which trusted actor may invoke registration. It does not prove
that the submitted external mapping is true. Q-010 therefore also requires a
broker/source-owner-approved record with bounded provenance. Both proofs are
necessary; neither replaces the other.

## Immutability Simplifies Integrity

Representing the immutable tuple and TradingAccountRef in one current-state
authority record allows unique constraints in both directions. Lifecycle can
change independently while identity never does. This is safer than a generic
mapping table that quietly invites aliases, reassignment, and merge behavior.

## Idempotency Must Survive Uncertain Outcomes

An idempotency key alone is insufficient. It must be tied to a canonical
semantic fingerprint and a durable result in the same transaction as state and
history. Exact replay can then return the original result, while a changed
retry conflicts. Concurrent uniqueness losers may return unchanged only when
the durable registration is semantically identical.

## Authorization Precedes Resolution

Account-reference existence is protected information. Every reference or tuple
lookup must use a fresh Q-009 ActorContext and exact capability before Q-010
data access. A purpose-specific SERVICE actor can drive offline provisioning
without a generic SYSTEM bypass or an HTTP token.

## MySQL Is the Smallest Coherent Authority

The Foundation needs bidirectional uniqueness, optimistic concurrency,
idempotent replay, lifecycle, and immutable history in one transaction. The
existing application MySQL/Spring JDBC/Flyway stack provides that boundary.
Redis or Kafka would add freshness and dual-write failure modes without
replacing the durable relational constraints.

## Skill Assessment

No repository Skill was added or changed. The architecture was approved and
ADR-012 accepted through the external Architect decision recorded in Q-010 V4,
but its patterns have not been implemented or runtime-verified. The reusable
findings remain recorded here, and Skill creation should be reconsidered after
approved Implementation Design and real MySQL/runtime verification establish a
repeatable pattern.

No Java, tests, SQL, Flyway migration, API, configuration, dependency, Kafka,
Redis, adapter, infrastructure, Q-008 implementation, staging, commit, or push
was performed.
