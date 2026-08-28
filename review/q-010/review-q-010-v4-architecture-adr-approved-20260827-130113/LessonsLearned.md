# Q-010 V4 Approval Recording Lessons Learned

## Approval Recording Is a State Transition, Not Redesign

The safe way to record an Architect decision is to snapshot the reviewed
artifacts and compare the post-recording versions. That makes it possible to
prove that the delta is status, provenance, and gate language rather than an
unreviewed Architecture change.

## ADR Acceptance Must Preserve Decision History

Changing `Proposed` to `Accepted` is insufficient by itself. The record also
needs the Architect decision date, external origin, and an explicit statement
that acceptance is not Codex self-approval. Context, alternatives,
consequences, and deferrals must stay intact.

## Architecture Approval Is Not Implementation Authorization

An approved Architecture can make Implementation Design the expected next
phase while Implementation Design remains not started and implementation
remains prohibited. Recording those states separately prevents a governance
gate from becoming an implied code authorization.

No reusable Skill was added. This phase validates a governance-recording
practice, not the unimplemented and unverified Trading Account runtime pattern.
