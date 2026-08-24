# Q-007 Bounded Context Design — Historical V1

## Design Principle

Use the smallest cohesive logical contexts. Do not create one context per noun,
and do not map a context to a microservice, repository, database, package, or
deployment during Design V1.

## Contexts

| Context | Classification | Owns conceptually | Consumes | Supplies | Explicit exclusions |
| --- | --- | --- | --- | --- | --- |
| Trading Data | Supporting upstream | Trading Data and external translation | Supported APIs/real SDKs through adapters | Broker-neutral Trading Data | Evidence, Rules, Decisions, Actions, cases |
| Risk Assessment | Core | Evidence, Evidence Set, Rule, Rule Evaluation, Decision | Trading Data | Decisions plus provenance references | Adapter protocol, Action Execution, case workflow, Audit implementation |
| Risk Action | Supporting downstream | Action intent; future Action Execution attempts/outcomes | Decisions | Action references/outcomes | Evidence derivation, Rule Evaluation, Decision creation, Risk Case workflow |
| Risk Case | Supporting optional downstream | Risk Case and Case Association | Decision/Action/Evidence references | Future collaboration context | Trading Data, evidence truth, Rules, Decisions, Action Execution |

## Why Evidence, Rule, and Decision Share One Context

They form one reasoning model. Evidence meaning defines what a Rule can assess;
Rule-version semantics define how a Decision is explained. Splitting them would
create premature contracts and consistency problems before real business
Requirements exist.

## Why Action Is Separate

Existing architecture requires risk detection/decisioning to remain separate
from action execution. Risk Assessment determines meaning; Risk Action owns an
intended response and, later, execution attempts/outcomes. A Decision must
remain valid even when execution fails.

## Why Risk Case Is Separate

Risk Case is optional downstream collaboration. It may associate multiple
Decisions/Actions without taking ownership of their provenance or forcing every
assessment into a case lifecycle.

## Modular-Monolith Boundary

The contexts are review vocabulary only. No directory, Java package, Maven
module, Spring component, table, topic, API, or deployment may be created from
this document without a later approved Requirement.
