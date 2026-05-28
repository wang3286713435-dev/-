#!/usr/bin/env bash
# M3G-4: controlled multi-real-project small-batch objectification execution.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
MAX_FILE_SIZE_BYTES="${MAX_FILE_SIZE_BYTES:-10485760}"
MAX_TOTAL_BYTES="${MAX_TOTAL_BYTES:-104857600}"
MAX_PROJECT_BYTES="${MAX_PROJECT_BYTES:-52428800}"
M3G4_READ_ONLY="${M3G4_READ_ONLY:-0}"

PASS=0
FAIL=0
TOKEN=""
SELECTED_PROJECT_IDS=()
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

remember_sample_if_readable() {
  local project_id="$1"
  local file_id="$2"
  local storage_uri
  storage_uri="$(storage_uri_for_file "${file_id}")"
  [[ -z "${storage_uri}" ]] && return 1
  local nas_path
  nas_path="$(nas_path_from_uri "${storage_uri}")"
  [[ -f "${nas_path}" && -r "${nas_path}" ]] || return 1
  SELECTED_PROJECT_IDS+=("${project_id}")
  SAMPLE_IDS+=("${file_id}")
  SAMPLE_STATS_BEFORE+=("$(file_stat_signature "${nas_path}")")
}

project_id_csv() {
  local IFS=,
  echo "${SELECTED_PROJECT_IDS[*]}"
}

file_id_csv() {
  local IFS=,
  echo "${SAMPLE_IDS[*]}"
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
  local confirmed="$1"
  local limit="$2"
  PROJECT_IDS="$(json_array_from_csv "$(project_id_csv)")" \
  FILE_IDS="$(json_array_from_csv "$(file_id_csv)")" \
  CONFIRMED="${confirmed}" \
  LIMIT_VALUE="${limit}" \
  MAX_TOTAL="${MAX_TOTAL_BYTES}" \
  MAX_PROJECT="${MAX_PROJECT_BYTES}" \
  python3 - <<'PY'
import json
import os
body = {
    "projectIds": json.loads(os.environ["PROJECT_IDS"]),
    "fileIds": json.loads(os.environ["FILE_IDS"]),
    "realProjectsOnly": True,
    "storageState": "NAS_ONLY",
    "checksumState": "ANY",
    "limit": int(os.environ["LIMIT_VALUE"]),
    "maxTotalBytes": int(os.environ["MAX_TOTAL"]),
    "maxFilesPerProject": 1,
    "maxBytesPerProject": int(os.environ["MAX_PROJECT"]),
    "concurrencyLimit": 1,
    "rateLimitBytesPerMinute": 10485760,
    "confirmed": os.environ["CONFIRMED"].lower() == "true",
    "targetProvider": "MINIO",
}
print(json.dumps(body, ensure_ascii=False))
PY
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

echo "=== M3G-4: controlled multi-project objectification execution ==="

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
if [[ "${M3G4_READ_ONLY}" == "1" || "${M3G4_READ_ONLY}" == "true" ]]; then
  if [[ "$(json_expr "${readiness_response}" "data['data']['configured'] == True and data['data']['reachable'] == True and data['data']['readable'] == True")" == "true" ]]; then
    pass "只读回归模式下对象存储可达且可读"
  else
    fail "只读回归模式下对象存储不可读：${readiness_response}"
  fi
elif [[ "$(json_expr "${readiness_response}" "data['data']['endpointType'] == 'NAS_SIDE_MINIO' and data['data']['readinessStatus'] == 'READY' and data['data']['writable'] == True")" == "true" ]]; then
  pass "NAS 侧 MinIO READY 且可写"
else
  fail "NAS 侧 MinIO 尚未 READY：${readiness_response}"
fi

echo ""
echo "--- 2. Select at most three real-project readable NAS_ONLY files ---"
inventory_response="$(api_get "/api/data-steward/storage-objectification-inventory")"
assert_ok "${inventory_response}"
assert_no_forbidden "inventory" "${inventory_response}"
candidate_projects="$(RESPONSE="${inventory_response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
projects = [
    str(item["projectId"])
    for item in payload["data"]["projects"]
    if item.get("realNasProject") and int(item.get("nasOnlyFiles") or 0) > 0
]
print(",".join(projects[:12]))
PY
)"
IFS=',' read -r -a project_candidates <<< "${candidate_projects}"
for project_id in "${project_candidates[@]}"; do
  [[ -z "${project_id}" ]] && continue
  [[ "${#SAMPLE_IDS[@]}" -ge 3 ]] && break
  dry_run_body="$(PROJECT_ID="${project_id}" MAX_SIZE="${MAX_FILE_SIZE_BYTES}" MAX_TOTAL="${MAX_TOTAL_BYTES}" MAX_PROJECT="${MAX_PROJECT_BYTES}" python3 - <<'PY'
import json
import os
body = {
    "projectIds": [int(os.environ["PROJECT_ID"])],
    "realProjectsOnly": True,
    "storageState": "NAS_ONLY",
    "checksumState": "ANY",
    "limit": 20,
    "maxSizeBytes": int(os.environ["MAX_SIZE"]),
    "maxTotalBytes": int(os.environ["MAX_TOTAL"]),
    "maxFilesPerProject": 1,
    "maxBytesPerProject": int(os.environ["MAX_PROJECT"]),
    "concurrencyLimit": 1,
}
print(json.dumps(body, ensure_ascii=False))
PY
)"
  dry_run_response="$(api_post "/api/data-steward/storage-objectification-plans:dry-run" "${dry_run_body}")"
  assert_ok "${dry_run_response}"
  assert_no_forbidden "project dry-run ${project_id}" "${dry_run_response}"
  candidate_file="$(RESPONSE="${dry_run_response}" MAX_SIZE="${MAX_FILE_SIZE_BYTES}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
