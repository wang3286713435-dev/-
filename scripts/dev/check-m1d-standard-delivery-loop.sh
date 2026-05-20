#!/usr/bin/env bash
# M1D: 标准驱动交付闭环强化 — 验收脚本
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
PROJECT_CODE="${PROJECT_CODE:-M1D-SMOKE-${RUN_ID}}"
TEMPLATE_CODE="${TEMPLATE_CODE:-MEP_BIM_BASIC}"

PASS=0
FAIL=0
TOKEN=""
SMOKE_PROJECT_ID=""

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

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

assert_ok() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY' >/dev/null
import json
import os
data = json.loads(os.environ["RESPONSE"])
assert data.get("code") == "OK", data
PY
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 25 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 25 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os

data = json.loads(os.environ["RESPONSE"]).get("data")
value = eval(os.environ["EXPR"], {"__builtins__": {}}, {"data": data, "len": len, "str": str, "int": int})
if isinstance(value, bool):
    print("true" if value else "false")
elif value is None:
    print("")
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
assert eval(os.environ["CODE"], {"__builtins__": {}}, {"data": data, "len": len, "any": any, "all": all, "sum": sum, "str": str}), label
PY
}

assert_no_forbidden_fields() {
  local name="$1"
  local response="$2"
  RESPONSE="${response}" python3 - "${name}" <<'PY'
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
for pattern in patterns:
    if re.search(pattern, raw, re.IGNORECASE):
        raise AssertionError(f"{name} contains forbidden marker: {pattern}")
PY
}

switch_project() {
  local project_id="$1"
  local response
  response="$(api_post "/api/core/projects/${project_id}:switch" '{}')"
  assert_ok "${response}"
  TOKEN="$(json_data_expr "${response}" "data['accessToken']")"
}

create_file() {
  local kind="$1"
  local name="$2"
  local mime="$3"
  local uri="$4"
  local response
  response="$(api_post "/api/data-steward/projects/${SMOKE_PROJECT_ID}/file-resources" \
    "{\"originalName\":\"${name}\",\"fileKind\":\"${kind}\",\"mimeType\":\"${mime}\",\"sizeBytes\":2048,\"storageUri\":\"${uri}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
  assert_ok "${response}"
  json_data_expr "${response}" "data['id']"
}

first_binding_id() {
  local response="$1"
  json_data_expr "${response}" "data['createdBindings'][0]['id'] if data['createdBindings'] else ''"
}

echo "=== M1D: 标准驱动交付闭环强化 ==="

echo ""
echo "--- 1. Login ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 10 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_data_expr "${login_response}" "data['accessToken']")"
pass "管理员登录成功"

echo ""
echo "--- 2. Create isolated smoke project and apply standard template ---"
mysql_exec "
INSERT INTO core_projects (code, name, industry_type, owner_org_name, status, asset_source, created_by, updated_by)
VALUES ('${PROJECT_CODE}', 'M1D标准驱动交付闭环冒烟项目', 'BUILDING_MEP', '验收脚本', 'ACTIVE', 'NAS_CATALOG_SMOKE', 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), deleted = 0, status = 'ACTIVE', asset_source = 'NAS_CATALOG_SMOKE';
SET @project_id = (SELECT id FROM core_projects WHERE code = '${PROJECT_CODE}' AND deleted = 0 LIMIT 1);
INSERT IGNORE INTO core_user_project_roles (user_id, project_id, role_id, created_by, updated_by)
VALUES (1, @project_id, 1, 1, 1);
SELECT @project_id;
" >/tmp/m1d-smoke-project-id.txt
SMOKE_PROJECT_ID="$(tail -1 /tmp/m1d-smoke-project-id.txt)"
if [[ -z "${SMOKE_PROJECT_ID}" ]]; then
  fail "无法创建 M1D smoke 项目"
  exit 1
fi
switch_project "${SMOKE_PROJECT_ID}"
pass "已创建并切换到隔离 smoke 项目: ${SMOKE_PROJECT_ID}"

apply_response="$(api_post "/api/master-data/projects/${SMOKE_PROJECT_ID}/initialization:apply-template" \
  "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmApply\":true}")"
