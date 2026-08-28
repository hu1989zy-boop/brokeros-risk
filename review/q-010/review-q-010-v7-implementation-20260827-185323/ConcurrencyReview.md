# Q-010 V7 Concurrency Review

## Registration arbitration

The adapter performs operation/identity checks inside each transaction but
treats MySQL uniqueness as final authority. Only root MySQL error 1062,
SQLState 23000, and an exact approved constraint name can be classified.

- Same complete identity, different operation IDs: one row is created and the
  compatible loser commits `UNCHANGED` with its own history.
- Same operation ID and fingerprint: one transaction commits; the loser
  re-reads and returns the exact durable result without another history row.
- Generated ref collision: only exact generated-ref unique constraints trigger
  a new candidate/full transaction, capped at three attempts.
- Same tuple with different immutable registration attestation: mapping
  conflict, no operation/history commit.

## Lifecycle arbitration

The update predicate includes internal ID, expected version, and expected
status. Concurrent distinct valid transitions from version 0 produce exactly
one version-1 update and one version conflict. There is no blind retry,
last-write-wins, distributed lock, or Redis dependency. A concurrent identical
operation may replay only after the losing transaction rolls back and the
durable operation is re-read.

## Atomicity

Current state, operation outcome, and immutable history share one
TransactionTemplate. Forced operation insert failure and forced history insert
failure both roll back the current-state insert. Unknown integrity, deadlock,
timeout, and connection/commit uncertainty fail unavailable for exact operator
retry with the same manifest/operation ID.

Concurrency review result: **PASS** based on executed MySQL 8.4 tests.
