# Q-010 Proposed Requirement Architect Review Prompt

Use this Prompt only after the Product Owner authorizes Q-010 Requirement
Architect Review.

====================================
Codex Prompt
====================================

Perform ONLY Q-010 Proposed Requirement Architect Review for:

`docs/requirements/Q-010-Trading-Account-Reference-Authority-Foundation.md`

Authority and scope:

1. Read `AGENTS.md`, Q-007/ADR-009, Q-008/ADR-010 and its latest prerequisite
   analysis, Q-009/ADR-011 and V10 closure, applicable Skills/Lessons, and the
   Q-010 V1 candidate-analysis Review Package.
2. Review whether Trading Account Reference Authority Foundation is the correct
   next formal Requirement and whether its boundary is cohesive, minimal,
   broker-neutral, and backed by a real authority rather than unchecked input.
3. Before approving Q-010, decide whether the Q-008 Implementation Design's
   pre-approval status header/Section 17 is an intentionally retained historical
   snapshot or requires a separate metadata-only governance repair. Do not
   change substantive Q-008 business/architecture design in this review.
4. Resolve or return exact corrections for source namespace, broker/tenant
   scope, external-key uniqueness, mapping cardinality, lifecycle/historical
   resolution, initial registration authority, consumer disclosure, security,
   auditability, and failure semantics.
5. Decide only the Requirement gate: APPROVED or CHANGES REQUIRED. Record a
   formal ADR determination recommendation, but do not create/accept an ADR or
   start Architecture unless separately authorized.
6. Do not implement Q-010 or Q-008; do not add Java, tests, migrations,
   dependencies, APIs, configuration, Kafka, Redis, adapters, or infrastructure.
7. Preserve all historical Review files/ZIPs and the current Git baseline.
8. Create a new immutable Q-010 Requirement Review package and ZIP; do not
   overwrite V1.
9. Do not stage, commit, push, reset, clean, or stash.
10. If approved, record Q-010 Requirement approval only and provide a complete
   ready-to-use Prompt for a separately authorized Q-010 Architecture + ADR
   Analysis phase. Do not imply implementation authorization.
11. Report the decision, required corrections/open decisions, ADR
    determination, Review path, ZIP path, Git status, and confirmation that
    implementation remains NOT STARTED / NOT ALLOWED, then stop.
