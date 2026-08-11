# ADR-002: System Isolation

- Status: Accepted
- Date: 2026-08-11

## Context

BrokerOS Risk must integrate with brokers, CRMs, and trading platforms without
becoming coupled to a particular vendor or taking ownership of another
system's data. MT4 and MT5 integrations also depend on real vendor SDKs that are
not currently available in this repository.

## Decision

- Keep external-system integrations behind adapters.
- Keep MT4 and MT5 implementation details in their respective adapters.
- Do not expose vendor SDK types to the core domain.
- Never directly modify another system's database.
- Do not invent Manager API interfaces or claim unsupported operations.
- Keep risk detection separate from risk action execution.
- Make critical decisions and action attempts auditable.
- Represent broker-specific policies as configuration rather than hard-coded
  branches in domain logic.

The Phase 0 MT4 and MT5 adapters remain placeholders until the real SDKs and
approved integration requirements are available.

## Consequences

- Core business logic remains portable across brokers, CRMs, and trading
  platforms.
- Integrations require explicit contracts, mapping, failure handling, and
  verification against supported APIs or SDKs.
- External schemas cannot become the internal domain model.
- Risk action execution can be controlled, audited, retried, or disabled
  independently from detection.
