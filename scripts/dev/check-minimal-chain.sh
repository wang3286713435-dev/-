#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-admin}"
PASSWORD="${3:-123456}"
TARGET_PROJECT_ID="${4:-2}"

login_response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")"

echo "== login =="
echo "${login_response}"

access_token="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${login_response}")"
refresh_token="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["refreshToken"])' <<< "${login_response}")"

refresh_response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/refresh" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"${refresh_token}\"}")"

echo "== refresh token =="
echo "${refresh_response}"

me_response="$(curl -sS "${BASE_URL}/api/core/users/me" \
  -H "Authorization: Bearer ${access_token}")"

echo "== current user =="
echo "${me_response}"

switch_response="$(curl -sS -X POST "${BASE_URL}/api/core/projects/${TARGET_PROJECT_ID}:switch" \
  -H "Authorization: Bearer ${access_token}")"

echo "== switch project =="
echo "${switch_response}"

switched_access_token="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${switch_response}")"

overview_response="$(curl -sS "${BASE_URL}/api/work-center/projects/${TARGET_PROJECT_ID}/home/overview" \
  -H "Authorization: Bearer ${switched_access_token}")"

echo "== home overview =="
echo "${overview_response}"
