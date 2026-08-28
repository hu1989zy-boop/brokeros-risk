# Q-010 Trading Account Reference Authority Foundation Implementation Design

## Document Status

- Requirement: Q-010 — APPROVED V1
- Architecture: Q-010 Architecture V1 — APPROVED
- ADR: ADR-012 — ACCEPTED
- Implementation Design version: V1
- Review phase: Q-010 V6 — Implementation Design Approval Recording
- Status: **APPROVED — EXTERNAL ARCHITECT**
- Architect approval date: 2026-08-27
- Approval origin: Explicit external Architect Review decision supplied for V6
- Implementation Design Complete: YES — submission ready
- Implementation Design Approved: YES
- Implementation Design V2 Required: NO
- Implementation: NOT STARTED
- Implementation Allowed: NO — pending independent review of the V6 approval
  recording and separate implementation authorization
- Date: 2026-08-27

This document converts the approved Q-010 Architecture into concrete future
implementation contracts. The external Architect approved this exact V1
Design through the decision recorded by Q-010 V6; Codex only records that
decision and does not self-approve. Approval creates no Java, Flyway migration,
table, endpoint, configuration, deployment behavior, Q-008 implementation,
staging, commit, or push.

## 1. Authority, Scope, and Non-Goals

### 1.1 Governing authority

This design is subordinate to:

1. repository `AGENTS.md` and development standards;
2. approved Q-010 Requirement V1;
3. approved Q-010 Architecture V1;
4. accepted ADR-012;
5. accepted ADR-002, ADR-009, ADR-010, and ADR-011; and
6. the implemented Q-009 ActorContext/authorization contracts.

If implementation evidence contradicts this design without changing an
approved invariant, the design must be repaired and reviewed. Identity
ownership, the external identity tuple, one-to-one immutability, lifecycle,
authoritative store, provisioning boundary, Q-008 disclosure, and history
atomicity cannot be changed inside implementation.

### 1.2 In scope

- the Q-010 Trading Account reference and authority-scope domain values;
- minimal current-state registries and immutable operation history;
- controlled scope and Trading Account registration;
- controlled lifecycle transitions;
- exact idempotency, uniqueness, concurrency, and transaction behavior;
- protected internal tuple resolution;
- the bounded Q-008 eligibility contract by `TradingAccountRef`;
- Q-009 service ActorContext and exact capability integration;
- a future additive Flyway V3 plan and Spring JDBC adapters;
- safe ResultCodes, exceptions, logs, metrics, and tests; and
- a non-Web, one-operation-per-manifest command boundary.

### 1.3 Explicit non-goals

This design excludes:

- Q-008 Risk Case implementation or changes to its approved aggregate/API;
- Evidence, Decision, Action, ActionOutcome, Rule Engine, Account Control, or
  external execution behavior;
- MT4/MT5/CRM/vendor adapters beyond a future canonical input contract;
- direct external database reads/writes, discovery, synchronization, CDC, or
  automatic registration;
- public/admin REST CRUD, UI, bulk registration, search, or reporting;
- customer, broker, tenant, organization, KYC, balance, position, or trading-
  state ownership;
- Redis keys/cache, Kafka topics/events, read replicas as authority, or another
  persistence system;
- generic IAM, roles, delegation, break-glass, or a second authorization model;
- aliasing, merge, split, reassignment, source migration, or multiple external
  identities per TradingAccountRef;
- cryptographic attestation, legal hold, retention/purge, or general Audit API;
  and
- any implementation before separate Design approval and explicit
  implementation authorization.

## 2. Design Outcome Summary

| Area | Concrete design decision |
| --- | --- |
| Module | `com.brokeros.risk.tradingaccount` in the existing deployable |
| Persistence | four additive MySQL tables through future Flyway V3 |
| References | exact `ta-<lowercase UUIDv4>` and `aas-<lowercase UUIDv4>` values |
| External key | validated Java String encoded once as exact UTF-8 and stored in `VARBINARY(512)` |
| Identity uniqueness | full scope/namespace/key unique index plus unique TradingAccountRef |
| Lifecycle | `ACTIVE`, `INACTIVE`, `RETIRED`; optimistic versioned named transitions |
| Operation model | one UUIDv4 operation ID is both manifest request ID and idempotency key |
| Fingerprint | SHA-256 of a fixed length-prefixed typed field sequence; no JSON-byte hashing |
| Transactions | one local MySQL transaction per registration/no-op/transition/history result |
| Concurrency | unique constraints and compare-and-set are final arbiters; no distributed lock |
| Provisioner | non-Web `WebApplicationType.NONE` command using one registered Q-009 SERVICE descriptor |
| Q-008 | one protected read-only eligibility query; no external identity disclosure |
| Dependencies | none beyond the committed Java/Spring JDBC/Jackson/MySQL/Flyway/Micrometer stack |
| Messaging/cache | none |

## 3. Module and Package Placement

The future feature remains inside the Phase 1 modular monolith:

```text
com.brokeros.risk.tradingaccount
├── domain
│   ├── TradingAccountRef
│   ├── AccountAuthorityScopeRef
│   ├── SourceNamespace
│   ├── ExternalAccountKey
│   ├── ExternalAccountIdentity
│   ├── AuthorityLifecycle
│   ├── AccountAuthorityScope
│   ├── TradingAccountReference
│   ├── AuthorityOperationId / AuthorityOperationType / AuthorityOperationOutcome
│   ├── AttestationReference / ChangeReason / ChangeReference
│   └── TradingAccountReferenceEligibility / EligibilityDecision
├── application
│   ├── TradingAccountCapabilities
│   ├── AuthorityScopeProvisioningService
│   ├── TradingAccountRegistrationService
│   ├── AuthorityScopeLifecycleService
│   ├── TradingAccountLifecycleService
│   ├── ExternalIdentityResolutionService
│   ├── TradingAccountReferenceEligibilityService
│   ├── ManifestFingerprintFactory
│   └── command/query/result records and expected BusinessExceptions
├── application.port
│   ├── TradingAccountAuthorityQueryPort
│   ├── TradingAccountAuthorityMutationPort
│   ├── TradingAccountRefGenerator
│   └── AccountAuthorityScopeRefGenerator
├── infrastructure.persistence
│   ├── JdbcTradingAccountAuthorityQueryAdapter
│   ├── JdbcTradingAccountAuthorityMutationAdapter
│   ├── JdbcAuthorityRowMappers
│   └── MySqlAuthorityConstraintClassifier
├── infrastructure.configuration
│   └── TradingAccountAuthorityConfiguration
└── interfaces.bootstrap
    ├── TradingAccountAuthorityBootstrapCommand
    ├── TradingAccountAuthorityManifestInput
    ├── TradingAccountAuthorityManifestMapper
    ├── TradingAccountProvisioningServiceDescriptor
    └── BootstrapExitCodeMapper
```

- `domain` uses only JDK types and owns identity/lifecycle invariants.
- `application` coordinates use cases, Q-009 authorization, fingerprinting,
  and typed ports; it imports no JDBC, Servlet, Spring Security, Jackson, or
  persistence record.
- `interfaces.bootstrap` is the only input adapter in the Foundation. It owns
  JSON/file/process concerns and is not a REST controller.
- `infrastructure.persistence` owns SQL, transaction templates, constraint-
  name classification, and row mapping.
- `infrastructure.configuration` composes beans and registers the purpose-
  specific Q-009 service descriptor.
