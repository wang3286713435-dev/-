#!/usr/bin/env bash
# M3X-Q: persistent background objectification queue smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
REGULAR_USER="${REGULAR_USER:-m3xq.queue.viewer}"
REGULAR_PASSWORD="${REGULAR_PASSWORD:-QueueViewer@123}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
MAX_TOTAL_FILES="${MAX_TOTAL_FILES:-3}"
MAX_FILES_PER_TICK="${MAX_FILES_PER_TICK:-1}"
MAX_BYTES_PER_TICK="${MAX_BYTES_PER_TICK:-10485760}"
MAX_FILE_SIZE_BYTES="${MAX_FILE_SIZE_BYTES:-10485760}"
MAX_TOTAL_BYTES="${MAX_TOTAL_BYTES:-31457280}"

PASS=0
FAIL=0
TOKEN=""
REGULAR_TOKEN=""
JOB_ID=""
SELECTED_PROJECT_IDS=()
SAMPLE_IDS=()
SAMPLE_STATS_BEFORE=()
HAS_READABLE_SAMPLE=false
FALLBACK_PROJECT_ID=""

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

assert_code() {
  local response="$1"
  local expected="$2"
  RESPONSE="${response}" EXPECTED="${expected}" python3 - <<'PY'
import json
import os

data = json.loads(os.environ["RESPONSE"])
assert data.get("code") == os.environ["EXPECTED"], data
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
    r"secret",
    r"password",
    r"access[_-]?key",
]
for pattern in patterns:
    if re.search(pattern, payload, re.IGNORECASE | re.DOTALL):
        raise SystemExit(f"{label} leaked forbidden pattern: {pattern}")
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

api_post_with_token() {
  local token="$1"
  local url="$2"
  local body="${3:-{}}"
  curl -sS --connect-timeout 3 --max-time 300 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

api_get_with_token() {
  local token="$1"
  local url="$2"
  curl -sS --connect-timeout 3 --max-time 120 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}"
}

login_user() {
  local username="$1"
  local password="$2"
  local response
  response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${username}\",\"password\":\"${password}\"}")"
  assert_ok "${response}"
  json_expr "${response}" "data['data']['accessToken']"
}

prepare_regular_user() {
  local project_id="$1"
  mysql_exec "INSERT INTO core_users (username, password_hash, display_name, status)
VALUES ('${REGULAR_USER}', '{noop}${REGULAR_PASSWORD}', 'M3X-Q 队列权限验证用户', 'ACTIVE')
ON DUPLICATE KEY UPDATE password_hash='{noop}${REGULAR_PASSWORD}', display_name='M3X-Q 队列权限验证用户', status='ACTIVE', deleted=0;" >/dev/null 2>&1
  mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${project_id}, r.id
FROM core_users u
JOIN core_roles r ON r.code = 'PROJECT_VIEWER'
WHERE u.username = '${REGULAR_USER}'
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), deleted=0;" >/dev/null 2>&1
}

assert_regular_forbidden() {
  local label="$1"
  local response="$2"
  assert_not_ok "${response}"
  assert_code "${response}" "STORAGE_OBJECTIFICATION_QUEUE_FORBIDDEN"
  assert_no_forbidden "${label}" "${response}"
  pass "${label} 被超级管理员门禁拒绝"
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
  local storage_uri nas_path
  storage_uri="$(storage_uri_for_file "${file_id}")"
  [[ -z "${storage_uri}" ]] && return 1
  nas_path="$(nas_path_from_uri "${storage_uri}")"
  [[ -f "${nas_path}" && -r "${nas_path}" ]] || return 1
  SELECTED_PROJECT_IDS+=("${project_id}")
  SAMPLE_IDS+=("${file_id}")
  SAMPLE_STATS_BEFORE+=("$(file_stat_signature "${nas_path}")")
}

