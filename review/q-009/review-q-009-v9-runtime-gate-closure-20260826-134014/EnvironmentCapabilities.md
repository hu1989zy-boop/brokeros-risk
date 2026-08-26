# Environment Capabilities

## Host and Toolchain

- OS: macOS 15.7.4, Darwin arm64
- Docker CLI: 29.7.2
- Docker Compose: v5.4.0
- Docker context: `desktop-linux`
- Docker server: 29.7.2
- Docker server OS: Docker Desktop / Linux
- Docker server architecture: aarch64
- Maven: 3.9.9
- Maven runtime: OpenJDK 23.0.2
- Project compiler release: Java 21

Docker daemon access from the normal sandbox was socket-restricted, so the
operator-authorized Docker runtime commands were executed outside that sandbox
against the same Docker Desktop `desktop-linux` context.

## Database Isolation

The pre-existing host MySQL 5.7.11 service on `127.0.0.1:3306` remained active
and untouched. Every destructive Flyway test used a uniquely named Docker
container, a unique database, ephemeral credentials, and a random host port.

The verified MySQL image was arm64 with:

- image ID: `sha256:5e7e005a680e75d935984d3d9390990d2a709b3ed67e92708e9e6747f1f754c9`
- repository digest: `mysql@sha256:b3b90af2a6552ae30c266fdb7d5dd55f3afb72404bb78d37fe8a23eb857fd3fb`

No credential value was logged or written to the repository.

## Compose Port Isolation

The repository Compose file publishes fixed host ports that conflict with
pre-existing host MySQL/Redis services. V9 used a task-owned temporary Compose
override with `!reset []` only for `ports`. All service definitions, images,
volumes, health checks, dependencies, application environment, network paths,
and repository verification logic were unchanged. The override passed
`docker compose config --quiet` and was deleted after execution.

## Temporary Verification Tools

Kustomize verification used the CI-pinned official kubectl v1.36.3 binary with
embedded Kustomize v5.8.1. Its official and actual SHA-256 matched:

`fc8582acde13869a606730a79379d6515f30c68afcced0b5ac8789d5d002b7d6`

The task-owned temporary directory was removed after use.

## Cleanup

- Disposable MySQL test containers remaining: 0
- Disposable Compose containers remaining: 0
- Disposable Compose volumes remaining: 0
- Disposable Compose networks remaining: 0
- Task-owned temporary override/tool directories remaining: 0

Downloaded base images and the Compose-built backend image may remain in Docker
Desktop's normal image cache; no container is running from them.
