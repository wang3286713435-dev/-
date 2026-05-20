#!/usr/bin/env bash
# Phase 2 Insert G3: Hermes working-agent MVP smoke.
# The script exercises platform-controlled tools only. It creates temporary
# metadata fixtures for the confirmation path, then deletes them best-effort.

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
RUN_CODE="PHASE2-G3-${RUN_ID}"

PASS_COUNT=0
FAIL_COUNT=0
TOKEN=""
PRIMARY_PROJECT_ID=""
SECONDARY_PROJECT_ID=""
SEC_ID=""
DD_ID=""
DT_ID=""
DOC_FILE_ID=""
BINDING_IDS=""
CLEANUP_DONE=0

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

json_get() {
  local path="$1"
  python3 -c '
import json, sys
path = sys.argv[1].split(".")
try:
    payload = json.load(sys.stdin)
except Exception:
    print("")
    raise SystemExit
value = payload.get("data", payload) if isinstance(payload, dict) else payload
for part in path:
    if isinstance(value, dict):
        value = value.get(part)
    elif isinstance(value, list) and part.isdigit() and int(part) < len(value):
        value = value[int(part)]
    else:
        value = None
    if value is None:
        break
if value is None:
    print("")
elif isinstance(value, bool):
    print("true" if value else "false")
else:
    print(value)
' "$path"
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
  response="$(curl -sS --connect-timeout 3 --max-time 10 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${response}"
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${response}"
}

api_get() {
  local path="$1"
  curl -sS --connect-timeout 3 --max-time 20 "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local path="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

api_delete() {
  local path="$1"
  curl -sS --connect-timeout 3 --max-time 20 -X DELETE "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}" >/dev/null || true
}

switch_project() {
  local project_id="$1"
  local response
  response="$(api_post "/api/core/projects/${project_id}:switch" '{}')"
  assert_ok "${response}"
  TOKEN="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${response}")"
}

cleanup() {
  if [ "${CLEANUP_DONE}" -eq 1 ]; then
    return
  fi
  CLEANUP_DONE=1
  if [ -z "${TOKEN:-}" ] || [ -z "${PRIMARY_PROJECT_ID:-}" ]; then
    return
  fi
  for binding_id in ${BINDING_IDS}; do
    api_delete "/api/work-center/projects/${PRIMARY_PROJECT_ID}/delivery-bindings/${binding_id}"
  done
  [ -n "${DOC_FILE_ID:-}" ] && api_delete "/api/data-steward/projects/${PRIMARY_PROJECT_ID}/file-resources/${DOC_FILE_ID}"
  [ -n "${DT_ID:-}" ] && api_delete "/api/master-data/projects/${PRIMARY_PROJECT_ID}/deliverable-types/${DT_ID}"
  [ -n "${DD_ID:-}" ] && api_delete "/api/master-data/projects/${PRIMARY_PROJECT_ID}/deliverable-definitions/${DD_ID}"
  [ -n "${SEC_ID:-}" ] && api_delete "/api/master-data/projects/${PRIMARY_PROJECT_ID}/section-nodes/${SEC_ID}"
}

trap cleanup EXIT

platform_chat() {
  local project_id="$1"
  local question="$2"
  local payload
  payload="$(PROJECT_ID_JSON="${project_id}" QUESTION_JSON="${question}" python3 - <<'PY'
import json
import os
project_id = os.environ["PROJECT_ID_JSON"]
question = os.environ["QUESTION_JSON"]
print(json.dumps({
    "session_id": "g3-hermes-working-agent-smoke",
    "message": question,
    "project_filters": [project_id],
    "mode": "catalog_lookup",
    "pageType": "agent_governance",
    "projectId": int(project_id),
    "sourceView": "ProjectAssetView",
    "currentRoute": f"/data-steward/assets/{project_id}/work/agent-governance",
    "pageTitle": "交付治理助手",
    "question": question,
}, ensure_ascii=False))
PY
)"
  api_post "/api/data-steward/chat" "${payload}"
}

