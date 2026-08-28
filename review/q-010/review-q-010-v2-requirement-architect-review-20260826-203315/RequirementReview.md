# Q-010 Requirement Review

## Review Outcome

- Decision: `APPROVED`
- Requirement: `Q-010 — Trading Account Reference Authority Foundation`
- Approved version: `V1`
- Architecture: `NOT STARTED`
- ADR: `REQUIRED — NOT CREATED`
- Implementation: `NOT STARTED / NOT ALLOWED`

## Correct Next Requirement

Q-010 follows the approved dependency direction. Q-009 supplies trusted actors
and capability authorization, but Q-008 still has no authority for its only
approved initial subject type, `TRADING_ACCOUNT`. Establishing that authority
does not depend on Evidence, Decision, Action, or ActionOutcome and therefore
is a smaller safe increment than starting those capabilities or Risk Case.

The Requirement remains upstream and supporting. It neither enters the
Evidence → Decision → Action → Risk Case ownership chain nor changes Decision
as the Core Domain.

## Cohesion and Minimality

The approved scope owns exactly one problem: stable BrokerOS Trading Account
identity and authoritative recognition. It includes only the mapping,
lifecycle, provenance, protected registration, and protected read behavior
needed to make the reference trustworthy.

It explicitly excludes customer/account master data, trading data, financial
values, vendor integration, auto-discovery, synchronization, UI, generalized
entity modeling, Rule Engine, Account Control, Audit platform, and Q-008
implementation.

## Authority, Scope, and Uniqueness

The complete external identity is:

```text
AccountAuthorityScopeRef + SourceNamespace + ExternalAccountKey
```

The authority scope is an opaque BrokerOS-owned reference and does not create
a Broker/Tenant/Organization master. The namespace identifies one governed
source instance and distinguishes source/platform, server, and environment
where collisions are possible. The complete tuple is unique.

Foundation cardinality is deliberately one-to-one. Aliases, merges,
cross-source migration, and reassignment would introduce identity-merging and
conflict authority that the current Requirement cannot justify.

## Lifecycle and Historical Resolution

Active references may support new associations. Deactivated or retired
references remain resolvable for history but are not eligible for new use.
Neither stable references nor their external identities may be deleted,
silently reused, or reassigned. Any reactivation can restore only the same
immutable mapping through an attributable operation.

## Real Initial Authority

The initial Foundation authorizes only controlled non-web provisioning. The
invoker must be a deployment-designated, pre-provisioned Q-009 actor with an
exact Q-010 capability. The manifest must be backed by a broker/source-owner-
approved record and retain bounded provenance.

This is not auto-enrollment. An HTTP caller, runtime consumer, unknown
principal, raw external key, direct external-database read, or discovered
vendor record cannot create authority. If the deployment cannot supply the
approved source attestation, implementation remains blocked.

## Consumer and Failure Boundary

Q-008 may validate a `TradingAccountRef` through a protected read-only
application contract and receive only recognized/eligible state with bounded
authority version/provenance. It receives no external account key, vendor DTO,
customer record, or source payload.

Authorization occurs before access. Unauthorized outcomes hide existence;
authorized unknown values are not found; inactive references are recognized
but ineligible; conflicts never select a winner; authority/database
unavailability returns a safe unavailable outcome and cannot create, mutate,
or return stale success.

## Gate Conclusion

No unresolved Requirement-boundary defect prevents approval. Architecture must
turn these decisions into a technical design and proposed ADR under a separate
authorization. It may not weaken or reopen the approved Requirement boundary.
