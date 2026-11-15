# Running the project

## 1. Prerequisites

- Docker Desktop
- Java 17 (only to build locally without Docker)
- Node 20 (only to run the frontend without Docker)
- k6 (optional, for load tests)

## 2. First start

```bash
cd apilens-mvp
cp .env.example .env

# Start databases first
docker compose up -d postgres-auth postgres-files minio rabbitmq redis

# Wait ~15s for Postgres to be healthy
docker compose ps

# Then start services
docker compose up -d auth-service file-service gateway frontend nginx
```

## 3. Demo user

Created automatically by Flyway migration `V2__seed_demo_user.sql`.
(`./scripts/seed-users.sh` can re-apply it; it is optional.)

## 4. Open

http://localhost  - login: ravi@example.com / Test@1234

## 5. Rebuild after code changes

```bash
docker compose build auth-service
docker compose up -d auth-service
```

## 6. Logs

```bash
docker compose logs -f auth-service
docker compose logs -f file-service
docker compose logs -f gateway
```

## 7. Stop everything

```bash
docker compose down        # stop, keep data
docker compose down -v     # stop, delete databases too
```
