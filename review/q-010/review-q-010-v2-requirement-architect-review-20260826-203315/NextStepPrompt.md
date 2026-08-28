# Q-010 Architecture + ADR Analysis Prompt

Use this Prompt only after the Product Owner separately authorizes Q-010
Architecture + ADR Analysis.

====================================
Codex Prompt
====================================

Perform ONLY Q-010 Architecture + ADR Analysis for the approved Requirement:

`docs/requirements/Q-010-Trading-Account-Reference-Authority-Foundation.md`

Authority and scope:

1. Read `AGENTS.md`, development standards, Q-007/ADR-009, Q-008/ADR-010 and
   its authoritative prerequisite/approval records, Q-009 Architecture/
   Implementation Design/ADR-011/V10 closure, applicable Skills/Lessons, and
   the complete Q-010 V1 and V2 Review packages.
2. Treat Q-010 Requirement V1 Section 2.1 as approved and fixed. Do not reopen
   its authority tuple, one-to-one cardinality, no-reassignment rule,
   lifecycle/history, non-web registration authority, Q-008 disclosure,
   capability, audit, or failure decisions.
3. Verify the next ADR number is unreserved. If `ADR-012` is available, create
   a **Proposed** ADR for Trading Account Reference Authority Architecture;
   otherwise stop and report the identifier conflict. Do not mark any ADR
   Accepted.
4. Produce a cohesive Q-010 Architecture covering bounded-context ownership,
   module/dependency direction, domain/reference types, opaque formats and
   canonicalization, authority-scope bootstrap, source provenance, non-web
   manifest provisioning, idempotency, concurrency, transaction boundaries,
   lifecycle/history, application-owned persistence analysis, Q-009 security,
   protected internal consumer contracts, disclosure, failure semantics,
   threat analysis, operations, and Q-008 compatibility.
5. Analyze the future additive MySQL/Flyway design, normalization/collation,
   uniqueness, foreign keys, optimistic locking, append-only history, indexes,
   query plans, retention, and MySQL 8.4 verification. Do not create a
   migration, table, Java type, repository, or executable schema.
6. Keep Q-010 broker/CRM/platform/vendor neutral and inside the existing Phase
   1 modular monolith. Do not create a Broker/Tenant/Customer master, universal
   entity framework, public enumeration API, automatic discovery/sync, direct
   external-database access, or invented vendor SDK.
7. Do not modify Q-008. Its V4 pre-approval statements are intentional
   submission-time history and its later approval record is authoritative.
   Q-008 remains unimplemented and blocked by other provider prerequisites.
8. Do not implement Q-010 or Q-008. Do not add Java, tests, Flyway migrations,
   dependencies, endpoints, runtime configuration, Kafka, Redis, adapters,
   Docker/Kubernetes changes, UI, or infrastructure.
9. Create/update the Q-010 Architecture document, Proposed ADR, an honest
   Architecture Lessons Learned entry, a new immutable timestamped Q-010 V3
   Architecture Review package, and a self-contained verified ZIP. Preserve
   V1/V2 Reviews and ZIPs.
10. The V3 package must include substantive Development Standards Compliance,
    ADR review, data/security/audit/threat analysis, Requirement traceability,
    verification evidence, outstanding decisions, bounded project tree, Git
    status/diff stat, package manifest, and a complete ready-to-use Prompt for
    the separately authorized Architect Architecture Review/approval gate.
11. Do not stage, commit, push, reset, clean, stash, or start Implementation
    Design. Architecture output must remain `READY FOR ARCHITECT REVIEW`, not
    approved; the ADR must remain `PROPOSED`.
12. Report Architecture/ADR Analysis result, Requirement conformance, proposed
    ADR identifier/status, unresolved Architecture decisions, production code
    change (`NO`), Architecture path, ADR path, Review directory, ZIP path, Git
    status, and confirmation that implementation remains NOT STARTED / NOT
    ALLOWED, then stop.
