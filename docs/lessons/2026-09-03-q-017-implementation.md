# Q-017 Implementation Lessons Learned

## Scope

Q-017 extended the Q-016 React Risk Console with the approved V1 Groups A+B+D:
case lifecycle actions, assignment/priority changes, and note correction. The
implementation consumed the existing backend operations unchanged and modified
only the development security bootstrap outside `frontend/`.

## What worked

- A declarative descriptor registry kept the eleven operation paths, input
  schemas, status availability, terminal flags, and error messages reviewable in
  one place while retaining typed repository methods for the real HTTP contract.
- One TanStack Query mutation runner made success invalidation and optimistic
  concurrency recovery consistent. A version conflict refetches the active case
  query before the dialog reports the conflict, while Ant Design Form preserves
  the operator's values for review and retry.
- Parameterizing the RTL/MSW matrix over all eleven descriptors produced direct
  evidence for success, pending controls, input validation, lifecycle errors,
  typed `403`, and conflict recovery without copying eleven test harnesses.
- Keeping status filtering in `CaseActionsBar` and all authorization in the
  backend preserved the thin-client boundary; no JWT capability parsing or
  speculative authorization endpoint was needed.

## Contract details that required care

- The approved design describes resolution evidence/action references as
  optional inputs, while the committed Java request DTO requires non-null sets.
  The console therefore presents optional fields but sends empty arrays when
  they are blank. This satisfies both the accepted UX and the actual backend
  contract without inventing association pickers.
- Resolution has no separate backend `reason` field. Its mandatory
  `resolutionSummary` is presented as the resolution summary and reason, while
  close and cancel send their explicit `reason` fields. No unsupported request
  member is added.
- History exposes note metadata and references, not the original note content.
  The correction entry point binds the selected `noteRef` and asks for a complete
  corrected note rather than pretending the original content is readable.

## Problems encountered

- An initial verification attempt used Jest's unsupported `--runInBand` flag
  with Vitest. The command failed before running tests; verification was rerun
  using the repository's exact `npm test` script and the invalid attempt is
  retained in the review evidence.
- Ant Design prefixes a loading button's accessible name with its loading icon.
  Pending-state tests initially searched for the non-loading accessible name and
  failed. Selecting the modal's primary submit control made the assertion test
  the intended disabled state without coupling it to icon-generated text.
- A note reference appears both in the notes panel and the complete history
  timeline. A test that assumed one occurrence was corrected to assert presence,
  matching the intentional two-view UI.

## Verification boundary

The Node component/unit suite and production build ran locally. The Q-017 live
Playwright lifecycle spec is delivered, but the required live Keycloak password
and seeded eligible OPEN case were not present, so Playwright reported the live
spec as skipped. This is an environment prerequisite gap, not evidence that the
authenticated lifecycle path passed.

## Reusable guidance evaluation

No new repository skill was necessary. The existing
`docs/skills/react-risk-console-development.md` already governs the reusable
thin-client, typed repository, `expectedVersion`, conflict preservation,
server-authoritative authorization, cache invalidation, and honest Playwright
verification patterns used here. Q-017-specific status maps and terminal fields
remain in its accepted Design and action registry rather than being generalized
prematurely.
