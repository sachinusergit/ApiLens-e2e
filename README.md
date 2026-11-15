# apilens-mvp

Enterprise API Testing Platform — MVP slice.

Contains: auth-service, file-service, gateway, React frontend,
Postgres (auth + files), MinIO, RabbitMQ, Redis, k6 load tests,
and DB/microservices test suite.

## Quick start

```bash
# 1. Copy env
cp .env.example .env

# 2. Start databases + infra
docker compose up -d postgres-auth postgres-files minio rabbitmq redis

# 3. Build + start services (first run: ~3-5 min)
docker compose up -d auth-service file-service gateway frontend nginx

# 4. Open in browser
open http://localhost
```

Demo user is seeded automatically:

- Email: ravi@example.com
- Password: Test@1234

## What you'll see in DevTools

Every UI action = a network call with:

- `Authorization: Bearer eyJ...`
- `X-Request-Id: <uuid>`
- `Content-Type: application/json` (or `multipart/form-data` for uploads)
- Full request payload + response

## Run tests

```bash
# DB + integration + isolation tests
cd microservices-tests && mvn test

# Load smoke test
k6 run loadtests/k6/smoke.js
```

## Re-zip anytime

```bash
./scripts/make-zip.sh
# -> apilens-mvp-YYYYMMDD-HHMM.zip
```
