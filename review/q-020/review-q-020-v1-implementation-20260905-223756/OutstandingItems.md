# Q-020 Outstanding Items

## Blocking items

None found in the authorized implementation and implementation-verification scope.

## Required next lifecycle action

Claude Code's independent implementation review and the Product Owner's gate
decision remain pending. This package deliberately does not self-accept the
implementation or advance Q-020 to final closure.

## Design interpretation for independent review

The Implementation Design's Evidence example mentions controller `@Pattern` while
the same design requires malformed natural keys to return each module's existing
request-invalid `ResultCode` and says to reuse value objects rather than hand-roll
regexes. The repository's generic constraint-violation path returns
`VALIDATION_ERROR`. The implementation therefore authorizes first and validates via
the existing `TradingAccountRef`, `DecisionRef`, and `ActionRef` constructors in the
application list services, mapping failures to the owning module request-invalid
exception. Real-MySQL HTTP tests prove the required external behavior. Independent
review should confirm this resolution toward the higher behavioral contract.

## Non-blocking environment advisories

- Flyway's current version warns that its tested MySQL ceiling is 8.1 while the
  disposable test image is MySQL 8.4. V1-V8 clean/migrate and all 317 tests passed.
- Vite reports one minified bundle chunk over 500 kB. Q-020 introduces no new major
  dependency and the build passes; bundle optimization is outside this requirement.
- Mockito and Node emitted future/experimental runtime warnings described in
  `Verification.md`; neither affected results.

## Bounded UI choice

Where several on-case actions exist, V1 uses the first action in the authoritative
Q-019 projection as the outcome browse scope. The approved design permits an
on-case action and does not define an action chooser. Manual entry remains available,
so no cross-case/global expansion was introduced.

## Repository state note

Four sibling ZIP files for Q-016 through Q-019 were already untracked before Q-020
work began. They were preserved untouched and are not part of this package.

## Assumptions

- Existing natural-scope indexes are sufficient because real-MySQL verification
  confirmed all four; no migration was justified.
- The server silently caps at 200 with no pagination or `more` flag, as approved.
- Valid unknown natural keys intentionally reveal only an empty result after module
  authorization, as specified.
