# Q-010 V3 — Architecture + ADR Analysis — Codex Prompt

You are Codex working in the **BrokerOS Risk** repository.

Your task is to execute **Q-010 V3 — Architecture + ADR Analysis** for the already-approved Requirement:

`Q-010 — Trading Account Reference Authority Foundation`

This is an **Architecture / ADR analysis phase only**.

Do **not** implement Q-010.
Do **not** start Implementation Design.
Do **not** implement or unblock Q-008.
Do **not** stage, commit, or push Git changes.
Do **not** mark the new ADR as Accepted on your own.

The Product Owner / external Architect will independently review the Architecture and Proposed ADR after this task.

---

## 1. Mandatory starting point

Before making any change, inspect the current repository state and use the repository itself as the source of truth.

At minimum read and reconcile:

- `AGENTS.md`
- `docs/requirements/Q-010-Trading-Account-Reference-Authority-Foundation.md`
- `docs/lessons/2026-08-26-q-010-requirement-candidate-analysis.md`
- `docs/lessons/2026-08-26-q-010-requirement-architect-review.md`
- `docs/requirements/Q-007-Requirement.md`
- `docs/architecture/q-007-brokeros-domain-foundation-design.md`
- `docs/adr/ADR-009-brokeros-risk-core-domain-model.md`
- `docs/requirements/Q-008-Requirement.md`
- `docs/architecture/q-008-risk-case-foundation-implementation-design.md`
- `docs/adr/ADR-010-risk-case-foundation.md`
- `docs/requirements/Q-009-Requirement.md`
- `docs/architecture/q-009-trusted-actor-authorization-architecture.md`
- `docs/architecture/q-009-trusted-actor-authorization-implementation-design.md`
- `docs/adr/ADR-011-trusted-actor-capability-authorization-architecture.md`
- `docs/skills/development-standards.md`
- `docs/skills/brokeros-risk-core-domain.md`
- `docs/skills/trusted-actor-authorization.md`
- any Q-008/Q-009 architect-approval / closure / review records that materially define current gates.

Also inspect:

- current branch and HEAD;
- `git status --short`;
- current ADR numbering;
- current backend/module/package structure;
- current Flyway baseline and database conventions;
- existing Q-009 ActorContext/capability implementation contracts only as architectural evidence, without changing them.

If the repository differs from the snapshot implied by this prompt, report the difference and follow the committed repository state unless doing so would contradict the approved Q-010 Requirement. Do not silently repair unrelated work.

---

## 2. Gate that is already approved and MUST NOT be reopened

Q-010 Requirement V1 is already **APPROVED**.

Architecture may choose representations and mechanisms only within the approved Requirement boundary. It must not reinterpret, weaken, broaden, or reopen the following decisions.

### 2.1 Authoritative external identity tuple

Every external account identity is exactly:

`AccountAuthorityScopeRef + SourceNamespace + ExternalAccountKey`

Rules:

- `AccountAuthorityScopeRef` is an opaque BrokerOS-owned reference to a broker/deployment authority scope.
- It is **not** a Customer, Broker, Tenant, Organization, CRM entity, or vendor master record.
- `SourceNamespace` identifies one governed external source instance and must distinguish platform/source, server, and environment whenever collisions are possible.
- A raw login/account number, vendor DB ID, source name, MT4/MT5 login, CRM account ID, or opaque caller string alone is never authoritative.

### 2.2 Foundation cardinality

- One complete external identity tuple maps to exactly one `TradingAccountRef`.
- One `TradingAccountRef` has exactly one authoritative external identity tuple.
- No aliases.
- No merges.
- No one-to-many mapping.
- No cross-source migration model.
- No reassignment to another `TradingAccountRef`.
- These require a future approved Requirement.

### 2.3 Lifecycle/history

- Active reference: eligible for new associations.
- Deactivated/retired reference: still historically resolvable but not eligible for new associations.
- Never physically delete authoritative references or identity tuples.
- Never silently reuse or reassign them.
- Reactivation, if Architecture retains it as a future-capable operation, can only restore the **same immutable mapping**, through an explicit authorized operation with durable provenance.

### 2.4 Initial registration authority

Foundation registration/lifecycle authority is:

