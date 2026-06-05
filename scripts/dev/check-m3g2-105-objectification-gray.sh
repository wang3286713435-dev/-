#!/usr/bin/env bash
# M3G-2: 105 project small-batch historical NAS file objectification gray.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
MAX_GRAY_FILES="${MAX_GRAY_FILES:-3}"
MAX_FILE_SIZE_BYTES="${MAX_FILE_SIZE_BYTES:-10485760}"
MAX_TOTAL_BYTES="${MAX_TOTAL_BYTES:-31457280}"

PASS=0
FAIL=0
TOKEN=""
TASK_ID=""
RERUN_TASK_ID=""
UNOBJECTIFIED_FILE_ID=""

SAMPLE_IDS=()
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
    r"/tmp(?:/|$)",
    r"/private(?:/|$)",
    r"\b(?:10|127)\.\d+\.\d+\.\d+\b",
    r"\b192\.168\.\d+\.\d+\b",
    r"\b172\.(?:1[6-9]|2\d|3[0-1])\.\d+\.\d+\b",
    r"\blocalhost\b",
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
    r"\baccess[_-]?key\b",
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
  curl -sS --connect-timeout 3 --max-time 45 -X GET "${BASE_URL}${url}" \
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

storage_uri_for_file() {
  local file_id="$1"
  mysql_exec "SELECT storage_uri FROM data_file_resources WHERE id = ${file_id} AND project_id = ${PROJECT_ID} AND deleted = 0 LIMIT 1;" 2>/dev/null | head -n 1
}

extract_dry_run_candidate_ids() {
  local response="$1"
  RESPONSE="${response}" MAX_SIZE="${MAX_FILE_SIZE_BYTES}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
max_size = int(os.environ["MAX_SIZE"])
allowed_reasons = {"ELIGIBLE_DRY_RUN", "MISSING_CHECKSUM"}
for item in payload.get("data", {}).get("sampleItems", []):
    if item.get("storageStatus") != "NAS_ONLY":
        continue
    if item.get("reason") not in allowed_reasons:
        continue
    if int(item.get("sizeBytes") or 0) > max_size:
        continue
    print(item.get("fileId"))
PY
}

remember_sample_if_readable() {
  local file_id="$1"
  local storage_uri
  storage_uri="$(storage_uri_for_file "${file_id}")"
  [[ -z "${storage_uri}" ]] && return 1
  local nas_path
  nas_path="$(nas_path_from_uri "${storage_uri}")"
  [[ -f "${nas_path}" && -r "${nas_path}" ]] || return 1
  SAMPLE_IDS+=("${file_id}")
  SAMPLE_STATS_BEFORE+=("$(file_stat_signature "${nas_path}")")
}

select_unobjectified_file() {
  local exclude_csv="$1"
  local sql
  sql="
    SELECT f.id
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
      AND fov.file_id IS NULL
      AND f.id NOT IN (${exclude_csv})
      AND f.size_bytes > 0
      AND f.size_bytes <= ${MAX_FILE_SIZE_BYTES}
      AND f.storage_uri IS NOT NULL
      AND f.storage_uri <> ''
    ORDER BY f.id
    LIMIT 100;
  "
  while IFS=$'\t' read -r file_id; do
    [[ -z "${file_id}" ]] && continue
    local storage_uri
    storage_uri="$(storage_uri_for_file "${file_id}")"
    [[ -z "${storage_uri}" ]] && continue
    local nas_path
    nas_path="$(nas_path_from_uri "${storage_uri}")"
    if [[ -f "${nas_path}" && -r "${nas_path}" ]]; then
      UNOBJECTIFIED_FILE_ID="${file_id}"
      return 0
    fi
  done < <(mysql_exec "${sql}" 2>/dev/null || true)
  return 1
}

sample_id_csv() {
  local IFS=,
  echo "${SAMPLE_IDS[*]}"
}

