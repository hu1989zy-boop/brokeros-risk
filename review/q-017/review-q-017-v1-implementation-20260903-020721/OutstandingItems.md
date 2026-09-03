# Q-017 Outstanding Items

## Blocking acceptance evidence

1. The Q-017 live Playwright lifecycle slice has not executed. Provide
   `E2E_OPERATOR_PASSWORD` and `E2E_Q017_CASE_NUMBER`, apply the updated local
   bootstrap, and use a seeded eligible OPEN case. Execute login → assign →
   begin review → change priority → resolve → close and confirm version/history
   assertions. Until this passes, AC1 and AC6 remain **FAIL** and Q-017 must not
   be accepted as complete.
2. Claude Code independent implementation review is still required. This
   package is the handoff evidence, not the independent review or Product Owner
   acceptance.

## Recorded assumptions / contract reconciliations

1. `ResolveRiskCaseRequest` has no separate `reason` member. The mandatory
   `resolutionSummary` is labelled “Resolution summary and reason”; adding an
   unsupported reason field would violate the committed contract.
2. Accepted Design describes resolution evidence/action refs as optional UI
   inputs, while the committed backend DTO marks both sets `@NotNull`. Blank
   inputs serialize as empty arrays, preserving optional UX and backend shape.
3. The backend read contract exposes note references/metadata through bounded
   history but not original note text. Correction therefore asks for complete
   replacement content and binds the selected `noteRef`; it does not prefill
   unavailable content. Very old notes outside the first 100 history records
   are not discoverable from this V1 screen.
4. The live lifecycle spec expects its seeded OPEN case to have a current
   decision and all server-side resolution prerequisites. It intentionally does
   not create/associate data because Groups C and E are out of scope.

## Non-blocking observations

1. Vite reports a 768.28 kB minified base chunk above its default 500 kB warning
   threshold. Q-017 adds no bundle-size acceptance target. Future performance
   work should be requirement-driven; the warning was not suppressed.
2. Node 26 reports an experimental process-localStorage warning during Vitest.
   It does not fail tests and Q-017 feature code does not use localStorage.

## Out of scope by approval

- Group C evidence/decision/action association operations and pickers.
- Group E case creation.
- Capability-aware UI through JWT claims or a backend probe.
- Role differentiation and production identity-provider provisioning.
- Backend/Flyway changes and separate current-note/current-association read APIs.

No next Requirement should begin from this handoff.
