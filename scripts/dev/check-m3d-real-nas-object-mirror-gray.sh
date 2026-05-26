#!/usr/bin/env bash
# M3D: real NAS small-scope object mirror gray smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
MAX_FILE_SIZE_BYTES="${MAX_FILE_SIZE_BYTES:-10485760}"

PASS=0
FAIL=0
TOKEN=""
TASK_ID=""
RERUN_TASK_ID=""

SAMPLE_IDS=()
SAMPLE_UUIDS=()
SAMPLE_TYPES=()
SAMPLE_EXTS=()
SAMPLE_STATES=()
SAMPLE_PATHS=()
SAMPLE_STATS_BEFORE=()

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
    "sum": sum,
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
    r"\btoken\b",
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
  curl -sS --connect-timeout 3 --max-time 180 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
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

select_sample() {
  local sample_type="$1"
  local ext_filter="$2"
  local required="$3"
  local sql
  sql="
    SELECT f.id, f.asset_uuid, f.file_kind, f.size_bytes, f.storage_uri,
           IF(fov.file_id IS NULL, 'NAS_ONLY', 'OBJECT_STORED') AS storage_state
    FROM data_file_resources f
    LEFT JOIN (
        SELECT DISTINCT file_id
        FROM data_file_object_versions
        WHERE active = 1
          AND deleted = 0
          AND storage_state = 'OBJECT_STORED'
    ) fov ON fov.file_id = f.id
    WHERE f.project_id = ${PROJECT_ID}
      AND f.deleted = 0
      AND f.size_bytes > 0
      AND f.size_bytes <= ${MAX_FILE_SIZE_BYTES}
      AND f.storage_uri IS NOT NULL
      AND f.storage_uri <> ''
      AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) IN (${ext_filter})
    ORDER BY f.id
    LIMIT 80;
  "

  while IFS=$'\t' read -r file_id asset_uuid file_kind size_bytes storage_uri storage_state; do
    [[ -z "${file_id}" ]] && continue
    local nas_path
    nas_path="$(nas_path_from_uri "${storage_uri}")"
    if [[ -f "${nas_path}" && -r "${nas_path}" ]]; then
      SAMPLE_IDS+=("${file_id}")
      SAMPLE_UUIDS+=("${asset_uuid}")
      SAMPLE_TYPES+=("${sample_type}")
      SAMPLE_EXTS+=("${ext_filter}")
      SAMPLE_STATES+=("${storage_state}")
      SAMPLE_PATHS+=("${nas_path}")
      SAMPLE_STATS_BEFORE+=("$(file_stat_signature "${nas_path}")")
      pass "${sample_type} 小样本已选择：fileId=${file_id} assetUuid=${asset_uuid} state=${storage_state} size=${size_bytes}"
      return 0
    fi
  done < <(mysql_exec "${sql}" 2>/dev/null || true)

  if [[ "${required}" == "required" ]]; then
    fail "${sample_type} 未找到可读且不超过上限的真实业务文件"
    return 1
  fi
  pass "${sample_type} 跳过：SKIPPED_NO_ELIGIBLE_MODEL_SAMPLE"
  return 0
}

sample_id_csv() {
  local IFS=,
  echo "${SAMPLE_IDS[*]}"
}

assert_samples_not_empty() {
  if [[ "${#SAMPLE_IDS[@]}" -lt 2 ]]; then
    fail "真实业务样本不足，至少需要 PDF + DWG"
    return 1
  fi
}

echo "=== M3D: real NAS object mirror gray ==="

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
pass "管理员登录并切换 105 / project ${PROJECT_ID} 成功"

echo ""
echo "--- 2. Select explicit real NAS samples ---"
select_sample "PDF" "'pdf'" "required"
select_sample "DWG" "'dwg'" "required"
select_sample "MODEL" "'rvt','ifc','nwd','nwc','glb','gltf'" "optional"
assert_samples_not_empty
file_ids_csv="$(sample_id_csv)"
pass "灰度样本显式 fileIds 已确定：${file_ids_csv}"

