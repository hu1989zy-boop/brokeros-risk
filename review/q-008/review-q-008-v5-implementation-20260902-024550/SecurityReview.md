# Q-008 Security Review

## Result

**PASS WITH CONDITIONS** for independent implementation review.

## Trusted actor and authorization

- Controllers obtain `ActorContext` from the existing Q-009 trusted resolver;
  no request DTO or header field accepts actor, created-by, updated-by, or
  server time.
- Every service calls `AuthorizationGuard.requireAllowed` before loading a case
  or invoking a reference provider. The application test
  `authorizationDenialOccursBeforeAnyCaseLoadOrProviderCall` verifies the call
  order and absence of side effects.
- Capabilities are operation-specific (`create`, `read`, `assign`, `associate`,
  `review`, `priority`, `note`, `resolve`, `close`, `cancel`, `reopen`) without
  inventing users, roles, teams, queues, or organization policy.

## Reference and subject integrity

- New-case subject validation delegates to Q-010's authoritative eligibility
  service. `ELIGIBLE` is the only accepted result;
  `RECOGNIZED_NOT_ELIGIBLE`, `NOT_FOUND`, and provider unavailability map to
  their distinct fail-closed Q-008 contracts.
- Evidence, Decision, Action, and ActionOutcome refs are parsed into owning-
  module value objects and confirmed through the shipped Q-011 through Q-014
  query services. No unchecked reference-string fallback exists.
- Action association requires the Action's originating Decision to already be
  associated with the case. Outcome association requires the outcome's
  pertaining Action to match the already-associated Action. Both are verified
  before any root/history/audit write.
- No upstream payload, database entity, internal primary key, external account
  identity, or vendor type crosses the Q-008 port/API boundary.

## Sensitive content

- Investigation note content is stored only in the note table and returned
  only through authorized case access; note mutation responses expose metadata,
  not content.
- Audit before/after JSON contains bounded status/priority/cycle/assignment/
  Decision facts and hashes only. The Audit factory does not serialize note
  text, intake summary, credentials, authentication headers, or upstream
  payloads.
- Static scans found no password, secret, token, DELETE endpoint, external
  execution, Kafka, or Redis behavior in the Q-008/audit implementation.
- Errors use existing safe `ResultCode`/`GlobalExceptionHandler` behavior and
  do not expose stack traces or reference-provider internals.

## Read and mutation audit

- Detail and history access authorize and persist `RISK_CASE_VIEWED` or
  `RISK_CASE_HISTORY_VIEWED` before returning content. Test-only audit failure
  prevents disclosure.
- Material changes persist actor, reason, source, affected reference, UTC time,
  request/trace correlation, and bounded before/after facts in the same local
  transaction as the case change.
- No DELETE endpoint or repository delete operation exists. Notes and Evidence
  disposition changes append new facts and preserve the prior record.

## Explicit condition

The approved source set contains no active-actor-by-reference query for a
request-supplied assignee. Q-008 validates the assignee as a canonical Q-009
`ActorRef` and records the authenticated acting principal as assigner, but does
not claim to verify the assignee's present active state. Adding such a provider
or policy would require an approved Q-009/Q-008 contract change.

Detailed legal hold, retention duration, exceptional redaction, team/queue
authorization, and regulatory policy remain explicitly deferred by Q-008 and
were not invented.
