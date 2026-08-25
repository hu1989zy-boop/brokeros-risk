# Q-009 Security Analysis

## Security Objective

Only a server-established, active BrokerOS actor may enter protected application
behavior, and each protected use case must receive an explicit capability allow
decision before protected access or mutation.

## Trust Boundaries

1. Credentials and platform attestations are untrusted until validated by the
   applicable authentication adapter.
2. A validated external principal is not a BrokerOS actor until mapping
   succeeds uniquely to an active ActorRef.
3. A mapped actor is not authorized until the use case obtains an explicit
   capability allow decision.
4. Framework security objects are infrastructure data and do not cross into
   application/domain contracts.
5. Caller actor fields, correlation data, and raw claims are never trust roots.

## Human Authentication

The architecture delegates human credential authentication to a trusted
external authority and treats BrokerOS as a protected API. The concrete
provider and JWT/opaque validation decision remain open. Any future adapter
must validate issuer/authority, intended audience, signature or introspection
result, validity interval, and freshness/revocation policy.

## Service Authentication

Service principals are explicitly typed and purpose-specific. Externally
invoked services require a validated service credential/attestation. Internal
jobs use an infrastructure-controlled bootstrap that resolves a provisioned,
active service mapping and creates a fresh context per operation. There is no
generic SYSTEM actor, universal service permission, or caller-accessible
bootstrap.

## Authorization Controls

- default deny and explicit allow;
- stable capability names rather than provider roles;
- explicit external-attribute-to-capability mapping;
- authoritative use-case-boundary enforcement;
- authorization before protected resource lookup/mutation;
- safe reason categories without policy or resource disclosure;
- policy provenance/version and UTC decision timestamp;
- fail closed on missing, inactive, ambiguous, unavailable, or indeterminate
  evidence.

## Context Controls

ActorContext is immutable, bounded, per execution, and cleared at completion.
It excludes raw credentials, full claims, framework objects, and a mutable
permission cache. Background work creates a fresh context. Cross-thread and
cross-process propagation is not approved under Q-009.

## Audit Controls

ActorRef and authentication/authorization provenance are derived only from the
trusted context. Caller-provided actor data cannot override audit identity.
Credentials and full claims are never logged or persisted. Q-009 supplies
trusted audit inputs without inventing the Audit module.

## Threat and Mitigation Matrix

| Threat | Mitigation | Residual/open issue |
| --- | --- | --- |
| Spoofed actor header/body/query | Ignore caller identity; derive ActorRef through verified mapping | Future API tests must prove spoof resistance |
| Forged credential | Provider/cryptographic validation and strict issuer/audience/validity | Exact provider/token model open |
| Stale/revoked credential | Explicit freshness/revocation strategy | Trade-off depends on JWT vs introspection |
| Privilege escalation | Least privilege, default deny, governed grants | Policy administration design open |
| External role confusion | Roles/claims only mapped inputs, never direct capability | Mapping lifecycle design open |
| Service impersonation | Separate principal type and purpose-specific credential/ActorRef | Concrete service mechanism open |
| SYSTEM bypass | No generic SYSTEM; normal capability evaluation for jobs | Bootstrap design/testing required later |
| Controller bypass | Authoritative application-use-case check | All protected use cases must be inventoried |
| Thread/context leak | Fresh immutable context, explicit pass, cleanup, no raw propagation | Async model deferred |
| Audit actor spoofing | ActorRef exclusively from trusted context | Audit persistence belongs to later scope |
| Provider/policy outage | Fail closed with safe unavailable outcome | Availability objectives open |
| Resource existence leak | Authorize before lookup; safe denial | Exact result codes open |

## Operational Surface

Only minimal non-sensitive liveness/readiness may be unauthenticated in
production. Sensitive Actuator and OpenAPI/Swagger surfaces must be disabled or
protected. The concrete route matrix is deferred to Implementation Design.

## Security Review Result

PASS for Architecture Review readiness. The proposal identifies the required
trust boundaries and threats without claiming that controls are implemented.
Implementation remains prohibited.
