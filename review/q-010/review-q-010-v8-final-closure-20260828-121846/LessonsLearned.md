# Q-010 Final Closure Lessons

1. Immutable reviewed-file hashes provide stronger approval provenance than a
   mutable status narrative. V8 verified the V7 hashes before applying any
   governance-only status synchronization.
2. Real database evidence must name distinct guarantees. Same-identity races,
   duplicate operation replay, generated-ref collision bounds, lifecycle CAS,
   and forced rollback are separate proofs even when executed by one suite.
3. Authorization ordering is part of non-enumeration correctness: a denial
   test is incomplete unless it proves zero Q-010 query/mutation interaction.
4. A durable idempotency result is valuable precisely after uncertain delivery;
   it must share the business transaction and remain replayable after timeout.
5. Runtime verification should use the repository's intended command layout.
   A Java 21 attempt from a backend-only image context could not locate the
   repository root; the corrected root-level command passed without a code
   change.

These are concrete Q-010 lessons, not generic process filler.