max_size = int(os.environ["MAX_SIZE"])
allowed = {"ELIGIBLE_DRY_RUN", "MISSING_CHECKSUM"}
for project in payload.get("data", {}).get("projects", []):
    for item in project.get("sampleItems", []):
        if item.get("storageStatus") == "NAS_ONLY" and item.get("reason") in allowed and int(item.get("sizeBytes") or 0) <= max_size:
            print(item.get("fileId"))
            raise SystemExit(0)
PY
)"
  if [[ -n "${candidate_file}" ]] && remember_sample_if_readable "${project_id}" "${candidate_file}"; then
    pass "已选择 projectId=${project_id} 的可读 NAS_ONLY 小样本 fileId=${candidate_file}"
  fi
done

if [[ "${#SAMPLE_IDS[@]}" -gt 0 && "${#SAMPLE_IDS[@]}" -le 3 ]]; then
  pass "执行样本已准备：projects=$(project_id_csv) files=$(file_id_csv)"
else
  fail "未找到可执行的真实项目小样本"
fi

echo ""
echo "--- 3. Confirm=false and over-limit requests are rejected ---"
unconfirmed_response="$(api_post "/api/data-steward/storage-objectification-plans:execute" "$(execute_body false "${#SAMPLE_IDS[@]}")")"
assert_not_ok "${unconfirmed_response}"
assert_no_forbidden "unconfirmed execute" "${unconfirmed_response}"
pass "confirmed=false 被拒绝"

over_limit_response="$(api_post "/api/data-steward/storage-objectification-plans:execute" "$(execute_body true 10)")"
assert_not_ok "${over_limit_response}"
assert_no_forbidden "over-limit execute" "${over_limit_response}"
pass "超出总文件数硬上限的执行请求被拒绝"

echo ""
echo "--- 4. Execute controlled small batch ---"
if [[ "${M3G4_READ_ONLY}" == "1" || "${M3G4_READ_ONLY}" == "true" ]]; then
  pass "M3G4_READ_ONLY 已开启，跳过真实对象化执行和重复执行验证"
  pass "只读模式未触发对象化迁移写入"
else
execute_response="$(api_post "/api/data-steward/storage-objectification-plans:execute" "$(execute_body true "${#SAMPLE_IDS[@]}")")"
assert_ok "${execute_response}"
assert_no_forbidden "execute response" "${execute_response}"
if [[ "$(json_expr "${execute_response}" "data['data']['dryRun'] == False and data['data']['executionStarted'] == True and data['data']['taskSource'] == 'MULTI_PROJECT_CONTROLLED_EXECUTION' and int(data['data']['selectedFileCount']) == int('${#SAMPLE_IDS[@]}') and int(data['data']['failedCount']) == 0")" == "true" ]]; then
  pass "受控小批对象化执行成功"
else
  fail "受控小批对象化执行结果异常：${execute_response}"
fi

created_count="$(json_expr "${execute_response}" "int(data['data']['createdCount'])")"
skipped_count="$(json_expr "${execute_response}" "int(data['data']['skippedCount'])")"
if [[ "${created_count}" -gt 0 || "${skipped_count}" -eq "${#SAMPLE_IDS[@]}" ]]; then
  pass "执行结果可解释：created=${created_count} skipped=${skipped_count}"
else
  fail "执行结果未体现成功或幂等跳过"
fi

echo ""
echo "--- 5. Re-run same files and verify idempotency ---"
rerun_response="$(api_post "/api/data-steward/storage-objectification-plans:execute" "$(execute_body true "${#SAMPLE_IDS[@]}")")"
assert_ok "${rerun_response}"
assert_no_forbidden "rerun response" "${rerun_response}"
if [[ "$(json_expr "${rerun_response}" "int(data['data']['skippedCount']) == int('${#SAMPLE_IDS[@]}') and int(data['data']['failedCount']) == 0")" == "true" ]]; then
  pass "重复执行同一批文件按幂等策略跳过"
else
  fail "重复执行未按幂等跳过：${rerun_response}"
fi

echo ""
echo "--- 6. Verify OBJECT_STORED, file-access, and NAS originals ---"
for index in "${!SAMPLE_IDS[@]}"; do
  file_id="${SAMPLE_IDS[$index]}"
  status_response="$(api_get "/api/data-steward/assets/files/${file_id}/storage-status")"
  assert_ok "${status_response}"
  assert_no_forbidden "storage status ${file_id}" "${status_response}"
  if [[ "$(json_expr "${status_response}" "data['data']['storageState'] == 'OBJECT_STORED' and data['data']['objectStored'] == True")" == "true" ]]; then
    pass "fileId=${file_id} storage-status 为 OBJECT_STORED"
  else
    fail "fileId=${file_id} storage-status 未进入 OBJECT_STORED"
  fi
  assert_access_downloads "${file_id}"
  storage_uri="$(storage_uri_for_file "${file_id}")"
  nas_path="$(nas_path_from_uri "${storage_uri}")"
  if [[ -f "${nas_path}" && "$(file_stat_signature "${nas_path}")" == "${SAMPLE_STATS_BEFORE[$index]}" ]]; then
    pass "fileId=${file_id} NAS 原文件仍存在且 size/mtime 未变化"
  else
    fail "fileId=${file_id} NAS 原文件状态变化"
  fi
done
fi

echo ""
echo "--- 7. Script tracking ---"
if git ls-files --error-unmatch scripts/dev/check-m3g4-controlled-multi-project-objectification.sh >/dev/null 2>&1; then
  pass "M3G-4 专项脚本已纳入 Git 跟踪"
else
  fail "M3G-4 专项脚本尚未纳入 Git 跟踪"
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
