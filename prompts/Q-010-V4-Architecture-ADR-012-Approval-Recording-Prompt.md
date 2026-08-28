# Q-010 V4 — Architecture + ADR-012 Approval Recording Prompt

## Role

You are Codex working inside the BrokerOS Risk repository.

This task is **Q-010 V4 — Architecture + ADR-012 Approval Recording**.

The Architect has completed an independent review of the Q-010 V3 Architecture + ADR Analysis package and issued the following decision:

- Q-010 Requirement: APPROVED
- Q-010 Architecture: APPROVED
- ADR-012: APPROVED FOR ACCEPTANCE RECORDING
- Implementation Design: NOT STARTED
- Implementation: NOT STARTED
- Git Commit: NOT YET ALLOWED

Your job in this round is only to record the approved architecture decision into the repository and produce a new review package.

Do **not** re-open requirement decisions that were already approved in Q-010 V2.
Do **not** redesign the approved architecture unless you discover a direct contradiction, corruption, or missing artifact that makes approval recording impossible.
Do **not** start implementation design.
Do **not** implement production code.

---

# 1. Mandatory Preflight

Before changing files:

1. Read and obey the repository root `AGENTS.md`.
2. Inspect the current Git branch, HEAD, working tree, and existing Q-010 artifacts.
3. Read:
   - Q-010 Requirement
   - Q-010 V3 Architecture document
   - ADR-012 candidate/proposed ADR
   - relevant Q-009 artifacts and ADRs referenced by Q-010
   - review index / outstanding items / lessons learned conventions already used by the repository
4. Confirm that the V3 architecture content materially matches the Architect-approved decision summarized below.
5. Preserve unrelated user changes and existing uncommitted work.
6. Do not overwrite older review packages. Create a new versioned review directory/package.

If repository reality differs from the assumptions below, follow the repository as source of truth and explain the discrepancy in the review package. Do not silently invent missing history.

---

# 2. Architect Approval Decision to Record

The following Q-010 architecture decisions are approved and must be recorded as the current architecture baseline.

## 2.1 Stable Trading Account Identity

`TradingAccountRef` is a BrokerOS-owned stable business identity.

It must not be derived from, replaced by, or treated as equivalent to:

- MT4 login
- MT5 login
- external platform account number
- CRM account ID
- vendor database primary key
- adapter persistence ID
- mutable external identifiers

The currently approved representation is a BrokerOS-generated opaque identifier, such as the V3-defined `ta-<UUIDv4>` form, unless the V3 artifact uses an equivalent already-reviewed representation.

Do not redesign this in V4.

## 2.2 External Account Authority Identity

An external account is identified under an authority boundary by the combination of:

- `AccountAuthorityScopeRef`
- `SourceNamespace`
- `ExternalAccountKey`

The tuple is the authoritative external identity key.

This protects BrokerOS from collisions across servers, environments, platforms, vendors, or other external authorities.

Do not collapse this tuple to only a login/account number.

## 2.3 Immutable Mapping

The mapping between external authority identity and `TradingAccountRef` is append-only / non-reassignable in the identity sense.

Approved constraints include:

- no silent remapping
- no reuse of one external identity for a different `TradingAccountRef`
- no reuse of one `TradingAccountRef` for a different external identity
- no destructive delete that erases historical resolution
- no hidden migration through ordinary CRUD

Historical identity continuity must be preserved.

## 2.4 Lifecycle

The approved lifecycle model includes the V3-defined account/scope lifecycle such as:

- ACTIVE
- INACTIVE
- RETIRED

Historical resolution remains possible after deactivation/retirement.

New Q-008 Risk Case associations may only be created when the required account and authority scope eligibility are ACTIVE according to the approved V3 contract.

Do not reinterpret INACTIVE or RETIRED as physical deletion.

## 2.5 Concurrency and Uniqueness

Uniqueness must be enforced by the authoritative database, not only by application-side “check then insert”.

The approved architecture requires database-level bidirectional uniqueness / equivalent constraints so concurrent provisioning cannot create duplicate or split-brain mappings.

Provisioning must be transactionally safe and idempotent.

## 2.6 Provisioning Boundary

Identity provisioning is approved as a controlled **non-public-Web** workflow.

The V3 architecture uses a controlled mechanism such as:

