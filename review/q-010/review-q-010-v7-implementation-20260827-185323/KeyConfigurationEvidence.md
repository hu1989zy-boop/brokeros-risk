# Q-010 V7 Key Configuration Evidence

- No dependency was added or version changed in `backend/pom.xml`.
- `SecurityModuleConfiguration` now injects Spring's collected
  `Set<RegisteredServiceDescriptor>` into the existing Q-009
  ServiceActorContextFactory; Q-010 contributes one purpose-specific descriptor.
- Q-010 adds no application property, permissive security flag, profile,
  scheduler, listener, watcher, REST route, Kafka topic, Redis key, or external
  connection.
- Existing DataSource, JDBC transaction manager, Flyway, ObjectMapper,
  Micrometer, Spring Security, and UTC Clock beans are reused.
- `scripts/verify-static.sh` now expects V1/V2/V3 and exactly four Q-010 tables.
- `scripts/verify-infrastructure.sh` now verifies the V3 Flyway row, seven total
  Q-009/Q-010 application tables, and V1/V2/V3 restart idempotence.
- The successful infrastructure run used a temporary, deleted Compose overlay
  to remove host port publication because port 3306 was already occupied. The
  repository Compose configuration itself was not changed.
