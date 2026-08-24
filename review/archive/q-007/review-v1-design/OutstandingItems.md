# Q-007 Review V1 Design Outstanding Items — Historical

## Blocking Before Design Approval

1. Architect approval of the canonical language and sequence.
2. Architect approval of Evidence-Based Risk Assessment and Decisioning as
   core domain.
3. Architect approval of the four logical bounded contexts.
4. Decision on proposed conceptual multiplicities.
5. Confirmation that Risk Case is optional downstream aggregation.
6. Confirmation of Decision → Action intent → Action Execution separation.
7. Approval to create ADR-009 after Design V1.

## Known Design Risks

- Premature multiplicity may look like an approved persistence model.
- `Rule Evaluation` may be mistaken for authorization to build a Rule Engine.
- `Action` may be mistaken for successful external execution.
- Future case-management requirements may try to reclaim ownership of Evidence
  or Decision.
- Logical contexts may be misread as microservices or package instructions.

## Deferred Work

- Java/package/module mapping;
- Rule syntax, execution, activation, storage, and administration;
- Workflow, assignments, approvals, SLAs, and state machines;
- Audit implementation;
- Risk Case implementation;
- action authorization/execution and external SDKs;
- schemas, APIs, ResultCodes, Redis keys, Kafka events/topics;
- detailed subjects, outcome/severity models, time windows, retention, and
  authorization.

Deferred work is not approved by Q-007 Design V1.

## Recommendation

Architect should review the seven decisions above. If Design V1 is approved
without change, use the following prompt to record approval and create the
architecture decision only. If the Architect requests revisions, the Architect
must provide a replacement ready-to-use Codex Prompt describing Design V2.

====================================
Codex Prompt
====================================

Apply the approved Q-007 BrokerOS Domain Foundation Design V1 decision in the
current `brokeros-risk` repository.

The Architect has approved:

- Trading Data → Evidence → Rule → Decision → Action → Risk Case;
- Evidence-Based Risk Assessment and Decisioning as the core domain;
- Trading Data, Risk Assessment, Risk Action, and Risk Case as logical bounded
  contexts inside the Phase 1 modular monolith;
- Rule Evaluation as a conceptual supporting term only;
- Decision, Action intent, and Action Execution as separate concepts;
- Risk Case as optional downstream aggregation, never the domain entry point;
- creation of ADR-009 for these accepted decisions.

Required work:

1. Read `AGENTS.md`, Q-007 Requirement, Q-007 Architecture Design, all accepted
   ADRs, and `review/q-007/review-v1-design/`.
2. Change Q-007 Requirement and Architecture Design status to Approved Design
   V1 without changing their approved meaning.
3. Create `docs/adr/ADR-009-brokeros-core-domain-and-context-boundaries.md` with
   Status Accepted, Context, Decision, Alternatives, and Consequences matching
   the approved Design V1 exactly.
4. Update only Q-007 Design Review documents necessary to record approval and
   ADR-009 acceptance.
5. Do not write Java or tests and do not implement Rule Engine, Workflow, Audit,
   Risk Case, Account Control, database, Redis, Kafka, APIs, adapters, or
   deployment changes.
6. Run `git diff --check`, protected-archive-aware static verification, and
   design-scope checks.
7. Output updated Design Review status, ADR result, Git status, Git diff stat,
   and remaining items.
8. Do not commit or push unless separately authorized.
9. Stop and wait for a separately approved Q-007 implementation Requirement;
   do not begin implementation from Design V1.
