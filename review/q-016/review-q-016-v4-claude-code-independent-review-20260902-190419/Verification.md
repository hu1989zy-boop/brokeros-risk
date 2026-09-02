# Q-016 v4 — Verification (commands I actually ran)

Every result below was produced by me, not read from Codex's report. Backend ran
in a disposable MySQL 8.4 container + `maven:3.9.9-eclipse-temurin-21-alpine`;
frontend ran in `ghcr.io/cirruslabs/flutter:stable` (Flutter stable, Dart >=3.11)
with `frontend/` bind-mounted. My machine is Apple Silicon (arm64), so the Flutter
image is arm64.

## Backend — independently reproduced

Full repository real-MySQL gate (all Q-008…Q-014 aliases mapped to one disposable
schema):

```
Result: 305 tests, 0 failures, 0 errors, 0 skipped.
```

This matches Codex's claimed 305/0/0. The additive `GET /api/risk-cases` endpoint
was code-reviewed: bounded (server cap 100), authorization-guarded
(`RiskCaseCapabilities.READ`), projection-only, stable ordering, no migration, no
Q-008…Q-014 business-rule change (AC 5, AC 8 → PASS).

## Frontend — sequence of runs

### Run A (log 01) — Codex's tree as delivered
```
flutter pub get            → FAILED  (F1: build_runner 2.16.0 vs freezed 3.2.5)
```
After manually applying F1, re-run surfaced F2:
```
flutter analyze            → 15 errors, all from the RiskCaseListPage collision
flutter test               → 2 of 3 test files fail to load (collision); api_contract_test: 3 passed
```

### Run B (log 02) — after F1 + F2 fixes
```
flutter pub get                                   → PUBGET_OK
dart run build_runner build --delete-conflicting  → BUILDRUNNER_OK
flutter analyze                                   → 2 issues (F3 error + F4 warning)
```
The 15 collision errors are gone; codegen regenerates cleanly for the renamed
class. Remaining: F3 (`hasError`) and F4 (unused import).

### Run C (log 03) — after F3 + F4 fixes
```
flutter pub get   → PUBGET_OK
flutter analyze   → No issues found! (ran in 8.0s)   [ANALYZE_EXIT=0]
```
**All application code and all 13 test files now compile and type-check for the
web target with zero analyzer issues.**

### Chrome-platform test execution — attempted, blocked by harness (not a defect)
```
flutter test --platform chrome
```
- On the chrome platform the js_interop/package:web errors do not occur (proving
  the earlier VM-platform errors were a wrong-platform artifact, not a defect).
- The test files **compile**; execution then fails at *browser launch*:
  - Ubuntu's `chromium-browser` in the image is a **snap stub** (`requires the
    chromium snap to be installed`) — snap cannot run in this container.
  - `google-chrome-stable` has **no linux/arm64 build** (Google ships Chrome for
    amd64 only); the arm64 image cannot install it.
- Therefore in-browser execution of the 13 tests could not be performed in my
  Docker harness. This is an environment limitation of the reviewer's machine, not
  a code defect — the tests compile and analyze clean. See condition C1.

## What I did NOT (could not) execute

- In-browser execution of the 13 Flutter unit/widget tests (condition C1).
- The live Keycloak → Flutter → backend → MySQL browser slice: login/PKCE, Bearer
  attach, list/filter/page, detail/history/associations, add-note version-conflict,
  401 refresh, 403 display, logout (condition C2, covering AC 2 and AC 6).

I am not representing either as passed. See `OutstandingConditions.md`.
