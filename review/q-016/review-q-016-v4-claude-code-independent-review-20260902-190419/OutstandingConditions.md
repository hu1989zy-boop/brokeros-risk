# Q-016 v4 — Outstanding Conditions

The gate is **PASS WITH CONDITIONS**. Two conditions remain before Q-016 can be
declared *complete*. Neither is a known code defect; both are executions my
verification environment could not perform.

## C1 — Execute the 13 frontend tests in a real browser

Run, on any host with a runnable Chrome/Chromium (a normal dev machine, or an
amd64 CI runner — not the reviewer's arm64 Docker harness):
```bash
cd frontend
flutter pub get
dart run build_runner build --delete-conflicting-outputs
flutter test --platform chrome
```
Expected: green. The tests already compile and `flutter analyze` reports zero
issues; only in-browser execution is unconfirmed. If any test fails, treat it as a
new finding.

## C2 — Live browser end-to-end slice against Keycloak (AC 2, AC 6)

Bring up the dev slice (`scripts/run-risk-console-dev.sh` with local-only `.env`
values) and exercise the full path in a browser:
- Keycloak login via OIDC Authorization Code + PKCE;
- backend JWT verification accepts the token; no credentials in any request body;
- list → filter → paginate; open detail → history → associations;
- add investigation note; force a version conflict and confirm typed handling;
- `401` triggers exactly one refresh-and-retry; `403` renders a typed
  authorization error; logout clears tokens.

## Note on the fixes already applied

All four defects (F1–F4) are fixed in the working tree and verified up to
static-analysis-clean. `ChangedFiles.md` lists every file I touched. The Product
Owner decides whether to accept these fixes directly (recommended — they are
minimal and verified) or route them back through Codex. No commit or push has been
performed by the reviewer.
