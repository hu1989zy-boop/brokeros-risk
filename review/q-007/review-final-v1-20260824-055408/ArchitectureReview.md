# Q-007 Architecture Review

## Verdict

**APPROVED WITH INFORMATIONAL ITEMS**

No blocking architecture or standards issue was found. Informational items are
limited to the uncommitted state, absence of a Q-007 candidate CI run, the local
Java 23 verification runtime versus the Java 21 project target, and unavailable
Docker/Kubernetes tooling. These do not alter the design-only Q-007 baseline.

## Evidence Reviewed

- `AGENTS.md`
- `docs/requirements/Q-007-Requirement.md`
- `docs/adr/ADR-009-brokeros-risk-core-domain-model.md`
- `docs/architecture/q-007-brokeros-domain-foundation-design.md`
- `docs/skills/brokeros-risk-core-domain.md`
- `review/archive/q-007/LessonsLearned.md`
- current root Review package
- complete staged file list, staged stat, and staged whitespace check

The protected `review/review-history/` directory and existing stash were
deliberately excluded from inspection as required by the review task.

## Requirement and Architecture Consistency

Q-007 requires a domain-foundation design and expressly defers implementation.
The architecture document provides the approved ubiquitous language, core-domain
definition, bounded-context boundaries, object relationships, lifecycle, and
context map. The staged candidate contains documentation and review evidence
only, so the Requirement and implementation boundary remain aligned.

## ADR-009 Consistency

ADR-009 is `Accepted` and is the authoritative decision for the canonical model:

```text
Evidence -> Decision -> Action -> Risk Case
```

The requirement, architecture design, skill, lessons, and review documents use
the same ordering and meaning. The architecture document references ADR-009 and
does not create a competing decision.

## Bounded-context Review

- **Trading Data** is the present upstream supporting context that supplies
  normalized trading facts. It is not the Core Domain.
- **Evidence** converts relevant facts into traceable, explainable support for a
  decision. It does not decide or execute.
- **Decision** is the Core Domain because it evaluates Evidence under policy and
  produces the risk judgment.
- **Action** records business intent resulting from a Decision. It does not call
  MT4/MT5 Manager APIs, CRM, Kafka, email, or any other execution mechanism.
- **Risk Case** is an optional downstream bounded context that may aggregate
  Decisions and Actions for human investigation; it is not the domain entry
  point or system center.
- **Execution/adapters** remain downstream from Action and outside the Q-007
  model implementation scope.

This preserves the required detection/decision versus action-execution boundary
and keeps vendor details outside the core domain.

## Future Considerations Review

The following remain informational evolution candidates and do not change Q-007:

- **Trading Data -> Observation:** documented only as a future possible
  generalization for broader evidence sources. Trading Data is not renamed.
- **Evidence Chain:** the possible Tick -> Quote -> Order -> Deal -> Position ->
  Exposure -> Evidence lineage is not modeled or implemented.
- **Decision metadata:** possible `confidence`, `reason`, `ruleVersion`,
  `traceId`, and `createdAt` fields are not modeled or implemented.
- **Rule Engine:** identified as a future decision engine; not implemented.
- **AI capability:** a future Decision-layer integration direction only; not
  implemented.

## Implementation Absence

The staged scope comparison against `backend/`, `frontend/`, `adapters/`,
`deploy/`, `scripts/`, `.github/`, and `docker-compose.yml` is empty. No staged
change implements Evidence, Decision, Action, Risk Case, Rule Engine, Workflow,
Audit, RBAC, AI, MT4/MT5 adapters, database migrations, Redis/Kafka behavior,
Docker/Kubernetes topology, CI, or Q-008.

## Development Standards Compliance

### AGENTS.md compliance

Checked the entire 38-file staged name/status list against the repository-wide
rules. The candidate has an approved Q-007 Requirement, accepted ADR, architecture
design, reusable skill, honest Lessons Learned, archived design review, and a
current root Review package. No Java or runtime implementation was added. The
new standalone package is intentionally un-staged and does not replace earlier
Q-007 review material.

### Architecture compliance

Compared the Requirement, ADR-009, architecture design, skill, and Lessons
Learned. All preserve the Phase 1 feature-first modular-monolith boundary,
broker/platform neutrality, adapter isolation, and the canonical model. No
microservices, horizontal package restructuring, vendor coupling, or future
business behavior was introduced.

### ADR compliance

ADR-009 contains Context, Decision, Alternatives, Consequences, and Future
Considerations, and its status is Accepted. It captures the material core-domain
decision. Existing applicable isolation and roadmap ADR constraints remain
unchanged; no competing ADR or silent architecture change was found.

### API standard compliance

The staged runtime-scope diff is empty, including `backend/`. Therefore no REST
endpoint, DTO, ResultCode, `ApiResponse`, validation boundary, or exception
contract changed. The existing API standards are unaffected.

### Database standard compliance

No staged path exists under Flyway migrations or runtime configuration, and no
schema/table design is introduced by Q-007. Flyway exclusivity, UTC, identifier,
money, and enum standards remain unaffected.

### Security standard compliance

The staged forbidden-path and high-confidence secret-marker scans passed. No
`.env`, credential, token, private key, ZIP, IDE metadata, or build artifact is
staged. Documentation avoids operational secret values and authentication data.

### Auditability compliance

Q-007 does not invent an Audit module. It establishes Evidence as explainable
decision input and preserves the future requirement that critical actions be
auditable, while deferring audit records and behavior to approved Requirements.
This is consistent with the auditability principle without speculative code.

### Skill compliance

`docs/skills/brokeros-risk-core-domain.md` was inspected and found to provide
concise implementation guidance for Evidence, Decision, Action, Risk Case,
context boundaries, prohibited shortcuts, and future requirement review. It is
not merely a Q-007 changelog and remains consistent with ADR-009.

## Deletion Review

The staged summary reports 747 deleted lines but zero deleted files. The major
line removals are intentional replacement of Q-006 text in the mutable root
Review snapshot with current Q-007 evidence:

- `review/ArchitectureReview.md`: 136 removed lines
- `review/Verification.md`: 113 removed lines
- `review/ProjectTree.txt`: 112 removed lines
- `review/Summary.md`: 110 removed lines
- `review/InitialBaselineCheck.md`: 87 removed lines
- remaining root Review refreshes and a small README replacement make up the
  balance

The prior Q-006 root snapshot remains in Git history. No tracked path has delete
status, and no historical Q-007 content is overwritten: the design review and
Lessons Learned are newly archived under `review/archive/q-007/`. No unrelated
Q-006 or older implementation is part of the staged candidate.

## Final Finding

Q-007 is internally consistent, implementation-free, and suitable to serve as
the official BrokerOS Risk design baseline. The informational verification items
are recorded in `OutstandingItems.md` and do not block commit readiness.
