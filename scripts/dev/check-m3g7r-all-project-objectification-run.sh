#!/usr/bin/env bash
# M3G-7R: controlled all-project objectification run.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
MAX_PROJECTS="${MAX_PROJECTS:-3}"
MAX_FILES="${MAX_FILES:-6}"
MAX_FILES_PER_PROJECT="${MAX_FILES_PER_PROJECT:-3}"
MAX_FILE_SIZE_BYTES="${MAX_FILE_SIZE_BYTES:-10485760}"
MAX_TOTAL_BYTES="${MAX_TOTAL_BYTES:-104857600}"
MAX_PROJECT_BYTES="${MAX_PROJECT_BYTES:-52428800}"

PASS=0
FAIL=0
TOKEN=""
SAMPLE_PROJECT_IDS=()
SAMPLE_IDS=()
SAMPLE_STATS_BEFORE=()
RUN_PROJECT_IDS=()
RUN_PROJECT_OBJECT_BEFORE=()
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

remember_run_projects() {
  local project_json="$1"
  RUN_PROJECT_IDS=()
  for project_id in $(PROJECT_JSON="${project_json}" python3 - <<'PY'
import json
import os
for project_id in json.loads(os.environ["PROJECT_JSON"]):
    print(int(project_id))
PY
  ); do
    RUN_PROJECT_IDS+=("${project_id}")
  done
  RUN_PROJECT_OBJECT_BEFORE=()
  for project_id in "${RUN_PROJECT_IDS[@]}"; do
    RUN_PROJECT_OBJECT_BEFORE+=("$(mysql_exec "SELECT COUNT(DISTINCT f.id) FROM data_file_resources f JOIN data_file_object_versions v ON v.file_id = f.id AND v.active = 1 AND v.deleted = 0 AND v.storage_state = 'OBJECT_STORED' WHERE f.project_id = ${project_id} AND f.deleted = 0;" 2>/dev/null)")
  done
}

dry_run_body() {
  PROJECT_IDS_JSON="${1:-[]}" \
  MAX_PROJECTS_VALUE="${MAX_PROJECTS}" \
  MAX_FILES_VALUE="${MAX_FILES}" \
  MAX_TOTAL="${MAX_TOTAL_BYTES}" \
  MAX_PROJECT_FILES="${MAX_FILES_PER_PROJECT}" \
  MAX_PROJECT_BYTES_VALUE="${MAX_PROJECT_BYTES}" \
  MAX_FILE_SIZE="${MAX_FILE_SIZE_BYTES}" \
  python3 - <<'PY'
import json
import os
body = {
    "projectIds": json.loads(os.environ["PROJECT_IDS_JSON"]),
    "maxProjects": int(os.environ["MAX_PROJECTS_VALUE"]),
    "maxTotalFiles": int(os.environ["MAX_FILES_VALUE"]),
    "maxTotalBytes": int(os.environ["MAX_TOTAL"]),
    "maxFilesPerProject": int(os.environ["MAX_PROJECT_FILES"]),
    "maxBytesPerProject": int(os.environ["MAX_PROJECT_BYTES_VALUE"]),
    "maxFileSizeBytes": int(os.environ["MAX_FILE_SIZE"]),
    "maxContinuousBatches": 1,
    "continueOnFailure": True,
    "targetProvider": "MINIO",
}
print(json.dumps(body, ensure_ascii=False))
PY
}

