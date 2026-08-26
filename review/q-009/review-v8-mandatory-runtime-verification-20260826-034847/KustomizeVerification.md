# Kustomize Verification

- Status: PASS
- kubectl version: v1.36.3
- embedded Kustomize version: v5.8.1

## Provenance

The repository CI workflow pins kubectl v1.36.3. The official darwin/arm64
binary and checksum were downloaded from `dl.k8s.io` into a unique directory
under `/private/tmp`.

- Expected SHA-256: `fc8582acde13869a606730a79379d6515f30c68afcced0b5ac8789d5d002b7d6`
- Actual SHA-256: `fc8582acde13869a606730a79379d6515f30c68afcced0b5ac8789d5d002b7d6`

## Command

`sh scripts/verify-kustomize.sh`

The temporary kubectl directory was prepended to `PATH` only for this command.

## Results

- Kustomize base render: PASS
- Kustomize test overlay render: PASS
- Kustomize prod overlay render: PASS
- Kustomize contract verification: PASS

The task-owned temporary binary and directory were deleted after verification.
No repository file or host configuration was modified.
