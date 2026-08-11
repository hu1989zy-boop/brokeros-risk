# Kubernetes Deployment

The manifests use Kustomize:

- `base/` contains the Deployment, Service, and common ConfigMap.
- `test/` applies the test profile and test resource settings.
- `prod/` applies the production profile and production resource settings.

Render an environment locally with:

```bash
kubectl kustomize deploy/kubernetes/base
kubectl kustomize deploy/kubernetes/test
kubectl kustomize deploy/kubernetes/prod
```

Run `sh scripts/verify-kustomize.sh` to render all three and assert their common
resource, profile, label, and Secret-reference contracts. Rendering is not a
cluster deployment.

Both environment overlays expect a Secret named `brokeros-risk-secrets` with a
`db-password` key. Create it through each cluster's approved secret-management
process; do not commit credentials or generated Secret manifests.

## Phase 0.5 review

The current Kustomize `base + environment overlays` structure is retained. It
is sufficient for the current single-deployable modular monolith and can accept
incremental configuration without introducing Helm or additional overlay
layers. Revisit the choice only when deployment variability creates a concrete
maintenance problem.
