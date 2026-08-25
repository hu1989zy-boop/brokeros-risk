# Q-009 Persistence Design Review

## Result

Persistence Design: **PASS FOR ARCHITECT REVIEW**

Database Migration Required: **YES**

Proposed migration: `V2__create_security_actor_foundation.sql` after approval.

## Target and Scope

- Target: MySQL 8.4.
- Migration authority: Flyway, forward-only, additive.
- Current baseline: V1 contains no business tables.
- Proposed tables: `security_actor`, `security_principal_mapping`, and
  `security_actor_capability` only.
- Provisioning data: not inserted by Flyway; supplied later through the
  controlled application bootstrap command.

## Standards Evidence

| Standard | Evidence |
| --- | --- |
| Names | snake_case tables/columns |
| Internal keys | `BIGINT id` primary keys |
| External identity | separate opaque UUIDv4 `actor_ref` |
| Time | UTC `DATETIME(6)` |
| Enum persistence | stable readable codes with checks, never ordinals |
| Exact keys | binary collations for case-sensitive issuer/subject/capability |
| History | disable/revoke states retained; no hard/cascade delete |
| Concurrency | unique constraints plus optimistic `version` updates |
| Migration safety | additive tables; no destructive DDL/DML or data movement |

## Keys and Constraints

- unique ActorRef;
- unique `(issuer, subject, principal_type)` mapping;
- unique `(actor_id, capability)` assignment;
- actor FKs with delete restricted;
- status/type/version/shape/timestamp checks; and
- indexed mapping and actor/grant status queries.

The design explicitly requires migration and repository tests against
disposable MySQL 8.4 for composite-key size, binary collation, FK/check
enforcement, exact mapping, grant/revoke behavior, query plans, optimistic
conflicts, and concurrent duplicates.

## Data Protection

The schema stores no password, token, authorization header, private key, claim
document, provider role/group, email, display name, session, or framework
authentication. Issuer/subject are necessary mapping keys and must not be
returned or logged. ActorRef is the business-facing opaque identity.

## Rollback and Repair

The old application ignores additive tables. Rollback retains them and any
provisioned data. Corrections use later migrations; an applied V2 is not edited.
Mapping/grant corrections use controlled lifecycle operations, not manual DDL
or deletion.

No unresolved persistence-design issue prevents Architect review.
