#!/usr/bin/env bash
# check-8b-gd1-glandar-adapter-skeleton.sh
# 8B-GD1：葛兰岱尔轻量化引擎适配骨架验收
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin@123}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
RUN_CODE="8B-GD1-CHECK-${RUN_ID}"

PASS=0
FAIL=0
TOKEN=""
PID=""
MODEL_FILE_ID=""
MODEL_INTEGRATION_ID=""
JOB_ID=""

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

cleanup() {
  mysql_exec "UPDATE data_model_integrations SET deleted=1, delete_token=id WHERE name LIKE '8B-GD1-CHECK-%';" >/dev/null 2>&1 || true
  mysql_exec "UPDATE data_file_resources SET deleted=1, delete_token=id WHERE original_name LIKE '8B-GD1-CHECK-%';" >/dev/null 2>&1 || true
}
trap cleanup EXIT

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 15 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1" body="${2:-{}}"
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
  for pattern in "/Volumes" "smb://" "nas://" "storage_path" "storageUri" "bucket" "object_key" "Token" "secret" "raw row" "SQL"; do
    if grep -qi "${pattern}" <<< "${response}" 2>/dev/null; then
      forbidden="${forbidden} ${pattern}"
    fi
  done
  if [ -z "${forbidden}" ]; then
    pass "${name} 未泄露底层存储、厂商凭据或 SQL 字段"
  else
    fail "${name} 响应包含禁出字段:${forbidden}"
  fi
}

echo "=== 8B-GD1: Glandar lightweight adapter skeleton ==="
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
if [ -n "${PID}" ] && [ "${PID}" != "null" ]; then
  pass "获取当前项目成功: ${PID}"
else
  fail "无法获取当前项目"
  exit 1
fi

