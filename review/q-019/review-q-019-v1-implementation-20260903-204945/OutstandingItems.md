# Q-019 Outstanding Items

## Gate condition

1. **AC4 live verification is outstanding.** Run
   `frontend/tests/e2e/q019AssociationProjectionResolve.spec.ts` against the full
   local Keycloak/backend/frontend stack with a real operator credential, a seeded
   eligible Risk Case, a referenceable Decision, and a referenceable Action. The
   required evidence is a non-skipped decision-associate → select-current →
   action-associate → projection-render → resolve → close result. Current result:
   one skipped, no live success claimed.

## Implementation assumption for independent review

2. **Overflow behavior:** the approved design gives 500 as an example sane cap but
   does not define a new overflow ResultCode. The implementation uses 500 per
   collection and the existing `RISK_CASE_INVARIANT_VIOLATION`/422 when exceeded.
   This avoids truncating an authoritative response and avoids inventing an API
   code. Independent review should confirm this technical choice.

## Non-blocking observations

3. Flyway warns that its configured version is tested through MySQL 8.1 while the
   disposable gate used MySQL 8.4. All eight migrations and 309 tests passed; no
   Q-019 dependency upgrade is authorized.
4. Vite reports a pre-existing/default chunk-size warning (774.41 kB minified for
   the base chunk). The build passes; performance/code-splitting is outside Q-019.

## No additional implementation debt identified

- Q-018 defect D2 is closed by the evidence association event-ref picker.
- No aggregate, migration, capability, write-path, or cross-module repair remains
  in the Q-019 implementation scope.
- Option B external-reference browse/search remains intentionally deferred to a
  separately governed Requirement.
- Claude Code independent review is still required before Product Owner closure;
  this package is not self-approval.