- no JPA annotation or persistence field enters a domain value.
- no `common`, `utils`, `manager`, generic CRUD service, or dumping-ground
  package is added.

## 4. Domain Types and Invariants

### 4.1 Identity and namespace values

| Type | Responsibility and exact rules | Equality / exposure |
| --- | --- | --- |
| `TradingAccountRef` | immutable String; exactly `ta-` plus canonical lowercase UUIDv4; length 39; generator uses `UUID.randomUUID()` behind a port | full exact value; may cross the Q-008 boundary |
| `AccountAuthorityScopeRef` | immutable String; exactly `aas-` plus canonical lowercase UUIDv4; length 40; server-generated only | full exact value; internal to Q-010/provisioning, never sent to Q-008 |
| `SourceNamespace` | record of `sourceFamily`, `sourceInstance`, `server`, `environment`; all four required and already canonical | structural exact equality; internal, never Q-008 |
| `ExternalAccountKey` | retains validated String plus an immutable copy of exact UTF-8 bytes; never numeric/case-normalized | byte equality/hashCode; internal and sensitive |
| `ExternalAccountIdentity` | exact aggregate value of scope ref + namespace + external key | structural/byte equality; internal only |

Reference constructors reject null, whitespace changes, upper-case UUIDs,
non-v4 UUIDs, wrong prefixes, and noncanonical UUID rendering. `toString()` is
safe for BrokerOS refs but `ExternalAccountKey.toString()` returns a fixed
redacted value such as `[external-account-key]`.

### 4.2 Lifecycle and current-state models

`AuthorityLifecycle` has exactly `ACTIVE`, `INACTIVE`, and `RETIRED`.

`AccountAuthorityScope` contains:

- `AccountAuthorityScopeRef`;
- lifecycle;
- nonnegative `long version`;
- immutable registration attestation;
- registered-by `ActorRef` and UTC creation time; and
- last successful `AuthorityOperationId` and update time.

`TradingAccountReference` contains:

- `TradingAccountRef`;
- immutable `ExternalAccountIdentity`;
- lifecycle;
- nonnegative `long version`;
- immutable registration attestation;
- registered-by `ActorRef` and UTC creation time; and
- last successful operation ID and update time.

The models expose named transition methods only:

| Operation | Allowed transition | Result |
| --- | --- | --- |
| register | absent → `ACTIVE` | version 0 |
| deactivate | `ACTIVE` → `INACTIVE` | version + 1 |
| reactivate | `INACTIVE` → `ACTIVE` | version + 1; same identity only |
| retire | `ACTIVE` or `INACTIVE` → `RETIRED` | version + 1; terminal |

Same-state requests, any `RETIRED → *`, arbitrary status assignment, identity
replacement, and negative/overflow versions are rejected. Version increment
uses `Math.incrementExact`; overflow is an internal integrity failure, never
wraparound.

### 4.3 Operation and provenance values

- `AuthorityOperationId` is an exact canonical lowercase UUIDv4 without a
  prefix. It is the single request/provisioning/idempotency identity.
- `AuthorityOperationType` is one of `REGISTER_AUTHORITY_SCOPE`,
  `REGISTER_TRADING_ACCOUNT`, `DEACTIVATE_AUTHORITY_SCOPE`,
  `REACTIVATE_AUTHORITY_SCOPE`, `RETIRE_AUTHORITY_SCOPE`,
  `DEACTIVATE_TRADING_ACCOUNT`, `REACTIVATE_TRADING_ACCOUNT`, or
  `RETIRE_TRADING_ACCOUNT`.
- `AuthorityOperationOutcome` is `CREATED`, `UPDATED`, or `UNCHANGED`.
- `AttestationReference` contains `source` and `reference`. Source is 1–32
  lowercase ASCII characters matching `[a-z][a-z0-9-]{0,31}`. Reference is
  1–128 Unicode scalar values, at most 512 UTF-8 bytes, no control/NUL or edge
  whitespace, and is not logged.
- `ChangeReason` is 1–256 Unicode scalar values, at most 1024 UTF-8 bytes, no
  control/NUL or edge whitespace.
- `ChangeReference` is 1–128 Unicode scalar values, at most 512 UTF-8 bytes,
  with the same safe-text rejection rules.
- `ManifestFingerprint` is exactly 32 bytes and never rendered or logged.

The attestation reference identifies the externally approved broker/source-
owner record. Q-010 validates the reference and records it; deployment
governance establishes that the referenced record was approved. Q-010 does not
invent an external attestation API or cryptographic proof. Those mechanisms
would require a future Requirement.

### 4.4 Eligibility values

`EligibilityDecision` is deliberately narrow:

- `ELIGIBLE_FOR_NEW_ASSOCIATION`;
- `NOT_RECOGNIZED`; and
- `RECOGNIZED_NOT_ELIGIBLE`.

`TradingAccountReferenceEligibility` contains:

- the supplied `TradingAccountRef`;
- one decision;
- optional opaque `AuthoritySnapshotRef`; and
- optional opaque `AuthorityProvenanceRef`.

Snapshot/provenance refs are present for recognized results and absent for
`NOT_RECOGNIZED`. They are opaque `tasv1-<64 lowercase hex>` and
`tapv1-<64 lowercase hex>` values derived with SHA-256 from fixed framed safe
inputs. Consumers cannot parse scope identity, lifecycle, version, operation
ID, or external identity from them.

## 5. Q-009 Authorization Integration

### 5.1 Capability catalog

`TradingAccountCapabilities` owns exactly three constants using the committed
Q-009 `Capability` type:

```text
READ = trading-account-reference:read
REGISTER = trading-account-reference:register
CHANGE_LIFECYCLE = trading-account-reference:change-lifecycle
```

| Use case | Required capability |
| --- | --- |
| validate by TradingAccountRef for Q-008 | `READ` |
| internal exact tuple resolution | `READ` |
| register authority scope | `REGISTER` |
| register Trading Account reference | `REGISTER` |
| deactivate/reactivate/retire scope | `CHANGE_LIFECYCLE` |
| deactivate/reactivate/retire account | `CHANGE_LIFECYCLE` |

Every service receives an existing Q-009 `ActorContext` and calls the committed
`AuthorizationGuard.requireAllowed` before any Q-010 repository call. The
returned `AuthorizationDecision` is retained for mutation history. A denial
throws the existing `AuthorizationDeniedException`; a Q-009 database failure
throws `SecurityDependencyUnavailableException`. Neither path touches Q-010
data, so target existence is not disclosed.

Value construction/manifest syntax validation may reject malformed public
syntax without a database lookup, but no recognition, lifecycle, mapping, or
provenance decision occurs before authorization.

### 5.2 Purpose-specific service context

The controlled command uses one singleton
`TradingAccountProvisioningServiceDescriptor` whose exact service code is:

```text
trading-account-reference-provisioner
```

`TradingAccountAuthorityConfiguration` exposes that exact descriptor instance
as `RegisteredServiceDescriptor`. The existing Q-009 composition root is
minimally adjusted during later implementation to inject the Spring-collected
`Set<RegisteredServiceDescriptor>` into `ServiceActorContextFactory` instead
of the current deliberate empty set. No Q-009 domain, mapping, capability, or
trust behavior changes.

Deployment must use the existing Q-009 bootstrap to provision one active
SERVICE principal with:

- issuer `urn:brokeros:risk:internal-service`;
- subject `trading-account-reference-provisioner`;
- direct grants `trading-account-reference:register` and
  `trading-account-reference:change-lifecycle`; and
