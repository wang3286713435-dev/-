#!/usr/bin/env bash
# 8C-GD-MAINLINE-FULL: Glandar full-project model catalog and controlled Viewer entry smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
EXPECT_GLANDAR_READY="${EXPECT_GLANDAR_READY:-false}"

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
    "next": next,
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
    r"secret",
    r"token\s*[:=]",
    r"password",
    r"\bselect\s+.+\s+from\b",
]
for pattern in patterns:
    if re.search(pattern, payload, re.IGNORECASE | re.DOTALL):
        raise SystemExit(f"{label} leaked forbidden pattern: {pattern}")
PY
}

api_get() {
  local path="$1"
  curl -sS --connect-timeout 3 --max-time 30 "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local path="$1"
  curl -sS --connect-timeout 3 --max-time 90 -X POST "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d '{}'
}

printf '== 8C-GD mainline full Glandar render smoke ==\n'

login_response="$(curl -sS --connect-timeout 3 --max-time 10 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
if assert_ok "${login_response}"; then
  TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
  pass "管理员登录成功"
else
  fail "管理员登录失败"
fi

switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch")"
if assert_ok "${switch_response}"; then
  TOKEN="$(json_expr "${switch_response}" "data['data']['accessToken']")"
  pass "切换到项目 ${PROJECT_ID}"
else
  fail "切换项目失败"
fi

model_list_response="$(api_get "/api/visualization-adapter/projects/${PROJECT_ID}/glandar/model-files")"
if assert_ok "${model_list_response}" && assert_no_forbidden "glandar model list" "${model_list_response}"; then
  pass "全项目葛兰岱尔模型清单返回 OK 且无 forbidden 字段"
else
  fail "全项目模型清单异常或存在 forbidden 字段"
fi

model_count="$(json_expr "${model_list_response}" "len(data['data'])")"
rvt_count="$(json_expr "${model_list_response}" "sum(1 for item in data['data'] if item.get('extension') == 'RVT')")"
non_pilot_rvt="$(json_expr "${model_list_response}" "next((item.get('fileId') for item in data['data'] if item.get('extension') == 'RVT' and item.get('fileId') not in {1257,1261,1264,3730,1258,1251,1259,1262,3729,1243}), '')")"
if [[ "${model_count}" -gt 10 && "${rvt_count}" -gt 10 && -n "${non_pilot_rvt}" ]]; then
  pass "模型清单已超过 10 个试点，找到非试点 RVT：${non_pilot_rvt}"
else
  fail "模型清单仍疑似停留在 10 个试点内：model_count=${model_count}, rvt_count=${rvt_count}, non_pilot=${non_pilot_rvt}"
fi

pilot_response="$(api_get "/api/visualization-adapter/projects/${PROJECT_ID}/glandar/rvt-pilot-files")"
pilot_count="$(json_expr "${pilot_response}" "len(data['data'])")"
if assert_ok "${pilot_response}" && [[ "${pilot_count}" -eq 10 ]] && assert_no_forbidden "glandar pilot list" "${pilot_response}"; then
  pass "历史 10 个 RVT 试点清单仍兼容可见"
else
  fail "历史 RVT 试点清单兼容异常"
fi

if [[ -n "${non_pilot_rvt}" ]]; then
  submit_response="$(api_post "/api/visualization-adapter/projects/${PROJECT_ID}/files/${non_pilot_rvt}/lightweight-jobs?force=false")"
  submit_code="$(json_expr "${submit_response}" "data.get('code')")"
  if [[ "${submit_code}" == "OK" ]]; then
    submit_status="$(json_expr "${submit_response}" "data['data'].get('taskStatus')")"
    if assert_no_forbidden "non-pilot submit" "${submit_response}" && [[ -n "${submit_status}" ]]; then
      pass "非试点 RVT 可提交轻量化任务，状态：${submit_status}"
    else
      fail "非试点 RVT 提交响应缺少状态或存在 forbidden 字段"
    fi
  elif [[ "${submit_code}" == "GLANDAR_ENGINE_NOT_CONFIGURED" ]]; then
    if assert_code "${submit_response}" "GLANDAR_ENGINE_NOT_CONFIGURED" && assert_no_forbidden "non-pilot submit blocked" "${submit_response}"; then
      pass "默认未配置 Station 时，非试点 RVT 返回 GLANDAR_ENGINE_NOT_CONFIGURED 且未执行真实任务"
    else
      fail "Station 未配置阻断响应异常"
    fi
  else
    fail "非试点 RVT 提交返回非预期 code=${submit_code}"
  fi
fi

ready_job_id="$(json_expr "${model_list_response}" "next((item.get('latestJobId') for item in data['data'] if item.get('taskStatus') == 'READY' and item.get('viewerAvailable') and item.get('latestJobId')), '')")"
if [[ -n "${ready_job_id}" ]]; then
  ticket_response="$(api_post "/api/visualization-adapter/projects/${PROJECT_ID}/lightweight-jobs/${ready_job_id}:viewer-ticket")"
  ticket_issued="$(json_expr "${ticket_response}" "data['data'].get('ticketIssued')")"
  engine_static_base="$(json_expr "${ticket_response}" "data['data'].get('engineStaticBase')")"
  if assert_ok "${ticket_response}" \
    && assert_no_forbidden "viewer ticket" "${ticket_response}" \
    && [[ "${ticket_issued}" == "true" ]] \
    && [[ "${engine_static_base}" == http://*:18087/static/ThreeJsEngine ]]; then
    pass "READY 轻量化任务可签发短期 Viewer 入口，并返回引擎静态资源地址"
  else
    fail "READY Viewer ticket 签发异常：ticketIssued=${ticket_issued}, engineStaticBase=${engine_static_base}"
  fi
else
  if [[ "${EXPECT_GLANDAR_READY}" == "true" ]]; then
    fail "EXPECT_GLANDAR_READY=true 但未找到 READY 模型"
  else
    pass "当前无 READY 模型，Viewer ticket 检查按环境阻断跳过"
  fi
fi

dashboard_response="$(api_get "/api/visualization-adapter/projects/${PROJECT_ID}/digital-twin-dashboard")"
if assert_ok "${dashboard_response}" && assert_no_forbidden "digital twin dashboard" "${dashboard_response}"; then
  pass "BIM 协同看板接口无 forbidden 字段"
else
  fail "BIM 协同看板接口异常或存在 forbidden 字段"
fi

if [[ -n "${ready_job_id}" ]]; then
  dashboard_viewer_available="$(json_expr "${dashboard_response}" "data['data']['modelSummary'].get('viewerAvailable')")"
  dashboard_engine_mode="$(json_expr "${dashboard_response}" "data['data']['modelSummary'].get('engineMode')")"
  dashboard_status_label="$(json_expr "${dashboard_response}" "data['data']['modelSummary'].get('statusLabel')")"
  if [[ "${dashboard_viewer_available}" == "true" ]] \
    && [[ "${dashboard_engine_mode}" == "GLANDAR" ]] \
    && [[ "${dashboard_status_label}" != "真实 Viewer 未接入" ]]; then
    pass "BIM 协同看板摘要已跟随 READY 模型切换为真实 Viewer 口径"
  else
    fail "BIM 协同看板摘要仍停留旧口径：viewer=${dashboard_viewer_available}, engine=${dashboard_engine_mode}, label=${dashboard_status_label}"
  fi
fi

printf '== RESULT: PASS=%s FAIL=%s ==\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
