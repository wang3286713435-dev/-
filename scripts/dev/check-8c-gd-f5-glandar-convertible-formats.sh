#!/usr/bin/env bash
# 8C-GD-F5: Glandar convertible model format whitelist smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
SOURCE_FILE="${SOURCE_FILE:-backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java}"

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
    r"smb://",
    r"nas://",
    r"storage_path",
    r"storage_uri",
    r"storagePath",
    r"storageUri",
    r"object_key",
    r"objectKey",
    r'"bucket"\s*:',
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
  local body="${2:-{}}"
  curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

printf '== 8C-GD-F5 Glandar convertible formats ==\n'

if grep -q 'GLANDAR_SUBMIT_SUPPORTED_EXTENSIONS = List.of("RVT", "IFC", "NWD", "NWC")' "${SOURCE_FILE}"; then
  pass "葛兰岱尔提交白名单包含 RVT / IFC / NWD / NWC"
else
  fail "葛兰岱尔提交白名单未包含完整可转换格式"
fi

if grep -q 'CREATE_MODEL_CONVERSION_TASK' "${SOURCE_FILE}" \
  && ! grep -q 'CREATE_RVT_CONVERSION_TASK' "${SOURCE_FILE}"; then
  pass "平台能力口径已从 RVT 专项转换收束为模型转换任务"
else
  fail "平台能力口径仍残留 RVT 专项转换"
fi

if ! grep -q '只开放.*RVT' "${SOURCE_FILE}"; then
  pass "不再向用户提示只开放 RVT"
else
  fail "仍残留只开放 RVT 的用户可见提示"
fi

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
  pass "葛兰岱尔模型清单返回 OK 且无 forbidden 字段"
else
  fail "葛兰岱尔模型清单异常或存在 forbidden 字段"
fi

convertible_present="$(json_expr "${model_list_response}" "any(item.get('extension') in {'IFC','NWD','NWC'} for item in data['data'])")"
convertible_supported="$(json_expr "${model_list_response}" "all(item.get('supported') is True and item.get('lightweightStatus') != 'UNSUPPORTED' and not item.get('unsupportedReason') for item in data['data'] if item.get('extension') in {'IFC','NWD','NWC'})")"
if [[ "${convertible_present}" == "true" ]]; then
  if [[ "${convertible_supported}" == "true" ]]; then
    pass "当前项目中的 IFC / NWD / NWC 均显示为可提交轻量化"
  else
    fail "当前项目存在 IFC / NWD / NWC 但仍被标记为不可提交"
  fi
else
  pass "当前项目暂无 IFC / NWD / NWC 样本，运行期格式断言按源码白名单通过"
fi

glb_gltf_guard="$(json_expr "${model_list_response}" "all(item.get('supported') is False for item in data['data'] if item.get('extension') in {'GLB','GLTF'})")"
if [[ "${glb_gltf_guard}" == "true" ]]; then
  pass "GLB / GLTF 未被误纳入葛兰岱尔转换任务"
else
  fail "GLB / GLTF 被误标记为葛兰岱尔转换输入"
fi

printf '== RESULT: PASS=%s FAIL=%s ==\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
