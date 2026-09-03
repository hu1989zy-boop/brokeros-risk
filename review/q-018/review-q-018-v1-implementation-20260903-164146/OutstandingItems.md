# Q-018 Outstanding Items

## Blocking contract and acceptance items

1. **Reference-prefix governance mismatch.** Q-018 requires `ev-/dc-/ac-/ao-`;
   the committed backend accepts `ev-/dec-/act-/aoc-`. This blocks decision,
   action, and outcome preview/association against the real backend. Technical
   recommendation: amend Q-018 to consume the established backend prefixes,
   because changing foundational persisted identifiers would have broader
   compatibility/data-migration risk. Product Owner/architect authorization is
   required before changing the approved frontend contract.
2. **Disposition target is unavailable.** Detail/history does not expose
   `associationEventRef`; no existing GET endpoint can preview it. Q-018's
   approved on-case picker + manual-preview fallback cannot be implemented under
   the zero-endpoint boundary. The delivered manual UUID field is format-checked
   and explicitly warned, but AC2/FR-02 remain FAIL.
3. **No authoritative current association projection.** History omits evidence
   replacement/event links and action outcome refs and is limited to 100 entries.
   The panel can only show a labelled bounded reconstruction. FR-01's effective
   evidence and FR-07's complete current associations are not proven/satisfied.
   Recommend an authorized additive bounded projection or an explicit requirement
   revision; neither is allowed by this prompt.
4. **Live association slice not executed.** Supply the live operator password,
   a seeded eligible case, and real decision/action entities only after items 1–3
   are governed. Apply the new capabilities and rerun the Playwright slice. AC1,
   AC4, and AC6 cannot pass on current evidence.
5. **Independent implementation review remains required.** This package is the
   Codex implementation handoff, not Claude Code review or Product Owner
   acceptance.

## Recorded assumptions and bounded decisions

1. Per prompt authority order, new external Q-018 inputs enforce the approved
   short prefixes even though backend inspection found conflicting longer
   prefixes. Existing Q-017 resolution retains `act-`; the rules are intentionally
   separate to avoid changing previously accepted behavior.
2. Because an association event has no approved GET endpoint, the manual
   disposition target is not falsely labelled preview-confirmed. The UI explains
   the gap and lets the backend remain authoritative if an operator has an event
   ID from another authoritative source.
3. `DECISION_ASSOCIATED.affectedRef` plus `detail.currentDecisionRef` are treated
   as the visible decision candidates. `ACTION_ASSOCIATED` and
   `OUTCOME_REFERENCED` affected refs are treated as visible action candidates.
   If `nextCursor` is present, both picker and display may be incomplete and the
   UI warns.
4. Evidence and action histories both use `WITHDRAWN`. Projection classification
   uses a previously observed action association when possible; otherwise the
   event is shown in evidence history. It does not infer ownership from the
   conflicting Q-018/backend prefixes.
5. No client-side relationship rule compares previewed subjects/decisions/actions.
   The backend remains authoritative for whether a recognized reference may be
   associated.

## Non-blocking observations

1. Vite reports a 772.91 kB minified base chunk above its default 500 kB warning
   threshold. No Q-018 bundle-size target exists; do not suppress or optimize it
   without a scoped performance requirement.
2. Node 26 reports an experimental process-localStorage warning during Vitest.
   It does not fail tests and Q-018 application code does not use localStorage.

## Explicitly out of scope and unchanged

- Backend Java, tests, Flyway, SQL, and existing REST endpoints.
- New list/search/current-association endpoints (Option B).
- Evidence, decision, action, or action-outcome creation.
- Group E Risk Case creation.
- JWT/capability-aware client authorization.
- Production IdP provisioning or deployment execution.

Do not begin another Requirement from this handoff.
