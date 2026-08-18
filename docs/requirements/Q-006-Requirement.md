# Q-006: Configuration Management Foundation

## Status

Approved

- Requirement ID: `Q-006`
- Architecture phase: Phase 1
- Change type: engineering foundation only
- Design date: 2026-08-18
- Approved: 2026-08-18
- Phase 2 implementation authorization: GRANTED on 2026-08-18

The Design Only baseline and Architecture Review were approved before Phase 2.
Implementation remains limited to this Requirement and Accepted ADR-008. No
business code, speculative properties class, runtime configuration service,
dependency, API, database, messaging, cache, CI, or deployment-topology change
is authorized.

## 1. Background

Q-005 completed the inbound observability baseline on the existing Java 21 and
Spring Boot modular monolith. The repository already uses Spring Boot
externalized configuration through `application.yml`, `application-test.yml`,
`application-prod.yml`, environment-variable placeholders, Docker Compose, and
Kubernetes ConfigMap/Secret references. Jakarta Bean Validation and the Spring
Boot test foundation are already present.

The current runtime properties are almost entirely framework-owned properties
under `spring.*`, `server.*`, `management.*`, `logging.*`, and `springdoc.*`.
The Java source currently contains no `@Value` and no application-owned
`@ConfigurationProperties` type. This is a useful clean baseline, but the
repository has no durable rule for how future BrokerOS Risk-owned configuration
groups will be named, bound, validated, documented, tested, and supplied
without exposing secrets.

Q-006 establishes that foundation before business configuration appears. It
does not invent broker policy, business settings, or an empty production
properties class merely to demonstrate Spring Boot binding.

## 2. Problem Statement

Configuration currently works for the engineering foundation, but its contract
is distributed across YAML, Compose, Kubernetes manifests, `.env.example`, and
README guidance. There is no single catalog that states each supported setting,
its canonical property name, deployment alias, owner, type, default, profile,
requiredness, sensitivity, source, validation, or restart behavior.

Spring Boot already performs typed binding for its own configuration. Future
application features, however, could introduce scattered `@Value` fields,
duplicate wrappers around framework properties, unvalidated strings, committed
secrets, conflicting environment aliases, hidden defaults, or runtime-refresh
assumptions. These patterns would make broker-specific policy difficult to
review and could allow invalid or sensitive configuration into production.

Q-006 must define a small, explicit configuration contract that reuses Spring
Boot rather than creating a second configuration framework.

## 3. Scope

### 3.1 Configuration ownership and naming

- Classify configuration as framework-owned, BrokerOS Risk-owned, deployment
  alias, or secret.
- Continue using Spring Boot's native property namespaces and built-in typed
  binders for framework-owned settings. Do not mirror datasource, Hikari,
  Redis, Kafka, Flyway, server, management, logging, or SpringDoc settings in
  custom wrapper classes.
- Reserve canonical `brokeros.risk.<capability>` prefixes, written in lower-case
  kebab form, for real BrokerOS Risk-owned configuration groups.
- Use `@ConfigurationProperties` for grouped application-owned settings. Do not
  introduce grouped application settings through scattered `@Value` fields.
- Do not add an empty, sample-only, or speculative production properties type.
  A production type requires at least one concrete setting owned and consumed
  by an approved capability.

### 3.2 Typed binding and validation

- Define the registration, immutability, prefix, nested-object, default-value,
  and ownership conventions for application-owned properties.
- Use Jakarta Bean Validation and `@Validated` for application-owned property
  constraints when such a group exists.
- Fail application startup for missing or invalid required configuration rather
  than deferring failure until first use.
- Keep validation deterministic and side-effect free. Configuration binding or
  validation must not call MySQL, Redis, Kafka, an adapter, or another external
  system.
- Avoid bespoke validators when standard type conversion and Jakarta
  constraints are sufficient.

### 3.3 Profiles and external sources

