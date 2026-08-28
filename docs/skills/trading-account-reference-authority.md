# Trading Account Reference Authority Skill

## When to use

Use this guidance for work that creates, resolves, changes, or consumes a
BrokerOS TradingAccountRef or AccountAuthorityScopeRef. Read Q-010, its approved
Architecture V1, ADR-012, approved Implementation Design V1, Q-009 security
guidance, and `development-standards.md` first. Those authorities win if this
skill ever differs.

## Identity boundary

- BrokerOS owns opaque `ta-<canonical-lowercase-UUIDv4>` references.
- An external account identity is the complete immutable scope + family +
  instance + server + environment + external-key tuple. Never treat a login,
  CRM ID, or platform field alone as authoritative.
- Validate namespace values without trimming, case folding, normalization, or
  vendor-specific rewriting.
- Preserve the external key's exact validated UTF-8 bytes in `VARBINARY`; never
  parse it as a number or expose it in logs, errors, metrics, or bounded
  consumer contracts.
- Do not add remap, merge, alias, repair, or delete behavior without a new
  Requirement and architecture decision.

## Authorization and mutation pattern

- Reuse Q-009 ActorContext and exact Q-010 capabilities. Authorization occurs
  before every authority lookup or mutation port call; only explicit ALLOW
  proceeds.
- Controlled provisioning is a one-manifest, non-Web command backed by a
  registered service descriptor and an active Q-009 mapping/grant. There is no
  public or admin REST provisioning surface.
- Hash a typed, length-framed semantic payload, not raw JSON. Operation ID,
  actor, timestamps, generated references, and JSON property order are excluded
  from the fingerprint.
- Store the operation outcome durably. Same operation ID plus same fingerprint
  replays the stored result; a changed fingerprint conflicts.
- Current state, final operation outcome, and exactly one immutable history row
  commit in one local transaction. A history/outcome failure rolls everything
  back.
- Use named MySQL uniqueness constraints as the final race arbiter and classify
  only MySQL 1062/SQLState 23000 plus an exact known constraint. Unknown
  integrity or connection failures fail as authority unavailable.
- Lifecycle changes use status-and-version CAS. Never retry with a fresh
  version, apply last-write-wins, or add a Redis/distributed lock.

## Read-only consumer boundary

Q-008 and future consumers receive only the Q-010-owned bounded eligibility
result. It may include opaque snapshot/provenance refs but never scope refs,
versions, lifecycle details, external identity, internal IDs, attestation,
actor data, or persistence records. Stored inactive/retired rows remain
recognized historical authority; only active account plus active scope is
eligible for a new association.

## Verification

- Unit-test reference, namespace, UTF-8 key, lifecycle, evidence, and fingerprint
  invariants.
- Prove application authorization precedes any authority port interaction.
- Run V1→V2→V3 and persistence tests against disposable MySQL 8.4. H2 or SQL
  inspection alone does not satisfy the gate.
- Exercise concurrent first registration, concurrent duplicate delivery,
  generated-ref collision, lifecycle CAS, CHECK enforcement, and forced
  operation/history rollback.
- Run the real non-Web command with an authoritative Q-009 service mapping and
  grants, and assert safe output omits the manifest's sensitive fields.
- Verify domain/application imports remain free of Spring, JDBC, Jackson,
  servlet, Kafka, Redis, and vendor APIs.