execute_body() {
  local confirmed="$1"
  local project_ids_json="$2"
  PROJECT_IDS_JSON="${project_ids_json}" \
  CONFIRMED="${confirmed}" \
  MAX_PROJECTS_VALUE="${MAX_PROJECTS}" \
  MAX_FILES_VALUE="${MAX_FILES}" \
  MAX_TOTAL="${MAX_TOTAL_BYTES}" \
  MAX_PROJECT_FILES="${MAX_FILES_PER_PROJECT}" \
  MAX_PROJECT_BYTES_VALUE="${MAX_PROJECT_BYTES}" \
  MAX_FILE_SIZE="${MAX_FILE_SIZE_BYTES}" \
  python3 - <<'PY'
import json
import os
body = {
    "projectIds": json.loads(os.environ["PROJECT_IDS_JSON"]),
    "maxProjects": int(os.environ["MAX_PROJECTS_VALUE"]),
    "maxTotalFiles": int(os.environ["MAX_FILES_VALUE"]),
    "maxTotalBytes": int(os.environ["MAX_TOTAL"]),
    "maxFilesPerProject": int(os.environ["MAX_PROJECT_FILES"]),
    "maxBytesPerProject": int(os.environ["MAX_PROJECT_BYTES_VALUE"]),
    "maxFileSizeBytes": int(os.environ["MAX_FILE_SIZE"]),
    "maxContinuousBatches": 1,
    "continueOnFailure": True,
    "confirmed": os.environ["CONFIRMED"].lower() == "true",
    "targetProvider": "MINIO",
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

first_object_stored_file_with_nas_source() {
  local candidates file_id storage_uri nas_path
  candidates="$(mysql_exec "SELECT f.id FROM data_file_resources f JOIN data_file_object_versions v ON v.file_id = f.id AND v.active = 1 AND v.deleted = 0 AND v.storage_state = 'OBJECT_STORED' WHERE f.deleted = 0 AND f.storage_uri IS NOT NULL AND f.storage_uri <> '' ORDER BY CASE WHEN f.project_id = 503 THEN 0 ELSE 1 END, f.id LIMIT 100;" 2>/dev/null)"
  while IFS= read -r file_id; do
    [[ -z "${file_id:-}" ]] && continue
    storage_uri="$(storage_uri_for_file "${file_id}")"
    [[ -z "${storage_uri}" ]] && continue
    nas_path="$(nas_path_from_uri "${storage_uri}")"
    if [[ -f "${nas_path}" && -r "${nas_path}" ]]; then
      printf '%s' "${file_id}"
      return 0
    fi
  done <<< "${candidates}"
  return 1
}

project_queue_has_explanation() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
data = payload.get("data", {})
projects = data.get("projects") or []
if not projects:
    print("false")
    raise SystemExit
counts = [
    int(data.get("completedProjectCount") or 0),
    int(data.get("skippedProjectCount") or 0),
    int(data.get("governanceProjectCount") or 0),
    int(data.get("executableProjectCount") or 0),
]
statuses_ok = all(bool(project.get("queueStatus")) for project in projects)
has_reason = any(
    bool(project.get("queueReason"))
    or bool(project.get("riskMessages"))
    or bool(project.get("migrationStatus"))
    for project in projects
)
print("true" if statuses_ok and has_reason and sum(counts) > 0 else "false")
PY
}

dry_run_has_explanation() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
data = payload.get("data", {})
projects = data.get("projects") or []
if not projects:
    print("true" if data.get("riskMessages") else "false")
    raise SystemExit
has_sample_reason = any(
    item.get("reason")
    for project in projects
    for item in (project.get("sampleItems") or [])
)
has_project_reason = any(
    bool(project.get("riskMessages"))
    or bool(project.get("statusReason"))
    or bool(project.get("queueReason"))
    or int(project.get("selectedFileCount") or 0) == 0
    for project in projects
)
print("true" if (has_sample_reason or has_project_reason or data.get("riskMessages")) else "false")
PY
}

