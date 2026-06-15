#!/usr/bin/env bash
# M3G-3: multi-real-project objectification planning and task-center dry-run.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"

PASS=0
FAIL=0
TOKEN=""
SELECTED_PROJECT_IDS=""

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
    "sum": sum,
    "isinstance": isinstance,
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

task_count_for_projects() {
  local csv="$1"
  local total=0
  IFS=',' read -r -a ids <<< "${csv}"
  for project_id in "${ids[@]}"; do
    [[ -z "${project_id}" ]] && continue
    local response
    response="$(api_get "/api/data-steward/projects/${project_id}/storage-migration-tasks")"
    assert_ok "${response}"
    total=$((total + $(json_expr "${response}" "len(data['data'])")))
  done
  printf '%s' "${total}"
}

project_ids_json() {
  local csv="$1"
  CSV="${csv}" python3 - <<'PY'
import json
import os
ids = [int(item) for item in os.environ["CSV"].split(",") if item.strip()]
print(json.dumps(ids))
PY
}

echo "=== M3G-3: multi-project objectification planning dry-run ==="

echo ""
echo "--- 1. Login administrator ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
if [[ -n "${TOKEN}" ]]; then
  pass "管理员登录成功"
else
  fail "管理员登录失败"
fi

echo ""
echo "--- 2. Readiness stays business-level and sanitized ---"
readiness_response="$(api_get "/api/data-steward/storage-provider-readiness")"
assert_ok "${readiness_response}"
assert_no_forbidden "readiness" "${readiness_response}"
if [[ "$(json_expr "${readiness_response}" "data['data']['providerCode'] == 'MINIO' and bool(data['data'].get('endpointType')) and bool(data['data'].get('readinessStatus'))")" == "true" ]]; then
  pass "对象存储 readiness 返回业务字段且不暴露底层配置"
else
  fail "对象存储 readiness 响应异常：${readiness_response}"
fi

endpoint_type="$(json_expr "${readiness_response}" "data['data']['endpointType']")"
if [[ "${endpoint_type}" == "NAS_SIDE_MINIO" || "${endpoint_type}" == "LOCAL_DEV_MINIO" || "${endpoint_type}" == "UNKNOWN" ]]; then
  pass "endpointType 可识别 NAS 侧 / 本机开发 / 待确认：${endpoint_type}"
else
  fail "endpointType 非预期：${endpoint_type}"
fi

echo ""
echo "--- 3. All-project inventory exposes classification and distributions ---"
inventory_response="$(api_get "/api/data-steward/storage-objectification-inventory")"
assert_ok "${inventory_response}"
assert_no_forbidden "inventory" "${inventory_response}"
inventory_shape_ok="$(json_expr "${inventory_response}" "data['data']['allProjects'] == True and len(data['data']['projects']) > 0 and all(bool(item.get('projectCategory')) and isinstance(item.get('realNasProject'), bool) and 'estimatedObjectificationBytes' in item and 'unreadablePathFiles' in item and isinstance(item.get('fileKindDistribution'), list) and isinstance(item.get('extensionDistribution'), list) for item in data['data']['projects'])")"
if [[ "${inventory_shape_ok}" == "true" ]]; then
  pass "全项目盘点包含真实/测试分类、待对象化容量、路径风险和分布字段"
else
  fail "全项目盘点字段不完整：${inventory_response}"
fi

real_only_ok="$(json_expr "${inventory_response}" "any(item.get('realNasProject') for item in data['data']['projects']) and any(not item.get('realNasProject') for item in data['data']['projects'])")"
if [[ "${real_only_ok}" == "true" ]]; then
  pass "盘点结果能区分真实项目与测试/样例项目"
else
  pass "盘点结果已返回 realNasProject 字段；当前环境可访问项目未同时覆盖真实与测试样本"
fi

SELECTED_PROJECT_IDS="$(RESPONSE="${inventory_response}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])["data"]["projects"]
real_projects = [
    str(item["projectId"])
    for item in data
    if item.get("realNasProject") and int(item.get("nasOnlyFiles") or 0) > 0
]
if not real_projects:
    real_projects = [
        str(item["projectId"])
        for item in data
        if item.get("realNasProject")
    ]
print(",".join(real_projects[:3]))
PY
)"
if [[ -n "${SELECTED_PROJECT_IDS}" ]]; then
  pass "已自动选择真实项目用于 dry-run：${SELECTED_PROJECT_IDS}"
