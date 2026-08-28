# Q-010 V5 — Implementation Design Prompt

## Role

You are Codex working inside the BrokerOS Risk repository.

This task is **Q-010 V5 — Implementation Design**.

The following gates are already closed and must be treated as authoritative:

- Q-010 Requirement: APPROVED
- Q-010 Architecture: APPROVED
- ADR-012: ACCEPTED
- Q-010 V4 Approval Recording: APPROVED
- Implementation Design: NOT STARTED
- Implementation: NOT STARTED
- Implementation Allowed: NO

Your responsibility in this round is to produce a complete, implementation-ready design based strictly on the approved Requirement, Architecture, and ADR.

This is **design only**.

Do not implement production code.
Do not create Flyway migrations.
Do not create REST endpoints.
Do not start Q-008 implementation.
Do not commit or push.

---

# 1. Mandatory Preflight

Before editing:

1. Read and obey repository root `AGENTS.md`.
2. Inspect current branch, HEAD, working tree, and Q-010 artifact history.
3. Read:
   - Q-010 Requirement
   - Q-010 Architecture
   - ADR-012
   - Q-010 V4 approval recording
   - Q-009 ActorContext / authorization artifacts used by Q-010
   - Q-008 Requirement/Architecture only where needed to define the consumer contract
   - existing repository conventions for implementation design documents, persistence, ResultCode, exceptions, tests, Flyway, package layout, review packaging
4. Preserve unrelated pre-existing changes.
5. Do not modify approved architectural semantics.
6. Do not self-approve the design.

If repository reality differs from this prompt, repository artifacts are the source of truth. Explain discrepancies rather than silently inventing replacements.

---

# 2. Design Objective

Translate the approved Q-010 Architecture into a concrete implementation design that an engineer can implement with minimal ambiguity.

The design must answer:

- which modules/packages/classes/interfaces are needed
- who owns each responsibility
- what the domain model is
- what persistence structures are required
- what uniqueness constraints are required
- what transactions exist
- how concurrency races are handled
- how provisioning works
- how idempotency works
- how lifecycle transitions work
- how authorization is enforced
- what Q-008 is allowed to consume
- what Q-008 is forbidden to consume
- how immutable history is recorded
- how failures map to result codes/exceptions
- how all of this is verified through tests

Do not leave critical implementation choices as “TBD” if they can be resolved from approved architecture and repository conventions.

---

# 3. Approved Architecture Invariants

The implementation design must preserve all of the following without weakening them.

## 3.1 TradingAccountRef

`TradingAccountRef` is a BrokerOS-owned stable opaque business identity.

It must not equal or derive identity semantics from:

- MT4 login
- MT5 login
- CRM account ID
- external account number
- vendor DB ID
- persistence PK

Use the representation already approved in Architecture/ADR.

## 3.2 External Identity Tuple

External authority identity is the tuple:

- `AccountAuthorityScopeRef`
- `SourceNamespace`
- `ExternalAccountKey`

This tuple must be treated as one unique external identity.

## 3.3 One-to-One Immutable Mapping

The design must enforce:

- one external identity tuple -> exactly one TradingAccountRef
- one TradingAccountRef -> exactly one external identity tuple
- no silent reassignment
- no destructive identity delete
- no hidden remap via update
- historical resolution remains possible

## 3.4 Lifecycle

Preserve the approved lifecycle model such as:

- ACTIVE
- INACTIVE
- RETIRED

Do not model inactive/retired as row deletion.

Only eligible ACTIVE account/scope combinations may create new Q-008 associations according to the approved contract.

## 3.5 Database Authority

MySQL / repository-approved relational persistence is authoritative.

Redis/Kafka are not identity authority.

## 3.6 Authorization

Reuse Q-009 `ActorContext` / capability authorization.

Do not invent a parallel authorization framework.

Authorization order must prevent account existence probing where required.

## 3.7 Provisioning Boundary

Provisioning is controlled and non-public-Web.

Do not design a public/admin REST CRUD API for registry ownership.

## 3.8 Q-008 Consumer Boundary

Q-008 must only consume a bounded read-only eligibility/authority contract.

Do not expose:

- MT login
- ExternalAccountKey
- SourceNamespace
- internal DB IDs
- CRM customer data
- vendor DTOs
- persistence entities

## 3.9 Immutable History

Registry mutations and required immutable history evidence must be transactionally consistent where the approved architecture requires it.

Do not weaken rollback semantics.

---

# 4. Required Implementation Design Sections

Create or update the repository-standard Q-010 Implementation Design document.

The document must include at least the following sections.

## 4.1 Scope and Non-Goals

