# Q-006 Proposed Implementation Plan

## Status

PLAN ONLY — DO NOT EXECUTE BEFORE ARCHITECT APPROVAL AND ACCEPTED ADR-008

## Task 1 — ConfigurationProperties Strategy

1. Draft and obtain acceptance of ADR-008.
2. Finalize the ownership taxonomy and reserve
   `brokeros.risk.<capability>` for application-owned keys.
3. Inventory every existing canonical key and deployment alias.
4. Keep Spring Boot framework properties in native namespaces; do not create
   datasource, Redis, Kafka, Flyway, server, logging, management, or SpringDoc
   wrapper classes.
5. Confirm whether any concrete application-owned setting exists inside the
   approved Q-006 boundary. If none exists, create no production
   `@ConfigurationProperties` type and record that outcome explicitly.
6. If a concrete group is separately approved, implement one cohesive immutable
   typed properties object in its owning capability with explicit registration.

Deliverable: accepted strategy plus the smallest justified property model, which
may intentionally contain no new production type.

## Task 2 — Validation

1. Define requiredness, type, unit, range, safe default, sensitivity, and
   profile behavior for every supported configuration item.
2. Reuse Spring Boot native conversion/validation for framework properties.
3. Apply `@Validated` and Jakarta constraints only to real BrokerOS-owned
   properties.
4. Add cross-field validation only for a documented invariant that standard
   constraints cannot express.
5. Ensure validation is startup-only, side-effect free, and does not reveal
   secret values.
6. Preserve `env` and `configprops` as unexposed Actuator endpoints.

Deliverable: explicit fail-fast and safe-diagnostic rules without external I/O.

## Task 3 — Integration Tests

1. Test base/test/prod configuration layering and supported override sources.
2. Test documented defaults and explicit environment/test-property overrides.
3. Test missing required production values and invalid type/range/duration
   values fail startup safely.
4. Test nested validation only if a real nested properties group is approved.
5. Assert supplied secret values do not appear in captured diagnostics.
6. Assert Actuator `env` and `configprops` remain unexposed.
7. Keep tests isolated from MySQL, Redis, Kafka, Docker, and Kubernetes when
   testing only binding/validation.
8. Retain all existing 19 tests and final infrastructure gates.

Deliverable: focused configuration regression evidence using existing Spring
Boot test dependencies.

## Task 4 — Documentation

1. Create a central configuration catalog with owner, canonical key, alias,
   type/unit, default, profile, requiredness, sensitivity, validation, source,
   restart, and compatibility columns.
2. Document Spring Boot source precedence relevant to the supported run modes.
3. Document local `.env`, CI ephemeral values, Kubernetes ConfigMap, and
   externally managed Secret conventions.
4. Update root/backend/deployment READMEs only where they currently duplicate or
   omit the approved contract.
5. Add `docs/skills/configuration-management.md` only after implementation
   proves reusable guidance.
6. Add an honest Q-006 Lessons Learned entry from actual work; do not copy this
   design's anticipated lessons as facts.

Deliverable: one authoritative, value-free configuration reference and updated
reusable guidance.

## Task 5 — Review and Closure

1. Inspect the final change against Q-006, ADR-008, AGENTS, architecture,
   accepted ADRs, skills, and Lessons Learned.
2. Run `mvn test`, `mvn package`, `git diff --check`, static verification,
   Kustomize rendering, and isolated Docker/infrastructure verification.
3. Use the approved CI path for any unavailable local Docker/Kubernetes gate;
   do not call unavailable checks PASS.
4. Verify no business module/table/topic/key/event, secret, new deployment,
   dynamic configuration service, or prohibited technology was introduced.
5. Regenerate the mandatory root Review Package with all eight evidence-backed
   compliance sections.
6. Obtain architect approval before another Requirement begins.

Deliverable: Q-006 final Review Package with honest PASS/PARTIAL evidence.

## Sequencing

```text
Architect approves Requirement/design
→ ADR-008 drafted and accepted
→ Task 1
→ Task 2
→ Task 3
→ Task 4
→ Task 5
→ Architect final review
```

No task in this plan is authorized by the Design Only package itself.
