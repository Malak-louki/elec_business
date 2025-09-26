#!/usr/bin/env bash
set -euo pipefail

MVNW="./mvnw"
if [ "${OS:-}" = "Windows_NT" ]; then MVNW="./mvnw.cmd"; fi

( cd backend && $MVNW -q test )

if [[ -f "frontend/package.json" ]]; then
  ( cd frontend && npm test -- --ci )
fi

echo "✅ Tests OK"
