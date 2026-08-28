# Q-010 Security and Threat Analysis

| Threat | Control |
| --- | --- |
| raw account/login spoofing | only complete attested tuple registers; consumers use BrokerOS ref |
| cross-server/environment collision | required exact instance/server/environment namespace |
| account-number reassignment | immutable no-reassignment/no-delete mapping |
| unauthorized registration | purpose-specific Q-009 SERVICE ActorContext plus register capability |
| confused-deputy adapter | future adapters resolve only; never auto-register or supply actor |
| forged provenance | separate deployment-approved source-owner record; capability is insufficient |
| replay abuse | unique operation ID plus semantic fingerprint |
| registration race | dual unique constraints and one transaction |
| existence probing | authorize before lookup; bounded denial |
| external-key leakage | no key/tuple in logs, errors, metrics labels, review evidence, or Q-008 result |
| manual DB corruption | fail integrity-closed; no arbitrary winner/automatic repair |
| stale cache/replica allow | neither cache nor replica authority selected |
| generic SYSTEM bypass | registered descriptor instance plus active SERVICE mapping/grant |
| state without history | history failure rolls back state |

## Q-009 Integration

- `trading-account-reference:read` protects both read contracts.
- `trading-account-reference:register` protects scope/account registration.
- `trading-account-reference:change-lifecycle` protects lifecycle transitions.
- ActorContext and ActorRef are server-derived; manifest actor fields are not
  accepted.
- Capability decisions occur before Q-010 data access and are not cached in
  ActorContext or Redis.
- Security/database unavailability never becomes an allow or stale success.

No credentials, token/claims, authorization headers, principal subject,
customer data, or vendor payload is introduced in the V3 artifacts.
