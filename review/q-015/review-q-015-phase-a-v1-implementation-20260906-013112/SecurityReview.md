# Q-015 Phase A Security Review

## Boundary and least authority

- The only new entry is authenticated by the existing Q-009 security filters and
  calls `AuthorizationGuard.requireAllowed(..., trading-data:ingest)`.
- Authorization is checked before service-type enforcement and envelope processing,
  retaining default-deny behavior and avoiding capability probing by an unauthorized
  principal.
- The use case then requires `ActorType.SERVICE`; humans/operators cannot ingest
  even if mistakenly assigned the capability.
- `deploy/keycloak/q015-ingestion-bootstrap.json` provisions exactly one `SERVICE`
  actor with exactly `trading-data:ingest`. It is separate from and does not alter
  `q016-security-bootstrap.json`.
- The operation is read-only with respect to trading platforms, brokers, CRMs, and
  customer systems. It writes only its two application-owned ingestion tables and
  publishes the approved internal Kafka message; it performs no account control or
  risk action.

## Input and data handling

- Required numeric fields use `@NotNull` plus range validation; identifiers are
  bounded to 128 safe ASCII characters; platform is restricted to `MT4|MT5`.
- Encoded payload input is capped at 87,380 Base64 characters, and decoded bytes are
  independently constrained to 1..65,535 in the domain and database.
- The envelope defensively copies bytes on construction and access. No parser,
  reflection, deserializer, SQL expression, logger, or metric inspects payload
  content.
- JDBC values are parameterized. Exact payload bytes are bound as binary data and
  no payload column is indexed.
- Kafka transport Base64-encodes the opaque byte array inside the envelope message;
  this is reversible transport encoding, not content interpretation.

## Failure and disclosure behavior

- Expected malformed, actor-type, backpressure, and authority-unavailable outcomes
  use stable result codes and the global exception envelope. Infrastructure causes
  and stack traces are not returned.
- A bounded five-second future prevents an unbounded request wait/buffer path.
- Authorization denial, backpressure, gap, result, and duration are metrics without
  customer payload or credential values.
- No password, token, full authorization header, PII, KYC data, production account,
  or payload content appears in fixtures, source logs, or this package. Review
  commands redact synthetic disposable database credentials.

## Findings

No unresolved secret exposure, payload logging, external-write, authorization, or
operator-privilege defect was found. Before production deployment, Kafka topic ACL,
TLS/authentication, replication/durability, maximum message size, and broker timeout
policy still require environment validation; those were not available to this
SDK-independent/local implementation stage and are not claimed as tested.
