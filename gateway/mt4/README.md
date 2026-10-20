# MT4 Gateway — build & end-to-end runbook (Q-015 Phase B, Windows line)

This is the **from-scratch rebuild guide** for the MT4 gateway: a C++ program that
connects to an MT4 server (Manager API, read-only), maps each trade into the ADR-024
**broker-neutral canonical event**, wraps it in the Phase A envelope, and **produces it to
Kafka** (`trading-data.canonical`). The Java backend consumes that topic and persists it
(Phase A store). Use this whenever the build machine is lost and must be rebuilt.

```
  MT4 server ──Manager API (read-only)──▶ mt4gateway.exe ──▶ Kafka `trading-data.canonical`
                                           (this folder)         │
                                                                 ▼
                                      Spring Boot backend @KafkaListener ──▶ MySQL trading_data_event
```

Authoritative specs (read if changing behaviour): the parent Requirement
`docs/requirements/Q-015-Trading-Data-Ingestion-Foundation.md`, the Phase A addendum,
`docs/adr/ADR-024-trading-data-canonical-model.md`, and
`docs/architecture/q-015-phase-b-mt4-gateway-implementation-design.md` (V2).

---

## What is in this folder (tracked in git)

| File | Purpose |
| --- | --- |
| `mt4gateway.cpp` | The gateway. Snapshot pull → canonical JSON → envelope → Kafka. |
| `buildgw.bat` | One-shot build in the x64 Native Tools prompt. |
| `README.md` | This runbook. |

## What is NOT in git (you supply it on each machine — `.gitignore` blocks it)