unique_project_ids_json() {
  local csv=""
  if [[ "${#SELECTED_PROJECT_IDS[@]}" -gt 0 ]]; then
    local IFS=,
    csv="${SELECTED_PROJECT_IDS[*]}"
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

queue_body() {
  local confirmed="$1"
  PROJECT_IDS="$(unique_project_ids_json)" \
  CONFIRMED="${confirmed}" \
  MAX_TOTAL_FILES_VALUE="${MAX_TOTAL_FILES}" \
  MAX_TOTAL_BYTES_VALUE="${MAX_TOTAL_BYTES}" \
  MAX_FILES_PER_TICK_VALUE="${MAX_FILES_PER_TICK}" \
  MAX_BYTES_PER_TICK_VALUE="${MAX_BYTES_PER_TICK}" \
  MAX_FILE_SIZE_VALUE="${MAX_FILE_SIZE_BYTES}" \
  python3 - <<'PY'
import json
import os

body = {
    "jobName": "M3X-Q smoke background queue",
    "scopeType": "SELECTED_PROJECTS",
    "projectIds": json.loads(os.environ["PROJECT_IDS"]),
    "maxTotalFiles": int(os.environ["MAX_TOTAL_FILES_VALUE"]),
    "maxTotalBytes": int(os.environ["MAX_TOTAL_BYTES_VALUE"]),
    "maxFilesPerTick": int(os.environ["MAX_FILES_PER_TICK_VALUE"]),
    "maxBytesPerTick": int(os.environ["MAX_BYTES_PER_TICK_VALUE"]),
    "maxFileSizeBytes": int(os.environ["MAX_FILE_SIZE_VALUE"]),
    "continueOnFailure": True,
    "confirmed": os.environ["CONFIRMED"].lower() == "true",
    "targetProvider": "MINIO",
}
print(json.dumps(body, ensure_ascii=False))
PY
}

dry_run_body() {
  PROJECT_IDS="$(unique_project_ids_json)" \
  python3 - <<PY
import json
import os

body = {
    "jobName": "M3X-Q smoke dry-run",
    "scopeType": "SELECTED_PROJECTS",
    "projectIds": json.loads(os.environ["PROJECT_IDS"]),
    "maxTotalFiles": ${MAX_TOTAL_FILES},
    "maxTotalBytes": ${MAX_TOTAL_BYTES},
    "maxFilesPerTick": ${MAX_FILES_PER_TICK},
    "maxBytesPerTick": ${MAX_BYTES_PER_TICK},
    "maxFileSizeBytes": ${MAX_FILE_SIZE_BYTES},
    "continueOnFailure": True,
    "confirmed": False,
    "targetProvider": "MINIO",
}
print(json.dumps(body, ensure_ascii=False))
PY
}

legacy_run_body() {
  local confirmed="$1"
  CONFIRMED="${confirmed}" python3 - <<'PY'
import json
import os

body = {
    "maxProjects": 1,
    "maxTotalFiles": 1,
    "maxFilesPerProject": 1,
    "maxTotalBytes": 1048576,
    "maxBytesPerProject": 1048576,
    "maxFileSizeBytes": 1048576,
    "maxContinuousBatches": 1,
    "continueOnFailure": True,
    "confirmed": os.environ["CONFIRMED"].lower() == "true",
    "targetProvider": "MINIO",
}
print(json.dumps(body, ensure_ascii=False))
PY
}

discover_readable_samples() {
  local candidates project_id file_id size_bytes
  candidates="$(mysql_exec "SELECT f.project_id, f.id, COALESCE(f.size_bytes, 0) AS size_bytes
FROM data_file_resources f
LEFT JOIN data_file_object_versions fov
  ON fov.file_id = f.id
 AND fov.active = 1
 AND fov.deleted = 0
 AND fov.storage_state = 'OBJECT_STORED'
WHERE f.deleted = 0
  AND f.project_id NOT IN (1, 503)
  AND fov.id IS NULL
  AND f.storage_uri LIKE 'nas:///Volumes/%'
  AND COALESCE(f.size_bytes, 0) BETWEEN 1 AND ${MAX_FILE_SIZE_BYTES}
ORDER BY f.project_id, f.id
LIMIT 500;" 2>/dev/null)"
  while IFS=$'\t' read -r project_id file_id size_bytes; do
    [[ -z "${project_id:-}" || -z "${file_id:-}" || -z "${size_bytes:-}" ]] && continue
    [[ "${#SAMPLE_IDS[@]}" -ge "${MAX_TOTAL_FILES}" ]] && break
    if [[ "${#SELECTED_PROJECT_IDS[@]}" -gt 0 && "${project_id}" != "${SELECTED_PROJECT_IDS[0]}" ]]; then
      continue
    fi
    if remember_sample_if_readable "${project_id}" "${file_id}"; then
      pass "已记录可读队列样本 fileId=${file_id}"
    fi
  done <<< "${candidates}"
}

first_overview_project_id() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os

payload = json.loads(os.environ["RESPONSE"])
projects = payload.get("data", {}).get("projects", [])
for project in projects:
    project_id = project.get("projectId")
    if project_id:
        print(project_id)
        break
PY
}

