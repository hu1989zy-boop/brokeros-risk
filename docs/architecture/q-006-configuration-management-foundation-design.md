# Q-006 Configuration Management Foundation — Design

## Status

Approved

The architect approved this design on 2026-08-18. Accepted ADR-008 records the
durable configuration strategy and governs Phase 2 implementation.

## Purpose

Define how BrokerOS Risk will own, bind, validate, document, supply, and test
configuration while preserving the existing Spring Boot modular monolith and
deployment model.

## Current Architecture

The repository already uses Spring Boot externalized configuration:

- `application.yml` provides base defaults and environment placeholders;
- `application-test.yml` and `application-prod.yml` provide profile overlays;
- Docker Compose maps ignored local `.env` values and service addresses;
- Kubernetes uses a ConfigMap for non-secret settings and `secretKeyRef` for
  the database password;
- GitHub Actions and repository scripts verify Maven, Compose, infrastructure,
  and Kustomize behavior;
- Jakarta Bean Validation is already on the classpath;
- Actuator exposes only `health` and `info`.

Repository search found no `@Value`, `@ConfigurationProperties`,
`@ConfigurationPropertiesScan`, direct `Environment` access, or
`System.getenv`/`System.getProperty` use in production Java. Current properties
are framework-owned and consumed by Spring Boot auto-configuration.

## Proposed Configuration Model

### Ownership categories

| Category | Examples | Owner | Binding rule |
| --- | --- | --- | --- |
| Framework-owned | `spring.datasource.*`, `spring.data.redis.*`, `spring.kafka.*`, `server.*`, `management.*`, `logging.*`, `springdoc.*` | Spring Boot/library | Keep native namespace and binder; do not wrap |
| BrokerOS Risk-owned | A future approved platform or feature setting | Owning BrokerOS capability | Canonical `brokeros.risk.<capability>` plus typed `@ConfigurationProperties` |
| Deployment alias | `DB_URL`, `REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS` | Repository deployment contract | Map explicitly to a canonical key and document compatibility |
| Secret | Database/Redis credentials and future approved credentials | Deployment environment | External value only; never commit or log |

This model avoids two configuration systems. Spring Boot continues to resolve
property-source precedence and bind framework configuration. BrokerOS code owns
only its own real settings.

### Type-safe application properties

When an approved BrokerOS Risk capability introduces a group of settings:

- use a lower-case kebab `brokeros.risk.<capability>` prefix;
- use one cohesive `@ConfigurationProperties` type owned by that capability;
- prefer immutable constructor/record binding when compatible with the required
  validation and framework lifecycle;
- use semantic types (`Duration`, bounded numeric values, URI/host types, stable
  enums) rather than strings where semantics are known;
- apply `@Validated` and Jakarta constraints to required and bounded values;
- cascade validation into nested structures explicitly;
- inject the typed object into its consumer; do not read the `Environment`
  repeatedly or spread `@Value` fields across components;
- do not inject services or perform I/O from a properties type or validator.

There is no current application-owned group. Q-006 must not create a production
type solely as an example. This is an explicit YAGNI boundary, not a failure to
adopt typed configuration.

### Registration

The implementation must choose one explicit Spring Boot registration mechanism
for real properties types. Class-by-class registration is initially preferred
because the application is small and it makes ownership visible. A repository-
wide scan should be introduced only when multiple real groups justify it.
Properties types are not general components and should contain no injected
application collaborators.

### Startup and refresh semantics

Configuration is resolved and validated at startup, then treated as immutable
for the process lifetime. Invalid required values fail startup. Dynamic refresh,
partial rollout, a configuration database, and runtime administration are out
of scope because they require separate consistency, authorization, audit,
rollback, and operational designs.

## Profiles and Property Sources

The current base/test/prod layout remains. Base files contain common framework
configuration and safe local defaults where already approved. Test overlays
optimize local/CI behavior. Production overlays require externally supplied
credentials and conservative log levels.

Profiles select configuration; they do not prove identity, permissions, or
secret safety. Environment variables and command-line arguments can override
packaged YAML according to Spring Boot precedence. Q-006 will document the
supported repository sources rather than add a custom precedence layer.

Environment aliases are compatibility contracts because Compose, Kubernetes,
operators, and CI may depend on them. A rename needs an explicit migration and
compatibility review, not a mechanical cleanup.

## Validation Model

Validation has two layers:

1. Spring Boot's native binders continue validating conversion and native
   semantics for framework-owned properties.
2. BrokerOS Risk-owned property groups use typed binding plus Jakarta Bean
   Validation for requiredness, range, format, and nested invariants.

Cross-field validation is added only for a real invariant that cannot be
expressed with standard constraints. Validation errors may identify the safe
property key and constraint but must not include a secret value.

## Secret Model

