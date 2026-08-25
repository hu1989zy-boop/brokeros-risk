# Q-008 Architect Approval and Prerequisite Analysis Outstanding Items

## Blocking Q-008 Implementation Authorization

1. **Trusted Actor and Authorization Foundation**
   - No approved or implemented authentication/authorization capability exists.
   - Q-009 is recommended as the next cohesive Requirement.
   - It must select a real identity authority, fail-closed runtime boundary,
     trusted ActorContext, authorization decision contract, and verification.
2. **Authoritative Trading Account subject provider**
   - Q-008 requires a typed `TRADING_ACCOUNT` subject, but the repository has
     no authoritative broker-neutral account-reference provider.
3. **Authoritative Evidence and Decision providers**
   - Q-007 supplies semantics only; the owning Decision capability is not
     implemented.
4. **Authoritative Action provider**
   - Action intent ownership is defined, but no runtime provider exists.
5. **Authoritative ActionOutcome provider**
   - Execution/outcome is separately owned and not implemented. Q-008 must not
     build Account Control or a vendor execution engine to fill the gap.
6. **Explicit final implementation authorization**
   - After every prerequisite is approved, implemented, wired, and verified,
     the external Architect must separately set Q-008 Implementation
     Authorization to YES and provide the executable Implementation Prompt.

## Not Outstanding

- Q-008 Requirement and Architecture approval;
- ADR-010 acceptance;
- Implementation Design V4 approval;
- CaseNumber strategy;
- immutable Resolution History;
- aggregate, lifecycle, persistence, concurrency, API, and audit design;
- ADR-009/ADR-010 compatibility.

No Implementation Design V5 is required.

## Q-009 Recommendation Boundary

Recommended Q-009 title/scope direction:

```text
Q-009 — Trusted Actor and Authorization Foundation
```

It should be cohesive and cross-cutting, not an omnibus Evidence/Decision/
Action/Account implementation. The Product Owner/Architect must authorize
drafting before a Q-009 file is created.

The authoritative reference providers require formal owning-capability
Requirements and sequencing. This Review intentionally does not invent their
Requirement IDs.

## Deferred Until Explicit Decision

- Q-009 Requirement drafting;
- all Q-009 architecture, ADR, design, and implementation;
- owning-provider Requirement IDs and sequencing;
- Q-008 implementation authorization;
- any Risk Case source, tests, migration, API, configuration, or dependency.

## Recommended Next Work

The following prompt is ready for use only after the Architect/Product Owner
authorizes drafting Q-009 and prerequisite sequencing.

====================================
Codex Prompt
====================================

Start Requirement Discovery only for a new BrokerOS Risk prerequisite:
Q-009 — Trusted Actor and Authorization Foundation.

Authority and scope:

1. Read AGENTS.md, Q-003/Phase 0.6 security standards, Q-005/ADR-007
   correlation boundaries, Q-007/ADR-009, Q-008/ADR-010, the approved Q-008
   Implementation Design V4, and the latest Q-008 Architect Approval and
   Prerequisite Analysis Review.
2. Create only a Draft Q-009 Requirement and its Requirement Review package.
   Status must remain `Draft — awaiting architect approval`.
3. Define a broker-neutral trusted authentication boundary, non-spoofable
   ActorContext, principal-to-ActorRef mapping, capability authorization,
   default-deny behavior, failure contracts, audit attribution, sensitive-data
   handling, runtime wiring expectations, tests, and ADR/dependency evaluation.
4. Identify the real identity authority/integration gap; do not invent a fake
   authentication provider, caller header, hard-coded actor, role hierarchy,
   organization directory, or full IAM administration.
5. Record—but do not implement—the separate owning-capability sequence needed
   for authoritative Trading Account, Evidence, Decision, Action, and
   ActionOutcome reference providers. Do not assign new Q-numbers without an
   explicit Product Owner/Architect decision.
6. Do not modify Q-008 V4 Design, create Design V5, authorize Q-008
   implementation, or create Risk Case/domain/provider/security implementation.
7. Do not add Spring Security or any dependency before Requirement and ADR
   approval.
8. Preserve all Q-007/Q-008 Reviews, ZIPs, and review-history. Create a new
   independent Q-009 Requirement Review and timestamped ZIP.
9. Run documentation, architecture, scope, security, whitespace, secret, ZIP,
   and Git checks. Record backend runtime tests as baseline only if executed.
10. Do not commit, push, stage, reset, or clean.
11. Report the Draft Requirement path, open decisions, ADR need, Review path,
    ZIP path/manifest, Git status, and confirmation that implementation remains
    unauthorized, then stop.
