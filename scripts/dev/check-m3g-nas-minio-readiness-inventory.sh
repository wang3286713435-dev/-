#!/usr/bin/env bash
# M3G-1: NAS-side MinIO readiness, objectification inventory, and dry-run plan.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"

PASS=0
FAIL=0
ADMIN_TOKEN=""

pass() {
  PASS=$((PASS + 1))
  printf '  [PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '  [FAIL] %s\n' "$1" >&2
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
  curl -sS --connect-timeout 3 --max-time 45 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}"
}

api_post() {
  local url="$1"
  local body="${2:-{}}"
  curl -sS --connect-timeout 3 --max-time 45 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

login() {
  local response
  response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${response}"
  json_expr "${response}" "data['data']['accessToken']"
}

echo "=== M3G-1: NAS-side MinIO readiness and objectification inventory ==="

echo "--- 1. Login administrator ---"
ADMIN_TOKEN="$(login)"
if [[ -n "${ADMIN_TOKEN}" ]]; then
  pass "管理员登录成功"
else
  fail "管理员登录失败"
fi

echo "--- 2. Readiness is business-level and sanitized ---"
readiness_response="$(api_get "/api/data-steward/storage-provider-readiness")"
assert_ok "${readiness_response}"
if assert_no_forbidden "readiness" "${readiness_response}" \
  && [[ "$(json_expr "${readiness_response}" "data['data']['providerCode']")" == "MINIO" ]] \
  && [[ -n "$(json_expr "${readiness_response}" "data['data']['endpointType']")" ]] \
  && [[ -n "$(json_expr "${readiness_response}" "data['data']['readinessStatus']")" ]]; then
  pass "readiness 返回 provider / endpointType / readinessStatus 且不泄露底层配置"
else
  fail "readiness 响应不符合要求"
fi

endpoint_type="$(json_expr "${readiness_response}" "data['data']['endpointType']")"
readiness_status="$(json_expr "${readiness_response}" "data['data']['readinessStatus']")"
if [[ "${endpoint_type}" == "NAS_SIDE_MINIO" || "${endpoint_type}" == "LOCAL_DEV_MINIO" || "${endpoint_type}" == "UNKNOWN" ]]; then
  pass "endpointType 可区分 NAS 侧、本机开发或待确认：${endpoint_type}/${readiness_status}"
else
  fail "endpointType 非预期：${endpoint_type}"
fi

echo "--- 3. All-project inventory is available and sanitized ---"
inventory_response="$(api_get "/api/data-steward/storage-objectification-inventory")"
assert_ok "${inventory_response}"
if assert_no_forbidden "inventory" "${inventory_response}" \
  && [[ "$(json_expr "${inventory_response}" "data['data']['allProjects']")" == "true" ]] \
  && [[ "$(json_expr "${inventory_response}" "len(data['data']['projects']) > 0")" == "true" ]]; then
  pass "全项目对象化盘点可查且不泄露底层路径"
else
  fail "全项目对象化盘点异常"
fi

has_project="$(json_expr "${inventory_response}" "any(int(item['projectId']) == int('${PROJECT_ID}') for item in data['data']['projects'])")"
if [[ "${has_project}" == "true" || "$(json_expr "${inventory_response}" "int(data['data']['totalProjects']) > 0")" == "true" ]]; then
  pass "对象化盘点包含 105/503 或至少包含真实项目聚合"
else
  fail "对象化盘点未包含任何可验收项目"
fi

echo "--- 4. Project inventory and dry-run plan stay read-only ---"
project_inventory_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-inventory")"
assert_ok "${project_inventory_response}"
assert_no_forbidden "project inventory" "${project_inventory_response}"
project_identity_ok="$(json_expr "${project_inventory_response}" "bool(data['data'].get('projectCode')) and bool(data['data'].get('projectName')) and len(data['data'].get('projects', [])) > 0 and bool(data['data']['projects'][0].get('projectCode')) and bool(data['data']['projects'][0].get('projectName'))")"
if [[ "${project_identity_ok}" == "true" ]]; then
  pass "单项目对象化盘点返回 projectCode/projectName"
else
  fail "单项目对象化盘点缺少 projectCode/projectName：${project_inventory_response}"
fi
before_tasks_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks")"
assert_ok "${before_tasks_response}"
before_task_count="$(json_expr "${before_tasks_response}" "len(data['data'])")"

dry_run_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-plans:dry-run" '{"storageState":"ANY","checksumState":"ANY","limit":25}')"
assert_ok "${dry_run_response}"
after_tasks_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks")"
assert_ok "${after_tasks_response}"
after_task_count="$(json_expr "${after_tasks_response}" "len(data['data'])")"

if assert_no_forbidden "dry-run" "${dry_run_response}" \
  && [[ "$(json_expr "${dry_run_response}" "data['data']['dryRun']")" == "true" ]] \
  && [[ "$(json_expr "${dry_run_response}" "data['data']['migrationStarted']")" == "false" ]] \
  && [[ "${before_task_count}" == "${after_task_count}" ]]; then
  pass "dry-run 返回计划但未创建迁移任务、未启动真实迁移"
else
  fail "dry-run 行为异常"
fi

if [[ "$(json_expr "${dry_run_response}" "'riskMessages' in data['data'] and len(data['data']['riskMessages']) > 0")" == "true" ]]; then
  pass "dry-run 返回风险说明和计划边界"
else
  fail "dry-run 缺少风险说明"
fi

echo "--- 5. Script tracking ---"
if git ls-files --error-unmatch scripts/dev/check-m3g-nas-minio-readiness-inventory.sh >/dev/null 2>&1; then
  pass "M3G-1 专项脚本已纳入 Git 跟踪"
else
  fail "M3G-1 专项脚本尚未纳入 Git 跟踪"
fi

echo
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
