#!/usr/bin/env bash
# M1C: 真实项目接入与工程主数据映射修复 MVP — 验收脚本
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
SUFFIX="${SUFFIX:-$(date +%Y%m%d%H%M%S)}"
PROJECT_CODE="${PROJECT_CODE:-M1C-SMOKE-${SUFFIX}}"
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

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 20 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

switch_project() {
  local project_id="$1"
  local response
  response="$(api_post "/api/core/projects/${project_id}:switch" '{}')"
  assert_ok "${response}"
  TOKEN="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${response}")"
}

json_sum_counts() {
  parse_json '
import json, sys
data=json.load(sys.stdin)["data"]
if "templatePreview" in data:
    data=data["templatePreview"]
counts=data
for key in ("willCreate", "created", "willSkip", "skipped", "counts"):
    if key in data:
        counts=data[key]
        break
print(sum(int(counts.get(k, 0) or 0) for k in ("sectionNodes","nodeTypes","deliverableDefinitions","deliverableTypes","deliverableAttributes","directoryTemplates")))
'
}

assert_no_forbidden_fields() {
  local name="$1"
  local response="$2"
  RESPONSE="${response}" python3 - "${name}" <<'PY'
import json
import os
import re
import sys

name = sys.argv[1]
raw = os.environ["RESPONSE"]
patterns = [
    r"/Volumes(?:/|$)",
    r"/Users(?:/|$)",
    r"\bnas://",
    r"\bsmb://",
    r"\bstorage_path\b",
    r"\bstorage_uri\b",
    r"\bstorageUri\b",
    r"\braw_path\b",
    r"\broot_path\b",
    r"\bnas_path\b",
    r"\btoken\b",
    r"\bsecret\b",
    r"\bpassword\b",
    r"\bselect\s+.+\s+from\b",
]
lower = raw.lower()
for pattern in patterns:
    if re.search(pattern, lower, re.IGNORECASE):
        raise AssertionError(f"{name} contains forbidden marker: {pattern}")
data = json.loads(raw)
payload = data.get("data", {})
summary = payload.get("assetSummary", {})
for clue in summary.get("directoryClues", []) or []:
    if str(clue).strip().lower() in {"volumes", "users", "storage", "nas", "smb"}:
        raise AssertionError(f"{name} exposes unsafe directory clue: {clue}")
PY
}

assert_real_project_assessment() {
  local project_id="$1"
  local expected_code="$2"
  local label="$3"
  local response="$4"
  EXPECTED_CODE="${expected_code}" LABEL="${label}" RESPONSE="${response}" python3 - <<'PY'
import json
import os

expected_code = os.environ["EXPECTED_CODE"]
label = os.environ["LABEL"]
data = json.loads(os.environ["RESPONSE"])["data"]
summary = data["assetSummary"]
assert data["projectCode"] == expected_code, (label, data["projectCode"])
assert data["assetCatalogOnly"] is True, label
assert data["evidenceMode"] == "catalog_only", label
assert data["realNasProject"] is True, label
assert summary["fileCount"] > 0, (label, summary)
assert summary["scanTaskCount"] >= 1, (label, summary)
assert summary["dominantFileExtensions"], (label, summary)
assert summary["dominantDisciplines"], (label, summary)
assert summary["directoryClues"], (label, summary)
assert data["nextActions"], label
for clue in data.get("evidenceClues", []):
    assert clue.get("evidenceMode") == "catalog_only", (label, clue)
PY
  assert_no_forbidden_fields "${label} assessment" "${response}"
}

assert_preview_contract() {
  local label="$1"
  local response="$2"
  RESPONSE="${response}" LABEL="${label}" python3 - <<'PY'
import json
import os

label = os.environ["LABEL"]
data = json.loads(os.environ["RESPONSE"])["data"]
assert data["assetCatalogOnly"] is True, label
assert data["evidenceMode"] == "catalog_only", label
assert data["dryRun"] is True, label
assert data["confirmedRequired"] is True, label
assert data["nasTouched"] is False, label
assert data["contentRead"] is False, label
items = data.get("draftItems") or []
assert items, label
for item in items:
    assert item["evidenceMode"] == "catalog_only", (label, item)
    assert item["evidenceSource"], (label, item)
    assert item["confidenceLevel"], (label, item)
    assert item["riskHint"], (label, item)
    assert item["pendingConfirmation"] is True, (label, item)
PY
  assert_no_forbidden_fields "${label} preview" "${response}"
}

echo "=== M1C: real project master-data onboarding ==="

echo ""
echo "--- 1. Login ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 10 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${login_response}")"
pass "管理员登录成功"

echo ""
echo "--- 2. Real project 105/503 assessment and preview ---"
switch_project 503
assessment_503="$(api_get "/api/master-data/projects/503/onboarding/assessment")"
assert_ok "${assessment_503}"
assert_real_project_assessment 503 "105" "project-105/503" "${assessment_503}"
pass "105/503 真实项目接入评估满足 catalog-only 合同"