- controlled;
- application-owned;
- non-web;
- executed by a deployment-designated pre-provisioned Q-009 actor;
- protected by the exact Q-010 capability;
- driven by a complete provisioning manifest;
- backed by a broker/source-owner-approved record;
- carrying a bounded provenance reference.

Forbidden:

- public/admin HTTP registration endpoint;
- caller-driven auto-registration;
- runtime consumer auto-registration;
- unknown authenticated principal registration;
- external-database discovery as registration authority;
- silent account creation from MT4/MT5/CRM data.

### 2.5 Q-008 consumer boundary

Q-008 may only receive a protected read-only application contract that validates a supplied `TradingAccountRef` and returns bounded safe state such as:

- recognized/not found;
- eligible/not eligible for new use;
- bounded authority version/provenance needed by the consuming use case.

Q-008 must not receive:

- `ExternalAccountKey`;
- vendor/source payloads;
- customer data;
- vendor DTOs;
- public external-key lookup behavior.

External-key resolution for a future adapter is a separate protected use case.

### 2.6 Exact Q-010 capabilities

Q-010 owns exactly:

- `trading-account-reference:read`
- `trading-account-reference:register`
- `trading-account-reference:change-lifecycle`

Protected reads and mutations must obtain identity from Q-009 `ActorContext`, authorize before protected data access, and default deny.

Roles, actor headers, source fields, ownership assertions, account IDs, or external attributes must not bypass this boundary.

### 2.7 Durable mutation provenance

Registration and lifecycle changes must durably capture, in the same application-owned transaction as the state change:

- trusted actor;
- UTC time;
- operation;
- immutable target/reference;
- authority/source provenance;
- reason;
- before state;
- after state;
- version.

Q-010 does not create a general Audit module.

### 2.8 Failure semantics

Preserve the approved fail-closed semantics:

- unauthenticated / forbidden / authority unavailable: follow Q-009 and disclose no account existence;
- authorized unknown `TradingAccountRef`: not found;
- inactive/retired ref: recognized but not eligible for new association;
- conflict or ambiguous durable state: safe integrity conflict, never select a winner;
- database/authority unavailable: dependency unavailable, no create/mutate/stale-success fallback.

---

## 3. Architecture work required in V3

Create a complete Q-010 Architecture document that turns the approved Requirement into an implementable architecture boundary, while still stopping before Implementation Design.

The Architecture must explicitly analyze and decide all of the following.

### 3.1 Module ownership and architectural boundary

Define the owning Phase-1 modular-monolith capability/module for Trading Account Reference Authority.

Clarify:

- what Q-010 owns;
- what remains owned by Q-007 Core Domain;
- what Q-009 owns;
- what Q-008 consumes;
- what future Trading Data / MT4 / MT5 / CRM adapters may do;
- what must never leak into the Q-010 domain/application boundary.

Do not create a Trading Account master-data module by accident.

### 3.2 Domain concepts

Define the conceptual responsibility and invariants of at least:

- `TradingAccountRef`
- `AccountAuthorityScopeRef`
- `SourceNamespace`
- `ExternalAccountKey`
- authoritative external identity tuple
- lifecycle state
- authority/provenance reference
- version/concurrency token
- immutable mutation/history record

Do not prematurely freeze exact Java class layouts, package implementation, table DDL, DTO field lists, or framework annotations. Those belong to Implementation Design.

However, the Architecture must be precise enough that Implementation Design will not need to reopen semantic questions.

### 3.3 Reference format and generation strategy

Select the architectural strategy for generating and validating stable opaque BrokerOS `TradingAccountRef` values.

Evaluate realistic alternatives such as:

- database-generated internal ID exposed as business reference;
- UUID-family opaque business references;
- application-generated prefixed opaque references;
- other reasonable BrokerOS-owned schemes.

The selected approach must:

- remain broker/vendor neutral;
- not expose application DB primary keys as domain/business authority;
- be globally safe enough for future broker deployments;
- support immutable historical resolution;
- have an explicit validation/canonical form;
- avoid semantic encoding that later becomes impossible to change.

Do not choose an implementation merely because it is easiest to code.

### 3.4 Authority-scope representation

Define the semantic model of `AccountAuthorityScopeRef` without turning Q-010 into broker/tenant/org master data.

Explain:

