# Q-007: BrokerOS Domain Foundation

## Status

Design Approved — Implementation Deferred

| Gate | Result |
| --- | --- |
| Requirement | PASS |
| Architecture | PASS |
| Design Review | PASS |
| Design Approved | PASS |
| Implementation | Deferred |

- Requirement ID: `Q-007`
- Architecture phase: Phase 1
- Approved baseline date: 2026-08-23
- Authoritative architecture decision: ADR-009
- Implementation authorization: NOT GRANTED

Q-007 establishes an architecture and language baseline only. It does not
authorize Java, APIs, persistence, messaging, integrations, or business
functionality.

## 1. Background

BrokerOS Risk completed its engineering foundation before formal risk-domain
development. Q-007 Design V1 explored a longer reasoning sequence containing
Trading Data and Rule. The final Architect review narrowed the authoritative
core-domain model and selected Decision as the Core Domain.

The accepted canonical model is:

```text
Evidence
    ↓
Decision
    ↓
Action
    ↓
Risk Case
```

Trading Data remains the current name for an upstream supporting context. Rule
and the future Rule Engine remain decision-support concepts, but they are not
stages in the canonical core-domain model. ADR-009 is authoritative when this
Requirement or supporting design material is summarized elsewhere.

## 2. Problem Statement

Future Requirements need one stable model that explains where risk reasoning
lives and prevents case management or external execution from becoming the
system center. Without that baseline, future work could let Risk Case own
Evidence, treat an Action as successful execution, couple Decision to an
MT4/MT5/CRM SDK, or distribute risk reasoning across vendor adapters.

Q-007 must establish the language, ownership, dependency direction, lifecycle,
and bounded-context baseline while deferring every executable capability.

## 3. Scope

### 3.1 Ubiquitous Language

- **Evidence** is traceable information that supports or refutes a risk
  conclusion. It is the starting point for explainability.
- **Decision** is the explainable risk conclusion made from Evidence. Decision
  is the BrokerOS Risk Core Domain.
- **Action** is business response intent produced by a Decision. It is not an
  execution attempt or outcome.
- **Risk Case** is an optional downstream bounded context that associates
  relevant Evidence, Decisions, and Actions for investigation or collaboration.

Supporting terms:

- **Trading Data** is the current upstream source language for broker-neutral
  trading observations. Q-007 does not rename it.
- **Rule** is a version-identifiable policy used by decisioning.
- **Rule Engine** is the future decision engine that evaluates Evidence under
  Rules to produce Decisions. Q-007 defines no implementation.
- **Execution** is downstream fulfillment of an Action through adapters such as
  MT4/MT5 Manager, CRM, Kafka, or Email. It is outside the Core Domain.

### 3.2 Core Domain and Boundaries

- Decision is the Core Domain.
- Evidence provides the explainable basis of a Decision.
- Action is downstream of Decision and remains separate from Execution.
- Risk Case is downstream and cannot own or control Decision creation.
- Trading Data remains a supporting upstream context.
- The current Phase 1 feature-first modular monolith remains unchanged.

### 3.3 Domain Relationship and Lifecycle

- A Decision must be attributable to Evidence.
- An Action must originate from a Decision.
- Execution attempts and outcomes must not rewrite the originating Decision or
  be represented as the Action itself.
- Risk Case association is optional and does not transfer ownership of
  Evidence, Decision, or Action.
- A future Rule Engine belongs to the Decision capability and must preserve
  rule-version and Evidence provenance.

### 3.4 Architecture Baseline

- ADR-009 owns the accepted architectural decision.
- `docs/architecture/q-007-brokeros-domain-foundation-design.md` provides the
  synchronized context map and non-implementation guidance.
- Future Requirements must conform to ADR-009 or explicitly replace it through
  an approved Requirement and ADR.

## 4. Non Goals

Q-007 does not authorize:

- Java source, packages, DTOs, entities, services, repositories, controllers,
  tests, or generated code;
- Rule Engine implementation, rule language, rules, thresholds, scoring,
  scheduling, streaming, or administration;
