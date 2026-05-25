#!/usr/bin/env bash
# M2J: 105 工程树人工复核与批量调整体验 smoke
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-503}"
PROJECT_CODE="${PROJECT_CODE:-105}"

PASS=0
FAIL=0
TOKEN=""

pass() {
  echo "  [PASS] $1"
  PASS=$((PASS + 1))
}

fail() {
  echo "  [FAIL] $1"
  FAIL=$((FAIL + 1))
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

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"]).get("data")
scope = {"__builtins__": {}, "data": data, "len": len, "all": all, "any": any, "sum": sum, "int": int, "str": str, "isinstance": isinstance, "list": list}
value = eval(os.environ["EXPR"], scope, scope)
if value is None:
    print("")
elif isinstance(value, bool):
    print("true" if value else "false")
else:
    print(value)
PY
}

assert_data() {
  local label="$1"
  local response="$2"
  local code="$3"
  LABEL="${label}" RESPONSE="${response}" CODE="${code}" python3 - <<'PY'
import json
import os
label = os.environ["LABEL"]
data = json.loads(os.environ["RESPONSE"]).get("data")
scope = {"__builtins__": {}, "data": data, "len": len, "all": all, "any": any, "sum": sum, "int": int, "str": str, "isinstance": isinstance, "list": list}
assert eval(os.environ["CODE"], scope, scope), label
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
    r"\bnas://",
    r"\bsmb://",
    r"\bafp://",
    r"\bstorage_path\b",
    r"\bstorage_uri\b",
    r"\bstoragePath\b",
    r"\bstorageUri\b",
    r"\braw_path\b",
    r"\bnas_path\b",
    r"\braw DB row\b",
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
    assert not re.search(pattern, payload, re.IGNORECASE), f"{label} contains forbidden pattern {pattern}: {payload[:1200]}"
PY
}

