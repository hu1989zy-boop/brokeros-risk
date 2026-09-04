# Q-019 Architecture Review

## Review scope

Reviewed the Q-019 implementation against Requirement V1, ADR-021, Architecture
V1, Implementation Design V1, the repository engineering protocol and decision
principles, root `AGENTS.md`, applicable development standards, the React console
skill, and recent Q-016/Q-017/Q-018 lessons.

## Architecture result

**PASS WITH CONDITIONS.** The code follows the approved additive-read design and
preserves the Phase 1 modular-monolith boundary. The sole condition is runtime
evidence for the live AC4 slice, not an identified architecture defect.

### Backend structure

- The route is added to the existing `RiskCaseController`; it does not introduce
  another controller, module, or service boundary.
- The controller only obtains actor context, calls `RiskCaseQueryService`, maps a
  response DTO, and returns `ApiResponse`.
- `RiskCaseQueryService.associations` authorizes before case lookup, reads one
  aggregate snapshot for case/version/current decision, and assembles immutable
  application projection records.
- `RiskCaseRepository` gains exactly the two approved read methods. Their JDBC
  implementations are parameterized `SELECT`s over existing application-owned
  tables with deterministic `case_version, id` ordering.
- Existing `findAllEffectiveEvidence` and `findAllEffectiveActions` remain the
  effective-state authority. No association rule is reimplemented in the REST or
  frontend layers.

### Frontend structure

- The existing `ApiClient` and repository context are reused; the response is
  parsed at the network boundary before entering UI state.
- A case-number-scoped TanStack Query key makes the new projection independently
  refreshable while the existing broad post-write invalidation remains intact.
- `AssociationsPanel` is a view of backend-owned state. Picker values come from
  projection refs and do not decide association validity client-side.
- Existing Q-017/Q-018 action descriptors and mutation runner remain the only
  write execution path.

## ADR-021 and design conformance

| Decision | Result | Evidence |
| --- | --- | --- |
| Single additive `/associations` GET | PASS | One new controller method and one repository client method. |
| Existing `risk-case:read` authorization | PASS | Query service uses the existing `RiskCaseCapabilities.READ`; no capability diff. |
| Bounded full projection, no pagination | PASS | A 500-item cap is applied independently to evidence events, effective evidence, decisions, and effective actions; 501 decisions produce the existing invariant error. |
| Existing tables and effective queries | PASS | Two plain SELECTs plus existing effective-evidence/action queries; zero migration diff. |
| Authoritative console projection | PASS | Panel/pickers consume `RiskCaseAssociations`; prior history reconstruction was removed. |
| Option B and writes deferred | PASS | No external search/list endpoint or business write was added. |

## Development Standards Compliance

### AGENTS.md compliance

The approved Requirement, Architecture, ADR-021, Design, engineering protocol,
development standards, repository skills, and recent lessons were inspected
before implementation. Work stayed within Q-019 and ends with this timestamped
review package and a conditional gate decision. No stage/commit/push occurred.

### Architecture compliance

The implementation remains inside the existing `com.brokeros.risk.riskcase`
module and the existing React Risk Console feature. It adds no service boundary,
vendor coupling, framework, infrastructure component, Kafka topic, Redis key, or
external-system database access. The modular-monolith and adapter boundaries are
unchanged.

### ADR compliance

ADR-021 is implemented directly: one bounded read endpoint, two read-only queries,
existing effective-state reads, and an authoritative console projection. ADR-018's
React decision remains unchanged. No decision with ADR significance was introduced
during implementation.

### API standard compliance

`RiskCaseController` returns `ApiResponse<RiskCaseAssociationsResponse>` through
the existing response helper. The endpoint is under `/api/risk-cases`, uses the
existing exception path and stable ResultCodes, exposes no persistence entity or
internal numeric ID, and has a frontend typed parser/contract test.

### Database standard compliance

`JdbcRiskCaseRepository` adds only parameterized `SELECT`s on
`risk_case_evidence_association_history` and
`risk_case_decision_association`. There is no migration or DDL/DML write. Existing
UTC timestamps and stable string refs are mapped to domain value types. Real MySQL
tests applied and validated all eight existing migrations.

### Security standard compliance

Authorization occurs before parsing/looking up the target case, evidenced by the
403-on-missing-target test. The existing `risk-case:read` capability is reused.
The response contains case-owned refs, enum/state markers, source, and timestamps
only; no external entity payload, actor identity, token, credential, or internal
row ID is returned or logged by Q-019.

### Auditability compliance

Q-019 is an explicitly pure read and does not introduce an audit/business write.
It reports through the existing Risk Case READ metric. All association mutations,
actor/reason/version records, and their existing audit behavior remain untouched.
The projection exposes the persisted evidence event refs and timestamps needed to
trace the case's association state without modifying historical records.

### Skill compliance

`docs/skills/react-risk-console-development.md` was applied for typed boundary
parsing, query invalidation, conflict refetch, and honest live-test handling. No
new repository skill is warranted because Q-019 adds no new reusable workflow.
The personal `brokeros-review-package` skill is used to validate and create the
non-overwriting archive and SHA-256 handoff.

## Boundary evidence

The final targeted boundary command returned zero paths for:

- `backend/.../riskcase/domain`
- `backend/src/main/resources/db/migration`
- `RiskCaseAssociationService.java`
- `RiskCaseCommandService.java`
- `RiskCaseResolutionService.java`
- `RiskCaseCapabilities.java`

`git diff --check` and `scripts/verify-static.sh` both passed.

## Independent-review focus

The independent reviewer should confirm the chosen 500-per-collection overflow
contract and execute AC4 with a real local stack and seeded references. No
unresolved standards violation was found in the implemented source.
