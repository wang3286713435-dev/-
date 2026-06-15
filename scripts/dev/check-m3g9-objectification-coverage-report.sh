#!/usr/bin/env bash
# M3G-9: all-project objectification coverage report and closure evidence.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"

PASS=0
FAIL=0
TOKEN=""

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

assert_data() {
  local label="$1"
  local response="$2"
  local expr="$3"
  if RESPONSE="${response}" python3 - "${expr}" <<'PY'
import json
import os
import sys
payload = json.loads(os.environ["RESPONSE"])
data = payload.get("data")
scope = {"data": data, "payload": payload, "len": len, "any": any, "all": all}
assert eval(sys.argv[1], scope, scope), payload
PY
  then
    pass "${label}"
  else
    fail "${label}"
  fi
}

assert_no_forbidden() {
  local label="$1"
  local payload="$2"
  if PAYLOAD="${payload}" python3 - <<'PY'
import os
import re
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
]
for pattern in patterns:
    if re.search(pattern, payload, flags=re.IGNORECASE | re.DOTALL):
        raise AssertionError(pattern)
PY
  then
    pass "${label}"
  else
    fail "${label}"
  fi
}

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" python3 - "${expr}" <<'PY'
import json
import os
import sys
payload = json.loads(os.environ["RESPONSE"])
data = payload.get("data")
scope = {"data": data, "payload": payload, "len": len, "any": any, "all": all}
value = eval(sys.argv[1], scope, scope)
if isinstance(value, (dict, list)):
    print(json.dumps(value, ensure_ascii=False))
elif value is None:
    print("")
else:
    print(value)
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
  curl -sS --connect-timeout 3 --max-time 60 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

login() {
  local login_response switch_response
  login_response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${login_response}"
  TOKEN="$(json_data_expr "${login_response}" "data['accessToken']")"
  switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch" '{}')"
  assert_ok "${switch_response}"
  TOKEN="$(json_data_expr "${switch_response}" "data['accessToken']")"
}

task_count() {
  mysql_exec "SELECT COUNT(1) FROM data_object_migration_tasks WHERE deleted = 0;" 2>/dev/null | head -n 1
}

printf 'M3G-9 objectification coverage report smoke\n'
printf 'Base URL: %s\n' "${BASE_URL}"

login
pass "管理员登录并切换到 ${PROJECT_ID}"

TASKS_BEFORE="$(task_count)"
COVERAGE_RESPONSE="$(api_get "/api/data-steward/storage-objectification-coverage")"
assert_ok "${COVERAGE_RESPONSE}" && pass "全项目覆盖率接口返回统一 OK 与 traceId" || fail "全项目覆盖率接口失败"

assert_data "报告为 dry-run / 只读口径" "${COVERAGE_RESPONSE}" "data['dryRun'] is True and data['reportCode'] == 'M3G-9'"
assert_data "平台级汇总字段完整" "${COVERAGE_RESPONSE}" "all(k in data['summary'] for k in ['totalProjects','completedProjects','partialProjects','nasOnlyProjects','failedOrGovernanceProjects','totalFiles','objectStoredFiles','nasOnlyFiles','failedFiles','overallObjectificationRate','totalSizeBytes','objectStoredSizeBytes','checksumCoverageRate'])"
assert_data "收口判断字段完整" "${COVERAGE_RESPONSE}" "all(k in data['closureAssessment'] for k in ['m3ClosureReady','blockingReasons','warnings','nextActions'])"
assert_data "项目列表包含 105 / projectId=503" "${COVERAGE_RESPONSE}" "any(int(p['projectId']) == 503 for p in data['projects'])"
assert_data "105 显示 2928/2928 或等价 100% COMPLETED" "${COVERAGE_RESPONSE}" "any(int(p['projectId']) == 503 and int(p['totalFiles']) >= 2928 and int(p['objectStoredCount']) >= 2928 and p['status'] == 'COMPLETED' and float(p['objectificationCoverageRate']) >= 100 for p in data['projects'])"
assert_data "非 105 项目状态可解释" "${COVERAGE_RESPONSE}" "any(int(p['projectId']) != 503 and p['status'] in ['COMPLETED','PARTIAL','NAS_ONLY','FAILED_NEEDS_GOVERNANCE','EXCLUDED'] and p['readStrategySummary'] in ['OBJECT_FIRST','MIXED','LEGACY_NAS','EXCLUDED'] for p in data['projects'])"
assert_data "项目行包含覆盖率、安全状态和失败分组字段" "${COVERAGE_RESPONSE}" "all(all(k in p for k in ['projectId','projectCode','projectName','projectCategory','onboardingStatus','totalFiles','objectStoredCount','nasOnlyCount','migrationFailedCount','governanceCount','unreadableCount','checksumCoverageRate','objectificationCoverageRate','lastObjectifiedAt','readStrategySummary','status','failureSummary']) for p in data['projects'])"
assert_no_forbidden "覆盖率报告响应不泄露禁出字段" "${COVERAGE_RESPONSE}"

TASKS_AFTER="$(task_count)"
if [[ "${TASKS_BEFORE}" == "${TASKS_AFTER}" ]]; then
  pass "报告查询未创建迁移任务"
else
  fail "报告查询创建了迁移任务：before=${TASKS_BEFORE} after=${TASKS_AFTER}"
fi

printf '\nM3G-9 coverage smoke result: PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
