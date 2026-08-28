# Q-010 V5 Persistence Design Review

## Result

- Persistence Design: **PASS FOR EXTERNAL ARCHITECT REVIEW**
- Authoritative store: MySQL 8.4 / application-owned schema
- Future migration: `V3__create_trading_account_reference_authority.sql`
- Migration created in V5: **NO**

## Planned Tables

| Table | Purpose | Principal integrity controls |
| --- | --- | --- |
| `trading_account_authority_scope` | current authority-scope state and immutable registration provenance | unique opaque scope ref; unique attestation source/ref; lifecycle/version checks |
| `trading_account_reference` | current immutable external tuple to BrokerOS ref mapping | unique TradingAccountRef; unique scope/namespace/exact-key tuple; restricted scope FK |
| `trading_account_authority_operation` | durable idempotency/replay outcome | unique operation UUID; fingerprint; typed target FK; outcome/version |
| `trading_account_authority_history` | append-only per-operation mutation evidence | one-to-one unique operation FK; actor/capability/decision/provenance/before-after evidence |

All tables use InnoDB, `BIGINT AUTO_INCREMENT id`, UTC `DATETIME(6)`, readable
codes, named constraints, and delete-restricted relationships. No table owns
customer, trading, vendor payload, credential, money, or general Audit data.

## Identity and Collation Evidence

- namespace fields are bounded ASCII with `ascii_bin` exact comparison;
- `external_account_key` is `VARBINARY(512)` and preserves exact validated
  UTF-8 bytes, including leading zeros, case, and normalization form;
- the full external tuple unique key includes scope ID plus all four namespace
  fields and the external key;
- `trading_account_ref` has its own unique key, enforcing the reverse mapping;
- generated refs and operation IDs are canonical lowercase UUIDv4 forms; and
- actual composite-key size/collation/constraint behavior must pass MySQL
  8.4.11 integration tests before implementation can close.

## Atomicity and Concurrency

One high-level JDBC mutation adapter owns each complete local transaction.
State insert/update, durable operation outcome, and one history row commit or
roll back together. Registration races resolve through named unique
constraints. Lifecycle races resolve through a status-and-version
compare-and-set. Unknown constraint errors, deadlocks, timeouts, and uncertain
commits fail unavailable; exact operator replay uses the same operation ID.

There is no distributed transaction, lock service, cache authority, event
authority, destructive delete, identity update, alias, reassignment, merge, or
manual repair path.

## Migration Safety

The future migration is one additive V3 after the committed V1/V2 sequence. It
creates exactly the four tables in FK order, inserts no data, and does not edit
an applied migration. Static and real-MySQL tests must prove clean/upgrade
migration, checks, binary uniqueness, restricted deletes, query plans, Flyway
validation/restart, and absence of destructive DDL/DML.

No unresolved persistence-design issue blocks external Architect review.
