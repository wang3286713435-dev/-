#!/usr/bin/env bash
# GS-1: platform global search must be real, permission-aware, and sanitized.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"

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
    r"\bSQL\b",
    r"raw row",
    r"token",
    r"secret",
]
for pattern in patterns:
    if re.search(pattern, payload, flags=re.IGNORECASE):
        raise AssertionError(f"{label} contains forbidden pattern: {pattern}")
PY
}

login_response="$(curl -sS --connect-timeout 3 --max-time 10 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
if assert_ok "${login_response}"; then
  TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
  pass "管理员登录成功"
else
  fail "管理员登录失败"
fi

search_response="$(curl -sS --connect-timeout 3 --max-time 12 \
  "${BASE_URL}/api/core/search/global?keyword=%E5%90%AF%E8%88%AA&limit=5" \
  -H "Authorization: Bearer ${TOKEN}")"
if assert_ok "${search_response}"; then
  pass "全局搜索接口返回 OK"
else
  fail "全局搜索接口未返回 OK"
fi

if [[ "$(json_expr "${search_response}" "data['data']['totalCount'] > 0")" == "true" ]]; then
  pass "关键词启航能搜到结果"
else
  fail "关键词启航没有结果"
fi

if [[ "$(json_expr "${search_response}" "any(group['type'] == 'PROJECT' for group in data['data']['groups'])")" == "true" ]]; then
  pass "搜索结果包含项目分组"
else
  fail "搜索结果缺少项目分组"
fi

if [[ "$(json_expr "${search_response}" "all(item.get('routeName') for group in data['data']['groups'] for item in group.get('items', []))")" == "true" ]]; then
  pass "所有搜索结果都有可跳转 routeName"
else
  fail "存在不可跳转的搜索结果"
fi

if assert_no_forbidden "global-search" "${search_response}"; then
  pass "搜索响应未泄露真实路径、对象 key 或密钥"
else
  fail "搜索响应存在禁出字段"
fi

model_response="$(curl -sS --connect-timeout 3 --max-time 12 \
  "${BASE_URL}/api/core/search/global?keyword=rvt&limit=5" \
  -H "Authorization: Bearer ${TOKEN}")"
if assert_ok "${model_response}"; then
  pass "模型/文件关键词搜索返回 OK"
else
  fail "模型/文件关键词搜索失败"
fi

if assert_no_forbidden "model-search" "${model_response}"; then
  pass "模型/文件搜索响应未泄露禁出字段"
else
  fail "模型/文件搜索响应存在禁出字段"
fi

blank_response="$(curl -sS --connect-timeout 3 --max-time 12 \
  "${BASE_URL}/api/core/search/global?keyword=&limit=5" \
  -H "Authorization: Bearer ${TOKEN}")"
if assert_ok "${blank_response}" && [[ "$(json_expr "${blank_response}" "data['data']['totalCount'] == 0")" == "true" ]]; then
  pass "空关键词安全返回空结果"
else
  fail "空关键词处理异常"
fi

printf '\nGS-1 global search check: PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
