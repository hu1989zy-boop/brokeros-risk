# ADR-008: Configuration Management Foundation

- Status: Accepted
- Date: 2026-08-18
- Requirement: Q-006

## Context

BrokerOS Risk already uses Spring Boot externalized configuration for its Java
21 modular monolith. Framework-owned settings are supplied through packaged
base/test/prod YAML, environment-variable placeholders, Docker Compose, and
Kubernetes ConfigMap/Secret references. Jakarta Bean Validation and the Spring
Boot test foundation already exist.

The production source currently has no `@Value`, application-owned
`@ConfigurationProperties`, direct `Environment`, or system-environment access.
Most settings belong to Spring Boot or another framework under `spring.*`,
`server.*`, `management.*`, `logging.*`, and `springdoc.*`. Future BrokerOS
capabilities still need a durable rule for ownership, naming, typed binding,
validation, sources, secrets, compatibility, and runtime lifecycle.

Without one strategy, future work could duplicate framework binders, scatter
string-based injection, add hidden defaults, expose secrets, silently rename
deployment aliases, or introduce dynamic configuration without consistency and
audit semantics.

## Decision

- Spring Boot Externalized Configuration is the only BrokerOS Risk runtime
  configuration mechanism during Phase 1.
- Keep framework-owned configuration in its native namespace and use the
  framework's native binder. Do not wrap datasource/Hikari, Flyway, Redis,
  Kafka, server, management, logging, SpringDoc, or other framework properties
  in BrokerOS-specific property classes.
- Reserve canonical lower-case kebab prefixes of the form
  `brokeros.risk.<capability>` for real BrokerOS Risk-owned configuration.
- Use one cohesive, immutable `@ConfigurationProperties` type for a real
  application-owned group. Register it explicitly while the number of groups is
  small, use semantic Java types, and apply `@Validated` plus Jakarta Bean
  Validation for required or bounded values.
- Do not create an empty, sample-only, speculative, or duplicate production
  properties type. When no real BrokerOS-owned configuration exists, no
  production `@ConfigurationProperties` class is the correct YAGNI outcome.
- Do not use scattered `@Value`, direct `Environment` lookups, or
  `System.getenv`/`System.getProperty` for grouped BrokerOS-owned settings.
- Resolve and validate configuration at startup, then treat it as immutable for
  the process lifetime. Invalid required application configuration fails fast.
  Binding and validation perform no external I/O.
- Treat canonical property names, deployment aliases, types, units, defaults,
  requiredness, profile behavior, and sensitivity as compatibility-reviewed
  external contracts. Do not rename or change them silently.
- Keep production secret values outside tracked YAML, source, tests, images,
  logs, exceptions, Actuator exposure, documentation, and Review evidence.
  Local populated `.env` files remain ignored; Kubernetes uses externally
  managed Secret references.
- Keep Actuator `env` and `configprops` endpoints unexposed over HTTP.
- Document the supported configuration catalog and source precedence. All
  configuration changes require process restart; runtime refresh is not
  supported.
- Do not introduce Apollo, Nacos, Spring Cloud Config, Vault, Consul, dynamic
  refresh, a configuration database/API/UI, or another configuration/secret
  infrastructure product under Q-006.

## Alternatives

### Continue with ad hoc placeholders and future `@Value`

Rejected because grouped application settings would lack consistent ownership,
type safety, validation, metadata, and reviewable compatibility.

### Wrap all Spring Boot properties in BrokerOS classes

Rejected because it duplicates mature framework binding, creates two sources
of truth, and increases drift and maintenance risk.

### Create an empty example properties class

Rejected because no current BrokerOS-owned setting needs a production class.
An empty example would be an unnecessary abstraction and likely dumping ground.

### Enable repository-wide configuration-properties scanning immediately

Deferred. Explicit class registration makes ownership visible while there are
few real groups. Scanning can be reconsidered through architecture review when
multiple approved groups demonstrate a need.

### Add Config Server, Vault, Consul, Apollo, Nacos, or dynamic refresh

Rejected for Q-006. No current requirement defines multi-service distribution,
runtime mutation, provider operations, authorization, audit, rollback, partial
failure, or multi-instance consistency.

### Use profiles as the configuration and security model

Rejected. Profiles select configuration but do not establish ownership,
validation, identity, authorization, sensitivity, or secret protection.

## Consequences

- BrokerOS Risk keeps one configuration mechanism and reuses Spring Boot's
  native property binding instead of maintaining wrappers.
- Future application-owned groups have a stable prefix, typed immutable model,
  fail-fast validation, and explicit ownership.
- Q-006 adds no production properties class because no current real
  BrokerOS-owned configuration exists.
- Configuration catalogs and deployment aliases become compatibility artifacts
  that must be reviewed when changed.
- Invalid application-owned configuration will prevent startup rather than fail
  on first use once such a group exists.
- Configuration changes require restart. Runtime administration and dynamic
  refresh remain unavailable until a later approved Requirement and ADR define
  their security, audit, rollout, rollback, and consistency semantics.
- Secret management remains an environment responsibility. Selecting a
  concrete external secret product requires a future decision.
- Tests and Reviews must verify profile loading, required/invalid values,
  override precedence, safe diagnostics, Actuator exposure, and the absence of
  speculative wrappers.
