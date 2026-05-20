#!/usr/bin/env bash
# M1E: 文件管理连续工作体验与后台任务可追踪性收口 smoke
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-2}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"

PASS=0
FAIL=0
TOKEN=""
FILE_ID=""

pass() {
  echo "  [PASS] $1"
  PASS=$((PASS + 1))
}

fail() {
  echo "  [FAIL] $1"
  FAIL=$((FAIL + 1))
}

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "OK", data' <<< "${response}" >/dev/null
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
    r"\bnas://",
    r"\bsmb://",
    r"\bstorage_path\b",
    r"\bstorage_uri\b",
    r"\bstoragePath\b",
    r"\bstorageUri\b",
    r"\braw row\b",
    r"\btoken\b",
    r"\bsecret\b",
    r"\bpassword\b",
    r"\bselect\s+.+\s+from\b",
]
for pattern in patterns:
    assert not re.search(pattern, payload, re.IGNORECASE), f"{label} contains forbidden pattern {pattern}: {payload[:500]}"
PY
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 25 "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 25 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

api_delete() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 25 -X DELETE "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" >/dev/null || true
}

cleanup() {
  if [[ -n "${FILE_ID}" ]]; then
    api_delete "/api/data-steward/projects/${PROJECT_ID}/file-resources/${FILE_ID}"
  fi
}
trap cleanup EXIT

wait_job_failed() {
  local job_id="$1"
  local response status
  for _ in $(seq 1 40); do
    response="$(api_get "/api/data-steward/assets/jobs/${job_id}")"
    assert_ok "${response}"
    status="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["status"])' <<< "${response}")"
    if [[ "${status}" == "FAILED" ]]; then
      echo "${response}"
      return 0
    fi
    sleep 1
  done
  response="$(api_get "/api/data-steward/assets/jobs/${job_id}")"
  echo "${response}"
  return 1
}

echo "--- 1. Static M1E UI contract check ---"
if rg -q "delivery.dataSteward.fileBrowser.state" frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue \
  && rg -q "data-m1e-continuity-bar" frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue \
  && rg -q "重置视图" frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue; then
  pass "文件管理存在项目级状态记忆与重置入口"
else
  fail "文件管理状态记忆或重置入口缺失"
fi

if rg -q "data-m1e-checksum-jobs" frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue \
  && rg -q "后台任务编号" frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue \
  && rg -q "平台文件ID" frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue; then
  pass "项目文件管理页存在 checksum 任务追踪区和业务化 ID 文案"
else
  fail "checksum 任务追踪区或业务化 ID 文案缺失"
fi

if rg -q "AssetJobResponseSanitizer" backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application \
  && rg -q "path_not_exposable" backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetJobResponseSanitizer.java; then
  pass "后端任务响应存在脱敏保护"
else
  fail "后端任务响应脱敏保护缺失"
fi

echo "--- 2. Login and switch project ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 25 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${login_response}")"
pass "管理员登录成功"

switch_response="$(curl -sS --connect-timeout 3 --max-time 25 -X POST "${BASE_URL}/api/core/projects/${PROJECT_ID}:switch" \
  -H "Authorization: Bearer ${TOKEN}")"
assert_ok "${switch_response}"
TOKEN="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${switch_response}")"
pass "项目切换成功"

echo "--- 3. Create isolated checksum failure fixture ---"
missing_uri="nas:///tmp/m1e-missing-${RUN_ID}.rvt"
file_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-resources" \
  "{\"originalName\":\"M1E-checksum-visible-${RUN_ID}.rvt\",\"fileKind\":\"MODEL\",\"mimeType\":\"application/octet-stream\",\"sizeBytes\":128,\"storageUri\":\"${missing_uri}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
assert_ok "${file_response}"
FILE_ID="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${file_response}")"
pass "隔离文件资产已创建"

job_response="$(api_post "/api/data-steward/assets/checksum-jobs" "{\"fileId\":${FILE_ID}}")"
assert_ok "${job_response}"
assert_no_forbidden "create checksum job response" "${job_response}"
JOB_ID="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${job_response}")"
pass "checksum 后台任务创建响应已脱敏"

echo "--- 4. Verify failed job is trackable and sanitized ---"
failed_job="$(wait_job_failed "${JOB_ID}")"
assert_ok "${failed_job}"
assert_no_forbidden "failed checksum job response" "${failed_job}"
parse_json '
import json,sys
job=json.load(sys.stdin)["data"]
assert job["id"], job
assert job["targetId"], job
assert job["status"] == "FAILED", job
assert "底层路径已隐藏" in (job.get("failureReason") or ""), job
assert "storagePath" not in str(job.get("requestPayload") or ""), job
' <<< "${failed_job}" >/dev/null
pass "失败任务可追踪且失败原因已脱敏"

jobs_response="$(api_get "/api/data-steward/assets/jobs?projectId=${PROJECT_ID}&jobType=CHECKSUM_CALC&limit=20")"
assert_ok "${jobs_response}"
assert_no_forbidden "checksum job list response" "${jobs_response}"
JOB_ID="${JOB_ID}" parse_json '
import json, os, sys
job_id=int(os.environ["JOB_ID"])
jobs=json.load(sys.stdin)["data"]
assert any(j["id"] == job_id and j["targetId"] for j in jobs), jobs
' <<< "${jobs_response}" >/dev/null
pass "项目内 checksum 任务列表可见且绑定平台文件ID"

echo "--- 5. Retry failed job ---"
retry_response="$(api_post "/api/data-steward/assets/jobs/${JOB_ID}:retry" "{}")"
assert_ok "${retry_response}"
retry_job="$(api_get "/api/data-steward/assets/jobs/${JOB_ID}")"
assert_ok "${retry_job}"
assert_no_forbidden "retried checksum job response" "${retry_job}"
parse_json '
import json,sys
job=json.load(sys.stdin)["data"]
assert job["status"] in ("PENDING", "RUNNING", "FAILED"), job
assert job["retryCount"] >= 1, job
' <<< "${retry_job}" >/dev/null
pass "失败任务可重试且响应不泄露路径"

echo "--- 6. Summary ---"
printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
