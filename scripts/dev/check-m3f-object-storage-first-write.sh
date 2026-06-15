#!/usr/bin/env bash
# M3F: new uploaded files write to object storage first.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
MINIO_CONTAINER="${MINIO_CONTAINER:-delivery-minio}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
PROJECT_CODE="M3F-SMOKE-${RUN_ID}"
PROJECT_NAME="M3F对象存储优先上传测试${RUN_ID}"
NAS_ROOT="${NAS_ROOT:-/tmp/delivery-m3f-object-first-${RUN_ID}}"
UPLOAD_DIR="object-first-zone"
UPLOAD_FILE_NAME="m3f-object-first-${RUN_ID}.txt"
UNAVAILABLE_FILE_NAME="m3f-object-unavailable-${RUN_ID}.txt"

PASS=0
FAIL=0
ADMIN_TOKEN=""
PROJECT_ID=""
ADMIN_ID=""
FILE_ID=""
OBJECT_ROWS=""
OBJECT_ENDPOINT_TYPE=""
OBJECT_READINESS_STATUS=""
MINIO_PAUSED=0
TMP_DIR="$(mktemp -d /tmp/delivery-m3f.XXXXXX)"

pass() {
  PASS=$((PASS + 1))
  printf '  [PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '  [FAIL] %s\n' "$1" >&2
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql --default-character-set=utf8mb4 -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

json_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
scope = {
    "__builtins__": {},
    "data": data,
    "len": len,
    "all": all,
    "any": any,
    "bool": bool,
    "int": int,
    "str": str,
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

assert_not_ok() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
assert data.get("code") != "OK", data
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
    r"/tmp(?:/|$)",
    r"/private(?:/|$)",
    r"/var(?:/|$)",
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
    r"\bSQL\b",
    r"\btoken\b",
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

post_json() {
  local url="$1"
  local body="${2:-{}}"
  curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

put_json() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 30 -X PUT "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

get_json() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 30 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}"
}

upload_file() {
  local file_path="$1"
  curl -sS --connect-timeout 3 --max-time 60 -X POST "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/nas/files:upload" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -F "parentPath=${UPLOAD_DIR}" \
    -F "fileKind=DOCUMENT" \
    -F "discipline=SMOKE" \
    -F "versionNo=V1" \
    -F "file=@${file_path};type=text/plain"
}

login() {
  local response
  response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${response}"
  json_expr "${response}" "data['data']['accessToken']"
}

switch_project() {
  local project_id="$1"
  local response
  response="$(post_json "/api/core/projects/${project_id}:switch" '{}')"
  assert_ok "${response}"
  json_expr "${response}" "data['data']['accessToken']"
}

cleanup() {
  if [[ "${MINIO_PAUSED}" == "1" ]]; then
    docker unpause "${MINIO_CONTAINER}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${OBJECT_ROWS}" ]] && docker ps --format '{{.Names}}' | grep -qx "${MINIO_CONTAINER}"; then
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
    done <<< "${OBJECT_ROWS}"
  fi
  if [[ -n "${PROJECT_ID}" ]]; then
    mysql_exec "UPDATE data_storage_objects so SET so.deleted=1, so.delete_token=so.id WHERE so.id IN (SELECT fov.storage_object_id FROM data_file_object_versions fov JOIN data_file_resources f ON f.id=fov.file_id WHERE f.project_id=${PROJECT_ID});" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_file_object_versions SET deleted=1, delete_token=id WHERE file_id IN (SELECT id FROM data_file_resources WHERE project_id=${PROJECT_ID});" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_nas_write_trial_configs SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_file_resources SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_nas_directory_records SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_asset_project_path_mappings SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_user_project_roles SET deleted=1 WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_projects SET deleted=1 WHERE id=${PROJECT_ID};" >/dev/null 2>&1 || true
  fi
  rm -rf "${NAS_ROOT}" "${TMP_DIR}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "=== M3F: object-storage-first upload ==="

echo ""
echo "--- 1. Prepare isolated project and safe directory ---"
mkdir -p "${NAS_ROOT}/${UPLOAD_DIR}"
printf 'M3F object-first upload smoke %s\n' "${RUN_ID}" > "${TMP_DIR}/${UPLOAD_FILE_NAME}"
printf 'M3F unavailable object-store smoke %s\n' "${RUN_ID}" > "${TMP_DIR}/${UNAVAILABLE_FILE_NAME}"

