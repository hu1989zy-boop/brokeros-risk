# Security review — Q-015 portable consumer

The approved trust transition is parent Q-015 §17 / Architecture V2 §3:
Kafka SASL/mTLS and topic ACLs replace the HTTP actor check for canonical ingestion.
No untrusted actor is constructed, no capability is added and Q-009 remains unchanged.
The opt-in HTTP test aid retains its SERVICE/capability authorization.

Inspected CanonicalTradingDataMessageReader, Kafka listener, quarantine publisher,
Kafka configuration, domain projection, shared service transaction, JDBC adapter,
HTTP controller, fixtures and changed docs/tests.

- Input must be UTF-8 JSON with a bounded size/depth/token length. V1 versions,
  enum/string types, UTC times, metadata and key alignment are checked. Duplicate
  properties and trailing JSON are rejected. No default typing, polymorphic class
  metadata, reflective vendor loading or external payload resolution exists.
- Parser exceptions are replaced with the existing stable request-invalid code
  and no cause. Listener errors are reduced to fixed partition/offset context.
  SQL/producer messages, arbitrary keys, payload values and caller headers are
  not copied to logs. The default Kafka producer error listener is suppressed.
- The quarantine intentionally retains untrusted raw data to avoid silent loss.
  It therefore needs at least the input topic's access/retention protection and
  restricted operator reads. Only safe broker coordinates/version/code headers
  are added; no arbitrary incoming headers or exception stack is forwarded.
- The consumer never writes external databases or issues trading commands. Local
  SQL remains parameterized in the unchanged JDBC adapter. No secrets or account
  holder identity are added to fixtures. The supplied de-identified fixture was
  not rewritten; test identifiers and injected failures are synthetic.
- Factory tests prove native SASL/TLS properties are retained, not that deployed
  ACLs/certificates are correct. Production topic provisioning, ACLs, mTLS/SASL,
  replication/min-ISR, retention, producer permissions and failure alerting still
  require deployment evidence. Source claims are trusted only after the broker
  authenticates/authorizes the gateway producer.
- A listener failure stops ingestion until an operator repairs the cause and
  restarts with the same group. Failure counters/logs and group lag expose this;
  the existing HTTP health endpoint does not report listener state. No secret
  configuration endpoint was added.

Verification evidence and package credential-scan outcome are in Verification.md.
This review is the implementation-side assessment; independent review remains next.
