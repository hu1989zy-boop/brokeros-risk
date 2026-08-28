# Q-010 V7 — Implementation Prompt

## Role

You are Codex working inside the BrokerOS Risk repository.

This task is **Q-010 V7 — Implementation**.

The complete design chain has passed independent Architect review:

- Q-010 Requirement: APPROVED
- Q-010 Architecture: APPROVED
- ADR-012: ACCEPTED
- Q-010 Implementation Design V1: APPROVED — External Architect — 2026-08-27
- Q-010 V6 Design Approval Recording: ACCEPTED

For the first time in Q-010, implementation is now authorized **only within the exact boundaries of the approved artifacts**.

Your task is to implement Q-010 faithfully, verify it rigorously, and produce a complete V7 Implementation Review Package.

Do not redesign the architecture while implementing.
Do not implement Q-008 business functionality.
Do not commit or push.

---

# 1. Mandatory Preflight

Before modifying production/runtime files:

1. Read and obey root `AGENTS.md`.
2. Inspect:
   - current branch
   - HEAD
   - `git status`
   - `git diff`
   - existing untracked Q-010 artifacts
3. Read the authoritative:
   - Q-010 Requirement
   - Q-010 Architecture
   - ADR-012
   - Q-010 approved Implementation Design
   - Q-010 V6 approval evidence
4. Read relevant existing implementation patterns for:
   - package/layer structure
   - domain value objects
   - application ports/services
   - persistence
   - Flyway
   - ResultCode/exceptions
   - Q-009 ActorContext / AuthorizationGuard / capabilities
   - Q-005 request/trace/logging conventions
   - testing
5. Inspect the actual MySQL version/configuration and current Flyway migration sequence.
6. Establish the baseline test/static-verification result before implementation where practical.
7. Preserve unrelated pre-existing user changes.
8. Do not “clean up” unrelated historical files.

If the approved design cannot be implemented without materially changing Requirement/Architecture/ADR semantics, STOP and report an architecture/design blocker. Do not silently improvise.

---

# 2. Source of Truth Priority

When details conflict, use this priority:

1. approved Q-010 Requirement
2. accepted ADR-012
3. approved Q-010 Architecture
4. approved Q-010 Implementation Design
5. repository-wide approved conventions / AGENTS.md
6. this prompt

Do not use this prompt to override an approved artifact.

---

# 3. Implementation Scope

Implement the approved Q-010 Trading Account Reference Authority capability.

The implementation should include all components required by the approved design, including where specified:

- domain/value types
- lifecycle model
- application commands/queries/services
- ports
- persistence adapters
- MySQL schema via Flyway
- database uniqueness constraints/indexes
- immutable history
- provisioning/idempotency persistence
- controlled non-Web provisioning boundary
- Q-009 authorization integration
- narrow Q-008-facing read-only eligibility/authority facade
- result/error mapping
- safe logging
- metrics/observability hooks using existing project conventions
- automated tests

Implement the approved design, not a reduced “MVP” that skips difficult guarantees.

---

# 4. Identity Guarantees — Non-Negotiable

## 4.1 TradingAccountRef

Implement `TradingAccountRef` as the approved BrokerOS-owned opaque identity.

It MUST NOT be derived from:

- MT4 login
- MT5 login
- CRM ID
- external account number
- DB primary key
- vendor identifier

Use the exact approved validation/format semantics.

## 4.2 External Identity

Implement the authoritative external identity tuple exactly as approved:

- `AccountAuthorityScopeRef`
- `SourceNamespace`
- `ExternalAccountKey`

Preserve approved canonicalization, byte/case/collation semantics.

Do not simplify the tuple.

## 4.3 One-to-One Mapping

Enforce at database level:

- external identity tuple -> one TradingAccountRef
- TradingAccountRef -> one external identity tuple

No silent remapping.
No destructive identity deletion.
No ordinary CRUD operation may repurpose an identity.

Application checks may improve errors, but DB constraints remain the final concurrency authority.

---

# 5. Flyway / Persistence Implementation

Create the approved Flyway migration(s) using the actual next repository migration version.

Do not blindly use `V3` if repository state has moved; inspect first.

Implement the approved tables for, as applicable:

- authority scope
- trading account registry
- provisioning/idempotency operation
- immutable history

Follow the approved Implementation Design exactly for:

- table names
- column names/types
- `VARBINARY` or equivalent exact external-key storage
- PKs
- unique constraints
- indexes
- status representation
- timestamps
- version/CAS field
- foreign keys where approved

