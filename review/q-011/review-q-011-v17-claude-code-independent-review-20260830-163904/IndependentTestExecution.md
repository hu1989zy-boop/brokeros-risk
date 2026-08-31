# Q-011 Independently Executed Test Run (Docker / Java 21 / MySQL 8.4)

This supplements `Summary.md`, which was based on direct code/DDL reading
only and explicitly disclosed that no test suite had been independently
executed. That gap is now closed: the full Maven test suite was actually
run by Claude Code, from a clean checkout of the current working tree,
in a disposable environment — not read from, not copied from, Codex's own
report.

## Environment

- Maven/JDK image: `maven:3.9.9-eclipse-temurin-21-alpine` (the same image
  `backend/Dockerfile`'s build stage uses), pulled fresh.
- Database: `mysql:8.4` official image → resolved to MySQL **8.4.11**,
  same patch version Codex's own `Verification.md` reports. Disposable
  container, its own Docker network, removed after the run.
- Both containers were stopped/removed and the network deleted at the end
  of this review; nothing was left running.

## Run 1 (mount artifact — recorded for transparency, not a code defect)

The first attempt mounted only `backend/` as the container's working
directory. Result: `Tests run: 124, Failures: 2, Errors: 7`. Investigation
of the 7 errors (`Cannot locate repository root`, `NoSuchFile
.../evidence`, `.../tradingaccount`) traced to
`SecurityArchitectureTests.repositoryRoot()` (and the equivalent helpers in
`EvidenceArchitectureTests`, `TradingAccountArchitectureTests`,
`ConfigurationContractIntegrationTests`), which locate the repo root by
checking whether the working directory or its parent contains a `docs/`
folder. Mounting only `backend/` removed that sibling `docs/` folder from
the container's filesystem entirely, so every reflection-based test that
depends on walking the real repository layout failed for a reason that
has nothing to do with the code under test. This was **my** setup error,
not Codex's or the repository's.

## Run 2 (corrected mount — the result that counts)

Mounted the full monorepo root, working directory `/workspace/backend`
inside the container, identical `Q009_MYSQL_TEST_URL`/`Q010_MYSQL_TEST_URL`/
`Q011_MYSQL_TEST_URL` (+ `_USERNAME`/`_PASSWORD`) environment variables all
pointing at the same disposable MySQL instance, matching how Codex's own
`Verification.md` describes running the "all three enabled" repository-wide
gate.

```
docker run --rm --network brokeros-q011-verify-net \
  -v "<repo-root>":/workspace -w /workspace/backend \
  -v brokeros-q011-verify-m2:/root/.m2 \
  -e Q009_MYSQL_TEST_URL=... -e Q009_MYSQL_TEST_USERNAME=root -e Q009_MYSQL_TEST_PASSWORD=... \
  -e Q010_MYSQL_TEST_URL=... -e Q010_MYSQL_TEST_USERNAME=root -e Q010_MYSQL_TEST_PASSWORD=... \
  -e Q011_MYSQL_TEST_URL=... -e Q011_MYSQL_TEST_USERNAME=root -e Q011_MYSQL_TEST_PASSWORD=... \
  maven:3.9.9-eclipse-temurin-21-alpine \
  mvn --batch-mode --no-transfer-progress test
```

**Result: `Tests run: 124, Failures: 2, Errors: 0, Skipped: 0`.** All 7
mount-artifact errors are gone, confirming they were purely environmental.
Every Q-011-owned test class passed (`EvidenceApplicationTests`,
`EvidenceArchitectureTests`, `EvidenceMetricsTests`, `EvidenceDomainTests`,
`EvidenceRestContractTests`, `Q011MySqlMigrationTests`,
`Q011MySqlPersistenceTests`, `Q011SecurityMySqlIntegrationTests`, and the
`FlywayMigrationTests` addition) — matching Codex's own 37/37 claim,
now independently reproduced rather than merely read.

Exactly two failures remain, both **pre-existing and unrelated to Q-011's
own logic**:

### 1. `Q009MySqlIntegrationTests` — already known (AC15)

```
Q009MySqlIntegrationTests.verifiesMigrationConstraintsQueryPlansAndPersistenceLifecycle:63
expected: 1
 but was: 3
```

Confirms Codex's own reported AC15 failure exactly. `git diff --stat` on
this file (already checked in the prior code-review pass) is empty —
Codex did not touch it. This is a stale hard-coded post-V1 migration count
(now correctly 3: V2+V3+V4), unrelated to Q-011.

### 2. `Q010BootstrapMySqlIntegrationTests` — a NEW finding, not in Codex's report

```
Q010BootstrapMySqlIntegrationTests.controlledCommandUsesTrustedServiceAuthorizationAndExactReplay:67
expected:
  "... occurredAt=2026-08-30T10:18:49.442501179Z
  "
 but was:
  "... occurredAt=2026-08-30T10:18:49.442501Z
  "
```

This test invokes the Q010 bootstrap command twice with the same manifest
(an idempotent-replay check) and asserts the two textual outputs are
byte-for-byte identical. The `occurredAt` field differs only in its last 3
digits: the first invocation's output carries genuine nanosecond-level
precision from `Instant.now()`, while MySQL's maximum fractional-seconds
column precision is 6 digits (microseconds) — so whatever the replay path
reads back from the database has the sub-microsecond digits truncated
away. Reproduced identically (same truncation pattern, different random
UUID) across both runs, so this is deterministic given this environment's
clock, not a one-off flake.

This is a **real, pre-existing latency/precision inconsistency in Q010's
bootstrap replay path** (the first-call output and the replay output pull
the timestamp from two different precision domains: raw in-memory
`Instant` vs. a DB round-trip). It did not appear in Codex's own
verification because that ran on a macOS/JBR21 host whose clock happened
not to expose sub-microsecond entropy for `Instant.now()`; this
Linux/Alpine JVM's clock does. It is **unrelated to Q-011** — Q-011 never
calls this code path, and nothing in the Q-011 diff touches
`TradingAccountAuthorityBootstrapCommand` or Q010's timestamp handling.

I did not attempt to fix this — it is outside Q-011's authorized scope
(touching Q010), and disposition (fix, waive, or track as a lesson) is a
Product Owner decision like AC15.

## Cleanup

`docker stop`/`docker rm` on the MySQL container, `docker network rm` on
the verification network, and `docker volume rm` on the Maven `.m2` cache
volume were all run after the second test run. No container, network, or
volume from this verification remains.

## Conclusion

Independent execution (not just code review) confirms: every Q-011-owned
test passes, and the only two failures in the full repository-wide gate
are both pre-existing, unrelated to Q-011, and outside Q-011's authorized
change boundary. One (AC15/Q009) was already known from Codex's own
report; the other (Q010 timestamp-precision replay mismatch) is new,
found only because this verification ran on a different OS/JVM clock than
Codex's host.