| Item | Where it goes | Why not committed |
| --- | --- | --- |
| `Include\MT4ManagerAPI.h` (MetaQuotes SDK header) | `gateway\mt4\Include\` | Licensed MetaQuotes vendor code. |
| `libs\mtmanapi64.dll` (MetaQuotes 64-bit runtime) | `gateway\mt4\libs\` | Licensed MetaQuotes vendor binary. |
| librdkafka (via vcpkg) | `C:\vcpkg\...` | Large toolchain, installed per machine. |
| `mt4gateway.exe`, `*.obj`, copied `*.dll` | `gateway\mt4\` (build output) | Rebuilt from source. |

> **Never commit the SDK.** The `Include\` and `libs\` folders here, and all `*.dll`/
> `*.exe`/`*.obj`, are git-ignored for exactly this reason.

---

## Prerequisites

- A 64-bit Windows machine (Windows Server 2022/2025 or Windows 10/11 x64).
- The MetaQuotes **MT4 Manager API** SDK obtained from your broker (you need
  `MT4ManagerAPI.h` and `mtmanapi64.dll`).
- Your MT4 **manager** server address, login, and password (read-only manager account).
- Internet access to download VS Build Tools, vcpkg/librdkafka, and the backend repo.

Two valid layouts:
- **Single box** (what we used): the Windows machine runs **both** the gateway and the
  Docker backend stack (Kafka/MySQL/backend). Then the gateway talks to Kafka on
  `127.0.0.1:29092`.
- **Split**: gateway on Windows, backend stack on another host. Then the gateway's last
  argument is `that-host-ip:29092` and that host must expose the Kafka port.

This runbook assumes the **single box** layout. For split, only the Kafka address changes.

---

## Part A — C++ toolchain

### A1. Visual Studio Build Tools (the C++ compiler `cl.exe`)

1. Download **Build Tools for Visual Studio** from
   <https://visualstudio.microsoft.com/downloads/> (scroll to "Tools for Visual Studio" →
   "Build Tools for Visual Studio"). The web installer is a few MB.
2. Run it. In the **Workloads** tab tick **"Desktop development with C++"**. Keep the
   default components. Install (installs to `C:\` by default; several GB).
3. After it finishes, open the Start menu and find **"x64 Native Tools Command Prompt for
   VS 2022"** (or 2026). **You must use THIS prompt for every build command below** — a
   plain `cmd`/PowerShell does not know where `cl.exe` is (`'cl' is not recognized`).

Verify: in that prompt, `cl` alone should print the compiler banner, not an error.

### A2. librdkafka (the Kafka client the gateway links against) — via vcpkg, no git needed

`git` is not required. In **PowerShell**:

```powershell
Invoke-WebRequest -Uri https://github.com/microsoft/vcpkg/archive/refs/heads/master.zip -OutFile C:\vcpkg.zip
Expand-Archive C:\vcpkg.zip -DestinationPath C:\
Rename-Item C:\vcpkg-master C:\vcpkg
C:\vcpkg\bootstrap-vcpkg.bat
C:\vcpkg\vcpkg install librdkafka:x64-windows
```

This leaves headers in `C:\vcpkg\installed\x64-windows\include`, the import lib at
`C:\vcpkg\installed\x64-windows\lib\rdkafka.lib`, and the runtime DLLs in
`C:\vcpkg\installed\x64-windows\bin` — exactly where `buildgw.bat` expects them.

---

## Part B — Get the source + place the SDK

1. Get this repository onto the box. Easiest without git: on GitHub open the repo →
   **Code ▸ Download ZIP**, copy it over, unzip (e.g. to `C:\brokeros-risk`).
2. Go to `C:\brokeros-risk\gateway\mt4\`. It already has `mt4gateway.cpp` + `buildgw.bat`.
3. From your MetaQuotes SDK, create these two subfolders **inside** `gateway\mt4\`:
   - `Include\MT4ManagerAPI.h`
   - `libs\mtmanapi64.dll`

   So the folder looks like:
   ```
   gateway\mt4\
     mt4gateway.cpp
     buildgw.bat
     Include\MT4ManagerAPI.h
     libs\mtmanapi64.dll
   ```

---

## Part C — Build the gateway

In the **x64 Native Tools Command Prompt for VS**, `cd` into the folder and run the script:

```bat
cd C:\brokeros-risk\gateway\mt4
buildgw.bat
```

On success it prints `BUILD OK` and copies `mtmanapi64.dll` + the librdkafka DLLs next to
`mt4gateway.exe` (the exe needs those DLLs beside it at runtime). If it prints
`BUILD FAILED`, copy the **entire** error text — see Troubleshooting.

---

## Part D — Backend stack (Docker: MySQL + Redis + Kafka + backend)

1. Install **Docker Desktop for Windows** and start it (wait until it says *Engine
   running*).
2. In `C:\brokeros-risk\` create a file named `.env` with dev-only values. Minimal example
   (adjust passwords; these are **ephemeral local dev** values, never production):

   ```dotenv
   MYSQL_ROOT_PASSWORD=devlocal_root_pw
   MYSQL_DATABASE=brokeros_risk
   MYSQL_HOST_PORT=3307
   # ...plus whatever else docker-compose.yml marks required (see its `environment:` keys)
   ```

   `MYSQL_HOST_PORT=3307` matters: on Windows, host port 3306 is often reserved/blocked
   ("forbidden by its access permissions"). The backend still reaches MySQL internally on
   `mysql:3306`; 3307 is only the host-side mapping.
3. Bring the stack up (PowerShell or cmd, in the repo root):

   ```bat
   docker compose --profile app up --build -d mysql redis kafka backend
   ```

   Wait until `docker ps` shows all four healthy/started.
4. Create the topic the gateway produces to (run once). Find the Kafka container name with
   `docker ps`, then:

   ```bat
   docker exec <kafka-container> /opt/kafka/bin/kafka-topics.sh --create ^
     --topic trading-data.canonical --partitions 3 --replication-factor 1 ^
     --bootstrap-server localhost:9092
   ```

   (On the `apache/kafka` image the scripts are under `/opt/kafka/bin/`. If it already
   exists you'll get a harmless "already exists" error.)

---

## Part E — Run end-to-end + verify

1. Run the gateway (in the x64 Native Tools prompt, from `gateway\mt4\`). **Use the IPv4
   literal `127.0.0.1`, not `localhost`** — see Troubleshooting:

   ```bat
   mt4gateway.exe <server:port> <managerLogin> <password> <sourceServerId> 127.0.0.1:29092 30
   ```

   The last `30` caps how many snapshot trades to send. Expect:
   `DONE. produced=30 delivered=30 failed=0 -> trading-data.canonical`.
2. (Optional) watch the raw messages:

   ```bat
   docker exec <kafka-container> /opt/kafka/bin/kafka-console-consumer.sh ^
     --topic trading-data.canonical --from-beginning --bootstrap-server localhost:9092 --timeout-ms 5000
   ```
3. Confirm the backend persisted them to MySQL:

   ```bat
   docker exec <mysql-container> mysql -uroot -p<MYSQL_ROOT_PASSWORD> -B brokeros_risk ^
     -e "SELECT COUNT(*) FROM trading_data_event;"
   ```

   A non-zero count that matches `delivered` (minus any intentional gap markers) means the
   full path MT4 → Kafka → backend → MySQL works.

---

## Troubleshooting (every failure we actually hit)

| Symptom | Cause | Fix |
| --- | --- | --- |
| `'cl' is not recognized` | Wrong prompt | Use the **x64 Native Tools Command Prompt for VS**, not plain cmd/PowerShell. |
| `#error Build as 64-bit` | 32-bit prompt | Same — the **x64** Native Tools prompt. |
| `BUILD FAILED` with `cannot open ... rdkafka.lib` / `MT4ManagerAPI.h` not found | SDK/vcpkg not placed | Confirm `Include\MT4ManagerAPI.h`, `libs\mtmanapi64.dll`, and `C:\vcpkg\installed\x64-windows\` all exist. |
| `mtmanapi64.dll not loaded` at runtime | DLL not beside the exe | `buildgw.bat` copies it; ensure `mtmanapi64.dll` + `rdkafka*.dll` sit next to `mt4gateway.exe`. |
| `Connect failed` / `Login failed` | Wrong server/login/password, or no network to the MT4 server | Verify the manager credentials and that the box can reach the server:port. |
| Gateway prints `1/1 brokers are down`, `delivered=0`, log shows `ipv6#[::1]:29092` | `localhost` resolved to IPv6, Kafka listens on IPv4 | Pass **`127.0.0.1:29092`**. (The source already sets `broker.address.family=v4`.) |
| `docker: invalid reference format` | `&`/`?` in an env value on the CLI | Use the `.env` file (`--env-file`), not inline env vars. |
| MySQL container won't bind / `port ... forbidden by its access permissions` | Host port 3306 reserved on Windows | Set `MYSQL_HOST_PORT=3307` in `.env`. |
| `unknown variable 'default-authentication-plugin'` | MySQL 8.4 dropped that flag | Remove it from any MySQL command/compose override. |

---

## Known gaps / not yet done (tracked — do not assume these work)

This gateway is the **W1+W2 snapshot** path. Deferred items (Q-015 §17 + the Phase B
consumer review conditions), **not** implemented here yet:

1. **Per-account `sourceSequence`.** Currently one global counter; the Phase A gap
   detection assumes per-`(server,seq)` ordering while Kafka partitions by account. Harmless
   for a single-threaded snapshot run (you may see benign gap markers), but must be fixed to
   per-account sequencing before concurrent/live production.
2. **Reconnect epoch field.** The envelope has no epoch; a reconnect that resets the
   sequence could collide with earlier rows. Needs a durable epoch/discontinuity field.
3. **Live pumping.** This sends a one-shot `TradesRequest` snapshot. Real-time
   `PumpingSwitchEx` streaming (ADD/UPDATE/DELETE, quotes, reconnect) is designed in the
   Implementation Design but not built here.
4. **Account `currency`** on the canonical event (ADR-024 money rule) is not yet populated.
5. **MT5 gateway** and the downstream **Flink** markout job are separate, later work.

Never invent MT4/MT5 Manager API interfaces or canonical fields without the real SDK
(AGENTS.md L75).