echo ""
echo "--- 3. Create M3C migration task for real NAS samples ---"
create_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks" "{\"fileIds\":[${file_ids_csv}],\"targetProvider\":\"MINIO\"}")"
assert_ok "${create_response}"
assert_no_forbidden "m3d migration create response" "${create_response}"
TASK_ID="$(json_data_expr "${create_response}" "data['taskId']")"
success_count="$(json_data_expr "${create_response}" "data['successCount']")"
failure_count="$(json_data_expr "${create_response}" "data['failureCount']")"
skipped_count="$(json_data_expr "${create_response}" "data['skippedCount']")"
if [[ "$(json_data_expr "${create_response}" "data['taskStatus'] in ['COMPLETED', 'PARTIAL_FAILED'] and data['totalCount'] == int(${#SAMPLE_IDS[@]}) and all(row['assetUuid'] for row in data['rows'])")" == "true" ]]; then
  pass "真实 NAS 灰度迁移任务已创建：success=${success_count} skipped=${skipped_count} failure=${failure_count}"
else
  fail "真实 NAS 灰度迁移任务结果不符合预期"
fi
if [[ "${failure_count}" != "0" ]]; then
  fail "真实 NAS 灰度存在失败文件，请查看业务化失败原因"
fi

echo ""
echo "--- 4. Verify task detail, storage-status, and controlled file-access ---"
detail_response="$(api_get "/api/data-steward/storage-migration-tasks/${TASK_ID}")"
assert_ok "${detail_response}"
assert_no_forbidden "m3d migration detail response" "${detail_response}"
if [[ "$(json_data_expr "${detail_response}" "all(row['storageState'] == 'OBJECT_STORED' and row['resultCode'] in ['MIRRORED', 'ALREADY_STORED'] for row in data['rows'])")" == "true" ]]; then
  pass "任务详情行级结果均为 OBJECT_STORED，原因可解释"
else
  fail "任务详情行级状态不符合预期"
fi

checksum_ok=0
for file_id in "${SAMPLE_IDS[@]}"; do
  status_response="$(api_get "/api/data-steward/assets/files/${file_id}/storage-status")"
  assert_ok "${status_response}"
  assert_no_forbidden "m3d storage status ${file_id}" "${status_response}"
  if [[ "$(json_data_expr "${status_response}" "data['storageState'] == 'OBJECT_STORED' and data['objectStored'] is True")" == "true" ]]; then
    pass "fileId=${file_id} storage-status 为 OBJECT_STORED"
  else
    fail "fileId=${file_id} storage-status 未进入 OBJECT_STORED"
  fi
  if [[ "$(json_data_expr "${status_response}" "data['checksumAvailable'] is True")" == "true" ]]; then
    checksum_ok=$((checksum_ok + 1))
  fi

  ticket_response="$(api_post "/api/data-steward/assets/files/${file_id}/access-tickets" '{"action":"DOWNLOAD"}')"
  assert_ok "${ticket_response}"
  assert_no_forbidden "m3d access ticket ${file_id}" "${ticket_response}"
  access_url="$(json_data_expr "${ticket_response}" "data['accessUrl']")"
  download_probe="$(curl -sS --connect-timeout 3 --max-time 90 -o /dev/null -w '%{http_code}:%{size_download}' "${BASE_URL}${access_url}")"
  if [[ "${download_probe}" == 200:* && "${download_probe##*:}" -gt 0 ]]; then
    pass "fileId=${file_id} 受控 file-access 可读取对象镜像"
  else
    fail "fileId=${file_id} 受控 file-access 读取失败"
  fi
done
pass "checksum 覆盖率：${checksum_ok}/${#SAMPLE_IDS[@]}"

echo ""
echo "--- 5. Verify idempotency and original NAS files are unchanged ---"
rerun_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks" "{\"fileIds\":[${file_ids_csv}],\"targetProvider\":\"MINIO\"}")"
assert_ok "${rerun_response}"
assert_no_forbidden "m3d migration rerun response" "${rerun_response}"
RERUN_TASK_ID="$(json_data_expr "${rerun_response}" "data['taskId']")"
active_version_count="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE file_id IN (${file_ids_csv}) AND active = 1 AND deleted = 0;" 2>/dev/null)"
if [[ "$(json_data_expr "${rerun_response}" "data['taskStatus'] == 'COMPLETED' and data['skippedCount'] == int(${#SAMPLE_IDS[@]}) and all(row['resultCode'] == 'ALREADY_STORED' for row in data['rows'])")" == "true" ]] \
  && [[ "${active_version_count}" == "${#SAMPLE_IDS[@]}" ]]; then
  pass "重复灰度迁移被幂等跳过，active 对象版本未重复污染"
else
  fail "重复灰度迁移幂等结果不符合预期"
fi

for index in "${!SAMPLE_IDS[@]}"; do
  path="${SAMPLE_PATHS[$index]}"
  before="${SAMPLE_STATS_BEFORE[$index]}"
  if [[ -f "${path}" && -r "${path}" && "$(file_stat_signature "${path}")" == "${before}" ]]; then
    pass "fileId=${SAMPLE_IDS[$index]} NAS 原文件仍存在且 size/mtime 未变化"
  else
    fail "fileId=${SAMPLE_IDS[$index]} NAS 原文件状态发生变化"
  fi
done

echo ""
echo "--- 6. Gray summary and git tracking ---"
coverage="$(printf '%s\n' "${SAMPLE_TYPES[@]}" | sort | uniq | paste -sd ',' -)"
printf '  [INFO] grayTaskId=%s rerunTaskId=%s success=%s skipped=%s failure=%s coverage=%s checksum=%s/%s forbiddenScan=PASS\n' \
  "${TASK_ID}" "${RERUN_TASK_ID}" "${success_count}" "${skipped_count}" "${failure_count}" "${coverage}" "${checksum_ok}" "${#SAMPLE_IDS[@]}"
if git ls-files --error-unmatch scripts/dev/check-m3d-real-nas-object-mirror-gray.sh >/dev/null 2>&1; then
  pass "M3D 专项脚本已纳入 Git 跟踪"
else
  fail "M3D 专项脚本未纳入 Git 跟踪"
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