job_processed_files() {
  local response="$1"
  json_expr "${response}" "int(data['data']['job']['processedFiles'] or 0)"
}

job_status() {
  local response="$1"
  json_expr "${response}" "data['data']['job']['jobStatus']"
}

poll_job_until_processed() {
  local job_id="$1"
  local min_processed="$2"
  local attempts="${3:-30}"
  local response processed status
  for _ in $(seq 1 "${attempts}"); do
    response="$(api_get "/api/data-steward/storage-objectification-queue/jobs/${job_id}")"
    assert_ok "${response}"
    assert_no_forbidden "queue job ${job_id}" "${response}"
    processed="$(job_processed_files "${response}")"
    status="$(job_status "${response}")"
    if [[ "${processed}" -ge "${min_processed}" || "${status}" == "COMPLETED" || "${status}" == "FAILED" ]]; then
      printf '%s' "${response}"
      return 0
    fi
    sleep 1
  done
  return 1
}

poll_job_until_terminal() {
  local job_id="$1"
  local attempts="${2:-60}"
  local response status
  for _ in $(seq 1 "${attempts}"); do
    response="$(api_get "/api/data-steward/storage-objectification-queue/jobs/${job_id}")"
    assert_ok "${response}"
    assert_no_forbidden "queue terminal job ${job_id}" "${response}"
    status="$(job_status "${response}")"
    if [[ "${status}" == "COMPLETED" || "${status}" == "FAILED" || "${status}" == "CANCELED" ]]; then
      printf '%s' "${response}"
      return 0
    fi
    sleep 1
  done
  return 1
}

first_success_file_id() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os

payload = json.loads(os.environ["RESPONSE"])
for item in payload.get("data", {}).get("items", []):
    if item.get("itemStatus") in ("OBJECT_STORED", "SKIPPED") and item.get("objectStored"):
        print(item.get("fileId"))
        break
PY
}

create_download_ticket_and_read() {
  local file_id="$1"
  local response access_url probe
  response="$(api_post "/api/data-steward/assets/files/${file_id}/access-tickets" '{"action":"DOWNLOAD"}')"
  assert_ok "${response}"
  access_url="$(json_expr "${response}" "data['data']['accessUrl']")"
  probe="$(curl -sS --connect-timeout 3 --max-time 120 -o /dev/null -w '%{http_code}:%{size_download}' "${BASE_URL}${access_url}")"
  [[ "${probe}" == 200:* && "${probe##*:}" -gt 0 ]]
}

echo "=== M3X-Q: objectification background queue ==="

echo ""
echo "--- 1. Login and readiness ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
pass "管理员登录成功"

overview_response="$(api_get "/api/data-steward/storage-objectification-queue/overview")"
assert_ok "${overview_response}"
assert_no_forbidden "queue overview" "${overview_response}"
if [[ "$(json_expr "${overview_response}" "data['data']['queueCode'] == 'M3X-Q' and data['data']['totalFiles'] >= data['data']['objectStoredFiles']")" == "true" ]]; then
  pass "对象化后台队列总览可查询"
else
  fail "对象化后台队列总览异常：${overview_response}"
fi

readiness_response="$(api_get "/api/data-steward/storage-provider-readiness")"
assert_ok "${readiness_response}"
assert_no_forbidden "storage readiness" "${readiness_response}"
if [[ "$(json_expr "${readiness_response}" "data['data']['endpointType'] == 'NAS_SIDE_MINIO' and data['data']['readinessStatus'] == 'READY' and data['data']['writable'] == True")" == "true" ]]; then
  pass "NAS_SIDE_MINIO 已识别且 READY"
else
  fail "NAS_SIDE_MINIO 尚未 READY：${readiness_response}"
fi

echo ""
echo "--- 2. Discover readable smoke samples ---"
discover_readable_samples
if [[ "${#SAMPLE_IDS[@]}" -ge 1 ]]; then
  HAS_READABLE_SAMPLE=true
  pass "已为 NAS 原文件不变校验记录 size/mtime，projectIds=$(unique_project_ids_json)"
