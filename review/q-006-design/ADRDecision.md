# Q-006 ADR Decision

## Decision

New ADR required: YES — completed.

Accepted identifier: `ADR-008`.

Proposed subject: Spring Boot externalized configuration, typed application
properties, startup validation, and secret-source boundaries.

## Why the ADR Threshold Is Met

Q-006 proposes rules that will govern every future module and environment:

- Spring Boot remains the only configuration-resolution mechanism;
- framework-owned properties stay in native namespaces and are not wrapped;
- application-owned groups use `brokeros.risk.<capability>` and typed validated
  `@ConfigurationProperties`;
- configuration is immutable after startup and dynamic refresh is deferred;
- deployment aliases are compatibility contracts;
- secret values remain externally supplied, with no selected secret product;
- Actuator configuration endpoints remain unexposed.

This is a durable cross-module and deployment contract, not an ordinary local
implementation detail. It therefore meets the AGENTS/Phase 0.6 ADR threshold
even though the current proposal adds no new dependency or process.

## Implementation Result

The architect approved the Requirement and both material choices:

1. whether no production properties class is the correct outcome until a real
   BrokerOS-owned setting exists;
2. whether startup-bound immutable configuration is the approved Phase 1
   strategy.

`docs/adr/ADR-008-configuration-management-foundation.md` was then created with
Context, Decision, Alternatives, and Consequences and marked Accepted before
Phase 2 implementation.

## Alternatives the ADR Should Record

- Continue ad hoc placeholders/`@Value` for application settings — reject.
- Wrap all Spring Boot native properties — reject.
- Add an empty sample properties group — reject.
- Add global configuration scanning immediately — defer until multiple real
  groups justify it; initially prefer explicit registration.
- Add Config Server/Vault/dynamic refresh — defer to a future Requirement.
- Use profiles as configuration/security management — reject.

## Approval Gate

Gate satisfied on 2026-08-18. ADR-008 was accepted before implementation.
