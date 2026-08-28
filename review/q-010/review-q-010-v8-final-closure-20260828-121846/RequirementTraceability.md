# Q-010 Closure Requirement Traceability Matrix

No approved MUST or acceptance criterion is omitted. Architecture references
refer to the approved Architecture V1; design references refer to approved
Implementation Design V1.

## Approved Section 2.1 resolutions

| Resolution | Architecture/design | Implementation and persistence | Test/verification | Status |
| --- | --- | --- | --- | --- |
| 1 scoped identity tuple | Arch 4–7; Design 4, 8–9 | typed scope/namespace/key/identity; exact V3 columns | domain bounds; byte/collation MySQL proof | PASS |
| 2 one-to-one immutable cardinality | Arch 8, 10; Design 8, 12–13 | two-direction unique constraints; no remap/delete port | duplicate/conflict/race/collision tests | PASS |
| 3 lifecycle/history | Arch 11, 14; Design 4, 8, 13–14 | named lifecycle, CAS, retained rows/history | lifecycle/CAS/historical eligibility tests | PASS |
| 4 controlled registration | Arch 9; Design 5, 10–11 | purpose-specific non-Web command/descriptor | strict parser and real command tests | PASS |
| 5 bounded consumer disclosure | Arch 12; Design 14 | eligibility service and opaque evidence values | active/ineligible/not-found/type checks | PASS |
| 6 exact Q-009 capabilities | Arch 13; Design 5–6 | AuthorizationGuard-first services | zero-port denial; real grant/revocation | PASS |
| 7 durable attribution | Arch 14–15; Design 8, 13 | operation/history in same transaction | operation/history forced rollback | PASS |
| 8 fail-closed semantics | Arch 17; Design 13–15 | bounded exceptions/outcomes; no stale fallback | denial/conflict/unavailable/CAS tests | PASS |
| 9 Q-008 governance | Arch 19; Design 1, 14 | Q-010 facade only; Q-008 untouched | inventory and boundary scan | PASS |

## Functional requirements

| Requirement | Architecture/design reference | Implementation component(s) | Persistence | Test/evidence | Status |
| --- | --- | --- | --- | --- | --- |
| Q010-FR-001 opaque BrokerOS ref | Arch 4.1/5; Design 4.1/7.3 | `TradingAccountRef`, generators | unique ref separate from BIGINT id | ref and collision tests | PASS |
| Q010-FR-002 complete identity tuple | Arch 4.5/6; Design 4.1/9 | scope, namespace, key, identity values | exact tuple columns/FK | domain and schema tests | PASS |
| Q010-FR-003 exact one-to-one mapping | Arch 8/10; Design 8.2/13 | registration service/adapter | tuple and ref unique constraints | mapping/race/collision tests | PASS |
| Q010-FR-004 controlled idempotent registration | Arch 9/10; Design 10–13 | command, fingerprint, registration services | durable operation/result/history | replay/conflict/command tests | PASS |
| Q010-FR-005 bounded read outcomes | Arch 12/17; Design 14–15 | eligibility and internal resolution services | bounded joined authoritative query | application eligibility tests | PASS |
| Q010-FR-006 historical resolvability | Arch 8/11; Design 4.2/14 | lifecycle domain/service | retained current/history rows; restrict FKs | CAS historical resolution test | PASS |
| Q010-FR-007 attributable safe mutations | Arch 11/14/15; Design 6/8.4/13 | lifecycle services/authorized context | version CAS + immutable history | concurrency/rollback tests | PASS |
| Q010-FR-008 ActorContext/capability | Arch 13; Design 5–6 | Q-009 guard and exact constants | auth decision copied to history | zero-interaction and command auth tests | PASS |
| Q010-FR-009 fail closed | Arch 17; Design 13–15/17 | bounded BusinessExceptions/classifier | no cache/stale success | denial, unknown integrity, revocation tests | PASS |
| Q010-FR-010 minimum metadata | Arch 12; Design 14/16 | eligibility/opaque snapshot/provenance | no sensitive consumer columns returned | type/output/source review | PASS |
| Q010-FR-011 adapter/vendor isolation | Arch 3/6; Design 1/3/17 | framework-neutral domain/application | application DB only | architecture/import scans | PASS |
| Q010-FR-012 no Kafka/Redis/permissive provider | Arch 8/16/20; Design 2/18 | none introduced | MySQL only | dependency/static/Compose checks | PASS |

## Acceptance criteria

| AC | Frozen evidence | Status |
| ---: | --- | --- |
| 1 | V1 Requirement plus V2 approval history preserved | PASS |
| 2 | approved Architecture V1 maps authority, tuple, lifecycle, non-Web and consumer boundaries | PASS |
| 3 | ADR-012 exists and is ACCEPTED before implementation | PASS |
| 4 | prefixed UUIDv4 ref is independent of raw vendor/database identity | PASS |
| 5 | uniqueness, duplicate, reassignment, race and lifecycle outcomes have real MySQL tests | PASS |
| 6 | Q-008-facing API is protected read-only and has no mutation/repository access | PASS |
| 7 | actual Q-009 context/capabilities/default deny protect all approved paths | PASS |
| 8 | who/when/what/before/after/reason/source commit atomically in history | PASS |
| 9 | additive Flyway V3 passed disposable MySQL 8.4.11; no external DB accessed | PASS |
| 10 | no trading/customer/Risk Case/rules/control/vendor/Kafka/Redis scope introduced | PASS |
| 11 | Java 21 Maven, real MySQL, security, static exception isolation, Compose, Kustomize and Review gates complete | PASS |
| 12 | Requirement, Architecture, ADR, Design, authorization, implementation, verification, approval, closure and future commit remain recorded separately | PASS |

## Cross-cutting MUST freeze

- Java release 21, Spring Boot 3.x, Maven, MySQL/Flyway, and one modular
  monolith deployable are preserved.
- V3 uses snake_case, BIGINT `id`, UTC DATETIME(6), readable codes, explicit
  unique/FK/CHECK constraints, and nonnegative optimistic versions.
- No money, trading values, customer data, vendor payload, external DB access,
  credentials, token, sensitive log, delete, Kafka business topic, or Redis
  business key exists in Q-010.
- Caller identity/correlation cannot establish ActorRef or capability;
  unauthorized behavior discloses no target existence.

Final requirement traceability result: **PASS**.