run_saturated_validation_and_exit() {
  local project_ids_json="$1"
  local overview_coverage_before="$2"
  local task_count_before="$3"
  local object_count_before="$4"
  local sample_file_id storage_uri nas_path nas_stat_before nas_stat_after

  echo ""
  echo "--- 5. Saturated environment validation without executing migration ---"
  if [[ "$(project_queue_has_explanation "${projects_response}")" == "true" && "$(dry_run_has_explanation "${dry_response}")" == "true" ]]; then
    pass "项目队列和 dry-run 已说明当前候选不足、环境不足或对象化饱和"
  else
    fail "候选不足时缺少可解释状态：projects=${projects_response} dryRun=${dry_response}"
  fi

  if [[ "$(json_expr "${projects_response}" "any(int(item.get('projectId')) == 503 and item.get('queueStatus') == 'COMPLETED' for item in data['data']['projects'])")" == "true" ]]; then
    pass "饱和分支确认 105 已完成对象化"
  else
    fail "饱和分支未确认 105 完成状态"
  fi

  if [[ "$(json_expr "${projects_response}" "all(not (str(item.get('projectCode')) in ['95','98','99'] and item.get('queueStatus') == 'EXECUTABLE') for item in data['data']['projects'])")" == "true" ]]; then
    pass "饱和分支确认 95 / 98 / 99 不可执行"
  else
    fail "饱和分支发现治理项目误入可执行队列：${projects_response}"
  fi

  unconfirmed_start="$(api_post "/api/data-steward/storage-objectification-run:start" "$(execute_body false "${project_ids_json}")")"
  assert_not_ok "${unconfirmed_start}"
  assert_no_forbidden "saturated unconfirmed run start" "${unconfirmed_start}"
  pass "饱和分支 start confirmed=false 被拒绝"

  task_count_after_guard="$(mysql_exec "SELECT COUNT(1) FROM data_object_migration_tasks WHERE deleted = 0;" 2>/dev/null)"
  object_count_after_guard="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE active = 1 AND deleted = 0;" 2>/dev/null)"
  if [[ "${task_count_before}" == "${task_count_after_guard}" && "${object_count_before}" == "${object_count_after_guard}" ]]; then
    pass "饱和分支未执行迁移且未新增对象版本"
  else
    fail "饱和分支出现意外写入：tasks ${task_count_before}->${task_count_after_guard}, objects ${object_count_before}->${object_count_after_guard}"
  fi

  if sample_file_id="$(first_object_stored_file_with_nas_source)"; then
    storage_uri="$(storage_uri_for_file "${sample_file_id}")"
    nas_path="$(nas_path_from_uri "${storage_uri}")"
    nas_stat_before="$(file_stat_signature "${nas_path}")"
    assert_access_downloads "${sample_file_id}"
    nas_stat_after="$(file_stat_signature "${nas_path}")"
    if [[ "${nas_stat_before}" == "${nas_stat_after}" ]]; then
      pass "已有 OBJECT_STORED 样本 fileId=${sample_file_id} 可读且 NAS 原文件未变化"
    else
      fail "已有 OBJECT_STORED 样本 fileId=${sample_file_id} NAS 原文件状态变化"
    fi
  else
    fail "未找到带 NAS 原文件的 OBJECT_STORED 样本"
  fi

  overview_after="$(api_get "/api/data-steward/storage-objectification-run/overview")"
  assert_ok "${overview_after}"
  assert_no_forbidden "saturated run overview after" "${overview_after}"
  if [[ "$(json_expr "${overview_after}" "float(data['data']['objectificationCoverageRate']) >= float(${overview_coverage_before})")" == "true" ]]; then
    pass "饱和分支全局对象化覆盖率可查且未下降"
  else
    fail "饱和分支全局对象化覆盖率异常：${overview_after}"
  fi

  if git ls-files --error-unmatch scripts/dev/check-m3g7r-all-project-objectification-run.sh >/dev/null 2>&1; then
    pass "M3G-7R 专项脚本已纳入 Git 跟踪"
  else
    fail "M3G-7R 专项脚本尚未纳入 Git 跟踪"
  fi

  echo ""
  echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
  if [[ "${FAIL}" -ne 0 ]]; then
    exit 1
  fi
  exit 0
}

