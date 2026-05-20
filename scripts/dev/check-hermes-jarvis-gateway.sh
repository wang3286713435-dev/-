#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-${1:-http://localhost:8080}}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-1}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
EXPECT_HERMES_AGENT_AVAILABLE="${EXPECT_HERMES_AGENT_AVAILABLE:-false}"
export EXPECT_HERMES_AGENT_AVAILABLE

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

assert_safe_payload() {
  local response="$1"
  if grep -Eqi 'nas://|smb://|/Volumes|storage_path|storage_uri|storagePath|storageUri|bearer[[:space:]]+|token[[:space:]]*[:=]|secret[[:space:]]*[:=]|raw row|select[[:space:]].*from|insert[[:space:]]+into|update[[:space:]].*set|delete[[:space:]]+from' <<< "${response}"; then
    fail "响应包含敏感路径或密钥字段"
    printf '%s\n' "${response}" >&2
    exit 1
  fi
}

login() {
  local username="$1"
  local password="$2"
  local response
  response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${username}\",\"password\":\"${password}\"}")"
  assert_ok "${response}"
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${response}"
}

switch_project() {
  local token="$1"
  local project_id="$2"
  local response
  response="$(curl -sS -X POST "${BASE_URL}/api/core/projects/${project_id}:switch" \
    -H "Authorization: Bearer ${token}")"
  assert_ok "${response}"
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${response}"
}

chat() {
  local token="$1"
  local project_id="$2"
  local asset_id="$3"
  local question="$4"
  local source_view="${5:-ProjectAssetView}"
  local payload
  payload="$(PROJECT_ID_JSON="${project_id}" ASSET_ID_JSON="${asset_id}" QUESTION_JSON="${question}" SOURCE_VIEW_JSON="${source_view}" python3 - <<'PY'
import json
import os

asset_id = os.environ["ASSET_ID_JSON"]
payload = {
    "pageType": "hermes_gateway_smoke",
    "projectId": int(os.environ["PROJECT_ID_JSON"]),
    "sourceView": os.environ["SOURCE_VIEW_JSON"],
    "question": os.environ["QUESTION_JSON"],
}
if asset_id:
    payload["assetId"] = int(asset_id)
print(json.dumps(payload, ensure_ascii=False))
PY
)"
  curl -sS -X POST "${BASE_URL}/api/agent/hermes/chat" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${payload}"
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
    "session_id": "hermes-platform-chat-alias-smoke",
    "message": os.environ["QUESTION_JSON"],
    "project_filters": [os.environ["PROJECT_ID_JSON"]],
    "mode": "catalog_lookup",
}, ensure_ascii=False))
PY
)"
  curl -sS -X POST "${BASE_URL}/api/data-steward/chat" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${payload}"
}

catalog_search() {
  local token="$1"
  local project_id="$2"
  local query="$3"
  local payload
  payload="$(PROJECT_ID_JSON="${project_id}" QUERY_JSON="${query}" python3 - <<'PY'
import json
import os

print(json.dumps({
    "query": os.environ["QUERY_JSON"],
    "project_filters": [os.environ["PROJECT_ID_JSON"]],
    "filters": {
        "asset_kind": ["FILE"],
        "index_eligibility": ["catalog_only"]
    },
    "page": {
        "limit": 5,
        "cursor": None
    }
}, ensure_ascii=False))
PY
)"
  curl -sS -X POST "${BASE_URL}/api/data-steward/catalog/search" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${payload}"
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

echo '== login and capabilities =='
admin_token="$(login "${ADMIN_USER}" "${ADMIN_PASSWORD}")"
admin_token="$(switch_project "${admin_token}" "${PROJECT_ID}")"
pass "管理员登录并切换到项目 ${PROJECT_ID}"

capabilities="$(curl -sS "${BASE_URL}/api/data-steward/hermes/capabilities" \
  -H "Authorization: Bearer ${admin_token}")"
assert_ok "${capabilities}"
assert_safe_payload "${capabilities}"
parse_json '
import json, sys
payload = json.load(sys.stdin)["data"]
assert payload["agentName"] == "Hermes", payload
assert payload["mode"] == "catalog_only", payload
supports = payload["supports"]
assert supports["catalogQuery"] is True, payload
assert supports["missingEvidence"] is True, payload
assert supports["operationPlanDraft"] is True, payload
assert supports["documentContentAnswer"] is False, payload
assert supports["dbCrud"] is False, payload
assert supports["nasCrud"] is False, payload
assert supports["productionRollout"] is False, payload
' <<< "${capabilities}" >/dev/null
pass "capabilities 返回 Hermes catalog-only 只读能力"

