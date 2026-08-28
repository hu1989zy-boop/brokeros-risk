# Q-010 V7 Implementation Review

## Implemented components

1. Domain and value objects under `com.brokeros.risk.tradingaccount.domain`
   enforce canonical UUIDv4 refs, exact SourceNamespace rules, byte-exact
   ExternalAccountKey handling, lifecycle transitions, typed operation data,
   fingerprints, and bounded eligibility evidence.
2. Application services and ports under `tradingaccount.application` enforce
   Q-009 authorization before every query/mutation port interaction. Separate
   services cover scope/account registration, scope/account lifecycle,
   external-identity resolution, and Q-008 eligibility.
3. `JdbcTradingAccountAuthorityMutationAdapter` owns explicit
   `TransactionTemplate` units that atomically persist current state, one
   durable operation outcome, and one immutable history row.
4. `JdbcTradingAccountAuthorityQueryAdapter` returns only bounded immutable
   views, rejects impossible cardinality, and fails closed on invalid stored
   state or persistence failure.
5. `V3__create_trading_account_reference_authority.sql` creates exactly the
   four approved tables, named unique/FK/CHECK constraints, and indexes.
6. `TradingAccountAuthorityBootstrapCommand` accepts one strict 64-KiB-bounded
   non-symlink JSON manifest, uses `WebApplicationType.NONE`, obtains the
   registered Q-009 service actor, authorizes the exact capability, and emits a
   bounded result/exit code.
7. `TradingAccountReferenceEligibilityService` implements only the Q-010-owned
   read-only contract allowed for a future Q-008 implementation.
8. Existing Micrometer infrastructure records low-cardinality operation and
   duration measurements; no exporter or dependency was introduced.

## Behavior review

- Same operation ID plus same typed fingerprint returns the stored outcome and
  does not append another operation/history row.
- Same operation ID plus changed semantics returns
  `TRADING_ACCOUNT_IDEMPOTENCY_CONFLICT`.
- New operation plus a compatible scope attestation or complete external tuple
  returns `UNCHANGED` with one new no-state-change operation/history row.
- Conflicting tuple provenance returns a mapping conflict without mutation.
- Generated refs are database-arbitrated and retried at most three full
  transactions only for exact named generated-ref constraints.
- Lifecycle changes use expected version plus current status CAS. There is no
  last-write-wins or automatic retry with a new version.
- There is no delete, remap, alias, merge, repair, generic CRUD, or public HTTP
  behavior.

## Review result

Implementation conforms to the approved design with no material deviation.
Codex does not grant Architect approval; independent review remains required.