- what creates/controls the scope reference conceptually;
- whether Q-010 stores a bounded locally governed scope registry/reference or consumes another authority;
- how unknown/inactive scopes behave;
- how the design avoids coupling to CRM/legal-entity schemas;
- why raw broker/company names are insufficient.

If a separate future authority is required, explicitly identify it as a dependency instead of inventing it.

### 3.5 `SourceNamespace` canonicalization

Define a governed canonical source namespace sufficient to distinguish collisions across source/platform/server/environment.

Architecture must decide:

- conceptual required dimensions;
- canonicalization and comparison rules;
- case sensitivity / binary comparison needs;
- mutability rules;
- how server/environment identity is preserved;
- what happens if a source is renamed externally;
- why a free-form caller string is not the authority.

Do not hard-code MT4/MT5-only semantics into the core.

### 3.6 `ExternalAccountKey` canonicalization

Define how an adapter/provisioning boundary supplies a canonical external key without assuming all systems use numeric logins.

Analyze:

- string vs numeric semantics;
- preservation of leading zeros;
- whitespace normalization;
- Unicode/ASCII boundary as appropriate;
- case sensitivity;
- maximum bounded size;
- logging/redaction requirements;
- uniqueness comparison semantics.

The Architecture must prevent silent collisions caused by unsafe normalization.

### 3.7 Mapping invariants and durable source of truth

Decide the durable authority model for:

- one tuple -> one `TradingAccountRef`;
- one `TradingAccountRef` -> one tuple;
- no reassignment;
- no physical deletion;
- historical resolvability;
- lifecycle state.

Analyze whether the authority should use:

- one aggregate/table boundary;
- separate reference/mapping/history concepts;
- immutable identity tuple plus mutable lifecycle record;
- another coherent relational structure.

Do not write exact DDL yet, but the Architecture must identify the necessary uniqueness and integrity constraints that later Flyway must enforce.

### 3.8 Registration/provisioning manifest

Define the conceptual non-web provisioning use case and manifest boundary.

The Architecture must specify the minimum categories of data required to prove an authorized registration, including:

- authoritative scope ref;
- source namespace;
- external key;
- broker/source-owner-approved provenance reference;
- reason/change ticket or equivalent bounded provenance;
- trusted ActorContext;
- idempotency identity;
- request/operation version where required.

Clarify what is application input versus what is derived server-side.

Do not create an HTTP registration controller.

### 3.9 Idempotency, duplicate, collision and retry semantics

Provide explicit architecture behavior for at least:

1. exact replay of the same valid registration manifest;
2. same tuple submitted again;
3. same tuple with a different proposed `TradingAccountRef` if callers are ever permitted to supply one;
4. same `TradingAccountRef` associated with another tuple;
5. concurrent attempts for the same tuple;
6. concurrent lifecycle mutation;
7. retry after client/process failure with uncertain outcome;
8. durable state that violates expected cardinality because of corruption/manual DB damage.

Required principle: never resolve ambiguity by arbitrarily choosing a winner.

### 3.10 Lifecycle state machine

Define the minimal Foundation lifecycle model and named operations.

At minimum determine whether the architecture uses concepts equivalent to:

- ACTIVE
- INACTIVE / DEACTIVATED
- RETIRED

and whether reactivation is supported now, architecturally reserved, or explicitly deferred.

For every transition specify:

- eligibility for new associations;
- historical resolution behavior;
- authorization capability;
- provenance requirement;
- optimistic/concurrency rule;
- forbidden transitions.

Avoid speculative workflow complexity.

### 3.11 Read-only consumer contract

Define the protected application contract Q-008 can depend on.

It must answer only the domain question needed by Q-008, e.g. whether a supplied `TradingAccountRef`:

- is recognized;
- is eligible for a new Risk Case association;
- has a bounded authority/provenance version that can be safely retained if required.

Analyze whether the contract should return a small result/value type rather than entity/persistence records.

Explicitly prohibit leaking:

- external identity key;
- source details not required by Q-008;
- persistence ID;
- vendor DTO;
- customer identity.

### 3.12 Protected external-key resolution boundary

Architecture may define a separate future/internal application use case for resolving a canonical scoped external identity tuple to `TradingAccountRef`, because a future source adapter will likely need it.

If included:

