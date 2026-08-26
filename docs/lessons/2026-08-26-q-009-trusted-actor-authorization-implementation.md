# Q-009 Trusted Actor and Authorization Implementation Lessons Learned

## Current Status

- Implementation authorization: Yes — explicit authorization received
  2026-08-26
- Implementation code: Present in the worktree
- Mandatory Runtime Verification: Pass — V9, MySQL 8.4.11 and complete
  zero-skip regression
- Architect Implementation Review: Pass / Approved — V10, 2026-08-26
- Implementation complete: Yes
- Blocking verification: None
- Ready for Git commit: Yes — technical gate only; commit not performed

## What Was Implemented

Q-009 adds framework-neutral actor, principal, context, capability, decision,
and lifecycle types; application ports and fail-closed services; Spring
Security signed-JWT authentication; exact authoritative JDBC actor mapping and
capability decisions; controlled non-web provisioning; a three-table additive
Flyway V2 migration; stable safe security ResultCodes; configuration catalog
updates; and focused domain, application, boundary, architecture, bootstrap,
migration, and MySQL integration tests.

Q-008 remains unchanged. No identity-provider vendor, role model, cache,
administration API, Kafka topic, external service protocol, or Audit module was
introduced.

## Why This Implementation

The implementation preserves the approved separation between authentication,
actor mapping, and authorization. Spring Security terminates at infrastructure;
application use cases receive immutable BrokerOS types and must invoke the
authorization guard explicitly. This prevents framework roles or a successful
credential from silently becoming business permission.

MySQL remains authoritative for both actor activation and direct capability
grants. Capabilities are intentionally absent from ActorContext, so each use
case observes current state rather than a stale per-request entitlement copy.

The service path uses a code-owned descriptor registry plus the same active
database mapping, which avoids fake service JWTs and a generic SYSTEM bypass
inside the Phase 1 monolith.

## Alternatives Kept Out

- H2 was not substituted for MySQL because it cannot prove MySQL collation,
  CHECK, index, Flyway, or query-plan behavior.
- Spring test security shortcuts were not used as the primary authentication
  proof; boundary tests sign RSA JWTs and traverse the real decoder/filter
  chain.
- JWT roles/scopes, actor headers, JIT provisioning, roles, Redis policy cache,
  and an online administration endpoint remain excluded.
- A second JWT library or pinned Nimbus version was not added; the Boot-managed
  resource-server stack supplies one Nimbus dependency.

## Problems Encountered

The first sandboxed Maven run could not initialize Mockito's inline Byte Buddy
agent because the sandbox denied the JVM attach/temp-directory mechanism. The
same suite passed outside that restriction: 58 tests, 0 failures, 0 errors, and
1 deliberately skipped MySQL test.

The repository's static script also scans all historical untracked files. It
was blocked by trailing whitespace in a pre-existing V6 staging prompt that the
implementation authorization forbids modifying. The Q-009 tracked diff and all
new Q-009 source/test/migration files pass whitespace checks independently.

The original implementation host provided no Docker or safe disposable MySQL
8.4 runtime, so V8 correctly left the completion gate closed. After the operator
provided Docker Desktop, V9 used isolated MySQL 8.4.11 containers and the
repository Compose stack without touching the pre-existing host MySQL 5.7.

Actual MySQL execution exposed a test-only translation assumption: MySQL CHECK
constraint violation 3819/HY000 is a Spring `DataAccessException` but is not
guaranteed to be `DataIntegrityViolationException`. The corrected assertion
still requires the exact vendor error code and SQL state, so it proves the
constraint without coupling the test to an unstable Spring subtype. The
targeted test then passed 1/1 with zero skips and the full suite passed 58/58
with zero skips.

## Reusable Lessons

- Map a validated principal to a server-owned actor before replacing the
  framework authentication context.
- Catch authentication translation and actor-mapping failures narrowly; do not
  wrap downstream application exceptions as authentication failures.
- Replace the framework principal with an immutable BrokerOS authentication
  object containing no credentials or authorities.
- Use complete-filter-chain signed JWT tests to prove issuer, audience, time,
  spoofing, safe response, and context cleanup behavior.
- Keep an executable real-MySQL suite present even when the current host cannot
  run it; skip honestly and keep the completion gate closed until it executes.
- For vendor-specific CHECK enforcement, assert the exact SQL error code/state
  beneath Spring's `DataAccessException`; do not assume all JDBC drivers and
  translators choose the same narrower exception subtype.
- Update infrastructure assertions when an approved migration changes the
  schema baseline; historical “zero business tables” checks otherwise become
  false requirements.

## Future Risks and Required Follow-up

- Supply environment-specific issuer, audience, optional JWK URI, and actual
  provisioning manifests through deployment governance before rollout.
- Add Mockito as an explicit test JVM agent when the project adopts a JDK where
  dynamic agent loading is disabled by default; no build change is required for
  the current Java 21 target.
- Track the Flyway 11.7.2 advisory about formal MySQL support through normal
  dependency maintenance; real MySQL 8.4.11 migration/runtime verification
  passed and no closure-time dependency change was justified.
- Recheck the exact Q-008 capability matrix only in a later separately
  authorized Q-008 integration phase.