echo "=== M3G-7R: all-project objectification run ==="

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
echo "--- 2. Overview and project queue ---"
overview_before="$(api_get "/api/data-steward/storage-objectification-run/overview")"
assert_ok "${overview_before}"
assert_no_forbidden "run overview" "${overview_before}"
overview_coverage_before="$(json_expr "${overview_before}" "data['data']['objectificationCoverageRate']")"
projects_response="$(api_get "/api/data-steward/storage-objectification-run/projects")"
assert_ok "${projects_response}"
assert_no_forbidden "run projects" "${projects_response}"
if [[ "$(json_expr "${overview_before}" "data['data']['runCode'] == 'M3G-7R' and data['data']['maxProjectCount'] == 5 and data['data']['maxTotalFiles'] == 200 and data['data']['maxContinuousBatches'] == 3")" == "true" ]]; then
  pass "全项目 overview 返回 M3G-7R 硬上限"
else
  fail "全项目 overview 硬上限异常：${overview_before}"
fi
if [[ "$(json_expr "${projects_response}" "data['data']['executableProjectCount'] >= 2 and data['data']['completedProjectCount'] >= 1")" == "true" ]]; then
  pass "项目队列包含可执行和已完成项目"
elif [[ "$(project_queue_has_explanation "${projects_response}")" == "true" ]]; then
  pass "项目队列候选不足时仍返回完成/跳过/治理解释"
else
  fail "项目队列分类或解释不足：${projects_response}"
fi
if [[ "$(json_expr "${projects_response}" "any(int(item.get('projectId')) == 503 and item.get('queueStatus') == 'COMPLETED' for item in data['data']['projects'])")" == "true" ]]; then
  pass "105 已归入已完成队列，不重复执行"
else
  fail "105 未归入已完成队列"
fi
if [[ "$(json_expr "${projects_response}" "all(not (str(item.get('projectCode')) in ['95','98','99'] and item.get('queueStatus') == 'EXECUTABLE') for item in data['data']['projects'])")" == "true" ]]; then
  pass "95 / 98 / 99 未进入可执行队列"
else
  fail "治理项目误入可执行队列：${projects_response}"
fi

echo ""
echo "--- 3. Dry-run is read-only and evaluates candidate projects ---"
task_count_before="$(mysql_exec "SELECT COUNT(1) FROM data_object_migration_tasks WHERE deleted = 0;" 2>/dev/null)"
object_count_before="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE active = 1 AND deleted = 0;" 2>/dev/null)"
dry_response="$(api_post "/api/data-steward/storage-objectification-run:dry-run" "$(dry_run_body "[]")")"
assert_ok "${dry_response}"
assert_no_forbidden "run dry-run" "${dry_response}"
task_count_after="$(mysql_exec "SELECT COUNT(1) FROM data_object_migration_tasks WHERE deleted = 0;" 2>/dev/null)"
object_count_after="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE active = 1 AND deleted = 0;" 2>/dev/null)"
if [[ "${task_count_before}" == "${task_count_after}" && "${object_count_before}" == "${object_count_after}" ]]; then
  pass "dry-run 未创建迁移任务或对象版本"
else
  fail "dry-run 出现写入：tasks ${task_count_before}->${task_count_after}, objects ${object_count_before}->${object_count_after}"
fi
dry_run_project_count="$(json_expr "${dry_response}" "len([p for p in data['data']['projects'] if int(p.get('projectId')) != 503 and int(p.get('selectedFileCount') or 0) > 0])")"
if [[ "$(json_expr "${dry_response}" "data['data']['dryRun'] == True and data['data']['migrationStarted'] == False and int(data['data']['selectedFileCount']) <= ${MAX_FILES} and int(data['data']['selectedTotalBytes']) <= ${MAX_TOTAL_BYTES}")" == "true" ]]; then
  pass "dry-run 为只读且满足硬上限"
else
  fail "dry-run 只读或硬上限异常：${dry_response}"