- keep it protected by Q-009;
- explain its intended consumer boundary;
- do not expose it to Q-008;
- do not expose it as a public lookup/search API;
- do not turn it into auto-registration.

If deferred, explicitly state the dependency and future decision point.

### 3.13 Q-009 security integration

Map Q-010 use cases to the exact approved capabilities:

- read;
- register;
- change-lifecycle.

Architecture must make authorization-before-data-access an invariant.

Also analyze:

- trusted ActorContext acquisition for the non-web provisioner;
- service actor requirements;
- no `SYSTEM` bypass;
- no caller ActorRef;
- existence-disclosure protection;
- no authorization caching that can create stale allow decisions unless separately designed and approved.

### 3.14 Auditability / mutation history

Design an application-owned mutation-history boundary sufficient for Q-010 without creating the general Audit module.

Define:

- atomicity with the state change;
- immutable history semantics;
- before/after state;
- actor attribution;
- operation/reason;
- provenance;
- UTC timestamp;
- version;
- what happens if the history write cannot succeed.

Required direction: fail the mutation atomically; do not commit authority state without its required history.

### 3.15 Transaction and concurrency model

Analyze the required transactional boundary for registration and lifecycle mutation.

Include:

- uniqueness race handling;
- optimistic locking/versioning versus locking alternatives;
- duplicate-key races;
- retry boundaries;
- stale-version rejection;
- idempotent replay;
- consistency of state + history;
- fail-closed handling of DB unavailability.

The Architecture must produce clear invariants for later MySQL/Flyway design.

### 3.16 Database and collation architecture

Q-010 Requirement permits application-owned MySQL only if Architecture approves it.

Make an explicit decision.

If MySQL is selected, explain:

- why it is the durable source of truth;
- why Redis/Kafka are not needed for this Foundation;
- why direct external DB access remains prohibited;
- binary/case-sensitive comparison requirements for identity components;
- uniqueness/index categories required later;
- immutable history retention expectations;
- Flyway requirement;
- MySQL compatibility expectations based on the repository's actual approved runtime baseline.

Do not create migrations in V3.

### 3.17 Failure model

Create an architecture-level failure matrix covering at least:

- no trusted actor;
- missing capability;
- unknown `TradingAccountRef`;
- recognized but inactive/retired ref;
- unknown authority scope;
- invalid source namespace;
- invalid external key;
- exact duplicate registration;
- conflicting mapping;
- stale version;
- corrupted/ambiguous durable state;
- DB unavailable;
- required provenance missing/invalid.

Keep unauthorized existence disclosure aligned with Q-009.

Do not invent public ResultCodes unless this phase genuinely requires an architecture decision; Implementation Design owns concrete code mapping unless an existing stable contract forces it.

### 3.18 Threat analysis

At minimum cover:

- account spoofing by raw login;
- cross-server/source collision;
- cross-environment collision;
- external account-number reassignment;
- unauthorized registration;
- confused deputy behavior from source adapters;
- forged provenance;
- replay/idempotency abuse;
- race-condition duplicate mapping;
- unauthorized existence probing;
- log leakage of external account keys;
- manual database corruption;
- stale cache/replica success (even if caching is not selected).

### 3.19 Q-008 dependency effect

Explicitly state what Q-010 Architecture would satisfy for Q-008 and what Q-008 prerequisites remain unsatisfied.

Q-010 V3 must **not** say that Q-008 implementation is authorized unless every separate approved prerequisite is actually implemented and the external Architect explicitly authorizes Q-008 later.

---

## 4. ADR analysis is mandatory

Q-010 Requirement explicitly requires an ADR.

Inspect existing ADR numbering in the repository. If `ADR-011` is still the latest accepted ADR and no competing ADR has been added, create the next candidate as:

`docs/adr/ADR-012-trading-account-reference-authority-foundation.md`

If numbering has changed, use the correct next available ADR number and explain the deviation in the Review Package.

### ADR status

