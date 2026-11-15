# Architecture

```text
Browser -> Nginx -> Gateway -> (auth-service | file-service)
                      |              |              |
                  (JWT check)   apilens_auth   apilens_files + MinIO
                                (Postgres)     (Postgres)
```

## Services

| Service      | Port | Owns                                   |
|--------------|------|----------------------------------------|
| gateway      | 8080 | JWT validation, routing                |
| auth-service | 8081 | users, organizations, refresh tokens   |
| file-service | 8084 | file metadata + MinIO storage          |
| frontend     | 5173 | React SPA                              |
| nginx        | 80   | reverse proxy                          |

## Databases (database-per-service)

| Service      | DB            | Host port |
|--------------|---------------|-----------|
| auth-service | apilens_auth  | 5433      |
| file-service | apilens_files | 5435      |

Each DB is on its own Postgres container with its own volume.
No service touches another service's DB.
