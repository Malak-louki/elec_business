#!/usr/bin/env bash
set -euo pipefail

MVNW="./mvnw"
if [ "${OS:-}" = "Windows_NT" ]; then MVNW="./mvnw.cmd"; fi

( cd backend && $MVNW -q -DskipTests package || true )

if [[ -f "frontend/package.json" ]]; then
  ( cd frontend && npm ci )
else
  echo "ℹ️ Pas de frontend/package.json : étape npm ignorée."
fi

echo "✅ Setup terminé"
