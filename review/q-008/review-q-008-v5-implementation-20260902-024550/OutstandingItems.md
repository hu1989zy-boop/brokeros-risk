# Q-008 Outstanding Items

## Gate conditions

1. **Assignee activity lookup is not an approved provider contract.** Q-009
   supplies a trusted ActorContext for the acting principal and a canonical
   `ActorRef` value object, but no bounded query that confirms an arbitrary
   assignee ref is currently active. Q-008 validates canonical shape and
   preserves assigner provenance. Independent review should confirm this is the
   intended reading or request a future governance decision; Q-008 must not
   invent an IAM lookup.
2. **Governance status mirrors are inconsistent.** The pre-existing V4 design
   header says Design Review is not approved and Implementation Allowed is NO,
   while Requirement §26, the V5 addendum Gate, the Product Owner instruction,
   and the implementation Prompt explicitly authorize this implementation.
   Older Requirement §21/§22 prose also describes the superseded governance-
   only stage. Implementation followed the later explicit authority and did
   not rewrite governance artifacts during this stage.
3. **Infrastructure verifier is stale.** `scripts/verify-infrastructure.sh`
   remains Q-004-specific, binds fixed host ports, validates only V1-V3, and
   requires exactly the old seven application tables. Its real run built the
   backend image but failed because host port 6379 was occupied; its later
   schema assertions would not be valid for V8. A separate maintenance prompt
   should make the verifier use isolated configurable ports and dynamic current
   migration/schema ownership.
4. **Flyway compatibility warning.** The repository's Flyway version reports
   tested MySQL support through 8.1, while mandated integration verification
   used MySQL 8.4.11. Every migration/persistence/full-regression test passed,
   but dependency compatibility should remain visible to maintainers.

## Assumptions made under the autonomy directive

- The exact visible-ASCII 16–128 byte `Idempotency-Key` is hashed without
  trimming; request bodies are validated/normalized according to their typed
  Q-008 contracts. This avoids silently treating two distinct header values as
  the same key.
- Because no active-assignee provider exists, a syntactically canonical Q-009
  `ActorRef` is the maximum honest validation Q-008 can perform for an assignee.
- The authoritative current gate is Requirement §26 plus V5 §5 and the cleared
  Prompt, despite stale earlier status lines in the same pre-existing documents.
- The new implementation review version is V5: V1-V4 are existing governance/
  design review generations, and the V5 addendum is the final implementation
  binding authority. No older package was modified.

## Explicitly deferred by approved scope

- Related/cross-case Decision associations.
- Team ownership, queues, assignment routing, and role/organization policy.
- Legal hold, retention duration, exceptional redaction, and detailed
  regulatory content policy.
- Kafka publication, Redis caching, external execution, vendor outcome
  interpretation, generic workflow engines, and case deletion.

## Implementation completeness statement

No known Q-008 behavior, Design §5 operation, Design §8 table, required
provider adapter, endpoint, or mandatory test category is omitted. This package
does not self-approve the implementation: Claude Code/Architect independent
review and Product Owner acceptance remain outstanding. No other Requirement
may begin from this package alone.
