# Q-020 Implementation Review Summary

## Review boundary

- Requirement: Q-020 External-Reference Scoped Search / Browse V1.
- Lifecycle stage: implementation and implementation verification only.
- Authorized inputs: approved Q-020 Requirement V1, ADR-022, Architecture V1,
  and Implementation Design V1.
- Intended consumer: Claude Code's independent implementation review.
- No independent-review acceptance, final closure, staging, commit, or push is
  claimed by this package.

## Outcome

Q-020 V1 is implemented as four additive, content-free scoped-list endpoints and
a case-scoped console browse/pick flow. The implementation reuses the existing
module `READ` capabilities, the existing authenticated `ApiClient`, and the Q-018
preview and association runner. Manual reference entry remains available.

Gate Decision: **PASS**

This is the implementation-stage verification decision only. Gate advancement
and deliverable acceptance remain with the Product Owner after independent review.

## Acceptance criteria

1. **PASS** — all four endpoints returned bounded, deterministically ordered
   references and minimal metadata against real MySQL; unknown valid keys returned
   `items: []`; malformed keys returned the module request-invalid code; denied
   reads returned `AUTHORIZATION_DENIED`.
2. **PASS** — source-boundary checks found no provenance or Risk Case domain/
   aggregate diff, no write-service or association-runner diff, no capability diff,
   and no migration. Backend production changes are limited to list services,
   summaries, port extensions, JDBC reads, DTOs, controller methods, and wiring.
3. **PASS** — list SQL names only reference, scope, optional status, and timestamp
   columns. Response records have the same minimal shape. Real-MySQL assertions
   reject the four content properties.
4. **PASS** — `ReferenceInput` defaults to scoped browse when a natural scope exists,
   selects into the existing preview flow, and retains manual mode. Case subject,
   current/associated decision, and on-case action are sourced from case detail and
   the Q-019 authoritative projection. The Q-018 request body is unchanged.
5. **PASS** — focused MySQL tests passed 4/4; the full backend suite passed 317/317;
   the frontend passed 156/156, strict typecheck, and the production build.

## Delivered areas

- Backend: `REFERENCE_LIST_MAX = 200`; four summary projections and list services;
  four query-port/JDBC list methods; four list DTOs and endpoint methods; module
  wiring; contract and real-MySQL coverage.
- Frontend: typed `ReferenceListRepository`; repository provider; four conditional
  TanStack Query hooks; browse/manual UI; least-authority scope projection and
  dialog wiring; repository, component, projection, and unchanged-request tests.
- Documentation: Q-020 implementation Lessons Learned and this timestamped package.

## Verification headline

- Focused backend real-MySQL: 4 tests, 0 failures, 0 errors, 0 skipped.
- Full backend real-MySQL-enabled suite: 317 tests, 0 failures, 0 errors, 0 skipped.
- Frontend focused: 52 tests, 0 failures.
- Frontend full: 13 files / 156 tests, all passed.
- TypeScript, Vite build, repository static verification, and `git diff --check`:
  passed.

See `Verification.md`, `TestInventory.txt`, and `OutstandingItems.md` for exact
evidence, warnings, failed intermediate attempts, and review attention items.