preview_503="$(api_get "/api/master-data/projects/503/onboarding/preview?templateCode=${TEMPLATE_CODE}")"
assert_ok "${preview_503}"
assert_preview_contract "project-105/503" "${preview_503}"
pass "105/503 草案预览满足 dry-run、人工确认、无 NAS 触碰"

echo ""
echo "--- 3. Real project 93/506 assessment, preview, confirm gate ---"
switch_project 506
assessment_506="$(api_get "/api/master-data/projects/506/onboarding/assessment")"
assert_ok "${assessment_506}"
assert_real_project_assessment 506 "93" "project-93/506" "${assessment_506}"
pass "93/506 真实项目接入评估满足 catalog-only 合同"

preview_506="$(api_get "/api/master-data/projects/506/onboarding/preview?templateCode=${TEMPLATE_CODE}")"
assert_ok "${preview_506}"
assert_preview_contract "project-93/506" "${preview_506}"
RESPONSE="${preview_506}" python3 - <<'PY'
import json
import os

items = json.loads(os.environ["RESPONSE"])["data"]["draftItems"]
sources = {item["evidenceSource"] for item in items}
assert "TEMPLATE_SKELETON" in sources, sources
assert "CATALOG_FILE_KIND_CLUE" in sources or "CATALOG_DIRECTORY_CLUE" in sources, sources
PY
pass "93/506 草案同时表达模板骨架与资产目录线索"

apply_without_confirm="$(api_post "/api/master-data/projects/506/onboarding/apply" "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":false}")"
assert_not_ok "${apply_without_confirm}"
pass "93/506 未人工确认时禁止应用真实项目接入草案"