ADMIN_ID="$(mysql_exec "SELECT id FROM core_users WHERE username='${ADMIN_USER}' AND deleted=0 ORDER BY id DESC LIMIT 1;")"
mysql_exec "INSERT INTO core_projects (code, name, industry_type, project_stage, project_manager_name, asset_status, asset_source, status, created_by, updated_by)
VALUES ('${PROJECT_CODE}', '${PROJECT_NAME}', 'BIM', 'SMOKE', 'M3F', 'ACTIVE', 'M3F_SMOKE', 'ACTIVE', 1, 1);"
PROJECT_ID="$(mysql_exec "SELECT id FROM core_projects WHERE code='${PROJECT_CODE}' AND deleted=0 ORDER BY id DESC LIMIT 1;")"
mysql_exec "INSERT INTO data_asset_project_path_mappings (project_id, provider_code, nas_path, match_strategy, enabled, sort_order, remark, created_by, updated_by)
VALUES (${PROJECT_ID}, 'NAS', '${NAS_ROOT}', 'PREFIX', 1, -100, 'M3F isolated root', 1, 1);"
mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${PROJECT_ID}, r.id FROM core_users u JOIN core_roles r ON r.code='PROJECT_ADMIN'
WHERE u.username='${ADMIN_USER}'
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);"
pass "隔离项目和安全测试目录已准备"

ADMIN_TOKEN="$(login)"
ADMIN_TOKEN="$(switch_project "${PROJECT_ID}")"
enable_body="{\"enabled\":true,\"allowedRelativeRoots\":[\"${UPLOAD_DIR}\"],\"allowedRoleCodes\":[\"PROJECT_ADMIN\"],\"allowedUserIds\":[${ADMIN_ID}],\"trialModeNotice\":\"M3F smoke only allows object-first-zone.\"}"
trial_response="$(put_json "/api/data-steward/projects/${PROJECT_ID}/nas/write-trial" "${enable_body}")"
assert_ok "${trial_response}"
assert_no_forbidden "trial response" "${trial_response}"
pass "管理员登录、切换项目并开启安全目录写入灰度"

readiness_response="$(get_json "/api/data-steward/storage-provider-readiness")"
assert_ok "${readiness_response}"
assert_no_forbidden "storage readiness" "${readiness_response}"
OBJECT_ENDPOINT_TYPE="$(json_expr "${readiness_response}" "data['data']['endpointType']")"
OBJECT_READINESS_STATUS="$(json_expr "${readiness_response}" "data['data']['readinessStatus']")"
if [[ -n "${OBJECT_ENDPOINT_TYPE}" && -n "${OBJECT_READINESS_STATUS}" ]]; then
  pass "对象存储 readiness 已识别：${OBJECT_ENDPOINT_TYPE}/${OBJECT_READINESS_STATUS}"
else
  fail "对象存储 readiness 缺少 endpointType/readinessStatus：${readiness_response}"
fi

echo ""
echo "--- 2. Upload file through legacy NAS-compatible endpoint ---"
upload_response="$(upload_file "${TMP_DIR}/${UPLOAD_FILE_NAME}")"
assert_ok "${upload_response}"
assert_no_forbidden "upload response" "${upload_response}"
upload_ok="$(json_expr "${upload_response}" "data['data']['storageStatus']=='OBJECT_STORED' and data['data']['storageProvider']=='OBJECT_STORAGE' and bool(data['data']['assetUuid']) and bool(data['data']['checksum'])")"
if [[ "${upload_ok}" == "true" ]]; then
  pass "上传响应显示 OBJECT_STORED，旧接口兼容但行为已对象存储优先"
else
  fail "上传响应未表达对象存储优先：${upload_response}"
fi
FILE_ID="$(json_expr "${upload_response}" "data['data']['fileId']")"
asset_uuid="$(json_expr "${upload_response}" "data['data']['assetUuid']")"
if [[ "${asset_uuid}" =~ ^[0-9a-fA-F-]{36}$ ]]; then
  pass "新上传文件生成稳定 assetUuid=${asset_uuid}"
else
  fail "新上传文件 assetUuid 异常：${asset_uuid}"
fi
if [[ ! -e "${NAS_ROOT}/${UPLOAD_DIR}/${UPLOAD_FILE_NAME}" ]]; then
  pass "新增文件本体未写入隔离 NAS 目录"
else
  fail "新增文件本体出现在 NAS 目录：${UPLOAD_DIR}/${UPLOAD_FILE_NAME}"
fi