- approved deployment provisioning provenance.

The command calls `ServiceActorContextFactory.create(descriptor)` for every
invocation. Object identity, active Q-009 mapping, and the exact grant must all
pass. There is no SYSTEM actor, caller ActorRef, token fabrication, human
context reuse, or profile bypass. The command does not expose read operations,
so its service actor does not need the `READ` grant.

## 6. Application Use Cases

Every mutation receives an `AuthorizedMutationContext` built after Q-009
authorization. It contains operation ID, fingerprint, trusted ActorContext,
allow decision, attestation, reason, change reference, and server UTC time.
Caller-supplied actor or time fields do not exist.

| Use case/service | Typed input | Order and transaction | Output / failures / history |
| --- | --- | --- | --- |
| register scope | operation metadata only | REGISTER authorization → validate/fingerprint → mutation port transaction | generated scope ref, `CREATED`/`UNCHANGED`, version; conflict on operation/provenance; one history row |
| register account | scope ref, namespace, external key, operation metadata | REGISTER authorization → validate → transaction checks active scope and operation → insert/duplicate handling | generated/existing TradingAccountRef, `CREATED`/`UNCHANGED`, version; safe scope/mapping conflicts; one history row |
| resolve exact tuple | ActorContext + `ExternalAccountIdentity` | READ authorization → read-only primary MySQL query | recognized bounded internal view or not found; no mutation/history |
| validate for Q-008 | ActorContext + TradingAccountRef | READ authorization → read-only joined query | bounded eligibility; no external fields; no mutation/history |
| deactivate/reactivate/retire scope | scope ref, expected version, operation metadata | CHANGE_LIFECYCLE authorization → validate → CAS transaction | updated scope ref/status/version; invalid transition/version/not-found; state + operation + history atomic |
| deactivate/reactivate/retire account | account ref, expected version, operation metadata | same as scope | updated account ref/status/version; state + operation + history atomic |

### 6.1 Registration rules

- Scope registration derives a new scope ref. Exact replay returns the stored
  result. A new operation using the exact same attestation source/reference
  returns that existing scope as `UNCHANGED` and writes a no-state-change
  operation/history row. The scope attestation tuple is therefore unique.
- Account registration requires an existing `ACTIVE` scope. It derives a new
  TradingAccountRef. A new operation for an existing complete external tuple
  returns `UNCHANGED` only when immutable registration attestation is exactly
  the same; otherwise it returns a mapping conflict.
- Registration never accepts a proposed scope/account ref and never repairs,
  merges, aliases, reassigns, or changes lifecycle.

### 6.2 Read rules

- Exact tuple resolution is internal to Q-010 duplicate handling and future
  separately approved adapters. It is not exported to Q-008 or HTTP.
- The Q-008 query treats every stored lifecycle state as historically
  recognized. Only account `ACTIVE` plus scope `ACTIVE` is eligible.
- A read that returns zero rows is not found/not recognized. More than one row,
  invalid enum/version/ref data, or inconsistent FK state is an authority-
  integrity failure; the code never chooses the first row.
- Reads use the primary application DataSource. No cache, replica, external
  lookup, or stale-success fallback exists.

## 7. Application Ports and Ownership

### 7.1 Query port

Conceptual Java signatures:

```text
interface TradingAccountAuthorityQueryPort {
    Optional<CompletedAuthorityOperation> findOperation(AuthorityOperationId id);
    Optional<AuthorityScopeState> findScope(AccountAuthorityScopeRef ref);
    Optional<TradingAccountState> findByExternalIdentity(ExternalAccountIdentity identity);
    Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref);
}
```

The port returns immutable application persistence views containing only data
needed by the use case. It never returns `ResultSet`, table IDs to consumers,
JDBC types, vendor DTOs, or unbounded history collections. Implementations
query into a list and reject cardinality greater than one even though unique
constraints should prevent it.

### 7.2 Mutation port

```text
interface TradingAccountAuthorityMutationPort {
    ScopeProvisioningResult registerScope(RegisterScopeSpec spec,
                                           AuthorizedMutationContext context);
    AccountProvisioningResult registerAccount(RegisterAccountSpec spec,
                                               AuthorizedMutationContext context);
    LifecycleChangeResult changeScopeLifecycle(ChangeScopeLifecycleSpec spec,
                                               AuthorizedMutationContext context);
    LifecycleChangeResult changeAccountLifecycle(ChangeAccountLifecycleSpec spec,
                                                 AuthorizedMutationContext context);
}
```

Each method is one complete unit of work. The JDBC adapter owns the local
transaction and must write current state, final operation outcome, and exactly
one history row atomically. There is intentionally no public independent
history-write port that a service could forget or call in another transaction.

The adapter uses `TransactionTemplate` with the existing JDBC transaction
manager rather than an application-layer unit-of-work abstraction. This keeps
Spring out of domain/application and supports an explicit rollback followed by
an out-of-transaction replay/conflict re-read after a database race.

### 7.3 Generator ports

`TradingAccountRefGenerator` and `AccountAuthorityScopeRefGenerator` return the
typed refs. Production adapters use JDK UUIDv4; tests use deterministic
sequences to force collision behavior. The mutation adapter makes the database
unique constraint the final authority and permits at most three generated-ref
attempts, each as a fresh full transaction with the same operation ID and
fingerprint. Exhaustion fails closed; no existing row is overwritten.

No attestation-verifier port is added. The Foundation has no approved external
attestation protocol or cryptographic contract. Syntactic validation plus the
deployment-controlled invocation/record approval described in Section 11 is
the approved boundary.

## 8. Concrete Persistence Model

The future migration creates exactly four application-owned InnoDB tables.
All timestamps are server-derived UTC `DATETIME(6)`. All internal primary keys
are `BIGINT AUTO_INCREMENT` named `id` and never cross application boundaries.
No table has cascade delete, delete use case, money, customer, vendor payload,
or external credential data.

### 8.1 `trading_account_authority_scope`

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key, internal only |
| `authority_scope_ref` | `CHAR(40)` ASCII `ascii_bin` | not null | unique canonical `aas-<UUIDv4>` |
| `lifecycle_status` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `ACTIVE`/`INACTIVE`/`RETIRED` check |
| `version` | `BIGINT` | not null, default 0 | nonnegative optimistic version |
| `registration_attestation_source` | `VARCHAR(32)` ASCII `ascii_bin` | not null | immutable controlled source code |
| `registration_attestation_ref` | `VARCHAR(128)` UTF-8 `utf8mb4_bin` | not null | immutable bounded external approval ref |
| `registered_by_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 UUIDv4 ActorRef |
| `last_operation_id` | `CHAR(36)` ASCII `ascii_bin` | not null | latest successful operation UUIDv4 |
| `created_at` | `DATETIME(6)` | not null | UTC registration time |
| `updated_at` | `DATETIME(6)` | not null | UTC current-state update time |

Constraints/indexes:

- PK `pk_trading_account_authority_scope(id)`;
- unique `uk_ta_authority_scope_ref(authority_scope_ref)`;
- unique `uk_ta_authority_scope_attestation(registration_attestation_source,
  registration_attestation_ref)` so the same approved scope record cannot
  silently create two scopes;
- checks for canonical scope/actor/operation UUIDv4 shapes, allowed lifecycle,
  nonnegative version, source regex, and attestation-ref length; and
- `idx_ta_authority_scope_lifecycle(lifecycle_status)`.

### 8.2 `trading_account_reference`

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key |
| `trading_account_ref` | `CHAR(39)` ASCII `ascii_bin` | not null | unique canonical `ta-<UUIDv4>` |
| `authority_scope_id` | `BIGINT` | not null | FK to scope, delete restricted |
| `source_family` | `VARCHAR(63)` ASCII `ascii_bin` | not null | exact governed namespace field |
| `source_instance` | `VARCHAR(63)` ASCII `ascii_bin` | not null | exact governed namespace field |
| `source_server` | `VARCHAR(128)` ASCII `ascii_bin` | not null | exact governed namespace field |
| `source_environment` | `VARCHAR(32)` ASCII `ascii_bin` | not null | exact governed namespace field |
| `external_account_key` | `VARBINARY(512)` | not null | exact validated UTF-8 bytes, never logged |
| `lifecycle_status` | `VARCHAR(16)` ASCII `ascii_bin` | not null | approved lifecycle check |
| `version` | `BIGINT` | not null, default 0 | optimistic version |
| `registration_attestation_source` | `VARCHAR(32)` ASCII `ascii_bin` | not null | immutable source |
| `registration_attestation_ref` | `VARCHAR(128)` UTF-8 `utf8mb4_bin` | not null | immutable record ref |
| `registered_by_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted ActorRef |
| `last_operation_id` | `CHAR(36)` ASCII `ascii_bin` | not null | latest successful operation UUIDv4 |
| `created_at` | `DATETIME(6)` | not null | UTC |
| `updated_at` | `DATETIME(6)` | not null | UTC |

