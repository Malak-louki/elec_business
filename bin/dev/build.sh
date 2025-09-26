#!/usr/bin/env bash
set -euo pipefail

MVNW="./mvnw"
if [ "${OS:-}" = "Windows_NT" ]; then MVNW="./mvnw.cmd"; fi

MODE="${1:-integrated}" # integrated | split

if [[ "$MODE" == "integrated" ]]; then
  if [[ -f "frontend/package.json" ]]; then
    ( cd frontend && npm run build -- --configuration production )

    # Détection robuste du dossier dist Angular (Angular 17/18/20 = dist/<app>/browser)
    DIST_SRC=""
    if [[ -d "frontend/dist/browser" ]]; then
      DIST_SRC="frontend/dist/browser"
    elif compgen -G "frontend/dist/*/browser" > /dev/null; then
      DIST_SRC=$(echo frontend/dist/*/browser | awk '{print $1}')
    elif [[ -d "frontend/dist" ]]; then
      DIST_SRC="frontend/dist"
    fi
    if [[ -z "$DIST_SRC" ]]; then
      echo "❌ Impossible de trouver le build Angular (dist)."; exit 1
    fi

    rm -rf backend/src/main/resources/static || true
    mkdir -p backend/src/main/resources/static
    cp -r "$DIST_SRC"/* backend/src/main/resources/static/
  else
    echo "ℹ️ Pas de frontend : build intégré sans assets Angular."
  fi

  ( cd backend && $MVNW -q clean package -DskipTests )
  echo "✅ Build intégré: backend/target/*.jar contient le front (si présent)"

else
  if [[ -f "frontend/package.json" ]]; then
    ( cd frontend && npm run build -- --configuration production )
  fi
  ( cd backend && $MVNW -q clean package -DskipTests )
  echo "✅ Build split: artefacts séparés"
fi
