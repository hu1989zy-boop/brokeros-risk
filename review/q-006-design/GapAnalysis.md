# Q-006 Gap Analysis

## Already Exists

| Capability | Repository evidence | Assessment |
| --- | --- | --- |
| Spring Boot configuration | `application.yml` and Boot auto-configuration | Reuse; no second framework |
| Profiles | `application-test.yml`, `application-prod.yml`, Compose/Kustomize profile selection | Reuse; document/test semantics |
| Framework typed binding | Datasource/Hikari, Redis, Kafka, Flyway, server, management, logging, SpringDoc namespaces | Reuse native binders; do not wrap |
| Bean Validation | `spring-boot-starter-validation` in `backend/pom.xml` | Reuse for future owned groups |
| Environment aliases | `DB_*`, `REDIS_*`, `KAFKA_*`, `SERVER_PORT`, `TRACING_*` placeholders | Treat as deployment contracts |
| Local secret handling | Ignored `.env`, empty `.env.example`, required Compose passwords | Preserve and document |
| Kubernetes secret reference | `brokeros-risk-secrets/db-password` via `secretKeyRef` | Preserve; Secret value remains external |
| Logging security | AGENTS/Phase 0.6/Q-005 prohibit secret and sensitive logging | Reuse and add config-specific checks |
| Actuator restriction | Web exposure includes only `health,info` | Preserve; no `env`/`configprops` exposure |
| Docker/Kubernetes/CI | Compose, Kustomize overlays, GitHub Actions, repository scripts | Reuse for final verification |
| Clean Java access baseline | No production `@Value`, `@ConfigurationProperties`, `Environment`, or `System.getenv` use | No current scattered-`@Value` defect |

## Need Improvement

| Gap | Evidence | Proposed future improvement |
| --- | --- | --- |
| No central configuration catalog | Supported variables are inferred from YAML, Compose, Kubernetes, and README | Document owner, key, alias, type, unit, default, profile, requiredness, sensitivity, source, validation, restart |
| No application-owned property convention | No current group exists and no durable prefix/ownership rule is documented | Reserve `brokeros.risk.<capability>` and require typed owned groups when real settings appear |
| Validation coverage is implicit | Framework conversion exists, but no focused tests describe missing/invalid/profile behavior | Add safe startup/binding tests after approval |
| Secret convention is distributed | Rules exist across AGENTS, `.gitignore`, README, Compose, Kubernetes, and Q-005 docs | Consolidate source/classification/redaction rules without choosing a secret product |
| Deployment aliases lack compatibility policy | Aliases are referenced by multiple runtime/deployment files | Treat aliases as contracts; require documented migration for rename/removal |
| Profile/precedence behavior is undocumented centrally | Base/test/prod overlays exist, but supported override order is not cataloged | Document Spring Boot precedence and test the repository-supported paths |
| Restart semantics are implicit | No dynamic refresh exists | State startup-bound immutable semantics and defer refresh |
| No configuration-specific review checklist | General standards apply but do not enumerate keys/defaults/sensitivity | Add future skill and final Review evidence |

## Clarification of Prompt Examples

### Scattered `@Value`

Not currently present. Repository search returned zero production usages. It is
a future regression risk, not an existing defect.

### Typed configuration not unified

Framework configuration is already typed internally by Spring Boot. What is
missing is a convention for future BrokerOS-owned groups. Creating wrappers for
current framework properties would make the design worse.

### Validation missing

Jakarta Bean Validation is present, and Spring Boot performs native type
conversion. There is no owned property class to validate today. Missing work is
the requiredness/range/profile test matrix and the rule for future owned groups.

### Secret convention missing

Secret handling is partially established, not absent: `.env` is ignored,
examples are empty, production DB password has no YAML default, Kubernetes uses
`secretKeyRef`, and secret logging is prohibited. The gap is consolidation,
classification, broader future credential coverage, and safe diagnostic tests.

### Documentation missing

READMEs point to `application.yml`, but there is no authoritative configuration
catalog or compatibility policy. This is the clearest current gap.

## Out of Scope

- Any business setting, broker policy, feature flag, dynamic rule, threshold,
  limit, workflow, or administrative configuration.
- Configuration persistence, API/UI, runtime refresh, approval/audit workflow,
  Config Server, Vault, Consul, operators, or external secret managers.
- Empty/example production properties classes or duplicate Spring property
  wrappers.
- Business code, API, Flyway, database, Redis, Kafka, adapters, observability
  infrastructure, Docker/Kubernetes topology, CI provider, packages, services,
  or repositories.
