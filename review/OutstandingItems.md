# Phase 0.6 Outstanding Items

## Remaining Work

- Architect review and approval of Q-003, ADR-005, and this Review Package.
- Create the repository's initial reviewed Git commit.
- Run Docker Compose, MySQL/Flyway, Kafka, Redis, and Kubernetes validation in an
  environment that provides the required tools.

## Known Issues

- `git diff --stat` cannot represent untracked files because the repository has
  no commit baseline.
- Maven uses Java 23 with `--release 21`, while the host default `java` command
  points elsewhere; builds are valid but developer setup is confusing.
- Mockito emits a non-failing dynamic-agent warning under the Maven Java 23
  runtime.

## Deferred Work

- CI enforcement for build, tests, Compose config, Kustomize rendering, and
  MySQL-backed Flyway migration verification.
- Optional architecture/static-analysis tools; deferred under YAGNI until
  recurring violations justify them.
- Authentication/authorization, API versioning, business result codes, business
  modules, business tables, topics, Redis keys, Audit implementation, and all
  external integrations; each requires an approved Requirement.

## Risks

- Standards can drift if future Architecture Reviews provide labels without
  inspected-scope evidence.
- Starting business work before an initial commit would continue to weaken diff
  traceability.
- Future numeric ResultCode adoption could be breaking if symbolic codes have
  external consumers; it requires explicit migration design.
- First business migrations remain at risk until validated on real MySQL.

## Suggested Next Step

After architect approval, establish a small Q-004 CI and integration-validation
foundation or explicitly approve the first business Requirement. Before the
first business schema migration, prioritize an initial Git baseline and real
MySQL/Flyway verification.
