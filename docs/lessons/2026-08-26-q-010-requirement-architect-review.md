# Q-010 Requirement Architect Review Lessons Learned

## Decision

Q-010 Trading Account Reference Authority Foundation is the correct next
Requirement and is approved as V1. It supplies one missing upstream subject
authority without absorbing Trading Account master data, Evidence, Decision,
Action, ActionOutcome, Risk Case, or external adapter behavior.

The approval resolves the Requirement-level identity tuple, one-to-one
cardinality, lifecycle/history, controlled non-web registration authority,
consumer disclosure, Q-009 capabilities, durable mutation provenance, and
fail-closed outcomes. Architecture must design within those decisions rather
than reopen them.

## Governance Lesson

An active design document may preserve the status it had when submitted if a
later immutable approval record explicitly says that the earlier status is a
historical snapshot and identifies the authoritative current gate. The Q-008
Architect Approval record does exactly that for the Implementation Design
header and Section 17. A metadata repair would erase useful chronology and is
not required.

Before treating two documents as inconsistent, inspect the later approval
record for an explicit authority and snapshot rule. Only repair active metadata
when no such authoritative classification exists.

## Identity-Authority Lesson

A raw external account key cannot be made safe merely by adding a source-name
string. A durable reference boundary needs all three dimensions:

1. an opaque BrokerOS-owned authority-scope reference;
2. a governed source-instance namespace; and
3. the external account key within that namespace.

The complete tuple must have explicit uniqueness, cardinality, lifecycle, and
reuse rules. For the Foundation, one-to-one mapping and no reassignment are
safer than speculative aliasing or account-merge support.

## Registration Lesson

Authorization answers who may invoke registration; it does not prove that the
mapping is true. The initial boundary therefore requires both a pre-provisioned
Q-009 actor with an exact capability and a broker/source-owner-approved record
represented by bounded provenance. If deployment governance cannot supply
that real attestation, implementation must remain blocked.

## Scope and Verification

This review changed governance documentation only. It added no Java, test,
Flyway migration, dependency, endpoint, configuration, Kafka, Redis, adapter,
or infrastructure artifact. Architecture, ADR creation/acceptance,
Implementation Design, implementation, staging, commit, and push were not
started.
