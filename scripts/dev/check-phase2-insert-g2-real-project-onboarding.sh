#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-${1:-http://127.0.0.1:8080}}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-}"
TEMPLATE_CODE="${TEMPLATE_CODE:-MEP_BIM_BASIC}"

PASS_COUNT=0
FAIL_COUNT=0

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf 'PASS: %s\n' "$1"
}

fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  printf 'FAIL: %s\n' "$1" >&2
}

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "OK", data' <<< "${response}" >/dev/null
}

assert_not_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] != "OK", data' <<< "${response}" >/dev/null
}

assert_safe_payload() {
  local response="$1"
  if grep -Eqi 'nas://|smb://|afp://|/Volumes/|/Users/|storage_path|storage_uri|storagePath|storageUri|raw row|bearer[[:space:]]+|token[[:space:]]*[:=]|secret[[:space:]]*[:=]|password[[:space:]]*[:=]|select[[:space:]].*from|insert[[:space:]]+into|update[[:space:]].*set|delete[[:space:]]+from' <<< "${response}"; then
    fail "响应包含禁止字段、原始路径、SQL 或密钥痕迹"
    printf '%s\n' "${response}" >&2
    exit 1
  fi
}

login() {
  local response
  response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${response}"
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${response}"
}

api_get() {
  local token="$1"
  local path="$2"
  curl -sS "${BASE_URL}${path}" -H "Authorization: Bearer ${token}"
}

api_post() {
  local token="$1"
  local path="$2"
  local body="$3"
  curl -sS -X POST "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

switch_project() {
  local token="$1"
  local project_id="$2"
  local response
  response="$(api_post "${token}" "/api/core/projects/${project_id}:switch" '{}')"
  assert_ok "${response}"
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${response}"
}

platform_chat() {
  local token="$1"
  local project_id="$2"
  local question="$3"
  local payload
  payload="$(PROJECT_ID_JSON="${project_id}" QUESTION_JSON="${question}" python3 - <<'PY'
import json
import os

print(json.dumps({
    "session_id": "g2-real-project-onboarding-smoke",
    "message": os.environ["QUESTION_JSON"],
    "project_filters": [os.environ["PROJECT_ID_JSON"]],
    "mode": "catalog_lookup",
    "pageType": "g2_real_project_onboarding",
    "projectId": int(os.environ["PROJECT_ID_JSON"]),
    "sourceView": "ProjectAssetView",
    "currentRoute": f"/data-steward/assets/{os.environ['PROJECT_ID_JSON']}/master-data/initialization",
    "pageTitle": "真实项目接入向导",
    "question": os.environ["QUESTION_JSON"],
}, ensure_ascii=False))
PY
)"
  api_post "${token}" "/api/data-steward/chat" "${payload}"
}

echo '== G2 real project onboarding and master-data mapping smoke =='
token="$(login)"
pass "管理员登录成功"

if [[ -z "${PROJECT_ID}" ]]; then
  me_response="$(api_get "${token}" "/api/core/users/me")"
  assert_ok "${me_response}"
  PROJECT_ID="$(parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; current=data.get("currentProject") or {}; projects=data.get("projects") or []; print(current.get("id") or (projects[0]["id"] if projects else ""))' <<< "${me_response}")"
fi

if [[ -z "${PROJECT_ID}" || "${PROJECT_ID}" == "null" ]]; then
  fail "无法确定项目 ID"
  exit 1
fi

token="$(switch_project "${token}" "${PROJECT_ID}")"
pass "已切换到项目 ${PROJECT_ID}"

echo '== asset overview defaults =='
real_projects="$(api_get "${token}" "/api/data-steward/assets/projects?assetSource=NAS_REAL%2A")"
assert_ok "${real_projects}"
assert_safe_payload "${real_projects}"
parse_json '
import json, sys
rows = json.load(sys.stdin)["data"]
assert len(rows) >= 2, rows
for row in rows:
    assert "projectSource" in row, row
    assert "projectCategory" in row, row
    assert "onboardingStatus" in row, row
    assert "fileCount" in row, row
    assert "dominantFileKinds" in row, row
    assert row["projectCategory"] != "TEST_PROJECT", row
    assert row["projectCategory"] != "SAMPLE_TEMPLATE", row
' <<< "${real_projects}" >/dev/null
pass "资产总览默认真实 NAS 项目带有来源/接入状态字段，至少可展示两个真实项目，且不混入测试或样例分类"

all_projects="$(api_get "${token}" "/api/data-steward/assets/projects")"
assert_ok "${all_projects}"
assert_safe_payload "${all_projects}"
parse_json '
import json, sys
rows = json.load(sys.stdin)["data"]
assert isinstance(rows, list), rows
smoke_keywords = ("b6a-smoke", "phase2-", "ph2", "smoke", "test", "测试")
checked = 0
for row in rows:
    assert "projectCategory" in row, row
    assert "onboardingStatus" in row, row
    text = ((row.get("code") or "") + " " + (row.get("name") or "")).lower()
    if any(keyword in text for keyword in smoke_keywords):
        checked += 1
        assert row["projectCategory"] == "TEST_PROJECT", row
        assert row["projectSource"] == "TEST", row
' <<< "${all_projects}" >/dev/null
pass "资产项目全量视图可用于前端主动切换样例/测试/归档筛选，历史 smoke 命名归入测试分类"

