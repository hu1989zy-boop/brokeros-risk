# Regression Verification

## Full Maven Regression

Command:

`cd backend && mvn verify`

Result:

- Build: SUCCESS
- Tests run: 58
- Failures: 0
- Errors: 0
- Skipped: 1
- Skipped test: `Q009MySqlIntegrationTests`

All executable unit, architecture, configuration, HTTP security-boundary,
Flyway contract, application context, and existing regression tests passed. The
single skipped test is mandatory for Q-009 completion, so a successful Maven
build does not make the overall verification pass.

## Security Test Evidence

- `SecurityArchitectureTests`: 2 passed
- `SecurityApplicationTests`: 6 passed
- `SecurityConfigurationTests`: 3 passed
- `SecurityBootstrapCommandTests`: 2 passed
- `SecurityDomainTests`: 7 passed
- `SecurityBoundaryIntegrationTests`: 10 passed
- `Q009MySqlIntegrationTests`: 1 skipped

## Dependency Evidence

`mvn dependency:tree '-Dincludes=org.springframework.security:*,com.nimbusds:*'`
completed successfully and resolved:

- Spring Boot security/resource-server starters 3.5.16
- Spring Security modules 6.5.11
- Nimbus JOSE JWT 9.37.4

## Repository Checks

- `git diff --check`: PASS
- `git diff --cached --check`: PASS; staging area empty
- shell syntax for verification scripts: PASS
- `scripts/verify-static.sh`: FAIL only on the pre-existing untracked
  `review/q-006-design/Q-009-V6-Approved-Design-Git-Baseline-Prompt.md`
  (two trailing-space lines and one blank line at EOF)

The unrelated V6 Prompt was not modified because V8 is verification-only and
the file is outside the approved Q-009 implementation baseline.

## Regression Gate

- Executed Maven regression: PASS
- Mandatory runtime coverage complete: NO
- Overall Verification: FAIL
