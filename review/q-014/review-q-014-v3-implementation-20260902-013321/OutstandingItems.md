# Outstanding Items

## Blocking item

The implementation-stage Gate Decision is **BLOCKED** because the mandatory full Q-009–Q-014 real-MySQL suite is not fully green.

Three existing Q-013 assertions use prefix matching that unintentionally includes the new sibling `action_outcome` namespace:

1. `Q013MySqlMigrationTests.migrationUpgradesV5CreatesExactlyThreeTablesWithoutSeedsAndValidatesOnRestart` uses an `action_%` table filter, so it sees Q-014's three new tables.
2. `Q013MySqlMigrationTests.metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex` uses the same broad table namespace and sees Q-014's two internal foreign keys.
3. `ActionRestContractTests.resultCodesExposeExactlyTheApprovedActionHttpContract` uses an `ACTION_` result-code prefix, so it sees the eight approved `ACTION_OUTCOME_*` codes.

This is test obsolescence at the Q-013/Q-014 namespace boundary, not a failure in the Q-014 dedicated suites. The Q-014 implementation prompt expressly prohibits changes to Q-013 files, so no repair was attempted.

## Required closure evidence

- Obtain separate authorization for the smallest Q-013 test-only repair.
- Replace broad prefix assumptions with exact Q-013-owned namespaces without weakening any Q-013 assertion.
- Re-run the complete Q-009–Q-014 real-MySQL gate and require 246/246 passing, or the then-current complete count.
- Produce a new, non-overwriting review package for that authorized repair.

## Non-blocking observations

- Flyway 11.7.2 warns that MySQL 8.4 is newer than its explicitly tested maximum of 8.1. All Q-014 MySQL tests nevertheless passed on MySQL 8.4.11; dependency upgrade evaluation remains separate work.
- The disposable database required `log_bin_trust_function_creators=1` because the constrained test user could not create test triggers while binary logging was enabled. This setting was made only inside the disposable container.
