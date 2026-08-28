# Q-010 V2 Requirement Architect Review Summary

## Decision

**APPROVED — Q-010 Requirement V1**

Trading Account Reference Authority Foundation is the correct next formal
Requirement. Its approved boundary is cohesive, minimal, broker-neutral, and
creates an upstream reference authority rather than a full account master or a
caller-input validation facade.

## Approved Corrections and Resolutions

The Requirement now fixes:

- `AccountAuthorityScopeRef + SourceNamespace + ExternalAccountKey` as the
  complete authoritative external identity;
- one external identity to one `TradingAccountRef`, and one authoritative
  external identity per `TradingAccountRef` in the Foundation;
- no aliases, merges, reassignment, physical deletion, or silent reuse;
- active eligibility and historical resolution after deactivation/retirement;
- controlled non-web provisioning by a pre-provisioned Q-009 actor, backed by
  broker/source-owner-approved provenance;
- a minimal protected Q-008 reference-validation contract with no external-key
  or vendor/customer disclosure;
- exact Q-010 read/register/lifecycle capabilities;
- durable same-transaction mutation provenance; and
- fail-closed not-found, ineligible, conflict, denial, and unavailable
  semantics.

## Q-008 Decision

The Q-008 Architect Approval record explicitly states that the V4 Design
header and Section 17 preserve submission-time status and that the later
approval record is authoritative. They are intentional history, not an active
gate inconsistency. No Q-008 repair or substantive change is required.

## Boundary

ADR analysis is formally required, but no ADR was created or accepted.
Architecture is not started. No production code, tests, migration,
configuration, dependency, API, Kafka, Redis, adapter, infrastructure, Q-008
implementation, Git staging, commit, or push was performed.
