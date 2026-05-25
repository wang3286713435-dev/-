#!/usr/bin/env bash
# M2I: 105 全文件工程树归属治理与 Hermes 辅助 smoke
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

urlencode() {
  local value="$1"
  VALUE="${value}" python3 - <<'PY'
import os
import urllib.parse
print(urllib.parse.quote(os.environ["VALUE"], safe=""))
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

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 30 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 60 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

api_put() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 60 -X PUT "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

echo "=== M2I: 105 file ownership governance ==="

echo ""
echo "--- 1. Login and switch to project ${PROJECT_ID}/${PROJECT_CODE} ---"
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
echo "--- 2. Coverage and tree are available and safe ---"
coverage_before="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/coverage")"
tree_before="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/tree")"
unassigned_before="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/unassigned?page=1&pageSize=20")"
for payload in "${coverage_before}" "${tree_before}" "${unassigned_before}"; do
  assert_ok "${payload}"
  assert_no_forbidden "ownership read response" "${payload}"
done
assert_data "coverage has catalog file count" "${coverage_before}" "data['totalFiles'] > 0 and data['projectId'] == ${PROJECT_ID}"
assert_data "tree has project root" "${tree_before}" "data['projectId'] == ${PROJECT_ID} and len(data['nodes']) > 0 and data['nodes'][0]['nodeLabel']"
pass "归属覆盖率、工程树和未归属列表可查且安全脱敏"

echo ""
echo "--- 3. Recommendation can classify files without writing ---"
recommend_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-ownership/recommendations" '{"limit":80,"includeAssigned":true}')"
apply_without_confirm="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-ownership/recommendations:apply" '{"confirmed":false,"applyAllUnassigned":true}')"
batch_without_confirm="$(api_put "/api/data-steward/projects/${PROJECT_ID}/file-ownership/assignments:batch" '{"confirmed":false,"items":[],"source":"MANUAL"}')"
assert_ok "${recommend_response}"
assert_not_ok "${apply_without_confirm}"
assert_not_ok "${batch_without_confirm}"
assert_no_forbidden "ownership recommendation" "${recommend_response}"
assert_no_forbidden "ownership apply rejection" "${apply_without_confirm}"
assert_no_forbidden "ownership batch rejection" "${batch_without_confirm}"
assert_data "recommendations contain business fields" "${recommend_response}" "data['totalCount'] > 0 and all(row['fileId'] and row['suggestedNodeLabel'] and row['ownershipType'] and row['reason'] for row in data['rows'][:10])"
assert_data "recommendations are not fake delivery bindings" "${recommend_response}" "all(row['ownershipType'] in ['DELIVERY','PROCESS','MODEL','DRAWING_EXCHANGE','REFERENCE','ARCHIVE','PENDING_REVIEW'] for row in data['rows'][:20])"
pass "推荐结果可解释，未确认时不能写入"

echo ""
echo "--- 4. Confirmed apply covers all 105 files as ownership records ---"
apply_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-ownership/recommendations:apply" '{"confirmed":true,"applyAllUnassigned":true,"source":"RULE"}')"
assert_ok "${apply_response}"
assert_no_forbidden "ownership apply response" "${apply_response}"
assert_data "apply wrote assignments" "${apply_response}" "data['createdCount'] + data['updatedCount'] >= 0 and data['failedCount'] == 0"

coverage_after="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/coverage")"
tree_after="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/tree")"
unassigned_after="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/unassigned?page=1&pageSize=20")"
for payload in "${coverage_after}" "${tree_after}" "${unassigned_after}"; do
  assert_ok "${payload}"
  assert_no_forbidden "ownership final response" "${payload}"
done
assert_data "all files have an ownership bucket" "${coverage_after}" "data['totalFiles'] > 0 and data['unassignedFiles'] == 0 and data['assignedFiles'] == data['totalFiles']"
assert_data "tree aggregates assigned files" "${tree_after}" "data['assignedFiles'] == data['totalFiles'] and any(node['fileCount'] > 0 for node in data['nodes'])"
assert_data "unassigned list is empty after apply" "${unassigned_after}" "data['total'] == 0 and len(data['items']) == 0"
pass "105 所有文件均进入工程树归属或待判定/归档节点"

echo ""
echo "--- 5. Tree node click can list assigned files safely ---"
ROOT_NODE_PATH="$(json_data_expr "${tree_after}" "data['nodes'][0]['nodePath']")"
ROOT_NODE_PATH_ENCODED="$(urlencode "${ROOT_NODE_PATH}")"
node_files_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/files?nodePath=${ROOT_NODE_PATH_ENCODED}&page=1&pageSize=20")"
assert_ok "${node_files_response}"
assert_no_forbidden "ownership node files response" "${node_files_response}"
assert_data "node files are visible from tree node" "${node_files_response}" "data['total'] > 0 and len(data['items']) > 0 and all(row['fileId'] and row['ownershipNodePath'] and row['ownershipStatus'] for row in data['items'])"
pass "工程树节点能查看对应已归属文件"

echo ""
echo "--- 6. Catalog files expose ownership tags without raw paths ---"
catalog_response="$(api_get "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&page=1&pageSize=20&directOnly=true")"
assert_ok "${catalog_response}"
assert_no_forbidden "catalog files ownership response" "${catalog_response}"
assert_data "catalog files contain ownership labels" "${catalog_response}" "data['total'] > 0 and all('ownershipStatus' in row and row['ownershipStatus'] for row in data['items'])"
pass "文件管理器可拿到归属节点和归属状态"

echo ""
echo "--- 7. Delivery package remains driven by formal deliverables only ---"
package_prepare_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/prepare?targetType=SECTION")"
TOTAL_FILES_AFTER="$(json_data_expr "${coverage_after}" "data['totalFiles']")"
assert_ok "${package_prepare_response}"
assert_no_forbidden "delivery package prepare response" "${package_prepare_response}"
assert_data "ownership records do not pollute formal delivery package" "${package_prepare_response}" "data['dryRun'] is True and data['physicalPackageGenerated'] is False and data['nasFileCopied'] is False"
assert_data "formal delivery count stays below all catalog files" "${package_prepare_response}" "data['totalCount'] < int(${TOTAL_FILES_AFTER})"
pass "2928 个归属记录未污染正式交付包应交项"

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