Clearly state this design covers Q-010 identity registry/provisioning/authority resolution only.

Explicitly exclude:

- Q-008 implementation
- MT4/MT5 adapter implementation beyond the bounded source identity input contract
- CRM synchronization implementation
- public account-management APIs
- Redis authoritative registry
- Kafka authoritative registry
- generic IAM redesign
- customer master-data ownership
- account trading-state execution controls unless already required by Q-010

## 4.2 Module and Package Placement

Define exact recommended package/module placement based on repository architecture.

For every proposed package/component, specify its layer:

- domain
- application
- interfaces/input
- infrastructure/persistence
- infrastructure/tooling or controlled provisioning boundary
- shared kernel only if repository convention justifies it

Avoid architecture leakage.

Do not put persistence annotations into domain objects unless that is explicitly the repository standard.

## 4.3 Domain Types

Design concrete domain types, including as applicable:

- `TradingAccountRef`
- `AccountAuthorityScopeRef`
- `SourceNamespace`
- `ExternalAccountKey`
- account registry aggregate/entity
- authority scope aggregate/entity
- lifecycle/status enums
- eligibility decision/value type
- provisioning manifest identity
- attestation/reference type
- immutable history action/type
- version/revision field if required for optimistic concurrency

For each type define:

- responsibility
- invariants
- construction/validation rules
- equality semantics
- serialization boundaries
- whether exposed outside Q-010

Avoid primitive obsession where repository conventions support value objects.

## 4.4 Application Use Cases

Design explicit use cases / application services for the approved behaviors.

At minimum consider:

- register/provision trading account identity
- resolve TradingAccountRef from external identity
- resolve bounded Q-008 eligibility/authority view by TradingAccountRef
- deactivate account
- retire account
- activate/deactivate authority scope if architecture requires it
- controlled lookup for provisioning/idempotency
- historical resolution
- conflict detection

For each use case specify:

- input command/query
- required ActorContext/capability
- validation order
- authorization order
- transaction boundary
- output type
- failure cases
- side effects/history writes

Do not create unnecessary CRUD operations.

## 4.5 Ports / Interfaces

Define implementation-facing interfaces/ports.

Examples may include:

- registry repository
- authority scope repository
- immutable history repository
- unit-of-work/transaction abstraction only if repository architecture uses one
- provisioning manifest loader/parser
- attestation verifier
- clock/ID generator only if testability/repository conventions require them
- Q-008 read-only consumer facade

For each port, define method intent and what domain/application layer is allowed to see.

Do not expose JPA/MyBatis/vendor DTOs across boundaries.

## 4.6 Persistence Model

Design the concrete relational model in detail.

For each proposed table specify:

- table name
- purpose
- columns
- SQL type
- nullable/non-null
- defaults
- primary key
- business unique keys
- indexes
- foreign keys if repository conventions use them
- lifecycle/status representation
- created/updated timestamps
- immutable history schema
- version column if needed

The design must make the one-to-one identity constraints enforceable at database level.

At minimum, explain how you guarantee both:

1. uniqueness of (`authority_scope_ref`, `source_namespace`, `external_account_key`)
2. uniqueness of `trading_account_ref`

If authority scope is normalized to a separate table, define its uniqueness and relationship.

If hashes/canonical forms are needed because of external key length/collation concerns, justify them carefully.

Do not actually create the migration in V5.

## 4.7 Canonicalization and Validation

Define exact rules for:

- SourceNamespace normalization
- ExternalAccountKey trimming/case sensitivity
- authority scope format
- TradingAccountRef format
- maximum lengths
- allowed character sets
- Unicode handling
- empty/blank rejection
- case-sensitive versus case-insensitive uniqueness
- MySQL collation implications

This section is mandatory because identity correctness depends on canonical comparison semantics.

Do not defer these decisions to implementation unless the approved Requirement explicitly requires source-specific case sensitivity that must remain configurable.

## 4.8 Concurrency and Idempotency

Provide a concrete design for race conditions.

Cover at least:

- two concurrent attempts to provision the same external identity
- two concurrent attempts using the same TradingAccountRef
- retry after timeout when the first transaction may already have committed
- duplicate manifest delivery
- identical idempotency key with identical payload
- identical idempotency key with conflicting payload
- concurrent lifecycle state transition
- history insert failure
- deadlock/retry behavior if relevant

Describe which outcome is returned in each case.

The database must be the final arbiter of uniqueness.

Avoid “SELECT then INSERT” as the only protection.

## 4.9 Transaction Design

Define exact transaction boundaries for:

- first-time provisioning
- idempotent replay
- lifecycle transition
- scope lifecycle transition
- history recording

Specify what must commit atomically.

