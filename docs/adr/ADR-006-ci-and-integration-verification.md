# ADR-006: CI and Integration Verification

- Status: Accepted
- Date: 2026-08-11

## Context

BrokerOS Risk has an approved engineering foundation but previously lacked a
Git baseline and automated evidence for Maven, Docker Compose, real
MySQL/Flyway startup, Redis, Kafka, and Kustomize rendering. A durable CI choice
affects every later Requirement and should be explicit before business schema
work begins.

## Decision

Use GitHub Actions as the initial CI provider with one verification-only
workflow for pushes and pull requests. Use Java 21 and blocking Maven test and
package checks. Run repository whitespace/shell validation, Kustomize rendering,
Docker Compose semantic validation, and isolated infrastructure verification in
the same workflow.

Keep the substantive verification logic in small POSIX shell scripts owned by
the repository so developers and another CI provider can run the same checks.
Pin external Actions to reviewed commit SHAs, grant only `contents: read`, do
not persist checkout credentials, provide no production Secret, and perform no
deployment.

Generate Compose credentials at verification time. Run Compose under a unique
project name and remove only that isolated project's resources after the run.
Treat Maven, Compose, MySQL/Flyway, Redis, Kafka, and Kustomize assertions as
blocking when their verification script executes.

## Alternatives

- Documentation-only manual checks were rejected because they do not prevent
  regression or provide repeatable evidence.
- Jenkins, GitLab CI, and a custom CI abstraction were not selected because the
  repository has no existing provider constraint and GitHub Actions provides a
  smaller initial configuration.
- Separate workflows per component were deferred because the current project is
  small and the extra coordination would not improve correctness.
- Testcontainers or a new Java integration-test framework was deferred because
  the current requirement can verify the real packaged Compose stack without
  adding backend dependencies or test-only Java abstractions.
- A shared BrokerOS framework/starter was rejected because there is only one
  real consumer.

## Consequences

- Every push and pull request can produce repeatable build and infrastructure
  evidence before merge.
- CI depends on GitHub-hosted runner capabilities and the pinned setup Actions;
  revisions require deliberate maintenance.
- Integration verification is slower than unit testing because it builds and
  starts the full local/test stack.
- The scripts remain portable enough for local use or a later CI migration, but
  resource names and Compose behavior intentionally remain Risk-specific.
- CI is not CD: no production credential, cluster access, or deployment step is
  introduced.