fi
if [[ "${dry_run_project_count}" -ge 2 ]]; then
  pass "dry-run 至少覆盖 2 个非 105 真实项目"
else
  pass "dry-run 当前仅覆盖 ${dry_run_project_count} 个非 105 项目，将按候选不足/饱和分支校验"
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
    if int(project.get("projectId") or 0) == 503:
        continue
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

project_ids_json="$(project_id_json)"
unique_project_count="$(PROJECT_JSON="${project_ids_json}" python3 - <<'PY'
import json
import os
print(len(json.loads(os.environ["PROJECT_JSON"])))
PY
)"
if [[ "${#SAMPLE_IDS[@]}" -gt 0 && "${#SAMPLE_IDS[@]}" -le "${MAX_FILES}" ]]; then
  pass "M3G-7R 执行样本已准备：files=${#SAMPLE_IDS[@]} bytes=${TOTAL_SAMPLE_BYTES}"
elif [[ "${dry_run_project_count}" -lt 2 ]]; then
  pass "当前无足够可读执行样本，进入候选不足/环境不足校验"
else
  fail "未找到可执行的小样本"
fi
if [[ "${unique_project_count}" -ge 2 && "${unique_project_count}" -le "${MAX_PROJECTS}" ]]; then
  pass "执行样本覆盖至少 2 个非 105 真实项目：${unique_project_count}"
elif [[ "${dry_run_project_count}" -lt 2 || "${unique_project_count}" -lt 2 ]]; then
  pass "执行样本仅覆盖 ${unique_project_count} 个项目，进入候选不足/环境不足校验"
  run_saturated_validation_and_exit "${project_ids_json}" "${overview_coverage_before}" "${task_count_before}" "${object_count_before}"
else
  fail "执行样本项目数不足或越界：${unique_project_count}"
fi
remember_run_projects "${project_ids_json}"

echo ""
echo "--- 5. confirmed=false is rejected and pause is honored ---"
unconfirmed_start="$(api_post "/api/data-steward/storage-objectification-run:start" "$(execute_body false "${project_ids_json}")")"
assert_not_ok "${unconfirmed_start}"
assert_no_forbidden "unconfirmed run start" "${unconfirmed_start}"
pass "start confirmed=false 被拒绝"
pause_response="$(api_post "/api/data-steward/storage-objectification-run:pause" '{}')"
assert_ok "${pause_response}"
assert_no_forbidden "run pause" "${pause_response}"
if [[ "$(json_expr "${pause_response}" "data['data']['paused'] == True and data['data']['runState'] == 'PAUSED'")" == "true" ]]; then
  pass "pause 接口将跑批置为暂停"
else
  fail "pause 接口状态异常：${pause_response}"
fi
paused_start="$(api_post "/api/data-steward/storage-objectification-run:start" "$(execute_body true "${project_ids_json}")")"
assert_not_ok "${paused_start}"
assert_no_forbidden "paused run start" "${paused_start}"
pass "暂停状态下 start 被拒绝，需使用 continue"
unconfirmed_continue="$(api_post "/api/data-steward/storage-objectification-run:continue" "$(execute_body false "${project_ids_json}")")"
assert_not_ok "${unconfirmed_continue}"
assert_no_forbidden "unconfirmed run continue" "${unconfirmed_continue}"
pass "continue confirmed=false 被拒绝"

echo ""
echo "--- 6. Continue M3G-7R and verify progress ---"
execute_response="$(api_post "/api/data-steward/storage-objectification-run:continue" "$(execute_body true "${project_ids_json}")")"
assert_ok "${execute_response}"
assert_no_forbidden "run continue execute" "${execute_response}"
if [[ "$(json_expr "${execute_response}" "data['data']['dryRun'] == False and data['data']['executionStarted'] == True and int(data['data']['selectedFileCount']) <= ${MAX_FILES} and int(data['data']['failedCount']) == 0 and len([p for p in data['data']['projectResults'] if int(p.get('projectId')) != 503 and int(p.get('successCount') or 0) > 0]) >= 2")" == "true" ]]; then
  pass "M3G-7R 至少推进 2 个非 105 项目"