echo '== platform gateway health =='
health="$(curl -sS "${BASE_URL}/api/data-steward/hermes/health" \
  -H "Authorization: Bearer ${admin_token}")"
assert_ok "${health}"
assert_safe_payload "${health}"
parse_json '
import json, os, sys
payload = json.load(sys.stdin)["data"]
assert payload["status"] in ("ok", "degraded"), payload
assert payload["mode"] == "read_only_gateway", payload
assert payload["contractVersion"] == "delivery_platform.asset_views.v1.1", payload
assert payload["runtimeWriteEnabled"] is False, payload
assert payload["agentAnswerIntegrationEnabled"] in (False, True), payload
if os.getenv("EXPECT_HERMES_AGENT_AVAILABLE", "false").lower() == "true":
    assert payload["status"] == "ok", payload
    assert payload["hermesAvailable"] is True, payload
    assert payload["gatewayEnabled"] is True, payload
    assert payload["agentAnswerIntegrationEnabled"] is True, payload
assert "baseUrl" not in payload, payload
' <<< "${health}" >/dev/null
pass "health 返回只读网关状态且不暴露内部配置"

echo '== catalog-only chat =='
catalog_response="$(chat "${admin_token}" "${PROJECT_ID}" "" "这个项目有哪些已登记资产？")"
assert_ok "${catalog_response}"
assert_safe_payload "${catalog_response}"
parse_json '
import json, os, sys
payload = json.load(sys.stdin)["data"]
assert payload["status"] in ("catalog_only", "missing_evidence"), payload
assert payload["assetCatalogOnly"] is True, payload
assert payload["queryId"], payload
assert payload["traceId"], payload
assert payload["sourceView"] == "ProjectAssetView", payload
assert isinstance(payload["pathHints"], list), payload
if os.getenv("EXPECT_HERMES_AGENT_AVAILABLE", "false").lower() == "true":
    assert payload["trace"]["agentMode"] == "openai_compatible_catalog_only", payload
    assert payload["answer"] != "Hermes 当前回答仅基于资产目录和权限上下文，不包含文件正文证据。", payload
else:
    assert payload["trace"]["agentMode"] in ("catalog_only", "openai_compatible_catalog_only"), payload
assert payload["trace"]["productionRollout"] is False, payload
assert payload["permission"]["permissionStatus"] == "allowed", payload
assert payload["operationPlan"]["requiresHumanApproval"] is True, payload
for action in payload["operationPlan"]["actions"]:
    assert action["status"] == "draft_only", payload
    assert "ingestion" not in action["actionType"].lower(), payload
    assert "writer" not in action["actionType"].lower(), payload
    assert "index" not in action["actionType"].lower(), payload
' <<< "${catalog_response}" >/dev/null
pass "有权项目返回 catalog-only 或 missing-evidence 且操作计划仅为草案"

platform_chat_response="$(platform_chat "${admin_token}" "${PROJECT_ID}" "用平台数据管家入口查询项目资产。")"
assert_ok "${platform_chat_response}"
assert_safe_payload "${platform_chat_response}"
parse_json '
import json, os, sys
payload = json.load(sys.stdin)["data"]
assert payload["status"] in ("catalog_only", "missing_evidence"), payload
assert payload["assetCatalogOnly"] is True, payload
assert payload["queryId"], payload
assert payload["traceId"], payload
assert isinstance(payload["pathHints"], list), payload
if os.getenv("EXPECT_HERMES_AGENT_AVAILABLE", "false").lower() == "true":
    assert payload["trace"]["agentMode"] == "openai_compatible_catalog_only", payload
    assert payload["answer"] != "Hermes 当前回答仅基于资产目录和权限上下文，不包含文件正文证据。", payload
else:
    assert payload["trace"]["agentMode"] in ("catalog_only", "openai_compatible_catalog_only"), payload
' <<< "${platform_chat_response}" >/dev/null
pass "平台语义 /api/data-steward/chat 别名可用"

