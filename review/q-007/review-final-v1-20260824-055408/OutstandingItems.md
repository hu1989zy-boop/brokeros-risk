# Q-007 Outstanding Items

## Blocking

Blocking Items: None

## Non-blocking

- The 38-file Q-007 candidate is staged but intentionally uncommitted; no CI run
  can exist for this exact candidate until a later approved commit/push step.
- Local verification used Java 23.0.2 while Maven compiled with `release 21`.
  The project-standard Java 21 environment remains the authoritative CI/runtime
  target.
- Docker, `kubectl`, and `kustomize` are unavailable locally. Their checks are
  not applicable because the staged design-only candidate changes no Docker or
  Kubernetes files.
- The first sandboxed `mvn package` attempt could not initialize Mockito's
  inline agent. The same command passed with JVM attachment permitted; no source
  or test change was made.
- `review/review-history/` remains untracked and intentionally untouched.
- The stash named `pre-q-007 unrelated review work` remains intact and was not
  inspected or restored.
- The standalone review directory and ZIP are external review artifacts and are
  intentionally not staged.

## Future Considerations

These are non-blocking architecture candidates only and require future approved
Requirements/ADRs before any change:

- Consider generalizing Trading Data to Observation when non-trading evidence
  sources justify it.
- Consider an Evidence Chain model for explicit lineage.
- Consider Decision metadata such as confidence, reason, rule version, trace ID,
  and creation time.
- Define Rule Engine behavior in a future Requirement.
- Define AI-assisted Decision behavior and explainability in a future Requirement.

No future consideration alters the Q-007 baseline.

## Commit Readiness

Ready for Git Commit: YES

Architect approval remains the process gate. This review task did not commit,
push, restore a stash, or start Q-008.

====================================
Codex Prompt
====================================

After Architect approval, commit only the existing 38-file staged Q-007
candidate. Do not add `review/q-007/review-final-v1-20260824-055408/`, its ZIP,
`review/review-history/`, build artifacts, or stash content. Re-run
`git diff --cached --check`, confirm the staged count and scope remain unchanged,
then commit using a project-compliant message. Do not push or begin Q-008 unless
the Product Owner separately authorizes those actions.