Constraints/indexes:

- PK `pk_trading_account_reference(id)`;
- unique `uk_trading_account_reference_ref(trading_account_ref)`;
- unique `uk_trading_account_reference_external_identity(authority_scope_id,
  source_family, source_instance, source_server, source_environment,
  external_account_key)`;
- FK `fk_ta_reference_scope` to scope `id` with `ON DELETE RESTRICT`;
- checks for ref UUIDv4, namespace regexes, `OCTET_LENGTH(external_account_key)
  BETWEEN 1 AND 512`, lifecycle, nonnegative version, ActorRef, operation ID,
  attestation source/ref; and
- index `idx_ta_reference_scope_lifecycle(authority_scope_id,
  lifecycle_status)`.

The composite unique key is at most approximately 806 payload bytes
(8 + 63 + 63 + 128 + 32 + 512), below the InnoDB 3072-byte key limit for the
repository's MySQL 8.4 target. Real MySQL verification remains mandatory.

### 8.3 `trading_account_authority_operation`

This is the durable idempotency outcome and replay source.

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key |
| `operation_id` | `CHAR(36)` ASCII `ascii_bin` | not null | globally unique UUIDv4 idempotency key |
| `schema_version` | `SMALLINT UNSIGNED` | not null | exactly 1 in Foundation |
| `operation_type` | `VARCHAR(40)` ASCII `ascii_bin` | not null | one approved operation code |
| `semantic_fingerprint` | `BINARY(32)` | not null | SHA-256 typed payload fingerprint |
| `target_type` | `VARCHAR(24)` ASCII `ascii_bin` | not null | `AUTHORITY_SCOPE`/`TRADING_ACCOUNT` |
| `authority_scope_id` | `BIGINT` | nullable | FK target for scope operation |
| `trading_account_id` | `BIGINT` | nullable | FK target for account operation |
| `target_ref` | `VARCHAR(40)` ASCII `ascii_bin` | not null | safe generated business ref |
| `outcome` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `CREATED`/`UPDATED`/`UNCHANGED` |
| `resulting_version` | `BIGINT` | not null | nonnegative durable replay result |
| `occurred_at` | `DATETIME(6)` | not null | server UTC operation time |

Constraints/indexes:

- unique `uk_ta_authority_operation_id(operation_id)`;
- FKs to scope/account with delete restricted;
- a check requiring exactly the target FK matching `target_type`;
- checks for schema 1, operation/target/outcome codes, canonical operation and
  target refs, and nonnegative version;
- indexes `idx_ta_operation_scope_time(authority_scope_id, occurred_at, id)`
  and `idx_ta_operation_account_time(trading_account_id, occurred_at, id)`.

### 8.4 `trading_account_authority_history`

