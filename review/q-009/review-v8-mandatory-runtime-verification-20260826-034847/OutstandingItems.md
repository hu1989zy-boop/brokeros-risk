# Outstanding Items

## Blocking

1. Provide an authorized, disposable MySQL 8.4 runtime and run
   `Q009MySqlIntegrationTests` with its documented environment variables. The
   test must execute with zero skips and verify Flyway V1 + V2 plus JDBC behavior.
2. Provide Docker with Compose v2 and run `scripts/verify-infrastructure.sh` to
   verify the repository Compose stack and application dependencies.
3. Re-run the full Maven regression after the mandatory MySQL test is activated;
   the Q-009 mandatory test skip count must be zero.
4. Repeat security and final gate assessment using the resulting MySQL 8.4 and
   Compose evidence.

These items block Verification PASS, Q-009 Complete, Architect Review readiness,
and Git commit readiness.

## Non-blocking Repository Hygiene

`scripts/verify-static.sh` detects whitespace in the pre-existing untracked file
`review/q-006-design/Q-009-V6-Approved-Design-Git-Baseline-Prompt.md` at lines
67, 68, and EOF. V8 did not alter that unrelated historical prompt.

## No Verified Defect

No Q-009 implementation defect was established by V8. The blocking failures are
missing execution capabilities/evidence, so no speculative code change was made.
