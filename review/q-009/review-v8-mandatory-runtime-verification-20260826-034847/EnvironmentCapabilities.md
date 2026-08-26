# Environment Capabilities

## Host

- OS: macOS 15.7.4, Darwin arm64
- Shell: zsh
- Direct `java -version`: Amazon Corretto 8 (`1.8.0_432`) through `/usr/bin/java`
- Maven: Apache Maven 3.9.9
- Maven runtime: Homebrew OpenJDK 23.0.2
- Project compiler release: Java 21

The direct shell Java and Maven runtime differ. Maven nevertheless compiled and
verified the Java 21 project successfully using its OpenJDK 23 runtime.

## Runtime Capability Inventory

| Capability | Observation | Usable for V8 |
| --- | --- | --- |
| Docker CLI | `docker` not found | NO |
| Docker Compose v2 | unavailable because Docker CLI is absent | NO |
| Docker daemon/socket | common Docker, Colima, Rancher Desktop, OrbStack, and Podman sockets absent | NO |
| Host MySQL | pre-existing MySQL 5.7.11 on `127.0.0.1:3306` | NO — wrong version and not disposable |
| MySQL 8.4 | no client, binary, server, or container runtime found | NO |
| Redis | pre-existing Redis on `127.0.0.1:6379`, `PING` returned `PONG` | observed only; not task-owned |
| Kafka | no listener on `127.0.0.1:9092` or `29092` | NO |
| Application | no listener on `127.0.0.1:8080` | NO |
| Local kubectl/kustomize | not installed on host | NO |
| Temporary official kubectl | v1.36.3, embedded Kustomize v5.8.1 | YES; used and removed |

## Safe Capability Actions

An official darwin/arm64 `kubectl` v1.36.3 binary was downloaded to a unique
task-owned directory under `/private/tmp`, verified against the official
SHA-256, used only for repository Kustomize rendering, and then deleted. The
expected and actual SHA-256 were:

`fc8582acde13869a606730a79379d6515f30c68afcced0b5ac8789d5d002b7d6`

No package manager, system software, daemon, host configuration, container,
volume, network, database, or pre-existing developer service was modified.

## Safety Boundary

The pre-existing MySQL and Redis processes were treated as developer-owned.
No credentials were guessed or exposed. The MySQL service was not used because
the Q-009 test performs destructive Flyway cleanup and the service is both the
wrong version and not known to be disposable.
