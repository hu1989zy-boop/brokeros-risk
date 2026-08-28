# Q-010 V7 Lessons Learned

The reusable implementation lessons are recorded at:

`docs/lessons/2026-08-27-q-010-trading-account-reference-authority-implementation.md`

Key findings were to generate candidate refs only after replay/compatible-state
checks, use real MySQL as the final proof for binary/composite identity and
race behavior, re-read durable operation outcomes after losing transactions,
exercise the actual Q-009 service descriptor/mapping/grants in the command
test, and advance infrastructure verification with each additive Flyway
baseline.

The reusable pattern is captured at:

`docs/skills/trading-account-reference-authority.md`
