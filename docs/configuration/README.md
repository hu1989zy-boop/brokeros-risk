# BrokerOS Risk Configuration Guide

## Purpose

This document is the authoritative catalog and operating convention for
BrokerOS Risk configuration. It implements Q-006 and Accepted ADR-008 without
adding a second configuration framework.

Spring Boot Externalized Configuration is the only runtime configuration
mechanism. Configuration is resolved and validated at startup, remains immutable
for the process lifetime, and requires restart when changed. Apollo, Nacos,
Spring Cloud Config, Vault, Consul, and dynamic refresh are not supported.

## Ownership Model

| Owner | Rule |
| --- | --- |
| Spring Boot/framework | Keep the native property namespace and binder. Never create BrokerOS wrappers for datasource/Hikari, Flyway, Redis, Kafka, server, management, logging, or SpringDoc. |
| BrokerOS Risk capability | Use `brokeros.risk.<capability>` only when an approved capability owns real settings. Bind one cohesive immutable `@ConfigurationProperties` type and validate it at startup. |
| Deployment environment | Supplies non-secret aliases through environment variables/ConfigMap and sensitive values through ignored local `.env` or an externally managed Secret. |

Q-009 adds the first approved BrokerOS-owned configuration group:
`brokeros.risk.security.jwt`. It owns only BrokerOS clock-skew tolerance.
Issuer, audience, and JWK settings remain in Spring Security's native namespace.

## Configuration Catalog

`Required` describes the current supported contract. A local/test default does
not make that value an approved production default. `Sensitivity` is
`Public`, `Internal`, or `Secret`; the catalog records no secret value.