echo ""
echo "--- 4. Create isolated M1C smoke project ---"
mysql_exec "
INSERT INTO core_projects (code, name, industry_type, owner_org_name, status, asset_source, created_by, updated_by)
VALUES ('${PROJECT_CODE}', 'M1C真实项目主数据冒烟项目', 'BUILDING_MEP', '验收脚本', 'ACTIVE', 'NAS_REAL_SMOKE', 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), deleted = 0, status = 'ACTIVE', asset_source = 'NAS_REAL_SMOKE';
SET @project_id = (SELECT id FROM core_projects WHERE code = '${PROJECT_CODE}' AND deleted = 0 LIMIT 1);
INSERT IGNORE INTO core_user_project_roles (user_id, project_id, role_id, created_by, updated_by)
VALUES (1, @project_id, 1, 1, 1);
INSERT INTO data_asset_project_path_mappings (project_id, provider_code, nas_path, match_strategy, enabled, sort_order, remark, created_by, updated_by)
VALUES (@project_id, 'NAS', CONCAT('m1c-smoke/', '${PROJECT_CODE}'), 'PREFIX', 1, 0, 'M1C smoke path mapping only', 1, 1)
ON DUPLICATE KEY UPDATE enabled = 1, deleted = 0, updated_by = 1;
INSERT INTO data_asset_scan_tasks (root_code, root_path, project_id, project_code, \`recursive\`, extensions, status, progress_message, total_scanned, auto_ingested, pending_review, failed_count, started_at, completed_at, created_by, updated_by)
VALUES (CONCAT('M1C-SMOKE-', '${SUFFIX}'), CONCAT('m1c-smoke/', '${PROJECT_CODE}'), @project_id, '${PROJECT_CODE}', 1, 'rvt,dwg,pdf', 'COMPLETED', 'M1C smoke catalog-only scan record', 3, 3, 0, 0, NOW(), NOW(), 1, 1);
INSERT INTO data_file_resources (
  project_id, original_name, file_kind, mime_type, size_bytes, storage_uri, logical_path,
  storage_provider, business_tag, version_no, process_status, review_status, confidence_level,
  discipline, source_type, last_verified_at, created_by, updated_by
) VALUES
(@project_id, 'M1C-建筑模型.rvt', 'MODEL', 'application/octet-stream', 4096, CONCAT('mock://m1c/', '${PROJECT_CODE}', '/model.rvt'), '模型/RVT/M1C-建筑模型.rvt', 'NAS', 'M1C_SMOKE', 'V1', 'PROCESSED', 'APPROVED', 'HIGH', 'ARCHITECTURE', 'NAS_SCAN', NOW(), 1, 1),
(@project_id, 'M1C-消防图纸.dwg', 'DRAWING', 'application/acad', 2048, CONCAT('mock://m1c/', '${PROJECT_CODE}', '/drawing.dwg'), '图纸/DWG/M1C-消防图纸.dwg', 'NAS', 'M1C_SMOKE', 'V1', 'PROCESSED', 'APPROVED', 'HIGH', 'FIRE_PROTECTION', 'NAS_SCAN', NOW(), 1, 1),
(@project_id, 'M1C-交付说明.pdf', 'DOCUMENT', 'application/pdf', 1024, CONCAT('mock://m1c/', '${PROJECT_CODE}', '/readme.pdf'), '文档/PDF/M1C-交付说明.pdf', 'NAS', 'M1C_SMOKE', 'V1', 'PROCESSED', 'APPROVED', 'HIGH', 'GENERAL', 'NAS_SCAN', NOW(), 1, 1);
SELECT @project_id;
" >/tmp/m1c-smoke-project-id.txt
SMOKE_PROJECT_ID="$(tail -1 /tmp/m1c-smoke-project-id.txt)"
echo "project=${SMOKE_PROJECT_ID} code=${PROJECT_CODE}"
pass "隔离 smoke 项目与目录级测试数据已创建"

echo ""
echo "--- 5. Smoke project onboarding flow ---"
switch_project "${SMOKE_PROJECT_ID}"
smoke_assessment="$(api_get "/api/master-data/projects/${SMOKE_PROJECT_ID}/onboarding/assessment")"
assert_ok "${smoke_assessment}"
assert_real_project_assessment "${SMOKE_PROJECT_ID}" "${PROJECT_CODE}" "M1C smoke" "${smoke_assessment}"
pass "smoke 项目评估返回真实项目来源、资产线索和脱敏目录线索"

smoke_preview_before="$(api_get "/api/master-data/projects/${SMOKE_PROJECT_ID}/onboarding/preview?templateCode=${TEMPLATE_CODE}")"
assert_ok "${smoke_preview_before}"
assert_preview_contract "M1C smoke before apply" "${smoke_preview_before}"
create_count_before="$(json_sum_counts <<< "${smoke_preview_before}")"
if [[ "${create_count_before}" -le 0 ]]; then
  fail "smoke 项目应用前没有待创建草案项"
  exit 1
fi
pass "smoke 项目应用前草案存在待创建项"

smoke_apply_without_confirm="$(api_post "/api/master-data/projects/${SMOKE_PROJECT_ID}/onboarding/apply" "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":false}")"
assert_not_ok "${smoke_apply_without_confirm}"
pass "smoke 项目未确认时禁止应用草案"

smoke_apply="$(api_post "/api/master-data/projects/${SMOKE_PROJECT_ID}/onboarding/apply" "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":true}")"
assert_ok "${smoke_apply}"
RESPONSE="${smoke_apply}" python3 - <<'PY'
import json
import os

data = json.loads(os.environ["RESPONSE"])["data"]
assert data["draftApplied"] is True, data
assert data["nasTouched"] is False, data
assert data["contentRead"] is False, data
template = data["templateResult"]
assert sum(template["created"].values()) > 0, template
assert template["standardStatus"]["deliverableStandardReady"] is True, template
PY
assert_no_forbidden_fields "M1C smoke apply" "${smoke_apply}"
pass "smoke 项目确认后可应用草案，且不触碰 NAS / 不读正文"

smoke_preview_after="$(api_get "/api/master-data/projects/${SMOKE_PROJECT_ID}/onboarding/preview?templateCode=${TEMPLATE_CODE}")"
assert_ok "${smoke_preview_after}"
assert_preview_contract "M1C smoke after apply" "${smoke_preview_after}"
RESPONSE="${smoke_preview_after}" python3 - <<'PY'
import json
import os

data = json.loads(os.environ["RESPONSE"])["data"]
data = data.get("templatePreview", data)
assert sum(data["willCreate"].values()) == 0, data
assert sum(data["willSkip"].values()) > 0, data
PY
pass "smoke 项目应用后预览表现为幂等跳过"

smoke_apply_again="$(api_post "/api/master-data/projects/${SMOKE_PROJECT_ID}/onboarding/apply" "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmed\":true}")"
assert_ok "${smoke_apply_again}"
RESPONSE="${smoke_apply_again}" python3 - <<'PY'
import json
import os

template = json.loads(os.environ["RESPONSE"])["data"]["templateResult"]
assert sum(template["created"].values()) == 0, template
assert sum(template["skipped"].values()) > 0, template
PY
assert_no_forbidden_fields "M1C smoke apply again" "${smoke_apply_again}"
pass "smoke 项目重复应用不覆盖已有主数据"

echo ""
echo "--- 6. Audit event ---"
audit_count="$(mysql_exec "SELECT COUNT(1) FROM core_audit_logs WHERE project_id = ${SMOKE_PROJECT_ID} AND module_code = 'master-data' AND action_code = 'masterdata.initialization.template-apply';")"
if [[ "${audit_count}" -lt 1 ]]; then
  fail "缺少 masterdata.initialization.template-apply 审计记录"
  exit 1
fi
pass "主数据初始化应用审计记录存在: ${audit_count}"

echo ""
echo "M1C check complete: PASS=${PASS} FAIL=${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
