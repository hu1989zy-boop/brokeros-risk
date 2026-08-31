# Q-011 Evidence Provenance Foundation — Implementation Review Summary

## Review status

This package presents the Q-011 implementation candidate for Claude Code's
independent review. It does **not** declare Q-011 complete, approved, or ready
for commit.

The implementation-specific Q-011 tests, the Q-010/Q-011 real-MySQL full
suite, packaging, dependency-tree check, and the updated static gate pass.
The repository-wide real-MySQL gate with Q-009 also enabled has one unresolved
failure in the unchanged `Q009MySqlIntegrationTests`: its V1-baseline test
still expects exactly one later migration, while V2, V3, and V4 now correctly
execute. Because the task explicitly prohibits modifying Q-009, Acceptance
Criterion 15 remains **FAIL** pending independent disposition.

## Implemented scope

- Added the modular-monolith `com.brokeros.risk.evidence` module with domain,
  application ports/services, JDBC adapters, configuration, observability,
  and REST input adapter.
- Added additive Flyway V4 with exactly `evidence_record`,
  `evidence_operation`, `evidence_operation_history`, and
  `evidence_access_log`.
- Implemented recording, correction, narrow provenance confirmation, and
  audited full-detail read using the exact Design V5 ordering rules.
- Added the nine approved Q-011 ResultCodes and no speculative codes.
- Added real MySQL 8.4.11 migration, persistence, concurrency, rollback,
  security, and fail-closed corruption tests.
- Updated the repository static gate for V4, the reusable MySQL nullable
  `CHECK` rule, and the Q-011 implementation Lessons Learned entry.

No Q-008 code or wiring, Q-009/Q-010 module file, V1–V3 migration, Kafka
topic, Redis key, deployment manifest, dependency, or production credential
was added or modified. Nothing was staged, committed, or pushed.

## Acceptance Criteria

| Criterion | Result | Evidence |
| --- | --- | --- |
| AC 1 | PASS | Approved Requirement V3 retains repository-backed gap analysis. |
| AC 2 | PASS | Narrow provenance contract is structurally compatible with Q-008 and is not wired into it. |
| AC 3 | PASS | Only `MANUAL` and `TRADING_ACCOUNT` are represented and database-enforced. |
| AC 4 | PASS | Q-011 `ACTIVE`/`SUPERSEDED` remains distinct from Q-008 association disposition. |
| AC 5 | PASS | `EvidenceProvenanceQueryService` and `EvidenceDetailReadService` are separate protected contracts. |
| AC 6 | PASS | ADR-013 amendment was re-accepted before implementation authorization. |
| AC 7 | PASS | Historical Requirement gate remained implementation-free; implementation began only after fresh authorization. |
| AC 8 | PASS | Application, REST, domain, persistence, concurrency, and security tests cover valid/invalid/denied/not-found/conflict outcomes. |
| AC 9 | PASS | Record/Correct reject `SERVICE` before replay; both read paths permit authorized non-HUMAN actors. |
| AC 10 | PASS | No delete use case/SQL exists; superseded records remain queryable. |
| AC 11 | PASS | Only Q-010 `NOT_RECOGNIZED` rejects; no Evidence is created. |
| AC 12 | PASS | Operation/history and mutation are atomic; correction reason is mandatory and constrained. |
| AC 13 | PASS | Correction copies the target subject; request DTO accepts no correction subject. |
| AC 14 | PASS | Full-detail access log commits in a dedicated non-read-only transaction before content return. |
| AC 15 | **FAIL** | Q-011 mandatory tests execute and pass, but the all-Q009/Q010/Q011 real-MySQL Maven gate has one stale Q-009 migration-count assertion failure. |

## Review entry points

- `ArchitectureReview.md`: architecture and standards evidence.
- `DesignTraceability.md`: Q011-FR-001–014 and Design §8.5 mapping.
- `Verification.md`: exact environment, commands, results, warnings, and
  failures.
- `SecurityReview.md`: authorization, disclosure, audit, and fail-closed
  analysis.
- `OutstandingItems.md`: the unresolved gate and independent-review boundary.