assert_access_downloads() {
  local file_id="$1"
  local label="$2"
  local ticket_response
  ticket_response="$(api_post "/api/data-steward/assets/files/${file_id}/access-tickets" '{"action":"DOWNLOAD"}')"
  assert_ok "${ticket_response}"
  assert_no_forbidden "${label} access ticket" "${ticket_response}"
  local access_url
  access_url="$(json_expr "${ticket_response}" "data['data']['accessUrl']")"
  local probe
  probe="$(curl -sS --connect-timeout 3 --max-time 120 -o /dev/null -w '%{http_code}:%{size_download}' "${BASE_URL}${access_url}")"
  if [[ "${probe}" == 200:* && "${probe##*:}" -gt 0 ]]; then
    pass "${label} 可通过受控 file-access 读取"
  else
    fail "${label} 受控 file-access 读取失败"
  fi
}

echo "=== M3G-2: 105 objectification gray ==="

echo ""
echo "--- 1. Login and switch to project ${PROJECT_ID} ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch" '{}')"
assert_ok "${switch_response}"
TOKEN="$(json_expr "${switch_response}" "data['data']['accessToken']")"
pass "管理员登录并切换 105 项目成功"

echo ""
echo "--- 2. Readiness must be NAS_SIDE_MINIO / READY ---"
readiness_response="$(api_get "/api/data-steward/storage-provider-readiness")"
assert_ok "${readiness_response}"
assert_no_forbidden "readiness" "${readiness_response}"
endpoint_type="$(json_expr "${readiness_response}" "data['data']['endpointType']")"
readiness_status="$(json_expr "${readiness_response}" "data['data']['readinessStatus']")"
if [[ "${endpoint_type}" == "NAS_SIDE_MINIO" && "${readiness_status}" == "READY" ]]; then
  pass "对象存储 readiness 为 ${endpoint_type}/${readiness_status}"
else
  fail "对象存储未处于 NAS_SIDE_MINIO/READY：${endpoint_type}/${readiness_status}"
fi

echo ""
echo "--- 3. Project inventory and dry-run sample selection ---"
before_inventory_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-inventory")"
assert_ok "${before_inventory_response}"
assert_no_forbidden "inventory before" "${before_inventory_response}"
object_before="$(json_expr "${before_inventory_response}" "int(data['data']['objectStoredFiles'])")"
nas_before="$(json_expr "${before_inventory_response}" "int(data['data']['nasOnlyFiles'])")"
coverage_before="$(json_expr "${before_inventory_response}" "data['data']['objectificationCoverageRate']")"
if [[ "$(json_expr "${before_inventory_response}" "data['data']['projectCode'] == '105' and int(data['data']['projectId']) == int('${PROJECT_ID}')")" == "true" ]]; then
  pass "105 对象化盘点可查：OBJECT_STORED=${object_before} NAS_ONLY=${nas_before}"
else
  fail "105 对象化盘点项目身份异常"
fi

dry_run_payload="{\"storageState\":\"NAS_ONLY\",\"checksumState\":\"ANY\",\"extensions\":[\"pdf\",\"png\",\"jpg\",\"jpeg\",\"webp\",\"docx\",\"xlsx\",\"pptx\",\"dwg\"],\"maxSizeBytes\":${MAX_FILE_SIZE_BYTES},\"maxTotalBytes\":${MAX_TOTAL_BYTES},\"limit\":50}"
dry_run_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-plans:dry-run" "${dry_run_payload}")"
assert_ok "${dry_run_response}"
assert_no_forbidden "dry-run" "${dry_run_response}"
if [[ "$(json_expr "${dry_run_response}" "data['data']['dryRun'] is True and data['data']['migrationStarted'] is False and int(data['data']['selectedFileCount']) >= 0")" == "true" ]]; then
  pass "105 dry-run 已生成，未启动迁移任务"
else
  fail "105 dry-run 结果异常"
fi

while IFS= read -r candidate_id; do
  [[ -z "${candidate_id}" ]] && continue
  remember_sample_if_readable "${candidate_id}" || true
  [[ "${#SAMPLE_IDS[@]}" -ge "${MAX_GRAY_FILES}" ]] && break
