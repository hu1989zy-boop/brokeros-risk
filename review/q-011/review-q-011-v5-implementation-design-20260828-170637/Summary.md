# Q-011 V5 Implementation Design Summary

- Review ID: `Q-011-V5-IMPLEMENTATION-DESIGN-20260828-170637`
- Prepared by: Claude Code, holding the external Architect role by explicit
  Product Owner direction (2026-08-28); self-authored, not independently
  reviewed.
- Inputs: approved Q-011 Requirement V2, approved Architecture V1, accepted
  ADR-013.
- Git baseline: `main` at `091d410`, clean, synced with `origin/main`.

## Deliverable

`docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`
(833 lines) — module/package layout, domain types, exact DDL for three
tables (`evidence_record`, `evidence_operation`, `evidence_operation_history`,
plus `evidence_access_log`), HTTP contract, idempotency/fingerprint design,
transaction/concurrency design, ResultCode table, security review, and a
7-part test design, following the same structural rigor as
`q-010-trading-account-reference-authority-implementation-design.md`.

## Notable Design Choices Beyond a Literal Architecture Restatement

- **Correction copies the target's subject rather than validating a
  submitted one** (Design §6.2): stronger than Requirement's minimum
  ("reject if mismatched") — makes subject-swap structurally impossible
  instead of merely checked.
- **Correction never re-calls Q-010** (Design §9): explicitly reasoned —
  re-validating eligibility at correction time would let an account
  becoming ineligible after initial recording block an otherwise-valid
  correction, reintroducing the exact problem Architecture's scope-
  narrowing was meant to avoid.
- **No new Q-009 SERVICE descriptor** (Design §5.2): unlike Q-010, every
  Q-011 use case runs under the calling `HUMAN`'s own context; no
  composition-root change to `ServiceActorContextFactory` is needed.
- **`supersedes_id` as a nullable-unique column** (Design §8.1): reuses a
  standard MySQL technique (multiple NULLs permitted in a unique index) to
  enforce "at most one correction per target" as a database constraint,
  not just an application check.

## What This Package Is Not

Not Product Owner approval. Not implementation authorization. Nothing has
been staged or committed.

## Next Step

Product Owner review and explicit approval decision on Implementation
Design V1. Implementation authorization (a separate, later decision) is not
implied by Design approval, matching every prior Q-008/009/010 gate.
