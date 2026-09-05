# Q-020 Implementation Lessons Learned

## Scope

Q-020 added four bounded, natural-key-scoped reference-list reads and connected
them to the existing Risk Case reference input. The implementation changes only
reference discovery: it leaves the four provenance aggregates, all write paths,
the Q-018 association registry and request bodies, capabilities, and Flyway
migrations unchanged.

## What worked

- Giving each module a small list service and a summary projection preserved the
  established controller/application/port/adapter boundary without loading the
  full-detail record. Each JDBC list statement names only the reference, natural
  scope, optional status, and `recorded_at` columns, which makes the content-free
  guarantee visible at the data-access boundary rather than relying only on DTO
  filtering.
- A shared `REFERENCE_LIST_MAX` keeps the four endpoints consistent. Real-MySQL
  fixtures with more than the cap and tied timestamps verify both bounding and
  the `recorded_at DESC, id DESC` deterministic ordering rule.
- Reusing each module's `READ` capability before parsing and querying retained the
  established least-authority failure order and authorization-denial metric while
  deliberately avoiding full-detail access-log records.
- The console repository and kind-specific enabled hooks kept URL construction and
  runtime parsing out of the UI. `ReferenceInput` can switch between a scoped
  searchable list and manual entry, while every selection still goes through the
  existing exact-reference preview before submission.
- Deriving the browse scope from the loaded case subject plus the Q-019 authoritative
  associations projection avoided global discovery. Current decision is preferred;
  an associated decision or action is used only when it is already on the case.

## Problems encountered

- Extending the four query ports initially required updating existing application
  test doubles before test compilation could succeed. Keeping those stubs explicit
  made the new port surface visible to all module tests.
- Standalone MockMvc defaults serialized `Instant` as a numeric timestamp, unlike
  Spring Boot's ISO-8601 application mapper. The Q-020 MySQL harness now installs an
  equivalent Java-time message converter so the endpoint tests verify the actual
  wire shape rather than accepting the harness default.
- A first rerun of the local-database tests was blocked by the filesystem/network
  sandbox. Repeating the same bounded command with approved localhost access reached
  the disposable MySQL instance; no application workaround was introduced.

## Validation boundary

The four focused endpoint tests and the complete backend suite ran from the full
repository checkout against a disposable MySQL 8.4 instance with Flyway V1-V8.
The frontend dependency install, strict type check, complete Vitest suite, and Vite
production build also ran. The MySQL run emitted the existing Flyway support-ceiling
warning for MySQL 8.4, and the Vite build emitted the existing large-chunk advisory;
neither warning caused a failure.

## Reusable guidance evaluation

No new repository skill was added. The established development standards already
cover additive API reads, stable capabilities, Flyway discipline, authorization,
content minimization, and verification. The React console skill already covers
typed boundary parsing, TanStack Query enablement, existing mutation reuse, and
manual fallback behavior. Q-020's natural scope keys and metadata shapes are
requirement-specific and are therefore captured in its design and review package.
