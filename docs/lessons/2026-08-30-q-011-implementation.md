# Q-011 Evidence Provenance Foundation — Implementation Lessons

## Context

Q-011 was implemented only after the Product Owner explicitly approved
Requirement V3, Architecture V4, the amended/re-accepted ADR-013, and
Implementation Design V5, then issued a fresh implementation authorization.
The work remained inside the Evidence module, the new V4 migration, Q-011
tests, the shared ResultCode catalog, and reusable standards documentation.
Q-008, Q-009, Q-010, V1–V3, deployment, Kafka, and Redis were not modified.

## What Worked

- Re-reading the current documents in their declared authority order prevented
  older resume-prompt summaries from reintroducing corrected behavior.
- Keeping authorization and the HUMAN check ahead of replay made the ordering
  directly testable, including the SERVICE-actor replay denial case.
- A narrow provenance view made observation/reason leakage structurally
  impossible for the future Q-008 consumer.
- Named MySQL constraints plus exact root-SQLException classification made the
  generated-reference retry bound and idempotency races testable without broad
  duplicate-error retries.
- Real MySQL 8.4.11 tests found a defect that static DDL inspection missed.

## Problems Found and Repaired

1. The first V4 history `before_status` check used
   `before_status = 'ACTIVE'` for `CORRECT`. MySQL treats a `CHECK` result of
   `UNKNOWN` as accepted, so a null value passed. The migration now includes
   an explicit `before_status IS NOT NULL`, and a real-MySQL test proves null,
   wrong-value, and correct-value behavior.
2. The first REST controller composition was also discovered in Q-010's
   non-Web bootstrap context, where no Web-only `ActorContextProvider` exists.
   The controller is now conditional on a Servlet Web application, preserving
   both the HTTP surface and the existing non-Web Q-010 command.
3. VARBINARY content initially decoded with replacement characters on malformed
   UTF-8. Row mapping now uses a strict decoder and fails authority reads closed
   on corrupted bytes.
4. Whitespace-only content was initially non-empty by byte count but still
   blank under the approved domain rule. Domain construction now rejects it
   without trimming or normalizing valid content.
5. The repository static gate still required exactly three migrations. Q-011
   extended the gate to require V1 through V4 and added explicit checks that
   V4 creates exactly the four approved Evidence tables and remains additive,
   forward-only, and schema-only.

## Existing Regression-Gate Gap

The unchanged Q-009 real-MySQL test still asserts that an unrestricted Flyway
migration from V1 executes exactly one additional migration. That expectation
was already stale after committed V3 (it would observe V2 and V3); with V4 it
observes three. Q-011's boundary prohibits modifying Q-009, so the implementation
does not rewrite that test. Q-011 instead adds its own real Q-009/Q-010
authorization integration tests, and the review package records this legacy
gate mismatch explicitly for independent disposition.

## Reusable Rule

For every MySQL `CHECK` over nullable columns, test the `NULL` case explicitly.
Do not infer rejection from an equality predicate: SQL three-valued logic can
turn the expression into `UNKNOWN`, which MySQL accepts. This rule was added to
`docs/skills/development-standards.md`.
