# Q-009 Outstanding Items

## Approval and gates

- Architect must approve or revise the Draft Requirement.
- Identity Authority remains **OPEN**.
- ADR is required but may be created only after Requirement approval and
  Architecture Gate authorization.
- Architecture, Design, and Implementation remain **NOT STARTED**.
- Implementation Allowed remains **NO**.

## Open architecture decisions

1. Real Phase 1 identity authority for humans and services.
2. Authentication mechanism and trusted-hop/bypass protections.
3. Security framework/dependency selection.
4. Principal-to-ActorRef mapping ownership, storage, lifecycle, and Audit.
5. Purpose-specific service identity provisioning and credential rotation.
6. Capability catalog/policy ownership, versioning, mapping, and invalidation.
7. Required target/resource authorization context.
8. Timeout, selective retry, cache, revocation, freshness, and outage details.
9. Safe unauthenticated/forbidden/indeterminate API failure contract.
10. Policy for Actuator, OpenAPI, Swagger, and other operational endpoints.
11. Whether delegation, impersonation, or break-glass is needed at all.
12. Test-context strategy without a permissive production fallback.

## Future Q-008 Provider Prerequisite Sequencing

Separate owning capabilities must eventually supply authoritative Trading
Account, Evidence, Decision, Action, and ActionOutcome providers. They remain
unimplemented and unnumbered. Q-009 does not remove those blockers and does not
authorize Q-008 implementation.

## Recommended next step

Architect Review of the Q-009 Requirement only.

====================================
Codex Prompt
====================================

Review `docs/requirements/Q-009-Requirement.md` as the Q-009 Architect
Requirement Gate. Verify its trusted identity boundary, ActorRef/ActorContext
distinction, HUMAN/SERVICE actor model, capability-based default-deny
authorization direction, Audit attribution, broker neutrality, Q-008
dependency, provider-prerequisite separation, ADR determination, and open
decisions. Return an explicit APPROVE or REVISE decision with findings. If
approved, authorize only the next Q-009 Architecture/ADR analysis gate. Do not
create implementation, modify Q-008 Design V4, authorize Q-008 implementation,
or implement any provider prerequisite.