assert_ok "${apply_response}"
assert_data "template apply ready" "${apply_response}" "sum(data['created'].values()) > 0 and data['standardStatus']['deliverableStandardReady'] is True"
assert_no_forbidden_fields "template apply" "${apply_response}"
pass "标准模板已应用，交付标准 ready"

echo ""
echo "--- 3. Prepare smoke files and targets ---"
DOC_FILE_APPROVE_ID="$(create_file "DOCUMENT" "M1D-审核通过资料-${RUN_ID}.pdf" "application/pdf" "mock://m1d/${RUN_ID}/approve.pdf")"
DOC_FILE_REJECT_ID="$(create_file "DOCUMENT" "M1D-整改驳回资料-${RUN_ID}.pdf" "application/pdf" "mock://m1d/${RUN_ID}/reject.pdf")"
DWG_FILE_ID="$(create_file "DRAWING" "M1D-图纸交付资料-${RUN_ID}.dwg" "application/acad" "mock://m1d/${RUN_ID}/drawing.dwg")"
pass "已创建目录级测试文件，不访问真实 NAS 文件正文"

sections_response="$(api_get "/api/master-data/projects/${SMOKE_PROJECT_ID}/section-nodes/tree")"
assert_ok "${sections_response}"
read -r SECTION_ID_ONE SECTION_ID_TWO < <(RESPONSE="${sections_response}" python3 - <<'PY'
import json
import os

items = []
def walk(nodes):
    for node in nodes or []:
        if node.get("status") == "ACTIVE":
            items.append(str(node["id"]))
        walk(node.get("children") or [])
walk(json.loads(os.environ["RESPONSE"])["data"])
while len(items) < 2:
    items.append("")
print(" ".join(items[:2]))
PY
)
if [[ -z "${SECTION_ID_TWO}" ]]; then
  section_create="$(api_post "/api/master-data/projects/${SMOKE_PROJECT_ID}/section-nodes" \
    "{\"code\":\"M1D-EXTRA-${RUN_ID}\",\"name\":\"M1D整改补交部位\",\"parentId\":null,\"status\":\"ACTIVE\",\"sortOrder\":9999}")"
  assert_ok "${section_create}"
  SECTION_ID_TWO="$(json_data_expr "${section_create}" "data['id']")"
fi
if [[ -z "${SECTION_ID_ONE}" || -z "${SECTION_ID_TWO}" ]]; then
  fail "缺少可用工程部位"
  exit 1
fi
pass "可用工程部位已准备: ${SECTION_ID_ONE}, ${SECTION_ID_TWO}"

types_response="$(api_get "/api/master-data/projects/${SMOKE_PROJECT_ID}/deliverable-types")"
assert_ok "${types_response}"
DOC_TYPE_ID="$(RESPONSE="${types_response}" python3 - <<'PY'
import json
import os
for row in json.loads(os.environ["RESPONSE"])["data"]:
    if row.get("fileKind") == "DOCUMENT" and row.get("bindingStrategy") == "SECTION_NODE":
        print(row["id"])
        break
PY
)"
DWG_TYPE_ID="$(RESPONSE="${types_response}" python3 - <<'PY'
import json
import os
for row in json.loads(os.environ["RESPONSE"])["data"]:
    if row.get("fileKind") == "DRAWING" and row.get("bindingStrategy") == "SECTION_NODE":
        print(row["id"])
        break
PY
)"
if [[ -z "${DOC_TYPE_ID}" ]]; then
  definitions_response="$(api_get "/api/master-data/projects/${SMOKE_PROJECT_ID}/deliverable-definitions")"
  assert_ok "${definitions_response}"
  DEF_ID="$(RESPONSE="${definitions_response}" python3 - <<'PY'
