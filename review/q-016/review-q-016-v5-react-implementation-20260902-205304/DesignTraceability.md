# Q-016 React Design Traceability

## Functional requirements

| Requirement | Implementation | Test/evidence | Result |
| --- | --- | --- | --- |
| Q016-FR-001 | `authConfig.ts`, `App.tsx`, `apiClient.ts`: OIDC code flow, Bearer, silent renew/one retry, logout | `authConfig.test.ts`, repository 401/retry/failure tests; live IdP outstanding | PASS WITH CONDITION |
| Q016-FR-002 | Add-note body is exactly `content` + `expectedVersion`; list uses filters/page only | Repository body assertion; source inspection | PASS |
| Q016-FR-003 | Bounded list/filter/page, detail + first 100 history entries, current decision and association references | List/detail loading/empty/error/success tests | PASS; full current-association projection is not available in the unchanged backend contract |
| Q016-FR-004 | Add-note mutation with displayed case version; typed ordinary error; conflict keeps input and refetches | Dialog state tests and detail success/conflict integration tests | PASS |
| Q016-FR-005 | Consumes delivered `GET /api/risk-cases`; no backend work | Empty `backend/` diff | PASS |
| Q016-FR-006 | Fixed page size 20, history limit 100, summary/detail DTO parsers, no vendor payload/business logic | Contract drift test and source inspection | PASS |
| Q016-FR-007 | React README + updated dev launcher, Vite port 4173 | Shell syntax/static checks; live full stack outstanding | PASS WITH CONDITION |

## React pivot addendum sections

| Addendum section | Trace | Result |
| --- | --- | --- |
| §1 Why this addendum exists | Flutter tracked tree removed; React implementation/lockfile added | PASS |
| §2 Target stack | `package.json` pins React, TypeScript, Vite, Router, Query, OIDC, axios, Table, Ant Design, Vitest/RTL/MSW/Playwright | PASS |
| §3 Repository layout | `frontend/src/app`, `core/api|auth|config`, `features/riskcase/api|model|ui`, `shared`, and `tests` match the prescribed ownership | PASS |
| §4 Carried over unchanged | Thin client; PKCE; backend endpoints, Keycloak/CORS/Compose unchanged; fixed bounded calls | PASS |
| §5 Testability | 27 Node/jsdom tests pass; Chromium installed/launched; live authenticated Playwright spec skipped for missing environment | PASS WITH CONDITION |
| §6 Architecture gate | Implementation stays within the accepted pivot bundle and stops at implementation-verification handoff | PASS |

## Vertical-slice mapping

1. Login: `LoginPage` calls only `signinRedirect`; credentials stay at Keycloak.
2. List: `RiskCaseListPage` → query hook → repository → bounded
   `GET /api/risk-cases`.
3. Detail: route page → repository → concurrent existing detail/history GETs.
4. Associations: current decision comes from detail; other opaque affected refs
   are labelled accurately as history references.
5. Operation: modal → mutation → existing note POST with `expectedVersion` →
   detail/list invalidation; conflict reloads and preserves note text.

## Deliberate implementation decisions

- Access/user state is memory-only; browser reload may require authentication
  again. This favors the approved security boundary over session convenience.
- Runtime parsers are used because TypeScript types alone cannot validate a
  server response.
- Query cache clears on subject changes so bounded sensitive case data cannot
  cross operator sessions.
- Feature routes are lazy loaded. No bundle-size target exists; the remaining
  Vite size warning is recorded rather than hidden.
