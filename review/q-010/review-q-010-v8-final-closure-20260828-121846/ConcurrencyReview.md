# Q-010 V8 Concurrency and Transaction Review

## Concurrency guarantees

| Guarantee | Final arbiter/evidence | Status |
| --- | --- | --- |
| same external identity concurrent provisioning | named complete-tuple unique constraint; concurrent barrier test | PASS |
| one TradingAccountRef cannot map to another tuple | generated-ref unique constraint; collision test capped at three | PASS |
| one external identity cannot map to another ref | complete-tuple unique constraint; compatible/conflicting registration tests | PASS |
| concurrent duplicate operation | unique operation ID plus post-race durable replay reread | PASS |
| lifecycle lost-update prevention | status + expected-version CAS; one winner/one conflict | PASS |

Database constraints remain the final uniqueness arbiter. Application prechecks
improve diagnosis but do not elect a winner. No distributed lock, Redis lock,
last-write-wins, or automatic retry with a fresh version exists.

## Atomicity and rollback

- business current state, final operation outcome, and exactly one history row
  share one `TransactionTemplate` transaction;
- forced operation-outcome insertion failure leaves no business state;
- forced history insertion failure removes both business state and operation;
- race losers roll back before authoritative replay/conflict reread;
- no tested failure leaves partial durable state.

The 10 persistence/concurrency tests and one full command test passed on real
MySQL 8.4.11 with 0 failures, errors, or skips. Result: **PASS**.
