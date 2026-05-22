#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVER_PORT="${SERVER_PORT:-8080}"
RUNTIME_DIR="${ROOT_DIR}/tmp/run-logs"
APP_JAR="delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar"
RUNTIME_JAR="${RUNTIME_DIR}/delivery-app-runtime.jar"
export SERVER_PORT

run_app() {
  mkdir -p "${RUNTIME_DIR}"
  cp "${APP_JAR}" "${RUNTIME_JAR}"
  exec java -jar "${RUNTIME_JAR}"
}

cd "${ROOT_DIR}/backend"

if [[ -x "./mvnw" ]]; then
  ./mvnw -pl delivery-app -am -DskipTests package
  run_app
  exit 0
fi

if command -v mvn >/dev/null 2>&1; then
  mvn -pl delivery-app -am -DskipTests package
  run_app
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
  bash -lc 'mvn -pl delivery-app -am -DskipTests package && mkdir -p /tmp/delivery-runtime && cp delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar /tmp/delivery-runtime/delivery-app-runtime.jar && exec java -jar /tmp/delivery-runtime/delivery-app-runtime.jar'
