# Q-010 V3 Outstanding Items

## Architecture Submission Blockers

None.

## Next Required Gate

External Architect Review must decide:

- Q-010 Architecture V1: APPROVED or CHANGES REQUIRED;
- ADR-012: ACCEPTED or CHANGES REQUIRED; and
- whether an Implementation Design phase may start.

Until that decision, Architecture and ADR remain Proposed and Implementation
Design must not start.

## Deferred to Implementation Design After Approval

- exact Java/port/service/repository types and package substructure;
- exact table/column/index/constraint names and Flyway version;
- manifest serialization, non-web CLI entry point, and exit behavior;
- semantic fingerprint representation and UUID collision retry bound;
- transaction/isolation/locking SQL and exception/ResultCode mapping;
- physical exact-byte ExternalAccountKey storage and MySQL query plans;
- history normalization, safe operational events, and test fixtures.

## Future Requirement Scope

Aliases/merge/migration/reassignment, automatic source discovery/sync,
MT4/MT5/CRM adapters, online administration, full account/broker/tenant master
data, cache/events, cryptographic attestation, retention/redaction, and global
federation require future Requirements.

## Q-008

Q-008 remains unimplemented and blocked by real Trading Account runtime,
Evidence, Decision, Action, ActionOutcome providers, compatibility verification,
and later explicit authorization.
