# Q-010 V5 Implementation Design Review

## Decision

- Implementation Design Complete for submission: **YES**
- Ready for external Architect Design Review: **YES**
- Architect Design Approval recorded: **NO**
- Implementation Allowed: **NO**

The design is concrete enough for an independent Architect decision. It is not
self-approved and no future component described below exists yet.

## Artifact

Authoritative draft:
`docs/architecture/q-010-trading-account-reference-authority-implementation-design.md`

Status: `DRAFT — AWAITING EXTERNAL ARCHITECT APPROVAL`.

## Completeness Review

| Required area | Result | Concrete evidence |
| --- | --- | --- |
| Scope/non-goals | COMPLETE | Section 1 isolates Q-010 and excludes Q-008 implementation, source adapters, public CRUD, master data, Kafka/Redis authority, and IAM redesign |
| Package placement | COMPLETE | Section 3 assigns domain, application, ports, JDBC/configuration, and non-Web input packages in the existing deployable |
| Domain types | COMPLETE | Section 4 defines refs, namespace/key equality, scope/account state, lifecycle, operation, provenance, and eligibility values |
| Use cases | COMPLETE | Section 6 defines registrations, both lifecycle families, exact resolution, Q-008 validation, ordering, outputs, failures, and history |
| Ports | COMPLETE | Section 7 defines bounded query/mutation/generator ports and keeps JDBC/history ownership inside one adapter |
| Persistence | COMPLETE | Section 8 defines four tables column-by-column with types, constraints, indexes, FKs, collations, versions, and timestamps |
| Canonicalization | COMPLETE | Section 9 fixes ASCII namespace formats and exact UTF-8/VARBINARY external-key semantics |
| Manifest | COMPLETE | Section 10 fixes schema V1, one operation ID/idempotency key, field matrix, strict parse, fingerprint inputs, and prohibited caller authority/time/ref fields |
| Execution boundary | COMPLETE | Section 11 selects a one-file, one-operation, non-Web command and exact safe exit categories |
| Q-009 authorization | COMPLETE | Section 5 maps three exact capabilities and one registered SERVICE descriptor; checks precede Q-010 access |
| Q-008 consumer | COMPLETE | Section 14 defines one eligibility query and three bounded decisions with explicit forbidden disclosure |
| Transactions/idempotency | COMPLETE | Sections 12–13 define atomic state/outcome/history, exact replay, conflicts, unique races, CAS, rollback, and uncertain commit |
| Error model | COMPLETE | Section 15 maps validation, replay, mapping, lifecycle, authorization, unavailable, and history failures to stable outcomes |
| Logging/metrics | COMPLETE | Section 16 defines allowed/forbidden values and low-cardinality existing-Micrometer metrics |
| Security | COMPLETE | Section 17 covers enumeration, privilege, tampering, replay, attribution, leakage, remap/delete, DB/history, and command exposure |
| Tests | COMPLETE | Section 18 separates domain, services, Q-009, command, MySQL, transaction/concurrency, Q-008, regression, and architecture tests |
| Flyway/rollout | COMPLETE | Section 19 names future V3, table order, prohibitions, MySQL 8.4.11 proof, rollout, and non-destructive rollback |
| Implementation sequence | COMPLETE | Section 20 orders ten future implementation steps and explicitly withholds authorization |
| Traceability | COMPLETE | Section 21 maps all 12 Q010-FR IDs and Acceptance Criteria to planned verification |
| Architecture gaps | COMPLETE | Section 22 records none and distinguishes deployment inputs/future Requirements |

## Key Implementation Choices Within Approved Architecture

- `ta-<lowercase UUIDv4>` and `aas-<lowercase UUIDv4>` refs are generated only
  by Q-010.
- External keys are validated Strings encoded to exact UTF-8 and stored in
  `VARBINARY(512)`; they are never normalized, logged, or exposed to Q-008.
- One operation UUID is the request/provisioning/idempotency identity.
- The fingerprint is SHA-256 over fixed typed length-prefixed fields, not raw
  JSON bytes.
- A high-level JDBC mutation port owns the complete `TransactionTemplate`
  transaction so current state, outcome, and history cannot diverge.
- Database unique constraints and lifecycle compare-and-set updates are the
  final concurrency arbiters; no distributed lock or general automatic retry
  is introduced.
- Q-008 receives only `TradingAccountRef`, a three-value decision, and optional
  opaque snapshot/provenance references.

## Review Limitation

This is documentation/static design review only. No runtime class, schema,
command, mapping, or test exists. All executable claims must be proven after a
separate Architect approval and explicit implementation authorization.
