# Kustomize Verification

- Gate: PASS
- kubectl: v1.36.3
- embedded Kustomize: v5.8.1
- official checksum verification: PASS

Command:

`sh scripts/verify-kustomize.sh`

Results:

- base render: PASS
- test overlay render: PASS
- prod overlay render: PASS
- Deployment/Service/ConfigMap/Secret references: PASS
- test/prod profile contract: PASS
- overall Kustomize contract: PASS

The verified task-owned kubectl binary was removed after the run. No Kubernetes
cluster mutation or deployment was performed.
