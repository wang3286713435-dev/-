#!/usr/bin/env bash
# M2E: 真实项目工程主数据人工确认与交付规则落地 smoke
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
PROJECT_CODE="${PROJECT_CODE:-105}"
TEMPLATE_CODE="${TEMPLATE_CODE:-MEP_BIM_BASIC}"

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
scope = {"__builtins__": {}, "data": data, "len": len, "all": all, "any": any, "sum": sum, "int": int, "str": str, "set": set}
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
scope = {"__builtins__": {}, "data": data, "len": len, "all": all, "any": any, "sum": sum, "int": int, "str": str, "set": set}
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

echo "=== M2E: real project master-data manual confirmation ==="

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
echo "--- 2. Pre-confirmation state and guardrails ---"
status_before="$(api_get "/api/master-data/projects/${PROJECT_ID}/standard-status")"
assert_ok "${status_before}"
assert_no_forbidden "status before" "${status_before}"
ready_before="$(json_data_expr "${status_before}" "data['deliverableStandardReady']")"
if [[ "${ready_before}" == "false" ]]; then
  pass "105 确认前 deliverableStandardReady=false"
  doc_before="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-completeness?viewType=DOCUMENT&targetType=SECTION")"
  drawing_before="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-completeness?viewType=DRAWING&targetType=SECTION")"
  package_before="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/prepare?viewType=DOCUMENT&targetType=SECTION")"
  assert_ok "${doc_before}"
  assert_ok "${drawing_before}"
  assert_ok "${package_before}"
  assert_no_forbidden "doc before" "${doc_before}"
  assert_no_forbidden "drawing before" "${drawing_before}"
  assert_no_forbidden "package before" "${package_before}"
  assert_data "no fake doc/drawing rows before confirm" "${doc_before}" "data['standardReady'] is False and data['totalRequired'] == 0 and len(data['rows']) == 0"
  assert_data "no fake drawing rows before confirm" "${drawing_before}" "data['standardReady'] is False and data['totalRequired'] == 0 and len(data['rows']) == 0"
  assert_data "no fake package rows before confirm" "${package_before}" "data['totalCount'] == 0 and data['dryRun'] is True and data['physicalPackageGenerated'] is False and data['nasFileCopied'] is False"
  pass "确认前文档/图纸交付和交付包不出现虚假应交项"
else
  pass "105 已处于 M2E 人工确认后状态，继续验证接口幂等与联动"
fi

reject_response="$(api_post "/api/master-data/projects/${PROJECT_ID}/onboarding/confirm" \
  "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":false,\"confirmationMode\":\"MANUAL_REVIEW\",\"riskAccepted\":true}")"
assert_not_ok "${reject_response}"
assert_no_forbidden "confirm reject" "${reject_response}"
pass "未带 confirmed=true 时拒绝人工确认"

empty_selection_response="$(api_post "/api/master-data/projects/${PROJECT_ID}/onboarding/confirm" \
  "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":true,\"confirmationMode\":\"MANUAL_REVIEW\",\"selectedDraftItemIds\":[],\"sectionStrategy\":\"DISCIPLINE_LEVEL\",\"nodeTypeStrategy\":\"LOCK_CONFIRMED\",\"deliverableStrategy\":\"FILE_TYPE_MINIMAL\",\"riskAccepted\":true}")"
assert_not_ok "${empty_selection_response}"
assert_no_forbidden "empty selection reject" "${empty_selection_response}"
pass "空 selectedDraftItemIds 被拒绝"

invalid_selection_response="$(api_post "/api/master-data/projects/${PROJECT_ID}/onboarding/confirm" \
  "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":true,\"confirmationMode\":\"MANUAL_REVIEW\",\"selectedDraftItemIds\":[\"NOT_A_REAL_DRAFT_ITEM_ID\"],\"sectionStrategy\":\"DISCIPLINE_LEVEL\",\"nodeTypeStrategy\":\"LOCK_CONFIRMED\",\"deliverableStrategy\":\"FILE_TYPE_MINIMAL\",\"riskAccepted\":true}")"
assert_not_ok "${invalid_selection_response}"
assert_no_forbidden "invalid selection reject" "${invalid_selection_response}"
pass "无效 selectedDraftItemIds 被拒绝"

preview_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/onboarding/preview?templateCode=${TEMPLATE_CODE}")"
assert_ok "${preview_response}"
assert_no_forbidden "onboarding preview" "${preview_response}"
selected_ids_json="$(RESPONSE="${preview_response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])["data"]
items = payload.get("draftItems") or []
selected = [
    f"{item['category']}:{item['name']}:{item['evidenceSource']}"
    for item in items
    if item.get("fromRealAssetClue")
]
if not selected and items:
    item = items[0]
    selected = [f"{item['category']}:{item['name']}:{item['evidenceSource']}"]
