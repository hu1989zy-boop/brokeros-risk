# Q-010 V8 Security Review

## Final checklist

- [x] Reuses Q-009 ActorContext, AuthorizationGuard, decision evidence, and service registry.
- [x] Adds no parallel authorization model, role grant, wildcard, SYSTEM, or caller ActorRef.
- [x] Authorization occurs before every sensitive Q-010 lookup/mutation.
- [x] Denied/revoked callers perform zero Q-010 access/mutation and cannot enumerate existence.
- [x] The Q-008 facade returns only ref, bounded decision, and opaque evidence.
- [x] No public/admin provisioning REST endpoint, scheduler, watcher, or batch exists.
- [x] Manifest handling rejects symlink/nonfile/oversize/unknown/duplicate/trailing/invalid input.
- [x] External key, namespace, attestation ref, reason, actor, fingerprint, SQL, and manifest are absent from normal output/errors/metrics.
- [x] ExternalAccountKey renders redacted and exact bytes remain confined to authority persistence.
- [x] No credentials, tokens, private keys, production data, runtime manifest, or `.env` content is included in V8.

The real non-Web command test proved active Q-009 mapping/grants, successful
scope/account provisioning, exact replay, grant revocation denial, and zero new
mutation after denial. Java 21 architecture/security tests passed. Security and
non-enumeration result: **PASS**.