verify_project_plan() {
  local project_id="$1"
  local label="$2"
  switch_project "${project_id}"

  local assessment overview missing rec_doc rec_drawing
  assessment="$(api_get "/api/master-data/projects/${project_id}/onboarding/assessment")"
  assert_ok "${assessment}"
  assert_safe_payload "${assessment}"
  parse_json '
import json, sys
data = json.load(sys.stdin)["data"]
assert data["assetCatalogOnly"] is True, data
assert data["evidenceMode"] == "catalog_only", data
assert data["onboardingStatus"], data
for key in ("fileCount", "modelFileCount", "drawingFileCount", "documentFileCount"):
    assert key in data["assetSummary"], data
assert isinstance(data["standardStatus"], dict), data
assert isinstance(data["nextActions"], list), data
' <<< "${assessment}" >/dev/null
  pass "${label} 主数据计划输入可读"

  overview="$(api_get "/api/work-center/projects/${project_id}/agent-governance/overview")"
  assert_ok "${overview}"
  assert_safe_payload "${overview}"
  parse_json '
import json, sys
data = json.load(sys.stdin)["data"]
for key in ("standardStatus", "documentDelivery", "drawingDelivery", "exportPrecheckSummary"):
    assert key in data, data
assert isinstance(data["nextActions"], list), data
assert data["summaryText"], data
' <<< "${overview}" >/dev/null
  pass "${label} 交付治理状态可读"

  missing="$(api_get "/api/work-center/projects/${project_id}/agent-governance/missing-items?targetType=SECTION")"
  assert_ok "${missing}"
  assert_safe_payload "${missing}"
  parse_json '
import json, sys
data = json.load(sys.stdin)["data"]
assert "totalCount" in data and isinstance(data["rows"], list), data
' <<< "${missing}" >/dev/null
  pass "${label} 缺失项计划输入可读"

  rec_doc="$(api_post "/api/work-center/projects/${project_id}/agent-governance/recommend-bindings" '{"viewType":"DOCUMENT","targetType":"SECTION","limitPerMissingItem":2}')"
  assert_ok "${rec_doc}"
  assert_safe_payload "${rec_doc}"
  rec_drawing="$(api_post "/api/work-center/projects/${project_id}/agent-governance/recommend-bindings" '{"viewType":"DRAWING","targetType":"SECTION","limitPerMissingItem":2}')"
  assert_ok "${rec_drawing}"
  assert_safe_payload "${rec_drawing}"
  pass "${label} 文档/图纸推荐方案可生成"
}

make_apply_body() {
  local confirmed="$1"
  local row_json="$2"
  CONFIRMED="${confirmed}" ROW_JSON="${row_json}" python3 - <<'PY'
import json
import os
row = json.loads(os.environ["ROW_JSON"])
print(json.dumps({
    "confirmed": os.environ["CONFIRMED"].lower() == "true",
    "viewType": row["viewType"],
    "targetType": row["targetType"],
    "items": [{
        "recommendationId": row["recommendationId"],
        "missingItemKey": row["missingItemKey"],
        "targetType": row["targetType"],
        "targetId": row["targetId"],
        "deliverableTypeId": row["deliverableTypeId"],
        "fileResourceId": row["fileResourceId"],
    }],
}, ensure_ascii=False))
PY
}

echo '== G3 Hermes working-agent MVP smoke =='
TOKEN="$(login)"
pass "管理员登录成功"

echo '== frontend action center contract =='
grep -q "Hermes Action Center" frontend/src/modules/data-steward/components/DataStewardPanel.vue
grep -q "操作草案" frontend/src/modules/data-steward/components/DataStewardPanel.vue
grep -q "待人工确认" frontend/src/modules/data-steward/components/DataStewardPanel.vue
grep -q "执行结果" frontend/src/modules/data-steward/components/DataStewardPanel.vue
grep -q "fetchOnboardingAssessment" frontend/src/modules/data-steward/components/DataStewardPanel.vue
grep -q "recommendAgentGovernanceBindings" frontend/src/modules/data-steward/components/DataStewardPanel.vue
grep -q "confirmed: true" frontend/src/modules/data-steward/components/DataStewardPanel.vue
pass "Hermes 面板包含 Action Center、计划、确认和执行结果入口"