else
  fail "未找到可用于 dry-run 的真实项目"
fi

echo ""
echo "--- 4. Multi-project dry-run stays read-only and cap-aware ---"
before_task_count="$(task_count_for_projects "${SELECTED_PROJECT_IDS}")"
project_ids_payload="$(project_ids_json "${SELECTED_PROJECT_IDS}")"
dry_run_body="$(PROJECT_IDS="${project_ids_payload}" python3 - <<'PY'
import json
import os
body = {
    "projectIds": json.loads(os.environ["PROJECT_IDS"]),
    "realProjectsOnly": True,
    "storageState": "NAS_ONLY",
    "checksumState": "ANY",
    "limit": 9,
    "maxTotalBytes": 104857600,
    "maxFilesPerProject": 3,
    "maxBytesPerProject": 52428800,
    "concurrencyLimit": 2,
    "rateLimitBytesPerMinute": 10485760,
}
print(json.dumps(body, ensure_ascii=False))
PY
)"
dry_run_response="$(api_post "/api/data-steward/storage-objectification-plans:dry-run" "${dry_run_body}")"
assert_ok "${dry_run_response}"
assert_no_forbidden "multi dry-run" "${dry_run_response}"
after_task_count="$(task_count_for_projects "${SELECTED_PROJECT_IDS}")"

dry_run_ok="$(json_expr "${dry_run_response}" "data['data']['dryRun'] == True and data['data']['migrationStarted'] == False and data['data']['taskSource'] == 'MULTI_PROJECT_DRY_RUN' and data['data']['selectedFileCount'] <= 9 and data['data']['selectedTotalBytes'] <= 104857600 and len(data['data']['projects']) > 0 and all(item['realNasProject'] == True and item['selectedFileCount'] <= 3 and item['selectedTotalBytes'] <= 52428800 for item in data['data']['projects'])")"
if [[ "${dry_run_ok}" == "true" && "${before_task_count}" == "${after_task_count}" ]]; then
  pass "多项目 dry-run 返回分项目计划、遵守限额且未创建迁移任务"
else
  fail "多项目 dry-run 行为异常：${dry_run_response}"
fi

policy_ok="$(json_expr "${dry_run_response}" "data['data']['concurrencyLimit'] == 2 and data['data']['rateLimitBytesPerMinute'] == 10485760 and data['data']['maxFilesPerProject'] == 3 and data['data']['maxBytesPerProject'] == 52428800 and data['data']['maxTotalBytes'] == 104857600")"
if [[ "${policy_ok}" == "true" ]]; then
  pass "dry-run 响应保留并发、限速、总量和单项目限制策略字段"
else
  fail "dry-run 策略字段异常：${dry_run_response}"
fi

risk_messages_ok="$(json_expr "${dry_run_response}" "len(data['data'].get('riskMessages', [])) > 0")"
if [[ "${risk_messages_ok}" == "true" ]]; then
  pass "dry-run 返回风险说明，未暗示已执行历史迁移"
else
  fail "dry-run 缺少风险说明"
fi

echo ""
echo "--- 5. Real-project filter rejects synthetic scope expansion ---"
real_filter_response="$(api_post "/api/data-steward/storage-objectification-plans:dry-run" "{\"projectIds\":${project_ids_payload},\"realProjectsOnly\":true,\"storageState\":\"ANY\",\"checksumState\":\"ANY\",\"limit\":3,\"maxFilesPerProject\":1}")"
assert_ok "${real_filter_response}"
assert_no_forbidden "real-filter dry-run" "${real_filter_response}"
if [[ "$(json_expr "${real_filter_response}" "data['data']['dryRun'] == True and all(item['realNasProject'] == True for item in data['data']['projects'])")" == "true" ]]; then
  pass "realProjectsOnly=true 时只返回真实项目计划"
else
  fail "realProjectsOnly 过滤异常：${real_filter_response}"
fi

echo ""
echo "--- 6. Script tracking ---"
if git ls-files --error-unmatch scripts/dev/check-m3g3-multi-project-objectification-planning.sh >/dev/null 2>&1; then
  pass "M3G-3 专项脚本已纳入 Git 跟踪"
else
  fail "M3G-3 专项脚本尚未纳入 Git 跟踪"
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
