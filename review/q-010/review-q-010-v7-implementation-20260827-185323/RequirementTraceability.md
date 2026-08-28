# Q-010 V7 Requirement Traceability

| Requirement concern | Implementation evidence | Verification evidence |
| --- | --- | --- |
| BrokerOS-owned opaque reference | `TradingAccountRef`, UUID generators, no caller-supplied registration ref | domain ref tests; generated collision MySQL test |
| Complete external identity tuple | `AccountAuthorityScopeRef`, `SourceNamespace`, `ExternalAccountKey`, `ExternalAccountIdentity` | boundary/value tests; unique tuple/concurrency tests |
| Exact comparison and storage | reject-not-normalize values; UTF-8 defensive bytes; `VARBINARY(512)` | Unicode/boundary tests; real byte query and collation inspection |
| One-to-one immutable authority | two business-ref/tuple uniqueness constraints; no remap/delete port | static architecture review; collision/mapping tests |
| Historical lifecycle authority | explicit ACTIVE/INACTIVE/RETIRED transitions and joined eligibility query | lifecycle unit/CAS tests; historical resolution remains present |
| Durable idempotency | typed framed SHA-256; unique operation; stored replay result | golden/sensitivity test; sequential/concurrent replay tests |
| Atomic provenance | state + operation + history in one TransactionTemplate | forced operation/history trigger rollback tests |
| Q-009 authorization | AuthorizationGuard-first services; registered command descriptor | zero-interaction denial test; real service command/grant/revocation test |
| Controlled provisioning | strict one-file WebApplicationType.NONE command and exit mapping | parser/field-matrix tests; real command success/replay/denial |
| Bounded Q-008 contract | `validateForNewRiskCaseAssociation` returns decision + opaque evidence only | active/inactive/not-found/unauthorized/non-disclosure tests |
| Sensitive-data protection | redacted key, bounded exceptions/report/metrics, no raw logging | redaction and command output assertions; source scan |
| No Q-008 implementation/scope creep | only Q-010-owned facade; no Q-008 package/source touched | file inventory and Git status |