import json
import os
rows = json.loads(os.environ["RESPONSE"])["data"]
print(rows[0]["id"] if rows else "")
PY
)"
  if [[ -z "${DEF_ID}" ]]; then
    fail "缺少交付物定义，无法创建 M1D 文档补交类型"
    exit 1
  fi
  doc_type_create="$(api_post "/api/master-data/projects/${SMOKE_PROJECT_ID}/deliverable-types" \
    "{\"deliverableDefinitionId\":${DEF_ID},\"code\":\"M1D-DOC-SEC-${RUN_ID}\",\"name\":\"M1D文档部位补交资料\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}")"
  assert_ok "${doc_type_create}"
  DOC_TYPE_ID="$(json_data_expr "${doc_type_create}" "data['id']")"
fi
if [[ -z "${DOC_TYPE_ID}" || -z "${DWG_TYPE_ID}" ]]; then
  fail "缺少 SECTION_NODE 文档/图纸交付物类型"
  exit 1
fi
pass "已找到文档/图纸交付物类型"

echo ""
echo "--- 4. Verify initial missing state and new completeness fields ---"
doc_completeness_before="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-completeness?viewType=DOCUMENT&targetType=SECTION")"
assert_ok "${doc_completeness_before}"
assert_data "document completeness before" "${doc_completeness_before}" "data['standardReady'] is True and data['totalRequired'] > 0 and data['missingCount'] > 0 and data['nextActionCode'] == 'BIND_MISSING_FILES' and 'approvedRate' in data and 'nextActionText' in data"
assert_no_forbidden_fields "document completeness before" "${doc_completeness_before}"
pass "文档交付完整率可说明缺失、审核状态和下一步动作"

echo ""
echo "--- 5. Draft -> submit -> approve -> export precheck ---"
draft_batch="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings:batch" \
  "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":${SECTION_ID_ONE},\"deliverableTypeId\":${DOC_TYPE_ID},\"fileResourceIds\":[${DOC_FILE_APPROVE_ID}],\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"DRAFT\",\"remark\":\"M1D approve flow\"}")"
assert_ok "${draft_batch}"
APPROVE_BINDING_ID="$(first_binding_id "${draft_batch}")"
if [[ -z "${APPROVE_BINDING_ID}" ]]; then
  fail "草稿挂接未创建"
  exit 1
fi
assert_no_forbidden_fields "draft batch" "${draft_batch}"
pass "文件补交先保存为草稿"

doc_completeness_draft="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-completeness?viewType=DOCUMENT&targetType=SECTION")"
assert_ok "${doc_completeness_draft}"
assert_data "document draft state" "${doc_completeness_draft}" "data['draftCount'] > 0 and data['nextActionCode'] == 'SUBMIT_REVIEW'"
pass "完整率返回草稿数量，并提示提交审核"

submit_response="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings/${APPROVE_BINDING_ID}:submit-review" '{}')"
assert_ok "${submit_response}"
assert_data "submit review" "${submit_response}" "data['reviewStatus'] == 'PENDING'"
pass "草稿可提交审核"

doc_completeness_pending="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-completeness?viewType=DOCUMENT&targetType=SECTION")"
assert_ok "${doc_completeness_pending}"
assert_data "document pending state" "${doc_completeness_pending}" "data['pendingReviewCount'] > 0 and data['nextActionCode'] == 'REVIEW_PENDING'"
pass "完整率返回待审数量，并提示审核处理"

approve_response="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings/${APPROVE_BINDING_ID}:approve" '{}')"
assert_ok "${approve_response}"
assert_data "approve binding" "${approve_response}" "data['reviewStatus'] == 'APPROVED'"
pass "待审资料可审核通过"

doc_completeness_approved="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-completeness?viewType=DOCUMENT&targetType=SECTION")"
assert_ok "${doc_completeness_approved}"
assert_data "document approved state" "${doc_completeness_approved}" "data['approvedCount'] > 0 and data['reviewReadyCount'] > 0 and data['approvedRate'] > 0"
assert_no_forbidden_fields "document approved completeness" "${doc_completeness_approved}"
pass "完整率返回已通过、可导出基础数量和审核通过率"

