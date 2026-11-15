# Database Testing

Three layers of DB tests in `microservices-tests/`:

## 1. Integration tests (FileIntegrationTest)

Hit a real HTTP endpoint, then assert the row was written to real Postgres.

```bash
cd microservices-tests
mvn test -Dtest=FileIntegrationTest
```

Requires the databases and services to be running (`docker compose up -d`).

## 2. Isolation tests (IsolationTest)

Prove the DBs are truly separate:

- `apilens_auth` must NOT have a `files` table
- `apilens_files` must NOT have a `users` table

```bash
mvn test -Dtest=IsolationTest
```

## 3. Shell-level check

```bash
./scripts/verify-db-isolation.sh
```

## What isolation buys you

- You cannot accidentally join across services in a query
- Schema changes in one service cannot break another
- Each service can scale its own DB independently
- Backups / restores are per service