- Evidence, Decision, Action, Risk Case, Workflow, Audit, RBAC, or Account
  Control implementation;
- business tables, Flyway migrations, Redis keys/data, Kafka topics/events,
  APIs, ResultCodes, or OpenAPI changes;
- MT4/MT5 Manager SDK, CRM, Kafka, Email, dealer plugin, LP, or other execution
  adapter implementation;
- AI models, AI-generated Evidence, confidence calculation, prompt contracts,
  or automated AI decisioning;
- package restructuring, microservices, repository splits, dependencies,
  configuration changes, CI changes, Docker changes, or Kubernetes changes;
- renaming Trading Data to Observation or implementing an Evidence Chain.

## 5. Acceptance Criteria

1. ADR-009 is Accepted and is the authoritative architecture source.
2. The canonical model is exactly Evidence → Decision → Action → Risk Case.
3. Decision is explicitly the Core Domain.
4. Evidence is the foundation of explainability and retains source provenance.
5. Action represents business intent only and is separate from Execution.
6. Risk Case is an optional downstream bounded context, not the system center.
7. Trading Data remains unchanged as the current upstream supporting name.
8. Rule Engine is documented only as the future decision engine; it is not
   implemented or designed as a runtime in Q-007.
9. External execution belongs to downstream adapters and no real SDK behavior
   is invented.
10. Future AI integration is identified at the Decision layer only as an
    evolution consideration.
11. No source, test, runtime, API, data, messaging, or deployment implementation
    change is introduced.
12. The final Review Package records PASS for Requirement, Architecture, Design
    Review, and Design Approved, with Implementation Deferred.

## 6. Technical Constraints

- Preserve broker, CRM, and trading-platform neutrality.
- Preserve one Phase 1 modular-monolith deployable and feature-first direction.
- Keep external systems behind adapters and never write directly to an
  external-system database.
- Keep Decision, Action, and Execution separate.
- Preserve Evidence and rule-version provenance for future explainability and
  auditability.
- Do not infer business states, authorization, retention, monetary precision,
  failure policy, or workflow from this design baseline.
- Do not modify the baseline without an explicit Requirement and accepted ADR.

## 7. Deliverables

- Approved `docs/requirements/Q-007-Requirement.md`.
- Accepted `docs/adr/ADR-009-brokeros-risk-core-domain-model.md`.
- Synchronized Q-007 architecture documentation.
- `docs/skills/brokeros-risk-core-domain.md`.
- Q-007 Lessons Learned documentation.
- Historical Design V1 review preserved under
  `review/archive/q-007/review-v1-design/`.
- Final Q-007 Review Package in `review/`.

## 8. Verification Plan

- Check that the accepted canonical model is consistent across active Q-007
  Requirement, ADR, architecture, skill, README, and final Review documents.
- Check that Decision is always identified as Core Domain.
- Check that Action and Execution remain separate and Risk Case remains
  downstream.
- Check that Future Considerations are labeled deferred and non-authorizing.
- Verify the candidate Git scope contains Q-007 documentation only.
- Run `git diff --check`, repository static verification, document contract
  checks, and prohibited implementation path checks.
- Record Maven, Docker, Kubernetes, database, Redis, and Kafka runtime checks as
  NOT APPLICABLE to this documentation-only closure.

## 9. Risks

- Future code may collapse Action into an adapter command or outcome.
- A case-management Requirement may try to move ownership back to Risk Case.
- Rule Engine or AI work may bypass Evidence provenance and explainability.
- “Decision is Core Domain” may be misread as permission to implement a giant
  service rather than a domain boundary.
- Future Observation or Evidence Chain proposals may be adopted without the
  required Requirement and ADR.

## 10. Review Checklist

- [x] Requirement PASS.
- [x] Architecture PASS.
- [x] Design Review PASS.
- [x] Design Approved PASS.
- [x] ADR-009 Accepted.
- [x] Decision identified as Core Domain.
- [x] Risk Case positioned downstream.
- [x] Action separated from Execution.
- [x] Skill and Lessons Learned created.
- [x] Implementation remains Deferred.
- [x] No Q-008 or business implementation started.
