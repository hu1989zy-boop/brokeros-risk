# Q-010 V3 Architecture + ADR Analysis Summary

## Result

**PASS — READY FOR EXTERNAL ARCHITECT REVIEW**

Q-010 Architecture V1 translates the approved Requirement into a complete
reference-authority boundary without entering Implementation Design. ADR-012
captures the durable identity, authority, lifecycle, MySQL, security, and
consumer decisions and remains Proposed.

## Key Decisions

- Q-010 owns a bounded Trading Account Reference Authority capability under the
  future `com.brokeros.risk.tradingaccount` module boundary.
- TradingAccountRef is server-generated `ta-<lowercase-UUIDv4>`; authority
  scopes use `aas-<lowercase-UUIDv4>`.
- SourceNamespace is a governed immutable four-part value covering source
  family, instance, server, and environment.
- ExternalAccountKey preserves exact bounded UTF-8 identity; generic lossy
  normalization is prohibited.
- One current-state authority record binds one immutable tuple to one immutable
  TradingAccountRef, uniquely in both directions.
- ACTIVE/INACTIVE/RETIRED preserve history; only an active account inside an
  active scope is eligible for new associations.
- Registration/lifecycle operations are non-web, manifest-driven, attested,
  idempotent, and protected by a fresh Q-009 SERVICE ActorContext.
- Application-owned MySQL is authoritative; state, durable operation outcome,
  and immutable history commit in one local transaction.
- Q-008 receives only a protected eligibility-by-TradingAccountRef contract;
  no external key, source detail, customer data, persistence ID, or vendor DTO.
- No Kafka, Redis, public provisioning API, external database lookup, new
  dependency, or adapter is introduced.

## Gate Boundary

- Requirement: APPROVED
- Architecture: PROPOSED / not approved
- ADR-012: PROPOSED / not accepted
- Implementation Design: NOT STARTED
- Implementation: NOT STARTED
- Implementation Allowed: NO
- Q-008: unchanged, unimplemented, and still blocked by other providers plus a
  later explicit authorization.

No production code, test, migration, SQL, API, dependency, configuration,
Kafka, Redis, adapter, Docker/Kubernetes behavior, staging, commit, or push was
performed.
