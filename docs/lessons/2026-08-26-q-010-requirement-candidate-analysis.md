# Q-010 Requirement Candidate Analysis Lessons Learned

## What Was Learned

After Q-009, BrokerOS has a real trusted Actor/authorization foundation, but a
trusted caller does not make an opaque business reference authoritative. Q-008
still requires independent Trading Account, Evidence, Decision, Action, and
ActionOutcome authorities owned by their proper capabilities.

The smallest next step is not necessarily the Core Domain's most valuable
feature. A Trading Account reference authority has lower semantic dependency
than Evidence or Decision and supplies the typed subject identity that future
risk capabilities need without inventing trading data, scoring, or execution.

## Reusable Decision Rule

When sequencing missing provider capabilities:

1. identify the first consumer's non-optional reference;
2. choose a provider whose truth can be established without another missing
   business provider;
3. keep recognition/reference scope separate from master data and behavior;
4. require a real registration/source authority and reject unchecked strings;
5. preserve vendor isolation and historical resolvability; and
6. do not treat completion of one prerequisite as authorization for the
   blocked consumer.

## Risks Preserved for Architect Review

The proposed Q-010 still needs explicit decisions for source namespace,
broker/tenant scope, external identity uniqueness, lifecycle, mapping
cardinality, initial registration authority, and ADR need. If these cannot be
resolved without inventing a source of truth, implementation must remain
blocked.

No implementation, architecture, ADR, dependency, migration, API,
configuration, commit, or push was created by this analysis.
