# Q-016 (React) v6 — Outstanding Conditions

Gate is **PASS WITH CONDITIONS**. C1 (run the frontend tests in a real browser)
is now **RESOLVED** — the unit/component/contract suite runs and passes on arm64
(27/27). One condition remains.

## C2 — Live Keycloak → backend → MySQL browser slice (AC 2, AC 6)

Codex delivered the Playwright spec (login → list → detail → add-note) and it is
discovered correctly, but its one test is **honestly skipped** because it needs
external inputs that were absent:

- a running dev stack: `docker compose --profile console up` (Keycloak +
  `console-backend` + MySQL), which requires the dev-only env values
  (`KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_OPERATOR_PASSWORD`, MySQL creds) in a root
  `.env`;
- `E2E_OPERATOR_PASSWORD` for the seeded operator;
- `E2E_CASE_NUMBER` — an existing Risk Case to open.

To satisfy C2, on a host with Docker + the dev `.env`:
```bash
docker compose --profile console up -d          # Keycloak + backend + MySQL
# seed/choose one Risk Case number, set it as E2E_CASE_NUMBER
cd frontend
E2E_OPERATOR_PASSWORD=<dev> E2E_CASE_NUMBER=<case> npm run test:e2e
```
Then confirm: Keycloak login via Auth Code + PKCE; Bearer accepted by the backend
(Q-009); list/filter/paginate; open detail/history/associations; add note; force
a version conflict; `401` silent refresh; `403` typed error; logout.

## Note

No defects were found in this review; the reviewer changed nothing. The delivery
is in the working tree (Flutter removed, React added), uncommitted, ready for
Product Owner acceptance and commit.
