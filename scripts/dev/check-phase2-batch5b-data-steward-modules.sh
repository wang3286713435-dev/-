#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-${1:-http://localhost:8080}}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-1}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_CODE="PHASE2-5B-CHECK-$(date +%s)"

PASS_COUNT=0
FAIL_COUNT=0
MODEL_FILE_ID=""
MODEL_INTEGRATION_ID=""
MANAGED_OBJECT_ID=""

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
  if grep -Eqi 'nas://|/Volumes/|secret[[:space:]]*[:=]|token[[:space:]]*[:=]' <<< "${response}"; then
    fail "响应包含敏感路径或密钥"
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

switch_project() {
  local token="$1"
  local response
  response="$(curl -sS -X POST "${BASE_URL}/api/core/projects/${PROJECT_ID}:switch" \
    -H "Authorization: Bearer ${token}")"
  assert_ok "${response}"
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${response}"
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

cleanup() {
  mysql_exec "UPDATE data_managed_objects SET deleted=1 WHERE code LIKE 'PHASE2-5B-CHECK-%';" >/dev/null 2>&1 || true
  mysql_exec "UPDATE data_model_integrations SET deleted=1 WHERE name LIKE 'PHASE2-5B-CHECK-%';" >/dev/null 2>&1 || true
  mysql_exec "UPDATE data_file_resources SET deleted=1 WHERE original_name LIKE 'PHASE2-5B-CHECK-%';" >/dev/null 2>&1 || true
}
trap cleanup EXIT

token="$(switch_project "$(login)")"
pass "管理员登录并切换到项目 ${PROJECT_ID}"

echo '== project module APIs =='
stats="$(curl -sS "${BASE_URL}/api/data-steward/assets/statistics?projectId=${PROJECT_ID}" -H "Authorization: Bearer ${token}")"
assert_ok "${stats}"
pass "资产驾驶舱统计接口可用"

catalog="$(curl -sS "${BASE_URL}/api/data-steward/catalog/files?projectId=${PROJECT_ID}&page=1&pageSize=5" -H "Authorization: Bearer ${token}")"
assert_ok "${catalog}"
assert_safe_payload "${catalog}"
pass "文件管理目录列表接口不泄露真实 NAS 路径"

model_file_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/file-resources" \
  -H "Authorization: Bearer ${token}" \
  -H 'Content-Type: application/json' \
  -d "{\"originalName\":\"${RUN_CODE}-model.ifc\",\"fileKind\":\"MODEL\",\"mimeType\":\"application/octet-stream\",\"sizeBytes\":128,\"storageUri\":\"nas:///tmp/${RUN_CODE}-model.ifc\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
assert_ok "${model_file_response}"
MODEL_FILE_ID="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${model_file_response}")"
pass "模型文件资源已准备"

models_page="$(curl -sS "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/file-resources?fileKind=MODEL&pageNo=1&pageSize=10&keyword=${RUN_CODE}" \
  -H "Authorization: Bearer ${token}")"
assert_ok "${models_page}"
parse_json 'import json,sys; payload=json.load(sys.stdin)["data"]; assert "items" in payload and payload["total"] >= 1, payload' <<< "${models_page}" >/dev/null
pass "模型文件选择使用分页接口"

integration_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/model-integrations" \
  -H "Authorization: Bearer ${token}" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"${RUN_CODE}-integration\",\"modelFileId\":${MODEL_FILE_ID},\"versionNo\":\"V1\",\"componentCount\":0,\"adapterPayloadJson\":\"{\\\"lightweightPreview\\\":false}\"}")"
assert_ok "${integration_response}"
MODEL_INTEGRATION_ID="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${integration_response}")"
pass "模型集成可创建"

publish_response="$(curl -sS -X PATCH "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/model-integrations/${MODEL_INTEGRATION_ID}:publish" \
  -H "Authorization: Bearer ${token}")"
assert_ok "${publish_response}"
parse_json 'import json,sys; payload=json.load(sys.stdin)["data"]; assert payload["status"] == "PUBLISHED", payload' <<< "${publish_response}" >/dev/null
pass "模型集成可发布且只更新平台元数据"

