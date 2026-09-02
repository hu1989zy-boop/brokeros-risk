# Q-016 — Claude Code Independent Implementation Review (v4)

- Requirement: Q-016 — Frontend Foundation (Flutter Risk Console)
- Lifecycle stage: Independent implementation review of Codex's v3 delivery
- Reviewer: Claude Code (external Architect role)
- Reviewed package: `review/q-016/review-q-016-v3-implementation-20260902-133820`
- Date: 2026-09-02
- **Gate Decision: PASS WITH CONDITIONS**

## One-paragraph verdict

Codex's backend work is correct and independently reproduced (305 tests, 0
failures, against a disposable real MySQL 8.4). Codex honestly declared that it
had no Flutter/Dart SDK and therefore marked every frontend acceptance criterion
"FAIL — not verified" rather than fabricating a pass — exactly as the
implementation prompt required. My independent Flutter verification then found
**two blocking defects that would have prevented the frontend from compiling at
all**, plus two minor test defects. I fixed all four and re-verified: the
frontend now resolves dependencies, generates code, and passes `flutter analyze`
with **"No issues found!"** — all application code and all 13 test files compile
and type-check for the web target. In-browser test *execution* and the live
Keycloak end-to-end slice remain the two outstanding conditions (see
`OutstandingConditions.md`), the first only because my Docker verification
harness is arm64 and has no runnable headless browser — not because of any code
defect.

## Why this review mattered

Had this delivery been accepted on Codex's self-report alone, a **non-compiling
frontend** would have been committed: `flutter pub get` failed outright (F1), and
even after that, the app had a hard name-collision compile error (F2). Neither was
discoverable without actually running the Flutter toolchain, which Codex could not
do. This is the core value of adversarial independent verification.

## Findings (detail in `Findings.md`)

| ID | Severity | Status | One-line |
| --- | --- | --- | --- |
| F1 | Blocking (would not resolve) | Fixed + verified | `build_runner 2.16.0` vs `freezed 3.2.5` analyzer conflict broke `flutter pub get` |
| F2 | Blocking (would not compile) | Fixed + verified | `RiskCaseListPage` defined as BOTH a data model and a widget → 15 compile errors |
| F3 | Minor (test) | Fixed + verified | test used `AsyncValue.hasError`, absent in flutter_riverpod 3.4.2 |
| F4 | Minor (test) | Fixed + verified | unused `api_contract.dart` import in a widget test |
| — | Regression (infra) | Fixed earlier | new Keycloak `${VAR:?}` vars broke `docker compose config` in `verify-infrastructure.sh` |
| — | False alarm — ruled out | No defect | "web-only imports break tests" was my own wrong test platform (VM vs `--platform chrome`), not a product defect |

## Acceptance criteria — reviewer view

| AC | Codex (v3) | Reviewer (v4, after fixes) |
| --- | --- | --- |
| 1 build/run console | FAIL — not verified | Compiles + analyzes clean; **in-browser run pending (C1)** |
| 2 OIDC/PKCE E2E | FAIL — not verified | Statically clean; **live Keycloak E2E pending (C2)** |
| 3 list/detail/history | FAIL — not executed | Backend list PASS; frontend compiles clean; run pending (C1) |
| 4 add-note + conflict | FAIL — not executed | Compiles clean; run pending (C1) |
| 5 additive list endpoint | PASS | **PASS — reproduced (real MySQL)** |
| 6 local slice E2E | FAIL — not executed | Static/config PASS; **live E2E pending (C2)** |
| 7 frontend tests run | FAIL — not run | 13 tests compile + analyze clean; **browser execution pending (C1)** |
| 8 no Q-008…Q-014 change | PASS | **PASS — confirmed** |

## Recommendation to the Product Owner

Accept Q-016 as **PASS WITH CONDITIONS**. The foundation is sound and now provably
builds. Before Q-016 is marked *complete*, satisfy C1 (run the 13 frontend tests
in a real browser) and C2 (live Keycloak browser E2E) — both listed in
`OutstandingConditions.md`. All four defects I found are already fixed in the
working tree and were verified up to static-analysis-clean; see `ChangedFiles.md`
for the full list of files I touched so you can decide whether to accept my fixes
directly or route them back through Codex.
