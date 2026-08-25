# Q-009 Trusted Actor and Authorization Architecture — Lessons Learned

## Date and Scope

- Date: 2026-08-26
- Scope: Q-009 Architecture Analysis and proposed ADR only
- Implementation performed: No

## What the Baseline Revealed

The existing request/trace correlation foundation is intentionally not an
identity foundation. Request IDs, trace IDs, MDC data, servlet requests, and
caller fields can help correlate an execution but cannot establish who is
trusted or what that actor may do. Keeping these semantics separate prevents an
observability feature from becoming an accidental authentication mechanism.

The current backend also has no security framework dependency or trusted actor
contract. The correct architecture therefore starts at the boundary and trust
model rather than assuming that a framework principal or token claims can be
passed directly into the domain.

## Reusable Lessons

### Authenticate, map, then authorize

Successful external authentication proves an authority/subject assertion. It
does not prove that the principal is a BrokerOS actor, and mapping an actor does
not grant a business capability. Treating authentication, actor mapping, and
authorization as separate fail-closed decisions makes ownership and testing
clear.

### Own stable application semantics, not credentials

A platform can delegate credential authentication while retaining a
provider-neutral ActorRef, mapping lifecycle, and capability policy. This avoids
both password-platform scope expansion and identity-provider coupling.

### Enforce at the use-case boundary

Controller or URL rules cannot protect internal schedulers and future adapters.
The application use-case boundary is the consistent authorization point across
transport mechanisms. Framework checks are valuable defense in depth but do
not replace the application decision.

### Background work needs an accountable actor

A generic `SYSTEM` identity creates an unauditable superuser. Background work
should resolve a purpose-specific active service actor, create a fresh context,
and request the same capability as any other caller.

### Keep the trusted context small

Raw tokens, full claims, framework objects, HTTP requests, and mutable sessions
increase leakage and coupling risk. A bounded immutable context should carry
only the stable actor, approved authentication provenance, and approved policy
inputs. Correlation remains a separate concern.

### Preserve open decisions honestly

Choosing JWT, opaque introspection, OIDC login, mTLS, or a named provider before
deployment and product constraints are known would turn an architecture
boundary into an unsupported implementation assumption. Recording the decision
criteria is more useful than inventing a provider.

## Verification Lesson

Security verification must prove negative paths: forged and stale credentials,
unmapped or inactive actors, insufficient capabilities, provider outages,
context leakage, caller actor spoofing, and background bypass. A happy-path
authentication test alone does not verify a trusted-actor foundation.

## Skill Evaluation

No repository skill was added or changed. This work produces a proposed,
unimplemented architecture; no implementation pattern has yet been exercised
and validated enough to become a reusable repository skill. The architecture
lessons are recorded here as required, and skill creation should be reassessed
after an approved implementation establishes verified patterns.
