# Q-010 V5 Lessons Learned Reference

Design-phase Lessons Learned are recorded at:

`docs/lessons/2026-08-27-q-010-trading-account-reference-authority-implementation-design.md`

The lesson captures:

- reuse of actual Q-009 contracts and the minimal service-descriptor
  composition change;
- durable semantic idempotency rather than a request key alone;
- a high-level mutation port that makes atomic history difficult to omit;
- exact external-key byte preservation;
- controlled non-Web provisioning rather than a public admin API;
- fail-closed, non-enumerating Q-008 eligibility disclosure; and
- why a reusable Skill is deferred until implementation/MySQL evidence exists.

It explicitly records that no proposed implementation/runtime behavior has
been verified and that no Java, SQL, migration, API, configuration, Q-008
implementation, staging, commit, or push occurred.