Verify migration compatibility with the repository's actual MySQL target.

Do not add speculative schema.

---

# 6. Domain Implementation

Implement the approved domain/value objects and invariants.

Requirements:

- reject invalid/blank identities at the correct boundary
- preserve exact equality semantics
- avoid primitive/string leakage where design specifies value objects
- lifecycle transitions must be explicit
- illegal transitions must fail deterministically
- domain objects must not depend on vendor/CRM/MT DTOs
- persistence concerns must not leak into domain unless explicitly approved by repository convention

Add focused unit tests for every invariant and transition.

---

# 7. Application Layer

Implement the approved use cases, including those required for:

- first-time provisioning
- idempotent provisioning replay
- conflict detection
- external identity resolution
- TradingAccountRef resolution
- lifecycle transition
- authority scope lifecycle handling
- historical resolution
- bounded Q-008 eligibility/authority query

For each operation preserve the approved:

- validation order
- authorization order
- transaction boundary
- result/error semantics
- history behavior

Do not create generic CRUD services as a shortcut.

---

# 8. Q-009 Authorization Integration

Reuse the actual Q-009 implementation.

Do not create a second authorization framework.

Implement the approved capabilities/checks.

For sensitive resolution/provisioning operations:

**authorization must happen before repository lookup where the design requires it.**

Tests must prove unauthorized callers cannot distinguish:

- existing account
- nonexistent account
- inactive account
- conflicting external identity

through lookup timing/result semantics beyond what the approved contract permits.

Do not leak existence through error messages.

---

# 9. Concurrency and Database Arbitration

Implement and test the approved concurrency strategy.

At minimum verify:

### Same external identity, concurrent first provisioning

Only one mapping may be created.

The other operation must resolve deterministically to the approved replay/conflict result.

### Same TradingAccountRef, different external identity

The DB uniqueness constraint must reject the second mapping.

### Different TradingAccountRef, same external identity

The DB uniqueness constraint must reject the second mapping.

### Concurrent lifecycle transition

Use the approved version/status CAS strategy.

No lost updates.

### Database Constraint as Final Arbiter

Tests must prove correctness does not depend only on:

`SELECT -> if absent -> INSERT`

Race safety must survive concurrent transactions.

Do not add Redis/distributed locks unless explicitly approved—which Q-010 currently does not require.

---

# 10. Idempotency

Implement the approved durable idempotency semantics.

Preserve:

- operation/request identity
- idempotency key
- semantic/canonical payload fingerprint where approved
- successful result replay
- conflicting replay rejection
- durable operation outcome
- atomic relationship to registry/history state

Test:

1. same idempotency key + same semantic payload
2. same idempotency key + different payload
3. retry after simulated timeout/unknown commit outcome
4. duplicate manifest delivery
5. concurrent duplicate delivery

Do not treat an idempotency key as a best-effort in-memory cache.

---

# 11. Transaction Atomicity

Implement the approved local transaction boundaries.

Where required, these must commit atomically:

- registry mutation
- lifecycle mutation
- durable provisioning/idempotency outcome
- immutable history

If required history persistence fails, the business mutation must rollback.

Add integration tests proving rollback.

Do not introduce distributed transactions.

---

# 12. Immutable History

Implement append-only historical evidence according to the approved design.

History must capture the approved identifiers/action/status/provenance fields without leaking prohibited sensitive data.

No update/delete API should exist for immutable history through normal application behavior.

Tests must demonstrate historical resolution remains possible after INACTIVE/RETIRED where approved.

---

# 13. Controlled Provisioning Boundary

Implement the approved non-public-Web provisioning mechanism.

This may be a repository-approved:

- CLI
- application runner
- controlled offline command
- internal operator entry point

Use exactly the mechanism selected by the approved Design.

Requirements include:

- manifest schema/version validation
- controlled file/input handling
- attestation/evidence validation
- actor/operator attribution
- authorization
- idempotency
- deterministic exit/result reporting
- safe logging

Do NOT expose provisioning through:

- public REST
- admin REST CRUD
- generic account-management HTTP endpoints

Do not print secrets or raw sensitive identity payloads.

---

# 14. Q-008 Bounded Read-Only Facade

Implement only the Q-010-owned read-only contract that future Q-008 implementation is allowed to call.

This is **not permission to implement Q-008 itself**.

The facade must expose only the approved bounded eligibility/authority evidence.

It must NOT expose:

