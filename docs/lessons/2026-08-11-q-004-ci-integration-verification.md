# Q-004 CI and Integration Verification Lessons Learned

## What was implemented

Q-004 established initial commit `8bf42bc`, a minimal GitHub Actions workflow,
repository-owned static/Kustomize/Compose verification scripts, loopback-only
local ports, Q-004 architecture and ADR documentation, and evidence-based
review updates. It added no business implementation or business data contract.

## Why this design

One workflow and three small POSIX scripts are enough for the current single
deployable. Keeping assertions in repository scripts makes local and CI behavior
consistent without creating a CI abstraction layer. Ephemeral credentials and
unique Compose projects make integration checks isolated and safe to clean up.

## Alternatives considered

- Manual-only verification was rejected because it cannot prevent regression.
- Multiple CI workflows were deferred because the foundation is small.
- Testcontainers and new Java test dependencies were deferred because the
  existing Compose stack is the artifact that needs verification.
- A shared BrokerOS framework was rejected because there is only one consumer.

## Environment issues

The local host has no Docker CLI/daemon, kubectl, or kustomize. A checksum-
verified temporary kubectl completed all Kustomize renders. A checksum-verified
standalone Docker Compose binary completed semantic config validation, but it
cannot start containers without a Docker daemon.

An `origin` remote was later configured for the GitHub repository and read
access was confirmed. Commit `33e0e48` was created to trigger the workflow, but
the host has neither HTTPS credentials nor an accepted SSH key for GitHub.
HTTPS push failed before upload with `could not read Username`; SSH
authentication failed with `Permission denied (publickey)`. No remote branch or
workflow run was created, so CI and runtime checks remain PARTIAL/NOT EXECUTED.

## Problems encountered

The first pre-baseline Maven test attempt ran inside a restricted filesystem
sandbox. Mockito/Byte Buddy could not attach to Java 23 and Surefire could not
create temporary files. Re-running the unchanged command with the required host
permissions passed all 12 tests; no product-code fix was appropriate.

The initial staged root commit also exposed many extra blank lines at EOF through
`git diff --cached --check`. A mechanical EOF normalization fixed the actual
commit content before the baseline was created.

## Lessons learned

- A real staged diff is necessary before an initial commit; an unstaged
  `git diff --check` cannot inspect untracked files.
- Semantic configuration, rendering, runtime startup, and real integration are
  separate evidence levels and must be reported separately.
- A CI workflow file can be syntax-valid and actionlint-valid while remaining
  PARTIAL until a runner actually executes it.
- A reachable remote is not equivalent to an authenticated CI execution path;
  preflight must distinguish remote discovery, write authentication, workflow
  dispatch, and completed-run evidence.
- Infrastructure cleanup must be scoped by a unique project name, not by broad
  resource deletion.

## Reusable patterns

- Requirement-first CI design with an explicit provider ADR.
- SHA-pinned read-only Actions and no production Secret exposure.
- Ephemeral credentials plus unique Compose project ownership.
- Flyway row, checksum, no-business-table, and restart-idempotence assertions.
- Component/status/evidence verification matrix.

## Future risks

- Docker/MySQL/Flyway/Redis/Kafka remain unverified at runtime until the workflow
  runs on a Docker-capable runner; the first business migration is blocked.
- Pinned Action and kubectl revisions require deliberate maintenance.
- The local Maven runtime is Java 23 with release 21, and Mockito dynamic-agent
  warnings should be addressed before a JDK disables that behavior.