else
  FALLBACK_PROJECT_ID="$(first_overview_project_id "${overview_response}")"
  if [[ -n "${FALLBACK_PROJECT_ID}" ]]; then
    SELECTED_PROJECT_IDS+=("${FALLBACK_PROJECT_ID}")
    pass "未找到本机可读 NAS_ONLY 小样本，进入候选不足/对象化饱和安全分支"
  else
    fail "未找到本机可读 NAS_ONLY 小样本，且队列总览没有可用于权限校验的项目"
  fi
fi

echo ""
echo "--- 3. Non-admin queue access must be forbidden ---"
prepare_regular_user "${SELECTED_PROJECT_IDS[0]}"
REGULAR_TOKEN="$(login_user "${REGULAR_USER}" "${REGULAR_PASSWORD}")"
pass "普通测试用户已准备并登录"

regular_overview_response="$(api_get_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-queue/overview" || true)"
assert_regular_forbidden "普通用户查看后台队列总览" "${regular_overview_response}"

regular_list_response="$(api_get_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-queue/jobs" || true)"
assert_regular_forbidden "普通用户查看后台队列列表" "${regular_list_response}"

regular_dry_run_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-queue/jobs:dry-run" "$(dry_run_body)" || true)"
assert_regular_forbidden "普通用户 dry-run 后台队列" "${regular_dry_run_response}"

regular_create_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-queue/jobs" "$(queue_body true)" || true)"
assert_regular_forbidden "普通用户创建后台队列" "${regular_create_response}"

echo ""
echo "--- 4. Dry-run must not write queue or object rows ---"
jobs_before="$(mysql_exec "SELECT COUNT(1) FROM data_objectification_jobs WHERE deleted = 0;" 2>/dev/null)"
items_before="$(mysql_exec "SELECT COUNT(1) FROM data_objectification_job_items WHERE deleted = 0;" 2>/dev/null)"
objects_before="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE active = 1 AND deleted = 0;" 2>/dev/null)"
dry_response="$(api_post "/api/data-steward/storage-objectification-queue/jobs:dry-run" "$(dry_run_body)")"
assert_ok "${dry_response}"
assert_no_forbidden "queue dry-run" "${dry_response}"
jobs_after="$(mysql_exec "SELECT COUNT(1) FROM data_objectification_jobs WHERE deleted = 0;" 2>/dev/null)"
items_after="$(mysql_exec "SELECT COUNT(1) FROM data_objectification_job_items WHERE deleted = 0;" 2>/dev/null)"
objects_after="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE active = 1 AND deleted = 0;" 2>/dev/null)"
if [[ "${jobs_before}" == "${jobs_after}" && "${items_before}" == "${items_after}" && "${objects_before}" == "${objects_after}" ]]; then
  pass "dry-run 未创建 job/item，也未新增对象版本"
else
  fail "dry-run 出现写入：jobs ${jobs_before}->${jobs_after}, items ${items_before}->${items_after}, objects ${objects_before}->${objects_after}"
fi

if [[ "${HAS_READABLE_SAMPLE}" == "true" && "$(json_expr "${dry_response}" "data['data']['dryRun'] == True and data['data']['jobCreated'] == False and int(data['data']['selectedFileCount']) > 0 and int(data['data']['selectedFileCount']) <= ${MAX_TOTAL_FILES}")" == "true" ]]; then
  pass "dry-run 生成小批队列计划"
elif [[ "${HAS_READABLE_SAMPLE}" != "true" && "$(json_expr "${dry_response}" "data['data']['dryRun'] == True and data['data']['jobCreated'] == False and int(data['data']['selectedFileCount']) >= 0")" == "true" ]]; then
  pass "候选不足/对象化饱和时 dry-run 仍不写入"
else
  fail "dry-run 未生成可执行小批计划：${dry_response}"
fi

if [[ "${HAS_READABLE_SAMPLE}" != "true" ]]; then
  echo ""
  echo "--- 5. Saturated environment safe exit ---"
  overview_after="$(api_get "/api/data-steward/storage-objectification-queue/overview")"
  assert_ok "${overview_after}"
  assert_no_forbidden "queue overview saturated" "${overview_after}"
  pass "候选不足/对象化饱和环境下未创建后台任务，队列总览仍可查询"

  if git ls-files --error-unmatch scripts/dev/check-m3xq-objectification-background-queue.sh >/dev/null 2>&1; then
    pass "M3X-Q 专项脚本已纳入 Git 跟踪"
  else
    fail "M3X-Q 专项脚本尚未纳入 Git 跟踪"
  fi

  echo ""
  echo "--- M3X-Q summary ---"
  printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
  if [[ "${FAIL}" -ne 0 ]]; then
    exit 1
  fi
  exit 0