- manifest-driven provisioning
- controlled operator/process invocation
- attestation/evidence
- deterministic idempotency and conflict handling

Do not add an admin REST API in this round.

## 2.7 Authorization Integration

Q-010 reuses Q-009 `ActorContext` / capability authorization.

Authorization must occur before sensitive identity lookup/resolution where required by the architecture.

The design must avoid external account existence probing through unauthorized calls.

Do not create a parallel authorization model.

## 2.8 Q-008 Consumer Contract

Q-008 may consume only the bounded account authority / eligibility contract approved in V3.

Q-008 must not receive or depend on raw identity-source details such as:

- MT login
- `ExternalAccountKey`
- `SourceNamespace`
- database internal IDs
- CRM customer data
- vendor DTOs
- infrastructure persistence entities

Q-008 remains read-only with respect to Q-010 identity ownership.

Q-010 does not unlock Q-008 implementation in this V4 task.

## 2.9 Persistence Authority

MySQL / the repository-approved relational persistence remains the authoritative identity source.

Redis and Kafka are not authoritative identity registries.

Do not introduce cache/event authority semantics during approval recording.

## 2.10 Audit / Immutable History

Approved state transitions and identity provisioning outcomes require immutable historical evidence.

Where the V3 architecture requires registry mutation plus history/event evidence to be committed in one local transaction:

- preserve that requirement
- history failure must cause rollback where specified
- do not weaken atomicity in V4

---

# 3. ADR-012 Approval Recording

Locate the actual ADR number and file used by the V3 package.

The Architect review expects this to be `ADR-012`, but the repository is authoritative.

If it is indeed ADR-012:

1. Change its decision/status from proposed/candidate to the repository's normal accepted/approved state.
2. Record the Architect acceptance date as the current repository task date.
3. Preserve the original problem statement, context, alternatives, decision, trade-offs, consequences, rejected options, and references.
4. Do not rewrite the ADR into a different design.
5. Add a concise approval/decision-history note only if consistent with repository conventions.

If the actual repository ADR number differs, do not create a duplicate ADR just to satisfy the prompt. Record the real ADR and explain the discrepancy in the review output.

---

# 4. Q-010 Architecture Status Recording

Update the Q-010 Architecture artifact so its status clearly reflects Architect approval.

Expected state after this task:

- Requirement: APPROVED
- Architecture: APPROVED
- ADR: ACCEPTED / APPROVED according to repository convention
- Implementation Design: NOT STARTED
- Implementation: NOT STARTED

The architecture document must continue to describe the V3-approved architecture.

This is an approval-recording task, not an architecture rewrite.

---

# 5. Requirement Status

Do not modify the substance of the approved Q-010 Requirement.

Only adjust cross-references/status metadata if needed to reflect that Architecture and ADR approval now exist.

If Requirement is already correctly marked APPROVED, leave it alone.

---

# 6. Explicit Prohibitions

You MUST NOT perform any of the following in V4:

- write production Java code
- create domain/application/infrastructure classes
- implement repositories/services/controllers
- implement provisioning commands
- implement CLI/runtime tooling
- create REST endpoints
- create Flyway migrations
- create or modify runtime database schema
- implement Redis usage
- implement Kafka events
- change Docker/Kubernetes runtime behavior
- implement Q-008
- implement Q-009 changes
- create Implementation Design
- mark Implementation Design as approved
- mark Implementation as allowed
- change approved Q-010 architectural semantics
- broaden Q-010 scope
- fix unrelated repository issues unless strictly required to produce review evidence
- perform `git commit`
- perform `git push`

If an unrelated issue is discovered, document it in `OutstandingItems.md` instead of fixing it.

---

# 7. Historical Static Verification Issue

The Architect review noted a prior static verification issue:

- `scripts/verify-static.sh` may return exit code `2`
- the cause was previously identified as whitespace in a historical Q-009 prompt/artifact
- it was not introduced by Q-010 V3

In V4:

1. Re-run applicable repository verification.
2. If the same historical issue remains, record it accurately.
3. Do not modify historical Q-009 artifacts merely to make the gate green.
4. Distinguish:
   - newly introduced V4 failures
   - pre-existing unrelated failures
5. Q-010 V4 must introduce no new static verification regression.

---

# 8. Documentation / Governance Updates