- Tracked YAML contains no production secret value.
- `.env` and `.env.*` remain ignored; `.env.example` stays value-free.
- Compose supplies local-only passwords from the developer environment.
- Kubernetes references an externally managed Secret and commits no Secret
  object or secret value.
- Images, CI configuration, test fixtures, logs, exceptions, Actuator, Review
  files, and configuration catalogs contain no credential value.
- Actuator `env` and `configprops` remain outside web exposure. Q-006 does not
  rely on endpoint sanitization as a reason to expose them.

Selecting Vault, Spring Cloud Config, external secret operators, mounted config
trees, or another secret service requires a later Requirement and ADR. Q-006
defines conventions without selecting such infrastructure.

## Documentation Model

After approval, a central configuration catalog should record for every
supported setting:

- owner and purpose;
- canonical property key;
- environment/deployment alias;
- type and unit;
- default value, if safe;
- required/optional status by profile;
- sensitivity classification;
- validation constraints;
- supported source (YAML, environment, Compose, ConfigMap, Secret);
- restart requirement;
- deprecation or compatibility notes.

The catalog records names and rules, not secret values.

## Implemented Verification

`ConfigurationContractIntegrationTests` verifies:

- base/test/prod property layering;
- environment or test-property override behavior;
- valid type conversion and documented defaults;
- missing required production values fail safely;
- invalid type/range/duration values fail startup;
- safe diagnostics that do not contain supplied secret values;
- Actuator `env` and `configprops` remain unexposed;
- the configuration catalog contains every deployment alias extracted from
  YAML, Compose, and Kubernetes sources.

The tests use Spring Boot utilities already available through
`spring-boot-starter-test`. Tests for binding and validation should not require
MySQL, Redis, Kafka, Docker, or Kubernetes unless the final approved change
alters those deployment contracts.

## Implementation Outcome

Q-006 added Accepted ADR-008, `docs/configuration/README.md`, seven focused
configuration contract tests, a reusable configuration-management skill, and an
honest Lessons Learned entry. No real BrokerOS-owned configuration exists, so
the approved YAGNI decision produced no production `@ConfigurationProperties`
type and no runtime YAML or dependency change.

## Impact Analysis

| Area | Design impact |
| --- | --- |
| Modular monolith | No change; configuration remains inside the one backend deployable |
| Java packages | No design-stage change; future properties belong to their owning capability, not a generic dumping ground |
| API | No endpoint, body, ResultCode, validation DTO, or exception contract change |
| Database/Flyway | No migration, schema, DDL, DML, or external database access |
| Redis | No key, TTL, value, client behavior, or source-of-truth change |
| Kafka | No topic, event, producer, consumer, or broker configuration change |
| Docker | Current `.env` and Compose behavior is inventoried only |
| Kubernetes | Current ConfigMap/Secret reference and overlays are inventoried only |
| CI | Existing gates are reused; no design-stage workflow change |
| Logging/tracing | Sensitive-value rules preserved; Q-005 correlation unchanged |
| External adapters | No MT4, MT5, CRM, BrokerPilot, oneZero, or other integration change |
| Auditability | No critical action or Audit module; configuration strategy must not become hidden business state |

## Alternatives

### Continue with ad hoc placeholders and future `@Value`

Rejected as a durable approach because it provides weak grouping, metadata,
validation, and ownership for application-owned settings.

### Wrap all Spring Boot properties in BrokerOS classes

Rejected because it duplicates mature framework binders, creates two sources of
truth, and adds maintenance without product value.

### Create an empty example properties class now

Rejected because no current application-owned setting needs it. A sample
production type would be an unnecessary abstraction and could become a dumping
ground.

### Add a configuration server or Vault now

Rejected for Q-006 because no multi-service distribution, dynamic refresh, or
secret-provider Requirement exists. It would add infrastructure and operational
decisions outside Phase 1 foundation scope.

### Use profiles as the only convention

Rejected because profiles do not define ownership, typing, validation,
sensitivity, aliases, or compatibility.

## ADR Decision

ADR required: YES — satisfied by Accepted ADR-008.

The proposed decision is durable and cross-cutting: Spring Boot externalized
configuration remains the only mechanism; framework-native properties are not
wrapped; BrokerOS-owned groups use a reserved prefix and typed validated
binding; configuration is startup-bound; and secrets remain externally
supplied without selecting a secret product. These rules affect every future
module and deployment contract, so they meet the ADR threshold even though no
new dependency is proposed.

ADR-008 was accepted before Java or runtime configuration implementation. It
confirms Spring Boot externalized configuration, native framework property
ownership, typed validated application groups only when real settings exist,
startup-bound immutable semantics, deployment-alias compatibility, and external
secret boundaries.

## References

- `AGENTS.md`
- `docs/architecture/phase-0.6-development-standards.md`
- ADR-001 through ADR-007
- `docs/skills/development-standards.md`
- Spring Boot 3.5 Externalized Configuration:
  `https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html`
