# Q-019 Implementation Lessons Learned

## Scope

Q-019 added the authorized, bounded Risk Case association projection read and
switched the React console from history reconstruction to that authoritative
projection. The change reused the existing association tables and effective-state
queries; it did not alter the aggregate, write services, capabilities, or Flyway
migrations.

## What worked

- A dedicated application projection kept persistence entities and internal row
  IDs out of the REST contract while letting the controller remain a thin DTO
  mapping boundary.
- The evidence view intentionally returns every append-only association event,
  because the disposition command needs each event reference. The existing
  effective-evidence query is still executed and bounded to keep the projection
  aligned with the aggregate's effective-state model without reimplementing that
  model in the client.
- Decisions are read from the association table and marked current by comparison
  with the aggregate snapshot. Actions come only from the existing effective-action
  query, preserving withdrawal and outcome semantics already owned by the backend.
- Giving the projection its own TanStack Query key made post-write invalidation and
  version-conflict refetch explicit. Unit/component tests assert both paths, so the
  panel and on-case pickers do not retain a stale history-derived view.

## Problems encountered

- The first real-MySQL projection run exposed a duplicate test decision reference;
  correcting only the fixture prefix produced the intended 501 distinct rows and
  verified the server cap.
- The first full repository run failed five pre-existing failure-injection tests
  because MySQL 8.4 binary logging prevented the disposable test user from creating
  temporary triggers. Setting `log_bin_trust_function_creators=1` on that disposable
  container restored the test harness's required capability; the unchanged full
  command then passed all 309 tests. This was an environment setup correction, not
  an application-code workaround.

## Verification boundary

The backend targeted projection gate and full repository gate ran against a real
disposable MySQL 8.4 instance. The complete frontend unit/component suite,
TypeScript check, and production build also ran. The live Playwright flow was
delivered and discovered, but the current environment had no operator credential
or seeded Risk Case, Decision, and Action references, so its execution truthfully
reported one skipped test.

## Reusable guidance evaluation

No new repository skill was added. The existing
`docs/skills/react-risk-console-development.md` already covers typed API parsing,
query invalidation, conflict refetch, and honest browser-test reporting. The
existing personal `brokeros-review-package` skill governs the non-overwriting
review archive and checksum. Q-019's distinction between append-only evidence
events and effective action state is requirement-specific and is recorded here
rather than generalized into another skill.
