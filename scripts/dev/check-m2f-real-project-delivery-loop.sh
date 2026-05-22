#!/usr/bin/env bash
# M2F: 真实项目交付闭环试运行 smoke
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

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 30 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

echo "=== M2F: real project delivery loop trial ==="

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
pass "已切换到真实项目 ${PROJECT_ID}/${PROJECT_CODE}"

echo ""
echo "--- 2. Master-data and standards drive delivery ---"
status_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/standard-status")"
section_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/section-nodes/tree")"
node_type_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/node-types")"
definition_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/deliverable-definitions")"
type_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/deliverable-types")"
for payload in "${status_response}" "${section_response}" "${node_type_response}" "${definition_response}" "${type_response}"; do
  assert_ok "${payload}"
  assert_no_forbidden "master-data response" "${payload}"
done
assert_data "standard ready" "${status_response}" "data['deliverableStandardReady'] is True and data['hasSectionTree'] is True and data['hasNodeTypes'] is True and data['nodeTypesLocked'] is True and data['hasDeliverableDefinitions'] is True and data['hasDeliverableTypes'] is True and data['hasDirectoryTemplates'] is True"
assert_data "section tree ready" "${section_response}" "len(data) > 0 and len(data[0].get('children') or []) > 0"
assert_data "node types locked" "${node_type_response}" "len(data) >= 3 and all(row['locked'] is True for row in data)"
assert_data "definitions ready" "${definition_response}" "any(row['code'] == 'DOCUMENT_DELIVERY' for row in data) and any(row['code'] == 'DRAWING_DELIVERY' for row in data)"
assert_data "types ready" "${type_response}" "any(row['fileKind'] == 'DOCUMENT' for row in data) and any(row['fileKind'] == 'DRAWING' for row in data)"
pass "105 正式工程主数据和交付标准可查，并已被锁定为交付前置"

echo ""
echo "--- 3. Document and drawing delivery completeness ---"
doc_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-completeness?viewType=DOCUMENT&targetType=SECTION&onlyMissing=true")"
drawing_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-completeness?viewType=DRAWING&targetType=SECTION&onlyMissing=true")"
for payload in "${doc_response}" "${drawing_response}"; do
  assert_ok "${payload}"
  assert_no_forbidden "delivery completeness" "${payload}"
done
assert_data "document delivery rows" "${doc_response}" "data['standardReady'] is True and data['totalRequired'] > 0 and data['missingCount'] > 0 and data['nextActionCode'] == 'BIND_MISSING_FILES' and len(data['rows']) == data['missingCount']"
assert_data "drawing delivery rows" "${drawing_response}" "data['standardReady'] is True and data['totalRequired'] > 0 and data['missingCount'] > 0 and data['nextActionCode'] == 'BIND_MISSING_FILES' and len(data['rows']) == data['missingCount']"
assert_data "document missing reason readable" "${doc_response}" "all(row['targetName'] in row['missingReason'] and row['deliverableDefinitionName'] in row['missingReason'] and row['deliverableTypeName'] in row['missingReason'] and '文档' in row['missingReason'] for row in data['rows'][:5])"
assert_data "drawing missing reason readable" "${drawing_response}" "all(row['targetName'] in row['missingReason'] and row['deliverableDefinitionName'] in row['missingReason'] and row['deliverableTypeName'] in row['missingReason'] and '图纸' in row['missingReason'] for row in data['rows'][:5])"
pass "文档/图纸交付视图能基于正式规则返回业务可读缺失项"

echo ""
echo "--- 4. Missing items and manual binding guardrails ---"
missing_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/agent-governance/missing-items?targetType=SECTION")"
recommend_drawing_response="$(api_post "/api/work-center/projects/${PROJECT_ID}/agent-governance/recommend-bindings" '{"viewType":"DRAWING","targetType":"SECTION","limitPerMissingItem":1}')"
apply_without_confirm_response="$(api_post "/api/work-center/projects/${PROJECT_ID}/agent-governance/recommendations:apply" '{"confirmed":false,"viewType":"DRAWING","targetType":"SECTION","items":[]}')"
DOC_TYPE_ID="$(json_data_expr "${doc_response}" "data['rows'][0]['deliverableTypeId']")"
invalid_batch_response="$(api_post "/api/work-center/projects/${PROJECT_ID}/delivery-bindings:batch" "{\"viewType\":\"DOCUMENT\",\"deliverableTypeId\":${DOC_TYPE_ID},\"fileResourceIds\":[],\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"DRAFT\"}")"
assert_ok "${missing_response}"
assert_ok "${recommend_drawing_response}"
assert_not_ok "${apply_without_confirm_response}"
assert_not_ok "${invalid_batch_response}"
for payload in "${missing_response}" "${recommend_drawing_response}" "${apply_without_confirm_response}" "${invalid_batch_response}"; do
  assert_no_forbidden "binding guardrail response" "${payload}"
done
assert_data "missing explanation readable" "${missing_response}" "data['totalCount'] > 0 and all(row['targetName'] in row['explanation'] and row['deliverableDefinitionName'] in row['explanation'] and row['deliverableTypeName'] in row['explanation'] and row['expectedFileKind'] in ['DOCUMENT','DRAWING'] for row in data['rows'][:5])"
assert_data "drawing recommendations safe" "${recommend_drawing_response}" "data['viewType'] == 'DRAWING' and data['targetType'] == 'SECTION' and data['totalCount'] >= 0"
pass "缺失项解释、候选推荐和人工确认/合法参数防线可用"

echo ""
echo "--- 5. Review, rectification and package draft views ---"
document_view_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-views?viewType=DOCUMENT")"
drawing_view_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-views?viewType=DRAWING")"
rectification_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/rectifications")"
package_prepare_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/prepare?targetType=SECTION")"
package_draft_response="$(api_post "/api/work-center/projects/${PROJECT_ID}/delivery-package/drafts" '{"targetType":"SECTION"}')"
for payload in "${document_view_response}" "${drawing_view_response}" "${rectification_response}" "${package_prepare_response}" "${package_draft_response}"; do
  assert_ok "${payload}"
  assert_no_forbidden "review package response" "${payload}"
done
assert_data "delivery views available" "${document_view_response}" "data['viewType'] == 'DOCUMENT' and data['totalCount'] >= 0"
assert_data "drawing view available" "${drawing_view_response}" "data['viewType'] == 'DRAWING' and data['totalCount'] >= 0"
assert_data "rectifications list available" "${rectification_response}" "isinstance(data, list)"
assert_data "package prepare dry run" "${package_prepare_response}" "data['dryRun'] is True and data['physicalPackageGenerated'] is False and data['nasFileCopied'] is False and data['totalCount'] > 0 and data['blockedCount'] == data['missingCount']"
assert_data "package draft dry run" "${package_draft_response}" "data['dryRun'] is True and data['physicalPackageGenerated'] is False and data['nasFileCopied'] is False and data['totalCount'] > 0 and len(data['rows']) == data['totalCount']"
pass "审核/整改查询与交付包草案 dry-run 链路不回归"

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
