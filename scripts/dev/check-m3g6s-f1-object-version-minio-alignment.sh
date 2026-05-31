#!/usr/bin/env bash
# M3G-6S-F1: align 105 historical active object versions with current NAS-side MinIO.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
REPAIR_BATCH_FILE_LIMIT="${REPAIR_BATCH_FILE_LIMIT:-50}"
REPAIR_BATCH_BYTES_LIMIT="${REPAIR_BATCH_BYTES_LIMIT:-1073741824}"
REPAIR_MAX_FILE_SIZE_BYTES="${REPAIR_MAX_FILE_SIZE_BYTES:-524288000}"
REPAIR_MAX_LOOPS="${REPAIR_MAX_LOOPS:-120}"

PASS=0
FAIL=0
TOKEN=""
SAMPLE_FILE_ID=""
SAMPLE_STAT_BEFORE=""

pass() {
  echo "  [PASS] $1"
  PASS=$((PASS + 1))
}

fail() {
  echo "  [FAIL] $1"
  FAIL=$((FAIL + 1))
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql --default-character-set=utf8mb4 -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
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

assert_data() {
  local label="$1"
  local response="$2"
  local expr="$3"
  if RESPONSE="${response}" python3 - "${expr}" <<'PY'
import json
import os
import sys
payload = json.loads(os.environ["RESPONSE"])
data = payload.get("data")
assert eval(sys.argv[1], {}, {"data": data, "payload": payload}), payload
PY
  then
    pass "${label}"
  else
    fail "${label}"
  fi
}

assert_no_forbidden() {
  local label="$1"
  local payload="$2"
  if PAYLOAD="${payload}" python3 - <<'PY'
import os
import re
payload = os.environ["PAYLOAD"]
patterns = [
    r"/Volumes/",
    r"smb://",
    r"nas://",
    r"storage_uri",
    r"storage_path",
    r"storageUri",
    r"storagePath",
    r"object_key",
    r"objectKey",
    r"object key",
    r'"bucket"\s*:',
    r"\bbucket\b",
    r"raw db row",
    r"raw row",
    r"\bselect\s+.+\s+from\b",
    r"\binsert\s+into\b",
    r"\bupdate\s+.+\s+set\b",
    r"\bdelete\s+from\b",
    r"token[^A-Za-z0-9_]",
    r"secret",
    r"password",
]
for pattern in patterns:
    if re.search(pattern, payload, flags=re.IGNORECASE | re.DOTALL):
        raise AssertionError(pattern)
PY
  then
    pass "${label}"
  else
    fail "${label}"
  fi
}

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" python3 - "${expr}" <<'PY'
import json
import os
import sys
payload = json.loads(os.environ["RESPONSE"])
data = payload.get("data")
value = eval(sys.argv[1], {}, {"data": data, "payload": payload})
if isinstance(value, (dict, list)):
    print(json.dumps(value, ensure_ascii=False))
elif value is None:
    print("")
else:
    print(value)
PY
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 600 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 1800 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

login() {
  local login_response switch_response
  login_response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${login_response}"
  TOKEN="$(json_data_expr "${login_response}" "data['accessToken']")"
  switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch" '{}')"
  assert_ok "${switch_response}"
  TOKEN="$(json_data_expr "${switch_response}" "data['accessToken']")"
}

integrity() {
  api_get "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-integrity?sampleLimit=5000"
}

repair_body() {
  python3 - <<PY
import json
print(json.dumps({
    "confirmed": True,
    "batchFileLimit": int("${REPAIR_BATCH_FILE_LIMIT}"),
    "batchBytesLimit": int("${REPAIR_BATCH_BYTES_LIMIT}"),
    "maxFileSizeBytes": int("${REPAIR_MAX_FILE_SIZE_BYTES}"),
    "targetProvider": "MINIO",
}, ensure_ascii=False))
PY
}

nas_path_from_uri() {
  local uri="$1"
  local path="${uri#nas://}"
  if [[ "${path}" != /* ]]; then
    path="/${path}"
  fi
  printf '%s' "${path}"
}

file_stat_signature() {
  local path="$1"
  PATH_VALUE="${path}" python3 - <<'PY'
import os
path = os.environ["PATH_VALUE"]
st = os.stat(path)
print(f"{st.st_size}:{int(st.st_mtime)}")
PY
}

remember_sample_stat() {
  local file_id="$1"
  local uri path
  uri="$(mysql_exec "SELECT storage_uri FROM data_file_resources WHERE id = ${file_id} AND project_id = ${PROJECT_ID} AND deleted = 0 LIMIT 1;" 2>/dev/null | head -n 1)"
  path="$(nas_path_from_uri "${uri}")"
  if [[ -f "${path}" ]]; then
    SAMPLE_STAT_BEFORE="$(file_stat_signature "${path}")"
  fi
}

verify_sample_stat() {
  local file_id="$1"
  local uri path after
  [[ -z "${SAMPLE_STAT_BEFORE}" ]] && return 0
  uri="$(mysql_exec "SELECT storage_uri FROM data_file_resources WHERE id = ${file_id} AND project_id = ${PROJECT_ID} AND deleted = 0 LIMIT 1;" 2>/dev/null | head -n 1)"
  path="$(nas_path_from_uri "${uri}")"
  after="$(file_stat_signature "${path}")"
  if [[ "${after}" == "${SAMPLE_STAT_BEFORE}" ]]; then
    pass "NAS 原文件抽样 size/mtime 未变化"
  else
    fail "NAS 原文件抽样 size/mtime 发生变化"
  fi
}

active_object_version_anomalies() {
  mysql_exec "
    SELECT COUNT(1)
    FROM (
      SELECT file_id, COUNT(1) AS active_count
      FROM data_file_object_versions
      WHERE active = 1
        AND deleted = 0
        AND file_id IN (
          SELECT id FROM data_file_resources WHERE project_id = ${PROJECT_ID} AND deleted = 0
        )
      GROUP BY file_id
      HAVING active_count <> 1
    ) t;
  " 2>/dev/null | head -n 1
}

assert_file_access_downloads() {
  local file_id="$1"
  local ticket_response access_url probe
  ticket_response="$(api_post "/api/data-steward/assets/files/${file_id}/access-tickets" '{"action":"DOWNLOAD"}')"
  assert_ok "${ticket_response}"
  assert_no_forbidden "download ticket response has no forbidden fields" "${ticket_response}"
  access_url="$(json_data_expr "${ticket_response}" "data['accessUrl']")"
  probe="$(curl -sS --connect-timeout 3 --max-time 180 -o /dev/null -w '%{http_code}:%{size_download}' "${BASE_URL}${access_url}")"
  if [[ "${probe}" == 200:* && "${probe##*:}" -gt 0 ]]; then
    pass "受控 file-access 可读取修复后的对象化文件"
  else
    fail "受控 file-access 读取修复后的对象化文件失败"
  fi
}

echo "=== M3G-6S-F1: object version / NAS-side MinIO alignment ==="

echo ""
echo "--- 1. Login and initial integrity ---"
login
pass "管理员登录并切换到 105"

initial_response="$(integrity)"
assert_ok "${initial_response}"
assert_no_forbidden "initial integrity has no forbidden fields" "${initial_response}"
assert_data "integrity endpoint is available" "${initial_response}" "data['projectId'] == ${PROJECT_ID} and data['totalObjectStoredCount'] <= 2928 and data['sampleCheckedCount'] == 2928"

initial_missing="$(json_data_expr "${initial_response}" "data['governanceItemCount']")"
initial_verified="$(json_data_expr "${initial_response}" "data['verifiedObjectCount']")"
current_missing="${initial_missing}"
current_verified="${initial_verified}"
SAMPLE_FILE_ID="$(json_data_expr "${initial_response}" "(data.get('sampleIssues') or [{}])[0].get('fileId')")"
if [[ -z "${SAMPLE_FILE_ID}" ]]; then
  SAMPLE_FILE_ID="$(mysql_exec "SELECT f.id FROM data_file_resources f JOIN data_file_object_versions fov ON fov.file_id=f.id AND fov.active=1 AND fov.deleted=0 AND fov.storage_state='OBJECT_STORED' WHERE f.project_id=${PROJECT_ID} AND f.deleted=0 ORDER BY f.id DESC LIMIT 1;" 2>/dev/null | head -n 1)"
fi
remember_sample_stat "${SAMPLE_FILE_ID}"
printf '  initial verified=%s governance=%s sampleFileId=%s\n' "${initial_verified}" "${initial_missing}" "${SAMPLE_FILE_ID}"

echo ""
echo "--- 2. Repair guardrail ---"
repair_without_confirm="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-integrity:repair" '{"confirmed":false}')"
assert_not_ok "${repair_without_confirm}"
assert_no_forbidden "unconfirmed repair rejection has no forbidden fields" "${repair_without_confirm}"
pass "未确认修复被拒绝"

echo ""
echo "--- 3. Controlled repair loop ---"
loop=0
while true; do
  printf '  loop=%s verified=%s governance=%s\n' "${loop}" "${current_verified}" "${current_missing}"
  if [[ "${current_missing}" == "0" ]]; then
    break
  fi
  if [[ "${loop}" -ge "${REPAIR_MAX_LOOPS}" ]]; then
    fail "修复循环超过上限后仍有治理项"
    break
  fi
  login
  repair_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-integrity:repair" "$(repair_body)")"
  assert_ok "${repair_response}"
  assert_no_forbidden "repair response has no forbidden fields" "${repair_response}"
  repaired_count="$(json_data_expr "${repair_response}" "data['repairedCount']")"
  failed_count="$(json_data_expr "${repair_response}" "data['failedCount']")"
  current_missing="$(json_data_expr "${repair_response}" "data['governanceItemCount']")"
  current_verified="$(json_data_expr "${repair_response}" "data['verifiedObjectCount']")"
  printf '    repaired=%s failed=%s\n' "${repaired_count}" "${failed_count}"
  if [[ "${repaired_count}" == "0" && "${current_missing}" != "0" ]]; then
    fail "本轮没有修复任何对象但仍存在治理项"
    break
  fi
  if [[ "${failed_count}" != "0" ]]; then
    echo "    warning: 本轮存在失败项，将继续后续小批重试；最终完整性仍会严格校验"
  fi
  loop=$((loop + 1))
done

echo ""
echo "--- 4. Final integrity, access and source protection ---"
final_response="$(integrity)"
assert_ok "${final_response}"
assert_no_forbidden "final integrity has no forbidden fields" "${final_response}"
assert_data "all active object versions are readable in current NAS-side MinIO" "${final_response}" "data['totalObjectStoredCount'] == 2928 and data['verifiedObjectCount'] == 2928 and data['governanceItemCount'] == 0 and data['fullyVerified'] is True"

status_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run")"
assert_ok "${status_response}"
assert_no_forbidden "long-run status has no forbidden fields" "${status_response}"
assert_data "105 objectification coverage remains complete" "${status_response}" "data['totalFileCount'] == 2928 and data['objectStoredCount'] == 2928 and float(data['objectificationCoverageRate']) == 100.0"

if [[ "$(active_object_version_anomalies)" == "0" ]]; then
  pass "active object version 未重复污染"
else
  fail "active object version 存在重复或缺失"
fi

assert_file_access_downloads "${SAMPLE_FILE_ID}"
verify_sample_stat "${SAMPLE_FILE_ID}"

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
