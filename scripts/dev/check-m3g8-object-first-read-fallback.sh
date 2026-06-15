#!/usr/bin/env bash
# M3G-8: object-first reads and explicit NAS fallback boundary.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"

PASS=0
FAIL=0
TOKEN=""
TMP_DIR="$(mktemp -d /tmp/delivery-m3g8.XXXXXX)"
SYNTHETIC_FILE_IDS=()
SYNTHETIC_OBJECT_IDS=()

cleanup() {
  set +e
  mysql_exec "UPDATE data_file_object_versions fov
JOIN data_file_resources f ON f.id = fov.file_id
SET fov.deleted = 1, fov.delete_token = fov.id
WHERE f.project_id = ${PROJECT_ID}
  AND f.original_name LIKE 'm3g8-%';" >/dev/null 2>&1
  mysql_exec "UPDATE data_storage_objects so
JOIN data_file_object_versions fov ON fov.storage_object_id = so.id
JOIN data_file_resources f ON f.id = fov.file_id
SET so.deleted = 1, so.delete_token = so.id
WHERE f.project_id = ${PROJECT_ID}
  AND f.original_name LIKE 'm3g8-%';" >/dev/null 2>&1
  for file_id in "${SYNTHETIC_FILE_IDS[@]:-}"; do
    [[ -n "${file_id}" ]] || continue
    curl -sS -X DELETE "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/file-resources/${file_id}" \
      -H "Authorization: Bearer ${TOKEN}" >/dev/null || true
    mysql_exec "UPDATE data_file_resources SET deleted = 1, delete_token = id WHERE id = ${file_id};" >/dev/null || true
  done
  for object_id in "${SYNTHETIC_OBJECT_IDS[@]:-}"; do
    [[ -n "${object_id}" ]] || continue
    mysql_exec "UPDATE data_file_object_versions SET deleted = 1, delete_token = id WHERE storage_object_id = ${object_id};
UPDATE data_storage_objects SET deleted = 1, delete_token = id WHERE id = ${object_id};" >/dev/null || true
  done
  mysql_exec "UPDATE data_file_resources SET deleted = 1, delete_token = id
WHERE project_id = ${PROJECT_ID}
  AND original_name LIKE 'm3g8-%';" >/dev/null 2>&1
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

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
    "float": float,
    "int": int,
    "str": str,
    "sum": sum,
    "list": list,
    "set": set,
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
    r"/private(?:/|$)",
    r"smb://",
    r"nas://",
    r"storage_path",
    r"storage_uri",
    r"storagePath",
    r"storageUri",
    r"object_key",
    r"objectKey",
    r'"bucket"\s*:',
    r"\bbucket\b",
    r"secret",
    r"token\s*[:=]",
    r"password",
    r"\bselect\s+.+\s+from\b",
]
for pattern in patterns:
    if re.search(pattern, payload, re.IGNORECASE | re.DOTALL):
        raise SystemExit(f"{label} leaked forbidden pattern: {pattern}")
PY
}

login() {
  local response
  response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${response}"
  TOKEN="$(json_expr "${response}" "data['data']['accessToken']")"
}

switch_project() {
  local response
  response="$(curl -sS -X POST "${BASE_URL}/api/core/projects/${PROJECT_ID}:switch" \
    -H "Authorization: Bearer ${TOKEN}")"
  assert_ok "${response}"
  TOKEN="$(json_expr "${response}" "data['data']['accessToken']")"
}

api_get() {
  local path="$1"
  curl -sS "${BASE_URL}${path}" -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local path="$1"
  local payload="$2"
  curl -sS -X POST "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${payload}"
}

create_file_resource() {
  local name="$1"
  local path="$2"
  local size="$3"
  local response
  response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-resources" \
    "{\"originalName\":\"${name}\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":${size},\"storageUri\":\"${path}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
  assert_ok "${response}"
  local file_id
  file_id="$(json_expr "${response}" "data['data']['id']")"
  SYNTHETIC_FILE_IDS+=("${file_id}")
  printf '%s' "${file_id}"
}

create_ticket() {
  local file_id="$1"
  local action="${2:-DOWNLOAD}"
  api_post "/api/data-steward/assets/files/${file_id}/access-tickets" "{\"action\":\"${action}\"}"
}

object_stored_sample_file_ids() {
  mysql_exec "SELECT f.id
FROM data_file_resources f
JOIN data_file_object_versions fov ON fov.file_id = f.id AND fov.active = 1 AND fov.deleted = 0 AND fov.storage_state = 'OBJECT_STORED'
JOIN data_storage_objects so ON so.id = fov.storage_object_id AND so.deleted = 0
WHERE f.project_id = ${PROJECT_ID}
  AND f.deleted = 0
  AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) IN ('pdf','png','jpg','jpeg','webp','gif','bmp','svg')
  AND COALESCE(fov.size_bytes, so.size_bytes, f.size_bytes, 0) BETWEEN 1 AND 2097152
ORDER BY f.id
LIMIT 20;"
}

