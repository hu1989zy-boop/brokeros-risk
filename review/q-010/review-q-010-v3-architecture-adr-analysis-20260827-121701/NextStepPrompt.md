# Q-010 Architecture + ADR-012 External Architect Review Prompt

Use this Prompt only after the Product Owner authorizes the external Architect
Review/approval gate for Q-010 Architecture V1 and Proposed ADR-012.

====================================
Codex Prompt
====================================

Perform ONLY Q-010 Architecture V1 + ADR-012 Architect Review for:

- `docs/requirements/Q-010-Trading-Account-Reference-Authority-Foundation.md`
- `docs/architecture/q-010-trading-account-reference-authority-architecture.md`
- `docs/adr/ADR-012-trading-account-reference-authority-foundation.md`
- `review/q-010/review-q-010-v3-architecture-adr-analysis-20260827-121701/`

Requirements:

1. Read `AGENTS.md`, development standards, Q-007/ADR-009, Q-008/ADR-010 and
   its authoritative approval/prerequisite records, Q-009 Architecture/
   Implementation Design/ADR-011/V10 closure, applicable Skills/Lessons, the
   approved Q-010 Requirement, and Q-010 V1/V2/V3 Review packages.
2. Verify that the V3 Architecture preserves every fixed Q-010 Section 2.1
   decision and does not expand into Trading Account master data, Q-008,
   vendor adapters, public provisioning, Kafka, Redis, or external DB access.
3. Review all 25 required Architecture questions, FR/Acceptance traceability,
   module ownership, reference formats, scope/namespace/key canonicalization,
   bidirectional uniqueness, lifecycle, attested provisioning, idempotency,
   concurrency, MySQL/Flyway boundary, Q-009 security, history atomicity,
   failure/threat model, and Q-008 effect.
4. Review ADR-012 for Context, durable Decision, meaningful Alternatives,
   Consequences, security/data/operations/dependencies/deferred decisions, and
   consistency with ADR-002/009/010/011.
5. Decide only:
   - Q-010 Architecture V1: `APPROVED` or `CHANGES REQUIRED`;
   - ADR-012: `ACCEPTED` or `CHANGES REQUIRED`;
   - Implementation Design Ready: `YES` only if both are approved/accepted.
6. If changes are required, keep Architecture/ADR Proposed and return exact
   corrections. Do not implement them outside an authorized review-fix scope.
7. If approved, record only the explicit Architect Architecture/ADR decision
   in Q-010 governance metadata and a new immutable timestamped V4 approval
   Review package/ZIP. Do not rewrite V1/V2/V3 historical packages.
8. Do not start Implementation Design, implement Q-010/Q-008, add Java/tests/
   Flyway/SQL/API/dependencies/configuration/Kafka/Redis/adapters/infrastructure,
   or modify Q-008/Q-009 semantics.
9. Do not stage, commit, push, reset, clean, or stash. Preserve all ZIPs as
   transfer artifacts.
10. Provide a complete ready-to-use Prompt for a separately authorized Q-010
    Implementation Design phase only if Architecture is approved and ADR-012
    is Accepted. That next Prompt must still prohibit implementation.
11. Report the formal decision, corrections/open items, Architecture/ADR paths,
    V4 Review path, ZIP path, Git status, and confirmation that Implementation
    remains NOT STARTED / NOT ALLOWED, then stop.
