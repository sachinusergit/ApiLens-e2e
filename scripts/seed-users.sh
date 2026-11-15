#!/usr/bin/env bash
# Applies the demo seed (ravi@example.com / Test@1234) to the auth DB.
# NOTE: the same user is also created automatically by migration V2__seed_demo_user.sql,
# so running this is optional (it is safe to re-run).
set -euo pipefail

cd "$(dirname "$0")/.."

echo "🌱 Seeding demo data..."

docker exec -i apilens-pg-auth psql -U apilens -d apilens_auth < db/seed/demo-data.sql

echo "✅ Demo user ready: ravi@example.com / Test@1234"
