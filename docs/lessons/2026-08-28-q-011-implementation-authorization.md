# Q-011 Implementation Authorization Lessons Learned

## What Was Decided

The Product Owner granted explicit implementation authorization for Q-011
on 2026-08-28, as a decision separate from and after Implementation Design
V1 approval, preserving the same gate separation Q-009 and Q-010 both
required. Claude Code then issued a complete Codex Prompt bounded by the
approved Requirement V2, Architecture V1, ADR-013, and Implementation
Design V1.

## Reusable Lesson

Even within a single fast-moving session where the same two parties (Claude
Code drafting/reviewing, Product Owner approving) move through every gate
in sequence, keep "Design is approved" and "implementation may begin" as
two separate recorded decisions. Collapsing them because the same session
already has momentum is exactly the shortcut this repository's governance
model exists to prevent — Design approval says the plan is sound; Implementation
Authorization says now is the deliberate moment to spend engineering effort
executing it, including confirming deployment inputs (capability grants)
are actually ready.

## Not Yet Resolved

Codex has not yet executed the Prompt. No Java, migration, endpoint,
dependency, commit, or push exists yet. The next independent action is
Claude Code's review of Codex's resulting non-overwriting review package,
not self-approval of Codex's output either.
