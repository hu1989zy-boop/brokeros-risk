# Q-016 v4 — Files changed by the reviewer

Full transparency: below is every file **I** (Claude Code, during this review)
modified or generated, separate from Codex's v3 delivery. All of `frontend/` is
new/untracked (Codex authored it); the entries under it are the specific files I
edited within it.

## Production code (Flutter app)

| File | Change | Finding |
| --- | --- | --- |
| `frontend/pubspec.yaml` | `build_runner: 2.16.0` → `2.15.1` | F1 |
| `frontend/lib/features/riskcase/data/risk_case_models.dart` | class `RiskCaseListPage` → `RiskCaseSummaryPage` (+ Freezed part refs) | F2 |
| `frontend/lib/features/riskcase/data/risk_case_repository.dart` | data-model type rename | F2 |
| `frontend/lib/features/riskcase/application/risk_case_notifiers.dart` | data-model type rename | F2 |
| `frontend/lib/features/riskcase/presentation/risk_case_list_page.dart` | only the `AsyncValue<…>` model type renamed; **widget class + State unchanged** | F2 |

## Generated (by build_runner / pub — not hand-edited)

| File | Note |
| --- | --- |
| `frontend/lib/features/riskcase/data/risk_case_models.g.dart` | regenerated for `RiskCaseSummaryPage` |
| `frontend/lib/features/riskcase/data/risk_case_models.freezed.dart` | regenerated for `RiskCaseSummaryPage` |
| `frontend/pubspec.lock` | newly produced by `flutter pub get` (was absent — Codex could not resolve) |

## Tests

| File | Change | Finding |
| --- | --- | --- |
| `frontend/test/risk_case_notifiers_test.dart` | data-model rename; `state.hasError` → `state.error, isNotNull` | F2, F3 |
| `frontend/test/risk_case_widgets_test.dart` | data-model rename; removed unused `api_contract.dart` import | F2, F4 |
| `frontend/test/api_contract_test.dart` | data-model rename | F2 |
| `frontend/test/support/fake_risk_case_repository.dart` | data-model rename | F2 |

## Left untouched (legitimately name the screen widget `RiskCaseListPage`)

- `frontend/lib/app/app_router.dart`
- the `class RiskCaseListPage extends ConsumerStatefulWidget` and its State in
  `frontend/lib/features/riskcase/presentation/risk_case_list_page.dart`

## Infrastructure (fixed earlier in this review arc)

| File | Change |
| --- | --- |
| `scripts/verify-infrastructure.sh` | export placeholder `KEYCLOAK_ADMIN_PASSWORD` / `KEYCLOAK_OPERATOR_PASSWORD` so `docker compose config` interpolates the new required-with-error vars |
| `docker-compose.yml` | (earlier) parameterized host ports |

## Authority basis

- F3, F4 are test-file-only, zero-business-impact, coverage-preserving → within
  the standing test-maintenance authority (§16.5-A).
- F1, F2 touch production files. They are unambiguous defect fixes (the frontend
  would not otherwise compile), each verified up to `flutter analyze` clean, and
  are surfaced here in full for the Product Owner's explicit accept/route decision
  rather than treated as silently authorized.
- No commit, stage, or push was performed by the reviewer.
