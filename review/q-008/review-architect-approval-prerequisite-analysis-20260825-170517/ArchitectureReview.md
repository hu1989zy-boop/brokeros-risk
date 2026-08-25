# Q-008 Architect Approval and Prerequisite Architecture Review

## Review Result

**PASS FOR ARCHITECT APPROVAL RECORDING AND PREREQUISITE ANALYSIS**

Implementation Design V4 is approved. Implementation is not authorized because
trusted Actor/authorization and authoritative upstream providers do not exist.
The Review neither reopens V4 nor expands Q-008.

## Scope Reviewed

- AGENTS.md and durable development standards;
- Q-007 Requirement, architecture, ADR-009, Core Domain Skill, and Lessons;
- Q-008 Requirement, ADR-010, V4 Implementation Design, V1–V4 Reviews/ZIPs;
- Phase 0.6 security statement, ADR-007, and observability guidance;
- backend Maven dependencies, production Java packages, application
  configuration, README, migration baseline, and relevant symbols;
- current Git scope and historical Review preservation.

## Approval Assessment

The external Architect explicitly approved CaseNumber, immutable Resolution
History, Aggregate Boundary, concurrency/versioning, deterministic ordering,
reference model, Action/Execution separation, persistence, audit transaction,
lifecycle, API boundary, and ADR compatibility. This Review records the
decision against the exact V4 Design hash. No Design V5 or ADR amendment is
required.

## Prerequisite Assessment

### Actor and authorization

Q-007 cannot satisfy the prerequisite. It contains no Actor/identity/security
contract and explicitly defers RBAC/runtime behavior. The backend has no Spring
Security dependency or authentication context. Request/Trace IDs are expressly
untrusted correlation metadata. A separate approved security foundation is
required.

### Authoritative references

Q-007 supplies semantic ownership only. There is no executable Trading Account,
Evidence, Decision, Action, or ActionOutcome provider. Thin Q-008 ports can
preserve dependency direction but cannot create truth. Provider implementations
must come from approved owning capabilities and fail closed.

### Requirement decision

Q-008 cannot implement missing cross-cutting security or upstream domain
capabilities without silently expanding scope. Q-009 is recommended for the
Trusted Actor and Authorization Foundation. Separate owning-capability
Requirements must deliver reference authorities; their IDs are not invented in
this Review.

## Dependency and Runtime Assessment

Approved future dependency direction is:

```text
security adapter → trusted ActorContext → Q-008 application authorization port
Q-008 application → reference-query ports → owning-capability published reads
Q-008 domain → no Spring Security, vendor, or upstream implementation classes
```

Authorization happens before data access. Reference checks are read-only and
are not another resource manager in the Risk Case/Audit database transaction.
Missing/denied/unavailable authorities fail closed; no permissive production
fallback is allowed.

## ADR Assessment

- ADR-009 compatibility: PASS; Decision stays Core Domain, Risk Case downstream.
- ADR-010 compatibility: PASS; approved aggregate/lifecycle/audit boundary is
  unchanged.
- New ADR in this task: NO; this task adds no dependency or runtime boundary.
- Future Q-009 ADR need: MUST BE EVALUATED if it selects Spring Security, an
  identity protocol/provider, principal contract, or other durable security
  boundary.
- Future provider ADR need: evaluate per owning-capability Requirement and
  integration boundary.

## Development Standards Compliance

### AGENTS.md compliance

Inspected the repository-wide Requirement, architecture, ADR, product boundary,
Git, Review, security, audit, and Definition of Done rules. Changes are limited
to Q-008 gate synchronization, an honest Lesson, and a new Review/ZIP. No
business implementation, historical overwrite, stage, commit, push, reset, or
clean occurs.

### Architecture compliance

The analysis preserves one Phase 1 modular monolith, broker/vendor neutrality,
Risk Case downstream ownership, and Action/Execution separation. It prevents
Risk Case from implementing upstream truth and rejects a generic omnibus
foundation.

### ADR compliance

ADR-007, ADR-009, and ADR-010 were inspected and remain unchanged. Request/Trace
IDs are not identity, Decision remains Core Domain, and Q-008 aggregate/audit
consistency is unchanged. The approved V4 file and V4 Review/ZIP hashes remain
frozen.

### API standard compliance

No endpoint, DTO, response, ResultCode, exception mapping, or OpenAPI artifact
changed. Future authentication/authorization failures require Q-009 contracts;
this Review does not pre-create them.

### Database standard compliance

No migration, SQL, table, column, index, constraint, or data change exists.
`backend/src/main/resources/db/migration/V1__initial_schema.sql` remains the
unchanged no-business-table baseline.

### Security standard compliance

Repository evidence was checked for Spring Security, authentication context,
ActorContext, authorization, unsafe headers, and secrets. The analysis rejects
caller-supplied identity, hard-coded actors, fake providers, and unchecked
references. No credential or sensitive production data is added.

### Auditability compliance

The approved same-transaction Risk Case/Audit design remains unchanged. This
analysis protects trustworthy actor attribution and refuses to call untrusted
request metadata an Audit actor. No critical runtime action occurs in this
documentation-only task.

### Skill compliance

`development-standards`, `brokeros-risk-core-domain`, and
`observability-correlation` were applied. No Skill update is required because
their existing rules already cover upstream ownership, external boundaries,
and the non-identity nature of correlation IDs. A new honest phase Lessons
Learned records the provider/trust-layer finding and baseline test environment
issue.

## Risk-system Impact

- Risk Case: Design approved; implementation still blocked.
- Decision/Evidence/Action: no implementation or ownership change.
- ActionOutcome/Account Control: no execution design or implementation.
- Audit: no module implementation; trusted actor prerequisite protected.
- Rule Engine: no impact.
- Kafka/Redis: no topic, event, key, or runtime change.
- MT4/MT5/Bridge/LP/CRM: no adapter or integration change.
- Deployment/CI: no change.

## Review Conclusion

- Architect Design Gate: APPROVED
- Implementation Design: V4 — APPROVED
- Actor/Auth prerequisite from Q-007: NO
- Reference-provider prerequisite from Q-007: NO
- Q-009 recommended: YES
- Q-008 Implementation Authorized: **NO**
