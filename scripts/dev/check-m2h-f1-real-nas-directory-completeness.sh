#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

TREE_NODE="${ROOT_DIR}/frontend/src/modules/data-steward/components/DirectoryTreeNodeItem.vue"
FILE_BROWSER="${ROOT_DIR}/frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue"
CATALOG_CONTROLLER="${ROOT_DIR}/backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/CatalogController.java"
CATALOG_SERVICE="${ROOT_DIR}/backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java"

PASS=0
FAIL=0
TOKEN=""

ok() {
  PASS=$((PASS + 1))
  printf '[PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '[FAIL] %s\n' "$1" >&2
}

json_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
scope = {"data": data, "len": len, "any": any, "all": all, "set": set, "sorted": sorted}
value = eval(os.environ["EXPR"], {"__builtins__": {}, **scope}, scope)
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
    r"\binsert\s+into\b",
    r"\bupdate\s+.+\s+set\b",
    r"\bdelete\s+from\b",
]
for pattern in patterns:
    assert not re.search(pattern, payload, re.IGNORECASE), f"{label} contains forbidden pattern {pattern}: {payload[:1000]}"
PY
}

urlencode() {
  python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1]))' "$1"
}

login() {
  local response
  response="$(curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${response}"
  json_expr "${response}" "data['data']['accessToken']"
}

get_json() {
  local path="$1"
  curl -sS --connect-timeout 3 --max-time 30 "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}"
}

echo '=== M2H-F1: real NAS directory completeness ==='

if rg -q "props\\.depth === 0" "${TREE_NODE}"; then
  fail "目录树节点仍强制顶层展开"
else
  ok "目录树节点不再通过 depth === 0 强制展开"
fi

if rg -q "handleNodeClick" "${TREE_NODE}" && rg -q "emit\\('toggle-expand', props\\.node\\.fullPath, !expanded\\.value\\)" "${TREE_NODE}"; then
  ok "点击目录名称也会切换展开/折叠状态"
else
  fail "点击目录名称未复用展开/折叠状态"
fi

remember_block="$(sed -n '/function rememberExpandedAncestors/,/^}/p' "${FILE_BROWSER}")"
if grep -q "parts.length <= 1" <<<"${remember_block}" && grep -q ".slice(0, -1)" <<<"${remember_block}"; then
  ok "选择目录时只自动展开祖先，不把当前目录强行加回展开集合"
else
  fail "选择目录仍可能把当前目录自身强行加回展开集合"
fi

if rg -q "fetchCatalogDirectoryChildren" "${FILE_BROWSER}" \
  && rg -q "/directory-children" "${CATALOG_CONTROLLER}" \
  && rg -q "listCatalogDirectoryChildren" "${CATALOG_SERVICE}"; then
  ok "前后端已接入当前目录 direct children 能力"
else
  fail "前后端缺少当前目录 direct children 能力"
fi

TOKEN="$(login)"
ok "管理员登录成功"

directories_response="$(get_json "/api/data-steward/catalog/directories?projectId=${PROJECT_ID}")"
assert_ok "${directories_response}"
assert_no_forbidden "catalog directories" "${directories_response}"
if [[ "$(json_expr "${directories_response}" "all(not (item['directoryPath'] or '').startswith('105-启航华居项目') for item in data['data'])")" == "true" ]]; then
  ok "目录树来源不再把 105 项目根目录误当成一级文件夹"
else
  fail "目录树来源仍包含 105 项目根目录包装文件夹"
fi

root_response="$(get_json "/api/data-steward/catalog/directory-children?projectId=${PROJECT_ID}&pageSize=100")"
assert_ok "${root_response}"
assert_no_forbidden "root direct children" "${root_response}"
ok "根目录 direct children 响应通过 forbidden-field scan"

expected_dirs="['00_工作进度','01_文件收发','02_项目资源','03_过程文件','04_共享文件','05_发布文件','06_归档文件','07_浏览动画']"
if [[ "$(json_expr "${root_response}" "all(path in [item['directoryPath'] for item in data['data']['directories']] for path in ${expected_dirs})")" == "true" ]]; then
  ok "105 根目录可见 00/01/02/03/04/05/06/07 真实直接子目录"
else
  fail "105 根目录缺少 00/01/02/03/04/05/06/07 真实直接子目录"
fi

if [[ "$(json_expr "${root_response}" "all('/' not in item['directoryPath'] for item in data['data']['directories'])")" == "true" ]] \
  && [[ "$(json_expr "${root_response}" "all('/' not in (item.get('logicalPath') or '') for item in data['data']['files']['items'])")" == "true" ]]; then
  ok "根目录只返回直接子文件夹和直接文件"
else
  fail "根目录 direct children 混入了深层条目"
fi

if [[ "$(json_expr "${root_response}" "any((item.get('registered') is False or item.get('registrationStatus') == 'UNREGISTERED') for item in data['data']['files']['items'])")" == "true" ]]; then
  ok "未登记直接文件以 UNREGISTERED 返回，不伪造 fileId"
else
  fail "未登记直接文件未按 UNREGISTERED 表达"
fi

child_path="$(urlencode '01_文件收发')"
child_response="$(get_json "/api/data-steward/catalog/directory-children?projectId=${PROJECT_ID}&directoryPath=${child_path}&pageSize=100")"
assert_ok "${child_response}"
assert_no_forbidden "01 direct children" "${child_response}"
if [[ "$(json_expr "${child_response}" "all(item['directoryPath'].startswith('01_文件收发/') and item['directoryPath'].count('/') == 1 for item in data['data']['directories'])")" == "true" ]] \
  && [[ "$(json_expr "${child_response}" "all((item.get('logicalPath') or '').count('/') <= 1 for item in data['data']['files']['items'])")" == "true" ]]; then
  ok "01_文件收发 只返回当前目录直接子项"
else
  fail "01_文件收发 direct children 混入深层条目"
fi

printf '=== Result: PASS=%d FAIL=%d ===\n' "${PASS}" "${FAIL}"

if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