precheck_doc="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-package/export-precheck?viewType=DOCUMENT&targetType=SECTION")"
assert_ok "${precheck_doc}"
assert_data "document precheck" "${precheck_doc}" "data['dryRun'] is True and data['packageGenerated'] is False and data['readyCount'] > 0 and data['missingCount'] > 0"
assert_no_forbidden_fields "document export precheck" "${precheck_doc}"
pass "导出预检查为只读 dry-run，已通过项可进入 READY"

echo ""
echo "--- 6. Reject -> rectification -> resolve/close/reopen -> re-approve ---"
reject_batch="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings:batch" \
  "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":${SECTION_ID_TWO},\"deliverableTypeId\":${DOC_TYPE_ID},\"fileResourceIds\":[${DOC_FILE_REJECT_ID}],\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"DRAFT\",\"remark\":\"M1D reject flow\"}")"
assert_ok "${reject_batch}"
REJECT_BINDING_ID="$(first_binding_id "${reject_batch}")"
if [[ -z "${REJECT_BINDING_ID}" ]]; then
  fail "驳回流程挂接未创建"
  exit 1
fi
api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings/${REJECT_BINDING_ID}:submit-review" '{}' >/dev/null
reject_response="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings/${REJECT_BINDING_ID}:reject" \
  '{"reason":"M1D smoke 驳回原因：资料版本与标准不一致"}')"
assert_ok "${reject_response}"
assert_data "reject binding" "${reject_response}" "data['reviewStatus'] == 'REJECTED'"
pass "审核驳回会返回 REJECTED"

doc_completeness_rejected="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-completeness?viewType=DOCUMENT&targetType=SECTION")"
assert_ok "${doc_completeness_rejected}"
assert_data "document rejected state" "${doc_completeness_rejected}" "data['rejectedCount'] > 0 and data['nextActionCode'] == 'HANDLE_RECTIFICATION'"
pass "完整率返回驳回数量，并提示进入整改闭环"

rectifications_response="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/rectifications")"
assert_ok "${rectifications_response}"
RECTIFICATION_ID="$(RESPONSE="${rectifications_response}" BINDING_ID="${REJECT_BINDING_ID}" python3 - <<'PY'
import json
import os
for row in json.loads(os.environ["RESPONSE"])["data"]:
    if str(row.get("bindingId")) == os.environ["BINDING_ID"]:
        print(row["id"])
        break
PY
)"
if [[ -z "${RECTIFICATION_ID}" ]]; then
  fail "驳回后未找到整改项"
  exit 1
fi
assert_no_forbidden_fields "rectifications list" "${rectifications_response}"
pass "驳回后已生成整改项"

resolve_response="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/rectifications/${RECTIFICATION_ID}:resolve" \
  '{"resolutionNote":"M1D smoke 已完成整改说明"}')"
assert_ok "${resolve_response}"
assert_data "resolve rectification" "${resolve_response}" "data['status'] == 'RESOLVED'"
pass "整改项可标记已处理"

close_response="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/rectifications/${RECTIFICATION_ID}:close" '{}')"
assert_ok "${close_response}"
assert_data "close rectification" "${close_response}" "data['status'] == 'CLOSED'"
pass "整改项可关闭"

reopen_response="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/rectifications/${RECTIFICATION_ID}:reopen" '{}')"
assert_ok "${reopen_response}"
assert_data "reopen rectification" "${reopen_response}" "data['status'] == 'REOPENED'"
pass "整改项可重新打开"

reapprove_response="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings/${REJECT_BINDING_ID}:approve" '{}')"
assert_ok "${reapprove_response}"
assert_data "approve rejected binding" "${reapprove_response}" "data['reviewStatus'] == 'APPROVED'"
pass "已驳回资料可复审通过"

echo ""
echo "--- 7. Drawing delivery smoke and package summary ---"
drawing_batch="$(api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings:batch" \
  "{\"viewType\":\"DRAWING\",\"sectionNodeId\":${SECTION_ID_ONE},\"deliverableTypeId\":${DWG_TYPE_ID},\"fileResourceIds\":[${DWG_FILE_ID}],\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"DRAFT\",\"remark\":\"M1D drawing flow\"}")"