fi

echo ""
echo "--- 5. Confirm gate and queue creation ---"
unconfirmed_response="$(api_post "/api/data-steward/storage-objectification-queue/jobs" "$(queue_body false)" || true)"
assert_not_ok "${unconfirmed_response}"
assert_no_forbidden "queue create without confirmed" "${unconfirmed_response}"
pass "confirmed=false 创建后台任务被拒绝"

create_response="$(api_post "/api/data-steward/storage-objectification-queue/jobs" "$(queue_body true)")"
assert_ok "${create_response}"
assert_no_forbidden "queue create" "${create_response}"
JOB_ID="$(json_expr "${create_response}" "int(data['data']['job']['jobId'])")"
if [[ "$(json_expr "${create_response}" "data['data']['job']['jobStatus'] == 'QUEUED' and int(data['data']['job']['totalFiles']) > 0 and int(data['data']['job']['maxFilesPerTick']) == ${MAX_FILES_PER_TICK}")" == "true" ]]; then
  pass "已创建后台对象化 job=${JOB_ID}"
else
  fail "后台对象化任务创建异常：${create_response}"
fi

echo ""
echo "--- 6. Non-admin queue controls must be forbidden ---"
regular_detail_response="$(api_get_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-queue/jobs/${JOB_ID}" || true)"
assert_regular_forbidden "普通用户查看后台队列详情" "${regular_detail_response}"

regular_pause_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-queue/jobs/${JOB_ID}:pause" '{}' || true)"
assert_regular_forbidden "普通用户暂停后台队列" "${regular_pause_response}"

regular_resume_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-queue/jobs/${JOB_ID}:resume" '{}' || true)"
assert_regular_forbidden "普通用户继续后台队列" "${regular_resume_response}"

regular_retry_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-queue/jobs/${JOB_ID}:retry-failed" '{}' || true)"
assert_regular_forbidden "普通用户重试后台队列" "${regular_retry_response}"

regular_cancel_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-queue/jobs/${JOB_ID}:cancel" '{}' || true)"
assert_regular_forbidden "普通用户取消后台队列" "${regular_cancel_response}"

echo ""
echo "--- 7. Non-admin legacy objectification-run access must be forbidden ---"
regular_run_overview_response="$(api_get_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-run/overview" || true)"
assert_regular_forbidden "普通用户查看旧全项目跑批总览" "${regular_run_overview_response}"

regular_run_projects_response="$(api_get_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-run/projects" || true)"
assert_regular_forbidden "普通用户查看旧全项目跑批项目列表" "${regular_run_projects_response}"

regular_run_dry_run_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-run:dry-run" "$(legacy_run_body false)" || true)"
assert_regular_forbidden "普通用户 dry-run 旧全项目跑批" "${regular_run_dry_run_response}"

regular_run_start_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-run:start" "$(legacy_run_body true)" || true)"
assert_regular_forbidden "普通用户开始旧全项目跑批" "${regular_run_start_response}"

regular_run_continue_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-run:continue" "$(legacy_run_body true)" || true)"
assert_regular_forbidden "普通用户继续旧全项目跑批" "${regular_run_continue_response}"

regular_run_pause_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-run:pause" '{}' || true)"
assert_regular_forbidden "普通用户暂停旧全项目跑批" "${regular_run_pause_response}"

regular_run_retry_response="$(api_post_with_token "${REGULAR_TOKEN}" "/api/data-steward/storage-objectification-run/retry-failed" "$(legacy_run_body true)" || true)"
assert_regular_forbidden "普通用户重试旧全项目跑批" "${regular_run_retry_response}"

echo ""
echo "--- 8. Worker progress, pause, resume, retry ---"
progress_response="$(poll_job_until_processed "${JOB_ID}" 1 35)"
processed_after_worker="$(job_processed_files "${progress_response}")"
if [[ "${processed_after_worker}" -ge 1 ]]; then
  pass "后台 worker 已自动处理至少 1 个队列项"
else
  fail "后台 worker 未处理队列项"
fi

pause_response="$(api_post "/api/data-steward/storage-objectification-queue/jobs/${JOB_ID}:pause" '{}')"
assert_ok "${pause_response}"
assert_no_forbidden "queue pause" "${pause_response}"
if [[ "$(job_status "${pause_response}")" == "PAUSED" || "$(job_status "${pause_response}")" == "COMPLETED" ]]; then
  pass "暂停接口可用"
