# Q-010 Trading Account Reference Authority Implementation Lessons

## Scope

Q-010 V7 implemented the already approved Trading Account Reference Authority
design. It did not implement Q-008 business behavior or add an external
platform adapter.

## What the implementation confirmed

1. Candidate generated references should be requested only after the
   transaction has rechecked both durable operation replay and compatible
   existing identity/attestation state. Generating earlier was harmless in
   production but consumed deterministic collision sequences during race
   tests and obscured the real algorithm. Moving generation immediately before
   insert made replay behavior and the three-attempt collision bound explicit.
2. Real MySQL 8.4 was necessary evidence. It verified the seven-table total
   schema, binary external-key storage, composite unique-key size, CHECK error
   3819, named duplicate constraints, concurrent arbitration, Flyway restart,
   and trigger-forced rollback. Static SQL inspection could not prove these.
3. Durable duplicate-operation replay has to be re-read after a losing
   transaction rolls back. It cannot be decided from a pre-insert SELECT or a
   generic duplicate exception alone.
4. A complete command test must exercise the registered Q-009 descriptor
   object, active service mapping, exact capability grant, strict manifest,
   database mutation, replay, revocation denial, and safe report together.
   Unit parser tests alone would miss trust-boundary wiring errors.
5. The repository infrastructure script originally described only V1/V2 and
   the Q-009 three-table baseline. An additive migration requires updating the
   verification authority to V1/V2/V3 and seven tables. A temporary Compose
   overlay can avoid host-port collisions without weakening the isolated
   internal health and schema checks.

## Final closure outcome

The external Architect approved the exact V7 implementation on 2026-08-27.
V8 verified the V7 artifact hashes before changing governance metadata, then
re-ran the full Java 21 Maven suite, all 11 mandatory Q-010 MySQL 8.4.11 tests,
Kustomize, dependency, and isolated Compose gates. The unchanged historical
Q-009 whitespace finding remains the only static-script exception.

The immutable V7 hashes made the approval provenance materially stronger than
relying on timestamps or a mutable working-tree description. Final closure
also confirmed that database concurrency tests must remain named against the
actual guarantees: tuple uniqueness, duplicate operation replay, generated-ref
collision bounds, lifecycle CAS, and transactional rollback are distinct
proofs even when one integration class executes them together.

Q-010 Final Closure is PASS / CLOSED as of 2026-08-28 and is ready for the
independent Architect Final Review of V8. No commit or push was performed.
