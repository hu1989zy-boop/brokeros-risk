# Q-006 Skill Analysis

## Current Decision

Do not create or modify a skill during Design Only.

The existing development-standards, engineering-foundation, CI verification,
and observability skills were inspected and remain applicable. They do not yet
provide one focused configuration-management workflow.

## Proposed Future Skill

After implementation and verification, evaluate creating:

`docs/skills/configuration-management.md`

It should contain reusable knowledge, not a Q-006 changelog:

- when a setting should use native Spring properties versus an application-
  owned group;
- `brokeros.risk.<capability>` naming and environment alias mapping;
- typed `@ConfigurationProperties` ownership and registration;
- immutable binding and semantic Java types;
- Jakarta validation and safe startup-failure patterns;
- profile, precedence, default, and compatibility rules;
- secret classification, value redaction, `.env`, ConfigMap, and Secret rules;
- binding/validation/profile/security test patterns;
- common mistakes such as scattered `@Value`, duplicate wrappers, empty groups,
  unsafe defaults, secret-bearing `toString`, and exposed Actuator endpoints;
- completion checklist and Review evidence.

## Creation Gate

Create the skill only after actual implementation reveals verified patterns and
mistakes. If Q-006 produces documentation/tests but no production properties
class, the skill must say that no empty type is the correct YAGNI outcome. It
must not invent code examples or incidents that did not occur.
