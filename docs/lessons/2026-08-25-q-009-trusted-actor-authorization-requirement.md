# Q-009 Trusted Actor and Authorization Requirement Discovery — Lessons Learned

## Context

Q-009 Requirement Discovery examined the repository state needed to provide a
trusted actor and authorization prerequisite for Q-008 Risk Case Foundation.
The authorized work was documentation and review packaging only.

## What the repository actually showed

- `ActorRef` exists as an approved broker-neutral domain semantic, but no
  authentication authority, authenticated principal, ActorContext provider, or
  authorization decision provider exists.
- Request ID and Trace ID are correlation identifiers under Q-005/ADR-007.
  They are deliberately unsuitable as identity, authorization, or Audit actor.
- The backend has no Spring Security dependency or configuration and no JWT,
  OAuth2, OIDC, session, gateway-identity, or proxy-identity contract.
- Q-008 V4 names capability checks and trusted ActorContext consumption, but
  its latest Architect prerequisite review correctly leaves implementation
  authorization blocked.

## Decisions captured at Requirement level

- Identity Authority remains an explicit open decision. Selecting a framework
  or token format before deciding the authority and trust boundary would not
  solve the identity problem.
- The authorization need is expressed as named capability checks with default
  deny, least privilege, server-side enforcement, and auditable decisions.
  External roles or groups may become mapped policy inputs later, but are not
  direct authorization proof.
- The minimum actor taxonomy is `HUMAN` and purpose-specific `SERVICE`.
  A generic privileged `SYSTEM` identity was rejected because it would erase
  provenance and encourage an authorization bypass.
- Caller-selected actor headers, DTO fields, correlation identifiers, and
  unverified claims remain outside the trusted boundary.

## Scope discipline

Q-009 supplies only the Trusted Actor/Authorization prerequisite. Trading
Account, Evidence, Decision, Action, and ActionOutcome authoritative providers
remain separately owned, unimplemented, and unnumbered. No Q-008 design or
implementation artifact was changed.

## Validation outcome

No implementation problem occurred because no implementation was authorized.
The central risk was semantic overstatement: treating an opaque ActorRef as
proof of authentication or authorization. The Requirement and Review make
that distinction explicit and preserve open architecture choices honestly.

## Reuse evaluation

No repository Skill update is justified at this gate. The existing development
standards, observability-correlation Skill, and Core Domain Skill already state
the reusable trust-boundary, correlation, and domain-model rules. A dedicated
security Skill should be evaluated only after an approved architecture and
verified implementation establish reusable patterns rather than conjecture.
