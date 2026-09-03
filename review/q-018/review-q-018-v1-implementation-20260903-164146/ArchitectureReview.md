# Q-018 Architecture Review

## Gate Decision

**BLOCKED** — the React implementation follows the approved Q-018 documents and
all locally executable checks pass, but the approved reference formats conflict
with the committed backend and the approved on-case/current-association UX lacks
the required backend read data. Live acceptance cannot be achieved without a
governance/API-contract decision outside this implementation authorization.

## Requirement and architecture alignment

- The implementation is a thin-client expansion of the existing Q-016/Q-017
  React SPA. Association invariants, authorization, identity, concurrency, and
  audit creation remain in the existing Risk Case backend.
- The six Group C commands extend the Q-017 descriptor registry and use the
  unchanged Q-017 `useCaseAction` mutation runner. No parallel execution
  framework or client aggregate was introduced.
- External references use one typed preview adapter over existing module reads.
  Preview cards expose only reference, related subject/decision/action, source,
  status where applicable, and recorded time; sensitive content fields are not
  parsed or rendered.
- Group C buttons are hosted in `AssociationsPanel`; Q-017 lifecycle actions stay
  in their existing status-filtered bar.
- The panel clearly distinguishes authoritative `currentDecisionRef` from the
  incomplete reconstruction available through the first bounded history page.

## ADR-020 and Design compliance

| Decision | Evidence | Result |
| --- | --- | --- |
| Six Group C operations | Six descriptors, typed repository methods, exact request tests | PASS (mocked backend) |
| Reuse Q-017 registry/runner | Existing registry extended; existing mutation runner unchanged | PASS |
| Option A existing GET previews | Four paths in `referencePreview.ts`; no new endpoint | PASS at client boundary |
| Approved external formats | Exact `ev-/dc-/ac-/ao-` UUIDv4 validators | PASS against Design; BLOCKED against committed backend |
| Preview required before submit | `ReferenceInput` confirmation gates modal OK | PASS |
| On-case disposition event picker | Event ID absent from detail/history; no GET preview exists | FAIL / BLOCKER |
| On-case decision/action pickers | Detail/history projection supplies associated/current decisions and active actions | PASS within loaded page |
| Current AssociationsPanel | Explicitly bounded history reconstruction; only current decision authoritative | PARTIAL / BLOCKER |
| Success/conflict refresh | Shared runner invalidates all risk-case queries and conflict-refetches active detail | PASS |
| Security capability grant | Five requested capabilities added to existing actor | PASS statically |
| Live association slice | Spec delivered and discovered; 1 skipped | NOT VERIFIED |

## Contract and dependency finding

The architecture assumes the approved reference grammar matches the source
modules. Direct inspection shows:

- `EvidenceRef` accepts `ev-` (aligned).
- `DecisionRef` accepts `dec-`, not approved Q-018 `dc-`.
- `ActionRef` accepts `act-`, not approved Q-018 `ac-`.
- `ActionOutcomeRef` accepts `aoc-`, not approved Q-018 `ao-`.

Opaque references must not be silently rewritten. The implementation prompt
orders contradictions toward the approved documents, so Q-018 validation remains
on the approved short forms and the incompatibility is surfaced as a blocker.
The lower-risk resolution is to amend Q-018 to consume the established backend
formats rather than migrate persisted identifiers and every existing contract.

The history response contains only `version`, `eventType`, `affectedRef`,
`actorRef`, and `occurredAt`. It does not contain the evidence
`associationEventRef`, a replacement evidence ref, or an outcome ref. Consequently
the same response cannot satisfy the disposition picker or a complete current
association projection.

## Development Standards Compliance

### AGENTS.md compliance

The execution protocol, architecture decision principles, root AGENTS.md,
approved Q-018 Requirement, ADR-020, Architecture, Implementation Design,
cleared prompt, ADR-018/019 context, Q-009 security governance, development
standards, applicable repository skills, and recent Lessons Learned were
inspected before and during implementation. Scope did not expand to prohibited
backend work, and this stage stops with an explicit BLOCKED decision.

### Architecture compliance

The product remains broker-, CRM-, and trading-platform-neutral. The frontend
consumes existing application APIs through typed adapters and does not access a
database, external CRM, broker system, trading platform, Kafka, or Redis. Risk
Case business rules and source-reference recognition remain server-owned.

The unresolved reference/history dependencies are named rather than hidden.
Because they prevent the approved UX and live interoperability, the architecture
cannot be marked fully compliant or PASS.

### ADR compliance

ADR-018's React/TypeScript/Vite/Ant Design/TanStack/axios stack and ADR-019's
registry/runner approach are preserved. ADR-020's Option A and six-operation
scope are implemented without a new endpoint. No new framework, service
boundary, persistence technology, or dependency was introduced, so no new ADR
is warranted by the code. The contract discrepancy requires ADR-020/Q-018
governance amendment before closure.

### API standard compliance

No application-owned backend API was added or changed. All six POST bodies mirror
the existing DTO field sets, include `expectedVersion`, omit actor identity, and
use encoded dynamic path segments. Existing `ApiResponse` parsing, Bearer
transport, typed 403 behavior, and unknown-ResultCode failure handling are
reused. The approved short reference formats remain incompatible with three
source-module GET/domain contracts, preventing a clean API compliance result.

### Database standard compliance

`git diff --stat -- backend/` and Git status for Java/Flyway paths are empty.
There is no SQL, migration, schema, money, timestamp-storage, data movement, or
database access change. No applied migration was edited.

### Security standard compliance

The centralized Q-016 Bearer/refresh transport is reused; no JWT claims or
capabilities are parsed by the client. The bootstrap adds only the five approved
capabilities. All six request-body tests assert `actorRef` is absent. React text
rendering is used without raw HTML/eval. Playwright trace, screenshot, and video
remain disabled. Source and review scans found no usable secret; test credentials
and references are synthetic placeholders.

The manual disposition-event fallback cannot be preview-confirmed. It is visibly
warned and format-checked, but this is weaker than the approved data-integrity
control and contributes to the BLOCKED gate.

### Auditability compliance

All six commands carry the displayed expected version and their approved reason/
source fields. Successful commands trigger detail/history refetch, and returned
versions/events are parsed. The client neither creates nor suppresses backend
audit records. The history read's missing event/replacement/outcome identifiers
limits the frontend's ability to display the complete audit-linked association
state and is recorded as a blocker.

### Skill compliance

The personal `brokeros-review-package` skill governs this package and verified
archive. Existing development, React console, trusted-actor authorization, and
configuration guidance were applied. A repository skill change was evaluated
and not made: `docs/skills/react-risk-console-development.md` already captures
the reusable thin-client, typed parsing, cache, conflict, security, and honest
live-test patterns. Q-018-specific contract defects are recorded in the required
Lessons Learned rather than generalized as an implementation pattern.

## Compatibility, operations, and performance

- The final 148-test suite includes all inherited Q-016/Q-017 tests; separating
  the Q-017 `act-` resolution validator from the Q-018 `ac-` validator removed an
  initially detected regression.
- The same development security-bootstrap file remains mounted by Compose; five
  capabilities are additive to the existing operator set.
- Vite reports a non-failing >500 kB base chunk warning: 772.91 kB minified,
  245.82 kB gzip. No Q-018 bundle-size criterion exists; the warning is retained.

## Cross-system impact

Only the React console and the explicitly authorized development/security
bootstrap grant change at runtime. Backend domain, persistence, migrations,
Audit, MySQL, Kafka, Redis, adapters, Compose topology, and Kubernetes resources
are unchanged. Required governance and review artifacts are additional
non-runtime files.
