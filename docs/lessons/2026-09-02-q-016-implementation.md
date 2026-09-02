# Q-016 Implementation Lessons Learned

## Scope

Q-016 established the web-first Flutter Risk Console, a local Keycloak
development profile, and one additive authorized Risk Case summary endpoint.
The UI proves the intended login → bounded list → detail/history/association
view → add-investigation-note flow without changing Q-008 aggregate rules or
any migration.

## What worked

- Separating the typed API contract, repository, Riverpod notifiers, and widgets
  kept transport and failure mapping out of presentation code.
- Reusing Q-009's verified JWT → ActorContext → capability boundary allowed the
  new list endpoint and existing note command to stay actor-free at every UI
  request boundary.
- Fetching `size + 1` summary rows, applying a hard maximum of 100, and sorting
  by `updated_at DESC, id DESC` provided bounded deterministic pagination with
  no count query or unauthorized migration.
- A one-shot security bootstrap manifest granted the seeded local Keycloak user
  only `risk-case:read` and `risk-case:note`; passwords remain in the ignored
  `.env`, including the development operator password.
- Real MySQL tests covered filtering, projection, ordering, the page cap, and
  the next-page result rather than proving SQL only by inspection.

## Problems encountered

- The installed `openid_client` browser convenience `Authenticator` still
  constructs an implicit flow. Its friendly class name was not evidence of the
  approved security protocol. The implementation instead uses
  `Flow.authorizationCodeWithPKCE`, stores only one-time state/verifier values
  in browser session storage, and processes the query callback explicitly.
- Flutter and Dart were not installed in the execution environment. The source,
  generated-code configuration, contract/unit/widget tests, and run commands
  were delivered, but code generation, analyzer, frontend tests, web build, and
  browser end-to-end verification could not truthfully be executed.
- The repository-wide configuration test exposed deployment aliases already
  present in Compose but absent from the authoritative catalog, then also
  required the new Q-016 Keycloak/CORS aliases. Updating the catalog restored
  the cross-repository contract instead of weakening the test.
- The disposable MySQL application user could not create failure-injection
  triggers while binary logging was enabled. As in Q-008, enabling
  `log_bin_trust_function_creators` only inside the disposable test container
  allowed the full regression gate without expanding product privileges.

## Audit decision

Existing detail and history reads emit an audit record tied to one Risk Case.
The new list returns a bounded multi-case summary and the audit schema requires
one target case; emitting one synthetic case target or many unapproved list
records would change audit semantics. Q-016 therefore authorizes the list with
`risk-case:read` but does not invent a list-read audit. Individual disclosure
auditing remains unchanged.

## Reusable result

The reusable browser-auth, thin-client, bounded-query, state-management, and
verification rules are captured in
`docs/skills/flutter-risk-console-development.md`. The key rule is to inspect
the actual OIDC flow constructed by a dependency rather than trusting a browser
helper's name or README-level claim.