- MT4/MT5 login
- ExternalAccountKey
- SourceNamespace
- internal persistence IDs
- CRM customer data
- vendor DTOs
- persistence entities

Q-008 must not query Q-010 tables directly.

Add contract tests for:

- active + eligible
- inactive account
- retired account
- inactive authority scope
- not found
- unauthorized
- forbidden-field/non-disclosure boundary

---

# 15. Error and Result Handling

Implement the approved repository-standard ResultCode/exception mapping.

Cover the approved failure classes, including:

- invalid refs
- invalid namespace/key
- duplicate exact replay
- conflicting replay
- already-mapped external identity
- already-mapped TradingAccountRef
- inactive/not-found scope
- inactive/retired account
- unauthorized/forbidden
- illegal transition
- unsupported manifest schema
- attestation failure
- persistence uniqueness conflict
- optimistic concurrency conflict
- history persistence failure
- internal consistency violation

Do not expose raw SQL exceptions or sensitive identity values to callers.

---

# 16. Sensitive Logging

Follow the approved logging rules.

Never log full raw manifests when sensitive identity data is present.

Apply approved masking/hash/reference behavior to:

- ExternalAccountKey
- TradingAccountRef where required
- authority scope
- idempotency key
- actor identity
- conflict diagnostics

Reuse existing Request ID / Trace ID / MDC conventions where applicable.

Tests or review evidence should demonstrate prohibited values are not emitted by designed logging paths.

---

# 17. Observability

Implement only the minimal metrics/logging hooks approved by the design and compatible with existing project infrastructure.

Examples, if approved:

- provisioning success/failure
- idempotent replay
- identity conflict
- authorization denial
- lifecycle transition
- DB conflict/deadlock

Do not introduce Prometheus/Grafana/ELK/exporter infrastructure if it is not already part of the approved project baseline.

---

# 18. Automated Test Requirements

Implement the approved test matrix.

At minimum include meaningful coverage for:

## Unit

- value object validation
- canonicalization
- equality
- lifecycle transitions
- fingerprint/idempotency semantics
- eligibility decision

## Application

- happy path provisioning
- exact replay
- conflicting replay
- authorization ordering
- not-found/non-enumeration behavior
- lifecycle commands
- Q-008 facade

## Persistence Integration

- Flyway migration
- exact external-key storage
- unique external tuple
- unique TradingAccountRef
- history persistence
- idempotency persistence
- CAS/version behavior

## Transaction

- history failure rolls back mutation
- operation outcome failure rolls back mutation where required

## Concurrency

Use real concurrent execution against the integration database where repository infrastructure permits.

Prove:

- same identity race creates one mapping
- competing TradingAccountRef race cannot split mapping
- lifecycle CAS prevents lost update

## Security

- unauthorized lookup does not enumerate identity existence
- prohibited fields never cross Q-008 facade
- raw external identity is not leaked through public errors/logging

Avoid tests that merely mock away the database constraints being tested.

---

# 19. Verification Gates

Run all applicable repository verification.

At minimum, where available:

- Maven tests
- focused Q-010 tests
- Flyway migration/integration verification
- static verification
- architecture/package-boundary checks
- Docker/MySQL integration checks if required by repository gate
- any repository-defined verification script

Record exact commands and results.

If Docker or another runtime dependency is required and unavailable, do not fake PASS. Record the exact blocker.

The historical Q-009 whitespace issue is known pre-existing context. If unchanged, classify it separately and do not “fix” it during Q-010.

No new static failure may be hidden as pre-existing.

---

# 20. Implementation Design Traceability

Create explicit traceability from implementation to approved design.

The review evidence should map major implementation components/tests to the relevant Implementation Design sections.

This should make it possible to answer:

- Was every approved MUST implemented?
- Was anything added that was not approved?
- Was anything silently omitted?

Any intentional deviation must be listed prominently.

A material architecture/design deviation means:

**Ready for Architect Review: NO**

until resolved.

---

# 21. No Scope Creep

Do not implement:

- Q-008 Risk Case functionality
- MT4/MT5 Manager API integration unrelated to the approved identity input boundary
- CRM synchronization
- customer master data
- generic account CRUD
- public identity APIs
- Redis identity cache as authority
- Kafka identity authority
- Flink
- Python/ML
- unrelated refactors
- deployment expansion unrelated to Q-010

---

# 22. Documentation Updates

After implementation, update only Q-010/repository governance documentation required to accurately describe implemented reality.

Do not mark Final Closure.

Expected end-of-V7 state before external review:

