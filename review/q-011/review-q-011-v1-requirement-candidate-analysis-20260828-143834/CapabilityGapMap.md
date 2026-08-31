# Capability and Gap Map (post Q-010)

## Already Established

| Capability | Evidence | Current meaning |
| --- | --- | --- |
| Repository/runtime foundation | Q-001/Q-002, ADR-001–004 | Java 21/Spring Boot modular monolith, MySQL, Redis, Kafka, Docker, Kubernetes, Flyway, API/error/validation foundation |
| Durable engineering standards | Q-003, ADR-005, development standards | Mandatory module, API, data, security, audit, review, and Git discipline |
| CI/integration verification | Q-004, ADR-006 | Maven, Compose, MySQL/Flyway, Redis/Kafka, and Kustomize verification path |
| Correlation/observability baseline | Q-005, ADR-007 | Request ID and W3C Trace ID; neither is identity |
| Configuration foundation | Q-006, ADR-008 | Spring Boot externalized configuration and secret boundaries |
| Core-domain language | Q-007, ADR-009 | Evidence → Decision → Action → Risk Case; Decision is Core Domain; design only |
| Risk Case design | Q-008, ADR-010, V4 design | Approved aggregate/lifecycle/persistence/API/audit design; implementation NOT STARTED, NOT ALLOWED |
| Trusted Actor/authorization | Q-009, ADR-011, V10 | Implemented JWT boundary, `ActorContext`, `Capability`-based authorization (`AuthorizationGuard`), provisioning, MySQL V2 |
| Trading Account Reference Authority | Q-010, ADR-012, V8 | Implemented `TradingAccountRef` identity, scoped external-identity mapping, lifecycle, non-web provisioning, and the exact `validateForNewRiskCaseAssociation` read contract Q-008 needs, MySQL V3 |

These capabilities must be reused, not duplicated inside Q-011.

## Approved but Not Implemented

- Q-007's domain model is approved but implementation remains Deferred.
- Q-008 Requirement, Architecture, ADR-010, and Implementation Design V4 are
  approved, but implementation is NOT STARTED and NOT ALLOWED. Its Implementation
  Gate names five required providers plus Actor/authorization; only two of
  those six are satisfied (see Prerequisite Satisfaction Matrix below).

## Missing Prerequisites (unchanged count, two now closed)

1. ~~Authoritative Trading Account reference provider~~ — **CLOSED by Q-010**.
2. Authoritative Evidence provider with source provenance — **open, this
   analysis's candidate**.
3. Authoritative Decision provider attributable to Evidence — open, blocked
   behind #2.
4. Authoritative Action-intent provider originating from Decision — open,
   blocked behind #3.
5. Authoritative ActionOutcome provider owned by a future execution/outcome
   capability — open, blocked behind #4 and a real vendor SDK.
6. Explicit Q-008 implementation authorization after all required providers
   are implemented, wired, and verified — open.

Q-009 closed the Actor/authorization prerequisite. Q-010 closed the Trading
Account prerequisite. Neither absorbed the remaining four domain-provider
prerequisites; each requires its own Requirement.

## Candidate Business Capabilities

- immutable, provenance-bearing Evidence records, scoped to a recognized
  Trading Account, authored by a trusted actor (this analysis's candidate);
- explainable Decision records after Evidence exists;
- Action intent records after Decision exists;
- ActionOutcome records after Action exists and a real execution/adapter
  boundary is separately approved; and
- a shared Audit query/retention capability, if a concrete cross-capability
  need emerges later (not currently blocking Q-008).

## Explicitly Premature

- full Q-008 Risk Case implementation, or any Q-008 slice, without a new
  explicit Architect implementation-authorization decision;
- Rule Engine, rule language, scoring, automated detection, or scheduling as
  an Evidence source;
- Trading Data ingestion, MT4/MT5/CRM/bridge/LP adapter behavior as an
  Evidence source;
- Account Control, Action execution, ActionOutcome, or any vendor Manager
  API behavior;
- Kafka business topics, Redis business keys/cache, streaming/CDC, Flink,
  Python/ML, AI decisioning, or microservices;
- a full Audit platform, retention engine, or legal-hold/redaction workflow;
  and
- a universal Broker/Customer/Account/Entity master-data framework.

## Governance Consistency Notes

- This analysis is produced under the role split recorded in
  `prompts/Claude-Code-BrokerOS-Risk-Project-Handover-Prompt.md`: Claude Code
  performs requirement analysis and review; Codex implements after gates
  clear; the Product Owner approves gates and performs Git operations.
- Q-011 is a **working ID for this candidate only**. It is the next
  sequential unused Requirement number as of 2026-08-28 and is not itself an
  approval of scope, number, or content — the same convention Q-010 used
  during its own V1 Candidate Analysis before Requirement Architect Review.
- No implementation, architecture, ADR, dependency, migration, API,
  configuration, commit, or push was created by this analysis.
