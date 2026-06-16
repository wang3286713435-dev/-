#!/usr/bin/env bash
# M3G-7: controlled Wave 1 objectification for non-105 real projects.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
MAX_PROJECTS="${MAX_PROJECTS:-3}"
MAX_FILES="${MAX_FILES:-15}"
MAX_FILES_PER_PROJECT="${MAX_FILES_PER_PROJECT:-5}"
MAX_FILE_SIZE_BYTES="${MAX_FILE_SIZE_BYTES:-10485760}"
MAX_TOTAL_BYTES="${MAX_TOTAL_BYTES:-104857600}"
MAX_PROJECT_BYTES="${MAX_PROJECT_BYTES:-52428800}"

PASS=0
FAIL=0
TOKEN=""
SAMPLE_PROJECT_IDS=()
SAMPLE_IDS=()
SAMPLE_STATS_BEFORE=()
TOTAL_SAMPLE_BYTES=0

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
    r"raw db row",
    r"raw row",
    r"\bselect\s+.+\s+from\b",
    r"\binsert\s+into\b",
    r"\bupdate\s+.+\s+set\b",
    r"\bdelete\s+from\b",
    r"token[^A-Za-z0-9_]",
    r"secret",
    r"password",
    r"access[_-]?key",
]
for pattern in patterns:
    assert not re.search(pattern, payload, re.IGNORECASE | re.DOTALL), f"{label} contains forbidden pattern {pattern}: {payload[:1200]}"
PY
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 120 -X GET "${BASE_URL}${url}" \
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
  local size_bytes="$3"
  local storage_uri
  storage_uri="$(storage_uri_for_file "${file_id}")"
  [[ -z "${storage_uri}" ]] && return 1
  local nas_path
  nas_path="$(nas_path_from_uri "${storage_uri}")"
  [[ -f "${nas_path}" && -r "${nas_path}" ]] || return 1
  SAMPLE_PROJECT_IDS+=("${project_id}")
  SAMPLE_IDS+=("${file_id}")
  SAMPLE_STATS_BEFORE+=("$(file_stat_signature "${nas_path}")")
  TOTAL_SAMPLE_BYTES=$((TOTAL_SAMPLE_BYTES + size_bytes))
}

project_id_json() {
  local csv=""
  if [[ "${#SAMPLE_PROJECT_IDS[@]}" -gt 0 ]]; then
    local IFS=,
    csv="${SAMPLE_PROJECT_IDS[*]}"
  fi
  CSV="${csv}" python3 - <<'PY'
import json
import os
seen = []
for raw in os.environ["CSV"].split(","):
    raw = raw.strip()
    if not raw:
        continue
    value = int(raw)
    if value not in seen:
        seen.append(value)
print(json.dumps(seen))
PY
}

file_id_json() {
  local csv=""
  if [[ "${#SAMPLE_IDS[@]}" -gt 0 ]]; then
    local IFS=,
    csv="${SAMPLE_IDS[*]}"
  fi
  CSV="${csv}" python3 - <<'PY'
import json
import os
print(json.dumps([int(item) for item in os.environ["CSV"].split(",") if item.strip()]))
PY
}

selected_count_for_project() {
  local target="$1"
  local count=0
  if [[ "${#SAMPLE_PROJECT_IDS[@]}" -eq 0 ]]; then
    printf '0'
    return 0
  fi
  for project_id in "${SAMPLE_PROJECT_IDS[@]}"; do
    if [[ "${project_id}" == "${target}" ]]; then
      count=$((count + 1))
    fi
  done
  printf '%s' "${count}"
}