Explain rollback behavior.

Do not require distributed transactions.

## 4.10 Provisioning Manifest

Define the manifest contract in implementation-ready form.

Include:

- schema/version
- request/provisioning ID
- idempotency key
- authority scope reference
- source namespace
- external account key
- requested TradingAccountRef behavior
- actor/operator identity reference
- attestation/evidence reference
- requested action
- timestamp rules
- optional metadata rules
- canonical payload fingerprint if used

Define:

- validation order
- unsupported schema behavior
- duplicate behavior
- conflicting replay behavior
- audit/history evidence
- output report

The manifest mechanism must remain controlled/non-public-Web.

## 4.11 Controlled Execution Boundary

Define how provisioning is invoked without creating a public REST API.

Use repository-compatible options such as:

- CLI/application runner
- controlled script calling an application boundary
- offline command
- internal operator job

Pick the design that best matches repository conventions.

Specify:

- allowed execution context
- authorization/attestation requirements
- input file handling
- secrets policy
- output/report handling
- failure exit codes

Do not implement the tool yet.

## 4.12 Q-009 Authorization Integration

Define exactly which capabilities/authorization checks apply to each Q-010 use case.

If new Q-010-specific capabilities are required, design them in a way compatible with Q-009 rather than redesigning Q-009.

Specify authorization-before-lookup behavior for sensitive operations.

Document how unauthorized vs not-found behavior avoids existence probing.

## 4.13 Q-008 Read-Only Contract

Design one narrow consumer contract for Q-008.

The contract should expose only what Q-008 needs to determine whether a `TradingAccountRef` is currently eligible/authoritative for new Risk Case association.

Define:

- input
- output fields
- status/decision enum
- semantics
- authorization expectations
- not-found behavior
- inactive behavior
- retired behavior
- inactive scope behavior

Explicitly list forbidden fields that must never cross this boundary.

Do not let Q-008 query Q-010 tables directly.

## 4.14 Error / Result Model

Map all important failure conditions to repository-standard ResultCode / exception patterns.

At minimum include:

- invalid TradingAccountRef
- invalid authority scope ref
- invalid namespace
- invalid external key
- duplicate exact replay
- duplicate conflicting replay
- external identity already mapped
- TradingAccountRef already mapped
- authority scope not found
- authority scope inactive
- account inactive
- account retired
- unauthorized
- forbidden
- not found
- illegal lifecycle transition
- unsupported manifest schema
- attestation failure
- persistence conflict
- history persistence failure
- internal consistency violation

Avoid leaking sensitive existence information.

## 4.15 Logging and Sensitive Data

Define what may and may not be logged.

Raw external identifiers may be sensitive.

Specify masking/hash/reference strategy for:

- ExternalAccountKey
- TradingAccountRef
- authority scope
- manifest ID
- idempotency key
- actor identity
- conflict diagnostics

Reuse existing Q-005 tracing/request correlation conventions where applicable.

Do not log full manifest bodies if they contain sensitive identity data.

## 4.16 Test Design

Provide a comprehensive test matrix.

Separate:

- value-object/domain unit tests
- application service tests
- authorization tests
- persistence integration tests
- transaction rollback tests
- concurrency tests
- manifest parser/validation tests
- idempotency tests
- lifecycle tests
- Q-008 consumer contract tests
- security/non-enumeration tests

For concurrency tests, define how to prove the DB constraint is authoritative.

For rollback tests, prove no state mutation remains when history persistence fails.

## 4.17 Flyway Plan

Design the future migration sequence without creating migration files.

Specify intended migrations such as:

- authority scope table
- trading account registry table
- provisioning/idempotency table if needed
- immutable history table
- constraints/indexes

Use repository naming/version conventions conceptually.

Call out MySQL version compatibility based on the repository actual configuration.

## 4.18 Observability

Define minimal metrics/logging useful for identity registry health, such as:

- provisioning success/failure counts
- idempotent replay counts
- conflict counts
- authorization denial counts
- lifecycle transition counts
- persistence conflict/deadlock counts

Do not introduce a new observability platform.

## 4.19 Security Review Checklist

Include a concise security design review covering:

- enumeration resistance
- privilege boundaries
- manifest tampering
- replay attacks
- operator attribution
- sensitive identifier leakage
- unauthorized remapping
- destructive deletion prevention
- direct DB bypass risk
- audit/history tampering

## 4.20 Implementation Sequence

Provide a recommended future implementation order, for example:

1. value objects/domain model
2. persistence schema/migrations
3. repositories
4. application services
5. authorization integration
6. controlled provisioning boundary
7. Q-008 read-only facade
8. history/audit completion
9. integration/concurrency/security tests
10. documentation verification

