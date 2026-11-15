# Database Layout

Database-per-service pattern. Each microservice owns exactly one database
and never touches another service's database.

| Service      | Database      | Container        | Port | Migration folder     |
|--------------|---------------|------------------|------|----------------------|
| auth-service | apilens_auth  | apilens-pg-auth  | 5433 | `migrations/auth/`   |
| file-service | apilens_files | apilens-pg-files | 5435 | `migrations/files/`  |

Each database also gets two sibling databases created automatically:

- `*_test` — used by integration tests
- `*_loadtest` — safe for k6 / JMeter runs

## Migrations

Flyway picks up `db/migrations/<service>/V*.sql` on service startup.
Never edit an applied migration — always add a new `V{n}__description.sql`.

## Applying the demo seed

```bash
docker exec -i apilens-pg-auth psql -U apilens -d apilens_auth < db/seed/demo-data.sql
```

## Verifying isolation

```bash
# This should FAIL (files table does not exist in apilens_auth):
docker exec -it apilens-pg-auth psql -U apilens -d apilens_auth -c "SELECT * FROM files"
# ERROR: relation "files" does not exist  <- expected
```
