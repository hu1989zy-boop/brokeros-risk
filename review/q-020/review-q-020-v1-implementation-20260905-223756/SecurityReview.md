# Q-020 Security Review

## Authorization

Each list service calls `authorizationGuard.requireAllowed(actorContext, M.READ)`
before parsing or querying. The existing capabilities are reused:

- `evidence:read`
- `decision:read`
- `action:read`
- `action-outcome:read`

No capability definition, seed, deployment role, or client-side authorization
decision changed. Denials use the standard `AUTHORIZATION_DENIED` response and the
existing module authorization-denial metric.

## Content minimization

The four new JDBC statements explicitly select only:

- evidence: `evidence_ref`, `subject_ref`, `status`, `recorded_at`;
- decision: `decision_ref`, `subject_ref`, `recorded_at`;
- action: `action_ref`, `decision_ref`, `status`, `recorded_at`;
- outcome: `action_outcome_ref`, `action_ref`, `recorded_at`.

They never select `observation_text`, `conclusion_text`, `intent_text`, or
`outcome_text`. Summary and response records have no content fields. Real-MySQL
endpoint tests seed synthetic private values into the mandatory columns and assert
the corresponding JSON properties do not exist.

## Query and enumeration boundary

Queries are parameterized and bound to one natural scope, capped at 200, with no
free-text predicate or global/cross-module path. Valid unknown scopes return an
empty list only after authorization. Malformed scopes are parsed by established
domain value objects and mapped to module request-invalid codes.

## Console least authority

Evidence and decision lists use only the loaded case's subject. Action discovery
uses the current on-case decision when present, otherwise an associated decision.
Outcome discovery uses an action already present in the authoritative Q-019 case
projection. Hooks do not run without the correct scope key, and `ReferenceInput`
activates only the list matching its reference kind.

## Identity and transport

The new repository reuses `ApiClient`; identity remains exclusively in the Bearer
JWT. No actor, user, role, token, or authorization header is added to query strings
or request bodies. Association bodies remain the approved Q-018 shapes.

## Logging and audit

List services have no access-log dependency and never call
`recordFullDetailAccess`; real-MySQL tests confirm zero list-path access-log rows.
No new application log statement was added, so reference and content values are
not emitted by this implementation. Existing write-side audit/version behavior is
untouched.

## Test and package data

All test references, actors, timestamps, tokens, and record content are synthetic.
The review package stores no password or token. Database passwords in command
evidence are redacted.

## Result

All Q-020 security acceptance properties are satisfied in the inspected scope. No
unresolved security violation was found; independent security review remains part
of the next authorized review stage.