- Requirement: APPROVED
- Architecture: APPROVED
- ADR-012: ACCEPTED
- Implementation Design: APPROVED
- Implementation: IMPLEMENTED — AWAITING ARCHITECT REVIEW
- Ready for Git Commit: NO

Codex must not self-approve Implementation.

---

# 23. V7 Review Package

Create a NEW review directory without overwriting V1–V6.

Preferred semantic label:

`q-010-v7-implementation`

Include repository-standard evidence and at least:

- `Summary.md`
- `ImplementationReview.md`
- `ArchitectureReview.md` or conformance review
- `Verification.md`
- `RequirementTraceability.md`
- `DesignTraceability.md`
- `SecurityReview.md`
- `PersistenceReview.md`
- `ConcurrencyReview.md`
- `OutstandingItems.md`
- `LessonsLearned.md`
- `PhaseReviewIndex.md`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `ProjectTree.txt`

Also include self-contained evidence sufficient to review the implementation outside the live working tree where practical.

At minimum include:

- authoritative artifact hashes
- relevant implementation file inventory
- migration snapshot
- test inventory/results
- key configuration evidence

Do not package secrets, credentials, `.env` contents, tokens, private keys, database dumps, or sensitive production data.

---

# 24. Review ZIP

Create:

`review-q-010-v7-implementation-<timestamp>.zip`

Requirements:

- do not overwrite older review ZIPs
- verify ZIP integrity
- verify expected files exist
- ensure no secrets are packaged
- record SHA-256 of the ZIP if repository conventions permit

---

# 25. Git Rules

**DO NOT COMMIT.**
**DO NOT PUSH.**

Do not stage files unless repository tooling strictly requires it.

Preserve unrelated pre-existing modifications/untracked files.

Report final Git status accurately.

The external Architect/user decides when commit is permitted.

---

# 26. Stop Conditions

Stop implementation and report BLOCKED if:

1. approved artifacts contradict each other materially;
2. implementation requires changing ADR-012;
3. implementation requires changing identity ownership/tuple semantics;
4. implementation requires a public provisioning API;
5. required Q-009 authorization capability cannot be integrated without redesign;
6. DB target cannot enforce approved identity semantics;
7. a material security flaw is discovered in the approved design;
8. implementation would require starting Q-008 business implementation;
9. a required verification gate cannot be honestly executed and repository policy makes it mandatory.

Do not work around an architectural blocker silently.

---

# 27. Acceptance Criteria

V7 is complete only if:

1. Approved Q-010 design is fully implemented.
2. TradingAccountRef remains BrokerOS-owned and opaque.
3. External identity tuple semantics are preserved.
4. DB-level bidirectional uniqueness is implemented.
5. Exact approved canonicalization/storage semantics are implemented.
6. No remapping/destructive identity deletion path exists.
7. Lifecycle semantics are implemented.
8. Durable idempotency is implemented.
9. Concurrent provisioning is safe.
10. CAS/concurrency lifecycle behavior is safe.
11. Registry/outcome/history atomicity is implemented.
12. History failure rollback is verified.
13. Controlled non-Web provisioning is implemented.
14. Q-009 authorization is reused.
15. Authorization-before-sensitive-lookup is verified.
16. Q-008 bounded read-only facade is implemented without Q-008 business implementation.
17. Sensitive identifiers are not improperly exposed.
18. Flyway migration is implemented and verified.
19. Unit/application/persistence/transaction/concurrency/security tests pass.
20. Approved design traceability is complete.
21. No material architecture/design deviation exists.
22. No unrelated scope creep occurred.
23. V7 review directory exists.
24. V7 ZIP exists and passes integrity verification.
25. No Git commit or push occurred.

---

# 28. Final Codex Response Format

Return a concise but complete report:

1. `Q-010 V7 Result`
2. Requirement status
3. Architecture status
4. ADR-012 status
5. Implementation Design status
6. Implementation status
7. major implementation components
8. Flyway migration created
9. tests added
10. verification commands/results
11. concurrency verification result
12. security/non-enumeration verification result
13. architecture/design deviations, if any
14. outstanding items
15. exact V7 review directory path
16. exact V7 review ZIP path
17. final Git status summary
18. explicit:

`Ready for Architect Implementation Review: YES`

Only say YES if all required gates are honestly satisfied.

Do NOT say:

`Ready for Git Commit: YES`

Do NOT commit or push.

The next action is independent Architect review of the Q-010 V7 implementation package.
