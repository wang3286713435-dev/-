#!/usr/bin/env bash
# M3G-6: 105 full objectification trial with continuous controlled batches.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
BATCH_FILE_LIMIT="${BATCH_FILE_LIMIT:-5}"
BATCH_BYTES_LIMIT="${BATCH_BYTES_LIMIT:-52428800}"

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

plan_body() {
  python3 - <<PY
import json
body = {
    "checksumState": "ANY",
    "batchFileLimit": int("${BATCH_FILE_LIMIT}"),
    "batchBytesLimit": int("${BATCH_BYTES_LIMIT}"),
}
print(json.dumps(body, ensure_ascii=False))
PY
}

full_plan() {
  api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-full-plan" "$(plan_body)"
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
    local already_tracked=0
    if [[ "${#TRACKED_IDS[@]}" -gt 0 ]]; then
      for tracked_id in "${TRACKED_IDS[@]}"; do
        if [[ "${tracked_id}" == "${file_id}" ]]; then
          already_tracked=1
          break
        fi
      done
    fi
    [[ "${already_tracked}" -eq 1 ]] && continue
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
  local confirmed="$2"
  FILE_IDS="$(json_array_from_csv "${ids_csv}")" CONFIRMED="${confirmed}" PROJECT_ID="${PROJECT_ID}" BATCH_BYTES="${BATCH_BYTES_LIMIT}" python3 - <<'PY'
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
    "rateLimitBytesPerMinute": 10485760,
    "confirmed": os.environ["CONFIRMED"].lower() == "true",
    "targetProvider": "MINIO",
}
print(json.dumps(body, ensure_ascii=False))
PY
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

echo "=== M3G-6: 105 full objectification trial ==="

echo ""
echo "--- 1. Login and NAS-side MinIO readiness ---"
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
  fail "NAS 侧 MinIO 尚未达到 M3G-6 执行条件：${readiness_response}"
fi

echo ""
echo "--- 2. Full plan for 105/project ${PROJECT_ID} ---"
plan_before="$(full_plan)"
assert_ok "${plan_before}"
assert_no_forbidden "full plan before" "${plan_before}"
db_total="$(mysql_exec "SELECT COUNT(1) FROM data_file_resources WHERE project_id = ${PROJECT_ID} AND deleted = 0;" 2>/dev/null | head -n 1)"
plan_total="$(json_expr "${plan_before}" "int(data['data']['totalFileCount'])")"
if [[ "${plan_total}" == "${db_total}" ]]; then
  pass "105 全量计划总文件数与台账一致：${plan_total}"
else
  fail "105 全量计划总文件数不一致：plan=${plan_total} db=${db_total}"
fi
before_object_stored="$(json_expr "${plan_before}" "int(data['data']['objectStoredCount'])")"
before_nas_only="$(json_expr "${plan_before}" "int(data['data']['nasOnlyCount'])")"
before_eligible="$(json_expr "${plan_before}" "int(data['data']['eligibleRemainingCount'])")"
before_next_count="$(json_expr "${plan_before}" "int(data['data']['nextBatchFileCount'])")"
if [[ "${plan_total}" -gt 0 && "${before_object_stored}" -ge "${plan_total}" && "${before_nas_only}" -eq 0 && "${before_eligible}" -eq 0 ]]; then
  pass "105 已全量对象化，M3G-6 下一批执行回归进入完成态兼容验证"
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
if [[ "${before_eligible}" -gt 0 && "${before_next_count}" -gt 0 && "${before_next_count}" -le "${BATCH_FILE_LIMIT}" ]]; then
  pass "全量计划给出下一批：eligible=${before_eligible} next=${before_next_count}"
else
  fail "105 当前没有可执行下一批，无法验证连续分批：${plan_before}"
fi

batch1_ids="$(batch_ids_from_plan "${plan_before}")"
remember_original_stats "${batch1_ids}"

echo ""
echo "--- 3. confirmed=false is rejected ---"
unconfirmed_response="$(api_post "/api/data-steward/storage-objectification-plans:execute" "$(execute_body "${batch1_ids}" false)")"
assert_not_ok "${unconfirmed_response}"
assert_no_forbidden "unconfirmed execute" "${unconfirmed_response}"
pass "下一批执行必须 confirmed=true"

echo ""
echo "--- 4. Execute first batch and idempotency ---"
execute1_response="$(api_post "/api/data-steward/storage-objectification-plans:execute" "$(execute_body "${batch1_ids}" true)")"
assert_ok "${execute1_response}"
assert_no_forbidden "execute batch1" "${execute1_response}"
created1="$(json_expr "${execute1_response}" "int(data['data']['createdCount'])")"
failed1="$(json_expr "${execute1_response}" "int(data['data']['failedCount'])")"
if [[ "${created1}" -gt 0 && "${failed1}" -eq 0 ]]; then
  pass "第一批执行成功：created=${created1} failed=${failed1}"
