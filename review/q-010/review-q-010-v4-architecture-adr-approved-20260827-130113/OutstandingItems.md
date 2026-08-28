# Q-010 V4 Outstanding Items

## Approval-Recording Blockers

None.

## Next Required Phase

Q-010 Implementation Design remains pending and may begin only under a
separate explicit instruction after independent review of this V4 package.
Implementation remains prohibited and requires its own later Design approval
and explicit implementation authorization.

## Approved Architecture Deferrals

- exact Java/package/port/service/repository types;
- exact table/column/index/constraint names and Flyway version;
- manifest serialization, CLI entry point, exit behavior, and file handling;
- semantic fingerprint representation and bounded UUID collision retry;
- transaction/isolation/locking SQL and exception/ResultCode mapping;
- exact-byte external-key persistence and query plans; and
- concrete tests and operational verification.

These are Implementation Design inputs, not V4 approval-recording changes.

## Future Requirement Scope

Aliases/merge/migration/reassignment, automated discovery/synchronization,
source adapters, online administration, master data, cache/events,
cryptographic attestation, retention/redaction, and federation remain outside
Q-010 Foundation.

## Q-008

Q-008 remains unimplemented and prerequisite-gated. Q-010 Architecture
approval supplies no runtime Trading Account authority and does not satisfy the
remaining Evidence, Decision, Action, ActionOutcome, compatibility, or
authorization prerequisites.

## Pre-existing Repository Issue

`scripts/verify-static.sh` was previously known to report whitespace only in
the historical committed
`review/q-006-design/Q-009-V6-Approved-Design-Git-Baseline-Prompt.md`. V4 must
record the rerun result and must not modify that unrelated file.
