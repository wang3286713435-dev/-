#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVER_PORT="${SERVER_PORT:-18080}"
export SERVER_PORT

cd "${ROOT_DIR}/backend"

if [[ -x "./mvnw" ]]; then
  ./mvnw -pl delivery-app -am -DskipTests package
  java -jar delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar
  exit 0
fi

if command -v mvn >/dev/null 2>&1; then
  mvn -pl delivery-app -am -DskipTests package
  java -jar delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar
  exit 0
fi

docker run --rm \
  --name delivery-backend-dev \
  --network infra_default \
  -p "${SERVER_PORT}:${SERVER_PORT}" \
  -e SERVER_PORT="${SERVER_PORT}" \
  -e DELIVERY_DB_URL="jdbc:mysql://mysql:3306/delivery_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8" \
  -e DELIVERY_DB_USERNAME="delivery" \
  -e DELIVERY_DB_PASSWORD="delivery123" \
  -v "${HOME}/.m2:/root/.m2" \
  -v "${ROOT_DIR}/backend:/workspace" \
  -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  bash -lc 'mvn -pl delivery-app -am -DskipTests package && java -jar delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar'