This table is append-only application history, not the general Audit module.

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key/order tiebreaker |
| `operation_row_id` | `BIGINT` | not null | unique FK to operation, delete restricted |
| `actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 actor |
| `capability` | `VARCHAR(127)` ASCII `ascii_bin` | not null | exact evaluated capability |
| `authorization_evaluated_at` | `DATETIME(6)` | not null | Q-009 decision time |
| `authorization_actor_version` | `BIGINT` | not null | observed active actor version |
| `authorization_grant_version` | `BIGINT` | not null | observed explicit grant version |
| `attestation_source` | `VARCHAR(32)` ASCII `ascii_bin` | not null | approved record source |
| `attestation_ref` | `VARCHAR(128)` UTF-8 `utf8mb4_bin` | not null | approved record ref |
| `change_reason` | `VARCHAR(256)` UTF-8 `utf8mb4_bin` | not null | bounded reason, not logged |
| `change_ref` | `VARCHAR(128)` UTF-8 `utf8mb4_bin` | not null | ticket/change ref |
| `before_lifecycle` | `VARCHAR(16)` ASCII `ascii_bin` | nullable | null only for creation |
| `after_lifecycle` | `VARCHAR(16)` ASCII `ascii_bin` | not null | resulting/current lifecycle |
| `before_version` | `BIGINT` | nullable | null only for creation |
| `resulting_version` | `BIGINT` | not null | nonnegative result |
| `request_id` | `VARCHAR(128)` ASCII `ascii_bin` | nullable | bounded correlation only |
| `trace_id` | `CHAR(32)` ASCII `ascii_bin` | nullable | W3C trace ID only |
| `occurred_at` | `DATETIME(6)` | not null | same operation UTC time |

Constraints/indexes:

- unique `uk_ta_authority_history_operation(operation_row_id)` guarantees one
  history row per durable operation;
- FK to operation with delete restricted;
- checks for ActorRef/capability syntax, nonnegative authorization/state
  versions, lifecycle codes, creation versus non-creation before fields,
  attestation/source/reason/change bounds, request ID, trace ID, and matching
  resulting version semantics; and
- index `idx_ta_authority_history_time(occurred_at, id)` for bounded
  operational review.

The application adapter contains insert-only history SQL. It exposes no update
or delete. Parent FKs and operation/history FKs prevent normal parent deletion.
No physical-delete API/repository method exists. Direct privileged database
tampering is an operational security breach; runtime detects inconsistent
cardinality/state and fails closed rather than repairing it.

## 9. Canonicalization and Validation

### 9.1 SourceNamespace

Input must already be canonical. Q-010 rejects instead of normalizing:

| Field | Exact rule |
| --- | --- |
| source family | `[a-z][a-z0-9-]{0,62}` |
| source instance | `[a-z][a-z0-9-]{0,62}` |
| server | `[a-z0-9][a-z0-9._-]{0,127}` |
| environment | `[a-z][a-z0-9-]{0,31}` |

All are ASCII, nonblank, untrimmed, and compared by `String.equals` plus
`ascii_bin` in MySQL. Uppercase or edge whitespace is invalid, not rewritten.

### 9.2 ExternalAccountKey

Validation iterates Unicode code points and exact UTF-8 bytes:

- 1–128 Unicode scalar values (`String.codePointCount`);
- 1–512 bytes under a `CharsetEncoder` configured with `REPORT` for malformed
  or unmappable input;
- reject unpaired surrogates, U+0000, ISO control characters, and leading or
  trailing `Character.isWhitespace`/`isSpaceChar` code points;
- preserve leading zeros, exact case, internal spaces, punctuation, and exact
  normalization form;
- no `trim`, `strip`, numeric parsing, case folding, locale transform,
  Unicode normalization, or transliteration; and
- encode once as UTF-8 for persistence/fingerprint; return defensive copies.

`VARBINARY(512)` makes the database comparison byte-exact and avoids PAD SPACE
or collation behavior. A future source adapter may apply only its separately
approved source contract before constructing the Q-010 value.

### 9.3 Other bounded values

Refs/operation IDs use exact lowercase UUIDv4 formats. Attestation/reason/
change-ref validation counts Unicode scalar values and UTF-8 bytes and rejects
control/NUL/edge whitespace without modifying content. Expected versions are
required and nonnegative for lifecycle operations and forbidden for
registration. Generic metadata maps and unknown manifest fields are rejected.

## 10. Provisioning Manifest Contract

The command accepts one UTF-8 JSON document representing exactly one operation.
Jackson uses a copied application `ObjectMapper` with unknown-property failure,
duplicate-key detection, trailing-token failure, and no polymorphic/default
typing. The maximum file size is 64 KiB before parse. Symbolic links are
rejected; the normalized path must be a regular readable file.

### 10.1 Common envelope

```json
{
  "schemaVersion": 1,
  "operationId": "9b62b702-ff95-4f1d-9520-5bd9f6ae4d6a",
  "operation": "REGISTER_TRADING_ACCOUNT",
  "authorityScopeRef": "aas-00000000-0000-4000-8000-000000000000",
  "tradingAccountRef": null,
  "sourceNamespace": {
    "sourceFamily": "platform-family",
    "sourceInstance": "source-instance-1",
    "server": "server-1",
    "environment": "production"
  },
  "externalAccountKey": "ExactExternalKey",
  "expectedVersion": null,
  "attestation": {
    "source": "broker-record",
    "reference": "approved-record-reference"
  },
  "reason": "Initial controlled registration",
  "changeRef": "change-ticket-reference"
}
```

The values above are structural examples, not production identity data or a
tracked default manifest.

`operationId` is both the request/provisioning ID and idempotency key; a second
idempotency field is prohibited. The manifest never accepts actor/operator
identity, timestamp, proposed generated ref, credential, token, vendor payload,
customer data, or arbitrary metadata. Trusted actor, authorization evidence,
generated ref, fingerprint, UTC time, and correlation come from the server.

### 10.2 Operation-specific field matrix

| Operation | Required target fields | Forbidden target fields |
| --- | --- | --- |
| `REGISTER_AUTHORITY_SCOPE` | none | both refs, namespace, key, expectedVersion |
| `REGISTER_TRADING_ACCOUNT` | authorityScopeRef, namespace, external key | tradingAccountRef, expectedVersion |
| scope deactivate/reactivate/retire | authorityScopeRef, expectedVersion | tradingAccountRef, namespace, key |
| account deactivate/reactivate/retire | tradingAccountRef, expectedVersion | authorityScopeRef, namespace, key |

Attestation, reason, and changeRef are mandatory for every operation. Schema
versions other than integer `1`, unknown operation codes, missing fields,
extra operation fields, duplicate JSON keys, and trailing content fail before
any write.

### 10.3 Validation and execution order

1. check one argument, regular non-symlink file, and 64 KiB bound;
2. strict-parse the envelope and validate only schema/operation needed to
   select capability;
3. obtain a fresh Q-009 service ActorContext;
4. require the exact operation capability, with no Q-010 lookup;
5. validate all typed fields and operation-specific absence rules;
6. build the fixed semantic fingerprint;
7. derive server UTC time and authorized mutation context;
8. execute one mutation-port transaction; and
9. emit a safe report only after commit.

The externally approved attestation record and manifest must be established
under deployment change control before invocation. If the operator cannot
establish it, invocation must not occur; missing/invalid references are also
rejected by Q-010.

## 11. Controlled Non-Web Execution Boundary

`TradingAccountAuthorityBootstrapCommand` follows the committed Q-009 command
pattern:

- requires exactly one external manifest path;
- starts `BrokerOsRiskApplication` with `WebApplicationType.NONE`;
- uses the application DataSource/Flyway/security/Q-010 beans;
- never listens on HTTP, watches a directory, polls, schedules, or processes a
  batch;
- accepts no inline JSON or environment variable containing the external key;
- does not copy the manifest into Git, logs, history, or a database blob;
- creates a fresh service ActorContext and authorizes the selected use case;
- closes the application context and file resources; and
- emits one safe result report.

Safe report fields are schema version, operation ID, operation type, outcome,
generated/existing BrokerOS target ref, resulting version, and server time.
It contains no external key/namespace, manifest body, attestation ref, reason,
ActorRef, fingerprint, credentials, or stack trace.

| Process exit | Meaning |
| ---: | --- |
| 0 | committed `CREATED`, `UPDATED`, `UNCHANGED`, or exact replay |
| 2 | malformed/unsupported/invalid manifest |
| 3 | actor mapping or authorization denied |
| 4 | idempotency/mapping/version/lifecycle conflict |
| 5 | authorized target not found or scope not eligible |
| 6 | Q-009/Q-010/MySQL dependency unavailable |
| 10 | unexpected safe internal failure |

Retries are explicit operator invocations using the exact same manifest and
operation ID after an uncertain result. Deployment access to invoke the
command is an operational control in addition to Q-009; it is not represented
as a Spring profile or application permission bypass.

## 12. Semantic Fingerprint and Idempotency

`ManifestFingerprintFactory` does not hash raw JSON. After typed validation it
serializes a fixed ordered field sequence:

1. schema version;
2. operation code;
3. authority scope ref or null marker;
4. TradingAccountRef or null marker;
5. four namespace fields or null markers;
6. exact external-key UTF-8 bytes or null marker;
7. expected version or null marker;
8. attestation source and reference;
9. reason; and
10. change reference.

Each byte field is prefixed by a signed four-byte big-endian length; `-1`
represents null and `0` represents an empty value (which validation normally
rejects). Integers use fixed eight-byte big-endian form. A constant ASCII
domain separator `brokeros-risk:q010:manifest-fingerprint:v1` is the first
framed field. SHA-256 from the JDK produces exactly 32 stored bytes.

The operation ID, actor, authorization versions/time, server time, request ID,
trace ID, generated target ref, and JSON property order are excluded. Changing
any semantic manifest field while reusing the operation ID therefore conflicts;
formatting/property-order differences do not.

| Case | Exact outcome |
| --- | --- |
| same operation ID + same fingerprint | return durable stored result; no new state/version/operation/history |
| same operation ID + different fingerprint | `TRADING_ACCOUNT_IDEMPOTENCY_CONFLICT`; no mutation |
| new operation ID + same scope attestation | `UNCHANGED` existing scope; new no-op operation/history |
| new operation ID + same account tuple + same registration attestation | `UNCHANGED` existing account; new no-op operation/history |
| new operation ID + existing tuple + different registration attestation | mapping conflict; no operation/history commit |
| lost response after commit | exact replay returns stored result |
| failure before commit | no state/operation/history survives; exact retry may execute |

## 13. Transaction, Concurrency, and Retry Design

### 13.1 Common transaction algorithm

1. authorization and complete validation occur before the transaction;
2. query operation ID before mutation; exact completed replay returns, changed
   fingerprint conflicts;
3. start one local InnoDB transaction through `TransactionTemplate`;
4. recheck operation ID inside the transaction;
5. load the necessary current scope/account state;
6. insert current state or execute lifecycle compare-and-set;
7. insert the final operation outcome referencing the resulting target row;
8. insert exactly one history row;
9. commit; then build/emit the response.

No success is returned before commit. Any exception from state, operation, or
history insertion rolls back all three. There is no `REQUIRES_NEW`, external
call, 2PC, Saga, event publication, or distributed lock.

### 13.2 Registration races

- Application prechecks improve diagnostics but never grant uniqueness.
- Concurrent same-tuple account inserts are serialized by
  `uk_trading_account_reference_external_identity`. The loser rolls back, then
  reloads the operation ID first and tuple second outside the failed
  transaction. It returns replay/unchanged only under the exact rules in
  Section 12; otherwise conflict.
- Concurrent same-attestation scope registration follows the scope attestation
  unique key and the same classification.
- Generated-ref collisions are identified only by the exact named unique
  constraint and MySQL error 1062. The full rolled-back transaction may retry
  with a new generated ref up to three attempts. Tuple/attestation/operation
  constraint failures are never treated as UUID collisions.
- A TradingAccountRef cannot be caller-supplied. One ref mapping to another
  tuple can arise only from an astronomically unlikely generator collision or
  database tampering; the unique constraint rejects it.

`MySqlAuthorityConstraintClassifier` inspects the root `SQLException` error
code, SQLState, and exact named constraint from the MySQL 8.4 driver. Unknown
duplicate/integrity errors fail as authority unavailable rather than being
misclassified as compatible replay. Real-driver tests lock down this behavior.

### 13.3 Lifecycle races

Lifecycle SQL is a single compare-and-set:

```text
UPDATE <current_table>
SET lifecycle_status = ?, version = version + 1,
    last_operation_id = ?, updated_at = ?
