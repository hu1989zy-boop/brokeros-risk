# Q-007 BrokerOS Domain Foundation Lessons Learned

## What Was Established

Q-007 established the documentation-only BrokerOS Risk domain baseline:
Evidence → Decision → Action → Risk Case. Decision is the Core Domain, Risk Case
is downstream, and external Execution remains separate from Action.

No business code, schema, API, Rule Engine, workflow, case capability, AI
capability, adapter implementation, or infrastructure was added.

## Why This Design

Evidence starts the model because a risk conclusion must be traceable to facts
that support or refute it. Starting with a case would prioritize work management
over explainability.

Decision is the Core Domain because BrokerOS creates value by turning Evidence
into an explainable risk conclusion. Data acquisition, action fulfillment, and
case collaboration support that responsibility but do not replace it.

Action differs from Execution because business intent and external outcome have
different ownership, authorization, error, retry, and audit semantics. Keeping
them separate also prevents vendor SDK details from entering the Core Domain.

Risk Case is downstream because investigation should associate existing risk
reasoning rather than own or trigger it. A Decision and its Action remain valid
without a case.

The model supports future AI explainability by placing any future AI capability
at the Decision layer and requiring Evidence provenance. It does not assume that
AI-generated confidence or reasons are trustworthy or authorize AI behavior.

## Alternatives Considered

- Risk Case-centered modeling was rejected because it couples reasoning to
  workflow and investigation artifacts.
- Combining Action and Execution was rejected because it hides partial failure
  and vendor boundaries.
- Keeping Trading Data and Rule in the canonical core chain was rejected in the
  final review; they remain supporting source/mechanism concepts.
- Renaming Trading Data to Observation was deferred to a future ADR.

## Problems Encountered

The final Architect decision intentionally differed from the Design V1 proposal
by narrowing the canonical model and selecting Decision alone as Core Domain.
The closure therefore had to update Requirement, architecture, review, and
guidance together instead of silently accepting conflicting documents.

## Reusable Lessons

- Identify the core business decision before designing workflow or case UI.
- Start explainable risk reasoning from attributable Evidence.
- Model business intent separately from integration attempts and outcomes.
- Keep downstream collaboration from owning upstream domain truth.
- Treat AI integration points as architecture boundaries, not permission to
  invent models, fields, confidence semantics, or automation.

## Future Risks

- Future Rule Engine work could emit Actions directly and bypass Decision.
- Case management could drift back into ownership of Evidence or Decision.
- Adapter outcomes could be mistaken for Action state.
- AI metadata could be added without provenance, governance, or trust rules.
- Observation and Evidence Chain concepts could be adopted without a formal
  Requirement and ADR.
