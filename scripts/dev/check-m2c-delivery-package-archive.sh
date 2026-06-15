#!/usr/bin/env bash
# M2C: 交付包草案 / 档案目录 / 清单导出 smoke
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
PROJECT_CODE="${PROJECT_CODE:-M2C-SMOKE-${RUN_ID}}"
TEMPLATE_CODE="${TEMPLATE_CODE:-MEP_BIM_BASIC}"

PASS=0
FAIL=0
TOKEN=""
PROJECT_ID=""
DRAFT_ID=""

pass() {
  echo "  [PASS] $1"
  PASS=$((PASS + 1))
}

fail() {
  echo "  [FAIL] $1"
  FAIL=$((FAIL + 1))
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
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

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"]).get("data")
scope = {"__builtins__": {}, "data": data, "len": len, "all": all, "any": any, "str": str, "int": int}
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
scope = {"__builtins__": {}, "data": data, "len": len, "all": all, "any": any, "str": str, "int": int}
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
    r"\bstorage_path\b",
    r"\bstorage_uri\b",
    r"\bstoragePath\b",
    r"\bstorageUri\b",
    r"\braw row\b",
    r"\braw DB row\b",
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

cleanup() {
  if [[ -n "${PROJECT_ID}" ]]; then
    mysql_exec "UPDATE work_delivery_package_drafts SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_user_project_roles SET deleted=1 WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_projects SET deleted=1 WHERE id=${PROJECT_ID};" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "=== M2C: delivery package draft and archive directory smoke ==="

echo ""
echo "--- 1. Login and prepare isolated project ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_data_expr "${login_response}" "data['accessToken']")"
pass "管理员登录成功"

mysql_exec "
INSERT INTO core_projects (code, name, industry_type, owner_org_name, status, asset_source, created_by, updated_by)
VALUES ('${PROJECT_CODE}', 'M2C交付包草案冒烟项目', 'BUILDING_MEP', '验收脚本', 'ACTIVE', 'M2C_SMOKE', 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), deleted = 0, status = 'ACTIVE', asset_source = 'M2C_SMOKE';
SET @project_id = (SELECT id FROM core_projects WHERE code = '${PROJECT_CODE}' AND deleted = 0 LIMIT 1);
INSERT INTO core_user_project_roles (user_id, project_id, role_id, created_by, updated_by)
SELECT u.id, @project_id, r.id, 1, 1
FROM core_users u
JOIN core_roles r ON r.code = 'PROJECT_ADMIN'
WHERE u.username = '${ADMIN_USER}' AND u.deleted = 0
LIMIT 1
ON DUPLICATE KEY UPDATE deleted = 0, role_id = VALUES(role_id);
SELECT @project_id;
" >/tmp/m2c-smoke-project-id.txt
PROJECT_ID="$(tail -1 /tmp/m2c-smoke-project-id.txt)"
if [[ -z "${PROJECT_ID}" ]]; then
  fail "无法创建 M2C smoke 项目"
  exit 1
fi

switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch" '{}')"
assert_ok "${switch_response}"
TOKEN="$(json_data_expr "${switch_response}" "data['accessToken']")"
pass "已创建并切换到隔离 smoke 项目: ${PROJECT_ID}"

apply_response="$(api_post "/api/master-data/projects/${PROJECT_ID}/initialization:apply-template" \
  "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmApply\":true}")"
assert_ok "${apply_response}"
assert_no_forbidden "template apply" "${apply_response}"
pass "已应用交付标准模板，准备生成应交项"

echo ""
echo "--- 2. Prepare read-only delivery package checklist ---"
prepare_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/prepare?targetType=SECTION")"
assert_ok "${prepare_response}"
assert_no_forbidden "prepare response" "${prepare_response}"
assert_data "prepare flags" "${prepare_response}" "data['dryRun'] is True and data['physicalPackageGenerated'] is False and data['nasFileCopied'] is False"
assert_data "prepare has rows" "${prepare_response}" "data['totalCount'] > 0 and len(data['rows']) == data['totalCount']"
assert_data "prepare archive paths are semantic" "${prepare_response}" "all((row['archiveDirectoryPath'] or '').startswith('交付档案/') for row in data['rows'])"
pass "只读预检查返回应交清单、语义档案目录和安全标记"

echo ""
echo "--- 3. Create and inspect draft snapshot ---"
draft_response="$(api_post "/api/work-center/projects/${PROJECT_ID}/delivery-package/drafts" '{"targetType":"SECTION"}')"
assert_ok "${draft_response}"
assert_no_forbidden "draft response" "${draft_response}"
assert_data "draft flags" "${draft_response}" "data['dryRun'] is True and data['physicalPackageGenerated'] is False and data['nasFileCopied'] is False"
DRAFT_ID="$(json_data_expr "${draft_response}" "data['draftId']")"
if [[ -z "${DRAFT_ID}" ]]; then
  fail "草案未返回 draftId"
  exit 1
fi
pass "已生成只读交付包草案: ${DRAFT_ID}"

list_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/drafts")"
assert_ok "${list_response}"
assert_no_forbidden "draft list" "${list_response}"
assert_data "draft list includes new draft" "${list_response}" "any(row['draftId'] == int('${DRAFT_ID}') for row in data)"
pass "草案列表可查询到新草案"

detail_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/drafts/${DRAFT_ID}")"
assert_ok "${detail_response}"
assert_no_forbidden "draft detail" "${detail_response}"
assert_data "draft detail rows" "${detail_response}" "data['draftId'] == int('${DRAFT_ID}') and len(data['rows']) == data['totalCount']"
pass "草案详情返回完整快照"

items_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/drafts/${DRAFT_ID}/items")"
assert_ok "${items_response}"
assert_no_forbidden "draft items" "${items_response}"
assert_data "draft items archive path" "${items_response}" "len(data) > 0 and all((row['archiveDirectoryPath'] or '').startswith('交付档案/') for row in data)"
pass "草案 items 接口返回脱敏档案目录"

echo ""
echo "--- 4. Export manifest and verify OpenAPI/audit ---"
manifest_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/drafts/${DRAFT_ID}:export-manifest")"
assert_ok "${manifest_response}"
assert_no_forbidden "manifest response" "${manifest_response}"
assert_data "manifest unified payload" "${manifest_response}" "data['dryRun'] is True and data['physicalPackageGenerated'] is False and data['nasFileCopied'] is False and data['csvContent'].startswith('\\ufeff')"
assert_data "manifest contains archive path only" "${manifest_response}" "'交付档案/' in data['csvContent'] and 'storage_uri' not in data['csvContent'].lower() and 'nas://' not in data['csvContent'].lower()"
pass "清单导出返回统一响应、traceId 和脱敏 CSV 内容"

openapi_response="$(curl -sS --connect-timeout 3 --max-time 20 "${BASE_URL}/v3/api-docs")"
grep -q "/api/work-center/projects/{projectId}/delivery-package/prepare" <<< "${openapi_response}"
grep -q "/api/work-center/projects/{projectId}/delivery-package/drafts" <<< "${openapi_response}"
grep -q "export-manifest" <<< "${openapi_response}"
pass "OpenAPI 已包含 M2C 接口"

audit_count="$(mysql_exec "SELECT COUNT(1) FROM core_audit_logs WHERE project_id=${PROJECT_ID} AND module_code='work-center' AND action_code IN ('work.delivery-package-draft.create', 'work.delivery-package-draft.export-manifest');")"
if [[ "${audit_count}" -lt 2 ]]; then
  fail "M2C 草案或清单导出审计不足: ${audit_count}"
  exit 1
fi
pass "草案生成和清单导出审计已记录"

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
