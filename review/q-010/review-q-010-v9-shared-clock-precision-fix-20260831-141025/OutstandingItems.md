# Outstanding Items

1. Claude Code must perform an independent code/governance review and re-run
   the full Q009/Q010/Q011 real-MySQL test gate in the Linux/Docker Java 21
   environment where the old sub-microsecond mismatch reproduces
   deterministically.
2. The existing Flyway compatibility warning for MySQL 8.4 remains recorded.
   Dependency changes were outside this task's authorized scope; migrations
   and tests passed.
3. The repository contains pre-existing, uncommitted Q-011 implementation and
   review work plus the previously authorized Q009 integration-test repair.
   Those changes were preserved and not folded into this fix.

No other implementation work is authorized by this package. These items mean
the shared-clock fix and Q-009/Q-010/Q-011 are not declared complete,
approved, or ready for commit here.
