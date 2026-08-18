# Configuration Management Skill

## When to Use

Use this skill whenever adding, changing, reviewing, documenting, or testing a
runtime setting, environment alias, profile override, default, validation rule,
or secret source in BrokerOS Risk.

Read the governing Requirement, ADR-008, the Q-006 architecture, `AGENTS.md`,
and `development-standards.md` first. A dynamic configuration service, secret
provider, business-policy store, or runtime administration capability requires
its own Requirement and ADR.

## Ownership Decision

Ask who owns the canonical property before writing code:

1. If Spring Boot or a library owns it, keep the native namespace and binder.
   Do not create wrappers such as `RedisProperties`, `KafkaProperties`,
   `DatasourceProperties`, or `FlywayProperties`.
2. If an approved BrokerOS capability owns a real cohesive group, use
   `brokeros.risk.<capability>` and one immutable typed properties object.
3. If it is only an environment alias, map and document it as a deployment
   compatibility contract; do not mistake the alias for application ownership.
4. If it is sensitive, classify the value as Secret and keep it external.

When no real BrokerOS-owned setting exists, add no production properties class.
Documentation and tests are preferable to an empty example abstraction.

## Application-Owned Properties Pattern

For a future approved group:

- use canonical lower-case kebab names;
- keep the type in the owning capability, not a generic dumping-ground package;
- prefer immutable constructor/record binding;
- use semantic Java types such as `Duration`, bounded numbers, URIs, or stable
  enums rather than strings where semantics are known;
- apply `@Validated`, Jakarta constraints, and explicit nested validation;
- register real groups explicitly while the repository has only a few;
- inject the typed object into its consumer;
- perform no external I/O or service injection during binding/validation;
- never expose a secret through `toString`, exceptions, logging, tests, or
  Actuator.

Avoid scattered `@Value`, repeated `Environment` lookups, direct
`System.getenv`, and `System.getProperty` for grouped application settings.

## Catalog Contract

Every supported setting records:

- Owner;
- Canonical Property;
- Environment Alias;
- Type and unit;
- Default;
- Requiredness;
- Profile;
- Sensitivity;
- Validation;
- Source;
- Restart requirement;
- Compatibility/deprecation behavior.

Update `docs/configuration/README.md` and tests in the same change. Renaming an
alias, changing type/unit/default/requiredness, or changing source precedence is
a compatibility decision, not a formatting cleanup.

## Profiles and Lifecycle

- Base/test/prod profiles select configuration; they are not a security or
  authorization boundary.
- Configuration is startup-bound and immutable for the process lifetime.
- Invalid required application configuration fails startup.
- Every configuration change requires restart.
- Do not add dynamic refresh, remote fallback, or an environment-specific Java
  branch without an approved Requirement and ADR.

## Secret Convention

- Commit names and source rules, never values.
- Keep `.env` and `.env.*` ignored and examples value-free.
- Use ephemeral values in CI and externally managed Kubernetes Secret
  references; never print interpolated secret configuration.
- Keep Actuator `env` and `configprops` unexposed.
- A safe validation error can name the property key/constraint but not the
  supplied secret value.
- Test secret-safe behavior with a runtime-generated synthetic value, not a
  committed credential-like literal.

## Test Pattern

Use existing Spring Boot test support before adding a dependency.
`ApplicationContextRunner` plus `ConfigDataApplicationContextInitializer` can
load the real packaged base/profile YAML without starting MySQL, Redis, Kafka,
or the web server.

For deterministic missing-property tests, remove the host
`systemEnvironment`/`systemProperties` property sources inside the runner. Then
cover:

1. profile overlay values;
2. missing required production placeholders;
3. invalid typed framework values;
4. environment-alias override of a packaged default;
5. diagnostics that do not contain a runtime-generated synthetic sensitive
   value;
6. Actuator `env`/`configprops` exclusion;
7. configuration catalog columns and every deployment alias extracted from
   YAML/Compose/Kubernetes sources.

Do not invent a fake production `@ConfigurationProperties` type for testing.

## Common Mistakes

- Wrapping framework properties to make them look BrokerOS-owned.
- Creating an empty properties class or broad `BrokerProperties` container.
- Treating a profile name as authorization or production detection.
- Adding a convenient production secret/default to tracked YAML.
- Logging an entire properties object or interpolated Compose configuration.
- Letting a missing-property test pass or fail based on the developer's shell.
- Testing a copied list of aliases while the actual deployment files drift.
- Enabling Actuator configuration endpoints because values are assumed to be
  sanitized.
- Claiming runtime refresh while only restarting the process.

## Validation Checklist

- Requirement and ADR authorize the setting and owner.
- Native framework binding is reused where available.
- No empty/group-less properties type or speculative abstraction exists.
- Catalog fields and alias compatibility are current.
- Required/invalid/profile/override/secret-safe behavior is tested.
- No real secret appears in Git, logs, errors, test output, or Review evidence.
- Actuator exposure remains `health,info` unless separately approved.
- No API, Flyway, database, Kafka topic/event, Redis business key, adapter, CI,
  or deployment-topology scope was added silently.
- Maven, static, Kustomize, applicable infrastructure, and Review gates are
  recorded honestly.