assert selected, payload
print(json.dumps(selected, ensure_ascii=False))
PY
)"
small_selection_json="$(RESPONSE="${preview_response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])["data"]
items = payload.get("draftItems") or []
preferred = None
for item in items:
    if item.get("category") == "DISCIPLINE_CANDIDATE":
        preferred = item
        break
if preferred is None:
    preferred = items[0]
print(json.dumps([f"{preferred['category']}:{preferred['name']}:{preferred['evidenceSource']}"], ensure_ascii=False))
PY
)"
pass "已获取后端草案项 ID，用于验证选择契约"

echo ""
echo "--- 3. Manual confirmation generates formal master data ---"
confirm_body="{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":true,\"confirmationMode\":\"MANUAL_REVIEW\",\"selectedDraftItemIds\":${selected_ids_json},\"sectionStrategy\":\"DISCIPLINE_LEVEL\",\"nodeTypeStrategy\":\"LOCK_CONFIRMED\",\"deliverableStrategy\":\"FILE_TYPE_MINIMAL\",\"riskAccepted\":true}"
confirm_response="$(api_post "/api/master-data/projects/${PROJECT_ID}/onboarding/confirm" "${confirm_body}")"
assert_ok "${confirm_response}"
assert_no_forbidden "confirm response" "${confirm_response}"
assert_data "confirm flags" "${confirm_response}" "data['confirmed'] is True and data['formalMasterDataGenerated'] is True and data['nasTouched'] is False and data['contentRead'] is False and data['evidenceMode'] == 'catalog_only'"
assert_data "confirm creates or keeps data" "${confirm_response}" "sum(data['created'].values()) > 0 or sum(data['skipped'].values()) > 0"
assert_data "confirm result has followups and sources" "${confirm_response}" "len(data['manualFollowUps']) > 0 and any(row['source'] for row in data['generatedItems']) and any(row['evidenceMode'] == 'catalog_only' for row in data['generatedItems'])"
if [[ "${ready_before}" == "false" ]]; then
  assert_data "first confirm created formal data" "${confirm_response}" "data['created']['sectionNodes'] > 0 and data['created']['nodeTypes'] > 0 and data['created']['deliverableDefinitions'] > 0 and data['created']['deliverableTypes'] > 0 and data['created']['directoryTemplates'] > 0"
fi
pass "人工确认后生成或保持正式工程主数据，并返回来源与后续人工补充事项"

small_selection_response="$(api_post "/api/master-data/projects/${PROJECT_ID}/onboarding/confirm" \
  "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":true,\"confirmationMode\":\"MANUAL_REVIEW\",\"selectedDraftItemIds\":${small_selection_json},\"sectionStrategy\":\"DISCIPLINE_LEVEL\",\"nodeTypeStrategy\":\"LOCK_CONFIRMED\",\"deliverableStrategy\":\"FILE_TYPE_MINIMAL\",\"riskAccepted\":true}")"
assert_ok "${small_selection_response}"
assert_no_forbidden "small selection response" "${small_selection_response}"
assert_data "small legal selection is not full apply" "${small_selection_response}" "0 < len(data['generatedItems']) < 40"
pass "合法小选择只返回所选草案项及必要依赖，不再全量返回 40 项"

status_after="$(api_get "/api/master-data/projects/${PROJECT_ID}/standard-status")"
assert_ok "${status_after}"
assert_no_forbidden "status after" "${status_after}"
assert_data "standard ready after confirm" "${status_after}" "data['deliverableStandardReady'] is True and data['hasSectionTree'] is True and data['nodeTypesLocked'] is True and data['hasDeliverableDefinitions'] is True and data['hasDeliverableTypes'] is True and data['hasDeliverableAttributes'] is True and data['hasDirectoryTemplates'] is True"
pass "standard-status 已反映正式规则就绪"

echo ""
echo "--- 4. Delivery and package linkage ---"
doc_after="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-completeness?viewType=DOCUMENT&targetType=SECTION")"
drawing_after="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-completeness?viewType=DRAWING&targetType=SECTION")"
package_after="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/prepare?targetType=SECTION")"
assert_ok "${doc_after}"
assert_ok "${drawing_after}"
assert_ok "${package_after}"
assert_no_forbidden "doc after" "${doc_after}"
assert_no_forbidden "drawing after" "${drawing_after}"
assert_no_forbidden "package after" "${package_after}"
assert_data "document delivery uses formal rules" "${doc_after}" "data['standardReady'] is True and data['totalRequired'] > 0 and len(data['rows']) == data['totalRequired']"
assert_data "drawing delivery uses formal rules" "${drawing_after}" "data['standardReady'] is True and data['totalRequired'] > 0 and len(data['rows']) == data['totalRequired']"
assert_data "package prepare uses formal rules" "${package_after}" "data['dryRun'] is True and data['physicalPackageGenerated'] is False and data['nasFileCopied'] is False and data['totalCount'] > 0 and len(data['rows']) == data['totalCount']"
pass "文档/图纸交付与交付包准备视图已基于正式规则工作"

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
