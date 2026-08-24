# Q-007 Final Architecture Review

## Review Result

APPROVED — PASS

ADR-009 establishes the accepted BrokerOS Risk baseline:

```text
Evidence → Decision → Action → Risk Case
```

Decision is the Core Domain, Action represents business intent only, Execution
belongs to downstream adapters, and Risk Case is an optional downstream bounded
context. Implementation remains Deferred.

## Decision Rationale

Evidence must precede a risk conclusion so that future decisions remain
explainable. Decision is the domain center because converting Evidence into a
meaningful risk conclusion is the differentiating BrokerOS capability. Action
and Risk Case are consequences/supporting collaboration rather than owners of
risk truth.

Separating Action from Execution protects the Core Domain from MT4/MT5 Manager,
CRM, Kafka, Email, transport, vendor, retry, and failure semantics. Positioning
future Rule Engine and AI capability at Decision preserves a clear extension
point while granting no implementation authorization.

## Architecture Impact

| Area | Result and evidence |
| --- | --- |
| Product boundary | Broker/CRM/trading-platform neutrality retained; external names appear only as adapter examples. |
| Core Domain | Decision formally selected as Core Domain in ADR-009. |
| Contexts | Trading Data remains supporting upstream; Decision is core; Action and Risk Case are downstream. |
| Modular monolith | No service, repository, deployment, or package split. |
| Action execution | Explicit downstream adapter boundary; no SDK or execution contract invented. |
| Rule Engine | Future Decision mechanism only; no engine, syntax, runtime, or module. |
| AI | Future integration consideration at Decision only; no model, metadata contract, or automation. |
| API/data/messaging | No endpoint, table, migration, Redis key, Kafka topic/event, or contract. |
| Operations | No dependency, configuration, CI, Docker, or Kubernetes change. |

## ADR Review

ADR-009 is necessary because the core-domain center, canonical model, bounded
context direction, Action/Execution separation, and future Rule Engine/AI
integration boundary govern multiple future Requirements. It contains Context,
Decision, Alternatives, Consequences, and clearly deferred Future
Considerations.

Active Q-007 architecture material references ADR-009 as authoritative. The V1
proposal is retained only in the historical Review archive and is explicitly
non-authoritative where it differs from ADR-009.

## Technical Debt and Risks

- No implementation debt is introduced because Q-007 is documentation only.
- Future code must avoid a giant undifferentiated Decision service.
- Evidence provenance, rule versioning, AI governance, Action authorization,
  execution failure policy, and Risk Case lifecycle still need formal future
  Requirements.
- Observation, Evidence Chain, and Decision Metadata are candidates only; they
  cannot be adopted directly from ADR-009 Future Considerations.

## Development Standards Compliance

### AGENTS.md compliance

Inspected root `AGENTS.md`, including product boundary, Phase 1 architecture,
requirements discipline, review package, Definition of Done, and Prompt
Delivery Policy. Q-007 has an approved Requirement, Accepted ADR, Skill,
Lessons Learned, complete root Review, and a ready-to-use Git/CI prompt. It adds
no unapproved implementation or future Requirement.

### Architecture compliance

Inspected the Q-007 Requirement/design and Phase 1 architecture constraints.
The accepted model remains broker-neutral and inside one feature-first modular
monolith. Trading Data stays upstream; vendor execution stays behind adapters;
Decision and Action Execution remain separated. No microservice, repository,
package, technology, or deployment change exists.

### ADR compliance

Inspected accepted ADR-001 through ADR-008 and new ADR-009. ADR-009 is
compatible with the modular-monolith roadmap and strengthens ADR-002's
detection/decision versus execution boundary. It changes the provisional Q-007
V1 model through an explicit Architect decision rather than silently. No other
accepted ADR is modified.

### API standard compliance

Inspected candidate paths and backend status. No controller, DTO, endpoint,
`ApiResponse`, ResultCode, exception, validation, OpenAPI, or Actuator file is
changed. Therefore no application-owned REST contract is introduced or
excluded from the unified API standard.

### Database standard compliance

Inspected `backend/src/main/resources/db/migration` scope and candidate Git
paths. No SQL, Flyway migration, entity, repository, table, column, index,
Redis key/data, or Kafka topic/event is changed. Future diagram/context terms
are conceptual and create no persistence contract.

### Security standard compliance

Inspected Q-007 files and candidate scope for credentials, tokens, private
keys, authentication headers, personal data, secret values, or new data
exposure. None is present. Q-007 adds no endpoint, logging, external call, AI
data flow, authorization decision, or secret handling behavior.

### Auditability compliance

ADR-009 and the Skill require Evidence provenance and preserve the originating
Decision across Action and future Execution. This is an architecture invariant,
not an Audit module. Actor, authorization, before/after state, attempt/outcome,
retention, and audit storage remain explicitly deferred.

### Skill compliance

Applied `docs/skills/development-standards.md` and created
`docs/skills/brokeros-risk-core-domain.md` as reusable future-Requirement
guidance rather than a changelog. The Skill references ADR-009 as authoritative
and covers Evidence, Decision, Action, Risk Case, Rule Engine, AI, and adapter
boundaries without inventing implementation.

## Conclusion

No unresolved architecture or standards violation exists in the Q-007 closure
scope. Architecture Review is Approved and PASS. Q-007 is an official design
baseline with implementation Deferred.