else
  fail "暂停后状态异常：${pause_response}"
fi

paused_processed="$(job_processed_files "${pause_response}")"
sleep 5
paused_check_response="$(api_get "/api/data-steward/storage-objectification-queue/jobs/${JOB_ID}")"
assert_ok "${paused_check_response}"
assert_no_forbidden "queue paused check" "${paused_check_response}"
paused_check_status="$(job_status "${paused_check_response}")"
paused_check_processed="$(job_processed_files "${paused_check_response}")"
if [[ "${paused_check_status}" == "COMPLETED" || "${paused_check_processed}" == "${paused_processed}" ]]; then
  pass "暂停后队列未继续推进"
else
  fail "暂停后队列仍在推进：${paused_processed}->${paused_check_processed}"
fi

resume_response="$(api_post "/api/data-steward/storage-objectification-queue/jobs/${JOB_ID}:resume" '{}')"
assert_ok "${resume_response}"
assert_no_forbidden "queue resume" "${resume_response}"
if [[ "$(job_status "${resume_response}")" == "QUEUED" || "$(job_status "${resume_response}")" == "COMPLETED" ]]; then
  pass "继续接口可用"
else
  fail "继续后状态异常：${resume_response}"
fi

retry_response="$(api_post "/api/data-steward/storage-objectification-queue/jobs/${JOB_ID}:retry-failed" '{}')"
assert_ok "${retry_response}"
assert_no_forbidden "queue retry failed" "${retry_response}"
pass "失败项重试接口可调用"

final_response="$(poll_job_until_terminal "${JOB_ID}" 60)"
final_status="$(job_status "${final_response}")"
if [[ "${final_status}" == "COMPLETED" || "${final_status}" == "FAILED" ]]; then
  pass "队列任务进入终态：${final_status}"
else
  fail "队列任务未进入终态：${final_response}"
fi

echo ""
echo "--- 9. Object-first read and NAS original immutability ---"
success_file_id="$(first_success_file_id "${final_response}")"
if [[ -n "${success_file_id}" ]]; then
  status_response="$(api_get "/api/data-steward/assets/files/${success_file_id}/storage-status")"
  assert_ok "${status_response}"
  assert_no_forbidden "queue objectified storage status" "${status_response}"
  if [[ "$(json_expr "${status_response}" "data['data']['storageState'] == 'OBJECT_STORED' and data['data']['objectStored'] == True")" == "true" ]]; then
    pass "队列对象化文件 storage-status=OBJECT_STORED"
  else
    fail "对象化文件 storage-status 异常：${status_response}"
  fi
  if create_download_ticket_and_read "${success_file_id}"; then
    pass "队列对象化文件可通过受控 file-access 读取"
  else
    fail "队列对象化文件受控读取失败"
  fi
else
  fail "未找到成功对象化文件，无法验证 file-access"
fi

for index in "${!SAMPLE_IDS[@]}"; do
  file_id="${SAMPLE_IDS[$index]}"
  storage_uri="$(storage_uri_for_file "${file_id}")"
  nas_path="$(nas_path_from_uri "${storage_uri}")"
  if [[ -f "${nas_path}" && "$(file_stat_signature "${nas_path}")" == "${SAMPLE_STATS_BEFORE[$index]}" ]]; then
    pass "fileId=${file_id} NAS 原文件 size/mtime 未变化"
  else
    fail "fileId=${file_id} NAS 原文件状态变化"
  fi
done

echo ""
echo "--- 10. Queue overview and script tracking ---"
overview_after="$(api_get "/api/data-steward/storage-objectification-queue/overview")"
assert_ok "${overview_after}"
assert_no_forbidden "queue overview after" "${overview_after}"
if [[ "$(json_expr "${overview_after}" "data['data']['completedJobCount'] >= 1 or data['data']['failedJobCount'] >= 1")" == "true" ]]; then
  pass "队列总览可展示任务进度与终态"
else
  fail "队列总览未反映任务终态：${overview_after}"
fi

if git ls-files --error-unmatch scripts/dev/check-m3xq-objectification-background-queue.sh >/dev/null 2>&1; then
  pass "M3X-Q 专项脚本已纳入 Git 跟踪"
else
  fail "M3X-Q 专项脚本尚未纳入 Git 跟踪"
fi

echo ""
echo "--- M3X-Q summary ---"
printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"

if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
