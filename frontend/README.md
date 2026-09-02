# BrokerOS Risk Console (React)

Q-016 provides a web-first, thin React 18 + strict TypeScript Risk Console. It
uses Vite, React Router, TanStack Query, TanStack Table, Ant Design, axios, and
`react-oidc-context`. The backend remains the authority for authentication,
authorization, validation, transitions, and optimistic concurrency.

## Prerequisites

- Node.js 20.19+ (Node 26.5.0 was used for the implementation verification)
- npm
- Docker with Compose for the existing local Keycloak/backend profile
- A repository-root `.env` copied from `.env.example` with local-only values
  for the required MySQL and Keycloak variables

No password, token, or client secret belongs in this directory or its runtime
configuration. `brokeros-risk-console` is an OIDC public client.

## Install and run the frontend

```bash
cd frontend
npm ci
npm run dev
```

Vite listens on `http://localhost:4173`, matching the already-delivered dev
Keycloak redirect URI and backend CORS origin. The defaults are:

| Setting | Default |
| --- | --- |
| API base URL | `http://localhost:8080` |
| OIDC authority | `http://localhost:8180/realms/brokeros` |
| OIDC public client | `brokeros-risk-console` |
| OIDC redirect URI | browser origin (`http://localhost:4173`) |

For deployment, replace `public/runtime-config.js` in the static artifact
before the app starts:

```js
window.__BROKEROS_CONFIG__ = {
  apiBaseUrl: 'https://risk-api.example.test',
  oidcAuthority: 'https://identity.example.test/realms/brokeros',
  oidcClientId: 'brokeros-risk-console',
  oidcRedirectUri: 'https://risk-console.example.test',
};
```

The file contains endpoints and a public client identifier only. Never put a
credential or token in it. `VITE_API_BASE_URL`, `VITE_OIDC_AUTHORITY`,
`VITE_OIDC_CLIENT_ID`, and `VITE_OIDC_REDIRECT_URI` are build-time fallbacks.

## Start the existing dev Keycloak and backend

From the repository root, after creating `.env`:

```bash
docker compose --profile console up --build --wait -d mysql redis kafka keycloak
docker compose --profile console exec -T keycloak /bin/bash -c \
  '/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password "$KC_BOOTSTRAP_ADMIN_PASSWORD"'
docker compose --profile console exec -T keycloak /bin/bash -c \
  '/opt/keycloak/bin/kcadm.sh set-password --realm brokeros --username q016-operator --new-password "$KEYCLOAK_OPERATOR_PASSWORD"'
docker compose --profile console run --build --rm security-bootstrap
docker compose --profile console up --build --wait -d console-backend
```

Then run `npm run dev` from `frontend/`, open `http://localhost:4173`, and sign
in as `q016-operator` using the local password from `.env`. Keycloak performs
Authorization Code + PKCE; the console never receives the password. The local
database must contain at least one Risk Case to exercise detail and add-note.

Stop the local profile when finished:

```bash
docker compose --profile console down
```

## Verification

```bash
npm run typecheck
npm test
npm run build
```

Vitest, React Testing Library, and MSW run in Node/jsdom without a browser.
The tests cover the typed `ApiResponse` contract, Bearer/401/403 behavior,
bounded list and detail loading/empty/error/success states, add-note success,
and `RISK_CASE_VERSION_CONFLICT` recovery.

The Playwright spec is a live test against the running dev console, Keycloak,
and backend. It intentionally does not record traces, screenshots, or video so
credentials and tokens cannot enter test artifacts. Provide values through the
process environment, not source or command history:

```bash
E2E_OPERATOR_PASSWORD='<local-dev-password>' \
E2E_CASE_NUMBER='<existing-case-number>' \
npm run test:e2e
```

`E2E_OPERATOR_USERNAME` defaults to the non-secret dev username
`q016-operator`. The selected case must be visible through the bounded list and
the test appends one investigation note to the local dev database.