project_path="$(mysql_exec "SELECT nas_path FROM data_asset_project_path_mappings WHERE project_id=${PROJECT_ID} AND enabled=1 AND deleted=0 ORDER BY sort_order, id LIMIT 1;" 2>/dev/null | head -n 1 || true)"
if [[ -n "${project_path}" ]]; then
  path_chat_response="$(platform_chat "${admin_token}" "${PROJECT_ID}" "这个项目路径在哪里？")"
  assert_ok "${path_chat_response}"
  assert_safe_payload "${path_chat_response}"
  PROJECT_PATH="${project_path}" parse_json '
import json, os, sys
payload = json.load(sys.stdin)["data"]
assert payload["status"] in ("catalog_only", "missing_evidence"), payload
assert payload["assetCatalogOnly"] is True, payload
assert payload["queryId"], payload
assert payload["traceId"], payload
assert payload["trace"]["agentMode"] in ("catalog_only", "openai_compatible_catalog_only"), payload
assert isinstance(payload["pathHints"], list) and len(payload["pathHints"]) >= 1, payload
for hint in payload["pathHints"]:
    assert hint["displayPath"].startswith("项目目录："), hint
    assert "底层路径已隐藏" in hint["pathHint"], hint
    assert hint["provider"], hint
    assert hint["matchStrategy"], hint
assert os.environ["PROJECT_PATH"] not in json.dumps(payload, ensure_ascii=False), payload
assert "项目目录：" in payload["answer"] or "path_not_exposable" in payload["answer"] or "路径映射" in payload["answer"], payload
assert payload["permission"]["permissionStatus"] == "allowed", payload
for forbidden in ("storage_path", "storage_uri", "storagePath", "storageUri", "raw row", "token", "secret", "/Volumes", "nas://", "smb://"):
    assert forbidden.lower() not in json.dumps(payload, ensure_ascii=False).lower(), payload
' <<< "${path_chat_response}" >/dev/null
  pass "平台语义 /api/data-steward/chat 支持脱敏项目路径自然语言查询"
else
  pass "当前项目无已启用路径映射，跳过项目路径自然语言查询断言"
fi

echo '== catalog metadata preview search =='
search_response="$(catalog_search "${admin_token}" "${PROJECT_ID}" "模型 文件")"
assert_ok "${search_response}"
assert_safe_payload "${search_response}"
parse_json '
import json, sys
payload = json.load(sys.stdin)["data"]
assert payload["assetCatalogOnly"] is True, payload
assert payload["permissionDecision"] == "allowed", payload
assert payload["evidenceMode"] == "catalog_only", payload
assert payload["queryId"], payload
assert payload["traceId"], payload
assert payload["safety"]["rawRowsOutput"] is False, payload
assert payload["safety"]["trueNasPathOutput"] is False, payload
assert payload["safety"]["secretPrinted"] is False, payload
assert "nextCursor" in payload, payload
for item in payload["results"]:
    assert item["assetRef"].startswith("file:"), item
    assert item["sourceView"] == "FileAssetView", item
    assert item["fileId"], item
    assert "displayPath" in item, item
    assert "pathHint" in item, item
    assert item["indexEligibility"] == "catalog_only", item
    assert item["contentEvidenceAvailable"] is False, item
    assert "asset_catalog_only" in item["missingEvidence"], item
    assert "storagePath" not in item, item
    assert "storageUri" not in item, item
    assert "storage_path" not in item, item
    assert "storage_uri" not in item, item
' <<< "${search_response}" >/dev/null
pass "V3 /api/data-steward/catalog/search 返回只读目录预览且不暴露路径"

echo '== content question must not be fabricated =='
content_response="$(chat "${admin_token}" "${PROJECT_ID}" "" "请阅读文件正文并总结里面的条款。")"
assert_ok "${content_response}"
assert_safe_payload "${content_response}"
parse_json '
import json, sys
payload = json.load(sys.stdin)["data"]
assert payload["status"] == "missing_evidence", payload
assert payload["evidenceMode"] == "missing_evidence", payload
assert payload["assetCatalogOnly"] is True, payload
assert payload["queryId"], payload
assert payload["traceId"], payload
assert isinstance(payload["pathHints"], list), payload
reasons = [item["reason"] for item in payload["missingEvidence"]]
assert "asset_catalog_only" in reasons, payload
assert "正文" in payload["answer"], payload
for action in payload["operationPlan"]["actions"]:
    assert action["status"] == "draft_only", payload
    assert "ingestion" not in action["actionType"].lower(), payload
    assert "writer" not in action["actionType"].lower(), payload
    assert "index" not in action["actionType"].lower(), payload
