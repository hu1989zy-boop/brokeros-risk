# Capability and Gap Map

## Already Established

| Capability | Evidence | Current meaning |
| --- | --- | --- |
| Repository/runtime foundation | Q-001/Q-002, ADR-001–004 | Java/Spring Boot modular monolith, MySQL, Redis, Kafka, Docker, Kubernetes, Flyway, API/error/validation foundation |
| Durable engineering standards | Q-003, ADR-005, development standards | Mandatory module, API, data, security, audit, review, and Git discipline |
| CI/integration verification | Q-004, ADR-006 | Maven, Compose, MySQL/Flyway, Redis/Kafka, and Kustomize verification path |
| Correlation/observability baseline | Q-005, ADR-007 | Request ID and W3C Trace ID; neither is identity |
| Configuration foundation | Q-006, ADR-008 | Spring Boot externalized configuration and secret boundaries |
| Core-domain language | Q-007, ADR-009 | Evidence → Decision → Action → Risk Case; Decision is Core Domain; design only |
| Risk Case design | Q-008, ADR-010, V4 design | Approved aggregate/lifecycle/persistence/API/audit design; no implementation |
| Trusted Actor/authorization | Q-009, ADR-011, V10 | Implemented JWT boundary, ActorContext, direct capability authorization, provisioning, MySQL V2 |

These capabilities must be reused, not duplicated inside Q-010.

## Approved but Not Implemented

- Q-007's domain model is approved but implementation remains Deferred.
- Q-008 Requirement, Architecture, ADR-010, and Implementation Design V4 are
  approved, but implementation is NOT STARTED and NOT ALLOWED.
- Q-008 team/queue ownership, related-case Decision associations, detailed
  retention/legal hold/redaction, and other extensions remain explicitly
  deferred.

## Missing Prerequisites

1. Authoritative Trading Account reference provider.
2. Authoritative Evidence provider with source provenance.
3. Authoritative Decision provider attributable to Evidence.
4. Authoritative Action-intent provider originating from Decision.
5. Authoritative ActionOutcome provider owned by a future execution/outcome
   capability.
6. Explicit Q-008 implementation authorization after all required providers
   are implemented, wired, and verified.

Q-009 closed the Actor/authorization prerequisite only. It did not absorb the
five domain-provider prerequisites.

## Candidate Business Capabilities

- a BrokerOS-owned, broker-neutral Trading Account reference authority;
- immutable, provenance-bearing Evidence records after source identity exists;
- explainable Decision records after Evidence exists;
- bounded audit records once concrete business mutations need a shared query/
  retention contract; and
- later, Action intent and Risk Case integration after their providers exist.

## Explicitly Premature

- full Q-008 Risk Case implementation or a reduced slice without a new explicit
  Architect decision;
- Rule Engine, rule language, scoring, automated detection, or scheduling;
- Account Control, Action execution, ActionOutcome, MT4/MT5/CRM/bridge/LP
  adapter behavior, leverage/restriction/close operations;
- Kafka business topics, Redis business keys/cache, streaming/CDC, Flink,
  Python/ML, AI decisioning, Elasticsearch, or microservices;
- full Audit platform, retention engine, legal-hold/redaction workflow, or
  reporting/search platform before their business and compliance contracts
  exist; and
- a universal Broker/Customer/Account/Entity master-data framework.

## Governance Consistency Notes

- The active Q-008 Requirement records V4 Design approval and prerequisite-
  blocked implementation. The Q-008 Implementation Design retains its original
  pre-approval `READY ... NOT APPROVED` header/Section 17 snapshot. This is a
  documentation-status inconsistency that must be reconciled or explicitly
  classified as a retained historical snapshot before Q-010 approval proceeds.
  It does not erase the explicit external approval recorded in the Requirement
  and approval Review, and this V1 does not modify Q-008.
- The root `review/PhaseReviewIndex.md` stops at Q-007. Dedicated Q-008/Q-009
  packages contain the later authority. Root-index consolidation is maintenance,
  not a reason to invent or implement a business feature.