execute_body() {
  local confirmed="$1"
  PROJECT_IDS="$(project_id_json)" \
  FILE_IDS="$(file_id_json)" \
  CONFIRMED="${confirmed}" \
  LIMIT_VALUE="${#SAMPLE_IDS[@]}" \
  MAX_TOTAL="${MAX_TOTAL_BYTES}" \
  MAX_PROJECT_FILES="${MAX_FILES_PER_PROJECT}" \
  MAX_PROJECT_BYTES_VALUE="${MAX_PROJECT_BYTES}" \
  python3 - <<'PY'
import json
import os
body = {
    "projectIds": json.loads(os.environ["PROJECT_IDS"]),
    "fileIds": json.loads(os.environ["FILE_IDS"]),
    "confirmed": os.environ["CONFIRMED"].lower() == "true",
    "targetProvider": "MINIO",
    "limit": int(os.environ["LIMIT_VALUE"]),
    "maxTotalBytes": int(os.environ["MAX_TOTAL"]),
    "maxFilesPerProject": int(os.environ["MAX_PROJECT_FILES"]),
    "maxBytesPerProject": int(os.environ["MAX_PROJECT_BYTES_VALUE"]),
}
print(json.dumps(body, ensure_ascii=False))
PY
}

dry_run_body() {
  PROJECT_IDS_JSON="${1}" \
  MAX_PROJECTS_VALUE="${MAX_PROJECTS}" \
  MAX_FILES_VALUE="${MAX_FILES}" \
  MAX_TOTAL="${MAX_TOTAL_BYTES}" \
  MAX_PROJECT_FILES="${MAX_FILES_PER_PROJECT}" \
  MAX_PROJECT_BYTES_VALUE="${MAX_PROJECT_BYTES}" \
  python3 - <<'PY'
import json
import os
body = {
    "projectIds": json.loads(os.environ["PROJECT_IDS_JSON"]),
    "maxProjects": int(os.environ["MAX_PROJECTS_VALUE"]),
    "limit": int(os.environ["MAX_FILES_VALUE"]),
    "maxTotalBytes": int(os.environ["MAX_TOTAL"]),
    "maxFilesPerProject": int(os.environ["MAX_PROJECT_FILES"]),
    "maxBytesPerProject": int(os.environ["MAX_PROJECT_BYTES_VALUE"]),
}
print(json.dumps(body, ensure_ascii=False))
PY
}

assert_access_downloads() {
  local file_id="$1"
  local ticket_response access_url probe
  ticket_response="$(api_post "/api/data-steward/assets/files/${file_id}/access-tickets" '{"action":"DOWNLOAD"}')"
  assert_ok "${ticket_response}"
  assert_no_forbidden "download ticket ${file_id}" "${ticket_response}"
  access_url="$(json_expr "${ticket_response}" "data['data']['accessUrl']")"
  probe="$(curl -sS --connect-timeout 3 --max-time 120 -o /dev/null -w '%{http_code}:%{size_download}' "${BASE_URL}${access_url}")"
  if [[ "${probe}" == 200:* && "${probe##*:}" -gt 0 ]]; then
    pass "fileId=${file_id} 可通过受控 file-access 读取"
  else
    fail "fileId=${file_id} 受控 file-access 读取失败"
  fi
}

echo "=== M3G-7: multi-project objectification Wave 1 ==="

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
if [[ "$(json_expr "${readiness_response}" "data['data']['endpointType'] == 'NAS_SIDE_MINIO' and data['data']['readinessStatus'] == 'READY' and data['data']['writable'] == True")" == "true" ]]; then
  pass "NAS 侧 MinIO READY 且可写"
else
  fail "NAS 侧 MinIO 尚未 READY：${readiness_response}"
fi

echo ""
echo "--- 2. Wave 1 candidates ---"
candidates_response="$(api_get "/api/data-steward/storage-objectification-wave/candidates")"
assert_ok "${candidates_response}"
assert_no_forbidden "wave candidates" "${candidates_response}"
if [[ "$(json_expr "${candidates_response}" "len(data['data']['candidates']) >= 1 and len(data['data']['candidates']) <= ${MAX_PROJECTS}")" == "true" ]]; then
  pass "候选项目接口返回受控数量候选"
else
  fail "候选项目数量异常：${candidates_response}"
fi
if [[ "$(json_expr "${candidates_response}" "all(str(item.get('projectCode')) not in ['105','95','98','99'] and int(item.get('projectId')) != 503 for item in data['data']['candidates'])")" == "true" ]]; then
  pass "105 / 95 / 98 / 99 未进入可执行候选"