else
  fail "第一批执行结果异常：${execute1_response}"
fi

rerun1_response="$(api_post "/api/data-steward/storage-objectification-plans:execute" "$(execute_body "${batch1_ids}" true)")"
assert_ok "${rerun1_response}"
assert_no_forbidden "rerun batch1" "${rerun1_response}"
if [[ "$(json_expr "${rerun1_response}" "int(data['data']['skippedCount']) == int('${before_next_count}') and int(data['data']['failedCount']) == 0")" == "true" ]]; then
  pass "重复执行第一批按幂等策略跳过"
else
  fail "重复执行第一批未按幂等跳过：${rerun1_response}"
fi

echo ""
echo "--- 5. Refresh full plan and execute second batch ---"
plan_after1="$(full_plan)"
assert_ok "${plan_after1}"
assert_no_forbidden "full plan after1" "${plan_after1}"
after1_object_stored="$(json_expr "${plan_after1}" "int(data['data']['objectStoredCount'])")"
after1_nas_only="$(json_expr "${plan_after1}" "int(data['data']['nasOnlyCount'])")"
if [[ "${after1_object_stored}" -gt "${before_object_stored}" && "${after1_nas_only}" -lt "${before_nas_only}" ]]; then
  pass "第一批后覆盖率持续上升：objectStored ${before_object_stored}->${after1_object_stored}"
else
  fail "第一批后覆盖率未上升：before=${before_object_stored} after=${after1_object_stored}"
fi

batch2_ids="$(batch_ids_from_plan "${plan_after1}")"
if [[ -z "${batch2_ids}" ]]; then
  pass "第一批后已没有可执行下一批，105 当前可执行项已清零"
else
  if [[ "$(IDS1="${batch1_ids}" IDS2="${batch2_ids}" python3 - <<'PY'
import os
s1 = set(int(x) for x in os.environ["IDS1"].split(",") if x)
s2 = set(int(x) for x in os.environ["IDS2"].split(",") if x)
print("true" if s1.isdisjoint(s2) else "false")
PY
)" == "true" ]]; then
    pass "第二批计划未重复第一批文件"
  else
    fail "第二批计划重复了第一批文件"
  fi
  remember_original_stats "${batch2_ids}"
  execute2_response="$(api_post "/api/data-steward/storage-objectification-plans:execute" "$(execute_body "${batch2_ids}" true)")"
  assert_ok "${execute2_response}"
  assert_no_forbidden "execute batch2" "${execute2_response}"
  created2="$(json_expr "${execute2_response}" "int(data['data']['createdCount'])")"
  failed2="$(json_expr "${execute2_response}" "int(data['data']['failedCount'])")"
  if [[ "${created2}" -gt 0 && "${failed2}" -eq 0 ]]; then
    pass "第二批执行成功：created=${created2} failed=${failed2}"
  else
    fail "第二批执行结果异常：${execute2_response}"
  fi
fi

echo ""
echo "--- 6. Final plan, file-access, governance, and originals ---"
plan_after2="$(full_plan)"
assert_ok "${plan_after2}"
assert_no_forbidden "full plan after2" "${plan_after2}"
after2_object_stored="$(json_expr "${plan_after2}" "int(data['data']['objectStoredCount'])")"
if [[ "${after2_object_stored}" -gt "${after1_object_stored}" || -z "${batch2_ids}" ]]; then
  pass "连续分批推进可验证：objectStored ${before_object_stored}->${after1_object_stored}->${after2_object_stored}"
else
  fail "第二批后覆盖率未继续上升：after1=${after1_object_stored} after2=${after2_object_stored}"
fi

all_ids_csv="$(IFS=,; echo "${TRACKED_IDS[*]}")"
if [[ -n "${all_ids_csv}" && "$(active_object_version_anomalies "${all_ids_csv}")" == "0" ]]; then
  pass "连续执行未产生重复 active object version"
else
  fail "active object version 存在重复或缺失"
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

failed_count="$(json_expr "${plan_after2}" "int(data['data']['migrationFailedCount'])")"
failure_reason_count="$(json_expr "${plan_after2}" "len(data['data']['failureReasons'])")"
governance_count="$(json_expr "${plan_after2}" "len(data['data']['governanceItems'])")"
if [[ "${failed_count}" -eq 0 || "${failure_reason_count}" -gt 0 || "${governance_count}" -gt 0 ]]; then
  pass "失败/治理口径可解释：failed=${failed_count} failureReasons=${failure_reason_count} governance=${governance_count}"
else
  fail "存在失败文件但缺少失败原因或治理清单：${plan_after2}"
fi

verify_original_stats

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
