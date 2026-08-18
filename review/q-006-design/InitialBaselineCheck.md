# Q-006 Initial Baseline Check

## Captured Baseline

- Date: 2026-08-18 (Asia/Shanghai)
- Branch: `main`
- HEAD: `f693128eb381564bc8f5f1fed02f2d933e9f2822`
- `origin/main`: `f693128eb381564bc8f5f1fed02f2d933e9f2822`
- Latest commit: `feat: establish Q-005 observability baseline`
- Q-005 CI: run `32104955908` PASS

## Initial Working Tree Before Q-006

```text
## main...origin/main
 M docs/lessons/2026-08-11-q-004-ci-integration-verification.md
 M review/ArchitectureReview.md
 M review/GitDiffStat.txt
 M review/GitStatus.txt
 M review/OutstandingItems.md
 M review/PhaseReviewIndex.md
 M review/Q-004-Patch-01.md
 M review/Summary.md
 M review/Verification.md
?? review/review-history/
```

These changes existed before Q-006 Design work. Q-006 does not own them. The
root Review files are the uncommitted Q-005 final closure update; the Q-004
lesson and Patch-01 are separate pre-existing documentation. The untracked
review-history archive is user-owned and protected.

## Source and Configuration Inventory

- Production Java had zero `@Value` usages.
- Production Java had zero `@ConfigurationProperties` types or scans.
- Production Java had no direct `Environment`, `System.getenv`, or
  `System.getProperty` access.
- Runtime configuration files were limited to existing base/test/prod YAML,
  Compose, Kubernetes manifests, and CI/deployment scripts.
- `V1__initial_schema.sql` remained the only Flyway migration.
- No business module, table, Kafka topic/event, or Redis business key existed.

## Preservation Decision

Q-006 uses `review/q-006-design/` for its Design Review Package rather than
overwriting the root Q-005 Review Package. The protected
`review/review-history/` archive is excluded from tree/static inspection and is
not read, modified, deleted, staged, or committed.