else
  fail "禁入项目进入候选：${candidates_response}"
fi
if [[ "$(json_expr "${candidates_response}" "any(str(item.get('projectCode')) == '105' or int(item.get('projectId')) == 503 for item in data['data']['excludedProjects'])")" == "true" ]]; then
  pass "105 已作为样板项目被排除"
else
  fail "105 未在排除列表中说明"
fi
candidate_project_ids="$(json_expr "${candidates_response}" "[item['projectId'] for item in data['data']['candidates'][:${MAX_PROJECTS}]]")"

echo ""
echo "--- 3. Wave 1 dry-run does not write tasks or objects ---"
task_count_before="$(mysql_exec "SELECT COUNT(1) FROM data_object_migration_tasks WHERE deleted = 0;" 2>/dev/null)"
object_count_before="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE active = 1 AND deleted = 0;" 2>/dev/null)"
dry_response="$(api_post "/api/data-steward/storage-objectification-wave:dry-run" "$(dry_run_body "${candidate_project_ids}")")"
assert_ok "${dry_response}"
assert_no_forbidden "wave dry-run" "${dry_response}"
task_count_after="$(mysql_exec "SELECT COUNT(1) FROM data_object_migration_tasks WHERE deleted = 0;" 2>/dev/null)"
object_count_after="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE active = 1 AND deleted = 0;" 2>/dev/null)"
if [[ "${task_count_before}" == "${task_count_after}" && "${object_count_before}" == "${object_count_after}" ]]; then
  pass "dry-run 未创建迁移任务或对象版本"
else
  fail "dry-run 出现写入：tasks ${task_count_before}->${task_count_after}, objects ${object_count_before}->${object_count_after}"
fi
if [[ "$(json_expr "${dry_response}" "data['data']['dryRun'] == True and data['data']['migrationStarted'] == False and int(data['data']['plannedProjectCount']) <= ${MAX_PROJECTS} and int(data['data']['selectedFileCount']) <= ${MAX_FILES} and int(data['data']['selectedTotalBytes']) <= ${MAX_TOTAL_BYTES} and all(int(project['projectId']) != 503 and str(project.get('projectCode')) not in ['105','95','98','99'] for project in data['data']['projects'])")" == "true" ]]; then
  pass "dry-run 计划满足项目、文件、容量和排除规则"
else
  fail "dry-run 计划越界：${dry_response}"
fi

echo ""
echo "--- 4. Select readable dry-run sample files ---"
sample_lines="$(RESPONSE="${dry_response}" MAX_SIZE="${MAX_FILE_SIZE_BYTES}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
max_size = int(os.environ["MAX_SIZE"])
allowed = {"ELIGIBLE_DRY_RUN", "MISSING_CHECKSUM"}
for project in payload.get("data", {}).get("projects", []):
    for item in project.get("sampleItems", []):
        size = int(item.get("sizeBytes") or 0)
        if item.get("storageStatus") == "NAS_ONLY" and item.get("reason") in allowed and size <= max_size:
            print(f"{project['projectId']}:{item['fileId']}:{size}")
PY
)"
while IFS=':' read -r project_id file_id size_bytes; do
  [[ -z "${project_id:-}" || -z "${file_id:-}" || -z "${size_bytes:-}" ]] && continue
  [[ "${#SAMPLE_IDS[@]}" -ge "${MAX_FILES}" ]] && break
  project_count="$(selected_count_for_project "${project_id}")"
  [[ "${project_count}" -ge "${MAX_FILES_PER_PROJECT}" ]] && continue
  [[ $((TOTAL_SAMPLE_BYTES + size_bytes)) -gt "${MAX_TOTAL_BYTES}" ]] && continue
  if remember_sample_if_readable "${project_id}" "${file_id}" "${size_bytes}"; then
    pass "已选择 projectId=${project_id} 的可读小样本 fileId=${file_id}"
  fi
done <<< "${sample_lines}"

if [[ "${#SAMPLE_IDS[@]}" -gt 0 && "${#SAMPLE_IDS[@]}" -le "${MAX_FILES}" ]]; then
  pass "Wave 1 执行样本已准备：files=${#SAMPLE_IDS[@]} bytes=${TOTAL_SAMPLE_BYTES}"
