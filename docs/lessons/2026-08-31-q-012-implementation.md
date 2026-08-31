# Q-012 Implementation Lessons Learned

## Scope

Implementation and verification of the Decision Provenance Foundation under
Q-012, Requirement V1, Architecture V1, ADR-014, and Implementation Design V1.

## What worked

- Treating Implementation Design §11.1 as the only execution-order authority
  kept authorization, the `HUMAN` restriction, raw fingerprinting, replay,
  content validation, Q-010 validation, Q-011 validation, context creation, and
  mutation in one testable sequence.
- Length-framed SHA-256 input plus sorted, de-duplicated raw Evidence references
  produced deterministic set semantics without making the replay path depend
  on later content validation.
- Keeping `subject_ref` and `evidence_ref` as checked opaque references, with
  only three Decision-internal foreign keys, preserved module ownership while
  still making the recording operation auditable and atomic.
- Real MySQL tests proved constraint metadata, enforcement, rollback, collision
  retry, idempotent concurrency, access-audit failure behavior, and query plans;
  source inspection alone would not have provided equivalent evidence.

## Problems found honestly

- The disposable MySQL user initially could not create test-only failure
  triggers because binary logging required trusted function creators. The
  application migration did not require this privilege. The disposable server
  was configured with `log_bin_trust_function_creators=1`, after which all 19
  mandatory Q-012 MySQL tests passed without weakening or skipping a test.
- The mandatory all-Q009/Q010/Q011/Q012 database gate exposed a pre-existing
  forward-compatibility assumption in
  `Q011MySqlMigrationTests.migrationUpgradesV3CreatesExactlyFourTablesAndValidatesOnRestart`:
  after V5 exists, its untargeted continuation from V3 executes V4 and V5, but
  the assertion requires exactly one migration. Q-012's Prompt simultaneously
  forbids modifying any Q-011 file, so this implementation stage cannot repair
  that failing regression without new authority. The review package records
  the exact blocker instead of hiding it or weakening the gate.
- A sandboxed Maven package attempt could not initialize Mockito's dynamic
  agent. Re-running the same build in the approved host execution context
  succeeded; both outcomes are retained in verification evidence.

## Reusable rule captured

`docs/skills/development-standards.md` now records the semantic-fingerprint
rule for unordered multi-reference inputs and replay-before-validation flows.
This applies beyond Q-012 wherever idempotency keys cover set-valued raw input.

## Follow-up boundary

Repairing the Q-011 migration test requires an explicitly authorized Q-011
test-maintenance or governance decision. It must not be smuggled into Q-012,
and no subsequent Requirement should begin until the independent review and
stage gate address this condition.
