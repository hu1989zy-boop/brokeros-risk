# Q-014 Architecture Review

## Gate Decision

**BLOCKED.** The implementation conforms to the approved Q-014 architecture and ADR-016 within the inspected scope, but the lifecycle gate cannot pass while the mandatory full regression suite has three failures.

## Architecture assessment

The new code is contained in `com.brokeros.risk.actionoutcome` and preserves the Phase 1 modular-monolith boundary. Domain and application packages do not depend on Spring, JDBC, persistence, or vendor APIs. Q-009 authorization and Q-013 action recognition are consumed through existing contracts; Q-014 owns no cross-module database foreign key and does not reach into Q-013 persistence.

The model is an immutable provenance fact, not an execution model: there is no status, correction/delete use case, external execution adapter, attempt/retry lifecycle, structured outcome taxonomy, or Q-008 consumer. The database permits multiple outcomes for the same action while each outcome references exactly one action. This matches the approved three Product Owner decisions.

The recording flow follows the design's authoritative order: Q-009 authorization, HUMAN restriction, raw semantic fingerprint, operation parsing/replay, content validation, Q-013 action recognition using the same actor context, authorized mutation construction, and an atomic record/operation write. Exact replay returns the original without another Q-013 call; changed replay conflicts.

The narrow provenance view structurally omits `outcomeText`. Full detail requires `action-outcome:read`; the access log uses an independent committed transaction and content is returned only after the audit succeeds. SERVICE actors are permitted for both read forms when authorized.

## Development Standards Compliance

### AGENTS.md compliance

Inspected the new action-outcome module, V7, Q-014 tests, shared result-code addition, static verification change, reusable standards note, and Lessons Learned entry. The change is the smallest coherent Q-014 implementation, stays broker/platform neutral, keeps controllers limited to validation/translation/orchestration, separates DTOs/domain/persistence, and does not introduce a dumping-ground package. No Q-009–Q-013 source/test or V1–V6 migration was modified. The mandatory final gate was run and honestly recorded as blocked.

### Architecture compliance

Inspected package dependencies and the architecture test suite. The module remains inside the approved modular monolith, hides JDBC behind ports, and consumes Q-009/Q-013 at application boundaries. No direct external-system database access, microservice split, Kafka topic, Redis key, Python/Flink component, or vendor binding was added. `ActionOutcomeArchitectureTests` enforces framework/vendor isolation, narrow-view confidentiality, immutability/no taxonomy, forbidden mutation/execution/logging behavior, and the Q-008 boundary.

### ADR compliance

Inspected ADR-016 and implementation surfaces. The code uses a distinct append-only ActionOutcome provenance module, exactly one recognized Q-013 action per outcome, many outcomes per action, free-text manual outcome, capability-based authorization, narrow/full read separation, and three owned tables without cross-module foreign keys. No decision conflicts with accepted ADR-016 were found.

### API standard compliance

Inspected `ActionOutcomeController`, request/response DTOs, validation, and `ResultCode`. Only POST `/api/action-outcomes` and GET `/api/action-outcomes/{actionOutcomeRef}` are exposed. The controller returns `ApiResponse`, request validation uses Jakarta Bean Validation/`@Valid`, entities are not exposed, and the eight new result codes are confined to the exact `ACTION_OUTCOME_*` namespace. Application failures use typed business exceptions handled through the existing global mechanism.

### Database standard compliance

Inspected V7 and MySQL migration/persistence tests. V7 is new and versioned; V1–V6 are untouched. It adds exactly `action_outcome_record`, `action_outcome_operation`, and `action_outcome_access_log`, with `BIGINT id`, snake_case names, UTC timestamps, approved checks/indexes, two internal restrictive foreign keys, and no seed/destructive DDL or cross-module FK. The migration test dynamically migrates all pending versions after a V6 baseline. Every approved §8.4 invariant is exercised on real MySQL 8.4.11.

### Security standard compliance

Inspected authorization order, actor restrictions, read audit behavior, error mapping, logging/static tests, and real Q-009/Q-013 integration tests. Recording is denied without `action-outcome:record`, requires HUMAN even on replay, and reuses the caller's actor context for Q-013 recognition. Reads require `action-outcome:read` but deliberately allow SERVICE. Outcome content is not placed in the narrow view, fingerprints, metrics tags, or logs; an audit failure fails closed before disclosure. No secrets or raw sensitive payloads are introduced.

### Auditability compliance

Inspected the immutable record, operation ledger, and access log. Each fact captures outcome reference, action reference, recording actor, source, outcome text, and UTC time. Mutation replay is durable and atomic. Full-detail access captures actor, reference, and access time and must commit before disclosure. No update/delete endpoint or persistence operation undermines provenance.

### Skill compliance

Inspected `docs/skills/development-standards.md` and the new lesson. The implementation adds a reusable rule requiring exact namespace ownership assertions when sibling names extend an existing prefix, directly reflecting the full-gate failure. The personal `brokeros-review-package` skill governs this timestamped, non-overwriting package, required-file validation, ZIP creation, and SHA-256 handoff.

## Unresolved standards violation

The implementation itself has no identified unresolved architecture violation. However, the repository's mandatory full-gate standard remains unmet because three Q-013 regression assertions are not namespace-safe after the approved Q-014 addition. That failed gate prevents PASS even though repairing those tests is outside the authorized Q-014 scope.
