#!/usr/bin/env bash
# M3A: object storage and StorageService foundation smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
MINIO_CONTAINER="${MINIO_CONTAINER:-delivery-minio}"
if [[ -f "tmp/local-env/nas-minio.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "tmp/local-env/nas-minio.env"
  set +a
fi
MINIO_ENDPOINT="${MINIO_ENDPOINT:-${DELIVERY_MINIO_ENDPOINT:-http://127.0.0.1:9000}}"
MINIO_BUCKET="${MINIO_BUCKET:-${DELIVERY_MINIO_DEFAULT_BUCKET:-delivery-m3a-smoke}}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-${DELIVERY_MINIO_ACCESS_KEY:-minioadmin}}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-${DELIVERY_MINIO_SECRET_KEY:-minioadmin123}}"

PASS=0
FAIL=0
TOKEN=""
SMOKE_FILE_ID=""
OBJECT_KEY="m3a-smoke/$(date +%Y%m%d%H%M%S)-$$.pdf"

pass() {
  PASS=$((PASS + 1))
  printf '  [PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '  [FAIL] %s\n' "$1" >&2
}

assert_ok() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
assert data.get("code") == "OK", data
assert data.get("traceId"), data
PY
}

assert_no_forbidden() {
  local label="$1"
  local payload="$2"
  LABEL="${label}" PAYLOAD="${payload}" python3 - <<'PY'
import os
import re
label = os.environ["LABEL"]
payload = os.environ["PAYLOAD"]
patterns = [
    r"/Volumes(?:/|$)",
    r"/Users(?:/|$)",
    r"/private(?:/|$)",
    r"\bnas://",
    r"\bsmb://",
    r"\bstorage_path\b",
    r"\bstorage_uri\b",
    r"\bstoragePath\b",
    r"\bstorageUri\b",
    r"\bobject_key\b",
    r"\bobjectKey\b",
    r"\bbucket\b",
    r"\braw DB row\b",
    r"\braw row\b",
    r"\bsecret\b",
    r"\bpassword\b",
    r"\bselect\s+.+\s+from\b",
    r"\binsert\s+into\b",
    r"\bupdate\s+.+\s+set\b",
    r"\bdelete\s+from\b",
]
for pattern in patterns:
    assert not re.search(pattern, payload, re.IGNORECASE), f"{label} contains forbidden pattern {pattern}: {payload[:1200]}"
PY
}

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"]).get("data")
scope = {
    "__builtins__": {},
    "data": data,
    "len": len,
    "all": all,
    "any": any,
    "set": set,
    "int": int,
    "str": str,
    "isinstance": isinstance,
    "list": list,
}
value = eval(os.environ["EXPR"], scope, scope)
if value is None:
    print("")
elif isinstance(value, bool):
    print("true" if value else "false")
else:
    print(value)
PY
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 30 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="${2:-{}}"
  curl -sS --connect-timeout 3 --max-time 60 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

cleanup() {
  if [[ -n "${SMOKE_FILE_ID}" ]]; then
    curl -sS --connect-timeout 3 --max-time 20 -X DELETE \
      "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/file-resources/${SMOKE_FILE_ID}" \
      -H "Authorization: Bearer ${TOKEN}" >/dev/null || true
  fi
  if docker ps --format '{{.Names}}' | grep -qx "${MINIO_CONTAINER}"; then
    docker exec \
      -e MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY}" \
      -e MINIO_SECRET_KEY="${MINIO_SECRET_KEY}" \
      -e MINIO_ENDPOINT="${MINIO_ENDPOINT}" \
      -e MINIO_BUCKET="${MINIO_BUCKET}" \
      -e OBJECT_KEY="${OBJECT_KEY}" \
      "${MINIO_CONTAINER}" sh -c '
        mc alias set local "$MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null 2>&1 || exit 0
        mc rm --force "local/${MINIO_BUCKET}/${OBJECT_KEY}" >/dev/null 2>&1 || true
      ' >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "=== M3A: StorageService foundation smoke ==="

echo ""
echo "--- 1. Login and switch to project ${PROJECT_ID} ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_data_expr "${login_response}" "data['accessToken']")"

switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch" '{}')"
assert_ok "${switch_response}"
TOKEN="$(json_data_expr "${switch_response}" "data['accessToken']")"
pass "管理员登录并切换项目成功"

echo ""
echo "--- 2. Provider health is available and sanitized ---"
health_response="$(api_get "/api/data-steward/storage/providers/health")"
assert_ok "${health_response}"
assert_no_forbidden "storage provider health" "${health_response}"
if [[ "$(json_data_expr "${health_response}" "any(row['providerCode'] == 'NAS' and row['configured'] is True and row['available'] is True and row['readonly'] is True for row in data)")" == "true" ]]; then
  pass "NAS provider health 可用且只读"
else
  fail "NAS provider health 不符合预期"
fi

if [[ "$(json_data_expr "${health_response}" "any(row['providerCode'] == 'MINIO' and row['configured'] is True and row['available'] is True and row['readonly'] is True for row in data)")" == "true" ]]; then
  pass "MinIO provider health 可用且只读"
else
  fail "MinIO provider health 不符合预期"
fi

if [[ "$(json_data_expr "${health_response}" "all(set(row.keys()) == {'providerCode', 'displayName', 'configured', 'available', 'readonly', 'writable', 'unavailableReason'} for row in data)")" == "true" ]]; then
  pass "provider health 未返回 bucket/key/storage uri 等底层字段"
else
  fail "provider health 字段集合不符合脱敏契约"
fi

echo ""
echo "--- 3. Existing NAS file storage status is safe ---"
catalog_response="$(api_get "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&page=1&pageSize=1")"
assert_ok "${catalog_response}"
file_id="$(json_data_expr "${catalog_response}" "data['items'][0]['fileId']")"
status_response="$(api_get "/api/data-steward/assets/files/${file_id}/storage-status")"
assert_ok "${status_response}"
assert_no_forbidden "NAS file storage status" "${status_response}"
if [[ "$(json_data_expr "${status_response}" "data['storageState'] in ['NAS_ONLY', 'OBJECT_STORED', 'MIGRATION_PENDING', 'MIGRATION_FAILED'] and data['activeProvider'] in ['NAS', 'MINIO', 'S3_COMPATIBLE', 'OSS', 'UNKNOWN'] and data['fileId'] == int(${file_id})")" == "true" ]]; then
  pass "既有文件 storage-status 返回稳定状态且不暴露路径"
else
  fail "既有文件 storage-status 不符合预期"
fi

echo ""
echo "--- 4. MinIO object read path works through existing access ticket ---"
if ! docker ps --format '{{.Names}}' | grep -qx "${MINIO_CONTAINER}"; then
  fail "未找到 MinIO 容器 ${MINIO_CONTAINER}"
else
  docker exec \
    -e MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY}" \
    -e MINIO_SECRET_KEY="${MINIO_SECRET_KEY}" \
    -e MINIO_ENDPOINT="${MINIO_ENDPOINT}" \
    -e MINIO_BUCKET="${MINIO_BUCKET}" \
    -e OBJECT_KEY="${OBJECT_KEY}" \
    "${MINIO_CONTAINER}" sh -c '
      set -e
      printf "%s\n" "M3A MinIO smoke object for StorageService read path" > /tmp/m3a-smoke.pdf
      mc alias set local "$MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null
      mc mb -p "local/${MINIO_BUCKET}" >/dev/null
      mc cp /tmp/m3a-smoke.pdf "local/${MINIO_BUCKET}/${OBJECT_KEY}" >/dev/null
      rm -f /tmp/m3a-smoke.pdf
    '
  pass "MinIO 测试对象已准备"

  create_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-resources" \
    "{\"originalName\":\"M3A-StorageService-MinIO-Smoke.pdf\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":52,\"storageUri\":\"minio://${MINIO_BUCKET}/${OBJECT_KEY}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
  assert_ok "${create_response}"
  SMOKE_FILE_ID="$(json_data_expr "${create_response}" "data['id']")"

  object_status_response="$(api_get "/api/data-steward/assets/files/${SMOKE_FILE_ID}/storage-status")"
  assert_ok "${object_status_response}"
  assert_no_forbidden "MinIO file storage status" "${object_status_response}"
  if [[ "$(json_data_expr "${object_status_response}" "data['storageState'] == 'OBJECT_STORED' and data['activeProvider'] == 'MINIO' and data['objectStored'] is True")" == "true" ]]; then
    pass "MinIO 文件 storage-status 表达为 OBJECT_STORED"
  else
    fail "MinIO 文件 storage-status 不符合预期"
  fi

  ticket_response="$(api_post "/api/data-steward/assets/files/${SMOKE_FILE_ID}/access-tickets" '{"action":"PREVIEW"}')"
  assert_ok "${ticket_response}"
  access_url="$(json_data_expr "${ticket_response}" "data['accessUrl']")"
  preview_body="$(curl -fsS --connect-timeout 3 --max-time 30 "${BASE_URL}${access_url}")"
  if [[ "${preview_body}" == *"M3A MinIO smoke object"* ]]; then
    pass "现有预览票据可通过 StorageService 读取 MinIO 对象"
  else
    fail "MinIO 预览票据读取内容不匹配"
  fi
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
