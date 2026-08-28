# Q-010 Recommendation Decision

## Recommended Requirement

**Q-010 — Trading Account Reference Authority Foundation**

## Why It Is Preferred

1. It follows the approved upstream-to-downstream dependency direction.
2. It removes one explicit Q-008 blocker without pretending all blockers are
   closed.
3. It is independently testable: scoped identity, uniqueness, lifecycle,
   authorization, provenance, and fail-closed queries have clear outcomes.
4. It reuses Q-009 instead of inventing identity/authorization again.
5. It avoids Rule Engine, Account Control, vendor SDK, Kafka, Redis, streaming,
   and full master-data scope.
6. A stable account reference supports later multi-broker mappings while
   keeping raw vendor identifiers out of Core Domain/Risk Case contracts.

## Why the Other Candidates Wait

- **Evidence Foundation waits** until account/source identity and a concrete
  initial evidence source can be defined without a generic evidence bucket.
- **Decision Foundation waits** because ADR-009 requires every Decision to be
  attributable to authoritative Evidence.
- **Audit Foundation waits** until concrete initial mutation consumers and
  access/retention semantics justify a shared capability rather than a
  speculative universal log.

## Important Boundary

Q-010 is a proposal, not approval. Even after Q-010 is eventually implemented,
Q-008 still lacks Evidence, Decision, Action, and ActionOutcome authorities and
requires a separate explicit implementation-authorization decision.