| Owner | Canonical Property | Environment Alias | Type | Default | Required | Profile | Sensitivity | Validation | Source | Restart Required | Compatibility |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Spring Boot | `spring.application.name` | None | String | `brokeros-risk-backend` | Yes | all | Public | Non-empty application identity | packaged base YAML | Yes | Stable log/telemetry identity |
| Spring Boot JDBC | `spring.datasource.url` | `DB_URL` | JDBC URL | Local MySQL URL; test uses test schema | Production deployment must override | base/test/prod | Internal | JDBC driver parses/connects at startup | YAML, environment, Compose, ConfigMap | Yes | Alias and URL semantics are deployment contracts |
| Spring Boot JDBC | `spring.datasource.username` | `DB_USERNAME` | String | `brokeros` in base/test; none in prod | Yes in prod | base/test/prod | Internal | Required placeholder in prod | YAML, environment, Compose, ConfigMap | Yes | Alias cannot be renamed silently |
| Spring Boot JDBC | `spring.datasource.password` | `DB_PASSWORD` | String | Empty in base/test; none in prod | Yes in prod | base/test/prod | Secret | Required placeholder in prod; value must not appear in diagnostics | ignored `.env`, environment, Kubernetes Secret | Yes | Secret source may change only by approved deployment decision |
| HikariCP | `spring.datasource.hikari.maximum-pool-size` | `DB_POOL_MAX_SIZE` | Integer | `10` base; `20` prod | No | base/prod | Internal | Integer binding plus Hikari validation | YAML, environment, prod ConfigMap | Yes | Type/default change requires capacity review |
| HikariCP | `spring.datasource.hikari.minimum-idle` | `DB_POOL_MIN_IDLE` | Integer | `1` base; `5` prod | No | base/prod | Internal | Integer binding plus Hikari validation | YAML, environment, prod ConfigMap | Yes | Must remain compatible with maximum pool size |
| HikariCP | `spring.datasource.hikari.connection-timeout` | `DB_CONNECTION_TIMEOUT_MS` | Long milliseconds | `30000` | No | all | Internal | Long binding plus Hikari validation | YAML, environment | Yes | Unit is milliseconds and cannot change silently |
| Flyway | `spring.flyway.enabled` | `FLYWAY_ENABLED` | Boolean | `true` | No | all | Internal | Boolean binding | YAML, environment | Yes | Disabling does not authorize manual schema changes |
| Spring Security Resource Server | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `SECURITY_JWT_ISSUER_URI` | HTTPS/URI | Test uses reserved `.test` issuer; no production default | Yes for web runtime | all | Internal | Nonblank exact trusted issuer; decoder validates `iss` | YAML and external environment | Yes | Q-009/ADR-011 trust contract; provider remains replaceable |
| Spring Security Resource Server | `spring.security.oauth2.resourceserver.jwt.audiences` | `SECURITY_JWT_AUDIENCES` | String list | Test uses `brokeros-risk-test`; no production default | Yes for web runtime | all | Internal | At least one nonblank exact audience | YAML and external environment | Yes | Audience values are deployment trust contracts |
| Spring Security Resource Server | `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `SECURITY_JWT_JWK_SET_URI` | HTTPS/URI | Empty | No when issuer discovery is available | all | Internal | Framework JWK decoder; issuer validation remains mandatory | YAML and external environment | Yes | Explicit URI may replace discovery without changing issuer semantics |
| BrokerOS Q-009 Security | `brokeros.risk.security.jwt.clock-skew` | `SECURITY_JWT_CLOCK_SKEW` | Duration | `60s` | No | all | Internal | Immutable duration from `0s` through `300s` | YAML and external environment | Yes | Q-009-owned trust tolerance; range cannot be weakened silently |
| Spring Data Redis | `spring.data.redis.host` | `REDIS_HOST` | Hostname | `localhost` | No | all | Internal | Host resolved by Redis client | YAML, environment, Compose, ConfigMap | Yes | Alias is a deployment contract |
| Spring Data Redis | `spring.data.redis.port` | `REDIS_PORT` | Integer port | `6379` | No | all | Internal | Integer/port binding | YAML, environment, Compose, ConfigMap | Yes | Alias/type are stable |
| Spring Data Redis | `spring.data.redis.password` | `REDIS_PASSWORD` | String | Empty | Only when the selected Redis requires authentication | all | Secret | Bound by Spring Data Redis; never log value | environment or future approved Secret reference | Yes | Adding a production source requires deployment review |
| Spring Data Redis | `spring.data.redis.timeout` | `REDIS_TIMEOUT` | Duration | `2s` | No | all | Internal | Spring duration binding | YAML, environment | Yes | Unit is explicit |
| Spring Kafka | `spring.kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | Host/port list | `localhost:29092` | No | all | Internal | Spring Kafka address binding/connectivity | YAML, environment, Compose, ConfigMap | Yes | Alias/list semantics are stable |
| Spring Kafka | `spring.kafka.consumer.group-id` | `KAFKA_CONSUMER_GROUP` | String | `brokeros-risk`; test uses `brokeros-risk-test` | No | base/test | Internal | Non-empty when a consumer is introduced | YAML, environment | Yes | No consumer/topic is authorized by this entry |
| Spring Kafka | `spring.kafka.consumer.auto-offset-reset` | None | Enum/string | `earliest` | No | all | Internal | Spring Kafka enum binding | packaged base YAML | Yes | Not permission to create a consumer |
| Spring Kafka | `spring.kafka.producer.acks` | None | Enum/string | `all` | No | all | Internal | Spring Kafka binding | packaged base YAML | Yes | Not permission to create a producer |
| Spring Boot server | `server.port` | `SERVER_PORT` | Integer port | `8080` | No | all | Public | Integer/port binding | YAML, environment, ConfigMap | Yes | API address change requires deployment review |
| Micrometer Tracing | `management.tracing.sampling.probability` | `TRACING_SAMPLING_PROBABILITY` | Decimal 0..1 | `0.1`; test uses `1.0` | No | base/test | Internal | Micrometer/Spring binding | YAML, environment | Yes | ADR-007 governs tracing semantics |
| Spring Boot profiles | `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | Profile list | None | Yes in supported Compose/Kubernetes run modes | deployment | Internal | Must select an existing profile | environment, Compose, test/prod ConfigMap overlays | Yes | Profiles are selection, not a security boundary |
| Docker Compose MySQL | Not an application property | `MYSQL_PASSWORD` | String | None | Yes for Compose | local/test | Secret | Compose required-variable check | ignored `.env` or ephemeral CI environment | Container restart | Local/CI only; never reuse as production evidence |
| Docker Compose MySQL | Not an application property | `MYSQL_ROOT_PASSWORD` | String | None | Yes for Compose | local/test | Secret | Compose required-variable check | ignored `.env` or ephemeral CI environment | Container restart | Local/CI only; never expose value |
| Spring Boot Actuator | `management.endpoints.web.exposure.include` | None | List | `health,info` | Yes | all | Internal | Contract test excludes `env` and `configprops` | packaged base YAML | Yes | Expansion requires Requirement/security review |
| Micrometer Tracing | `management.tracing.propagation.type` | None | Enum/list | `w3c` | Yes | all | Internal | Spring/Micrometer binding | packaged base YAML | Yes | ADR-007; do not add proprietary propagation |
| Spring Boot tracing export | `management.otlp.tracing.export.enabled` | None | Boolean | `false` | Yes | all | Internal | Boolean binding | packaged base YAML | Yes | Exporter requires a future Requirement/ADR |
| Spring Boot tracing export | `management.zipkin.tracing.export.enabled` | None | Boolean | `false` | Yes | all | Internal | Boolean binding | packaged base YAML | Yes | Exporter requires a future Requirement/ADR |
| SpringDoc | `springdoc.api-docs.path` | None | HTTP path | `/v3/api-docs` | Yes | all | Public | Existing endpoint tests | packaged base YAML | Yes | API documentation contract |
| SpringDoc | `springdoc.swagger-ui.path` | None | HTTP path | `/swagger-ui.html` | Yes | all | Public | Existing endpoint tests | packaged base YAML | Yes | UI path compatibility contract |
| SpringDoc | `springdoc.api-docs.enabled` | None | Boolean | `true` base/test; `false` prod | Yes | all | Internal | Production profile disables API docs | packaged base/prod YAML | Yes | Q-009 operational endpoint policy |
| SpringDoc | `springdoc.swagger-ui.enabled` | None | Boolean | `true` base/test; `false` prod | Yes | all | Internal | Production profile disables Swagger UI | packaged base/prod YAML | Yes | Q-009 operational endpoint policy |

## Profile Contract

- Base configuration contains the approved common runtime foundation. Q-009
  requires externally supplied issuer and audience for a web runtime and has no
  permissive security-off mode.
- `test` selects the test database schema, test Kafka consumer-group name,
  non-blocking local datasource initialization, reserved test issuer/audience/
  JWK locations, full trace sampling, and DEBUG application logging.
- `prod` requires externally supplied database username/password, uses fail-fast
  datasource initialization, larger Hikari defaults, disables OpenAPI/Swagger,
  and uses INFO logging.
- Docker Compose's optional backend profile selects Spring profile `test`.
- Kubernetes test/prod overlays select the matching Spring profile.

Profiles do not establish authorization, secret protection, broker identity, or
business policy. A production process started without the supported `prod`
selection is a deployment error; Q-006 does not add a second environment flag.

## Override Priority

Spring Boot property-source precedence remains authoritative. For the supported
run modes, a higher-priority command-line/test property, Java system property,
or OS environment variable can override external or packaged profile/base
configuration. Profile-specific configuration overrides the corresponding base
configuration at the same location.

Environment aliases in the catalog are explicitly referenced by packaged YAML
placeholders. They are compatibility contracts even when the canonical Spring
property name is unchanged. Rename or removal requires a Requirement,
migration/compatibility plan, documentation update, tests, and Review evidence.

See the Spring Boot 3.5 externalized-configuration reference for the complete
ordering:
`https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html`.

## Validation Strategy

### Framework-owned properties

Use Spring Boot and the owning framework's native conversion and validation.
Do not create BrokerOS wrappers merely to attach constraints. Configuration
contract tests verify profile selection, required production placeholders,
invalid typed values, environment-alias overrides, and safe diagnostics.

### Future BrokerOS Risk-owned properties

Only a real approved group may introduce an immutable
`@ConfigurationProperties("brokeros.risk.<capability>")` type. Apply
`@Validated`, Jakarta constraints, nested validation, and semantic Java types.
Fail startup on invalid required values. Binding and validation must be
side-effect free and must not call MySQL, Redis, Kafka, or an adapter.

## Secret Convention

- Never commit, log, return, document, or place a real secret in tests, Review
  files, images, exception messages, or property-object `toString` output.
- `.env` and `.env.*` remain ignored. `.env.example` contains names only.
- Compose local/CI secrets come from ignored developer input or ephemeral CI
  environment values and must not be printed.
- Kubernetes commits only references to an externally managed Secret; it does
  not commit a Secret object or value.
- Actuator web exposure remains limited to `health` and `info`. `env` and
  `configprops` stay unexposed rather than relying solely on sanitization.
- Validation diagnostics may identify a safe property key or constraint but
  must not include a supplied secret value.

Selecting a concrete external secret provider requires a future approved
Requirement and ADR.

## Change Procedure

For every configuration addition or change:

1. Identify the approved Requirement and owning capability.
2. Classify framework-owned, BrokerOS-owned, deployment alias, or secret.
3. Reuse the native framework property when one exists.
4. Define canonical key, alias, type/unit, default, requiredness, profile,
   sensitivity, validation, source, restart, and compatibility.
5. Add or update binding/profile/invalid/missing/secret-safe tests.
6. Update this catalog, applicable architecture/ADR, Skill, Lessons, and Review.
7. Run Maven, static, Kustomize, and applicable infrastructure/CI gates.

Do not add a business setting, empty properties class, wrapper, runtime refresh,
or secret provider without the separate approval it requires.
