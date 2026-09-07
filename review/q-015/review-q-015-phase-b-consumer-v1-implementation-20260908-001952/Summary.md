# Q-015 Phase B portable consumer — implementation handoff

**Gate Decision: PASS WITH CONDITIONS.** This is the Implementation / Implementation
Verification gate assessment for Claude Code's independent review. It is not
Product Owner acceptance, independent review, live gateway acceptance or Final Closure.

## Delivered behavior

- Kafka canonical input now invokes the existing local ingestion transaction
  without re-publishing to its input topic. The record offset advances only after
  event/gap commit or confirmed quarantine publication; infrastructure failures
  stop consumption and keep the original input replayable.
- An ADR-024 typed routing projection and strict UTF-8/envelope reader validate
  neutral kinds, refs, side/reason, UTC time, key and provenance. Original canonical
  payload object bytes are stored intact; no payload columns or monetary defaults.
- Malformed input reaches a visible dead-letter topic with safe broker coordinates.
  The legacy authenticated HTTP entry is default-off and documented as a test aid.
- Added recorded/synthetic contract and real-MySQL + embedded-Kafka replay coverage,
  operating/configuration guidance, reusable development guidance and an honest
  lesson at docs/lessons/2026-09-08-q-015-phase-b-consumer-implementation.md.

## Evidence and conditions

Final Linux Java 21 Maven package: **356 tests, 0 failures, 0 errors, 0 skipped**
across 76 suites. Q-015 contributes **38 tests**, including **4 real-MySQL plus
embedded-Kafka tests** and the unchanged Phase A persistence/HTTP regression suite.
The focused new contract/Kafka selection also passed 12/12. Static and all three
Kustomize renders passed. See Verification.md and TestInventory.txt.

The actual V9 is the previously accepted **non-partitioned fallback**; migration,
JDBC store, envelope and golden fixture bytes are unchanged. All production Java
changes stay in tradingdata; no SDK, gateway, new capability or other business
module was changed. Nineteen source/test/documentation files changed; exact scope
and hashes are in SourceManifest.sha256. The six pre-existing untracked review
ZIPs listed in GitStatus.txt were left untouched.

The main conditions are existing global-sequence/gap limits across account
partitions; undefined gateway reconnect epoch encoding and raw quote account
attribution; missing currency in supplied monetary observations; production broker
security/retention/provisioning/monitoring verification; and at-least-once recovery.
The typed model is a routing projection, not a complete financial payload validator.
OutstandingItems.md records each limitation and authority reconciliation.

## Stage boundary and recommendation

Approved inputs: Q-015 parent §17 V2 authorization, Phase A addendum, accepted
ADR-024, Architecture V2 §3 and Gateway Design V2 wire context. Allowed scope:
portable Java consumer/model/tests/docs plus this new review package. Forbidden:
C++/SDK, MT5 gateway, schema/reliability redesign, other requirements, old-package
edits, staging/commit/push and production deployment.

Requirement status: portable requested behaviors implemented and tested within the
stated projection/serialized-sequence assumptions. Architecture/design status:
aligned to the V2 trigger revision while retaining the documented Phase A limits.
No newly introduced violation or unexplained test failure remains. Full Q-015/live
MT4 acceptance is not claimed.

The scoped MySQL container/volume/network and embedded brokers were cleaned up;
temporary credentials were deleted. The existing Maven cache was retained. Nothing
was staged, committed or pushed; HEAD remains ab82faa27d8f2d4fd79f086e5df80791e26350ab.

Recommendation: submit this package and actual workspace to Claude Code for
independent review, focusing on the conditions above. Stop at this boundary; do
not begin gateway work, acceptance, Final Closure or Git automatically.
