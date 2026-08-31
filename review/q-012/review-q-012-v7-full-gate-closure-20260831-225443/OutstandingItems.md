# Outstanding Items

## Current narrow fix

No implementation or verification blocker remains for the authorized
`Q011MySqlMigrationTests` migration-count repair. The previously failing
all-module real-MySQL gate is green.

Independent review remains mandatory. This package does not approve the fix,
Q-011, or Q-012, and no commit is authorized by this stage.

## Deferred observation — not authorized here

`Q012MySqlMigrationTests.migrationUpgradesV4CreatesExactlyFourTablesWithoutSeedsAndValidatesOnRestart`
contains the same latent pattern: after a fixed V4 baseline it hard-codes that
one unrestricted later migration executes. It does not fail today because only
V5 follows V4, but a future V6 would make it stale.

Per the governing Prompt, this Q-012 file was only reported and was not changed.
Any preventive repair requires a separately authorized future maintenance
stage. It is not a blocker for the present 165/165 gate.

## Non-blocking environment note

Flyway 11.7.2 warns that its latest explicitly tested MySQL version is 8.1.
Actual MySQL 8.4.11 verification passed with zero failures or skips. No
dependency update was authorized or attempted.

## Gate Decision

**PASS** for implementation verification of the narrow fix, pending independent
review.
