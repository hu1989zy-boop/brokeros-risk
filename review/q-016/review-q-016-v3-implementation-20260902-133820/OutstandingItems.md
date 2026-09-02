# Q-016 Outstanding Items

## Blocking verification conditions

1. Install/use Flutter stable with Dart >=3.11, then resolve pinned dependencies,
   generate Freezed/JSON sources, analyze, execute all 13 frontend tests, and build
   Flutter web. Flutter/Dart were absent here; none of these results is implied.
2. Run `scripts/run-risk-console-dev.sh` with local-only `.env` values and verify the
   complete browser slice: Keycloak login, list/filter/page, open detail/history/
   associations, add note, version-conflict reload, `401` refresh, `403` display, and
   logout.
3. Independently review generated Dart sources and `pubspec.lock`. They are absent
   because code generation and dependency resolution could not run.

## Assumptions and deliberate choices

- Add investigation note is the one fully implemented operation; assignment remains
  outside this Foundation because the approved design requires at least one operation.
- Page metadata uses `hasNext`, explicitly permitted by Design §4; no unbounded total
  count query was added.
- List reads do not append a fabricated per-case audit event. Existing detail/history
  reads audit one concrete Risk Case, whereas one list response spans many cases and
  no approved multi-target audit contract exists.
- The Keycloak user has a fixed identity but no committed password. The launcher sets
  its password from `KEYCLOAK_OPERATOR_PASSWORD` at runtime.
- Fixed ports 4173, 8180, and 8080 and the exact CORS origin are local-development
  contracts, not production deployment decisions.

## Residual technical risks

- No new migration was authorized. The query uses existing indexes and a bounded
  result, but `updated_at DESC, id DESC` with arbitrary optional filters may still
  scan/sort more rows as production volume grows. Measure with representative data;
  any new covering index requires a separately authorized migration.
- Current Flyway emits a compatibility warning for MySQL 8.4 (tested support through
  MySQL 8.1), although all 305 tests and migrations passed. Track dependency support
  through the normal upgrade/governance process.
- Static inspection cannot prove browser secure-storage behavior, discovery metadata,
  provider logout, or token-refresh interoperability. The dynamic checks above are
  mandatory.

## Independent review handoff

This package is evidence for Claude Code's independent implementation review. It does
not mark Q-016 complete or approved and grants no authority to begin another
Requirement.
