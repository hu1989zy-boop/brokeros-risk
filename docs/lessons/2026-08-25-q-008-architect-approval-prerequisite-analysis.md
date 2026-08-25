# Q-008 Architect Approval and Prerequisite Analysis Lessons Learned

## What Was Decided

The external Architect approved the exact Q-008 Implementation Design V4.
Approval resolves the Design Gate but does not authorize implementation. The
repository still lacks both a trusted Actor/authorization capability and
authoritative Trading Account, Evidence, Decision, Action, and ActionOutcome
reference providers.

Q-007 supplies the authoritative domain language and ownership direction, but
it intentionally supplies no executable contract or provider. The current
backend likewise has no Spring Security dependency, authentication context,
ActorContext, authorization service, or upstream domain capability.

## Why Approval and Authorization Stay Separate

A design can be internally correct while its production dependencies are not
available. Treating Design approval as implementation authorization would
encourage fake providers, caller-supplied actor identity, or unchecked opaque
references. Each would make audit and domain integrity appear complete while
removing the trust boundary that gives them meaning.

## Option Lessons

- Reusing a documentation-only domain baseline cannot satisfy a runtime
  prerequisite.
- A consumer-owned port is useful dependency inversion, but an interface alone
  is not an authoritative provider.
- A fake, always-true, or hard-coded adapter is not a temporary production
  implementation; it is an authorization and data-integrity bypass.
- Cross-cutting trusted identity/authorization deserves its own cohesive
  Requirement and dependency/ADR review.
- Evidence, Decision, Action, ActionOutcome, and Trading Account truth must be
  supplied by their owning capabilities, not implemented inside Risk Case.

## Reusable Architecture Rule

Before authorizing a capability that consumes references, verify all three
layers independently:

1. the semantic ownership contract exists;
2. a real production authority owns and can answer the query; and
3. runtime wiring fails closed when that authority is absent or unavailable.

Passing only the first or second layer is insufficient.

## Verification Lesson

The first sandboxed Maven run failed because Mockito/Byte Buddy could not attach
to the JVM or create its temporary directory. Re-running the unchanged
repository baseline outside that restriction passed all 26 tests. The initial
environment failure was not represented as a product defect or hidden from the
Review evidence.

## Future Risks

- Q-008 could be partially wired with permissive providers merely to make the
  application start.
- Request ID or Trace ID could be mistaken for a trusted actor despite ADR-007.
- A broad Q-009 could combine security and unrelated domain ownership into a
  dumping-ground foundation.
- Risk Case could start owning upstream records if provider sequencing is not
  assigned to the correct capability Requirements.

No business implementation, Q-009 draft, Design V5, migration, API, dependency,
or runtime configuration change was created in this phase.