done < <(extract_dry_run_candidate_ids "${dry_run_response}")

if [[ "${#SAMPLE_IDS[@]}" -gt 0 ]]; then
  pass "从 dry-run 计划选择 ${#SAMPLE_IDS[@]} 个可读小样本"
else
  fail "dry-run 没有可执行的 NAS_ONLY 小样本，未扩大到其他项目"
fi

file_ids_csv="$(sample_id_csv)"
select_unobjectified_file "${file_ids_csv}" || true
if [[ -n "${UNOBJECTIFIED_FILE_ID}" ]]; then
  pass "已选择未对象化对照文件 fileId=${UNOBJECTIFIED_FILE_ID}"
else
  fail "未找到可读 NAS_ONLY 对照文件"
fi

echo ""
echo "--- 4. Create small-batch objectification task ---"
create_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks" "{\"fileIds\":[${file_ids_csv}],\"targetProvider\":\"MINIO\"}")"
assert_ok "${create_response}"
assert_no_forbidden "migration create" "${create_response}"
TASK_ID="$(json_expr "${create_response}" "data['data']['taskId']")"
success_count="$(json_expr "${create_response}" "int(data['data']['successCount'])")"
failure_count="$(json_expr "${create_response}" "int(data['data']['failureCount'])")"
skipped_count="$(json_expr "${create_response}" "int(data['data']['skippedCount'])")"
if [[ "$(json_expr "${create_response}" "data['data']['taskStatus'] in ['COMPLETED', 'PARTIAL_FAILED'] and int(data['data']['totalCount']) == int('${#SAMPLE_IDS[@]}') and all(row['assetUuid'] for row in data['data']['rows'])")" == "true" ]]; then
  pass "105 小批对象化任务完成：success=${success_count} skipped=${skipped_count} failure=${failure_count}"
else
  fail "105 小批对象化任务结果不符合预期"
fi
if [[ "${failure_count}" != "0" ]]; then
  fail "105 小批对象化存在失败文件，请查看业务化失败原因"
fi

echo ""
echo "--- 5. Verify OBJECT_STORED and controlled file-access ---"
detail_response="$(api_get "/api/data-steward/storage-migration-tasks/${TASK_ID}")"
assert_ok "${detail_response}"
assert_no_forbidden "migration detail" "${detail_response}"
if [[ "$(json_expr "${detail_response}" "all(row['storageState'] == 'OBJECT_STORED' and row['resultCode'] in ['MIRRORED', 'ALREADY_STORED'] for row in data['data']['rows'])")" == "true" ]]; then
  pass "任务详情行级结果均为 OBJECT_STORED 且原因可解释"
else
  fail "任务详情行级状态不符合预期"
fi

for file_id in "${SAMPLE_IDS[@]}"; do
  status_response="$(api_get "/api/data-steward/assets/files/${file_id}/storage-status")"
  assert_ok "${status_response}"
  assert_no_forbidden "storage status ${file_id}" "${status_response}"
  if [[ "$(json_expr "${status_response}" "data['data']['storageState'] == 'OBJECT_STORED' and data['data']['objectStored'] is True")" == "true" ]]; then
    pass "fileId=${file_id} storage-status 为 OBJECT_STORED"
  else
    fail "fileId=${file_id} storage-status 未进入 OBJECT_STORED"
  fi
  assert_access_downloads "${file_id}" "fileId=${file_id} 对象化文件"
done

echo ""
echo "--- 6. Verify NAS_ONLY file remains available ---"
nas_only_status_response="$(api_get "/api/data-steward/assets/files/${UNOBJECTIFIED_FILE_ID}/storage-status")"
assert_ok "${nas_only_status_response}"
assert_no_forbidden "nas-only storage status" "${nas_only_status_response}"
if [[ "$(json_expr "${nas_only_status_response}" "data['data']['storageState'] == 'NAS_ONLY' and data['data']['objectStored'] is False")" == "true" ]]; then
  pass "未对象化对照文件仍显示 NAS_ONLY"
else
  fail "未对象化对照文件状态异常"
