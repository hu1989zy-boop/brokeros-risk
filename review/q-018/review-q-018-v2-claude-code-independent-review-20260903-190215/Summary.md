# Q-018 — Claude Code Independent Implementation Review (v2)

- Requirement: Q-018 — Risk Console Association Management (Group C), V1
- Reviewed: Codex v1 delivery (`review-q-018-v1-implementation-20260903-164146`)
- Baseline: `7d886c9`
- Reviewer: Claude Code (external Architect role) — Date: 2026-09-03
- **Gate Decision: PASS** (after the Architect's prefix correction + Product Owner
  decision B1)

## Verdict

Codex delivered a correct Group C implementation and — to its credit — marked it
**BLOCKED**, surfacing **two contract defects that were rooted in my (the
Architect's) design**, not in Codex's code. Independent review **confirmed both**.
One (the reference prefixes) was a genuine design error I have now corrected and
re-verified; the other (an on-case ref not exposed by the backend) is resolved by
the Product Owner's B1 decision (manual-entry fallback, no backend change). The
governance model worked in the useful direction here: the implementer caught the
architect's mistake, and did so honestly rather than shipping something broken.

## Confirmed defects (both real; independently verified)

### D1 — Reference prefix mismatch (Architect design error → corrected)

My Q-018 documents specified `ev-/dc-/ac-/ao-`. The committed backend regexes are
`ev-` (V4) / **`dec-`** (V5) / **`act-`** (V6) / **`aoc-`** (V7) — I had verified
only evidence and assumed the other three. Codex followed the authority rule
(implement per the approved docs) and then honestly flagged the mismatch.

**Correction (Claude Code):** fixed the four Q-018 documents and the delivered
frontend — `actionInputs.ts` format patterns, `ReferenceInput` placeholders, and
the test fixtures (`Q018AssociationActions.test.tsx`, `AssociationsPanel.test.tsx`)
— from `dc-/ac-/ao-` to `dec-/act-/aoc-`. Prefix correctness is authoritative
(matches the backend migration regexes).

### D2 — On-case `associationEventRef` not exposed (design assumption → B1)

The Risk Case detail/history does not expose an evidence association's
`associationEventRef` (history exposes only `{version, eventType, affectedRef,
actorRef}`; detail exposes `currentDecisionRef` only). So the evidence-disposition
**on-case picker + preview** (FR-02 ideal) cannot be met without a backend change.
Per Product Owner decision **B1**, the disposition target uses the **manual-entry
fallback** (already implemented) — no backend change. Decision-selection and
action-for-outcome pickers *are* derivable from history and work. The
`AssociationsPanel` is an honestly-labelled bounded history reconstruction, not an
authoritative projection.

## Independently reproduced (fresh `npm ci`, after the correction)

| Check | Result |
| --- | --- |
| `npm ci` | 309 packages, no lockfile change |
| `tsc --noEmit` (strict) | **0 errors** |
| `vitest run` | **148 passed / 148 (12 files)** |
| `vite build` | PASS (1,578 modules) |
| Backend untouched | `git diff -- backend/` empty; no migration |
| Capability grant | operator += `{risk-case:associate, evidence:read, decision:read, action:read, action-outcome:read}` — verified against the services; least-privilege reads |

## Code review (correct)

- Six association descriptors → correct endpoints/bodies; every request carries
  `expectedVersion`; **no actor identity in any body**; path segments
  `encodeURIComponent`-escaped; run through the shared `useCaseAction` runner
  (Bearer, `401` refresh, `403`→typed error, version-conflict reload).
- `ReferenceInput`/`useReferencePreview`: format-validates then previews via the
  existing `GET /{ref}`; disabled submit until a valid preview; typed
  not-found/forbidden/invalid states. Preview shows identifying fields only.
- On-case decision/action pickers derived from detail/history; disposition target
  uses the guarded manual fallback (B1).
- No `localStorage` tokens, no `console.*`, no `dangerouslySetInnerHTML`/`eval`, no
  JWT-claims parsing/capability probe.

## Acceptance criteria — reviewer view (after correction + B1)

| AC | Result |
| --- | --- |
| 1 operations end-to-end | **PASS (component)** — 148 tests; full live happy-path needs the decision/action Core-Domain seed (below) |
| 2 preview + on-case selection | **PASS** — external preview + decision/action pickers; disposition = B1 manual fallback |
| 3 version-conflict reload | **PASS** — shared runner + tests |
| 4 unblock resolve | **PASS by construction** — decision-associate + select + action-associate are implemented; live confirmation needs the seed chain |
| 5 no backend change / no identity | **PASS** — backend diff empty; only the bootstrap grant |
| 6 tests + build | **PASS** — typecheck 0, 148/148, build; live Playwright spec present (skipped for missing seeded refs) |

## Note on live verification

The full live association happy-path (associate a real decision → select →
associate a real action → then Q-017 `resolve` reachable) requires seeding the
decision/action Core-Domain chain — the same precondition class as Q-017 `resolve`.
The prefix correctness (the actual blocker) is verified authoritatively against the
backend migration regexes plus 148 component tests and a clean build; the live
happy-path is covered by the delivered Playwright spec, to run when that chain is
seeded (candidate Q-019).

## Recommendation

**Accept Q-018 V1** as corrected. The blocker was my design error (prefixes), now
fixed and re-verified; D2 is resolved by B1. Codex's implementation is otherwise
correct and its honesty (BLOCKED, not faked) is exactly right. The only reviewer
changes were the prefix correction (docs + a few frontend/test literals); nothing
else in Codex's code was altered.
