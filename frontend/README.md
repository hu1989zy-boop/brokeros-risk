# BrokerOS Risk Console

Q-016 establishes a Flutter web-first operator console. It is a thin client of
the BrokerOS Risk APIs: it renders backend-owned state, submits commands with
`expectedVersion`, and never decides authorization or Risk Case transitions.

## Local development

Prerequisites: Docker Compose v2 and Flutter stable with Chrome web support.

1. From the repository root, copy `.env.example` to `.env` and set the four
   local-only values. Do not commit `.env`.
2. Run the whole development slice:

   ```bash
   ./scripts/run-risk-console-dev.sh
   ```

The command starts MySQL, Redis, Kafka, Keycloak 26.7.3, provisions the seeded
operator in the existing security authority, starts the backend with its
development-only CORS profile, generates immutable Dart model code, and opens
Flutter web on `http://localhost:4173`.

The seeded browser username is `q016-operator`; its password is the
`KEYCLOAK_OPERATOR_PASSWORD` value from the untracked local `.env`. The realm is
a local-development fixture only. The operator is granted only
`risk-case:read` and `risk-case:note`.

Configuration is supplied through compile-time values:

- `BROKEROS_API_BASE_URL` (default `http://localhost:8080`)
- `BROKEROS_OIDC_ISSUER` (default
  `http://localhost:8180/realms/brokeros`)
- `BROKEROS_OIDC_CLIENT_ID` (default `brokeros-risk-console`)

The browser uses OIDC Authorization Code with PKCE. Access tokens remain in
memory; only the refresh token is placed in platform secure storage. API
requests add the Bearer token in the HTTP interceptor, never in a request body.

## Verification

With Flutter installed:

```bash
flutter pub get
dart run build_runner build --delete-conflicting-outputs
flutter analyze
flutter test
flutter build web
```

Generated Freezed/json-serializable sources and `pubspec.lock` are produced by
the first setup run. Q-016 pins every direct dependency in `pubspec.yaml`.
