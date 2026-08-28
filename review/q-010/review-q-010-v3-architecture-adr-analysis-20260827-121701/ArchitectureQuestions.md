# Q-010 Required Architecture Review Questions

1. Q-010 Trading Account Reference Authority owns TradingAccountRef.
2. A raw MT4/MT5 login lacks scope, server/environment, provenance, lifecycle,
   immutability, and BrokerOS identity.
3. The complete scope + four-part namespace + external-key tuple prevents
   server/environment collision.
4. Immutable no-reassignment/no-delete rules prevent external number reuse
   from rewriting history.
5. Complete-tuple uniqueness and one local transaction prevent two refs for one
   tuple under concurrency.
6. TradingAccountRef uniqueness, server-only generation, and immutable identity
   prevent two tuples for one ref.
7. The complete external identity tuple is immutable after registration.
8. ACTIVE, INACTIVE, and RETIRED remain historically resolvable.
9. Only ACTIVE account inside ACTIVE scope is eligible for a new Q-008 case.
10. Q-008 consumes protected `validateForNewRiskCaseAssociation` and receives
    recognized/eligibility plus bounded authority evidence only.
11. Q-008 cannot see ExternalAccountKey: **No**.
12. A purpose-specific registered SERVICE descriptor obtains a fresh context
    through Q-009 ServiceActorContextFactory and active mapping.
13. Register authorization proves permission to invoke, not mapping truth.
14. Mapping truth is backed by the deployment-approved broker/source-owner
    record identified by bounded attestation source/ref.
15. Same operation ID/fingerprint returns the durable recorded result without
    another mutation/version/history entry.
16. Changed fingerprint/provenance/ref conflicts and selects no winner.
17. State, operation outcome, and immutable history share one MySQL transaction.
18. History persistence failure rolls back the complete mutation.
19. MySQL unavailability returns dependency unavailable with no stale fallback.
20. Redis/Kafka are not required because they cannot enforce the initial
    atomic relational authority and would add consistency failure modes.
21. No new dependency or framework is required.
22. Exact Java/DDL/manifest/CLI/transaction/error/query/test mechanics are
    deferred to Implementation Design.
23. Aliases/migration/adapters/automation/online admin/master data/cache/events/
    retention/federation require future Requirements.
24. After implementation, Q-010 can satisfy only Q-008 Trading Account subject
    authority; Evidence, Decision, Action, ActionOutcome and later explicit
    Q-008 authorization remain.
25. Implementation is not authorized; external Architecture/ADR review, later
    Design approval, and explicit implementation authorization are required.