echo '== real project selection =='
real_projects="$(api_get "/api/data-steward/assets/projects?assetSource=NAS_REAL%2A")"
assert_ok "${real_projects}"
assert_safe_payload "${real_projects}"
read -r PRIMARY_PROJECT_ID SECONDARY_PROJECT_ID < <(parse_json '
import json, sys
rows = json.load(sys.stdin)["data"]
assert len(rows) >= 2, rows
primary = next((row for row in rows if str(row.get("code")) == "105"), None)
assert primary is not None, rows
secondary = next(row for row in rows if row.get("projectId") != primary.get("projectId"))
print(primary["projectId"], secondary["projectId"])
' <<< "${real_projects}")
pass "已定位 105 项目 ${PRIMARY_PROJECT_ID} 和另一个真实项目 ${SECONDARY_PROJECT_ID}"

echo '== plan inputs on 105 and another real project =='
verify_project_plan "${PRIMARY_PROJECT_ID}" "105 项目"
verify_project_plan "${SECONDARY_PROJECT_ID}" "另一个真实项目"

echo '== Missing Evidence boundary =='
switch_project "${PRIMARY_PROJECT_ID}"
content_chat="$(platform_chat "${PRIMARY_PROJECT_ID}" "这个 RVT 里面有哪些构件参数、DWG 图层和模型内容？")"
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
assert reasons & {"rvt_parse_evidence_missing", "dwg_parse_evidence_missing", "model_parse_evidence_missing", "component_evidence_missing"}, data
' <<< "${content_chat}" >/dev/null
pass "正文/DWG/RVT/BIM 构件类问题返回 Missing Evidence"

echo '== controlled apply confirmation path =='
node_types="$(api_get "/api/master-data/projects/${PRIMARY_PROJECT_ID}/node-types")"
assert_ok "${node_types}"
NT_ID="$(parse_json '
import json, sys
rows = json.load(sys.stdin)["data"]
active = [row for row in rows if row.get("status") == "ACTIVE"] or rows
assert active, rows
print(active[0]["id"])
' <<< "${node_types}")"
pass "复用节点类型 ${NT_ID}"

sec_resp="$(api_post "/api/master-data/projects/${PRIMARY_PROJECT_ID}/section-nodes" "{\"code\":\"${RUN_CODE}-SEC\",\"name\":\"${RUN_CODE} Hermes Section\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}")"
assert_ok "${sec_resp}"
SEC_ID="$(json_get "id" <<< "${sec_resp}")"
pass "临时部位节点已创建"

dd_resp="$(api_post "/api/master-data/projects/${PRIMARY_PROJECT_ID}/deliverable-definitions" "{\"nodeTypeId\":${NT_ID},\"code\":\"${RUN_CODE}-DEF\",\"name\":\"${RUN_CODE} Hermes Definition\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":9999,\"status\":\"ACTIVE\"}")"
assert_ok "${dd_resp}"
DD_ID="$(json_get "id" <<< "${dd_resp}")"
pass "临时交付定义已创建"

dt_resp="$(api_post "/api/master-data/projects/${PRIMARY_PROJECT_ID}/deliverable-types" "{\"deliverableDefinitionId\":${DD_ID},\"code\":\"${RUN_CODE}-DOC\",\"name\":\"${RUN_CODE} Hermes Document\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}")"
assert_ok "${dt_resp}"
DT_ID="$(json_get "id" <<< "${dt_resp}")"
pass "临时交付类型已创建"

file_resp="$(api_post "/api/data-steward/projects/${PRIMARY_PROJECT_ID}/file-resources" "{\"originalName\":\"${RUN_CODE} Hermes Section Hermes Document.pdf\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":1024,\"storageUri\":\"mock://phase2-g3/${RUN_ID}/document.pdf\",\"checksum\":\"${RUN_CODE}-DOC-CHECKSUM\",\"businessTag\":\"hermes document\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
assert_ok "${file_resp}"
DOC_FILE_ID="$(json_get "id" <<< "${file_resp}")"
pass "临时候选文件元数据已创建"

rec_resp="$(api_post "/api/work-center/projects/${PRIMARY_PROJECT_ID}/agent-governance/recommend-bindings" '{"viewType":"DOCUMENT","targetType":"SECTION","limitPerMissingItem":1}')"
assert_ok "${rec_resp}"
assert_safe_payload "${rec_resp}"
rec_row_json="$(TARGET_ID="${SEC_ID}" TYPE_ID="${DT_ID}" FILE_ID="${DOC_FILE_ID}" python3 -c '
import json, os, sys
rows = json.load(sys.stdin)["data"]["rows"]
for row in rows:
    if str(row.get("targetId")) == os.environ["TARGET_ID"] and str(row.get("deliverableTypeId")) == os.environ["TYPE_ID"] and str(row.get("fileResourceId")) == os.environ["FILE_ID"]:
        print(json.dumps(row, ensure_ascii=False))
        break
' <<< "${rec_resp}")"
if [ -n "${rec_row_json}" ]; then
  pass "推荐方案包含临时候选文件"
else
  fail "推荐方案未包含临时候选文件"
  exit 1
fi

unconfirmed_body="$(make_apply_body false "${rec_row_json}")"
unconfirmed_resp_file="$(mktemp)"
unconfirmed_http="$(curl -sS --connect-timeout 3 --max-time 20 -o "${unconfirmed_resp_file}" -w "%{http_code}" -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "${unconfirmed_body}" \
  "${BASE_URL}/api/work-center/projects/${PRIMARY_PROJECT_ID}/agent-governance/recommendations:apply")"
unconfirmed_resp="$(cat "${unconfirmed_resp_file}")"
rm -f "${unconfirmed_resp_file}"
assert_safe_payload "${unconfirmed_resp}"
if [ "${unconfirmed_http}" != "200" ]; then
  pass "未人工确认时拒绝执行挂接"
else
  assert_not_ok "${unconfirmed_resp}"
  pass "未人工确认时返回非 OK"
fi

confirmed_body="$(make_apply_body true "${rec_row_json}")"
apply_resp="$(api_post "/api/work-center/projects/${PRIMARY_PROJECT_ID}/agent-governance/recommendations:apply" "${confirmed_body}")"
assert_ok "${apply_resp}"
assert_safe_payload "${apply_resp}"
created_count="$(json_get "createdCount" <<< "${apply_resp}")"
BINDING_IDS="$(parse_json '
import json, sys
data = json.load(sys.stdin)["data"]
print(" ".join(str(row["bindingId"]) for row in data.get("results", []) if row.get("bindingId")))
' <<< "${apply_resp}")"
if [ "${created_count:-0}" -ge 1 ] 2>/dev/null; then
  pass "人工确认后调用平台挂接并返回执行结果"
else
  fail "人工确认后未创建挂接"
fi

audit_resp="$(api_get "/api/core/projects/${PRIMARY_PROJECT_ID}/audit-logs?limit=100")"
assert_ok "${audit_resp}"
if grep -q "work.agent-governance.apply" <<< "${audit_resp}"; then
  pass "审计日志包含 G3 复用的 apply 记录"
else
  fail "审计日志未找到 apply 记录"
fi
assert_safe_payload "${audit_resp}"

echo '== result =='
printf 'PASS=%s FAIL=%s\n' "${PASS_COUNT}" "${FAIL_COUNT}"
if [ "${FAIL_COUNT}" -ne 0 ]; then
  exit 1
fi