WHERE id = ? AND version = ? AND lifecycle_status = ?
```

Exactly one row must update. Zero rows causes rollback; after rollback, the
adapter first checks whether the operation ID committed concurrently. If so,
it applies replay rules. Otherwise the service returns not-found, invalid
transition, or version conflict from a fresh authoritative read. It never
blindly repeats with a new version or uses last-write-wins.

### 13.4 Deadlocks and uncertain commit

MySQL deadlock 1213, lock timeout 1205, connection loss, and commit uncertainty
are not automatically retried by a generic policy. The transaction is rolled
back when the driver can determine failure; the command returns dependency
unavailable. The operator retries the exact same manifest/operation ID. If the
first commit actually succeeded, durable replay returns it; otherwise the
operation executes once. Logical conflicts are never retried automatically.

### 13.5 History failure

Operation history is inserted after current state and operation outcome but
before commit. A history constraint/insert failure marks the transaction
rollback-only. Tests force the history insert to fail and prove that current
state/version and the operation row are both absent/unchanged afterward.

## 14. Q-008 Read-Only Consumer Contract

The only Q-008-facing contract is:

```text
TradingAccountReferenceEligibility validateForNewRiskCaseAssociation(
    ActorContext actorContext,
    TradingAccountRef tradingAccountRef)
