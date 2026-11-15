#!/usr/bin/env bash
set -euo pipefail

TIMESTAMP=$(date +%Y%m%d-%H%M)
ZIP="apilens-mvp-${TIMESTAMP}.zip"

cd "$(dirname "$0")/.."

echo "📦 Creating $ZIP..."

zip -r "$ZIP" . \
  -x "*/node_modules/*" \
  -x "*/target/*" \
  -x "*/.git/*" \
  -x "*/.idea/*" \
  -x "*/.vscode/*" \
  -x "*.zip" \
  -x ".env" \
  -x "*.log" \
  -x "*/dist/*" \
  -x "*/.vite/*"

echo "✅ Created $ZIP ($(du -h "$ZIP" | cut -f1))"