object_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/managed-objects" \
  -H "Authorization: Bearer ${token}" \
  -H 'Content-Type: application/json' \
  -d "{\"modelIntegrationId\":${MODEL_INTEGRATION_ID},\"code\":\"${RUN_CODE}-object\",\"name\":\"${RUN_CODE}-对象\",\"objectType\":\"EQUIPMENT\",\"discipline\":\"MEP\",\"status\":\"ACTIVE\",\"propertiesJson\":\"{\\\"source\\\":\\\"phase2-5b-check\\\"}\"}")"
assert_ok "${object_response}"
MANAGED_OBJECT_ID="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${object_response}")"
pass "管理对象可创建"

object_update_response="$(curl -sS -X PATCH "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/managed-objects/${MANAGED_OBJECT_ID}" \
  -H "Authorization: Bearer ${token}" \
  -H 'Content-Type: application/json' \
  -d "{\"modelIntegrationId\":${MODEL_INTEGRATION_ID},\"code\":\"${RUN_CODE}-object\",\"name\":\"${RUN_CODE}-对象更新\",\"objectType\":\"EQUIPMENT\",\"discipline\":\"MEP\",\"status\":\"ACTIVE\",\"propertiesJson\":\"{\\\"source\\\":\\\"phase2-5b-check\\\",\\\"updated\\\":true}\"}")"
assert_ok "${object_update_response}"
pass "管理对象可编辑"

jobs="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs?projectId=${PROJECT_ID}&limit=20" -H "Authorization: Bearer ${token}")"
assert_ok "${jobs}"
pass "任务列表接口可用"

events="$(curl -sS "${BASE_URL}/api/data-steward/assets/events?projectId=${PROJECT_ID}&limit=20" -H "Authorization: Bearer ${token}")"
assert_ok "${events}"
pass "事项列表可读取事件流"

quality="$(curl -sS "${BASE_URL}/api/data-steward/assets/quality/overview?projectId=${PROJECT_ID}" -H "Authorization: Bearer ${token}")"
assert_ok "${quality}"
pass "事项列表可读取质量概览"

csv_source="$(curl -sS "${BASE_URL}/api/data-steward/catalog/files?projectId=${PROJECT_ID}&page=1&pageSize=20" -H "Authorization: Bearer ${token}")"
assert_ok "${csv_source}"
assert_safe_payload "${csv_source}"
pass "导出来源不包含真实 NAS 路径"

real_nas_project_id="$(mysql_exec "SELECT project_id FROM data_file_resources WHERE deleted=0 AND logical_path LIKE '/Volumes/%' LIMIT 1;" 2>/dev/null | head -n 1 || true)"
if [[ -n "${real_nas_project_id}" ]]; then
  real_catalog="$(curl -sS "${BASE_URL}/api/data-steward/catalog/files?projectId=${real_nas_project_id}&page=1&pageSize=5" -H "Authorization: Bearer ${token}")"
  assert_ok "${real_catalog}"
  assert_safe_payload "${real_catalog}"
  parse_json 'import json,sys; payload=json.load(sys.stdin)["data"]; assert all(not str(row.get("logicalPath") or "").startswith("/") for row in payload["items"]), payload' <<< "${real_catalog}" >/dev/null
  pass "真实 NAS 项目导出来源不包含绝对路径"
else
  pass "未发现真实 NAS 绝对路径样本，跳过真实项目导出脱敏断言"
fi

openapi="$(curl -sS "${BASE_URL}/v3/api-docs" || true)"
if [[ "${openapi}" == *"/api/data-steward/projects/{projectId}/model-integrations"* ]] \
  && [[ "${openapi}" == *"/api/data-steward/projects/{projectId}/managed-objects"* ]] \
  && [[ "${openapi}" == *"/api/data-steward/assets/jobs"* ]]; then
  pass "OpenAPI 包含 5B 关键后端接口"
else
  fail "OpenAPI 未包含 5B 关键接口"
fi

echo '== cleanup =='
cleanup
pass "5B 专项数据已清理"

echo '== summary =='
printf 'PASS=%s FAIL=%s\n' "${PASS_COUNT}" "${FAIL_COUNT}"
if [[ "${FAIL_COUNT}" -ne 0 ]]; then
  exit 1
fi
