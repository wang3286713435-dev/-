#!/usr/bin/env bash
# check-phase2-batch8a-bim-lightweight-adapter.sh
# 二期批次 8A：BIM 轻量化适配层与 Mock 预览入口 — 验收脚本
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
RUN_CODE="PHASE2-8A-CHECK-${RUN_ID}"

PASS=0
FAIL=0
TOKEN=""
PID=""
MODEL_FILE_ID=""
MODEL_INTEGRATION_ID=""

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

cleanup() {
  mysql_exec "UPDATE data_model_integrations SET deleted=1, delete_token=id WHERE name LIKE 'PHASE2-8A-CHECK-%';" >/dev/null 2>&1 || true
  mysql_exec "UPDATE data_file_resources SET deleted=1, delete_token=id WHERE original_name LIKE 'PHASE2-8A-CHECK-%';" >/dev/null 2>&1 || true
}
trap cleanup EXIT

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 15 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1" body="$2"
  curl -sS --connect-timeout 3 --max-time 15 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

json_val() {
  python3 -c "
import sys,json
try:
    d=json.load(sys.stdin)
    data=d.get('data',d)
    keys='$1'.split('.')
    for k in keys:
        if isinstance(data,dict):
            data=data.get(k,'')
        elif isinstance(data,list):
            data=data[int(k)] if k.isdigit() else ''
        else:
            data=''
    if isinstance(data,bool):
        print('true' if data else 'false')
    else:
        print(data)
except Exception:
    print('')
" 2>/dev/null
}

assert_ok() {
  local response="$1"
  python3 -c 'import json,sys; payload=json.load(sys.stdin); assert payload["code"] == "OK", payload' <<< "${response}" >/dev/null
}

assert_safe_payload() {
  local name="$1" response="$2"
  local forbidden=""
  for pattern in "/Volumes" "smb://" "nas://" "storage_path" "storageUri" "raw row" "SQL"; do
    if grep -qi "${pattern}" <<< "${response}" 2>/dev/null; then
      forbidden="${forbidden} ${pattern}"
    fi
  done
  if [ -z "${forbidden}" ]; then
    pass "${name} 未泄露真实或底层存储路径"
  else
    fail "${name} 响应包含禁出字段:${forbidden}"
  fi
}

echo "=== Phase 2 Batch 8A: BIM 轻量化适配层与 Mock 预览入口 ==="
cleanup

echo ""
echo "--- 1. Login ---"
LOGIN_RESP="$(curl -sS --connect-timeout 3 --max-time 10 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")"
TOKEN="$(echo "${LOGIN_RESP}" | json_val "accessToken")"
if [ -n "${TOKEN}" ] && [ "${TOKEN}" != "null" ]; then
  pass "管理员登录成功"
else
  fail "管理员登录失败"
  exit 1
fi

echo ""
echo "--- 2. Current project ---"
ME_RESP="$(api_get "/api/core/users/me")"
PID="$(echo "${ME_RESP}" | json_val "currentProject.id")"
PNAME="$(echo "${ME_RESP}" | json_val "currentProject.name")"
if [ -n "${PID}" ] && [ "${PID}" != "null" ]; then
  pass "当前项目: PID=${PID} NAME=${PNAME}"
else
  fail "无法获取当前项目"
  exit 1
fi

