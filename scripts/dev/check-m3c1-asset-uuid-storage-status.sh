#!/usr/bin/env bash
# M3C-1: stable asset UUID and single-file storage status smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
MINIO_CONTAINER="${MINIO_CONTAINER:-delivery-minio}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin123}"

PASS=0
FAIL=0
TOKEN=""
SMOKE_FILE_ID=""
SMOKE_ASSET_UUID=""
TASK_ID=""
TMP_DIR="$(mktemp -d /tmp/delivery-m3c1.XXXXXX)"

pass() {
  PASS=$((PASS + 1))
  printf '  [PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '  [FAIL] %s\n' "$1" >&2
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
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

assert_uuid() {
  local label="$1"
  local value="$2"
  LABEL="${label}" VALUE="${value}" python3 - <<'PY'
import os
import re
label = os.environ["LABEL"]
value = os.environ["VALUE"]
assert re.fullmatch(r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", value), f"{label} is not UUID: {value}"
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

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 30 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="${2:-{}}"
  curl -sS --connect-timeout 3 --max-time 90 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

urlencode() {
  VALUE="$1" python3 - <<'PY'
import os
import urllib.parse
print(urllib.parse.quote(os.environ["VALUE"], safe=""))
PY
}

cleanup() {
  if [[ -n "${SMOKE_FILE_ID}" ]]; then
    local object_rows
    object_rows="$(mysql_exec "SELECT so.bucket, so.object_key FROM data_storage_objects so JOIN data_file_object_versions fov ON fov.storage_object_id = so.id WHERE fov.file_id = ${SMOKE_FILE_ID} AND so.deleted = 0;" 2>/dev/null || true)"
    if [[ -n "${object_rows}" ]] && docker ps --format '{{.Names}}' | grep -qx "${MINIO_CONTAINER}"; then
      while IFS=$'\t' read -r bucket object_key; do
        [[ -z "${bucket}" || -z "${object_key}" ]] && continue
        docker exec \
          -e MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY}" \
          -e MINIO_SECRET_KEY="${MINIO_SECRET_KEY}" \
          -e BUCKET="${bucket}" \
          -e OBJECT_KEY="${object_key}" \
          "${MINIO_CONTAINER}" sh -c '
            mc alias set local http://127.0.0.1:9000 "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null 2>&1 || exit 0
            mc rm --force "local/${BUCKET}/${OBJECT_KEY}" >/dev/null 2>&1 || true
          ' >/dev/null 2>&1 || true
      done <<< "${object_rows}"
    fi
    mysql_exec "UPDATE data_file_object_versions SET deleted = 1, delete_token = id WHERE file_id = ${SMOKE_FILE_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_object_migration_tasks SET deleted = 1, delete_token = id WHERE file_id = ${SMOKE_FILE_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_storage_objects so SET so.deleted = 1, so.delete_token = so.id WHERE so.id IN (SELECT storage_object_id FROM data_file_object_versions WHERE file_id = ${SMOKE_FILE_ID});" >/dev/null 2>&1 || true
    if [[ -n "${TOKEN}" ]]; then
      curl -sS --connect-timeout 3 --max-time 20 -X DELETE \
        "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/file-resources/${SMOKE_FILE_ID}" \
        -H "Authorization: Bearer ${TOKEN}" >/dev/null || true
    fi
  fi
  if [[ -n "${TASK_ID}" ]]; then
    mysql_exec "UPDATE data_object_migration_task_batches SET deleted = 1, delete_token = id WHERE id = ${TASK_ID};" >/dev/null 2>&1 || true
  fi
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

echo "=== M3C-1: asset UUID and storage status ==="

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
echo "--- 2. Database contract and view checks ---"
column_count="$(mysql_exec "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_file_resources' AND COLUMN_NAME = 'asset_uuid';")"
if [[ "${column_count}" == "1" ]]; then
  pass "data_file_resources.asset_uuid 字段存在"
else
  fail "data_file_resources.asset_uuid 字段不存在"
fi

missing_uuid_count="$(mysql_exec "SELECT COUNT(1) FROM data_file_resources WHERE deleted = 0 AND (asset_uuid IS NULL OR asset_uuid = '');")"
duplicate_uuid_count="$(mysql_exec "SELECT COUNT(1) FROM (SELECT asset_uuid FROM data_file_resources WHERE deleted = 0 GROUP BY asset_uuid HAVING COUNT(1) > 1) d;")"
if [[ "${missing_uuid_count}" == "0" ]]; then
  pass "既有文件 asset_uuid 已回填"
else
  fail "存在未回填 asset_uuid 的文件: ${missing_uuid_count}"
fi
if [[ "${duplicate_uuid_count}" == "0" ]]; then
  pass "asset_uuid 无重复"
else
  fail "存在重复 asset_uuid: ${duplicate_uuid_count}"
fi

file_view_total="$(mysql_exec "SELECT COUNT(1) FROM FileAssetView;")"
file_view_missing="$(mysql_exec "SELECT COUNT(1) FROM FileAssetView WHERE asset_uuid IS NULL OR asset_uuid = '';")"
model_view_missing="$(mysql_exec "SELECT COUNT(1) FROM ModelAssetView WHERE asset_uuid IS NULL OR asset_uuid = '';")"
if [[ "${file_view_total}" -gt 0 && "${file_view_missing}" == "0" ]]; then
  pass "FileAssetView 输出 asset_uuid"
else
  fail "FileAssetView asset_uuid 不完整"
fi
if [[ "${model_view_missing}" == "0" ]]; then
  pass "ModelAssetView 对模型文件输出 asset_uuid"
else
  fail "ModelAssetView 存在缺失 asset_uuid 的模型行"
fi

echo ""
echo "--- 3. New file resource gets backend-generated assetUuid ---"
sample_file="${TMP_DIR}/m3c1-smoke.pdf"
printf '%s\n' "M3C-1 asset UUID smoke file" > "${sample_file}"
smoke_name="M3C1-AssetUuid-Smoke-$(date +%Y%m%d%H%M%S)-$$.pdf"
create_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-resources" \
  "{\"originalName\":\"${smoke_name}\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":$(wc -c < "${sample_file}"),\"storageUri\":\"nas://${sample_file}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
assert_ok "${create_response}"
SMOKE_FILE_ID="$(json_data_expr "${create_response}" "data['id']")"
SMOKE_ASSET_UUID="$(json_data_expr "${create_response}" "data['assetUuid']")"
assert_uuid "created assetUuid" "${SMOKE_ASSET_UUID}"
pass "新建文件资源返回稳定 assetUuid"

db_asset_uuid="$(mysql_exec "SELECT asset_uuid FROM data_file_resources WHERE id = ${SMOKE_FILE_ID} AND deleted = 0;")"
if [[ "${db_asset_uuid}" == "${SMOKE_ASSET_UUID}" ]]; then
  pass "数据库 asset_uuid 与 API 返回一致"
else
  fail "数据库 asset_uuid 与 API 返回不一致"
fi

echo ""
echo "--- 4. Catalog APIs include assetUuid and remain sanitized ---"
encoded_asset_uuid="$(urlencode "${SMOKE_ASSET_UUID}")"
catalog_response="$(api_get "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&keyword=${encoded_asset_uuid}&page=1&pageSize=5")"
assert_ok "${catalog_response}"
assert_no_forbidden "catalog file list" "${catalog_response}"
if [[ "$(json_data_expr "${catalog_response}" "any(row['fileId'] == int(${SMOKE_FILE_ID}) and row['assetUuid'] == '${SMOKE_ASSET_UUID}' for row in data['items'])")" == "true" ]]; then
  pass "catalog files list 返回 assetUuid"
else
  fail "catalog files list 未返回新建文件 assetUuid"
fi

catalog_detail_response="$(api_get "/api/data-steward/catalog/files/${SMOKE_FILE_ID}")"
assert_ok "${catalog_detail_response}"
assert_no_forbidden "catalog file detail" "${catalog_detail_response}"
if [[ "$(json_data_expr "${catalog_detail_response}" "data['assetUuid'] == '${SMOKE_ASSET_UUID}' and data['fileId'] == int(${SMOKE_FILE_ID})")" == "true" ]]; then
  pass "catalog file detail 返回 assetUuid"
else
  fail "catalog file detail assetUuid 不符合预期"
fi

search_response="$(api_post "/api/data-steward/catalog/search" \
  "{\"query\":\"${SMOKE_ASSET_UUID}\",\"project_filters\":[\"${PROJECT_ID}\"],\"filters\":{\"assetKind\":[\"FILE\"],\"indexEligibility\":[\"catalog_only\"]},\"page\":{\"limit\":5}}")"
assert_ok "${search_response}"
assert_no_forbidden "catalog search" "${search_response}"
if [[ "$(json_data_expr "${search_response}" "any(row['fileId'] == int(${SMOKE_FILE_ID}) and row['assetUuid'] == '${SMOKE_ASSET_UUID}' and row['sourceView'] == 'FileAssetView' for row in data['results'])")" == "true" ]]; then
  pass "catalog search 返回 assetUuid/sourceView/fileId"
else
  fail "catalog search 未返回 assetUuid/sourceView/fileId"
fi

file_view_uuid="$(mysql_exec "SELECT asset_uuid FROM FileAssetView WHERE file_id = ${SMOKE_FILE_ID};")"
if [[ "${file_view_uuid}" == "${SMOKE_ASSET_UUID}" ]]; then
  pass "FileAssetView 可按新文件读到 asset_uuid"
else
  fail "FileAssetView 新文件 asset_uuid 不一致"
fi

echo ""
echo "--- 5. Storage status and M3B mirror row include assetUuid ---"
status_before_response="$(api_get "/api/data-steward/assets/files/${SMOKE_FILE_ID}/storage-status")"
assert_ok "${status_before_response}"
assert_no_forbidden "storage status before migration" "${status_before_response}"
if [[ "$(json_data_expr "${status_before_response}" "data['assetUuid'] == '${SMOKE_ASSET_UUID}' and data['storageState'] in ['NAS_ONLY', 'MIGRATION_PENDING', 'OBJECT_STORED', 'MIGRATION_FAILED']")" == "true" ]]; then
  pass "storage-status 返回 assetUuid 与单文件稳定状态"
else
  fail "storage-status 首次状态不符合预期"
fi

migration_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks" \
  "{\"fileIds\":[${SMOKE_FILE_ID}],\"targetProvider\":\"MINIO\"}")"
assert_ok "${migration_response}"
assert_no_forbidden "storage migration create" "${migration_response}"
TASK_ID="$(json_data_expr "${migration_response}" "data['taskId']")"
if [[ "$(json_data_expr "${migration_response}" "data['taskStatus'] == 'COMPLETED' and data['rows'][0]['assetUuid'] == '${SMOKE_ASSET_UUID}' and data['rows'][0]['storageState'] == 'OBJECT_STORED'")" == "true" ]]; then
  pass "M3B 小样本迁移行返回 assetUuid 且 OBJECT_STORED"
else
  fail "M3B 小样本迁移行不符合 M3C-1 契约"
fi

status_after_response="$(api_get "/api/data-steward/assets/files/${SMOKE_FILE_ID}/storage-status")"
assert_ok "${status_after_response}"
assert_no_forbidden "storage status after migration" "${status_after_response}"
if [[ "$(json_data_expr "${status_after_response}" "data['assetUuid'] == '${SMOKE_ASSET_UUID}' and data['storageState'] == 'OBJECT_STORED' and data['objectStored'] is True")" == "true" ]]; then
  pass "M3B 迁移后 storage-status 仍显示 OBJECT_STORED"
else
  fail "M3B 迁移后 storage-status 不符合预期"
fi

echo ""
echo "=== M3C-1 smoke summary: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