' <<< "${content_response}" >/dev/null
pass "正文类问题返回 Missing Evidence，不编造正文答案"

echo '== model parse question must be missing evidence =='
model_parse_response="$(chat "${admin_token}" "${PROJECT_ID}" "" "这个 RVT 模型里有哪些构件参数和 Level/Grid？")"
assert_ok "${model_parse_response}"
assert_safe_payload "${model_parse_response}"
parse_json '
import json, sys
payload = json.load(sys.stdin)["data"]
assert payload["status"] == "missing_evidence", payload
assert payload["evidenceMode"] == "missing_evidence", payload
assert payload["assetCatalogOnly"] is True, payload
assert payload["queryId"], payload
assert payload["traceId"], payload
assert isinstance(payload["pathHints"], list), payload
reasons = [item["reason"] for item in payload["missingEvidence"]]
assert "asset_catalog_only" in reasons, payload
assert any(reason in reasons for reason in ("rvt_parse_evidence_missing", "model_parse_evidence_missing", "component_evidence_missing")), payload
text = json.dumps(payload, ensure_ascii=False).lower()
for forbidden in ("storage_path", "storage_uri", "storagepath", "storageuri", "nas://", "smb://", "/volumes", "token", "secret"):
    assert forbidden not in text, payload
for action in payload["operationPlan"]["actions"]:
    assert action["status"] == "draft_only", payload
    assert "parser" not in action["actionType"].lower(), payload
    assert "writer" not in action["actionType"].lower(), payload
    assert "index" not in action["actionType"].lower(), payload
' <<< "${model_parse_response}" >/dev/null
pass "RVT/DWG/BIM/构件类问题返回 Missing Evidence，不编造模型内容"

echo '== fail closed on invalid project scope =='
denied_response="$(chat "${admin_token}" "99999999" "" "查询这个项目资产。")"
assert_ok "${denied_response}"
assert_safe_payload "${denied_response}"
parse_json '
import json, sys
payload = json.load(sys.stdin)["data"]
assert payload["status"] == "denied", payload
assert payload["permission"]["permissionStatus"] == "denied", payload
assert payload["permission"]["failClosedApplied"] is True, payload
' <<< "${denied_response}" >/dev/null
pass "无效项目范围 fail closed"

echo '== audit must exist and stay safe =='
audit_count="$(mysql_exec "SELECT COUNT(*) FROM core_audit_logs WHERE module_code='data-steward' AND action_code LIKE 'agent.hermes.chat.%';")"
if [[ "${audit_count}" -ge 3 ]]; then
  pass "已写入 Hermes 平台侧审计"
else
  fail "未查到足够的 Hermes 审计记录"
fi

sensitive_count="$(mysql_exec "SELECT COUNT(*) FROM core_audit_logs WHERE module_code='data-steward' AND action_code LIKE 'agent.hermes.chat.%' AND (LOWER(details_json) LIKE '%nas://%' OR details_json LIKE '%/Volumes/%' OR LOWER(details_json) LIKE '%token%' OR LOWER(details_json) LIKE '%secret%');")"
if [[ "${sensitive_count}" == "0" ]]; then
  pass "Hermes 审计 details 未发现路径或密钥泄露"
else
  fail "Hermes 审计 details 出现敏感信息"
fi

catalog_search_audit_count="$(mysql_exec "SELECT COUNT(*) FROM core_audit_logs WHERE module_code='data-steward' AND action_code='agent.hermes.catalog.search';")"
if [[ "${catalog_search_audit_count}" -ge 1 ]]; then
  pass "已写入目录预览搜索审计"
else
  fail "未查到目录预览搜索审计记录"
fi

echo '== summary =='
printf 'PASS=%s FAIL=%s\n' "${PASS_COUNT}" "${FAIL_COUNT}"
if [[ "${FAIL_COUNT}" -ne 0 ]]; then
  exit 1
fi