echo ""
echo "--- 3. Prepare mock model metadata ---"
MODEL_FILE_RESP="$(api_post "/api/data-steward/projects/${PID}/file-resources" \
  "{\"originalName\":\"${RUN_CODE}.ifc\",\"fileKind\":\"MODEL\",\"mimeType\":\"application/octet-stream\",\"sizeBytes\":4096,\"storageUri\":\"minio://delivery/${RUN_CODE}.ifc\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
assert_ok "${MODEL_FILE_RESP}"
MODEL_FILE_ID="$(echo "${MODEL_FILE_RESP}" | json_val "id")"
if [ -n "${MODEL_FILE_ID}" ] && [ "${MODEL_FILE_ID}" != "null" ]; then
  pass "安全 Mock 模型文件元数据已创建: ${MODEL_FILE_ID}"
else
  fail "模型文件元数据创建失败"
fi

INTEGRATION_RESP="$(api_post "/api/data-steward/projects/${PID}/model-integrations" \
  "{\"name\":\"${RUN_CODE}-integration\",\"modelFileId\":${MODEL_FILE_ID},\"versionNo\":\"V1\",\"componentCount\":0,\"adapterPayloadJson\":\"{\\\"adapter\\\":\\\"mock-bim\\\",\\\"lightweightPreview\\\":false}\"}")"
assert_ok "${INTEGRATION_RESP}"
MODEL_INTEGRATION_ID="$(echo "${INTEGRATION_RESP}" | json_val "id")"
if [ -n "${MODEL_INTEGRATION_ID}" ] && [ "${MODEL_INTEGRATION_ID}" != "null" ]; then
  pass "模型集成元数据已创建: ${MODEL_INTEGRATION_ID}"
else
  fail "模型集成元数据创建失败"
fi

echo ""
echo "--- 4. Lightweight status ---"
STATUS_RESP="$(api_get "/api/visualization-adapter/projects/${PID}/model-integrations/${MODEL_INTEGRATION_ID}/lightweight-status")"
assert_ok "${STATUS_RESP}"
python3 -c '
import json, sys
payload = json.load(sys.stdin)["data"]
assert payload["projectId"] == int(sys.argv[1]), payload
assert payload["integrationId"] == int(sys.argv[2]), payload
assert payload["modelFileId"] == int(sys.argv[3]), payload
assert payload["engineMode"] == "MOCK", payload
assert payload["engineConnected"] is False, payload
assert payload["lightweightStatus"] in ("NOT_CONNECTED", "NOT_STARTED"), payload
assert payload["viewerAvailable"] is False, payload
assert payload["taskStatus"] == "NOT_CREATED", payload
assert payload["conversionRequired"] is True, payload
assert payload["componentIndexStatus"] == "NOT_STARTED", payload
assert payload["previewMode"] == "BIM_LIGHTWEIGHT", payload
assert payload["statusLabel"], payload
assert "Mock" in payload["actionHint"], payload
assert "CREATE_REAL_CONVERSION_TASK" in payload["forbiddenOperations"], payload
' "${PID}" "${MODEL_INTEGRATION_ID}" "${MODEL_FILE_ID}" <<< "${STATUS_RESP}" >/dev/null
pass "lightweight-status 返回 Mock 只读状态矩阵"
assert_safe_payload "lightweight-status" "${STATUS_RESP}"

echo ""
echo "--- 5. Lightweight plan ---"
PLAN_RESP="$(api_get "/api/visualization-adapter/projects/${PID}/model-integrations/${MODEL_INTEGRATION_ID}/lightweight-plan")"
assert_ok "${PLAN_RESP}"
python3 -c '
import json, sys
payload = json.load(sys.stdin)["data"]
assert payload["projectId"] == int(sys.argv[1]), payload
assert payload["integrationId"] == int(sys.argv[2]), payload
assert payload["engineMode"] == "MOCK", payload
assert payload["dryRun"] is True, payload
assert payload["taskCreated"] is False, payload
assert payload["engineBindingRequired"] is True, payload
assert payload["realConversionExecuted"] is False, payload
assert payload["nasFileTouched"] is False, payload
assert payload["viewerAvailable"] is False, payload
assert len(payload["requiredConditions"]) >= 5, payload
joined = "\n".join(payload["requiredConditions"] + payload["futureSteps"] + payload["riskWarnings"])
for keyword in ("BIM 引擎", "模型格式", "存储策略", "任务", "权限"):
    assert keyword in joined, payload
assert "READ_MODEL_BODY" in payload["forbiddenOperations"], payload
' "${PID}" "${MODEL_INTEGRATION_ID}" <<< "${PLAN_RESP}" >/dev/null
pass "lightweight-plan 返回 dryRun 准备条件且未创建任务"
assert_safe_payload "lightweight-plan" "${PLAN_RESP}"

echo ""
echo "--- 6. OpenAPI ---"
OPENAPI_RESP="$(api_get "/v3/api-docs")"
STATUS_PATH="$(echo "${OPENAPI_RESP}" | jq -r '(.paths | keys[] | select(contains("lightweight-status"))) // ""' 2>/dev/null | head -n 1)"
PLAN_PATH="$(echo "${OPENAPI_RESP}" | jq -r '(.paths | keys[] | select(contains("lightweight-plan"))) // ""' 2>/dev/null | head -n 1)"
if [ -n "${STATUS_PATH}" ] && [ -n "${PLAN_PATH}" ]; then
  pass "OpenAPI 包含 8A 两个轻量化只读接口"
else
  fail "OpenAPI 未包含 8A 接口: status=${STATUS_PATH} plan=${PLAN_PATH}"
fi

echo ""
echo "--- 7. Frontend guard wording ---"
if grep -q "轻量化状态" frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue \
  && grep -q "查看轻量化准备" frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue \
  && grep -q "打开 3D 预览入口" frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue \
  && grep -q "当前为 Mock 适配，未执行真实轻量化转换" frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue; then
  pass "前端模型集成页已提供 Mock 轻量化状态和安全入口文案"
else
  fail "前端模型集成页缺少 8A 状态列、操作或 Mock 文案"
fi

echo ""
echo "--- 8. Cleanup metadata ---"
cleanup
LEFT_MODEL_COUNT="$(mysql_exec "SELECT COUNT(1) FROM data_model_integrations WHERE deleted=0 AND name LIKE 'PHASE2-8A-CHECK-%';" 2>/dev/null | tr -d '[:space:]' || echo "UNKNOWN")"
LEFT_FILE_COUNT="$(mysql_exec "SELECT COUNT(1) FROM data_file_resources WHERE deleted=0 AND original_name LIKE 'PHASE2-8A-CHECK-%';" 2>/dev/null | tr -d '[:space:]' || echo "UNKNOWN")"
if [ "${LEFT_MODEL_COUNT}" = "0" ] && [ "${LEFT_FILE_COUNT}" = "0" ]; then
  pass "8A 测试元数据已清理"
else
  fail "8A 测试元数据清理异常: models=${LEFT_MODEL_COUNT} files=${LEFT_FILE_COUNT}"
fi

echo ""
echo "=== Phase 2 Batch 8A Result: PASS=${PASS} FAIL=${FAIL} ==="
if [ "${FAIL}" -eq 0 ]; then
  echo "ALL PASS"
else
  echo "SOME FAILED"
  exit 1
fi
