# Q-010 V7 Independent Architect Implementation Review Prompt

You are reviewing BrokerOS Risk Q-010 V7 — Trading Account Reference Authority
Foundation Implementation.

Review the exact package:

`review/q-010/review-q-010-v7-implementation-20260827-185323/`

and its ZIP/hash. Treat Q-010 Requirement V1, Architecture V1, accepted ADR-012,
and approved Implementation Design V1 as authoritative. Verify implementation,
V3 schema, Q-009 authorization integration, identity non-disclosure, durable
idempotency/history, concurrency/CAS, real MySQL evidence, bounded Q-008 facade,
scope exclusions, standards evidence, Git status, and the known pre-existing
Q-009 whitespace classification.

Do not approve Q-008 implementation, Final Closure, commit, or push as part of
this review.

Return one formal decision:

- `Q-010 V7 IMPLEMENTATION: APPROVED`; or
- `Q-010 V7 IMPLEMENTATION: CHANGES REQUIRED` with exact blocking findings.

Also state:

- Requirement V1 status
- Architecture V1 status
- ADR-012 status
- Implementation Design V1 status
- architecture/design deviation: YES/NO
- Final Closure authorized: YES/NO
- Git commit authorized: YES/NO

If further Codex work is required, finish with a complete ready-to-execute
prompt under:

```text
====================================
Codex Prompt
====================================
```