This is a design sequence only.

Do not execute it in V5.

---

# 5. Design Decision Discipline

If you identify a design question that would materially change an approved ADR decision, stop and classify it as an architecture gap.

Do not silently decide architecture-level changes inside Implementation Design.

Examples:

- changing identity ownership
- changing the external identity tuple
- allowing remapping
- changing the authoritative store
- adding public CRUD APIs
- changing Q-008 visibility boundary
- introducing distributed consensus
- using Redis/Kafka as authority

If such a gap exists, mark V5 as BLOCKED and document the exact required architecture re-review.

For ordinary implementation choices consistent with the approved architecture, make the decision in the design and justify it.

---

# 6. Implementation Design Status

Create/update the Q-010 Implementation Design artifact with status:

`DRAFT — awaiting Architect approval`

or the exact repository-equivalent wording.

Do not mark it APPROVED.

Do not set Implementation Allowed to YES.

Expected gate at the end of V5:

- Requirement: APPROVED
- Architecture: APPROVED
- ADR-012: ACCEPTED
- Implementation Design: DRAFT / AWAITING ARCHITECT APPROVAL
- Implementation: NOT STARTED
- Implementation Allowed: NO

---

# 7. Verification

Run all documentation/static checks appropriate to a design-only change.

Do not run implementation/runtime tests that require code which does not yet exist.

Record:

- branch
- HEAD
- git status
- diff stat
- static/doc verification
- known pre-existing failures
- whether V5 introduced any new verification failure

The historical Q-009 whitespace issue must remain classified as pre-existing if unchanged.

Do not modify unrelated historical artifacts solely to make verification green.

---

# 8. Review Package

Create a new review directory without overwriting previous Q-010 reviews.

Preferred semantic label:

`q-010-v5-implementation-design`

Include at minimum, following repository convention:

- `Summary.md`
- `ArchitectureReview.md` or `DesignReview.md` as appropriate
- `Verification.md`
- `OutstandingItems.md`
- `LessonsLearned.md`
- `PhaseReviewIndex.md`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `ProjectTree.txt`
- copy/reference of the Implementation Design artifact as repository convention requires

The review must explicitly say:

- this is Implementation Design only
- no production code was added
- no Flyway migration was added
- no runtime schema was changed
- no REST API was added
- Q-008 implementation did not start
- Git commit/push were not performed
- Architect approval is still required before implementation

---

# 9. Review ZIP

Create a new ZIP archive.

Preferred name:

`review-q-010-v5-implementation-design-<timestamp>.zip`

Verify integrity and content.

Do not overwrite older ZIPs.

---

# 10. Git Rules

Do not commit.
Do not push.

Do not stage files unless strictly required by repository tooling.

Preserve unrelated pre-existing changes.

---

# 11. Acceptance Criteria

V5 is complete only if:

1. An implementation-ready Q-010 design document exists.
2. It preserves Requirement + Architecture + ADR-012 exactly.
3. Package/module responsibilities are explicit.
4. Domain types and invariants are explicit.
5. Application use cases are explicit.
6. Ports/interfaces are explicit.
7. Persistence tables/columns/types/constraints/indexes are explicit.
8. Canonicalization/collation/case rules are explicit.
9. Database-level uniqueness strategy is explicit.
10. Concurrency race handling is explicit.
11. Idempotency semantics are explicit.
12. Transaction and rollback semantics are explicit.
13. Provisioning manifest contract is explicit.
14. Controlled non-Web execution boundary is explicit.
15. Q-009 authorization integration is explicit.
16. Q-008 read-only contract is explicit and narrow.
17. ResultCode/exception mapping is explicit.
18. Sensitive logging rules are explicit.
19. Test matrix is comprehensive.
20. Future Flyway plan is explicit but no migration exists yet.
21. Implementation Design is not self-approved.
22. Implementation remains prohibited.
23. A new V5 review directory exists.
24. A new V5 review ZIP exists.
25. No Git commit or push occurred.

---

# 12. Final Codex Response Format

When finished, respond with:

1. `Q-010 V5 Result`
2. Requirement status
3. Architecture status
4. ADR-012 status
5. Implementation Design status
6. Implementation status
7. main design artifacts created/updated
8. verification result
9. architecture gaps, if any
10. outstanding items
11. exact review directory path
12. exact review ZIP path
13. Git status summary
14. explicit statement:

`Ready for Architect Design Review: YES`

Do not say:

`Ready for Implementation: YES`

Do not say:

`Ready for Git Commit: YES`

The next action is an independent Architect review of the Q-010 V5 Implementation Design.