echo ""
echo "--- 3. Verify storage-status, object version, and controlled file-access ---"
status_response="$(get_json "/api/data-steward/assets/files/${FILE_ID}/storage-status")"
assert_ok "${status_response}"
assert_no_forbidden "storage status" "${status_response}"
status_ok="$(json_expr "${status_response}" "data['data']['assetUuid']=='${asset_uuid}' and data['data']['storageState']=='OBJECT_STORED' and data['data']['objectStored'] == True")"
if [[ "${status_ok}" == "true" ]]; then
  pass "storage-status 返回 OBJECT_STORED 且 assetUuid 稳定"
else
  fail "storage-status 异常：${status_response}"
fi

object_version_count="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE file_id=${FILE_ID} AND active=1 AND deleted=0 AND storage_state='OBJECT_STORED';")"
if [[ "${object_version_count}" == "1" ]]; then
  pass "数据库存在且仅存在一个 active object version"
else
  fail "active object version 数量异常：${object_version_count}"
fi
OBJECT_ROWS="$(mysql_exec "SELECT so.bucket, so.object_key FROM data_storage_objects so JOIN data_file_object_versions fov ON fov.storage_object_id=so.id WHERE fov.file_id=${FILE_ID} AND fov.active=1 AND fov.deleted=0 AND so.deleted=0;" 2>/dev/null || true)"

ticket_response="$(post_json "/api/data-steward/assets/files/${FILE_ID}/access-tickets" '{"action":"DOWNLOAD"}')"
assert_ok "${ticket_response}"
assert_no_forbidden "download ticket" "${ticket_response}"
access_url="$(json_expr "${ticket_response}" "data['data']['accessUrl']")"
curl -fsS --connect-timeout 3 --max-time 45 "${BASE_URL}${access_url}" -o "${TMP_DIR}/downloaded.txt"
if cmp -s "${TMP_DIR}/${UPLOAD_FILE_NAME}" "${TMP_DIR}/downloaded.txt"; then
  pass "受控 file-access 可读取对象存储新增文件内容"
else
  fail "受控 file-access 读取内容与上传文件不一致"
fi

catalog_response="$(get_json "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&directoryPath=${UPLOAD_DIR}&directOnly=true&pageSize=20")"
assert_ok "${catalog_response}"
assert_no_forbidden "catalog direct files" "${catalog_response}"
catalog_ok="$(json_expr "${catalog_response}" "any(item['fileId']==int('${FILE_ID}') and item['storageProvider']=='OBJECT_STORAGE' for item in data['data']['items'])")"
if [[ "${catalog_ok}" == "true" ]]; then
  pass "文件管理目录列表可见新文件且显示对象存储业务状态"
else
  fail "目录列表未返回对象存储新文件：${catalog_response}"
fi

echo ""
echo "--- 4. Object storage unavailable is fail-closed ---"
if [[ "${OBJECT_ENDPOINT_TYPE}" == "LOCAL_DEV_MINIO" ]] && docker ps --format '{{.Names}}' | grep -qx "${MINIO_CONTAINER}"; then
  docker pause "${MINIO_CONTAINER}" >/dev/null
  MINIO_PAUSED=1
  set +e
  unavailable_response="$(upload_file "${TMP_DIR}/${UNAVAILABLE_FILE_NAME}")"
  unavailable_rc=$?
  set -e
  docker unpause "${MINIO_CONTAINER}" >/dev/null
  MINIO_PAUSED=0
  if [[ "${unavailable_rc}" -eq 0 && -n "${unavailable_response}" ]]; then
    assert_not_ok "${unavailable_response}"
    assert_no_forbidden "object unavailable response" "${unavailable_response}"
    if [[ ! -e "${NAS_ROOT}/${UPLOAD_DIR}/${UNAVAILABLE_FILE_NAME}" ]]; then
      pass "对象存储不可用时上传失败且未静默写入 NAS fallback"
    else
      fail "对象存储不可用时出现 NAS fallback 文件本体"
    fi
  else
    fail "对象存储不可用时未返回业务化 JSON 响应"
  fi
elif [[ "${OBJECT_ENDPOINT_TYPE}" == "NAS_SIDE_MINIO" ]]; then
  pass "NAS 侧 MinIO 环境下不暂停本机容器；已通过 ${OBJECT_ENDPOINT_TYPE}/${OBJECT_READINESS_STATUS} 和对象优先上传链路验证"
else
  pass "对象存储不可用场景跳过：当前 endpointType=${OBJECT_ENDPOINT_TYPE:-UNKNOWN}，不执行本机容器暂停模拟"
fi

echo ""
echo "=== Summary ==="
printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
