# Approved Baseline Outstanding Items

## Manual action still required

- The Product Owner must review the exact scope and run the proposed staging
  commands manually if accepted.
- After inspecting the staged diff, the Product Owner may create the suggested
  commit. Codex did not stage or commit.
- Post-commit verification commands are included in
  `ProposedStagingCommands.txt`.

## Non-blocking repository policy follow-up

No ZIP is tracked and current ZIPs are review delivery artifacts, but
`.gitignore` has no ZIP rule. A separate governance cleanup may consider a
scoped rule such as `review/**/*.zip` after confirming whether any future ZIP
must be versioned. This task does not modify `.gitignore`.

## Preserved project gates

- Q-008 implementation remains blocked by prerequisites and explicit later
  authorization. No V5 exists or is required.
- Q-009 Architecture and its required ADR remain not started. Requirement
  approval does not select Identity Authority or implementation technology.
- Trading Account, Evidence, Decision, Action, and ActionOutcome provider
  sequencing remains separately owned and unresolved.

## Historical status notes

Q-008 V1/V2/V4 and Q-009 V1 Review files retain the status that was true when
each package was submitted. Later authoritative approval is recorded in the
Q-008 Architect Approval package and the approved Q-009 Requirement/current
baseline record. Rewriting the old snapshots would damage audit history.
