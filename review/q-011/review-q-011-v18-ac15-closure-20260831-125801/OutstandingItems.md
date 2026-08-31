# Q-011 V18 AC15 Closure Outstanding Items

## Deferred Q-010 timestamp precision

The previously documented
`Q010BootstrapMySqlIntegrationTests.controlledCommandUsesTrustedServiceAuthorizationAndExactReplay`
timestamp-precision mismatch remains deliberately deferred. It passed on this
macOS/JBR host, while v17 reproduced it in Linux/Docker due to nanosecond versus
MySQL microsecond precision. Per the V18 boundary, no Q-010 file was inspected
for repair or modified. Governing record:
`docs/lessons/2026-08-31-q-010-bootstrap-replay-timestamp-precision.md`.

## Existing Flyway compatibility warning

Flyway 11.7.2 warns that MySQL 8.4 is newer than its formally listed tested
support maximum of 8.1. The required MySQL 8.4.11 migration, validation,
restart, schema, concurrency, and full-suite tests passed. No dependency change
was authorized or made.

## Independent closure review

Claude Code has not yet independently reviewed this v18 closure package. This
package does not declare Q-011 complete or ready for commit. Nothing was staged,
committed, or pushed.

No AC15 test-maintenance blocker remains in this execution: the all-datasource
124-test real-MySQL gate and static gates passed with zero skips.
