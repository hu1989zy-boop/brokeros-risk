# Q-014 Implementation Lessons Learned

## Scope

Q-014 added the Action Outcome Provenance Foundation as a new Phase 1
modular-monolith capability. It records immutable, `MANUAL`, human-entered
outcome facts pertaining to one Action and intentionally does not model real
execution or a result taxonomy.

## What worked

- Reusing Q-013's narrow `confirmProvenance` contract preserved the upstream
  authority boundary without a cross-module database foreign key.
- Keeping authorization, the `HUMAN` check, raw fingerprinting, replay, content
  validation, upstream confirmation, context creation, and mutation in one
  explicit order made the security and idempotency tests direct.
- The fixed V6 baseline plus dynamic `flyway.info().pending().length` assertion
  avoided repeating the stale unrestricted-migration-count defect.
- Real MySQL tests proved the many-to-one rule by persisting two different
  outcome facts for the same `action_ref`.

## Problems encountered

- The filesystem sandbox denied loopback JDBC connections with
  `SocketException: Operation not permitted`. The same mandatory tests were
  rerun against the disposable local MySQL container with host permission.
- MySQL 8.4 binary logging initially prevented the disposable test user from
  creating failure-injection triggers. Enabling
  `log_bin_trust_function_creators` only inside that disposable container
  restored the intended tests without changing application code.
- The repository-wide gate exposed three Q-013 test failures. Q-013 selected
  tables with `LIKE 'action_%'` and ResultCodes with `startsWith("ACTION_")`,
  so the additive Q-014 `action_outcome_*` / `ACTION_OUTCOME_*` namespace was
  incorrectly counted as Q-013. Q-014's strict Prompt prohibited modifying
  Q-013, so the failures were recorded as an external blocker rather than
  silently repaired.

## Reusable rule

When a module name is a prefix of another approved module, inventory and
metadata tests must use exact names or a delimiter-aware ownership boundary.
A broad prefix is not future-safe evidence of module ownership. This rule was
added to `docs/skills/development-standards.md`.

## Verification implication

A targeted Q-014 PASS and a repository-wide Gate failure are different facts.
Both must be reported: Q-014's 42 tests passed, while final implementation
verification remains blocked until the three pre-existing Q-013 assertions are
made namespace-aware under separate authority.
