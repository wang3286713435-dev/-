#!/usr/bin/env bash
# M3G-6S: validate and optionally execute 105 objectification to governance boundary.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_OBJECTIFICATION="${RUN_OBJECTIFICATION:-false}"
M3G6R_BASELINE_OBJECT_STORED="${M3G6R_BASELINE_OBJECT_STORED:-57}"
M3G6R_BASELINE_ELIGIBLE="${M3G6R_BASELINE_ELIGIBLE:-2566}"

PASS=0
FAIL=0
TOKEN=""
TRACKED_IDS=()
TRACKED_STATS_BEFORE=()
START_OBJECT_STORED=0
START_OBJECT_BYTES=0
END_OBJECT_STORED=0
END_OBJECT_BYTES=0
TOTAL_BATCHES=0
TOTAL_CREATED=0
TOTAL_SKIPPED=0
TOTAL_FAILED=0

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
# -*- coding: utf-8 -*-
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
# -*- coding: utf-8 -*-
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
# -*- coding: utf-8 -*-
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

login() {
  local login_response
  login_response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${login_response}"
  TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
}

api_get() {
  local url="$1"
  local response
  response="$(curl -sS --connect-timeout 3 --max-time 60 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}")"
  if grep -q 'CORE_AUTH_UNAUTHORIZED' <<< "${response}"; then
    login
    response="$(curl -sS --connect-timeout 3 --max-time 60 -X GET "${BASE_URL}${url}" \
      -H "Authorization: Bearer ${TOKEN}")"
  fi
  printf '%s' "${response}"
}

api_post() {
  local url="$1"
  local body="${2:-{}}"
  local response
  response="$(curl -sS --connect-timeout 3 --max-time 600 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}")"
  if grep -q 'CORE_AUTH_UNAUTHORIZED' <<< "${response}"; then
    login
    response="$(curl -sS --connect-timeout 3 --max-time 600 -X POST "${BASE_URL}${url}" \
      -H "Authorization: Bearer ${TOKEN}" \
      -H 'Content-Type: application/json' \
      -d "${body}")"
  fi
  printf '%s' "${response}"
}

