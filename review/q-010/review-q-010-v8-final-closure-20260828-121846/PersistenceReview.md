# Q-010 V8 Persistence Review

## Migration and schema

`V3__create_trading_account_reference_authority.sql` is byte-identical to the
V7 reviewed migration snapshot and creates exactly:

1. `trading_account_authority_scope`
2. `trading_account_reference`
3. `trading_account_authority_operation`
4. `trading_account_authority_history`

The migration sequence V1/V2/V3 passed on MySQL 8.4.11 and restart reported
the schema at V3 with no new migration. The four Q-010 tables plus three Q-009
tables were the only application tables.

## Final integrity checks

- internal primary keys are `BIGINT id`; business refs remain distinct;
- TradingAccountRef, scope ref, scope attestation, complete external tuple,
  operation ID, and operation-history relation have named unique constraints;
- external keys remain `VARBINARY(512)` with exact validated UTF-8 bytes;
- refs/namespace/codes use binary collations and enforced CHECK constraints;
- FK relationships are delete-restricted; no cascade/delete use case exists;
- lifecycle and nonnegative optimistic version columns match the design;
- operation outcome is durable and history is insert-only by adapter contract;
- no DROP, TRUNCATE, ALTER, data INSERT, UPDATE, or DELETE exists in V3.

Real MySQL tests also proved error 3819/HY000 CHECK enforcement and named
1062/23000 uniqueness classification. Persistence result: **PASS**.
