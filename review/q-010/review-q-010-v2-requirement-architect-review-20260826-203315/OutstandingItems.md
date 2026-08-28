# Q-010 V2 Outstanding Items

## Requirement Gate Blockers

None. Q-010 Requirement V1 is approved.

## Required Before Architecture Work

- Obtain separate Product Owner authorization to execute the Architecture +
  ADR Analysis Prompt.

## Architecture Inputs — Not Requirement Reopeners

1. Select opaque formats and validation for `TradingAccountRef`,
   `AccountAuthorityScopeRef`, and `SourceNamespace`.
2. Define controlled authority-scope bootstrap and source-owner provenance
   validation without storing credentials/vendor payloads.
3. Define the non-web manifest, exact idempotency fingerprint, concurrency,
   transaction, and conflict diagnostics.
4. Select lifecycle code names and decide whether reactivation is implemented
   initially or remains disabled.
5. Define the protected internal query contracts and bounded version/provenance
   fields.
6. Design additive MySQL constraints, normalization/collation, indexes,
   immutable history, retention, and query plans.
7. Create a proposed ADR for Architect review; do not mark it Accepted.

If no deployment can supply a controlled registrar and broker/source-owner-
approved record, implementation must remain blocked. Architecture may not
replace that authority with unchecked input or direct external-database access.

## Explicitly Not Started

- Q-010 Architecture or ADR creation/acceptance;
- Implementation Design, implementation, runtime verification, or staging;
- Q-008 implementation or authorization;
- Java, tests, Flyway, API, dependency, configuration, Kafka, Redis, adapter,
  infrastructure, commit, or push.