echo ""
echo "--- 3. Prepare model metadata ---"
MODEL_FILE_RESP="$(api_post "/api/data-steward/projects/${PID}/file-resources" \
  "{\"originalName\":\"${RUN_CODE}.rvt\",\"fileKind\":\"MODEL\",\"mimeType\":\"application/octet-stream\",\"sizeBytes\":4096,\"storageUri\":\"minio://delivery/${RUN_CODE}.rvt\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
assert_ok "${MODEL_FILE_RESP}"
MODEL_FILE_ID="$(echo "${MODEL_FILE_RESP}" | json_val "id")"
if [ -n "${MODEL_FILE_ID}" ] && [ "${MODEL_FILE_ID}" != "null" ]; then
  pass "模型文件元数据已创建: ${MODEL_FILE_ID}"
else
  fail "模型文件元数据创建失败"
fi

INTEGRATION_RESP="$(api_post "/api/data-steward/projects/${PID}/model-integrations" \
  "{\"name\":\"${RUN_CODE}-integration\",\"modelFileId\":${MODEL_FILE_ID},\"versionNo\":\"V1\",\"componentCount\":0,\"adapterPayloadJson\":\"{\\\"adapter\\\":\\\"glandar-skeleton\\\",\\\"lightweightPreview\\\":false}\"}")"
assert_ok "${INTEGRATION_RESP}"
MODEL_INTEGRATION_ID="$(echo "${INTEGRATION_RESP}" | json_val "id")"
if [ -n "${MODEL_INTEGRATION_ID}" ] && [ "${MODEL_INTEGRATION_ID}" != "null" ]; then
  pass "模型集成元数据已创建: ${MODEL_INTEGRATION_ID}"
else
  fail "模型集成元数据创建失败"
fi

echo ""
echo "--- 4. Existing 8A status/plan compatibility ---"
STATUS_RESP="$(api_get "/api/visualization-adapter/projects/${PID}/model-integrations/${MODEL_INTEGRATION_ID}/lightweight-status")"
assert_ok "${STATUS_RESP}"
python3 -c '
import json, sys
p = json.load(sys.stdin)["data"]
assert p["engineMode"] == "MOCK", p
assert p["viewerAvailable"] is False, p
assert p["taskStatus"] == "NOT_CREATED", p
assert p["conversionRequired"] is True, p
assert "Mock" in p["actionHint"], p
assert "CREATE_REAL_CONVERSION_TASK" in p["forbiddenOperations"], p
' <<< "${STATUS_RESP}" >/dev/null
pass "lightweight-status 默认保持 MOCK 安全口径"
assert_safe_payload "lightweight-status" "${STATUS_RESP}"

PLAN_RESP="$(api_get "/api/visualization-adapter/projects/${PID}/model-integrations/${MODEL_INTEGRATION_ID}/lightweight-plan")"
assert_ok "${PLAN_RESP}"
python3 -c '
import json, sys
p = json.load(sys.stdin)["data"]
assert p["engineMode"] == "MOCK", p
assert p["dryRun"] is True, p
assert p["taskCreated"] is False, p
assert p["realConversionExecuted"] is False, p
assert p["viewerAvailable"] is False, p
assert "READ_MODEL_BODY" in p["forbiddenOperations"], p
' <<< "${PLAN_RESP}" >/dev/null
pass "lightweight-plan 仍为 dry-run 准备检查"
assert_safe_payload "lightweight-plan" "${PLAN_RESP}"

echo ""
echo "--- 5. New job skeleton endpoints ---"
CREATE_JOB_RESP="$(api_post "/api/visualization-adapter/projects/${PID}/model-integrations/${MODEL_INTEGRATION_ID}/lightweight-jobs" "{}")"
assert_ok "${CREATE_JOB_RESP}"
JOB_ID="$(echo "${CREATE_JOB_RESP}" | json_val "jobId")"
python3 -c '
import json, sys
p = json.load(sys.stdin)["data"]
assert p["engineMode"] == "MOCK", p
assert p["taskCreated"] is False, p
assert p["realUploadExecuted"] is False, p
assert p["realConversionExecuted"] is False, p
assert p["modelBodyRead"] is False, p
assert p["nasFileTouched"] is False, p
assert p["viewerAvailable"] is False, p
assert p["jobId"], p
assert "CALL_STATION_SPLIT_UPLOAD" in p["forbiddenOperations"], p
' <<< "${CREATE_JOB_RESP}" >/dev/null
pass "POST lightweight-jobs 返回安全骨架且不创建真实任务"
assert_safe_payload "lightweight-jobs create" "${CREATE_JOB_RESP}"

JOB_RESP="$(api_get "/api/visualization-adapter/projects/${PID}/lightweight-jobs/${JOB_ID}")"
assert_ok "${JOB_RESP}"
python3 -c '
import json, sys
p = json.load(sys.stdin)["data"]
assert p["engineMode"] == "MOCK", p
assert p["taskStatus"] == "NOT_CREATED", p
assert p["progressPercent"] == 0, p
assert p["viewerAvailable"] is False, p
assert p["realUploadExecuted"] is False, p
assert p["realConversionExecuted"] is False, p
' <<< "${JOB_RESP}" >/dev/null
pass "GET lightweight-jobs/{jobId} 返回可追踪安全状态"
assert_safe_payload "lightweight-jobs detail" "${JOB_RESP}"

TICKET_RESP="$(api_post "/api/visualization-adapter/projects/${PID}/lightweight-jobs/${JOB_ID}:viewer-ticket" "{}")"
assert_ok "${TICKET_RESP}"
python3 -c '
import json, sys
p = json.load(sys.stdin)["data"]
assert p["engineMode"] == "MOCK", p
assert p["viewerAvailable"] is False, p
assert p["ticketIssued"] is False, p
assert p["viewerTicket"] is None, p
assert p["launchUrl"] is None, p
assert "OPEN_REAL_3D_VIEWER" in p["forbiddenOperations"], p
' <<< "${TICKET_RESP}" >/dev/null
pass "viewer-ticket 在骨架阶段不签发真实 Viewer 入口"
assert_safe_payload "viewer-ticket" "${TICKET_RESP}"

echo ""
echo "--- 6. OpenAPI ---"
OPENAPI_RESP="$(api_get "/v3/api-docs")"
CREATE_PATH="$(echo "${OPENAPI_RESP}" | jq -r '(.paths | keys[] | select(contains("lightweight-jobs"))) // ""' 2>/dev/null | head -n 1)"
VIEWER_PATH="$(echo "${OPENAPI_RESP}" | jq -r '(.paths | keys[] | select(contains(":viewer-ticket"))) // ""' 2>/dev/null | head -n 1)"
if [ -n "${CREATE_PATH}" ] && [ -n "${VIEWER_PATH}" ]; then
  pass "OpenAPI 包含 8B-GD1 lightweight job / viewer-ticket 接口"
else
  fail "OpenAPI 缺少 8B-GD1 接口: job=${CREATE_PATH} viewer=${VIEWER_PATH}"
fi

echo ""
echo "--- 7. Cleanup metadata ---"
cleanup
LEFT_MODEL_COUNT="$(mysql_exec "SELECT COUNT(1) FROM data_model_integrations WHERE deleted=0 AND name LIKE '8B-GD1-CHECK-%';" 2>/dev/null | tr -d '[:space:]' || echo "UNKNOWN")"
LEFT_FILE_COUNT="$(mysql_exec "SELECT COUNT(1) FROM data_file_resources WHERE deleted=0 AND original_name LIKE '8B-GD1-CHECK-%';" 2>/dev/null | tr -d '[:space:]' || echo "UNKNOWN")"
if [ "${LEFT_MODEL_COUNT}" = "0" ] && [ "${LEFT_FILE_COUNT}" = "0" ]; then
  pass "8B-GD1 测试元数据已清理"
else
  fail "8B-GD1 测试元数据清理异常: models=${LEFT_MODEL_COUNT} files=${LEFT_FILE_COUNT}"
fi

echo ""
echo "=== 8B-GD1 Result: PASS=${PASS} FAIL=${FAIL} ==="
if [ "${FAIL}" -eq 0 ]; then
  echo "ALL PASS"
else
  echo "SOME FAILED"
  exit 1
fi
