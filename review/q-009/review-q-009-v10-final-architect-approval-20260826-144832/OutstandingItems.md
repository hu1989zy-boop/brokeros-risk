# Outstanding Items

## Blocking Items

None.

## Non-Blocking Maintenance

- Flyway emits an advisory that MySQL 8.4 is newer than its latest tested MySQL
  8.1 line. Actual MySQL 8.4.11 migration and regression pass. Reevaluate the
  Flyway version during a separately approved dependency-maintenance task.
- The unrelated untracked historical V6 prompt contains pre-existing trailing
  whitespace. It is outside the Q-009 baseline and this package and was not
  modified.

## Operator Actions

- Obtain external review of this V10 package.
- After external approval, stage the explicit approved text baseline and
  manually commit it. Do not stage Review ZIP transfer artifacts.

No Git commit or push was performed by V10.
