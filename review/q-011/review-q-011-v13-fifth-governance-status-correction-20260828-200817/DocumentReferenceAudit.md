# Q-011 V13 Document Reference Audit

Every `see §N`-style cross-reference within
`docs/requirements/Q-011-Evidence-Provenance-Foundation.md` was checked
individually against the document's actual heading list, and against
what the surrounding sentence was actually trying to point to. No global
find-and-replace was used.

## Actual headings in the Requirement document (post-fix)

```
## Status
## 1. Background
## 2. Existing Capability and Gap Analysis
## 3. Problem Statement
## 4. Goals
## 5. Scope and Non-Goals
## 6. Domain Definitions
## 7. Functional Requirements
## 8. Security Requirements
## 9. Data Integrity and Provenance Requirements
## 10. Acceptance Criteria
## 11. Technical Constraints
## 12. Dependencies
## 13. Verification Plan
## 14. Risks and Architecture Inputs
## 15. Deliverables
## 16. Review Checklist
## 17. Current Gate
## 18. Requirement Architect Review Record (V1 → V2)
## 19. Requirement Correction Record (V2 → V3, Fourth Governance Round)
```

The document ends at §19. There is no §20 and never has been within this
document.

## Every `§20` reference found, before and after

| Location (pre-fix line) | What it was trying to reference | Resolution |
| --- | --- | --- |
| Status intro parenthetical | The four-document round-four finding | Repointed to Implementation Design §20.10 (cross-document record); this document's own share noted as §19 |
| Gate table, "Requirement V3" row | This document's own Goal 5 fix | Repointed to §19 |
| Gate table, "Implementation Allowed" row | The full four-round correction history | Repointed to Implementation Design §20 |
| Requirement version bullet | This document's own Goal 5 fix | Repointed to §19 |
| §17 Requirement status line | This document's own Goal 5 fix | Repointed to §19 |
| §17 Implementation Allowed paragraph, closing sentence | The complete four-document finding-by-finding record | Repointed to Implementation Design §20.10; this document's own share noted as §19 |

## Verification That Target Sections Exist and Match

- `docs/requirements/Q-011-Evidence-Provenance-Foundation.md` §19
  ("Requirement Correction Record (V2 → V3, Fourth Governance Round)")
  exists and its content is exactly the Goal 5 finding these references
  describe.
- `docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`
  §20 ("Design Gaps and Outstanding Decisions") and its subsection §20.10
  ("Fourth governance-consistency round (all four documents)") exist and
  their content is exactly the complete cross-document finding these
  references describe.

## Result

Zero remaining unqualified `§20` references within the Requirement
document itself. All six original references now resolve to a section
that actually exists and whose content matches what the surrounding
sentence claims.
