# Q-010 V1 Outstanding Items

## Blocking Requirement Approval Decisions

1. Reconcile or explicitly classify the Q-008 Implementation Design's retained
   pre-approval status header/Section 17 against the later external approval
   recorded by the active Q-008 Requirement and approval Review. Do not change
   substantive Q-008 design.
2. Confirm or revise the recommended Q-010 title and boundary.
3. Decide the real initial registration/import authority. If no trustworthy
   source or controlled operator process exists, implementation must remain
   blocked.
4. Decide source namespace and broker/platform/server/environment identity.
5. Decide whether explicit broker/tenant identity is required in Q-010.
6. Decide external mapping cardinality, reassignment, and lifecycle/history
   semantics.
7. Decide the minimum safe consumer query and disclosure boundary.
8. Perform formal ADR determination; current analysis expects ADR Required YES.

## Non-Blocking Governance Maintenance Outside Q-010

- Consider updating the root Review index, which currently stops at Q-007, to
  reference Q-008 and Q-009 dedicated closures.

These are documentation maintenance items. They do not authorize changing
Q-008 design or implementation and do not convert Q-010 to APPROVED.

## Not Started

- Q-010 Architecture or ADR;
- Implementation Design or implementation;
- Java, test, Flyway, API, configuration, dependency, infrastructure, Kafka,
  Redis, or adapter work;
- Q-008 implementation/authorization; and
- Git staging, commit, or push.

The next authorized activity is Architect review of the proposed Requirement
using `NextStepPrompt.md`.
