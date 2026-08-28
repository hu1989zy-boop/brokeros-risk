# Q-010 Implementation Design Approval Recording Lessons Learned

## Decision and Boundary

The external Architect approved the exact Q-010 Implementation Design V1 that
was submitted through V5. Q-010 V6 records that supplied decision; Codex does
not approve its own work.

The approval closes the Design decision gate only. Implementation remains NOT
STARTED and Implementation Allowed remains NO pending independent review of
the V6 approval-recording package and a separate explicit implementation
authorization. Q-008 remains unimplemented and prerequisite-gated.

## Approval Must Identify Exact Content

A summary can establish review intent, but it cannot independently prove which
large design body was approved. The V5 ZIP contained complete Review summaries
but referenced the 1217-line authoritative Design only by repository path.

V6 therefore preserves two distinct hashes:

1. the pre-recording V5 Design hash, proving the exact draft supplied for
   approval; and
2. the current authoritative Design hash after status/provenance-only edits.

The V6 Review also contains a byte-identical snapshot of the current
authoritative Design. A companion artifact manifest proves equality. The copy
is Review evidence only; the document under `docs/architecture/` remains the
single authority.

## Preserve Decisions While Advancing Gates

Approval recording should modify status, provenance, and next-gate text only.
The domain, persistence, canonicalization, transaction, concurrency,
idempotency, authorization, Q-008 disclosure, error, logging, Flyway, security,
and test decisions remain byte-for-byte unchanged outside the status/gate
regions identified by the preflight comparison.

This separation prevents a bookkeeping phase from silently redesigning an
approved artifact while still allowing active governance documents to show the
current gate consistently.

## Skill Assessment

No repository Skill was added or modified. Architect approval confirms the
Design decision but does not provide implementation or runtime evidence for a
new reusable pattern. The Q-010 transaction/idempotency guidance should be
reassessed only after separately authorized implementation and real MySQL 8.4
verification.

No Java, test, SQL, Flyway migration, endpoint, provisioning runtime,
configuration, dependency, Docker/Kubernetes behavior, Q-008 implementation,
staging, commit, or push was performed.