fi
assert_access_downloads "${UNOBJECTIFIED_FILE_ID}" "NAS_ONLY 对照文件"

echo ""
echo "--- 7. Verify coverage, idempotency, and NAS originals ---"
after_inventory_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-inventory")"
assert_ok "${after_inventory_response}"
assert_no_forbidden "inventory after" "${after_inventory_response}"
object_after="$(json_expr "${after_inventory_response}" "int(data['data']['objectStoredFiles'])")"
nas_after="$(json_expr "${after_inventory_response}" "int(data['data']['nasOnlyFiles'])")"
coverage_after="$(json_expr "${after_inventory_response}" "data['data']['objectificationCoverageRate']")"
if [[ "${success_count}" -gt 0 && "${object_after}" -ge $((object_before + success_count)) ]]; then
  pass "105 覆盖率已变化：OBJECT_STORED ${object_before} -> ${object_after}，覆盖率 ${coverage_before}% -> ${coverage_after}%"
elif [[ "${skipped_count}" -eq "${#SAMPLE_IDS[@]}" && "${object_after}" -ge "${object_before}" ]]; then
  pass "105 样本已对象化，重复执行按幂等跳过：OBJECT_STORED=${object_after}"
else
  fail "105 对象化覆盖率变化不符合预期：before=${object_before} after=${object_after}"
fi
if [[ "${nas_after}" -le "${nas_before}" ]]; then
  pass "105 NAS_ONLY 数量未异常增加：${nas_before} -> ${nas_after}"
else
  fail "105 NAS_ONLY 数量异常增加：${nas_before} -> ${nas_after}"
fi

rerun_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks" "{\"fileIds\":[${file_ids_csv}],\"targetProvider\":\"MINIO\"}")"
assert_ok "${rerun_response}"
assert_no_forbidden "migration rerun" "${rerun_response}"
RERUN_TASK_ID="$(json_expr "${rerun_response}" "data['data']['taskId']")"
active_version_count="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE file_id IN (${file_ids_csv}) AND active = 1 AND deleted = 0;" 2>/dev/null)"
if [[ "$(json_expr "${rerun_response}" "data['data']['taskStatus'] == 'COMPLETED' and int(data['data']['skippedCount']) == int('${#SAMPLE_IDS[@]}') and all(row['resultCode'] == 'ALREADY_STORED' for row in data['data']['rows'])")" == "true" ]] \
  && [[ "${active_version_count}" == "${#SAMPLE_IDS[@]}" ]]; then
  pass "重复灰度任务幂等跳过，active object version 未重复污染"
else
  fail "重复灰度任务幂等结果不符合预期"
fi

for index in "${!SAMPLE_IDS[@]}"; do
  storage_uri="$(storage_uri_for_file "${SAMPLE_IDS[$index]}")"
  nas_path="$(nas_path_from_uri "${storage_uri}")"
  if [[ -f "${nas_path}" && -r "${nas_path}" && "$(file_stat_signature "${nas_path}")" == "${SAMPLE_STATS_BEFORE[$index]}" ]]; then
    pass "fileId=${SAMPLE_IDS[$index]} NAS 原文件仍存在且 size/mtime 未变化"
  else
    fail "fileId=${SAMPLE_IDS[$index]} NAS 原文件状态发生变化"
  fi
done

echo ""
echo "--- 8. Script tracking and summary ---"
printf '  [INFO] taskId=%s rerunTaskId=%s success=%s skipped=%s failure=%s objectStored=%s->%s nasOnly=%s->%s\n' \
  "${TASK_ID}" "${RERUN_TASK_ID}" "${success_count}" "${skipped_count}" "${failure_count}" \
  "${object_before}" "${object_after}" "${nas_before}" "${nas_after}"
if git ls-files --error-unmatch scripts/dev/check-m3g2-105-objectification-gray.sh >/dev/null 2>&1; then
  pass "M3G-2 专项脚本已纳入 Git 跟踪"
else
  fail "M3G-2 专项脚本尚未纳入 Git 跟踪"
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
