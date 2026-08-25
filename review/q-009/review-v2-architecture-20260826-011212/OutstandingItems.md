# Q-009 Architecture Outstanding Items

## Blocking Current Architect Decision

No packaging or documentation blocker remains. The Architect must review the
architecture and ADR-011.

## Awaiting Architect Decision

1. Accept, reject, or request revision of the pluggable-hybrid identity model.
2. Accept, reject, or request revision of the BrokerOS-owned
   VerifiedPrincipal → ActorRef mapping → ActorContext boundary.
3. Accept, reject, or request revision of capability-based use-case enforcement.
4. Accept, reject, or request revision of the service actor/no-SYSTEM rule.
5. Accept, reject, or request revision of Spring Security as an infrastructure
   adapter only.
6. Accept, reject, or request revision of ADR-011; it remains Proposed.

## Deferred After Architecture Approval

- concrete identity provider and token validation/revocation model;
- service authentication mechanism;
- actor mapping/provisioning and capability policy lifecycle;
- persistence, caching, operations, API, and detailed test design;
- Implementation Design authorization;
- Implementation authorization.

## Explicitly Not Authorized

- Q-009 Implementation Design;
- Q-009 Implementation;
- Q-008 Implementation or a Q-008 V5 design;
- source, dependency, configuration, migration, endpoint, Kafka, Redis,
  Docker, or Kubernetes changes;
- Git staging, commit, or push.

## Recommended Next Step

Architect Review of Q-009 Architecture and proposed ADR.
