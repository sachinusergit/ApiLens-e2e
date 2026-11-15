#!/usr/bin/env bash
# Proves DB-per-service isolation by attempting cross-DB queries.
set -uo pipefail

echo "════════════════════════════════════════════════════════════════"
echo " DB Isolation Check"
echo "════════════════════════════════════════════════════════════════"

fail=0

check_absent() {
  local db=$1 table=$2
  if docker exec "apilens-pg-$db" psql -U apilens -d "apilens_$db" -c "SELECT * FROM $table" >/dev/null 2>&1; then
    echo "❌ FAIL: table '$table' unexpectedly present in apilens_$db"
    fail=$((fail+1))
  else
    echo "✅ PASS: table '$table' correctly absent from apilens_$db"
  fi
}

check_present() {
  local db=$1 table=$2
  if docker exec "apilens-pg-$db" psql -U apilens -d "apilens_$db" -c "SELECT COUNT(*) FROM $table" >/dev/null 2>&1; then
    echo "✅ PASS: table '$table' correctly present in apilens_$db"
  else
    echo "❌ FAIL: table '$table' missing in apilens_$db"
    fail=$((fail+1))
  fi
}

check_present auth users
check_present auth organizations
check_absent  auth files
check_present files files
check_absent  files users
check_absent  files organizations

echo "════════════════════════════════════════════════════════════════"
if [ "$fail" -eq 0 ]; then
  echo "✅ All isolation checks passed"
else
  echo "❌ $fail check(s) failed"
  exit 1
fi