```

It is an application interface/service owned by Q-010, not a REST endpoint or
repository. Q-008 passes the same trusted ActorContext used by its case use
case. Q-008 must first enforce its own `risk-case:*` capability; Q-010 then
independently requires `trading-account-reference:read` before Q-010 lookup.

| Durable state | Returned decision |
| --- | --- |
| no account row | `NOT_RECOGNIZED`, no snapshot/provenance |
| account ACTIVE and scope ACTIVE | `ELIGIBLE_FOR_NEW_ASSOCIATION` |
| account INACTIVE or RETIRED | `RECOGNIZED_NOT_ELIGIBLE` |
| scope INACTIVE or RETIRED | `RECOGNIZED_NOT_ELIGIBLE` |
| inconsistent/ambiguous rows | throw safe authority-unavailable failure |
| MySQL unavailable | throw `TRADING_ACCOUNT_AUTHORITY_UNAVAILABLE` |
| authorization denied/unavailable | existing Q-009 safe failure before lookup |

Recognized responses include opaque snapshot/provenance refs derived from the
account/scope versions and last operation IDs. They expose no raw version,
scope ref, lifecycle detail, external key, SourceNamespace, internal ID,
attestation, actor, customer data, vendor DTO, or persistence record. All
ineligible lifecycle cases deliberately collapse to one bounded decision.

Q-008 never queries Q-010 tables and cannot register, mutate, discover, or
resolve an external identity. This design does not wire or modify Q-008; the
facade becomes available only to a separately authorized Q-008 implementation.

## 15. Error, ResultCode, and Exception Model

Future implementation adds only real Q-010 ResultCodes. Expected failures use
small `BusinessException` subclasses; unexpected exceptions remain bounded by
`GlobalExceptionHandler` if reached from a future HTTP consumer. The non-Web
command maps the same codes to Section 11 exit categories.

| Condition | ResultCode / application outcome | HTTP category if later surfaced |
| --- | --- | ---: |
| invalid TradingAccountRef | `TRADING_ACCOUNT_REFERENCE_INVALID` | 400 |
| invalid scope ref | `ACCOUNT_AUTHORITY_SCOPE_INVALID` | 400 |
| invalid namespace | `SOURCE_NAMESPACE_INVALID` | 400 |
| invalid external key | `EXTERNAL_ACCOUNT_KEY_INVALID` | 400 |
| malformed manifest/field matrix | `TRADING_ACCOUNT_MANIFEST_INVALID` | 400 |
| unsupported schema | `TRADING_ACCOUNT_MANIFEST_SCHEMA_UNSUPPORTED` | 400 |
| missing/invalid attestation | `TRADING_ACCOUNT_ATTESTATION_INVALID` | 422 |
| exact replay | stored success result | 200-equivalent |
| compatible duplicate under new operation | `UNCHANGED` success | 200-equivalent |
| same operation ID/different fingerprint | `TRADING_ACCOUNT_IDEMPOTENCY_CONFLICT` | 409 |
| tuple or scope provenance conflict | `TRADING_ACCOUNT_MAPPING_CONFLICT` | 409 |
| scope not found | `ACCOUNT_AUTHORITY_SCOPE_NOT_FOUND` | 404 |
| authorized account not found for mutation | `TRADING_ACCOUNT_REFERENCE_NOT_FOUND` | 404 |
| inactive/retired scope during account registration | `ACCOUNT_AUTHORITY_SCOPE_NOT_ELIGIBLE` | 422 |
| stale expected version | `TRADING_ACCOUNT_VERSION_CONFLICT` | 409 |
| same-state/forbidden/terminal transition | `TRADING_ACCOUNT_INVALID_TRANSITION` | 422 |
| Q-009 actor/capability denial | existing `ACTOR_ACCESS_DENIED` / `AUTHORIZATION_DENIED` | 403 |
| Q-009 authority unavailable | existing `SECURITY_DEPENDENCY_UNAVAILABLE` | 503 |
| MySQL unavailable/unknown integrity/UUID retry exhausted | `TRADING_ACCOUNT_AUTHORITY_UNAVAILABLE` | 503 |
| history insert failure | rollback, then `TRADING_ACCOUNT_AUTHORITY_UNAVAILABLE` | 503 |
| account inactive/retired Q-008 read | `RECOGNIZED_NOT_ELIGIBLE` result, not exception | 200-equivalent |

No error includes an external key, namespace, attestation reference, target
existence before authorization, constraint value, SQL, or stack trace. Unknown
constraint/inconsistent state is unavailable, never an arbitrary conflict
winner.

## 16. Logging, Sensitive Data, and Observability

### 16.1 Logging rules

Allowed bounded fields:

- event name, operation type, outcome, safe ResultCode;
- operation ID/manifest ID;
- generated BrokerOS TradingAccountRef or scope ref after authorization;
- resulting version;
- request ID/trace ID from existing correlation; and
- UTC duration/time.

Never log:

- ExternalAccountKey or any reversible/hash fingerprint of it;
- SourceNamespace or complete external tuple;
- manifest body/path content, semantic fingerprint, attestation ref, reason;
- credentials, tokens, claims, external principal key;
- raw SQL/parameters or constraint values; or
- customer/vendor payloads.

ActorRef is persisted in immutable history but omitted from ordinary Q-010
logs. Conflict diagnostics report only constraint category (`OPERATION`,
`EXTERNAL_IDENTITY`, `SCOPE_ATTESTATION`, `GENERATED_REF`, `UNKNOWN`) after
authorization. `ExternalAccountKey.toString()` is redacted to reduce accidental
structured logging leakage.

### 16.2 Metrics

Reuse the existing Micrometer/Actuator platform; add no dependency or exporter.
Low-cardinality counters/timers may be:

- `brokeros.risk.trading.account.authority.operations` tagged by operation and
  outcome;
- `brokeros.risk.trading.account.authority.conflicts` tagged by safe category;
- `brokeros.risk.trading.account.authority.authorization.denied` tagged only by
  capability/action category;
- `brokeros.risk.trading.account.authority.persistence.failures` tagged by
  deadlock/timeout/unavailable/integrity; and
- `brokeros.risk.trading.account.authority.duration` tagged by operation.

No metric tag contains refs, operation IDs, actors, namespace, external keys,
attestation, exception messages, SQL, or correlation IDs. Health uses existing
DataSource readiness; Q-010 adds no public detailed health endpoint.

## 17. Security Design Review

| Threat | Required implementation control |
| --- | --- |
| account enumeration | exact Q-009 authorization before every Q-010 lookup; bounded denial |
| privilege escalation | module-owned exact capabilities; only explicit ALLOW; no role/SYSTEM/wildcard |
| arbitrary service identity | descriptor singleton object identity plus active Q-009 mapping/grant |
| manifest tampering | strict schema/unknown/duplicate/trailing checks; semantic fingerprint tied to operation ID |
| replay/change after timeout | durable operation ID + fingerprint; changed replay conflicts |
| false mapping attestation | deployment-controlled approved record plus required bounded provenance; capability alone insufficient |
| identifier leakage | no key/tuple/manifest/attestation in logs, metrics, reports, errors, or Q-008 |
| unauthorized remap | no update method for identity; two unique keys; generated ref only |
| destructive deletion | no delete port/SQL/API; restrict FKs; retained operation/history |
| state without history | one transaction; forced history failure rollback test |
| direct DB bypass/corruption | least-privilege deployment governance; runtime cardinality/enum/ref checks and fail closed |
| stale cache/replica | neither exists; primary MySQL only |
| external-system coupling | no external DB/API/SDK; canonical values only |
| command exposure | WebApplicationType.NONE, explicit file, no scheduler/watcher/REST |

Implementation review must statically prove domain/application contain no
Spring Security, Servlet, JDBC, Jackson, vendor, Redis, or Kafka imports and no
actor/header/role bypass strings.

## 18. Test Design

### 18.1 Value-object and domain unit tests

- generated/ref parsing: correct prefix/lowercase/canonical UUIDv4; reject v1,
  upper-case, whitespace, wrong prefix, malformed values;
- all SourceNamespace bounds/regexes and exact case;
- ExternalAccountKey: empty, 128/129 scalar, 512/513 byte boundaries,
  surrogate errors, control/NUL, edge Unicode whitespace, leading zero,
  internal space, case, composed/decomposed distinction, defensive bytes;
- lifecycle allowed transitions, terminal RETIRED, same-state rejection,
  version increment/overflow;
- attestation/reason/change bounds and redacted key `toString`;
- operation UUID/type/outcome and eligibility invariants; and
- exact fingerprint golden vectors, property-order independence, and one-field
  change sensitivity.

### 18.2 Application-service tests

Use fake ports only as focused test doubles, never production providers:

- every use case invokes `AuthorizationGuard` before any Q-010 port;
- denial/unavailability yields zero Q-010 interactions;
- registration, exact replay, compatible duplicate, conflicting replay;
- unknown/inactive/retired scope behavior;
- lifecycle commands and stale/illegal outcomes;
- tuple resolution and eligibility mappings;
- recognized ineligible states remain historical; and
- safe exception/result mapping contains no sensitive input.

### 18.3 Q-009 integration tests

- register the exact singleton descriptor and reject another instance/string;
- absent/disabled service mapping, absent/revoked grant, and security DB outage
  fail closed;
- each command creates a fresh ActorContext/execution ID;
- REGISTER cannot perform lifecycle and CHANGE_LIFECYCLE cannot register;
- no READ grant is implicitly inherited; and
- ActorRef/decision versions and time copied to history, never external
  principal key.

### 18.4 Manifest/command tests

- one argument, regular non-symlink file, 64 KiB size, UTF-8 errors;
- strict schema, duplicate keys, unknown fields, trailing JSON;
- every operation-specific required/forbidden field combination;
- caller actor/timestamp/ref/credential/generic metadata fields rejected;
- safe output and each exit code; captured logs/output contain no key,
  namespace, attestation, reason, actor, fingerprint, or manifest body;
- WebApplicationType.NONE and no HTTP listener; and
- exact replay after a simulated lost response.

### 18.5 Real MySQL 8.4 migration/persistence tests

Use a disposable MySQL 8.4.11 environment through mandatory
`Q010_MYSQL_TEST_URL`, username, and password inputs. No H2 proof and no skipped
mandatory gate is accepted. Verify:

- clean V1→V2→V3 and existing V2→V3 upgrade, Flyway validate/restart/checksum;
- exactly four new tables and no data seed/destructive DDL;
- ref, lifecycle, version, namespace, operation, capability, FK, and CHECK
  constraints including MySQL error 3819/SQLState HY000;
- exact VARBINARY key behavior (`001` ≠ `1`, `A` ≠ `a`, NFC ≠ NFD);
- both unique directions and named constraint classification under error 1062;
- composite index creation within the actual InnoDB key limit;
- FK/restrict deletion and append-only adapter SQL;
- query plans use the business-ref/full-tuple/operation unique indexes and do
  not full-scan authority tables;
- actor/operation/history fields and UTC microseconds round-trip; and
- MySQL outage/driver failures translate to safe unavailable outcomes.

### 18.6 Transaction and concurrency tests

- two independent transactions insert the same tuple after a barrier: exactly
  one commit; database unique key, not precheck, elects the winner;
- concurrent same operation ID/same fingerprint returns one commit plus one
  replay; changed fingerprint conflicts;
- concurrent scope attestation registration returns created/unchanged;
- forced generated-ref unique collision retries at most three and never
  overwrites;
- concurrent lifecycle commands with expected version N: one commit, one
  version conflict/replay classification;
- install a disposable test-only BEFORE INSERT history trigger that signals an
  error, then prove current-state/version and operation row roll back; remove
  the trigger before other tests;
- connection/commit uncertainty followed by the exact operation replay;
- deadlock/lock-timeout classification returns unavailable with no automatic
  changed operation; and
- no partial scope/account/operation/history rows after any failed transaction.

### 18.7 Q-008 consumer/security tests

- valid actor with READ: active/active eligible;
- unknown returns not recognized;
- every inactive/retired account/scope combination collapses to recognized not
  eligible;
- result contains only ref, decision, opaque snapshot/provenance;
- unauthorized/missing/revoked READ and security outage call no Q-010 query;
- MySQL outage and corrupted multi-row/invariant state fail unavailable;
- external key, namespace, scope, raw versions, operation IDs, persistence IDs,
  lifecycle detail, vendor/customer data are absent by type/static inspection;
- Q-008 has no Q-010 repository/table import and cannot mutate; and
- validation evidence changes when scope/account version/last operation changes
  without revealing those inputs.

### 18.8 Regression and architecture tests

- existing Q-009 signed-JWT/service/MySQL tests continue passing;
- existing health, `ApiResponse`, GlobalExceptionHandler, correlation, config,
  Flyway, Compose, and Kustomize gates remain green;
- package dependency test prohibits infrastructure/framework imports from
  Q-010 domain/application;
- static scan proves no REST controller, Kafka/Redis, external DB, vendor SDK,
  delete SQL, migration edits, permissive provider, SYSTEM, actor header, or
  raw external key logging; and
- Maven dependency tree remains unchanged and contains no ORM/JSON/hash/retry
  library added for Q-010.

## 19. Flyway and Rollout Plan

The next future migration is exactly:

```text
V3__create_trading_account_reference_authority.sql
```

It is one coherent forward-only additive migration after committed V1/V2. It
creates the four tables in dependency order: scope, account reference,
operation, history. It inserts no scope, account, actor, grant, manifest, or
vendor data and does not edit V1/V2. Correction uses V4 or later, never an edit
after application.

Static migration tests must assert exactly the four approved CREATE TABLE
names, `ascii_bin`, `utf8mb4_bin`, `VARBINARY(512)`, named constraints, and
`ON DELETE RESTRICT`, and prohibit DROP/TRUNCATE/DELETE/UPDATE/data INSERT.

Future rollout after all separate approvals:

1. apply/validate V3 on disposable MySQL 8.4.11 and then the target database;
2. provision/verify the Q-009 purpose-specific SERVICE actor and two direct
   grants through the existing Q-009 bootstrap;
3. approve one non-secret Q-010 manifest outside Git under deployment change
   control;
4. invoke the Q-010 non-Web command and retain its safe report;
5. query safe BrokerOS refs/current states through controlled operational
   verification without printing external keys; and
6. enable separately approved application consumers.

Application rollback leaves additive tables/history intact. Bad identity data
is deactivated/retired only through a separately authorized operation; it is
never manually deleted/remapped. Identity repair, merge, or reassignment needs
a future Requirement.

## 20. Recommended Future Implementation Sequence

Only after Design approval and explicit implementation authorization:

1. add Q-010 ResultCodes, domain values, transitions, fingerprint, and unit
   tests;
2. add future Flyway V3 plus static and disposable MySQL migration tests;
3. implement query/mutation ports and JDBC transaction adapters;
4. implement registration/idempotency/concurrency/history services and tests;
5. implement lifecycle services and CAS/rollback tests;
6. register the Q-010 descriptor through the minimal Q-009 composition-root
   extension and verify service authorization;
7. implement the strict non-Web command/manifest/output boundary;
8. implement the Q-010 eligibility facade and contract tests without wiring or
   modifying Q-008;
9. run complete Maven, MySQL 8.4, Flyway, security, static, Compose,
   Kustomize, logging, and concurrency gates with zero mandatory skip; and
10. update Skills/Lessons from actual verified implementation and create the
    final Review Package.

This ordering is a design, not authorization and not a commit plan.

## 21. Requirement and Acceptance Traceability

| Requirement | Design coverage | Planned verification |
| --- | --- | --- |
| Q010-FR-001 | Sections 4.1, 7.3, 8.2 | ref value/generator/unique tests |
| Q010-FR-002 | Sections 4.1, 8.2, 9 | tuple/value/collation tests |
| Q010-FR-003 | Sections 8.2, 12, 13 | two unique directions and race tests |
| Q010-FR-004 | Sections 6, 10–13 | manifest/idempotency/non-Web tests |
| Q010-FR-005 | Sections 6.2, 14, 15 | eligibility/not-found/unavailable tests |
| Q010-FR-006 | Sections 4.2, 8, 14 | lifecycle/history/delete-restriction tests |
| Q010-FR-007 | Sections 4.2–4.3, 8.4, 13 | CAS/attribution/atomic-history tests |
| Q010-FR-008 | Sections 5–6 | authorization-before-port tests |
| Q010-FR-009 | Sections 13–15, 17 | denial/conflict/outage/non-enumeration tests |
| Q010-FR-010 | Sections 14, 16 | type/serialization/log absence tests |
| Q010-FR-011 | Sections 1, 3, 9, 17 | package/static/external-isolation tests |
| Q010-FR-012 | Sections 2, 8, 16, 18 | dependency/package/schema inspection |

All twelve Q-010 Acceptance Criteria have a concrete design home: approved
governance remains separate; opaque identity cannot derive from vendor data;
both uniqueness directions and concurrency have testable outcomes; Q-008 is a
narrow read-only consumer; Q-009 protects all access; history is atomic;
future schema is additive/application-owned/MySQL-verified; no trading,
customer, vendor, Kafka, Redis, or prohibited technology enters scope; and all
later runtime/Review/authorization/commit gates remain separate.

## 22. Architecture Gap and Outstanding Decisions

### 22.1 Architecture gaps

**None.** Every decision made here falls within the explicit Implementation
Design deferrals in approved Architecture Section 22. The design does not
change ownership, tuple, immutability, lifecycle, MySQL authority, provisioning
boundary, authorization, Q-008 disclosure, or history atomicity.

### 22.2 Deployment inputs, not design gaps

- actual target-deployment authority scopes;
- actual broker/source-owner-approved attestation records/references;
- actual manifests, reasons, and change tickets;
- actual Q-009 service ActorRef generated during deployment bootstrap; and
- target-environment database credentials/change window.

These values are intentionally external and must never be invented or
committed as defaults.

### 22.3 Future Requirement scope

Aliases/merge/migration/reassignment, automated discovery/synchronization,
source adapters, public administration, master data, cache/events,
cryptographic attestation, retention/redaction, direct repair, and federation
remain future Requirements.

## 23. Design Gate

- Requirement: APPROVED V1
- Architecture: APPROVED V1
- ADR-012: ACCEPTED
- Implementation Design V1 complete for submission: YES
- Implementation Design V1 approved: **YES — EXTERNAL ARCHITECT — 2026-08-27**
- Implementation Design V2 required: NO
- Approval recording: V6 — awaiting independent Architect review
- Implementation: NOT STARTED
- Implementation Allowed: **NO**
- Q-008 Implementation: NOT STARTED / PREREQUISITE-GATED

Next gate: **Independent Architect review of the Q-010 V6 Design Approval
Recording package. Do not start implementation without that review and a
separate explicit implementation authorization.**
