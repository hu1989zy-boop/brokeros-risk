# Q-015 Phase A Implementation Lessons Learned

## Scope

Q-015 Phase A added an SDK-independent, payload-opaque ingestion foundation: a
Q-009-authorized `SERVICE` entry point, metadata-only reliability handling, Kafka
publication, two new application-owned tables, synthetic replay coverage, and a
public-document research note. It did not add a gateway, Manager API abstraction,
canonical trading fields, native translation, or any Q-008 through Q-014 behavior.

## What worked

- Keeping arbitrary payload bytes in an immutable envelope and limiting every
  reliability decision to envelope metadata made the Phase A/Phase B boundary
  enforceable in code and architecture tests.
- The plain MySQL unique key on `(source_server_id, source_sequence)` gives global
  de-duplication correctness. MySQL's rule that every partitioned-table unique key
  include the partition expression would weaken that invariant, so the Design's
  documented non-partitioned V1 fallback was the safer choice.
- A `BLOB` supports the approved 65,535-byte payload maximum without pushing a
  `VARBINARY(65535)` into InnoDB's inline row-size limit. Domain, request, and
  database checks all enforce the same byte boundary.
- A narrow Kafka sender seam allowed topic, key, byte serialization, timeout, and
  failure mapping to be tested without an embedded broker. Hand-written doubles
  also avoid Mockito's runtime self-attachment dependency in constrained JDK
  environments.
- Wrapping database append, gap marker creation, and the synchronous publish call
  in one local transaction rolls database state back when publication fails. This
  closes the otherwise permanent “persisted duplicate that can never be
  republished” failure mode.

## Problems encountered

- Adding a package-private constructor for the Kafka test seam meant Spring no
  longer had a single constructor to choose. Marking the production constructor
  explicitly with `@Autowired` restored deterministic application wiring.
- The first full-database run exposed existing trigger-based test fixtures that
  require `log_bin_trust_function_creators=1` under MySQL 8.4. That setting was
  applied only to the disposable test container; no production configuration was
  changed.
- MySQL partitioning and global source-sequence uniqueness cannot both be expressed
  with the intended keys. Shipping the authorized non-partitioned fallback must be
  treated as a visible Phase A condition, not described as partitioned storage.

## Residual boundaries

- A local JDBC transaction cannot atomically commit Kafka and MySQL. A Kafka
  acknowledgement followed by database commit failure can still create a published
  message without its row, and a retry can publish again. An outbox, Kafka
  transaction, or equivalent coordination mechanism requires a future approved
  design; Phase A does not authorize a third table or new delivery contract.
- The V1 gap lookup uses the highest accepted sequence and does not serialize
  concurrent requests for the same source server. Sequential ingestion is covered;
  concurrent per-server cursor ownership and gap reconciliation remain future
  design work.
- Gap markers are durable and visible, but actual snapshot/resync belongs to the
  Phase B gateway. Public MetaTrader documentation informs research only and is not
  evidence of Manager SDK support.

## Verification boundary

Focused Q-015/Flyway contract tests passed 24/24. The final full-repository backend
gate ran from the complete checkout against a disposable MySQL 8.4 database with
all Q-008 through Q-015 MySQL suites enabled: 342 tests passed with zero failures,
errors, or skips. The run emitted Flyway's support-ceiling warning for MySQL 8.4 and
Docker's legacy-link warning; neither was a test failure.

## Reusable guidance evaluation

No new repository skill was added. Existing development standards already govern
opaque integration boundaries, Flyway migrations, stable capabilities, failure
mapping, and honest verification. The non-partitioned fallback, sequence
concurrency, and cross-resource delivery limitations are specific to the accepted
Q-015 Phase A design and are recorded here and in its review package.
