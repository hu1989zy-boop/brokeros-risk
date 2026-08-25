# Q-009 Architecture V2 Architect Approval

## Architect Decision

- Architecture: Q-009 Trusted Actor and Authorization Foundation
- Architecture version: V2
- Architect Decision: APPROVED
- Decision origin: explicit external Architect Review decision supplied by the
  Product Owner
- Decision recorded: 2026-08-26
- No Architecture V3 required: YES
- Implementation permission granted: NO

No Q-009 Architecture V3 is required. The `v3` in this Review directory and ZIP
is the Approval Review Package version only.

## Approved Architecture Decisions

1. Identity authority architecture is Pluggable-Hybrid: an external/pluggable
   authority authenticates credentials while BrokerOS owns stable actor,
   mapping, and capability semantics.
2. The concrete identity provider remains OPEN.
3. BrokerOS does not own external human credentials by default.
4. Authentication adapters translate trusted provider/framework identity into
   a required BrokerOS-owned `VerifiedPrincipal`.
5. BrokerOS owns authoritative principal-to-ActorRef mapping and lifecycle.
6. `ActorRef` remains an opaque, broker-neutral domain identity reference.
7. Q-009 owns an immutable, trusted, per-execution `ActorContext`.
8. HUMAN and SERVICE identities are distinct; there is no generic privileged
   SYSTEM actor.
9. Authorization is capability-based, default-deny, explicit-allow, and
   least-privilege.
10. Application code authorizes capabilities, not external roles. External
    roles may only be governed inputs to BrokerOS capability mappings.
11. Each business module owns its business capability catalog; Q-009 owns the
    authorization model, naming convention, decision boundary, and default-deny
    semantics.
12. The application use-case boundary is the authoritative authorization
    enforcement point before protected access or mutation.
13. Controller, route, and framework checks may exist only as defense in depth.
14. Audit actor attribution originates from trusted ActorContext, never from
    caller-selected actor fields or identity headers.
15. Spring Security may be used as an infrastructure/framework adapter, while
    application and domain cores remain independent of Spring Security types.
16. All unauthenticated, unmapped, inactive, denied, unavailable, or
    indeterminate paths fail closed.

## Approved Trust Flow

```text
External / Pluggable Identity Authority
        ↓
Authentication Adapter
        ↓
VerifiedPrincipal
        ↓
Authoritative Actor Mapping
        ↓
ActorRef
        ↓
ActorContext
        ↓
Capability Authorization
        ↓
Application Use Case
        ↓
Audit Attribution
```

## Deferred Without Reopening Architecture

Concrete provider, token validation, service credentials, persistence,
provisioning, caching/invalidation, runtime wiring, and concrete contracts move
to Implementation Design. They do not require Architecture V3 unless a future
proposal would change an approved boundary.
