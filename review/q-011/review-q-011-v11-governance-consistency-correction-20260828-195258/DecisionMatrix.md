# Q-011 V11 Decision Matrix

Confirms the same locked decision is stated identically, in substance,
across all four documents. "Locked" means: not reopened by this round; any
future change requires a new explicit Product Owner decision, not a
self-correction.

| Decision | Requirement (V3 candidate) | Architecture (V4 candidate) | ADR-013 (amendment) | Implementation Design (V5 candidate) |
| --- | --- | --- | --- | --- |
| Recognized subject bar | `Q011-FR-002`: subject need only be Q-010-recognized | §9: accepts `ELIGIBLE_FOR_NEW_ASSOCIATION` and `RECOGNIZED_NOT_ELIGIBLE` | Subject validation (amended): same two outcomes accepted | §6.1/§11.4/§13: same, `EVIDENCE_SUBJECT_NOT_RECOGNIZED` only on `NOT_RECOGNIZED` |
| `RECOGNIZED_NOT_ELIGIBLE` (e.g. inactive/retired) | Explicitly in scope, not future | Explicitly in scope (§9, §22 no longer lists it) | Explicitly in scope (amended Deferred Decisions) | Explicitly in scope (§20.9 no longer lists it) |
| `NOT_RECOGNIZED` | Only rejecting outcome | Only rejecting outcome | Only rejecting outcome | Only rejecting outcome (§16.2 tests it) |
| Record `ActorType` | `Q011-FR-005`: `HUMAN` required | §12/§16: `HUMAN` required | Context/Security Implications: `HUMAN` required | §11.1/§11.4 step 3: `HUMAN` required, never skipped even on replay |
| Correct `ActorType` | `Q011-FR-005`: `HUMAN` required | same | same | same |
| Provenance-read `ActorType` | Goal 5 (fixed): no additional restriction | Implicit (no `HUMAN` requirement stated for reads) | Consumer boundary (amended): any authorized actor type | §11.4: not required, any authorized `ActorType` |
| Full-detail-read `ActorType` | Goal 5 (fixed): no additional restriction | same | Consumer boundary (amended): any authorized actor type, explicitly not `HUMAN`-restricted | §11.4/§7.3: not required, any authorized `ActorType` |
| Q-008 consumer limitation | Narrow provenance contract only (§10.3-equivalent) | §11: narrow contract only | Consumer boundary (amended): contract limitation, not an actor-type rule | §10.3: narrow contract only, in-process |
| Full-detail access audit | FR-014: access-log before disclosure | §11.2/§13: access-log row before content | Durable authority section: unchanged, mirrors `RISK_CASE_VIEWED` | §7.3/§11.2/§12.4: access-log insert before content, short dedicated (not database-read-only) transaction |
| Correction and Q-010 | FR-007: correction never separately specifies, but subject must match target | §9: correction never calls Q-010, hard rule | Subject validation (amended): unaffected — correction behavior was already correctly stated | §9/§11.1: correction never calls Q-010, new or replay |
| Replay order | Implicit via FR-009 (idempotent) | §10: replay returns stored result | Unaffected (ADR does not specify code-level ordering) | §11.1 (authoritative): auth+`HUMAN` first, never skipped; replay check next; content/Q-010/target-status only for new operations |
| Current approval/authorization state | V2 approved; **V3 candidate pending** | V3 approved; **V4 candidate pending** | Accepted (original); **amendment pending re-acceptance** | V4 approved; **V5 candidate pending** |

## Verdict

Every locked decision is stated identically in substance across all four
documents as of this round's edits. No cross-document contradiction
remains for any row above. The only remaining variance is each document's
own gate/version state, which is expected — they are candidates awaiting
the same round of Product Owner approval, not yet a single approved
baseline.
