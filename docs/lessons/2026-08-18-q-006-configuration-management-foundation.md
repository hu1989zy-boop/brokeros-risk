# Q-006 Configuration Management Foundation Lessons Learned

## What Was Implemented

Q-006 accepted ADR-008, created one authoritative configuration catalog,
documented ownership/validation/profile/secret/compatibility rules, and added
seven configuration contract integration tests. The tests exercise actual
base/test/prod YAML, missing production placeholders, invalid typed values,
environment-alias priority, safe diagnostics, Actuator exposure, and catalog
coverage of deployment aliases.

No production `@ConfigurationProperties` type, `@Value`, configuration wrapper,
dependency, runtime YAML change, API, database migration, Redis business data,
Kafka topic/event, CI change, or deployment-topology change was added.

## Why This Design

The current settings are owned by Spring Boot and other framework components.
Wrapping them would create a duplicate configuration model without adding
product value. The real gap was a durable ownership convention, centralized
catalog, deterministic verification, and secret-safe operating guidance.

ADR-008 makes no production properties class the intentional YAGNI result until
an approved BrokerOS-owned setting actually exists.

## Alternatives Considered

- Framework wrapper classes were rejected because native binders already own
  datasource/Hikari, Redis, Kafka, Flyway, server, management, logging, and
  SpringDoc configuration.
- An empty `BrokerProperties` example was rejected because it would be
  speculative and likely become a dumping ground.
- A fake test-only `@ConfigurationProperties` group was rejected because it
  would test an invented contract rather than the repository's real
  configuration.
- Apollo, Nacos, Config Server, Vault, Consul, and dynamic refresh were rejected
  because Q-006 defines no runtime-mutation, provider, authorization, audit,
  rollout, rollback, or consistency requirement.

## Problems Encountered

The focused configuration tests passed on their first execution, but review of
the missing-production-property test exposed a determinism risk: a developer or
CI host could already define `DB_PASSWORD`, making the test dependent on the
shell. The runner was tightened to remove system environment and system property
sources before resolving packaged configuration. The same seven tests then
passed again.

The configuration catalog spans YAML, Compose, and Kubernetes. Maintaining a
second handwritten alias list in tests would drift, so the test extracts
placeholders and environment names from the actual repository files and asserts
that the catalog documents each one.

## Lessons Learned

- Configuration ownership must be decided before choosing an annotation.
- Native framework binding is already typed configuration; application wrappers
  are not automatically an improvement.
- A foundation can be complete without a production class when there is no real
  application-owned property.
- Missing-property tests must isolate host property sources to be repeatable.
- Documentation is a verifiable contract when tests derive expectations from
  the actual deployment sources.
- Runtime-generated synthetic sensitive values test diagnostic redaction
  without committing credential-like literals.
- Profiles select configuration but do not prove that a process is authorized
  or safe for production.

## Reusable Patterns

- Ownership decision: framework native → application owned → deployment alias
  → secret.
- Catalog each key with owner/type/unit/default/required/profile/sensitivity/
  validation/source/restart/compatibility.
- Use `ApplicationContextRunner` with real Config Data and no external service
  startup for configuration contract tests.
- Remove host property sources for deterministic missing/invalid scenarios.
- Extract aliases from source artifacts rather than copy them into a second
  manual list.
- Add typed application properties only when the first real approved consumer
  appears.

## Future Risks

- A future module may confuse its environment alias with its canonical
  application property or wrap a native Spring property.
- Production profile activation remains a deployment responsibility; a later
  deployment Requirement may need stronger environment admission controls.
- A future secret provider needs explicit authentication, authorization,
  rotation, availability, failure, and incident-response decisions.
- Dynamic business configuration would require audit, versioning, approval,
  atomic rollout, rollback, and multi-instance consistency before adoption.
- Catalog/tests must evolve together when aliases or deployment sources change.
