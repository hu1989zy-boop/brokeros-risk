# Q-010 ADR Determination

## Determination

**ADR REQUIRED: YES**

- ADR created: NO
- ADR accepted: NO
- Architecture started: NO

## Rationale

Q-010 creates a durable business identity and source-of-truth boundary, owns
the application-side external identity mapping, defines lifecycle and
historical resolution, publishes a cross-capability read contract, and fixes
the boundary between BrokerOS-owned references and external systems. These are
long-lived ownership, data, integration, security, and dependency-direction
decisions that meet the repository ADR threshold.

## Required ADR Scope

The separately authorized Architecture phase should propose an ADR covering:

- Trading Account reference and authority-scope ownership;
- external identity tuple and canonicalization;
- one-to-one cardinality and no-reassignment rule;
- source-of-truth and controlled non-web provisioning boundary;
- lifecycle, historical resolution, and append-only provenance;
- Q-009 capability enforcement and disclosure boundary;
- application-owned persistence and external-system isolation; and
- Q-008/internal consumer contract and fail-closed semantics.

This review does not reserve an ADR number, create a proposed ADR, or claim ADR
acceptance.
