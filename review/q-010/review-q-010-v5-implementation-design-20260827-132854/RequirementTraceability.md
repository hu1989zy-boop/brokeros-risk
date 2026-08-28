# Q-010 V5 Requirement Traceability

## Result

- Requirement traceability: **PASS FOR EXTERNAL ARCHITECT REVIEW**
- Normative functional IDs: **12**
- IDs covered by the Design: **12**
- Runtime acceptance proven in V5: **NO — design only**

## Matrix

| Requirement | Design evidence | Planned executable evidence |
| --- | --- | --- |
| `Q010-FR-001` | opaque ref domain/generator and account table | format/generator/unique-key tests |
| `Q010-FR-002` | exact scope/namespace/key tuple and byte rules | value/collation/VARBINARY tests |
| `Q010-FR-003` | two unique mapping directions, no identity update/delete | duplicate/race/restricted-delete tests |
| `Q010-FR-004` | controlled command, strict manifest, attestation, replay | parser/authorization/idempotency tests |
| `Q010-FR-005` | protected exact resolution and narrow eligibility | not-found/outage/eligibility tests |
| `Q010-FR-006` | retained ACTIVE/INACTIVE/RETIRED state/history | lifecycle/historical-resolution tests |
| `Q010-FR-007` | named CAS transitions plus atomic provenance | concurrency/rollback/history tests |
| `Q010-FR-008` | exact Q-009 capabilities and guard-before-port | denial-order/service-grant tests |
| `Q010-FR-009` | fail-closed conflicts, outage, no stale fallback | error/non-enumeration/outage tests |
| `Q010-FR-010` | typed bounded Q-008 contract and forbidden fields | type/static/serialization/log tests |
| `Q010-FR-011` | broker/vendor isolation and no external DB/SDK | package/dependency/static scans |
| `Q010-FR-012` | minimal four-table Phase 1 foundation | migration/package/schema inspection |

## Acceptance Boundary

The twelve Requirement Acceptance Criteria are assigned to the domain,
persistence, authorization, manifest, transaction, consumer, security, static,
real-MySQL, regression, Review, and later gate evidence in Design Sections
18/21. Documentation cannot satisfy runtime criteria; implementation,
mandatory runtime verification, final Architect review, staging, and commit
remain separate recorded gates.

## Q-008 Dependency Trace

Q-010 designs only the authoritative `TRADING_ACCOUNT` subject-reference
eligibility prerequisite. Evidence, Decision, Action, ActionOutcome, and Q-008
implementation authorization remain unsatisfied. Q-008 cannot read Q-010
tables or external identity and must independently enforce its own capability
before Q-010 enforces `trading-account-reference:read`.