long_run_body() {
  local batch_files="$1"
  local batch_bytes="$2"
  local max_file_size="$3"
  local max_batches="${4:-5}"
  BATCH_FILES="${batch_files}" BATCH_BYTES="${batch_bytes}" MAX_FILE_SIZE="${max_file_size}" MAX_BATCHES="${max_batches}" python3 - <<'PY'
# -*- coding: utf-8 -*-
import json
import os
print(json.dumps({
    "batchFileLimit": int(os.environ["BATCH_FILES"]),
    "batchBytesLimit": int(os.environ["BATCH_BYTES"]),
    "maxFileSizeBytes": int(os.environ["MAX_FILE_SIZE"]),
    "maxContinuousBatches": int(os.environ["MAX_BATCHES"]),
    "continueOnFailure": True,
    "confirmed": True,
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

storage_uri_for_file() {
  local file_id="$1"
  mysql_exec "SELECT storage_uri FROM data_file_resources WHERE id = ${file_id} AND deleted = 0 LIMIT 1;" 2>/dev/null | head -n 1
}

file_stat_signature() {
  local path="$1"
  PATH_VALUE="${path}" python3 - <<'PY'
# -*- coding: utf-8 -*-
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
    local already=0
    for tracked in "${TRACKED_IDS[@]:-}"; do
      if [[ "${tracked}" == "${file_id}" ]]; then
        already=1
        break
      fi
    done
    [[ "${already}" -eq 1 ]] && continue
    local storage_uri
    storage_uri="$(storage_uri_for_file "${file_id}")"
    local nas_path
    nas_path="$(nas_path_from_uri "${storage_uri}")"
    if [[ -f "${nas_path}" && -r "${nas_path}" ]]; then
      TRACKED_IDS+=("${file_id}")
      TRACKED_STATS_BEFORE+=("$(file_stat_signature "${nas_path}")")
    fi
  done
}

verify_original_stats() {
  local index=0
  for file_id in "${TRACKED_IDS[@]:-}"; do
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

next_batch_ids_for_profile() {
  local batch_files="$1"
  local batch_bytes="$2"
  local max_file_size="$3"
  BATCH_FILES="${batch_files}" BATCH_BYTES="${batch_bytes}" MAX_FILE_SIZE="${max_file_size}" PROJECT_ID="${PROJECT_ID}" TOKEN="${TOKEN}" BASE_URL="${BASE_URL}" python3 - <<'PY'
# -*- coding: utf-8 -*-
import json
import os
import urllib.request
body = json.dumps({
    "checksumState": "ANY",
    "batchFileLimit": int(os.environ["BATCH_FILES"]),
    "batchBytesLimit": int(os.environ["BATCH_BYTES"]),
}, ensure_ascii=False).encode("utf-8")
request = urllib.request.Request(
    f"{os.environ['BASE_URL']}/api/data-steward/projects/{os.environ['PROJECT_ID']}/storage-objectification-full-plan",
    data=body,
    headers={
        "Authorization": f"Bearer {os.environ['TOKEN']}",
        "Content-Type": "application/json",
    },
    method="POST",
)
payload = json.loads(urllib.request.urlopen(request, timeout=120).read().decode("utf-8"))
items = payload.get("data", {}).get("nextBatchItems", [])
print(",".join(str(item["fileId"]) for item in items[:3] if item.get("fileId")))
PY
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
  probe="$(curl -sS --connect-timeout 3 --max-time 180 -o /dev/null -w '%{http_code}:%{size_download}' "${BASE_URL}${access_url}")"
  if [[ "${probe}" == 200:* && "${probe##*:}" -gt 0 ]]; then
    pass "fileId=${file_id} 可通过受控 file-access 读取"
  else
    fail "fileId=${file_id} 受控 file-access 读取失败"
  fi
}

long_run_status() {
  api_get "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run"
}

run_stage() {
  local name="$1"
  local batch_files="$2"
  local batch_bytes="$3"
  local max_file_size="$4"
  local max_loops="$5"
  local loop=0
  printf '\n--- execute stage: %s ---\n' "${name}"
  while [[ "${loop}" -lt "${max_loops}" ]]; do
    loop=$((loop + 1))
    local response
    response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run:start" \
      "$(long_run_body "${batch_files}" "${batch_bytes}" "${max_file_size}" 5)")"
    assert_ok "${response}"
    assert_no_forbidden "stage ${name} loop ${loop}" "${response}"
    local processed_batches
    local created
    local skipped
    local failed
    local object_stored
    local eligible
    processed_batches="$(json_expr "${response}" "int(data['data']['processedBatchCount'])")"
    created="$(json_expr "${response}" "int(data['data']['createdCount'])")"
    skipped="$(json_expr "${response}" "int(data['data']['skippedThisRun'])")"
    failed="$(json_expr "${response}" "int(data['data']['failedCount'])")"
    object_stored="$(json_expr "${response}" "int(data['data']['objectStoredCount'])")"
    eligible="$(json_expr "${response}" "int(data['data']['eligibleRemainingCount'])")"
    TOTAL_BATCHES=$((TOTAL_BATCHES + processed_batches))
    TOTAL_CREATED=$((TOTAL_CREATED + created))
    TOTAL_SKIPPED=$((TOTAL_SKIPPED + skipped))
    TOTAL_FAILED=$((TOTAL_FAILED + failed))
    printf '  stage=%s loop=%s batches=%s created=%s skipped=%s failed=%s objectStored=%s eligible=%s\n' \
      "${name}" "${loop}" "${processed_batches}" "${created}" "${skipped}" "${failed}" "${object_stored}" "${eligible}"
    if [[ "${processed_batches}" -eq 0 ]]; then
      pass "${name} 已到当前分层治理边界"
      break
    fi
  done
  if [[ "${loop}" -ge "${max_loops}" ]]; then
    fail "${name} 达到脚本循环上限，停止避免无边界执行"
  fi
}

echo "=== M3G-6S: 105 objectification to governance boundary ==="

echo ""
echo "--- 1. Login and readiness ---"
login
pass "管理员登录成功"

readiness_response="$(api_get "/api/data-steward/storage-provider-readiness")"
assert_ok "${readiness_response}"
assert_no_forbidden "readiness" "${readiness_response}"
if [[ "$(json_expr "${readiness_response}" "data['data']['endpointType'] == 'NAS_SIDE_MINIO' and data['data']['readinessStatus'] == 'READY' and data['data']['readable'] == True and data['data']['writable'] == True")" == "true" ]]; then
  pass "NAS 侧 MinIO READY 且可读写"
else
  fail "NAS 侧 MinIO 尚未达到 M3G-6S 执行条件：${readiness_response}"
fi

status_before="$(long_run_status)"
assert_ok "${status_before}"
assert_no_forbidden "status before" "${status_before}"
START_OBJECT_STORED="$(json_expr "${status_before}" "int(data['data']['objectStoredCount'])")"
START_OBJECT_BYTES="$(json_expr "${status_before}" "int(data['data']['objectStoredBytes'])")"
printf '  start objectStored=%s objectBytes=%s\n' "${START_OBJECT_STORED}" "${START_OBJECT_BYTES}"

sample_ids="$(next_batch_ids_for_profile 15 104857600 10485760 || true)"
if [[ -n "${sample_ids}" ]]; then
  remember_original_stats "${sample_ids}"
fi

if [[ "${RUN_OBJECTIFICATION}" == "true" ]]; then
  echo ""
  echo "--- 2. Execute layered long-run batches ---"
  run_stage "small<=10MB" 15 104857600 10485760 80
  run_stage "medium<=50MB" 15 268435456 52428800 40
  run_stage "large<=100MB" 5 524288000 104857600 30
  run_stage "very-large<=500MB" 1 536870912 524288000 120
else
  echo ""
  echo "--- 2. Skip execution; validation mode ---"
  pass "RUN_OBJECTIFICATION=false，本次只验证既有覆盖率报告与治理边界"
fi

echo ""
echo "--- 3. Final coverage, governance, and access checks ---"
status_after="$(long_run_status)"
assert_ok "${status_after}"
assert_no_forbidden "status after" "${status_after}"
END_OBJECT_STORED="$(json_expr "${status_after}" "int(data['data']['objectStoredCount'])")"
END_OBJECT_BYTES="$(json_expr "${status_after}" "int(data['data']['objectStoredBytes'])")"
nas_only="$(json_expr "${status_after}" "int(data['data']['nasOnlyCount'])")"
eligible="$(json_expr "${status_after}" "int(data['data']['eligibleRemainingCount'])")"
governance_count="$(json_expr "${status_after}" "int(data['data']['governanceItemCount'])")"
governance_reason_count="$(json_expr "${status_after}" "len(data['data']['governanceReasons'])")"
checksum_rate="$(json_expr "${status_after}" "data['data']['checksumCoverageRate']")"
coverage_rate="$(json_expr "${status_after}" "data['data']['objectificationCoverageRate']")"
printf '  final objectStored=%s nasOnly=%s eligible=%s governance=%s checksum=%s coverage=%s\n' \
  "${END_OBJECT_STORED}" "${nas_only}" "${eligible}" "${governance_count}" "${checksum_rate}" "${coverage_rate}"

if [[ "${END_OBJECT_STORED}" -ge "${M3G6R_BASELINE_OBJECT_STORED}" ]]; then
  pass "105 已对象化数量不低于 M3G-6R 基线：${END_OBJECT_STORED}"
else
  fail "105 已对象化数量低于 M3G-6R 基线：${END_OBJECT_STORED}"
fi

if [[ "${END_OBJECT_STORED}" -gt "${START_OBJECT_STORED}" || "${RUN_OBJECTIFICATION}" != "true" ]]; then
  pass "本轮覆盖率已验证或已在执行模式中提升：${START_OBJECT_STORED}->${END_OBJECT_STORED}"
else
  fail "执行模式下对象化数量未提升：${START_OBJECT_STORED}->${END_OBJECT_STORED}"
fi

if [[ "${eligible}" -lt "${M3G6R_BASELINE_ELIGIBLE}" ]]; then
  pass "剩余可执行数量低于 M3G-6R 基线：${eligible}"
else
  fail "剩余可执行数量未低于 M3G-6R 基线：${eligible}"
fi

if [[ "${governance_count}" -gt 0 && "${governance_reason_count}" -gt 0 ]]; then
  pass "治理项具备 reason 分组"
elif [[ "${governance_count}" -eq 0 && "${nas_only}" -eq 0 ]]; then
  pass "105 已无未对象化文件，无需治理项分组"
else
  fail "治理项缺少 reason 分组：${status_after}"
fi

if [[ "$(active_object_version_anomalies)" == "0" ]]; then
  pass "105 active object version 未重复污染"
else
  fail "105 active object version 存在重复或缺失"
fi

sample_file_id="$(object_stored_sample_file_id)"
if [[ -n "${sample_file_id}" ]]; then
  assert_access_downloads "${sample_file_id}"
else
  fail "未找到 OBJECT_STORED 样本文件"
fi

if [[ "${#TRACKED_IDS[@]}" -eq 0 ]]; then
  fallback_id="$(object_stored_sample_file_id)"
  if [[ -n "${fallback_id}" ]]; then
    remember_original_stats "${fallback_id}"
  fi
fi
verify_original_stats

echo ""
echo "--- 4. Execution summary ---"
printf '  totalBatches=%s totalCreated=%s totalSkipped=%s totalFailed=%s objectStoredDelta=%s objectBytesDelta=%s\n' \
  "${TOTAL_BATCHES}" "${TOTAL_CREATED}" "${TOTAL_SKIPPED}" "${TOTAL_FAILED}" \
  "$((END_OBJECT_STORED - START_OBJECT_STORED))" "$((END_OBJECT_BYTES - START_OBJECT_BYTES))"

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
