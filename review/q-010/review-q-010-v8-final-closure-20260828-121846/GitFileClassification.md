# Q-010 V8 Git File Classification

## Q-010 required

Tracked modifications:

- `backend/src/main/java/com/brokeros/risk/api/ResultCode.java`
- `backend/src/main/java/com/brokeros/risk/security/infrastructure/configuration/SecurityModuleConfiguration.java`
- `backend/src/test/java/com/brokeros/risk/FlywayMigrationTests.java`
- `scripts/verify-infrastructure.sh`
- `scripts/verify-static.sh`

These five files are reviewed V7 Q-010 integration/verification changes. Their
modification times preceded V7 package creation, and V8 made no change to them.

Untracked Q-010 implementation scope:

- `backend/src/main/java/com/brokeros/risk/tradingaccount/`
- `backend/src/main/resources/db/migration/V3__create_trading_account_reference_authority.sql`
- `backend/src/test/java/com/brokeros/risk/tradingaccount/`
- Q-010 Requirement/Architecture/ADR/Design/Skill/Lessons
- Q-010 lifecycle Prompts and V1–V8 Review evidence

## Prior Q-010 lifecycle artifacts

The V1–V7 directories and ZIP files under `review/q-010/`, earlier Q-010
Prompts, and earlier Q-010 Lessons are required historical governance evidence.
They were preserved and not overwritten.

## Unrelated pre-existing

None appears as modified or untracked in the final Git status. The tracked
historical Q-009 whitespace issue remains byte-unchanged in HEAD and is not a
Q-010 change.

## Unexpected

None. No build output, IDE file, local database file, log, runtime manifest,
credential, `.env`, temporary Compose overlay, or accidental nested ZIP is
present in the Git candidate scope.

Staged files: 0. ZIP files remain untracked review-transfer artifacts and must
not be staged unless a later explicitly approved staging plan says otherwise.