echo '== frontend G2-B contract checks =='
grep -q "G2 真实项目治理入口" frontend/src/modules/data-steward/pages/AssetOverviewPage.vue
grep -q "asset-governance-trail" frontend/src/modules/data-steward/pages/AssetOverviewPage.vue
grep -q "hermes-global-entry" frontend/src/modules/core/layout/AppLayout.vue
grep -q "currentRoute" frontend/src/modules/data-steward/api/dataSteward.ts
grep -q "当前上下文" frontend/src/modules/data-steward/components/DataStewardPanel.vue
pass "前端已包含资产总览治理路径、Hermes 常驻入口和页面上下文透传"

echo '== onboarding assessment and dry-run preview =='
assessment="$(api_get "${token}" "/api/master-data/projects/${PROJECT_ID}/onboarding/assessment")"
assert_ok "${assessment}"
assert_safe_payload "${assessment}"
parse_json '
import json, sys
data = json.load(sys.stdin)["data"]
assert data["assetCatalogOnly"] is True, data
assert data["evidenceMode"] == "catalog_only", data
assert data["onboardingStatus"], data
summary = data["assetSummary"]
for key in ("fileCount", "modelFileCount", "drawingFileCount", "documentFileCount", "pathMappingCount", "dominantFileKinds"):
    assert key in summary, data
assert isinstance(data["evidenceClues"], list), data
assert isinstance(data["gaps"], list), data
assert isinstance(data["nextActions"], list), data
' <<< "${assessment}" >/dev/null
pass "接入评估返回 catalog-only 证据、目录汇总、缺口和下一步"

preview="$(api_get "${token}" "/api/master-data/projects/${PROJECT_ID}/onboarding/preview?templateCode=${TEMPLATE_CODE}")"
assert_ok "${preview}"
assert_safe_payload "${preview}"
parse_json '
import json, sys
data = json.load(sys.stdin)["data"]
assert data["dryRun"] is True, data
assert data["confirmedRequired"] is True, data
assert data["nasTouched"] is False, data
assert data["contentRead"] is False, data
assert data["evidenceMode"] == "catalog_only", data
assert data["templateCode"] == "MEP_BIM_BASIC", data
assert isinstance(data["draftItems"], list) and len(data["draftItems"]) > 0, data
assert all(item["pendingConfirmation"] is True for item in data["draftItems"]), data
assert isinstance(data["warnings"], list) and data["warnings"], data
' <<< "${preview}" >/dev/null
pass "接入草案预览为 dry-run，要求人工确认，且声明不触碰 NAS/不读正文"

apply_without_confirm="$(api_post "${token}" "/api/master-data/projects/${PROJECT_ID}/onboarding/apply" "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":false}")"
assert_not_ok "${apply_without_confirm}"
assert_safe_payload "${apply_without_confirm}"
pass "接入草案未人工确认时拒绝应用"

echo '== Hermes project-level natural language =='
path_chat="$(platform_chat "${token}" "${PROJECT_ID}" "这个项目路径在哪里？")"
assert_ok "${path_chat}"
assert_safe_payload "${path_chat}"
parse_json '
import json, sys
data = json.load(sys.stdin)["data"]
assert data["assetCatalogOnly"] is True, data
assert data["queryId"], data
assert data["traceId"], data
assert data["sourceView"] == "ProjectAssetView", data
assert data["permission"]["permissionStatus"] == "allowed", data
assert data["permission"]["projectScopeChecked"] is True, data
assert isinstance(data["pathHints"], list), data
for hint in data["pathHints"]:
    assert "displayPath" in hint and hint["displayPath"].startswith("项目目录："), hint
    assert "pathHint" in hint and "底层路径已隐藏" in hint["pathHint"], hint
' <<< "${path_chat}" >/dev/null
pass "Hermes 项目路径自然语言查询只返回 display_path/path_hint 脱敏提示"

guidance_chat="$(platform_chat "${token}" "${PROJECT_ID}" "这个页面是干什么的，我下一步应该做什么？")"
assert_ok "${guidance_chat}"
assert_safe_payload "${guidance_chat}"
parse_json '
import json, sys
data = json.load(sys.stdin)["data"]
assert data["assetCatalogOnly"] is True, data
assert data["queryId"], data
assert data["traceId"], data
assert data["sourceView"] == "ProjectAssetView", data
assert data["evidenceMode"] in ("catalog_only", "missing_evidence"), data
answer = data.get("answer") or ""
assert "目录" in answer or "治理" in answer or "下一步" in answer, data
' <<< "${guidance_chat}" >/dev/null
pass "Hermes 页面级自然语言引导返回目录级治理说明"

content_chat="$(platform_chat "${token}" "${PROJECT_ID}" "这个 RVT 里面有哪些构件参数？")"
assert_ok "${content_chat}"
assert_safe_payload "${content_chat}"
parse_json '
import json, sys
data = json.load(sys.stdin)["data"]
assert data["status"] == "missing_evidence", data
assert data["evidenceMode"] == "missing_evidence", data
assert data["assetCatalogOnly"] is True, data
reasons = {item["reason"] for item in data["missingEvidence"]}
assert "asset_catalog_only" in reasons, data
assert reasons & {"rvt_parse_evidence_missing", "model_parse_evidence_missing", "component_evidence_missing"}, data
assert data["trace"]["productionRollout"] is False, data
' <<< "${content_chat}" >/dev/null
pass "RVT/DWG/BIM/构件正文类问题返回 Missing Evidence"

printf '== result ==\nPASS=%s FAIL=%s\n' "${PASS_COUNT}" "${FAIL_COUNT}"
if [[ "${FAIL_COUNT}" -ne 0 ]]; then
  exit 1
fi