object_bucket_for_file() {
  local file_id="$1"
  mysql_exec "SELECT so.bucket
FROM data_file_object_versions fov
JOIN data_storage_objects so ON so.id = fov.storage_object_id AND so.deleted = 0
WHERE fov.file_id = ${file_id}
  AND fov.active = 1
  AND fov.deleted = 0
  AND fov.storage_state = 'OBJECT_STORED'
LIMIT 1;"
}

create_unreadable_object_version() {
  local file_id="$1"
  local bucket="$2"
  local key="smoke/m3g8/missing-$(date +%s)-${file_id}.bin"
  mysql_exec "INSERT INTO data_storage_objects (
    provider, bucket, object_key, checksum, content_type, size_bytes,
    source_provider, source_uri_digest, source_path_digest, storage_state, migration_status, last_verified_at
  ) VALUES (
    'MINIO', '${bucket}', '${key}', 'm3g8-missing-checksum', 'application/pdf', 32,
    'NAS', 'm3g8-source-uri', 'm3g8-source-path', 'OBJECT_STORED', 'COMPLETED', NOW()
  );
SET @object_id = LAST_INSERT_ID();
UPDATE data_file_object_versions SET active = 0 WHERE file_id = ${file_id} AND active = 1 AND deleted = 0;
INSERT INTO data_file_object_versions (
  file_id, storage_object_id, version_no, active, storage_state, migration_status,
  checksum, content_type, size_bytes, last_verified_at
) VALUES (
  ${file_id}, @object_id, 'M3G8-SMOKE', 1, 'OBJECT_STORED', 'COMPLETED',
  'm3g8-missing-checksum', 'application/pdf', 32, NOW()
);
SELECT @object_id;"
}

