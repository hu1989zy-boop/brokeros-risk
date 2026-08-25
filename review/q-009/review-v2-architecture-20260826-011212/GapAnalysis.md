# Q-009 Gap Analysis

## Baseline Gap

The current backend has request and trace correlation but no trusted actor or
authorization foundation. Repository inspection found no Spring Security or
OAuth2 resource-server dependency, authentication adapter, actor mapping,
ActorContext, capability policy, or authorization enforcement boundary.

## Architecture Gaps Resolved by This Proposal

- identity authority ownership model: resolved as pluggable-hybrid;
- human/service trust pipeline: resolved conceptually;
- `VerifiedPrincipal`, ActorRef mapping, and ActorContext ownership: resolved;
- capability semantics and naming: resolved;
- authoritative enforcement point: resolved;
- Spring Security dependency direction: resolved;
- background actor and no-SYSTEM rule: resolved;
- fail-closed, audit, operational endpoint, and context rules: resolved;
- ADR need and scope: resolved in proposed ADR-011.

## Intentionally Open Before Implementation

| Gap | Why it remains open | Required next authority |
| --- | --- | --- |
| Concrete identity provider | Deployment/product evidence is absent | Architect/provider decision |
| JWT vs opaque/introspection and OIDC flow | Depends on provider, revocation, UI/BFF, availability | Architecture/Implementation Design |
| Service credential mechanism | Depends on invocation/deployment environment | Architecture/Implementation Design |
| Actor provisioning/lifecycle API | Administrative workflow not required yet | Requirement/Implementation Design |
| Actor mapping and policy schema | Implementation is prohibited this turn | Approved Implementation Design + Flyway review |
| Policy storage/cache/invalidation | Requires consistency and availability decisions | Implementation Design/ADR if material |
| Broker/tenant organization model | Not approved in Q-009 | New/changed Requirement and architecture |
| Exact result codes/routes | Must follow approved API design | Implementation Design |
| Async identity/delegation | No async business path is in current scope | Separate Requirement/ADR |

## Q-008 Remaining Gap

The proposal can eventually provide trusted ActorRef and capability decisions,
but it does not authorize Q-008. Q-008 also lacks approved Trading Account,
Evidence, Decision, Action, and ActionOutcome provider contracts. No Q-008 V5
or implementation was created.

## Dependency Gap

Future HTTP security implementation is expected to need Spring Security and
likely OAuth2 Resource Server dependencies. Exact token-specific dependencies
remain open, and `backend/pom.xml` was not changed.

## Gap Review Result

The remaining gaps are explicit and appropriately deferred. None has been
silently filled with a provider-specific or implementation assumption.
