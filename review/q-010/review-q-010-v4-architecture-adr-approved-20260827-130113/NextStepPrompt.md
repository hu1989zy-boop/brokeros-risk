# Q-010 V4 Independent Architect Review Prompt

You are the independent Architect reviewing BrokerOS Risk Q-010 V4 —
Architecture + ADR-012 Approval Recording.

Review exactly:

- `docs/requirements/Q-010-Trading-Account-Reference-Authority-Foundation.md`;
- `docs/architecture/q-010-trading-account-reference-authority-architecture.md`;
- `docs/adr/ADR-012-trading-account-reference-authority-foundation.md`; and
- `review/q-010/review-q-010-v4-architecture-adr-approved-20260827-130113/`.

Verify that:

1. the supplied external decision is faithfully recorded as Q-010
   Architecture V1 APPROVED and ADR-012 ACCEPTED;
2. ADR acceptance records date/origin and is not Codex self-approval;
3. Requirement substance and V3 Architecture decisions did not change;
4. Implementation Design and implementation remain NOT STARTED;
5. Implementation Allowed remains NO;
6. no Java, Flyway, API, dependency, configuration, runtime, Q-008, or Q-009
   implementation change was introduced;
7. the historical static issue is distinguished from V4 scope; and
8. the Review directory and ZIP are complete and internally consistent.

Return one decision:

- `Q-010 V4 APPROVAL RECORDING: APPROVED`; or
- `Q-010 V4 APPROVAL RECORDING: CHANGES REQUIRED`, with exact repair items.

If approved, confirm whether a separately instructed Q-010 Implementation
Design phase may begin. Do not authorize implementation and do not treat this
Review as Git commit permission. Any recommended Codex work must include a
complete, directly executable Prompt under the repository's Prompt Delivery
Policy.