- Preserve the existing base, `test`, and `prod` profile structure unless
  implementation analysis identifies an approved defect.
- Treat profiles as configuration selection, not as a security boundary or a
  secret store.
- Document Spring Boot property precedence relevant to the repository and the
  mapping between canonical keys and deployment environment variables.
- Treat Q-006 configuration as startup-bound and immutable for the process
  lifetime. Dynamic refresh is not authorized.

### 3.4 Secret convention

- Classify sensitive settings explicitly.
- Keep production secret values outside tracked YAML, documentation, tests,
  images, and Git history.
- Retain ignored local `.env` files for Compose-only developer secrets and
  tracked empty examples without values.
- Retain environment-managed Kubernetes Secret references; Q-006 does not
  create or commit a Secret object or select a new secret-management product.
- Keep Actuator `env` and `configprops` endpoints unexposed over HTTP in this
  phase and never print secret values in validation errors or Review evidence.

### 3.5 Documentation and verification

- Create a central configuration reference containing owner, canonical key,
  environment alias, type, default, profile, requiredness, sensitivity,
  validation, source, and restart behavior.
- Add focused automated verification after approval for profile layering,
  override behavior, required production values, invalid value rejection, and
  secret-safe diagnostics.
- Reuse the current Maven, Spring Boot test, static, CI, Docker, Kubernetes, and
  Review foundations. Change only the artifacts proven necessary by the final
  approved implementation plan.

## 4. Non Goals

Q-006 does not authorize:

- Risk Case, Rule Engine, Workflow, Account Control, Audit Module, RBAC, or any
  other business capability;
- broker-specific policies, thresholds, limits, trading rules, account rules,
  or feature flags;
- a configuration database, configuration API/UI, runtime administration,
  approval workflow, versioned business-policy model, or audit implementation;
- Spring Cloud Config, Vault, Consul, ZooKeeper, Kubernetes Operator, dynamic
  refresh, hot reload, or a third-party configuration service;
- a new dependency, starter, code generator, Maven plugin, annotation processor,
  or configuration abstraction without separate approval;
- wrapping Spring Boot's datasource, Hikari, Redis, Kafka, Flyway, server,
  management, logging, or SpringDoc properties in BrokerOS-specific classes;
- changing current property values, defaults, profile semantics, environment
  aliases, ports, connection settings, log levels, tracing behavior, or API
  contracts merely to normalize naming;
- exposing Actuator `env` or `configprops` endpoints;
- Java package restructuring, DDD restructuring, microservices, another
  deployable, or repository splitting;
- a Flyway migration, database table, Redis key, Kafka topic/event, adapter,
  Docker topology change, Kubernetes topology change, or CI provider change;
- Flink, Python, Elasticsearch, OpenSearch, ELK, Prometheus, Grafana, Jaeger,
  Zipkin, or an OTLP Collector.

## 5. Acceptance Criteria

### Design approval gate

1. This Requirement, Q-006 architecture design, Gap Analysis, implementation
   plan, ADR determination, and Design Review Package are complete and marked
   `Design Only — Architect Approval Required`.
2. The design is based on repository evidence and does not claim that `@Value`
   or custom `@ConfigurationProperties` already exists.
3. No Java source, runtime YAML, test, dependency, CI, Docker, Kubernetes,
   Flyway, Redis, or Kafka implementation changes are present in the design
   change set.
4. The Q-005 root Review Package and pre-existing Q-004 working-tree changes
   remain preserved; the protected `review/review-history/` archive is not read,
   modified, staged, or committed.
5. Architect approval explicitly resolves the proposed ADR-008 requirement and
   confirms whether Q-006 implementation may remain policy/tests/documentation
   only until a real application-owned property group exists.

### Future implementation gate after approval

6. Spring Boot externalized configuration remains the only runtime
   configuration mechanism; no parallel registry or custom framework is added.
7. Framework-owned properties continue using native Spring Boot namespaces and
   binders without duplicate BrokerOS wrappers.