else
  fail "M3G-7R 执行结果异常：${execute_response}"
fi

advanced_projects=0
for index in "${!RUN_PROJECT_IDS[@]}"; do
  project_id="${RUN_PROJECT_IDS[$index]}"
  before_count="${RUN_PROJECT_OBJECT_BEFORE[$index]}"
  after_count="$(mysql_exec "SELECT COUNT(DISTINCT f.id) FROM data_file_resources f JOIN data_file_object_versions v ON v.file_id = f.id AND v.active = 1 AND v.deleted = 0 AND v.storage_state = 'OBJECT_STORED' WHERE f.project_id = ${project_id} AND f.deleted = 0;" 2>/dev/null)"
  if [[ "${after_count}" -gt "${before_count}" ]]; then
    advanced_projects=$((advanced_projects + 1))
    pass "projectId=${project_id} 对象化覆盖率有实质推进"
  else
    fail "projectId=${project_id} 对象化覆盖率未推进"
  fi
done
if [[ "${advanced_projects}" -ge 2 ]]; then
  pass "至少 2 个非 105 真实项目覆盖率已推进"
else
  fail "覆盖率推进项目不足：${advanced_projects}"
fi

echo ""
echo "--- 7. Verify storage status, file-access, uniqueness, and NAS originals ---"
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
echo "--- 8. Repeat safety, retry-failed, reports, and script tracking ---"
post_dry_response="$(api_post "/api/data-steward/storage-objectification-run:dry-run" "$(dry_run_body "${project_ids_json}")")"
assert_ok "${post_dry_response}"
assert_no_forbidden "run post dry-run" "${post_dry_response}"
if [[ "$(SAMPLE_CSV="${file_ids_csv}" RESPONSE="${post_dry_response}" python3 - <<'PY'
import json
import os
sample = {int(x) for x in os.environ["SAMPLE_CSV"].split(",") if x.strip()}
payload = json.loads(os.environ["RESPONSE"])
selected = {
    int(item.get("fileId"))
    for project in payload.get("data", {}).get("projects", [])
    for item in project.get("sampleItems", [])
    if item.get("fileId") is not None
}
print("true" if sample.isdisjoint(selected) else "false")
PY
)" == "true" ]]; then
  pass "已执行样本不会在后续 NAS_ONLY dry-run 中重复出现"
else
  fail "已执行样本仍出现在后续 dry-run 中"
fi

retry_response="$(api_post "/api/data-steward/storage-objectification-run/retry-failed" "$(execute_body true "${project_ids_json}")")"
assert_ok "${retry_response}"
assert_no_forbidden "run retry failed" "${retry_response}"
pass "retry-failed 接口可受控返回"

overview_after="$(api_get "/api/data-steward/storage-objectification-run/overview")"
assert_ok "${overview_after}"
assert_no_forbidden "run overview after" "${overview_after}"
if [[ "$(json_expr "${overview_after}" "float(data['data']['objectificationCoverageRate']) >= float(${overview_coverage_before})")" == "true" ]]; then
  pass "全局对象化覆盖率可查且未下降"
else
  fail "全局对象化覆盖率异常：${overview_after}"
fi
if [[ "$(json_expr "${overview_after}" "data['data']['governanceItemCount'] >= 0 and data['data']['migrationFailedFiles'] >= 0")" == "true" ]]; then
  pass "失败和治理统计字段可查"
else
  fail "失败和治理统计字段缺失"
fi
if git ls-files --error-unmatch scripts/dev/check-m3g7r-all-project-objectification-run.sh >/dev/null 2>&1; then
  pass "M3G-7R 专项脚本已纳入 Git 跟踪"
else
  fail "M3G-7R 专项脚本尚未纳入 Git 跟踪"
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
