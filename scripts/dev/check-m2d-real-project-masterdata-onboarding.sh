#!/usr/bin/env bash
# M2D: 真实项目工程主数据接入草案增强 smoke
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
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

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"]).get("data")
scope = {"__builtins__": {}, "data": data, "len": len, "all": all, "any": any, "str": str, "int": int, "set": set}
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
    assert not re.search(pattern, payload, re.IGNORECASE), f"{label} contains forbidden pattern {pattern}: {payload[:1000]}"
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
scope = {"__builtins__": {}, "data": data, "len": len, "all": all, "any": any, "str": str, "int": int, "set": set}
assert eval(os.environ["CODE"], scope, scope), label
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

echo "=== M2D: real project master-data onboarding draft enhancement ==="

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
echo "--- 2. Assessment contract ---"
status_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/standard-status")"
assert_ok "${status_response}"
assert_no_forbidden "standard status" "${status_response}"
assert_data "真实项目标准仍未就绪" "${status_response}" "data['deliverableStandardReady'] is False"
pass "真实项目工程主数据未被误置为 ready"

assessment_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/onboarding/assessment")"
assert_ok "${assessment_response}"
assert_no_forbidden "assessment" "${assessment_response}"
assert_data "assessment base flags" "${assessment_response}" "data['projectCode'] == '${PROJECT_CODE}' and data['realNasProject'] is True and data['assetCatalogOnly'] is True and data['evidenceMode'] == 'catalog_only'"
assert_data "asset counts" "${assessment_response}" "data['assetSummary']['fileCount'] >= 2000 and data['assetSummary']['drawingFileCount'] >= 2000 and data['assetSummary']['modelFileCount'] >= 100 and data['assetSummary']['documentFileCount'] >= 100 and data['assetSummary']['spreadsheetFileCount'] >= 1"
assert_data "extension distribution" "${assessment_response}" "all(code in {row['code'] for row in data['assetSummary']['extensionDistribution']} for code in ['DWG','PDF','RVT','XLSX'])"
assert_data "discipline distribution" "${assessment_response}" "len({row['label'] for row in data['assetSummary']['disciplineDistribution']} & {'建筑','结构','电气','给排水','消防','智能化','暖通','燃气','通用/未标注'}) >= 4"
assert_data "governance and missing evidence" "${assessment_response}" "len(data['assetSummary']['governanceRisks']) > 0 and any(item['code'] == 'ASSET_CATALOG_ONLY' for item in data['missingEvidence'])"
pass "assessment 返回真实资产统计、分布、治理风险与 Missing Evidence"

echo ""
echo "--- 3. Preview contract ---"
preview_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/onboarding/preview?templateCode=${TEMPLATE_CODE}")"
assert_ok "${preview_response}"
assert_no_forbidden "preview" "${preview_response}"
assert_data "preview flags" "${preview_response}" "data['dryRun'] is True and data['confirmedRequired'] is True and data['nasTouched'] is False and data['contentRead'] is False and data['assetCatalogOnly'] is True and data['evidenceMode'] == 'catalog_only'"
assert_data "draft item metadata" "${preview_response}" "len(data['draftItems']) > 0 and all(item['evidenceMode'] == 'catalog_only' and item['evidenceSource'] and item['confidenceLevel'] and item['riskHint'] and item['pendingConfirmation'] is True for item in data['draftItems'])"
assert_data "real clues and template skeleton separated" "${preview_response}" "any(item['fromRealAssetClue'] is True and item['fromTemplateSkeleton'] is False for item in data['draftItems']) and any(item['fromTemplateSkeleton'] is True for item in data['draftItems'])"
assert_data "draft candidates" "${preview_response}" "all(category in {item['category'] for item in data['draftItems']} for category in ['DISCIPLINE_CANDIDATE','DELIVERABLE_TYPE_CANDIDATE','TARGET_CANDIDATE'])"
assert_data "preview missing evidence" "${preview_response}" "any(item['code'] == 'MODEL_PARSE_EVIDENCE_MISSING' for item in data['missingEvidence']) and any(item['code'] == 'DRAWING_PARSE_EVIDENCE_MISSING' for item in data['missingEvidence'])"
pass "preview 返回资产候选草案、模板参考骨架和证据边界"

echo ""
echo "--- 4. Real project must not become ready via template apply ---"
direct_apply_response="$(api_post "/api/master-data/projects/${PROJECT_ID}/initialization:apply-template" "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmApply\":true}")"
assert_not_ok "${direct_apply_response}"
assert_no_forbidden "direct apply rejected" "${direct_apply_response}"
assert_data "direct apply code" "${direct_apply_response}" "data is None"
pass "真实项目 direct apply-template 被阻断"

onboarding_apply_response="$(api_post "/api/master-data/projects/${PROJECT_ID}/onboarding/apply" "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":true}")"
assert_ok "${onboarding_apply_response}"
assert_no_forbidden "onboarding apply" "${onboarding_apply_response}"
assert_data "onboarding apply is non-writing draft confirmation" "${onboarding_apply_response}" "data['draftApplied'] is False and data['templateResult'] is None and data['nasTouched'] is False and data['contentRead'] is False and data['evidenceMode'] == 'catalog_only'"
pass "真实项目 onboarding apply 只确认草案，不写成正式标准"

status_after_response="$(api_get "/api/master-data/projects/${PROJECT_ID}/standard-status")"
assert_ok "${status_after_response}"
assert_no_forbidden "standard status after" "${status_after_response}"
assert_data "ready remains false" "${status_after_response}" "data['deliverableStandardReady'] is False"
pass "草案确认后交付标准仍未误置为 ready"

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