urlencode() {
  local value="$1"
  VALUE="${value}" python3 - <<'PY'
import os
import urllib.parse
print(urllib.parse.quote(os.environ["VALUE"], safe=""))
PY
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 30 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 40 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

api_put() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 40 -X PUT "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

echo "=== M2J: 105 ownership review smoke ==="

echo ""
echo "--- 1. Login and switch project ${PROJECT_ID}/${PROJECT_CODE} ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_data_expr "${login_response}" "data['accessToken']")"
pass "管理员登录成功"

switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch" '{}')"
assert_ok "${switch_response}"
TOKEN="$(json_data_expr "${switch_response}" "data['accessToken']")"
pass "已切换到 105 真实项目"

echo ""
echo "--- 2. Query tree node files with M2J filters ---"
tree_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/tree")"
assert_ok "${tree_response}"
assert_no_forbidden "ownership tree" "${tree_response}"
ROOT_NODE_PATH="$(json_data_expr "${tree_response}" "data['nodes'][0]['nodePath']")"
ROOT_NODE_PATH_ENCODED="$(urlencode "${ROOT_NODE_PATH}")"
node_files_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/files?nodePath=${ROOT_NODE_PATH_ENCODED}&status=CONFIRMED&page=1&pageSize=5")"
review_only_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/files?nodePath=${ROOT_NODE_PATH_ENCODED}&reviewOnly=true&page=1&pageSize=5")"
for payload in "${node_files_response}" "${review_only_response}"; do
  assert_ok "${payload}"
  assert_no_forbidden "ownership files" "${payload}"
done
assert_data "confirmed node files available" "${node_files_response}" "data['total'] > 0 and len(data['items']) > 0"
pass "工程树文件列表支持状态/待复核筛选且安全脱敏"

FILE_ID="$(json_data_expr "${node_files_response}" "data['items'][0]['fileId']")"
ORIGINAL_TYPE="$(json_data_expr "${node_files_response}" "data['items'][0]['ownershipType']")"
ORIGINAL_NODE_KEY="$(json_data_expr "${node_files_response}" "data['items'][0]['ownershipNodeKey']")"
ORIGINAL_NODE_LABEL="$(json_data_expr "${node_files_response}" "data['items'][0]['ownershipNodeLabel']")"
ORIGINAL_NODE_PATH="$(json_data_expr "${node_files_response}" "data['items'][0]['ownershipNodePath']")"
TARGET_TYPE="PROCESS"
if [[ "${ORIGINAL_TYPE}" == "PROCESS" ]]; then
  TARGET_TYPE="REFERENCE"
fi

echo ""
echo "--- 3. Batch review requires explicit confirmation ---"
reject_without_confirm="$(api_put "/api/data-steward/projects/${PROJECT_ID}/file-ownership/assignments:review" \
  "{\"confirmed\":false,\"fileIds\":[${FILE_ID}],\"action\":\"CONFIRM\"}")"
assert_not_ok "${reject_without_confirm}"
assert_no_forbidden "review rejection" "${reject_without_confirm}"
pass "未 confirmed=true 时批量复核写入被拒绝"

echo ""
echo "--- 4. Batch update type, move node and reject/restore safely ---"
update_type_response="$(api_put "/api/data-steward/projects/${PROJECT_ID}/file-ownership/assignments:review" \
  "{\"confirmed\":true,\"fileIds\":[${FILE_ID}],\"action\":\"UPDATE_TYPE\",\"ownershipType\":\"${TARGET_TYPE}\",\"reason\":\"M2J smoke: temporary type update\"}")"
move_same_node_response="$(api_put "/api/data-steward/projects/${PROJECT_ID}/file-ownership/assignments:review" \
  "{\"confirmed\":true,\"fileIds\":[${FILE_ID}],\"action\":\"MOVE_NODE\",\"nodeKey\":\"${ORIGINAL_NODE_KEY}\",\"nodeLabel\":\"${ORIGINAL_NODE_LABEL}\",\"nodePath\":\"${ORIGINAL_NODE_PATH}\",\"reason\":\"M2J smoke: same-node move validation\"}")"
reject_response="$(api_put "/api/data-steward/projects/${PROJECT_ID}/file-ownership/assignments:review" \
  "{\"confirmed\":true,\"fileIds\":[${FILE_ID}],\"action\":\"REJECT\",\"reason\":\"M2J smoke: temporary reject\"}")"
restore_response="$(api_put "/api/data-steward/projects/${PROJECT_ID}/file-ownership/assignments:review" \
  "{\"confirmed\":true,\"fileIds\":[${FILE_ID}],\"action\":\"UPDATE_NODE_AND_TYPE\",\"ownershipType\":\"${ORIGINAL_TYPE}\",\"nodeKey\":\"${ORIGINAL_NODE_KEY}\",\"nodeLabel\":\"${ORIGINAL_NODE_LABEL}\",\"nodePath\":\"${ORIGINAL_NODE_PATH}\",\"reason\":\"M2J smoke: restore original ownership\"}")"
for payload in "${update_type_response}" "${move_same_node_response}" "${reject_response}" "${restore_response}"; do
  assert_ok "${payload}"
  assert_no_forbidden "review write response" "${payload}"
  assert_data "review write updates exactly one row" "${payload}" "data['requestedCount'] == 1 and data['updatedCount'] == 1 and data['failedCount'] == 0"
done
pass "批量改类型、移动节点、驳回和恢复接口可用"

echo ""
echo "--- 5. Restore verification and audit trail ---"
RESTORE_NODE_PATH_ENCODED="$(urlencode "${ORIGINAL_NODE_PATH}")"
restore_check_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/files?nodePath=${RESTORE_NODE_PATH_ENCODED}&status=CONFIRMED&page=1&pageSize=200")"
audit_response="$(api_get "/api/core/projects/${PROJECT_ID}/audit-logs?moduleCode=data-steward&limit=80")"
assert_ok "${restore_check_response}"
assert_ok "${audit_response}"
assert_no_forbidden "restore check" "${restore_check_response}"
assert_no_forbidden "audit response" "${audit_response}"
assert_data "file restored to original node and type" "${restore_check_response}" "any(row['fileId'] == int('${FILE_ID}') and row['ownershipType'] == '${ORIGINAL_TYPE}' and row['ownershipNodePath'] == '${ORIGINAL_NODE_PATH}' and row['ownershipStatus'] == 'CONFIRMED' for row in data['items'])"
assert_data "review audit exists" "${audit_response}" "any(row.get('actionCode') == 'data.file-ownership.review-batch' for row in data)"
pass "复核动作已恢复原始归属并写入审计"

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
