# Q-006 Design Architecture Review

## Review Result

DESIGN APPROVED

The architect approved this design on 2026-08-18. It fits the Phase 1 modular
monolith and has no identified standards violation. Final implementation status
is recorded in the root Q-006 Review Package rather than this design snapshot.

## Existing Foundation That Can Be Reused

### Already Exists

- Spring Boot 3.5 externalized configuration and native typed binders.
- Base, `test`, and `prod` YAML files.
- Explicit environment placeholders for datasource, Hikari, Flyway, Redis,
  Kafka, server, and tracing settings.
- Jakarta Bean Validation dependency.
- Spring Boot test and Maven foundations.
- Spring Boot Logback and Q-005 safe logging/correlation rules.
- Docker Compose `.env` convention with ignored populated files.
- Kubernetes ConfigMap plus external Secret reference.
- Blocking GitHub Actions, static verification, Kustomize rendering, and
  isolated infrastructure verification.
- Actuator web exposure limited to `health` and `info`.

### Need Improvement

- Configuration ownership and canonical naming rules for future application-
  owned settings.
- A central property catalog with type/default/profile/source/sensitivity and
  compatibility information.
- Explicit typed-binding and Bean Validation conventions for real BrokerOS
  configuration groups.
- Focused tests for profile layering, overrides, required production values,
  invalid value rejection, and secret-safe diagnostics.
- A documented secret-source convention spanning local, CI, and Kubernetes.
- An explicit startup-bound/no-dynamic-refresh decision.
- A compatibility rule for existing deployment aliases.

### Out of Scope

- Business configuration, broker policies, thresholds, rules, feature flags,
  or runtime administration.
- A configuration database/API/UI, dynamic refresh, Config Server, Vault,
  Consul, operators, or another secret/configuration product.
- Rewrapping Spring Boot properties, adding an empty property type, or adding a
  new dependency without approval.
- API, database, Redis, Kafka, adapter, Docker topology, Kubernetes topology,
  CI provider, package, deployable, or repository changes.

## Architecture Rationale

The existing runtime already delegates configuration resolution and framework
binding to Spring Boot. Reusing that mechanism avoids duplicate sources of
truth. A future BrokerOS-owned group needs a clear prefix, owner, typed model,
and validation because those properties become externally supplied contracts.

There is no current application-owned group and no `@Value` usage. Creating a
sample production group would not close a real runtime gap and would violate
the YAGNI/avoid-unnecessary-abstraction rules. Q-006 should first establish the
contract, documentation, and verification; a production type appears only with
a concrete approved setting.

Configuration is proposed as startup-bound. Dynamic refresh would require
atomicity, rollout, authorization, auditing, rollback, failure, and multi-
instance consistency semantics that are not present in Q-006.

## Architecture Impact

| Area | Design result |
| --- | --- |
| Modular monolith | Preserved; one repository and one backend deployable |
| Package structure | No design-stage change; no horizontal or business package |
| API | No endpoint/body/header/ResultCode/exception change |
| Database/Flyway | No migration, DDL, DML, table, or external DB access |
| Redis | No key, TTL, cache, or connectivity change |
| Kafka | No topic, event, producer, consumer, or broker change |
| Logging/tracing | Existing Logback and Q-005 correlation/security retained |
| Docker/Kubernetes | Existing configuration sources inventoried; no manifest change |
| CI | Existing verification reused; no workflow change |
| External adapters | No MT4, MT5, CRM, BrokerPilot, oneZero, or SDK work |
| Risk business | No Risk Case, Rule Engine, Workflow, Account Control, Audit, or RBAC |

## ADR Evaluation

ADR required: YES.

The proposal establishes a durable cross-module and deployment strategy:
Spring Boot remains the only configuration mechanism, framework properties are
not wrapped, BrokerOS-owned settings use typed validated groups under a reserved
prefix, configuration is startup-bound, and secrets remain externally supplied.
Because this affects every future module and environment contract, it meets the
ADR threshold even without a new dependency.

ADR-008 was accepted before Phase 2 implementation and records the approved
configuration strategy.

## Development Standards Compliance

### AGENTS.md compliance

Evidence checked: root `AGENTS.md`, Q-001 through Q-005 context, Phase 0.5/0.6
and Q-004/Q-005 architecture, ADR-001 through ADR-007, current skills, recent
Lessons Learned, and the working tree. Q-006 adds only an identified Requirement,
architecture design, and dedicated Review package. It introduces no business
module, technology, implementation, or vendor coupling. Architect approval is
an explicit gate.

### Architecture compliance

The proposed design preserves Java/Spring Boot, the feature-first modular
monolith, one backend deployable, current profiles, and existing deployment
layout. It adds no microservice, repository split, adapter, Flink, Python,
Elasticsearch, or speculative horizontal layer. Framework-native configuration
is reused rather than duplicated.

### ADR compliance

ADR-001 modular-monolith/stack, ADR-002 isolation/configurable broker policy,
ADR-003 foundation standards, ADR-004 local/deployment layout, ADR-005 durable
standards, ADR-006 CI gates, and ADR-007 observability remain unchanged. The
design identifies a new cross-cutting decision and blocks implementation until
ADR-008 is accepted; it does not silently amend any accepted ADR.

### API standard compliance

Inspected `backend/src/main/java/com/brokeros/risk/api`, health API conventions,
and the Q-006 file scope. No controller, endpoint, `ApiResponse`,
`ErrorResponse`, `ResultCode`, DTO, Bean Validation REST boundary, exception
mapping, OpenAPI contract, or Actuator native format changes in Design Only.

### Database standard compliance

Inspected `backend/src/main/resources/db/migration`; unchanged
`V1__initial_schema.sql` remains the only migration. Q-006 creates no SQL,
entity, repository, table, column, index, DDL/DML, Hibernate schema setting, or
external database write. Typed configuration must not be used to bypass Flyway.

### Security standard compliance

Inspected `.gitignore`, `.env.example`, application profiles, Compose,
Kubernetes `secretKeyRef`, Actuator exposure, and Q-005 logging guidance. The
design prohibits committed/logged secret values and keeps `env`/`configprops`
unexposed. New documents name keys and sources only; they contain no credential
value. A later implementation must test safe failure diagnostics.

### Auditability compliance

Q-006 has no critical business action, state transition, policy change UI, or
Audit module. Startup configuration must not become hidden mutable business
state. Future dynamic or administrable configuration remains excluded because
it would require actor, reason, before/after, approval, timestamp, and rollback
semantics.

### Skill compliance

`docs/skills/development-standards.md`,
`phase-0.5-engineering-foundation.md`, `ci-integration-verification.md`, and
`observability-correlation.md` were inspected. Per the user instruction, no
skill is created during design. `SkillAnalysis.md` defines the proposed future
configuration-management skill and blocks its creation until implementation
produces verified reusable knowledge.

## Technical Debt and Recommendations

- Resolved: ADR-008 was accepted before implementation.
- Resolved: the architect confirmed that no production
  `@ConfigurationProperties` type should be created until a concrete
  application-owned setting exists.
- Existing non-blocking debt: configuration documentation is distributed and
  profile/alias/requiredness behavior lacks focused tests.
- Existing operational risk: production behavior depends on correct `prod`
  profile activation; Q-006 should document and test the supported path without
  inventing an authorization or environment-detection mechanism.

Recommendation: approve the narrow design, then implement documentation and
verification first. Add production configuration code only when the approved
Q-006 scope identifies a real owned property group.
