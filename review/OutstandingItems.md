# Q-006 Outstanding Items

## Blocking Before Final PASS

1. Create a Q-006 commit only after explicit user authorization.
2. Push that revision only after explicit user authorization.
3. Confirm the existing GitHub Actions workflow passes for the exact Q-006
   commit, including Compose config, isolated startup, MySQL/Flyway, Redis,
   Kafka, backend health, fatal-log scan, and cleanup.
4. Refresh the root Review with immutable commit/run/job evidence and obtain
   Architect Final Review.

The local Docker tool is absent, so the runtime gate cannot be completed on this
host. This is the only blocker to final Q-006 PASS.

## Known Non-Blocking Issues

- Local Maven runs on Java 23 while compiling release 21. The project and CI
  target remain Java 21.
- Mockito/Byte Buddy emits a future dynamic-agent warning during tests. All 26
  tests pass.
- Production correctness depends on explicitly activating the `prod` profile.
  Q-006 documents and tests the contract but does not turn profile selection
  into authorization or environment admission.
- The working tree contains pre-existing Q-004 documentation changes. They are
  preserved and not claimed as Q-006 implementation.
- The user-owned `review/review-history/` archive remains outside Q-006. It was
  not read, modified, deleted, staged, committed, or included in the tree scan.

## Intentionally Not Implemented (YAGNI)

- No production `@ConfigurationProperties` class because no real BrokerOS-owned
  setting currently exists.
- No wrapper around datasource/Hikari, Redis, Kafka, Flyway, server,
  management, logging, tracing, or SpringDoc properties.
- No empty `BrokerProperties`, `Mt4Properties`, or `Mt5Properties` container.
- No custom validator because no application-owned invariant exists.
- No remote configuration/Secret system or dynamic refresh.
- No runtime configuration API/UI, persistence, approval, rollback, or audit
  workflow.

## Deferred Work

The following are possible subjects for a future formally approved Requirement;
they are not automatically authorized Q-007 scope:

- Introduce the first immutable validated BrokerOS-owned properties type only
  alongside a concrete approved consuming capability.
- Evaluate stronger production environment admission only if operations define
  a real requirement beyond explicit profile activation.
- Select a Secret provider only with requirements for authentication,
  authorization, rotation, availability, failure, and incident response.
- Design dynamic/business configuration only with versioning, audit, approval,
  rollout, rollback, and multi-instance consistency semantics.
- Address the Mockito future agent-attachment warning in a dedicated test-
  foundation task if/when the supported JDK requires it.

Business modules, business tables, Redis business state, Kafka topics/events,
MT4/MT5 Manager SDKs, Audit Module, RBAC, microservices, and prohibited
technologies remain outside Q-006.

## Recommendation for Q-007

Do not infer or implement Q-007 from this Review. After Q-006 final PASS, define
Q-007 through a formal Requirement and architecture review. If Q-007 introduces
the first real BrokerOS-owned configuration, apply ADR-008 and the new skill;
otherwise keep the current no-properties-class boundary.
