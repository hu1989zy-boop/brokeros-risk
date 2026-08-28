# Q-010 Trading Account Reference Authority Implementation Design Lessons Learned

## Design Gate

- Requirement: Q-010 V1 — APPROVED
- Architecture: Q-010 V1 — APPROVED
- ADR-012: ACCEPTED
- Design-phase snapshot: V1 was DRAFT when this lesson was created
- Current Implementation Design: V1 — APPROVED by external Architect on
  2026-08-27
- Approval recording: V6 — awaiting independent Architect review
- Implementation: NOT STARTED
- Implementation Allowed: NO

This lesson records design-phase findings only. The later external approval
does not change its evidence boundary: it does not claim that the proposed Java
contracts, Flyway V3 schema, transactions, command boundary, authorization
integration, MySQL behavior, or tests exist or have passed.

## Existing Contracts Must Shape the New Boundary

Q-009 already owns `ActorContext`, capability evaluation, the service-actor
factory, controlled provisioning, and denial semantics. Q-010 should consume
those contracts rather than create another actor model or authorization
framework. The smallest future composition change is to register one
purpose-specific service descriptor and let the existing Q-009 factory build
its context; it is not permission to weaken or redesign Q-009.

The same rule applies to Q-008. Q-010 can design a narrow eligibility provider
for the approved `TradingAccountRef` prerequisite, but it must not implement or
reshape the blocked Risk Case aggregate. Each consumer use case will still
need its own Q-009 authorization before invoking the independently protected
Q-010 read.

## Idempotency Is a Durable Domain Outcome

A request UUID is not sufficient by itself. The future operation record must
bind that UUID to a deterministic semantic fingerprint and the final durable
outcome inside the same local transaction as current state and immutable
history. Exact replay can return the original result; reuse with different
semantics must fail as a conflict.

The fingerprint must exclude generated references, caller identity, time, and
transport formatting. A typed, length-prefixed byte encoding avoids JSON field
ordering and delimiter ambiguity while preserving the exact external-account
bytes selected by the approved architecture.

## Make Atomic History Difficult to Forget

A generic repository plus an optional history repository would allow a future
service to update current state without provenance. The design instead places
each complete mutation—operation outcome, current state, and immutable
history—behind one high-level mutation port implemented with one local Spring
transaction. This makes the approved atomic-history invariant part of the
adapter contract rather than a convention callers can omit.

Lifecycle changes use compare-and-set version checks. Stale changes fail and
are not automatically retried because retrying against a newer state would
silently change the operator's approved intent.

## Preserve External Identifiers as Bytes

The approved architecture forbids generic numeric parsing, trimming, case
folding, or Unicode normalization. The concrete design therefore treats an
external account key as a validated UTF-8 byte sequence and plans binary MySQL
storage/comparison. This preserves leading zeros and source-defined case while
avoiding deployment-dependent collations.

Any stronger source-specific canonicalization requires a separately approved
adapter/source contract. Q-010 Foundation must not guess it.

## Controlled Provisioning Is Narrower Than an Admin API

The initial boundary needs no REST controller. A strict, one-operation JSON
manifest executed in a non-Web application context keeps registration and
lifecycle transitions explicit and auditable without introducing public CRUD,
bulk behavior, or manual DDL.

The manifest contains business intent and bounded provenance only. It must not
contain credentials, ActorContext, timestamps, proposed generated references,
or unbounded metadata. Actual authority scopes, attestation records, actors,
credentials, and change tickets remain deployment inputs and cannot be
invented in repository defaults.

## Fail Closed Without Leaking Authority State

Q-010's future Q-008 contract should disclose only whether a reference is
eligible, unrecognized, or recognized but not eligible. Inactive and retired
scope/account combinations collapse into the same non-eligible outcome; raw
external tuples, internal database IDs, versions, lifecycle details, and
attestation data stay private.

Authorization must occur before every Q-010 port call. Database outages,
uncertain commits, and authorization dependency failures return bounded
unavailable/denied outcomes and never fall back to cached, caller-supplied, or
vendor data.

## Verification and Skill Assessment

The implementation phase must prove the design with real MySQL 8.4 migration,
binary uniqueness, replay, concurrent registration, compare-and-set lifecycle,
rollback, authorization-ordering, command-manifest, disclosure, logging, and
architecture tests. Mandatory database gates must not silently skip or fall
back to H2.

No repository Skill was added or changed in this design phase. The potentially
reusable transaction/idempotency pattern has not yet been implemented or
runtime-verified; promoting it to a Skill now would turn a proposal into an
unearned standard. Reassess after approved implementation produces executable
evidence.

No Java, test, SQL, Flyway migration, endpoint, configuration, dependency,
Kafka, Redis, adapter implementation, Q-008 implementation, staging, commit,
or push was performed.
