# Scripts

Repository-owned verification entry points:

- `verify-static.sh [base-commit]` — validates working/staged whitespace, the
  relevant commit range, and POSIX shell syntax.
- `verify-kustomize.sh` — renders and checks base/test/prod Kubernetes contracts
  with `kubectl kustomize`.
- `verify-infrastructure.sh` — generates ephemeral credentials, validates
  Compose, starts an isolated full stack, checks MySQL/Flyway, Redis, Kafka, and
  backend health, then removes only that isolated Compose project.
- `run-risk-console-dev.sh` — starts the local-only Keycloak, provisions the
  seeded Q-016 operator capabilities, starts the development backend, and runs
  the React web console. Copy `.env.example` to `.env` first. The seeded
  browser login is `q016-operator` with `KEYCLOAK_OPERATOR_PASSWORD` from the
  local `.env`; it is development-only and has only `risk-case:read` and
  `risk-case:note`.

Run Maven checks separately because they are explicit blocking CI steps:

```bash
cd backend
mvn test
mvn package
```

The infrastructure script requires Docker Compose v2 and OpenSSL. It never
connects to production infrastructure, creates no business table/topic/key, and
does not print generated credentials.