8. Any real BrokerOS Risk-owned grouped settings use canonical
   `brokeros.risk.<capability>` keys and typed `@ConfigurationProperties` with
   documented ownership. No empty or speculative production group is added.
9. Required or bounded application-owned values use startup-time Jakarta Bean
   Validation; automated tests prove valid binding and safe startup failure for
   missing/invalid values.
10. The configuration catalog documents every supported deployment variable
    currently referenced by base/test/prod YAML, Compose, and Kubernetes,
    including its sensitivity and source.
11. Production secrets have no committed value or unsafe default, local secret
    files remain ignored, Kubernetes continues to reference an externally
    managed Secret, and diagnostics do not reveal values.
12. Profiles and override precedence are documented and tested without treating
    a profile as authorization or secret protection.
13. Actuator `env` and `configprops` remain unexposed, and no API endpoint or
    response contract is added or changed.
14. No business module, schema, topic, event, key, external integration,
    package restructure, deployment split, or prohibited technology is added.
15. `mvn test`, `mvn package`, `git diff --check`, static verification,
    Kustomize rendering, and the existing isolated infrastructure gate pass for
    the final implementation, with unavailable local checks verified by the
    approved CI path before final PASS.
16. The approved ADR (if required), configuration skill, honest Lessons Learned,
    central configuration documentation, and final root Review Package are
    complete before Q-006 is closed.

## 6. Technical Constraints

- Preserve Java 21, Spring Boot 3.x, Maven, MySQL, Redis, Kafka, Docker,
  Kubernetes, Flyway, SpringDoc, Actuator, Micrometer Tracing, and Logback.
- Preserve one repository and one feature-first Spring Boot modular-monolith
  deployable.
- Use the existing `spring-boot-starter-validation`; Q-006 is expected to need
  no new runtime dependency.
- Use canonical lower-case kebab property names. Environment aliases must be
  upper-case and documented; aliases are deployment contracts and must not be
  renamed silently.
- Treat bound configuration contracts as externally supplied public contracts.
  Default, requiredness, type, unit, range, and name changes require
  compatibility review.
- Use explicit types such as `Duration`, integer/long ranges, URI/host types, or
  enums where they express real semantics. Do not represent every value as a
  `String`.
- Do not place credentials or sensitive payloads in property-class `toString`,
  logs, exceptions, Actuator exposure, test output, or Review evidence.
- Do not use application configuration as authentication, authorization, audit
  identity, idempotency, or business state.
- No runtime refresh, fallback to remote systems, hidden production default, or
  environment-dependent Java branch is permitted without an approved design.
- Do not create a properties type that merely duplicates `DataSourceProperties`,
  Hikari, Redis, Kafka, Flyway, server, management, logging, or SpringDoc.

## 7. Deliverables

### Current Design Only deliverables

- `docs/requirements/Q-006-Requirement.md`.
- `docs/architecture/q-006-configuration-management-foundation-design.md`.
- A dedicated Q-006 Design Review Package under `review/q-006-design/` with
  Summary, Architecture Review, Requirement Review, Gap Analysis,
  Implementation Plan, ADR Decision, Outstanding Items, Verification, Git
  status, bounded diff statistics, bounded project tree, and Initial Baseline
  Check.

### Future deliverables after approval

- The smallest approved typed-configuration/validation implementation, if a
  concrete application-owned property group is authorized.
- Focused configuration integration tests without external service calls.
- Central configuration and secret-convention documentation.
- ADR-008 if the architect accepts the ADR determination.
- A reusable configuration-management skill and an honest Q-006 Lessons
  Learned entry based on actual implementation.
- The final mandatory root Review Package.

## 8. Verification Plan

### Design-stage verification

```bash
git status --short --branch
rg -n '@Value|@ConfigurationProperties|ConfigurationPropertiesScan' backend/src
git diff --check
sh scripts/verify-static.sh
cd backend && mvn test
cd backend && mvn package
```