assert_ok "${drawing_batch}"
DRAWING_BINDING_ID="$(first_binding_id "${drawing_batch}")"
api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings/${DRAWING_BINDING_ID}:submit-review" '{}' >/dev/null
api_post "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-bindings/${DRAWING_BINDING_ID}:approve" '{}' >/dev/null

drawing_completeness="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-completeness?viewType=DRAWING&targetType=SECTION")"
assert_ok "${drawing_completeness}"
assert_data "drawing completeness" "${drawing_completeness}" "data['standardReady'] is True and data['approvedCount'] > 0 and data['reviewReadyCount'] > 0"
assert_no_forbidden_fields "drawing completeness" "${drawing_completeness}"
pass "图纸交付也返回审核状态和可导出基础数量"

package_summary="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-package/summary?targetType=SECTION")"
assert_ok "${package_summary}"
assert_data "package summary" "${package_summary}" "data['documentSummary']['reviewReadyCount'] > 0 and data['drawingSummary']['reviewReadyCount'] > 0"
assert_no_forbidden_fields "package summary" "${package_summary}"
pass "交付包准备视图汇总文档/图纸缺失、待审、驳回、可交付状态"

precheck_drawing="$(api_get "/api/work-center/projects/${SMOKE_PROJECT_ID}/delivery-package/export-precheck?viewType=DRAWING&targetType=SECTION")"
assert_ok "${precheck_drawing}"
assert_data "drawing precheck" "${precheck_drawing}" "data['dryRun'] is True and data['packageGenerated'] is False and data['readyCount'] > 0"
assert_no_forbidden_fields "drawing export precheck" "${precheck_drawing}"
pass "图纸导出预检查为只读 dry-run"

echo ""
echo "--- 8. Real project endpoint smoke (503 / 506) ---"
for real_project_id in 503 506; do
  switch_project "${real_project_id}"
  for view_type in DOCUMENT DRAWING; do
    real_comp="$(api_get "/api/work-center/projects/${real_project_id}/delivery-completeness?viewType=${view_type}&targetType=SECTION")"
    assert_ok "${real_comp}"
    assert_no_forbidden_fields "real ${real_project_id} ${view_type} completeness" "${real_comp}"
    real_precheck="$(api_get "/api/work-center/projects/${real_project_id}/delivery-package/export-precheck?viewType=${view_type}&targetType=SECTION")"
    assert_ok "${real_precheck}"
    assert_no_forbidden_fields "real ${real_project_id} ${view_type} precheck" "${real_precheck}"
  done
  pass "真实项目 ${real_project_id} 文档/图纸完整率与预检查接口安全返回"
done

echo ""
echo "--- 9. Frontend information architecture static check ---"
if grep -q "项目资产" frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue \
  && grep -q "工程主数据" frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue \
  && grep -q "交付工作中心" frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue; then
  pass "项目工作台展示 项目资产 -> 工程主数据 -> 交付工作中心"
else
  fail "项目工作台缺少三段信息结构文案"
fi

if grep -q "项目资产" frontend/src/modules/core/components/ProjectWorkspaceNav.vue \
  && grep -q "交付工作中心" frontend/src/modules/core/components/ProjectWorkspaceNav.vue \
  && grep -q "先确认主数据" frontend/src/modules/core/components/ProjectWorkspaceNav.vue; then
  pass "项目内导航展示三段顺序与工作中心准入状态"
else
  fail "项目内导航缺少三段顺序或准入状态"
fi

if grep -q "生成 / 确认工程主数据草案" frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue \
  && grep -q "生成 / 确认工程主数据草案" frontend/src/modules/work-center/components/DeliveryViewPanel.vue; then
  pass "工程主数据未就绪时，工作台和交付页均提示先生成 / 确认草案"
else
  fail "缺少工程主数据未就绪准入提示"
fi

echo ""
echo "M1D check complete: PASS=${PASS} FAIL=${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
