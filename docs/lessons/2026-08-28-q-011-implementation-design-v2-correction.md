# Q-011 Implementation Design V2 Correction Lessons Learned

## What Happened

Codex received the Implementation Authorization Prompt built from
Implementation Design V1, read AGENTS.md and all four governing documents
exactly as instructed, and halted before writing any code — reporting six
precise, line-cited contradictions instead of guessing how to resolve them.
Every one of the six turned out to be real: a cross-table `CHECK`
constraint MySQL cannot express, two idempotent-replay ordering bugs (in
both recording and correction) that would have broken retries of already-
successful requests, a self-contradictory and actually-unused enum value,
one mislabeled transaction, and one arithmetic slip in a summary table.

## Reusable Lesson: Self-Review Catches Different Things Than Attempted Implementation

This session's Requirement-stage self-review (§18 of the Requirement) and
Architecture-stage self-review both looked hard for defects and found real
ones. Neither found any of these six. That is not a failure of rigor so
much as a difference in method: reading a design for internal consistency
tests different failure modes than actually working through exact
execution order, database constraint scope, and what happens on a retry.
The two idempotency-ordering bugs in particular are the kind of thing that
is nearly invisible when reading a design top to bottom, and immediately
obvious when asking "what happens if this exact request arrives twice."

Conclusion: an implementer that reads the design, tries to build it
literally, and stops to report contradictions instead of silently working
around them is a genuinely independent check, even when — as here — the
same party (Claude Code, in the Architect role) will go on to fix and
re-approve the design. The value came from Codex refusing to improvise.

## Reusable Lesson: Idempotent Replay Must Be Checked First, Before Anything State-Dependent

Generalize beyond Q-011: for any mutation with idempotency-key replay
semantics, the replay check belongs before content validation, before any
call to another module/dependency, and before loading or checking the
target's current state — never after. Otherwise a request that succeeded
once can fail on retry for reasons that have nothing to do with whether it
originally succeeded (the target's state moved on, an external dependency
became unavailable, business conditions changed). This is a pattern worth
checking for explicitly in any future Requirement with a mutation +
idempotency design, not just re-deriving from scratch each time.

## Not Yet Resolved

Product Owner approval of Implementation Design V2 is outstanding.
Implementation remains not authorized. No code exists.
