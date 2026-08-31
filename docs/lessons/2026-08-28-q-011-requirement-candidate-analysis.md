# Q-011 Requirement Candidate Analysis Lessons Learned

## What Was Learned

After Q-010, BrokerOS has both a trusted Actor/authorization foundation and a
trusted Trading Account reference authority, but Q-008 still requires
Evidence, Decision, Action, and ActionOutcome authorities owned by their
proper capabilities. Re-running the same candidate-selection method used
before Q-010 — rather than assuming the first-named missing provider is the
right next step — showed that Evidence is now the only candidate whose
blocking prerequisite is resolved by completed work.

An earlier framing in this session's own conversation assumed "start with
Evidence" without re-deriving that conclusion. Re-checking against the
repository's own prior Q-010 candidate analysis surfaced the actual
dependency rule and prevented drafting a full Requirement on an unverified
assumption.

## Reusable Decision Rule (confirmed, unchanged from Q-010)

When sequencing missing provider capabilities:

1. identify the first consumer's non-optional reference;
2. choose a provider whose truth can be established without another missing
   business provider;
3. keep recognition/reference scope separate from master data and behavior;
4. require a real registration/source authority and reject unchecked
   strings;
5. preserve vendor isolation and historical resolvability; and
6. do not treat completion of one prerequisite as authorization for the
   blocked consumer.

## New Lesson: Scope the First Increment by Source, Not by Capability Name

A capability can be blocked as a whole while still having a narrow, honest
first increment. Evidence's blocker was "no concrete initial source without
inventing a generic bucket." Rather than wait for Trading Data
ingestion/Rule Engine, the correct move is the same one Q-008 already made
for intake (`MANUAL` vs `DECISION_DRIVEN`): split Evidence by source and
implement only the source that has no missing dependency (human-authored,
actor-attributed observation) while explicitly excluding automated sources
as a named non-goal rather than an implicit gap.

## Risks Preserved for Architect Review

The proposed Q-011 still needs explicit decisions for evidence content
shape, correction/supersession mechanics, provenance metadata boundaries,
sensitive-content access control, and ADR need. If these cannot be resolved
without inventing an unstructured evidence store, implementation must remain
blocked, exactly as the equivalent risk was preserved for Q-010.

No implementation, architecture, ADR, dependency, migration, API,
configuration, commit, or push was created by this analysis.
