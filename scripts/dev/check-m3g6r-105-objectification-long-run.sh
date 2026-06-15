#!/usr/bin/env bash
# M3G-6R: 105 objectification long-run control smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
BATCH_FILE_LIMIT="${BATCH_FILE_LIMIT:-3}"
BATCH_BYTES_LIMIT="${BATCH_BYTES_LIMIT:-52428800}"
MAX_FILE_SIZE_BYTES="${MAX_FILE_SIZE_BYTES:-10485760}"

PASS=0
FAIL=0
TOKEN=""
TRACKED_IDS=()
TRACKED_STATS_BEFORE=()

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
    r"/var(?:/|$)",
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
  curl -sS --connect-timeout 3 --max-time 60 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="${2:-{}}"
  curl -sS --connect-timeout 3 --max-time 300 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

long_run_body() {
  local confirmed="${1:-true}"
  local batches="${2:-2}"
  local batch_files="${3:-${BATCH_FILE_LIMIT}}"
  CONFIRMED="${confirmed}" BATCHES="${batches}" BATCH_FILES="${batch_files}" \
  BATCH_BYTES="${BATCH_BYTES_LIMIT}" MAX_FILE_SIZE="${MAX_FILE_SIZE_BYTES}" python3 - <<'PY'
import json
import os
body = {
    "batchFileLimit": int(os.environ["BATCH_FILES"]),
    "batchBytesLimit": int(os.environ["BATCH_BYTES"]),
    "maxFileSizeBytes": int(os.environ["MAX_FILE_SIZE"]),
    "maxContinuousBatches": int(os.environ["BATCHES"]),
    "continueOnFailure": True,
    "confirmed": os.environ["CONFIRMED"].lower() == "true",
    "targetProvider": "MINIO",
}
print(json.dumps(body, ensure_ascii=False))
PY
}

plan_body() {
  BATCH_FILES="${BATCH_FILE_LIMIT}" BATCH_BYTES="${BATCH_BYTES_LIMIT}" python3 - <<'PY'
import json
import os
print(json.dumps({
    "checksumState": "ANY",
    "batchFileLimit": int(os.environ["BATCH_FILES"]),
    "batchBytesLimit": int(os.environ["BATCH_BYTES"]),
}, ensure_ascii=False))
PY
}

full_plan() {
  api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-full-plan" "$(plan_body)"
}

batch_ids_from_plan() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
items = payload.get("data", {}).get("nextBatchItems", [])
print(",".join(str(item["fileId"]) for item in items if item.get("fileId")))
PY
}

json_array_from_csv() {
  local csv="$1"
  CSV="${csv}" python3 - <<'PY'
import json
import os
items = [int(item) for item in os.environ["CSV"].split(",") if item.strip()]
print(json.dumps(items))
PY
}

