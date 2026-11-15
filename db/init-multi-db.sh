#!/bin/bash
# Runs on first Postgres container start.
# Creates test + loadtest databases alongside the primary one.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE ${POSTGRES_DB}_test;
    CREATE DATABASE ${POSTGRES_DB}_loadtest;
EOSQL

echo "Created ${POSTGRES_DB}_test and ${POSTGRES_DB}_loadtest"