- Compare final status with the captured initial baseline.
- Confirm all Q-006 additions are Markdown/text design artifacts only.
- Confirm no tracked source, runtime YAML, test, dependency, CI, Docker,
  Kubernetes, Flyway, Redis, or Kafka implementation file changed for Q-006.
- Record Docker/Kubernetes runtime checks as not required for a documentation-
  only design delta; do not reuse an old run as evidence of new implementation.

### Future implementation verification

- Test valid binding, defaults, environment overrides, profile layering,
  missing required values, invalid type/range values, and nested validation.
- Assert startup failures name safe property keys/constraints but never values
  of secrets.
- Assert no application-owned grouped setting is consumed through scattered
  `@Value` and no framework-native properties are duplicated.
- Assert Actuator `env` and `configprops` remain unexposed.
- Run the full Maven, static, Kustomize, Compose/infrastructure, and CI gates.

## 9. Risks

### Inventing configuration to justify a foundation

There is currently no application-owned property group. Adding an empty or
sample production class would violate the repository rule against unnecessary
abstractions. The approved design must allow Q-006 to establish conventions,
tests, and documentation without inventing a setting.

### Duplicating framework configuration

Wrapping Spring Boot datasource, Redis, Kafka, Flyway, management, logging, or
server properties creates two sources of truth and inconsistent validation.
Framework-owned properties must remain framework-owned.

### Breaking deployment aliases

`DB_URL`, `DB_USERNAME`, `REDIS_HOST`, and similar names are already used by
YAML, Compose, and Kubernetes. Renaming them as a cleanup can break deployment
even when the canonical Spring property is unchanged.

### Unsafe defaults or late failure

Convenient local defaults can become unsafe in production, while missing
validation can postpone failure until traffic arrives. Requiredness must be
profile-aware and verified without embedding a production secret.

### Secret leakage

Configuration catalogs, validation exceptions, test diagnostics, Actuator, and
Review evidence can reveal sensitive values. Documentation and tests must use
names/classification only and redact values.

### Profile and precedence ambiguity

Spring Boot has ordered property sources and profile overlays. Undocumented
override behavior can make local, CI, and production results differ. Q-006 must
document the supported sources without adding another precedence layer.

### Premature dynamic configuration

Runtime refresh introduces consistency, concurrency, audit, rollback, and
partial-application concerns. It is excluded until a concrete Requirement
defines those semantics.

## 10. Review Checklist

- [ ] Architect approved this Requirement and the Q-006 architecture boundary.
- [ ] ADR-008 requirement and proposed configuration strategy were explicitly
      accepted or rejected with rationale before implementation.
- [ ] Current repository inventory was checked; no false claim of existing
      scattered `@Value` or custom property classes remains.
- [ ] Framework-owned and BrokerOS Risk-owned configuration remain distinct.
- [ ] No empty/sample production property group or duplicate framework wrapper
      is introduced.
- [ ] Property names, types, defaults, units, requiredness, profiles, sources,
      sensitivity, validation, and restart behavior are documented.
- [ ] Secret values remain outside Git, logs, API responses, Actuator exposure,
      tests, images, and Review evidence.
- [ ] Existing environment aliases are treated as compatibility contracts.
- [ ] Valid, missing, invalid, profile, override, and secret-safe diagnostic
      cases are covered after implementation.
- [ ] API, database/Flyway, Redis, Kafka, adapter, auditability, observability,
      Docker, Kubernetes, and CI impacts are explicitly reviewed.
- [ ] No business policy, table, topic, key, external integration, package
      restructure, new deployable, or prohibited technology is introduced.
- [ ] Skill and Lessons Learned are created only after actual implementation.
- [ ] All verification results are honest; unavailable or inapplicable checks
      are not marked PASS.
- [ ] Final Review Package contains evidence for all eight mandatory standards
      compliance areas before Q-006 can be marked PASS.
