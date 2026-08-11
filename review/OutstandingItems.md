# Q-004 Outstanding Items

## Remaining Work

- Configure an approved GitHub HTTPS credential or SSH key for the existing
  `origin`, push local commit `33e0e48`, and run `.github/workflows/ci.yml` on a
  Docker-capable GitHub runner; alternatively execute
  `scripts/verify-infrastructure.sh` on an equivalent isolated host.
- Capture actual MySQL health and `flyway_schema_history` evidence, including V1
  checksum/success and post-restart row count.
- Capture Redis PONG/empty-keyspace and Kafka broker API evidence.
- Regenerate the Q-004 Review Package after those checks and obtain architect
  review of Q-004 and ADR-006.

## Known Issues

- This host has no Docker CLI/daemon. A standalone Compose binary can validate
  semantics but cannot start containers.
- The configured GitHub `origin` is readable but cannot be written with the
  available credentials. HTTPS push cannot obtain a username and GitHub rejects
  the available SSH identity. No remote branch or Actions run was created.
- Maven runs on local Java 23 with `--release 21`; Mockito warns that dynamic
  agent loading will be restricted in a future JDK.

## Deferred Work

- CI performance optimization or job splitting; first obtain one successful
  end-to-end run.
- CD and production deployment automation.
- Shared BrokerOS framework extraction; current patterns have one consumer.
- Authentication, authorization, API versioning, business ResultCodes, all risk
  modules, formal Audit, business tables, topics, Redis keys, and adapters.

## Risks

- Q-004 runtime acceptance is incomplete until real MySQL/Flyway, Redis, and
  Kafka checks pass.
- A first business migration could still fail on MySQL despite unit/static
  success.
- Pinned Actions and kubectl versions require maintenance as runner support and
  security releases evolve.

DO NOT START FIRST BUSINESS MIGRATION.

## Suggested Next Step

Provide approved write authentication for the configured GitHub remote and push
`33e0e48` to run the Q-004 workflow, or run the infrastructure script on an
approved local/test host. Resolve any runtime failure, regenerate evidence, and
request architect approval. Do not start Phase 1 business work before that gate
passes.