The new ADR must be created as **Proposed** / **Awaiting Architect Review** (or the repository's equivalent pre-acceptance status).

Do **not** mark it `Accepted`.
Do **not** invent an Architect approval date.
Do **not** imply approval from this Codex task.

### ADR must include

At minimum:

- Context
- Decision
- Alternatives Considered
- Consequences
- Security implications
- Data/integrity implications
- Operational implications
- Dependencies
- Deferred decisions

The ADR should capture the durable architectural decisions rather than copy the entire Architecture document.

### Alternatives to evaluate honestly

Evaluate meaningful alternatives, including where appropriate:

- using raw MT4/MT5/CRM account IDs as BrokerOS identity;
- direct external-system lookup as runtime authority;
- a full Trading Account master-data module;
- auto-registration on first observation;
- public/admin HTTP provisioning;
- one-to-one immutable mapping versus alias/migration-capable mapping;
- database identity versus independent opaque BrokerOS business reference;
- MySQL durable authority versus cache/event-only authority;
- mutable mapping versus immutable identity + lifecycle.

Reject or defer alternatives with explicit reasons tied to Q-010, Q-007/ADR-009, Q-009/ADR-011, security, integrity, broker neutrality, and Phase-1 scope.

---

## 5. Architecture document output

Create a dedicated Architecture document following repository naming conventions, preferably:

`docs/architecture/q-010-trading-account-reference-authority-architecture.md`

Use the actual repository convention if different.

Set status honestly, e.g.:

- Requirement: APPROVED
- Architecture: PROPOSED / AWAITING ARCHITECT REVIEW
- ADR: PROPOSED / AWAITING ARCHITECT REVIEW
- Implementation Design: NOT STARTED
- Implementation: NOT STARTED
- Implementation Allowed: NO

The architecture document must include enough detail to support an independent Architect review without reading Codex's chat output.

---

## 6. Requirement / existing document updates

You may update Q-010 governance/status documentation only where necessary to record that Architecture V1/V3 submission and a Proposed ADR now exist.

You must NOT:

- change approved Q-010 Requirement semantics;
- rewrite the approved V2 Requirement review decision;
- mark Architecture approved;
- mark ADR accepted;
- start Implementation Design;
- change Q-008 to implementation-authorized;
- modify Q-009 accepted architecture semantics.

Preserve historical approval records as immutable chronology unless the repository's governance explicitly requires a new additive status record.

---

## 7. No implementation in this phase

The following are forbidden in Q-010 V3:

- production Java implementation;
- Java tests for a not-yet-approved design;
- Flyway migration;
- SQL schema;
- REST endpoint/controller;
- Spring configuration;
- new Maven dependency;
- Kafka topic/event;
- Redis key/cache;
- MT4/MT5/CRM adapter implementation;
- Docker/Kubernetes behavior change;
- public provisioning API;
- Q-008 implementation;
- staging/commit/push.

If you discover that an architecture decision cannot be made safely without a missing Requirement or external fact, record the blocker explicitly instead of inventing behavior.

---

## 8. Verification required in V3

Because this is an architecture-only phase, verification must distinguish documentation/governance verification from runtime validation.

At minimum execute and record:

- repository status / branch / HEAD inspection;
- `git diff --check`;
- changed-file scope inspection;
- search confirming no forbidden implementation artifacts were introduced;
- search for accidental secrets/credentials in changed files;
- ADR numbering/status verification;
- cross-document consistency check for Q-007/Q-008/Q-009/Q-010 gates;
- architecture traceability check against all Q010-FR and Acceptance Criteria;
- verify no generated review ZIP is unintentionally tracked/staged;
- final `git status --short`.

You may run Maven tests if useful as a baseline sanity check, but if no production code changed, label them honestly as baseline/regression evidence rather than evidence that Q-010 is implemented.

Do not claim runtime validation of a design that has not been implemented.

---

## 9. Lessons / skills

Create an honest Q-010 Architecture-phase Lessons Learned document if repository workflow requires one, for example:

`docs/lessons/2026-08-27-q-010-trading-account-reference-authority-architecture.md`

It must capture reusable lessons from actual analysis only, especially around:

- stable internal reference authority versus external account identity;
- scope/source/key collision prevention;
- no-reassignment history semantics;
- provisioning attestation versus authorization;
- authorization-before-resolution;
- durable integrity under concurrency.

Evaluate whether a reusable Skill update/new skill is warranted.

Do **not** create a skill merely to satisfy a checkbox. If no reusable skill change is justified before architecture approval, record that assessment in the Review Package.

---

## 10. Mandatory versioned Review Package

Do not overwrite a previous review package.

Create a new unique review directory for this run using the repository's established versioned/timestamped convention, representing:

**Q-010 V3 — Architecture + ADR Analysis**

The review directory must contain at least:

- `Summary.md`
- `ArchitectureReview.md`
- `ProjectTree.txt`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `Verification.md`
- `OutstandingItems.md`
- `PhaseReviewIndex.md` if this repository currently uses it
- any other review artifact required by the committed `AGENTS.md`

Also produce a ZIP of **that exact new review directory** using the established repository convention.

Do not overwrite earlier Q-010 V1/V2 review directories or ZIPs.

The Review Package must clearly state:

- phase = Q-010 V3 Architecture + ADR Analysis;
- Requirement = approved;
- Architecture = awaiting external Architect review;
- ADR = proposed / awaiting external Architect review;
- Implementation Design = not started;
- Implementation = not started;
- Implementation Allowed = NO;
- Q-008 remains unimplemented and prerequisite-gated;
- whether Q-010 Architecture has any blocker;
- exact changed files;
- exact verification actually executed;
- no staging/commit/push performed.

### `ArchitectureReview.md` mandatory content

Include a substantive `Development Standards Compliance` section with evidence for:

- AGENTS.md compliance;
- Q-007 / ADR-009 core-domain compliance;
- Q-008 / ADR-010 consumer-boundary compliance;
- Q-009 / ADR-011 trusted actor/capability compliance;
- Q-010 approved Requirement compliance;
- module/boundary compliance;
- API boundary compliance;
- database/Flyway standards compliance;
- security compliance;
- auditability/history compliance;
- external-system isolation compliance;
- skill/lessons compliance.

Do not write only `Compliant`; cite the inspected files and explain why.

---

## 11. Required architecture review questions

Before concluding, answer these explicitly in the Architecture document or Review Package:

1. What exactly owns `TradingAccountRef`?
2. Why is a raw MT4/MT5 login not a TradingAccountRef?
3. What prevents account `12345` on two servers/environments from colliding?
4. What prevents an external system from reassigning an old account number and silently changing historical meaning?
5. What prevents one tuple from mapping to two BrokerOS refs under concurrency?
6. What prevents one BrokerOS ref from mapping to two tuples?
7. Is the external identity tuple immutable after registration?
8. What states remain historically resolvable?
9. What states are eligible for a new Q-008 Risk Case association?
10. What is the exact protected contract Q-008 is allowed to consume?
11. Can Q-008 see an external account key? The expected answer under the approved Requirement is **No**.
12. How does a non-web provisioner obtain a trusted Q-009 ActorContext?
13. Why does `register` authorization not by itself prove that the mapping is true?
14. What provenance/attestation proves registration authority?
15. How does exact replay/idempotency behave?
16. How are conflicting retries handled?
17. How is state + immutable history made atomic?
18. What happens when history persistence fails?
19. What happens when MySQL is unavailable?
20. Are Redis or Kafka required? If not, why not?
21. Does Architecture require any new dependency/framework? If not, state that clearly.
22. Which decisions are intentionally deferred to Implementation Design?
23. Which decisions require a future Requirement rather than Implementation Design?
24. What Q-008 prerequisite does Q-010 satisfy, and what prerequisites remain?
25. Is implementation now authorized? Expected answer: **No, pending external Architect approval and later Implementation Design approval.**

---

## 12. Final Codex response format

When finished, respond concisely with:

1. **Q-010 V3 result** — PASS / BLOCKED for Architecture submission readiness.
2. **Architecture document** — path and status.
3. **ADR** — number, path, and `Proposed/Awaiting Architect Review` status.
4. **Key decisions** — compact summary.
5. **Files changed** — compact list.
6. **Verification** — commands/checks and results.
7. **Outstanding items / blockers**.
8. **Review directory** — exact path.
9. **Review ZIP** — exact path.
10. **Git state** — explicitly confirm whether anything was staged/committed/pushed.
11. **Next gate** — state exactly:

`External Architect Review of Q-010 Architecture + Proposed ADR. Do not start Implementation Design until explicitly approved.`

Do not ask the Product Owner to choose implementation details that Architecture can resolve from the approved Requirement and repository standards. If a genuine requirement-level ambiguity remains, document it as a blocker instead of guessing.
