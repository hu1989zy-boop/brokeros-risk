# ADR-009: BrokerOS Risk Core Domain Model

- Status: Accepted
- Date: 2026-08-23
- Requirement: Q-007

## Context

BrokerOS Risk needs a durable domain baseline before any risk business module
is implemented. A Risk Case-centered design would make investigation workflow
the system center, while coupling Action to external execution would leak MT4,
MT5, CRM, messaging, or notification concerns into risk reasoning.

Q-007 Design V1 explored Trading Data and Rule as stages in a longer reasoning
sequence. Final Architect review determined that those are supporting inputs
and mechanisms rather than the canonical core-domain chain. The accepted model
must place explainability first, Decision at the Core Domain, and execution
behind downstream adapters.

## Decision

Adopt the following canonical BrokerOS Risk domain model:

```text
Evidence
    ↓
Decision
    ↓
Action
    ↓
Risk Case
```

- **Evidence** is the traceable foundation for explainability. A Decision must
  be attributable to the Evidence that supports or refutes its conclusion.
- **Decision** is the BrokerOS Risk Core Domain. It owns the explainable risk
  conclusion, not external execution or case workflow.
- **Action** represents business response intent produced by a Decision. It is
  intentionally separate from an execution request, attempt, or outcome.
- **Risk Case** is an optional downstream bounded context for investigation,
  collaboration, and association. It is not the entry point or owner of
  Evidence, Decision, Action, or decisioning.
- The future **Rule Engine** is the decision engine: it evaluates Evidence under
  versioned Rules to produce Decisions. Q-007 does not authorize its design or
  implementation.
- **Execution** belongs to downstream adapters, including future MT4/MT5
  Manager, CRM, Kafka, Email, or other integration mechanisms. Adapter behavior
  requires its own Requirement, real supported API/SDK, timeout, failure,
  idempotency, security, and audit decisions.
- Trading Data remains the current upstream supporting context name. It is not
  renamed or removed by this decision.
- Future AI capabilities integrate at the Decision layer and must preserve
  Evidence provenance and explainability. This statement authorizes no AI
  implementation.

The model is a domain dependency and ownership baseline. It is not a workflow,
runtime pipeline, package layout, service decomposition, database schema, API,
Kafka topology, or deployment design.

## Alternatives

### Make Risk Case the core domain and entry point

Rejected because it would organize work before explaining why risk exists,
encourage case ownership of Evidence/Decision, and couple decisioning to case
workflow.

### Combine Action with Execution

Rejected because a business intent and an external attempt/outcome have
different ownership, authorization, failure, retry, and audit semantics.
Combining them would leak vendor and transport behavior into the Core Domain.

### Keep Trading Data and Rule inside the canonical core chain

Rejected for the accepted baseline. Trading Data is a supporting upstream
source and Rule/Rule Engine is a decision mechanism. They remain important but
do not displace Evidence, Decision, Action, and Risk Case as the canonical
domain model.

### Rename Trading Data to Observation in Q-007

Deferred. The broader name may become useful, but changing it now would exceed
the approved Q-007 baseline and needs a future Requirement and ADR.

## Consequences

- Decision becomes the stable center of future domain Requirements.
- Evidence provenance is required for explainability and future review or AI
  assistance.
- Rule Engine becomes the future decision engine and must produce Decisions
  rather than Actions, executions, or cases directly.
- Risk Case remains downstream and optional; case management cannot own or
  control decisioning.
- Action models business intent only. External execution can evolve, fail,
  retry, or be disabled independently through adapters.
- MT4/MT5 Manager, CRM, Kafka, Email, and other integrations remain outside the
  Core Domain.
- Future AI capabilities have a defined integration boundary at Decision, but
  confidence, models, governance, human review, and safety remain undecided.
- The decision does not create Java packages, APIs, schemas, events, topics,
  caches, workflows, or infrastructure.

## Future Considerations

The following candidates are documentation-only. They do not change the Q-007
baseline and require future approved Requirements and ADRs before adoption.

### Candidate 1: Observation Context

Trading Data may evolve into a more generic bounded context named
**Observation** because future Evidence sources may include:

- Trading Data;
- Market Data;
- Execution Trace;
- Dealer Plugin Events;
- LP Feedback;
- Audit Events;
- AI-generated Observations.

This evolution is intentionally deferred. Q-007 does not rename Trading Data.

### Candidate 2: Evidence Chain

Future versions may explore an evidence lineage such as:

```text
Tick
↓
Quote
↓
Order
↓
Deal
↓
Position
↓
Exposure
↓
Evidence
```

This is an architectural idea only. It defines no entity, persistence model,
adapter contract, data ownership, cardinality, or implementation.

### Candidate 3: Decision Metadata

Future Decision objects may evaluate metadata such as:

- `confidence`;
- `reason`;
- `ruleVersion`;
- `traceId`;
- `createdAt`.

These candidates may support AI explainability, but Q-007 defines no fields,
types, requiredness, persistence, API contract, trust semantics, or generation
logic.
