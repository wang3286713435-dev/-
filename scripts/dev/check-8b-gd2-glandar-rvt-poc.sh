#!/usr/bin/env bash
# 8B-GD2：葛兰岱尔 RVT PoC 真实提交、查询与 Viewer 入口验收。
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
MODEL_FILE_ID="${MODEL_FILE_ID:-1257}"
POLL_SECONDS="${POLL_SECONDS:-600}"
POLL_INTERVAL="${POLL_INTERVAL:-10}"

PASS=0
FAIL=0
TOKEN=""
JOB_ID=""

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 60 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1" body="${2:-{}}"
  curl -sS --connect-timeout 3 --max-time 300 -X POST "${BASE_URL}${url}" \
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
  for pattern in "/Volumes" "smb://" "nas://" "storage_path" "storageUri" "storage_uri" "bucket" "object_key" "Token" "secret" "raw row" "SQL"; do
    if grep -qi "${pattern}" <<< "${response}" 2>/dev/null; then
      forbidden="${forbidden} ${pattern}"
    fi
  done
  if [ -z "${forbidden}" ]; then
    pass "${name} 未泄露底层路径、对象定位、凭据或 SQL"
  else
    fail "${name} 响应包含禁出字段:${forbidden}"
  fi
}

echo "=== 8B-GD2: Glandar RVT PoC ==="
echo "Target project=${PROJECT_ID}, file=${MODEL_FILE_ID}"

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
echo "--- 2. Switch project ---"
SWITCH_RESP="$(api_post "/api/core/projects/${PROJECT_ID}:switch" "{}")"
assert_ok "${SWITCH_RESP}"
TOKEN="$(echo "${SWITCH_RESP}" | json_val "accessToken")"
if [ -n "${TOKEN}" ] && [ "${TOKEN}" != "null" ]; then
  pass "已切换到项目 ${PROJECT_ID}"
else
  fail "项目切换未返回新令牌"
  exit 1
fi

echo ""
echo "--- 3. Submit RVT conversion job ---"
CREATE_RESP="$(api_post "/api/visualization-adapter/projects/${PROJECT_ID}/files/${MODEL_FILE_ID}/lightweight-jobs?force=true" "{}")"
assert_ok "${CREATE_RESP}"
assert_safe_payload "lightweight create" "${CREATE_RESP}"
JOB_ID="$(echo "${CREATE_RESP}" | json_val "jobId")"
TASK_CREATED="$(echo "${CREATE_RESP}" | json_val "taskCreated")"
ENGINE_MODE="$(echo "${CREATE_RESP}" | json_val "engineMode")"
STATUS="$(echo "${CREATE_RESP}" | json_val "taskStatus")"
if [ -n "${JOB_ID}" ] && [ "${ENGINE_MODE}" = "GLANDAR" ] && [ "${TASK_CREATED}" = "true" ]; then
  pass "已创建或复用葛兰岱尔任务: ${JOB_ID} (${STATUS})"
else
  fail "未创建葛兰岱尔任务: ${CREATE_RESP}"
  exit 1
fi

echo ""
echo "--- 4. Poll job status ---"
deadline=$((SECONDS + POLL_SECONDS))
JOB_RESP=""
while [ "${SECONDS}" -le "${deadline}" ]; do
  JOB_RESP="$(api_get "/api/visualization-adapter/projects/${PROJECT_ID}/lightweight-jobs/${JOB_ID}")"
  assert_ok "${JOB_RESP}"
  assert_safe_payload "lightweight job poll" "${JOB_RESP}"
  STATUS="$(echo "${JOB_RESP}" | json_val "taskStatus")"
  PROGRESS="$(echo "${JOB_RESP}" | json_val "progressPercent")"
  echo "  status=${STATUS} progress=${PROGRESS}%"
  if [ "${STATUS}" = "READY" ]; then
    pass "葛兰岱尔任务转换完成"
    break
  fi
  if [ "${STATUS}" = "FAILED" ]; then
    fail "葛兰岱尔任务失败: $(echo "${JOB_RESP}" | json_val "lastErrorMessage")"
    exit 1
  fi
  sleep "${POLL_INTERVAL}"
done

if [ "${STATUS}" != "READY" ]; then
  fail "等待 ${POLL_SECONDS}s 后仍未转换完成"
  exit 1
fi

MODEL_ACCESS="$(echo "${JOB_RESP}" | json_val "modelAccessAddress")"
if [[ "${MODEL_ACCESS}" == http://192.168.1.37:* ]]; then
  pass "Viewer 模型地址为局域网 Station 地址"
else
  fail "Viewer 模型地址异常: ${MODEL_ACCESS}"
fi

echo ""
echo "--- 5. Viewer ticket ---"
TICKET_RESP="$(api_post "/api/visualization-adapter/projects/${PROJECT_ID}/lightweight-jobs/${JOB_ID}:viewer-ticket" "{}")"
assert_ok "${TICKET_RESP}"
assert_safe_payload "viewer ticket" "${TICKET_RESP}"
VIEWER_AVAILABLE="$(echo "${TICKET_RESP}" | json_val "viewerAvailable")"
TICKET_ISSUED="$(echo "${TICKET_RESP}" | json_val "ticketIssued")"
LAUNCH_URL="$(echo "${TICKET_RESP}" | json_val "launchUrl")"
ENGINE_STATIC="$(echo "${TICKET_RESP}" | json_val "engineStaticBase")"
if [ "${VIEWER_AVAILABLE}" = "true" ] && [ "${TICKET_ISSUED}" = "true" ] && [[ "${LAUNCH_URL}" == http://192.168.1.37:* ]] && [[ "${ENGINE_STATIC}" == http://192.168.1.37:* ]]; then
  pass "Viewer 入口已受控签发"
else
  fail "Viewer 入口未就绪: ${TICKET_RESP}"
fi

echo ""
echo "=== Summary ==="
echo "PASS=${PASS} FAIL=${FAIL}"
if [ "${FAIL}" -ne 0 ]; then
  exit 1
fi
echo "ALL PASS"
