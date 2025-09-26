#!/usr/bin/env bash
set -euo pipefail

MVNW="./mvnw"
if [ "${OS:-}" = "Windows_NT" ]; then MVNW="./mvnw.cmd"; fi

( cd backend && $MVNW -q spring-boot:run )
