# Q-017 Architecture Review

## Gate Decision

**PASS WITH CONDITIONS** — static, build, and automated component/contract
evidence conforms to ADR-019 and the accepted Design. Live authenticated
backend execution remains outstanding, and independent review is not performed
by this package's author.

## Requirement and architecture alignment

- The change is a thin-client expansion of the existing React SPA. Lifecycle,
  invariants, authorization, actor identity, version checks, and audit creation
  remain owned by the existing backend.
- `actionDescriptors.ts` is the single declarative source for operation inputs,
  endpoint suffixes, status availability, terminal flags, and typed messages.
  `useCaseAction.ts` centralizes mutation and cache behavior.
- Status filtering is only a UI convenience. The code does not read JWT claims
  or infer capability; `AuthorizationError`/backend `ResultCode` responses remain
  authoritative and visible.
- Groups C and E are absent. Optional resolution references remain free-text
  validated opaque values; no picker, recognition rule, or association command
  was added.

## ADR-019 / Design compliance

| Decision | Evidence | Result |
| --- | --- | --- |
| Declarative registry | 11 descriptors, exact paths/inputs/statuses/terminal flags | PASS |
| Shared TanStack Query runner | One `useMutation`, success invalidation, conflict refetch | PASS |
| Reuse Q-016 client | Existing `ApiClient` is injected unchanged | PASS |
| Status-only availability | Six-state `CaseActionsBar` test matrix | PASS |
| Backend authorization authority | No claims/probe; 11 typed 403 tests | PASS |
| Terminal confirmation | Resolve/close/cancel use validate→review→confirm flow | PASS |
| Conflict preservation | 11 conflict/refetch/preserved-input/version-8 retry tests | PASS |
| Detail integration | Action bar, generic dialog, notes correction panel | PASS |
| Security grant | Exact eight-capability bootstrap assertion passes | PASS |
| Live vertical slice | Spec delivered and discovered; environment-dependent execution skipped | CONDITION |

## Module and contract impact

The dependency direction remains UI → action/model → typed repository → Q-016
API client. Request DTO types mirror committed Java records. Detail and note
responses reuse their runtime parsers; resolution adds a bounded parser for the
existing resolution response record. All mutations carry `expectedVersion` and
invalidate/refetch backend state rather than editing a client-owned aggregate.

The design calls resolution reference inputs optional, while the committed Java
DTO requires non-null sets. Empty UI fields therefore serialize as empty arrays.
The backend resolution request has no independent `reason`; the required
`resolutionSummary` is labelled as summary and reason. These reconciliations
are recorded in `OutstandingItems.md`.

## Development Standards Compliance

### AGENTS.md compliance

The approved Requirement, ADR-019, Architecture, Design, cleared implementation
prompt, engineering execution protocol, architecture decision principles,
ADR-018/Q-016 context, development standards, applicable repository skills, and
recent Lessons Learned were inspected before implementation. Work stayed within
the authorized stage and stopped at review handoff. No stage, commit, or push
was performed.

### Architecture compliance

The product remains broker-, CRM-, and platform-neutral. Opaque Risk Case,
actor, evidence, and action references cross the existing REST adapter only. No
business transition, security decision, external database access, vendor
coupling, new service boundary, or unnecessary platform dependency was added.

### ADR compliance

ADR-019 is implemented directly and ADR-018's React/TypeScript/Vite/Ant Design/
TanStack/axios stack is unchanged. No framework, system boundary, persistence,
messaging, deployment strategy, or major dependency decision was introduced;
no new ADR is required.

### API standard compliance

No application-owned API was changed. The frontend uses existing `/api/risk-
cases` POST contracts and the existing `ApiResponse` parser. Exact request-body
tests cover every operation, assert `expectedVersion`, and assert absence of
`actorRef`. An unknown future ResultCode is tested as failure, not success.

### Database standard compliance

`git diff --stat -- backend/` and `git status --short -- backend/` are empty.
There is no Flyway, SQL, schema, money, timestamp, or data-migration change.
Frontend code does not access a database.

### Security standard compliance

Authentication continues through the Q-016 Bearer/refresh path. No token,
secret, password, claim parsing, capability probe, identity override, raw HTML,
or script evaluation is introduced. The bootstrap grants exactly the eight
approved V1 capabilities and neither `associate` nor `create`. Live Playwright
trace, screenshot, and video remain disabled.

### Auditability compliance

Every operation includes the current backend version and the backend receives
the operation-specific reason/summary fields. The client neither creates nor
suppresses audit records; it refetches detail/history after success and conflict
and renders returned versions, actor references, timestamps, and event types.

### Skill compliance

The `brokeros-review-package` skill governs this package and archive. Existing
development, React console, trusted-actor authorization, and configuration
skills were applied. A skill update was evaluated and not required because the
existing React skill already captures the reusable thin-client, conflict,
authorization, cache, and test patterns. The required Q-017 lesson is present.

## Compatibility, operations, and performance

- Existing Q-016 list/detail/add-note flows remain covered in the 103-test suite.
- The development launcher continues to reference the same bootstrap file; its
  documentation now names the Q-017 V1 capabilities.
- Vite reports a non-blocking >500 kB base-chunk warning (768.28 kB minified,
  245.00 kB gzip). No Q-017 bundle-size requirement exists; the warning is not
  hidden and remains an optimization item.

## Cross-system impact

Risk Case REST is consumed unchanged. Q-009 security implementation, Audit,
MySQL, Redis, Kafka, MT4/MT5, external adapters, Compose topology, and Kubernetes
resources have no code/configuration change from this task, except the explicitly
authorized local security bootstrap capability grant under `deploy/`.