open_ticket_and_headers() {
  local access_url="$1"
  local headers_file="$2"
  curl -sS -D "${headers_file}" -o /dev/null --max-time 60 "${BASE_URL}${access_url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

prepare_readable_object_sample() {
  local sample_path="${TMP_DIR}/m3g8-object-readable.pdf"
  printf 'M3G-8 synthetic object-first sample. No production NAS mutation.\n' >"${sample_path}"
  local sample_size
  sample_size="$(wc -c <"${sample_path}" | tr -d ' ')"
  local sample_file_id
  sample_file_id="$(create_file_resource "m3g8-object-readable-$(date +%s).pdf" "${sample_path}" "${sample_size}")"

  local task_response
  task_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks" \
    "{\"fileIds\":[${sample_file_id}],\"targetProvider\":\"MINIO\"}")"
  assert_no_forbidden "m3g8 synthetic objectification task" "${task_response}"
  if [[ "$(json_expr "${task_response}" "data.get('code') == 'OK' and int(data['data']['successCount']) == 1 and data['data']['taskStatus'] == 'COMPLETED'")" != "true" ]]; then
    fail "无法通过受控对象化任务准备 M3G-8 临时可读样本"
    return
  fi

  local sample_ticket
  sample_ticket="$(create_ticket "${sample_file_id}" "DOWNLOAD")"
  assert_no_forbidden "m3g8 synthetic object ticket" "${sample_ticket}"
  if [[ "$(json_expr "${sample_ticket}" "data.get('code') == 'OK' and data['data']['storageStatus'] == 'OBJECT_STORED' and data['data']['readSource'] == 'OBJECT_STORAGE' and data['data']['fallbackUsed'] is False and data['data']['objectReadable'] is True")" == "true" ]]; then
    object_file_id="${sample_file_id}"
    object_ticket="${sample_ticket}"
    pass "历史 OBJECT_STORED 样本不可读时，已通过受控对象化任务准备临时可读对象样本"
  else
    fail "M3G-8 临时对象样本未满足对象优先读取口径"
  fi
}

printf '== M3G-8 object-first read / fallback smoke ==\n'

login
switch_project
pass "管理员登录并切换项目 ${PROJECT_ID}"

read_policy="$(api_get "/api/data-steward/storage-read-policy")"
assert_ok "${read_policy}"
assert_no_forbidden "storage-read-policy" "${read_policy}"
if [[ "$(json_expr "${read_policy}" "data['data']['objectFirstEnabled'] is True and data['data']['nasFallbackEnabled'] in [True, False]")" == "true" ]]; then
  pass "读取策略接口返回对象优先和 fallback 显式状态"
else
  fail "读取策略接口没有返回对象优先/fallback 状态"
fi

object_file_id=""
object_ticket=""
object_candidate_ids="$(object_stored_sample_file_ids || true)"
if [[ -z "${object_candidate_ids}" ]]; then
  prepare_readable_object_sample
else
  while IFS= read -r candidate_file_id; do
    [[ -n "${candidate_file_id}" ]] || continue
    candidate_ticket="$(create_ticket "${candidate_file_id}" "DOWNLOAD")"
    assert_no_forbidden "object-ticket-candidate" "${candidate_ticket}"
    candidate_code="$(json_expr "${candidate_ticket}" "data.get('code')")"
    if [[ "${candidate_code}" == "OK" ]]; then
      object_file_id="${candidate_file_id}"
      object_ticket="${candidate_ticket}"
      break
    fi
  done <<<"${object_candidate_ids}"
  if [[ -z "${object_file_id}" ]]; then
    prepare_readable_object_sample
  elif [[ "$(json_expr "${object_ticket}" "data['data']['storageStatus'] == 'OBJECT_STORED' and data['data']['readSource'] == 'OBJECT_STORAGE' and data['data']['fallbackUsed'] is False and data['data']['objectReadable'] is True")" == "true" ]]; then
    pass "OBJECT_STORED 文件访问票据明确标记 OBJECT_STORAGE 且未 fallback"
  else
    fail "OBJECT_STORED 文件访问票据读取口径不正确"
  fi
  if [[ -n "${object_file_id}" ]]; then
    object_access_url="$(json_expr "${object_ticket}" "data['data']['accessUrl']")"
    object_headers="${TMP_DIR}/object.headers"
    open_ticket_and_headers "${object_access_url}" "${object_headers}"
    assert_no_forbidden "object-access-headers" "$(cat "${object_headers}")"
    if grep -qi '^X-Delivery-Read-Source: OBJECT_STORAGE' "${object_headers}" \
      && grep -qi '^X-Delivery-Fallback-Used: false' "${object_headers}"; then
      pass "对象存储读取响应头明确标记 readSource/fallback"
    else
      fail "对象存储读取响应头缺少 readSource/fallback 标记"
    fi
  fi
fi

nas_file_path="${TMP_DIR}/m3g8-nas-only.pdf"
printf 'M3G-8 synthetic NAS-only file. No production NAS mutation.\n' >"${nas_file_path}"
nas_file_size="$(wc -c <"${nas_file_path}" | tr -d ' ')"
nas_file_id="$(create_file_resource "m3g8-nas-only-$(date +%s).pdf" "${nas_file_path}" "${nas_file_size}")"
nas_ticket="$(create_ticket "${nas_file_id}" "DOWNLOAD")"
assert_ok "${nas_ticket}"
assert_no_forbidden "nas-ticket" "${nas_ticket}"
if [[ "$(json_expr "${nas_ticket}" "data['data']['storageStatus'] == 'NAS_ONLY' and data['data']['readSource'] == 'LEGACY_NAS' and data['data']['fallbackUsed'] is False")" == "true" ]]; then
  pass "NAS_ONLY 文件仍可受控读取且明确标记 LEGACY_NAS"
else
  fail "NAS_ONLY 文件读取口径不正确"
fi

if [[ -n "${object_file_id}" ]]; then
  object_bucket="$(object_bucket_for_file "${object_file_id}")"
  bad_file_path="${TMP_DIR}/m3g8-unreadable-source.pdf"
  printf 'M3G-8 unreadable object source file. Object copy intentionally absent.\n' >"${bad_file_path}"
  bad_file_size="$(wc -c <"${bad_file_path}" | tr -d ' ')"
  bad_file_id="$(create_file_resource "m3g8-object-unreadable-$(date +%s).pdf" "${bad_file_path}" "${bad_file_size}")"
  bad_object_id="$(create_unreadable_object_version "${bad_file_id}" "${object_bucket}" | tail -n 1)"
  SYNTHETIC_OBJECT_IDS+=("${bad_object_id}")
  bad_ticket="$(create_ticket "${bad_file_id}" "DOWNLOAD")"
  assert_not_ok "${bad_ticket}"
  assert_no_forbidden "object-unreadable-ticket" "${bad_ticket}"
  if [[ "$(json_expr "${bad_ticket}" "data.get('code') == 'ASSET_OBJECT_NOT_READABLE'")" == "true" ]]; then
    pass "对象副本不可读时返回 Missing/异常语义，不静默回退 NAS"
  else
    fail "对象副本不可读时错误码不符合 M3G-8 边界"
  fi
fi

precheck="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/export-precheck?viewType=DOCUMENT&targetType=SECTION")"
assert_ok "${precheck}"
assert_no_forbidden "delivery-export-precheck" "${precheck}"
if [[ "$(json_expr "${precheck}" "all(('storageStatus' in row and 'readSource' in row) for row in data['data']['rows'])")" == "true" ]]; then
  pass "交付包导出预检查返回 storageStatus/readSource"
else
  fail "交付包导出预检查未返回读取口径"
fi

if [[ "${FAIL}" -ne 0 ]]; then
  printf 'M3G-8 smoke finished: PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}" >&2
  exit 1
fi

printf 'M3G-8 smoke finished: PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