Update only the governance artifacts normally required by this repository for an Architect approval recording.

Where applicable, update:

- Q-010 Architecture status
- ADR status
- Q-010 skill/status references if repository convention requires it
- phase/review index
- lessons learned
- outstanding items
- summary
- architecture review / approval record
- verification record

Do not fabricate artifacts that the repository convention does not use.

If a Q-010 Skill exists and needs only status/reference synchronization, make the minimum necessary update.

Do not create implementation instructions inside the Skill.

---

# 9. Review Package Requirements

Create a new review directory for this round without overwriting V1/V2/V3.

Use the repository's established naming pattern.

Preferred semantic label:

`q-010-v4-architecture-adr-approved`

The package must contain enough evidence for an independent Architect to verify exactly what changed.

Include at minimum, where repository conventions permit:

## `Summary.md`

State:

- task: Q-010 V4 Architecture + ADR Approval Recording
- Requirement status
- Architecture status
- ADR status
- Implementation Design status
- Implementation status
- files changed
- explicit statement that no production implementation was performed
- explicit statement that no Git commit/push was performed

## `ArchitectureReview.md`

Record the Architect decision being applied:

- Architecture APPROVED
- ADR approved for acceptance recording
- key approved invariants
- no implementation authorization

This is a recording of the supplied Architect decision, not a new self-approval by Codex.

## `Verification.md`

Include:

- Git branch and HEAD
- working tree status before/after where possible
- documentation/static verification performed
- result of each command
- any pre-existing failure separately identified
- confirmation that no runtime implementation was added

## `GitStatus.txt`

Capture final `git status`.

## `GitDiffStat.txt`

Capture final diff stat.

## `ProjectTree.txt`

Include the relevant Q-010 / ADR / review artifact tree; do not dump enormous irrelevant dependency trees.

## `OutstandingItems.md`

Clearly state:

- Implementation Design is still pending
- Implementation remains prohibited
- next expected phase is Q-010 Implementation Design only after separate instruction
- any pre-existing unrelated static issue
- any genuine architecture follow-up found during recording

## `PhaseReviewIndex.md`

Update or provide the repository-convention index entry showing V4 approval recording and preserving earlier review history.

## `LessonsLearned.md`

Only add lessons that are actually justified by this phase.
Do not generate generic filler.

---

# 10. Review ZIP

Create a ZIP archive of the V4 review directory.

Preferred name:

`review-q-010-v4-architecture-adr-approved-<timestamp>.zip`

Do not overwrite any previous review ZIP.

Verify that the ZIP contains the intended review artifacts before finalizing.

---

# 11. Git Rules

Do not commit.

Do not push.

At the end, the working tree should contain only the changes required for this V4 approval recording plus any unrelated pre-existing user changes that were already present.

Do not stage files unless repository tooling strictly requires it; if anything becomes staged, report it explicitly.

The Architect/user will decide when Git commit is allowed.

---

# 12. Acceptance Criteria

V4 is complete only when all of the following are true:

1. V3-approved Q-010 Architecture is formally marked approved.
2. The approved ADR is formally marked accepted/approved according to repository convention.
3. Requirement remains approved without semantic drift.
4. Implementation Design remains NOT STARTED.
5. Implementation remains NOT STARTED / NOT ALLOWED.
6. No production Java/runtime code has been added.
7. No Flyway/database implementation has been added.
8. No Q-008 implementation has been started.
9. No Q-009 redesign has been introduced.
10. No public/admin Web provisioning API has been introduced.
11. Review/governance artifacts are updated.
12. A new V4 review directory exists.
13. A new V4 review ZIP exists.
14. Verification distinguishes new failures from pre-existing unrelated ones.
15. No Git commit or push has been performed.

---

# 13. Final Codex Response Format

When finished, respond with a concise status report containing:

1. `Q-010 V4 Result`
2. Requirement status
3. Architecture status
4. ADR number + final status
5. Implementation Design status
6. Implementation status
7. verification result
8. any pre-existing/non-blocking issue
9. exact path to the V4 review directory
10. exact path to the V4 review ZIP
11. Git status summary
12. explicit statement:

`Ready for Architect Review: YES`

Do not say `Ready for Git Commit: YES`.

The next action after this task is independent Architect review of the V4 package.
