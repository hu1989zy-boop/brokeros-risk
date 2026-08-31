# Q-011 Design V4 Blocker Resolution Lessons Learned

## What Happened

Codex halted a third time, this time filing a formal written blocker
report rather than only a chat message, against a Design (V3) that had
already been approved and authorized following a full end-to-end
consistency audit. Every finding was again real. One — a direct
contradiction between the approved Requirement's subject-recognition bar
and Architecture/Design's stricter eligibility bar — was not something a
reviewer should silently resolve; it required an explicit Product Owner
decision, since it changes what the Foundation actually permits.

## Reusable Lesson: A Completeness Claim Needs a Mechanical Check

The prior round's own lesson said "grep, not memory." The §8.5
constraint-to-test table from that round still wasn't actually exhaustive
— it read as complete without having been checked column-by-column against
the DDL sections it claimed to cover. The fix this round was to walk every
column of every table in source order and produce one row per constraint,
rather than transcribing what the table already seemed to say. A sentence
asserting completeness is not evidence of completeness.

## Reusable Lesson: Separate "This Is a Bug" from "This Is a Decision"

Four of the five defects this round were review-fixable: a wording
overclaim, a stale status line, an incomplete table, an ambiguous column
description. The fifth — the Requirement/Architecture subject-bar
contradiction — was a scope decision disguised as a consistency defect,
and treating it the same way (just pick the version that reads better and
fix the other) would itself have been a defect: silently narrowing an
approved Requirement. The distinguishing question is whether the fix
changes what the system is allowed to do, not just what the documents say
about what it's allowed to do. Only the Product Owner can move that line.

## Not Yet Resolved

Product Owner approval of Implementation Design V4 and Architecture V3 is
outstanding. No implementation authorization exists for V4. No Codex
Prompt has been issued this round. No code exists — three consecutive
halts, zero lines written.
