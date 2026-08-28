# Q-010 V7 Security Review

## Trust and authorization

- The module reuses Q-009 ActorContext, AuthorizationGuard, AuthorizationDecision,
  exact Capability values, and ServiceActorContextFactory.
- The deployment command descriptor code is
  `trading-account-reference-provisioner`; Spring registers the exact singleton
  instance in the Q-009 object-identity registry.
- Reads require `trading-account-reference:read`; registration requires
  `trading-account-reference:register`; lifecycle requires
  `trading-account-reference:change-lifecycle`.
- Authorization precedes every authority query/mutation port. Unauthorized
  eligibility tests perform zero Q-010 lookups; the real command performs zero
  mutation after grant revocation.

## Input and disclosure controls

The non-Web command rejects extra arguments, symlinks, non-files, unreadable or
oversized input, unknown fields, duplicate JSON keys, trailing tokens,
unsupported schema/operation, invalid typed values, and invalid operation field
matrices. It accepts no actor, timestamp, credential, token, generated ref,
vendor/customer payload, or generic metadata.

ExternalAccountKey has a redacted `toString`; metrics have only operation and
outcome tags. Command output/errors contain bounded result fields/codes and no
external key, namespace, attestation ref, reason, actor, fingerprint, manifest,
SQL, or stack trace. Q-008 receives no raw lifecycle/scope/version/identity.

## Static boundary

Architecture tests inspect domain/application source for Spring, servlet,
JDBC, Jackson, Kafka, Redis, and vendor imports and scan the entire module for
REST controllers, actor headers, role/admin bypass vocabulary, or delete ports.
No prohibited dependency or bypass was found.

Security review result: **PASS FOR ARCHITECT SUBMISSION**.
