# Outstanding Items

## Blocking

1. Run the opt-in Q-009 migration/repository suite against a dedicated
   disposable MySQL 8.4 database. The current host has no Docker, MySQL
   client/server, or reachable local MySQL.
2. Run the updated isolated infrastructure verification on a host with Docker
   Compose v2. The script currently stops at preflight.
3. Render and validate base/test/prod Kustomize overlays on a host with
   `kubectl`. The current script stops at preflight.
4. Regenerate a new Review Package after those checks pass. Do not overwrite
   this V7 package.

## Non-blocking Deployment Inputs

- Select and govern actual environment issuer, audience, and optional explicit
  JWK Set URI before runtime rollout.
- Supply actual HUMAN/SERVICE mappings and direct capability grants only after
  their owning use cases are authorized, using the controlled manifest path.
- Monitor the JDK warning that Mockito dynamic self-attachment will be disabled
  in a future JDK; the current Java 21 project target does not require a build
  change.

## Known Unrelated Repository Condition

`scripts/verify-static.sh` scans all untracked files and is blocked by trailing
whitespace in the pre-existing untracked file
`review/q-006-design/Q-009-V6-Approved-Design-Git-Baseline-Prompt.md`.
The Q-009 authorization forbids modifying that historical prompt. Tracked and
new Q-009 implementation files pass scoped whitespace checks.