else
  fail "未找到可执行的 Wave 1 小样本"
fi
unique_project_count="$(project_id_json | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))')"
if [[ "${unique_project_count}" -ge 1 && "${unique_project_count}" -le "${MAX_PROJECTS}" ]]; then
  pass "执行项目数量在 Wave 1 上限内：${unique_project_count}"
else
  fail "执行项目数量越界：${unique_project_count}"
fi

echo ""
echo "--- 5. Confirm=false is rejected ---"
unconfirmed_response="$(api_post "/api/data-steward/storage-objectification-wave:execute" "$(execute_body false)")"
assert_not_ok "${unconfirmed_response}"
assert_no_forbidden "unconfirmed wave execute" "${unconfirmed_response}"
pass "confirmed=false 被拒绝"

echo ""
echo "--- 6. Execute Wave 1 and verify idempotency ---"
execute_response="$(api_post "/api/data-steward/storage-objectification-wave:execute" "$(execute_body true)")"
assert_ok "${execute_response}"
assert_no_forbidden "wave execute" "${execute_response}"
if [[ "$(json_expr "${execute_response}" "data['data']['dryRun'] == False and data['data']['executionStarted'] == True and int(data['data']['selectedFileCount']) == ${#SAMPLE_IDS[@]} and int(data['data']['failedCount']) == 0")" == "true" ]]; then
  pass "Wave 1 小批对象化执行成功"
else
  fail "Wave 1 执行结果异常：${execute_response}"
fi

rerun_response="$(api_post "/api/data-steward/storage-objectification-wave:execute" "$(execute_body true)")"
assert_ok "${rerun_response}"
assert_no_forbidden "wave rerun" "${rerun_response}"
if [[ "$(json_expr "${rerun_response}" "int(data['data']['createdCount']) == 0 and int(data['data']['skippedCount']) == ${#SAMPLE_IDS[@]} and int(data['data']['failedCount']) == 0")" == "true" ]]; then
  pass "重复执行同一批文件按幂等策略跳过"
else
  fail "重复执行未按幂等跳过：${rerun_response}"
fi

echo ""
echo "--- 7. Verify storage status, file-access, active object uniqueness, and NAS originals ---"
file_ids_csv="$(IFS=,; echo "${SAMPLE_IDS[*]}")"
active_count="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE file_id IN (${file_ids_csv}) AND active = 1 AND deleted = 0 AND storage_state = 'OBJECT_STORED';" 2>/dev/null)"
duplicate_max="$(mysql_exec "SELECT COALESCE(MAX(c),0) FROM (SELECT file_id, COUNT(1) AS c FROM data_file_object_versions WHERE file_id IN (${file_ids_csv}) AND active = 1 AND deleted = 0 GROUP BY file_id) x;" 2>/dev/null)"
if [[ "${active_count}" -eq "${#SAMPLE_IDS[@]}" && "${duplicate_max}" -eq 1 ]]; then
  pass "已对象化文件 active OBJECT_STORED 版本唯一"
else
  fail "active object version 异常：active=${active_count} duplicateMax=${duplicate_max}"
fi

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

echo ""
echo "--- 8. Wave reports and script tracking ---"
report_response="$(api_get "/api/data-steward/storage-objectification-wave/reports")"
assert_ok "${report_response}"
assert_no_forbidden "wave reports" "${report_response}"
if [[ "$(json_expr "${report_response}" "data['data']['projectCount'] >= ${unique_project_count} and data['data']['objectStoredFiles'] >= ${#SAMPLE_IDS[@]}")" == "true" ]]; then
  pass "每项目覆盖率报告可查询"
else
  fail "Wave 1 报告异常：${report_response}"
fi
if git ls-files --error-unmatch scripts/dev/check-m3g7-multi-project-objectification-wave1.sh >/dev/null 2>&1; then
  pass "M3G-7 专项脚本已纳入 Git 跟踪"
else
  fail "M3G-7 专项脚本尚未纳入 Git 跟踪"
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
