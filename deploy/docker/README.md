# Docker Deployment

The root `docker-compose.yml` is the local Docker entry point. Running
`docker compose up -d` starts MySQL, Redis, and Kafka for a fast host-based
backend development loop.

Create a local, ignored environment file before running Compose:

```bash
cp .env.example .env
# Set MYSQL_PASSWORD and MYSQL_ROOT_PASSWORD in .env.
```

The backend is retained under the optional `app` profile and is built from
`backend/Dockerfile`:

```bash
docker compose --profile app up --build
```

This directory is reserved for future Docker-specific deployment assets that
do not belong in the local Compose file.
