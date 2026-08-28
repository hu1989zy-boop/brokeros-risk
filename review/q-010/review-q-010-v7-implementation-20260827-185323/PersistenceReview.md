# Q-010 V7 Persistence Review

## Migration

`V3__create_trading_account_reference_authority.sql` is additive and creates:

1. `trading_account_authority_scope`
2. `trading_account_reference`
3. `trading_account_authority_operation`
4. `trading_account_authority_history`

It has no DROP, TRUNCATE, ALTER, data INSERT, UPDATE, or DELETE. Parent and
history FKs use delete restriction. The application exposes no physical delete
or independent history update/write port.

## Data integrity

- Refs, actors, operation IDs, capabilities, namespaces, lifecycles,
  attestation bounds, versions, target FK shape, correlation fields, and
  history before/after semantics have CHECK constraints.
- The external key is binary-exact and the complete identity tuple is unique.
- Scope attestation, account/scope refs, operation IDs, and operation-history
  relationship are unique.
- Current-state updates use ID + expected version + expected status CAS.
- JDBC list queries reject cardinality greater than one and invalid stored
  enum/ref data fails authority unavailable.

## Runtime evidence

Disposable MySQL 8.4 applied and validated V1/V2/V3, accepted the composite
key, enforced exact constraints, returned CHECK error 3819, arbitrated races,
and preserved one state/outcome/history unit. Trigger-forced operation/history
failures left no state or operation residue. Infrastructure restart preserved
exactly one successful row for each Flyway version.

Migration risk: additive table/index creation only; no existing data movement
or destructive compatibility risk. Normal deployment still incurs DDL time for
four empty tables.
