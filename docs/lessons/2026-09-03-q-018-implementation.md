# Q-018 Implementation Lessons Learned

## Scope

Q-018 extended the Q-016/Q-017 React Risk Console with the six approved Group C
association operations, bounded external-reference previews, on-case selectors,
and an association panel. The implementation reused the existing Q-017 action
registry and mutation runner, changed no backend Java or migration, and added
only the approved operator capabilities under `deploy/`.

## What worked

- Adding Group C descriptors to the existing registry preserved one execution
  path for Bearer authentication, `expectedVersion`, typed backend errors, cache
  invalidation, and version-conflict input preservation.
- A small typed preview repository and one debounced `ReferenceInput` made all
  four external reference kinds use the existing authenticated `GET /{ref}`
  APIs. Submit remains disabled until an exact response-reference match is
  parsed and displayed.
- Keeping association operations inside `AssociationsPanel` left the Q-017
  lifecycle action bar stable and made the association context and operations
  available together.
- The parameterized RTL/MSW matrix exercised all six operations against the
  exact path/body contracts, including success, pending, validation, ordinary
  `ResultCode`, `403`, and conflict/retry behavior.

## Contract findings that materially limit Q-018

- The approved Q-018 documents require `ev-`, `dc-`, `ac-`, and `ao-` external
  references. The committed backend domain accepts `ev-`, `dec-`, `act-`, and
  `aoc-`. Following the prompt's authority rule keeps the new Q-018 client
  validation on the approved short prefixes, but the three non-evidence preview
  and association flows therefore cannot interoperate with the current backend.
  This is a governance-versus-runtime contract blocker, not a client-side alias
  opportunity; silently translating opaque identifiers would be unsafe.
- Risk Case detail/history exposes the current decision and affected decision,
  action, and evidence references. It does not expose an evidence association's
  `associationEventRef`, so the disposition target cannot be populated as the
  approved on-case picker. No existing GET endpoint can preview that event ID;
  the bounded fallback is a manually entered canonical UUID with client format
  validation and an explicit UI warning.
- History records an action ref for `OUTCOME_REFERENCED`, not the outcome ref,
  and records the original evidence ref for a disposition, not the replacement
  evidence ref or association event ID. It is therefore not a complete current
  association projection. The panel labels the view as reconstructed from the
  loaded history page and treats only `currentDecisionRef` as authoritative.

## Problems encountered

- Reusing the Q-018 `ac-` action regex as the existing Q-017 resolution regex
  initially caused five Q-017 `resolve` tests to fail. The rules were separated:
  Q-017 retains its committed `act-` contract while Q-018 uses the explicitly
  approved `ac-` input contract. The targeted regression suite then passed.
- Evidence and action association histories both use the event name
  `WITHDRAWN`. The client must use previously observed association context when
  classifying that event and must not infer the association kind from the event
  name alone.

## Verification boundary

The Node unit/component suite, strict TypeScript check, and production Vite build
are executable without a browser. The live association slice requires the full
Keycloak/backend stack, credentials, a seeded eligible case, and real source
entities. Even with those inputs, the approved/client and committed/backend
prefix mismatch must be governed before the decision/action steps can pass.

## Reusable guidance evaluation

No new repository skill was added. The existing
`docs/skills/react-risk-console-development.md` already covers the reusable
thin-client action runner, typed boundary parsing, cache invalidation, input
preservation, secret-safe test design, and honest live-verification rules. The
newly discovered reference-prefix and history-shape gaps are Q-018 contract
findings that require governance/API alignment rather than a generalized coding
workflow.
