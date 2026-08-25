# Q-009 Gap Analysis

## Already Exists

- Phase 1 Java/Spring Boot modular-monolith and adapter boundaries.
- Safe API/error and validation foundations for future use.
- Request ID and W3C Trace ID correlation under Q-005/ADR-007.
- Broker-neutral Core Domain semantics including opaque ActorRef under
  Q-007/ADR-009.
- Q-008 actor-attribution and named-capability needs in its approved V4 Design.
- Security, sensitive-logging, auditability, and external-integration standards.

None of these is an authentication or authorization provider.

## Need Improvement

- Formal separation of external identity, verified principal, ActorRef,
  ActorContext, authorization decision, and Audit actor.
- An explicit caller/server trust boundary.
- Accountable non-human identity instead of a generic `SYSTEM` label.
- Consistent named-capability enforcement across HTTP and internal use cases.
- Auditable allow/deny context that excludes credentials and raw claims.

## Missing

- approved identity authority and authentication mechanism;
- Spring Security or another approved security runtime;
- JWT/OAuth2/OIDC/session/gateway/proxy trust contract;
- verified principal and principal-to-ActorRef mapping authority;
- trusted ActorContext owner/provider;
- capability catalog, policy source, and authorization decision provider;
- human/service provisioning, deactivation, credential, and lifecycle rules;
- failure/API contract and focused security verification.

## Out of Scope

- Q-008 implementation or redesign;
- Trading Account, Evidence, Decision, Rule Engine, Action, ActionOutcome,
  Account Control, Execution Engine, MT4/MT5, Manager API, CRM login, or other
  owning capability implementation;
- customer/IB login, KYC, full IAM, company directory, organization model, SSO
  administration, permission UI, break-glass, or delegation;
- Kafka authorization, microservices, API Gateway deployment, Flink, Python;
- Java/security configuration, dependencies, persistence, endpoints, cache,
  messaging, Docker, Kubernetes, or CI changes.

## Open Decisions

Identity authority, authentication mechanism, security framework, ActorRef
mapping ownership, service identity, capability-policy source, target context,
availability/freshness, API failures, operational endpoints, delegation, and
local/test trust strategy all remain open for Architect review and later
Architecture/ADR work.

## Future Q-008 Provider Prerequisite Sequencing

Q-009 addresses only trusted ActorContext and authorization. Authoritative
Trading Account, Evidence, Decision, Action, and ActionOutcome providers remain
separate prerequisites. They are not implemented, absorbed into Q-009, given
new designs, or assigned speculative Requirement IDs. Even an approved and
implemented Q-009 would not by itself authorize Q-008 implementation.
