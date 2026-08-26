# Lessons Learned

Create one honest entry after every phase or important Requirement using:

```text
YYYY-MM-DD-<phase-or-requirement>.md
```

Each entry contains:

- What was implemented
- Why this design
- Alternatives considered
- Problems encountered
- Lessons learned
- Reusable patterns
- Future risks

Do not invent incidents to make the document look complete. When no material
problem occurred, write `No significant implementation issue encountered.`

Lessons Learned records experience from completed work. Durable instructions
belong in `AGENTS.md`, architecture documents, accepted ADRs, or `docs/skills`.

Completed Requirement entries include:

- `2026-08-23-q-007-brokeros-domain-foundation.md` — aligning the final
  Evidence/Decision/Action/Risk Case baseline, separating Action from Execution,
  and preserving explainability for future AI work.
- `2026-08-26-q-009-trusted-actor-authorization-implementation.md` — preserving
  authentication/mapping/authorization separation, validating the signed-JWT
  filter boundary, keeping MySQL verification honest, and avoiding implicit
  privilege paths.