execute_body() {
  local ids_csv="$1"
  FILE_IDS="$(json_array_from_csv "${ids_csv}")" PROJECT_ID="${PROJECT_ID}" BATCH_BYTES="${BATCH_BYTES_LIMIT}" python3 - <<'PY'
import json
import os
file_ids = json.loads(os.environ["FILE_IDS"])
body = {
    "projectIds": [int(os.environ["PROJECT_ID"])],
    "fileIds": file_ids,
    "realProjectsOnly": True,
    "storageState": "NAS_ONLY",
    "checksumState": "ANY",
    "limit": len(file_ids),
    "maxTotalBytes": int(os.environ["BATCH_BYTES"]),
    "maxFilesPerProject": len(file_ids),
    "maxBytesPerProject": int(os.environ["BATCH_BYTES"]),
    "concurrencyLimit": 1,
    "confirmed": True,
    "targetProvider": "MINIO",
}
print(json.dumps(body, ensure_ascii=False))
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

storage_uri_for_file() {
  local file_id="$1"
  mysql_exec "SELECT storage_uri FROM data_file_resources WHERE id = ${file_id} AND deleted = 0 LIMIT 1;" 2>/dev/null | head -n 1
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

remember_original_stats() {
  local ids_csv="$1"
  IFS=',' read -r -a ids <<< "${ids_csv}"
  for file_id in "${ids[@]}"; do
    [[ -z "${file_id}" ]] && continue
    local storage_uri
    storage_uri="$(storage_uri_for_file "${file_id}")"
    local nas_path
    nas_path="$(nas_path_from_uri "${storage_uri}")"
    if [[ -f "${nas_path}" && -r "${nas_path}" ]]; then
      TRACKED_IDS+=("${file_id}")
      TRACKED_STATS_BEFORE+=("$(file_stat_signature "${nas_path}")")
    else
      fail "fileId=${file_id} NAS 原文件不可读，不能作为本轮样本"
    fi
  done
}

verify_original_stats() {
  local index=0
  for file_id in "${TRACKED_IDS[@]}"; do
    local storage_uri
    storage_uri="$(storage_uri_for_file "${file_id}")"
    local nas_path
    nas_path="$(nas_path_from_uri "${storage_uri}")"
    if [[ -f "${nas_path}" && "$(file_stat_signature "${nas_path}")" == "${TRACKED_STATS_BEFORE[$index]}" ]]; then
      pass "fileId=${file_id} NAS 原文件仍存在且 size/mtime 未变化"
    else
      fail "fileId=${file_id} NAS 原文件状态变化"
    fi
    index=$((index + 1))
  done
}

active_object_version_anomalies() {
  local ids_csv="$1"
  mysql_exec "
    SELECT COUNT(1)
    FROM (
      SELECT file_id, COUNT(1) AS active_count
      FROM data_file_object_versions
      WHERE active = 1
        AND deleted = 0
        AND file_id IN (${ids_csv})
      GROUP BY file_id
      HAVING active_count <> 1
    ) t;
  " 2>/dev/null | head -n 1
}

object_stored_sample_file_id() {
  mysql_exec "
    SELECT f.id
    FROM data_file_resources f
    JOIN data_file_object_versions fov ON fov.file_id = f.id
      AND fov.active = 1
      AND fov.deleted = 0
      AND fov.storage_state = 'OBJECT_STORED'
    WHERE f.project_id = ${PROJECT_ID}
      AND f.deleted = 0
    ORDER BY f.id DESC
    LIMIT 1;
  " 2>/dev/null | head -n 1
}

assert_access_downloads() {
  local file_id="$1"
  local ticket_response
  ticket_response="$(api_post "/api/data-steward/assets/files/${file_id}/access-tickets" '{"action":"DOWNLOAD"}')"
  assert_ok "${ticket_response}"
  assert_no_forbidden "download ticket ${file_id}" "${ticket_response}"
  local access_url
  access_url="$(json_expr "${ticket_response}" "data['data']['accessUrl']")"
  local probe
  probe="$(curl -sS --connect-timeout 3 --max-time 120 -o /dev/null -w '%{http_code}:%{size_download}' "${BASE_URL}${access_url}")"
  if [[ "${probe}" == 200:* && "${probe##*:}" -gt 0 ]]; then
    pass "fileId=${file_id} 可通过受控 file-access 读取"
  else
    fail "fileId=${file_id} 受控 file-access 读取失败"
  fi
}

echo "=== M3G-6R: 105 objectification long-run control ==="

echo ""
echo "--- 1. Login and readiness ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
pass "管理员登录成功"

readiness_response="$(api_get "/api/data-steward/storage-provider-readiness")"
assert_ok "${readiness_response}"
assert_no_forbidden "readiness" "${readiness_response}"
if [[ "$(json_expr "${readiness_response}" "data['data']['endpointType'] == 'NAS_SIDE_MINIO' and data['data']['readinessStatus'] == 'READY' and data['data']['readable'] == True and data['data']['writable'] == True")" == "true" ]]; then
  pass "NAS 侧 MinIO READY 且可读写"
else
  fail "NAS 侧 MinIO 尚未达到 M3G-6R 执行条件：${readiness_response}"
fi

echo ""
echo "--- 2. Long-run status and safeguards ---"
status_before="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run")"
assert_ok "${status_before}"
assert_no_forbidden "long-run status before" "${status_before}"
if [[ "$(json_expr "${status_before}" "data['data']['runState'] in ['IDLE','RUNNING','PAUSED','COMPLETED','PARTIAL_WITH_FAILURES','FAILED'] and data['data']['projectId'] == int('${PROJECT_ID}')")" == "true" ]]; then
  pass "105 长跑状态接口可查"
else
  fail "105 长跑状态接口字段异常：${status_before}"
fi
before_object_stored="$(json_expr "${status_before}" "int(data['data']['objectStoredCount'])")"
before_total="$(json_expr "${status_before}" "int(data['data']['totalFileCount'])")"
before_eligible="$(json_expr "${status_before}" "int(data['data']['eligibleRemainingCount'])")"

unconfirmed_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run:start" "$(long_run_body false 1)")"
assert_not_ok "${unconfirmed_response}"
assert_no_forbidden "long-run unconfirmed" "${unconfirmed_response}"
pass "未 confirmed 的长跑开始被拒绝"

over_limit_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run:start" "$(long_run_body true 6 16)")"
assert_not_ok "${over_limit_response}"
assert_no_forbidden "long-run over-limit" "${over_limit_response}"
pass "后端长跑硬上限生效"

if [[ "${before_total}" -gt 0 && "${before_object_stored}" -ge "${before_total}" && "${before_eligible}" -eq 0 ]]; then
  pass "105 已全量对象化，长跑推进回归进入完成态兼容验证"
  sample_file_id="$(object_stored_sample_file_id)"
  if [[ -n "${sample_file_id}" ]]; then
    remember_original_stats "${sample_file_id}"
    status_response="$(api_get "/api/data-steward/assets/files/${sample_file_id}/storage-status")"
    assert_ok "${status_response}"
    assert_no_forbidden "storage status ${sample_file_id}" "${status_response}"
    if [[ "$(json_expr "${status_response}" "data['data']['storageState'] == 'OBJECT_STORED' and data['data']['objectStored'] == True")" == "true" ]]; then
      pass "已对象化样本 storage-status 为 OBJECT_STORED"
    else
      fail "已对象化样本 storage-status 异常：${status_response}"
    fi
    assert_access_downloads "${sample_file_id}"
    if [[ "$(active_object_version_anomalies "${sample_file_id}")" == "0" ]]; then
      pass "完成态样本未产生重复 active object version"
    else
      fail "完成态样本 active object version 异常"
    fi
    verify_original_stats
  else
    fail "完成态未找到 OBJECT_STORED 样本文件"
  fi
  echo ""
  echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
  if [[ "${FAIL}" -ne 0 ]]; then
    exit 1
  fi
  exit 0
fi

plan_before="$(full_plan)"
assert_ok "${plan_before}"
assert_no_forbidden "full plan before long-run" "${plan_before}"
batch_ids="$(batch_ids_from_plan "${plan_before}")"
if [[ -n "${batch_ids}" ]]; then
  remember_original_stats "${batch_ids}"
  pass "记录下一批 NAS 原文件状态"
else
  fail "105 当前没有可执行下一批，无法验证长跑推进"
fi

echo ""
echo "--- 3. Start two controlled batches ---"
start_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run:start" "$(long_run_body true 2)")"
assert_ok "${start_response}"
assert_no_forbidden "long-run start" "${start_response}"
processed_batches="$(json_expr "${start_response}" "int(data['data']['processedBatchCount'])")"
created_count="$(json_expr "${start_response}" "int(data['data']['createdCount'])")"
after_start_object_stored="$(json_expr "${start_response}" "int(data['data']['objectStoredCount'])")"
if [[ "${processed_batches}" -ge 2 && "${created_count}" -gt 0 && "${after_start_object_stored}" -gt "${before_object_stored}" ]]; then
  pass "长跑连续推进多批且覆盖率上升：${before_object_stored}->${after_start_object_stored}"
else
  fail "长跑启动结果异常：${start_response}"
fi

echo ""
echo "--- 4. Pause, reject paused start, and resume ---"
pause_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run:pause" "{}")"
assert_ok "${pause_response}"
assert_no_forbidden "long-run pause" "${pause_response}"
if [[ "$(json_expr "${pause_response}" "data['data']['runState'] == 'PAUSED' and data['data']['paused'] == True")" == "true" ]]; then
  pass "长跑暂停状态可查"
else
  fail "长跑暂停状态异常：${pause_response}"
fi

paused_start_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run:start" "$(long_run_body true 1)")"
assert_not_ok "${paused_start_response}"
assert_no_forbidden "long-run paused start" "${paused_start_response}"
pass "暂停后不会通过开始接口继续推进"

resume_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run:resume" "$(long_run_body true 1)")"
assert_ok "${resume_response}"
assert_no_forbidden "long-run resume" "${resume_response}"
after_resume_object_stored="$(json_expr "${resume_response}" "int(data['data']['objectStoredCount'])")"
if [[ "${after_resume_object_stored}" -gt "${after_start_object_stored}" ]]; then
  pass "继续后从剩余可执行项推进：${after_start_object_stored}->${after_resume_object_stored}"
else
  fail "继续后覆盖率未继续上升：${resume_response}"
fi

echo ""
echo "--- 5. Retry, idempotency, governance, and file access ---"
retry_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run:retry-failures" "$(long_run_body true 1)")"
assert_ok "${retry_response}"
assert_no_forbidden "long-run retry failures" "${retry_response}"
pass "失败项重试接口可调用并返回治理口径"

rerun_response="$(api_post "/api/data-steward/storage-objectification-plans:execute" "$(execute_body "${batch_ids}")")"
assert_ok "${rerun_response}"
assert_no_forbidden "long-run idempotency rerun" "${rerun_response}"
if [[ "$(json_expr "${rerun_response}" "int(data['data']['skippedCount']) == len([x for x in '${batch_ids}'.split(',') if x]) and int(data['data']['failedCount']) == 0")" == "true" ]]; then
  pass "重复执行已对象化批次按幂等策略跳过"
else
  fail "重复执行未幂等跳过：${rerun_response}"
fi

if [[ -n "${batch_ids}" && "$(active_object_version_anomalies "${batch_ids}")" == "0" ]]; then
  pass "长跑未产生重复 active object version"
else
  fail "active object version 存在重复或缺失"
fi

status_after="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run")"
assert_ok "${status_after}"
assert_no_forbidden "long-run status after" "${status_after}"
governance_count="$(json_expr "${status_after}" "int(data['data']['governanceItemCount'])")"
governance_sample_count="$(json_expr "${status_after}" "len(data['data']['governanceItems'])")"
if [[ "${governance_count}" -gt 0 || "${governance_sample_count}" -gt 0 ]]; then
  pass "失败/超大/不可读治理清单可解释：governance=${governance_count}"
else
  fail "治理清单为空，无法解释剩余不可执行项：${status_after}"
fi

first_file_id="${TRACKED_IDS[0]}"
status_response="$(api_get "/api/data-steward/assets/files/${first_file_id}/storage-status")"
assert_ok "${status_response}"
assert_no_forbidden "storage status ${first_file_id}" "${status_response}"
if [[ "$(json_expr "${status_response}" "data['data']['storageState'] == 'OBJECT_STORED' and data['data']['objectStored'] == True")" == "true" ]]; then
  pass "已对象化文件 storage-status 为 OBJECT_STORED"
else
  fail "已对象化文件 storage-status 异常：${status_response}"
fi
assert_access_downloads "${first_file_id}"

verify_original_stats

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
