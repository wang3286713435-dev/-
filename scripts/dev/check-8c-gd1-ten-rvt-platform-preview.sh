#!/usr/bin/env bash
# 8C-GD1：10 个 RVT 轻量化预览试点验收。
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-503}"
READY_FILE_ID="${READY_FILE_ID:-1257}"
SUBMIT_ONE_NOT_READY="${SUBMIT_ONE_NOT_READY:-true}"
REQUIRE_ALL_READY="${REQUIRE_ALL_READY:-true}"

EXPECTED_IDS=(1257 1261 1264 3730 1258 1251 1259 1262 3729 1243)
PASS=0
FAIL=0
TOKEN=""

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

echo "=== 8C-GD1: 10 RVT pilot platform preview ==="

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

SWITCH_RESP="$(api_post "/api/core/projects/${PROJECT_ID}:switch" "{}")"
assert_ok "${SWITCH_RESP}"
TOKEN="$(echo "${SWITCH_RESP}" | json_val "accessToken")"
pass "已切换到项目 ${PROJECT_ID}"

PILOT_RESP="$(api_get "/api/visualization-adapter/projects/${PROJECT_ID}/glandar/rvt-pilot-files")"
assert_ok "${PILOT_RESP}"
assert_safe_payload "pilot list" "${PILOT_RESP}"

python3 - "$PILOT_RESP" "${EXPECTED_IDS[@]}" <<'PY'
import json,sys
payload=json.loads(sys.argv[1])
expected=[int(x) for x in sys.argv[2:]]
rows=payload["data"]
ids=[int(row["fileId"]) for row in rows]
assert len(rows)==10, ids
assert ids==expected, ids
for row in rows:
    assert row["modelFormat"]=="RVT", row
    assert row.get("assetUuid"), row
    assert "modelAccessAddress" not in row, row
print("OK")
PY
pass "试点清单固定为 10 个 RVT 文件"

if [ "${REQUIRE_ALL_READY}" = "true" ]; then
  READY_COUNT="$(python3 - "$PILOT_RESP" <<'PY'
import json,sys
rows=json.loads(sys.argv[1])["data"]
print(sum(1 for row in rows if row.get("taskStatus")=="READY" and row.get("viewerAvailable") is True))
PY
)"
  if [ "${READY_COUNT}" = "10" ]; then
    pass "10 个试点 RVT 均已轻量化 READY 且 viewer 可用"
  else
    fail "10 个试点 RVT 尚未全部 READY：ready=${READY_COUNT}/10"
  fi
fi

READY_STATUS="$(python3 - "$PILOT_RESP" "$READY_FILE_ID" <<'PY'
import json,sys
rows=json.loads(sys.argv[1])["data"]
target=int(sys.argv[2])
for row in rows:
    if int(row["fileId"])==target:
        print(row.get("taskStatus") or "")
        break
PY
)"
if [ "${READY_STATUS}" = "READY" ]; then
  pass "基准 RVT ${READY_FILE_ID} 已轻量化 READY"
else
  fail "基准 RVT ${READY_FILE_ID} 未处于 READY: ${READY_STATUS}"
fi

READY_JOB_ID="$(python3 - "$PILOT_RESP" "$READY_FILE_ID" <<'PY'
import json,sys
rows=json.loads(sys.argv[1])["data"]
target=int(sys.argv[2])
for row in rows:
    if int(row["fileId"])==target:
        print(row.get("latestJobId") or "")
        break
PY
)"
if [ -z "${READY_JOB_ID}" ]; then
  CREATE_READY_RESP="$(api_post "/api/visualization-adapter/projects/${PROJECT_ID}/files/${READY_FILE_ID}/lightweight-jobs" "{}")"
  assert_ok "${CREATE_READY_RESP}"
  assert_safe_payload "ready job create/reuse" "${CREATE_READY_RESP}"
  READY_JOB_ID="$(echo "${CREATE_READY_RESP}" | json_val "jobId")"
fi

TICKET_RESP="$(api_post "/api/visualization-adapter/projects/${PROJECT_ID}/lightweight-jobs/${READY_JOB_ID}:viewer-ticket" "{}")"
assert_ok "${TICKET_RESP}"
assert_safe_payload "viewer ticket" "${TICKET_RESP}"
VIEWER_AVAILABLE="$(echo "${TICKET_RESP}" | json_val "viewerAvailable")"
ENGINE_STATIC="$(echo "${TICKET_RESP}" | json_val "engineStaticBase")"
MODEL_ACCESS="$(echo "${TICKET_RESP}" | json_val "modelAccessAddress")"
if [ "${VIEWER_AVAILABLE}" = "true" ] && [[ "${ENGINE_STATIC}" == http://192.168.1.37:*ThreeJsEngine* ]] && [[ "${MODEL_ACCESS}" == http://192.168.1.37:*root.glt* ]]; then
  pass "基准模型 viewer ticket 可打开 Glendale 预览"
else
  fail "基准模型 viewer ticket 异常: ${TICKET_RESP}"
fi

if [ "${REQUIRE_ALL_READY}" = "true" ]; then
  READY_JOB_IDS="$(python3 - "$PILOT_RESP" <<'PY'
import json,sys
rows=json.loads(sys.argv[1])["data"]
for row in rows:
    print(row.get("latestJobId") or "")
PY
)"
  ALL_TICKET_OK="true"
  for JOB_ID in ${READY_JOB_IDS}; do
    if [ -z "${JOB_ID}" ]; then
      ALL_TICKET_OK="false"
      continue
    fi
    ROW_TICKET_RESP="$(api_post "/api/visualization-adapter/projects/${PROJECT_ID}/lightweight-jobs/${JOB_ID}:viewer-ticket" "{}")"
    if ! python3 -c 'import json,sys; p=json.load(sys.stdin); assert p["code"]=="OK" and p["data"].get("viewerAvailable") is True' <<< "${ROW_TICKET_RESP}" >/dev/null 2>&1; then
      ALL_TICKET_OK="false"
    fi
    for pattern in "/Volumes" "smb://" "nas://" "storage_path" "storageUri" "storage_uri" "bucket" "object_key" "Token" "secret" "raw row" "SQL"; do
      if grep -qi "${pattern}" <<< "${ROW_TICKET_RESP}" 2>/dev/null; then
        ALL_TICKET_OK="false"
      fi
    done
  done
  if [ "${ALL_TICKET_OK}" = "true" ]; then
    pass "10 个试点 RVT 均可签发安全 viewer ticket"
  else
    fail "至少一个试点 RVT viewer ticket 异常或包含禁出字段"
  fi
fi

if [ "${SUBMIT_ONE_NOT_READY}" = "true" ]; then
  NEXT_FILE_ID="$(python3 - "$PILOT_RESP" <<'PY'
import json,sys
rows=json.loads(sys.argv[1])["data"]
for row in rows:
    if row.get("taskStatus") not in ("READY","RUNNING","UPLOADED","SUBMITTED"):
        print(row["fileId"])
        break
PY
)"
  if [ -n "${NEXT_FILE_ID}" ]; then
    CREATE_RESP="$(api_post "/api/visualization-adapter/projects/${PROJECT_ID}/files/${NEXT_FILE_ID}/lightweight-jobs" "{}")"
    assert_ok "${CREATE_RESP}"
    assert_safe_payload "not ready submit" "${CREATE_RESP}"
    JOB_ID="$(echo "${CREATE_RESP}" | json_val "jobId")"
    ENGINE_MODE="$(echo "${CREATE_RESP}" | json_val "engineMode")"
    TASK_CREATED="$(echo "${CREATE_RESP}" | json_val "taskCreated")"
    if [ -n "${JOB_ID}" ] && [ "${ENGINE_MODE}" = "GLANDAR" ] && [ "${TASK_CREATED}" = "true" ]; then
      pass "未转换试点 RVT 可提交轻量化任务: file=${NEXT_FILE_ID}, job=${JOB_ID}"
    else
      fail "未转换试点 RVT 提交异常: ${CREATE_RESP}"
    fi
  else
    pass "10 个试点 RVT 均已有可复用轻量化任务"
  fi
fi

OPENAPI_RESP="$(api_get "/v3/api-docs")"
if grep -q "/api/visualization-adapter/projects/{projectId}/glandar/rvt-pilot-files" <<< "${OPENAPI_RESP}"; then
  pass "OpenAPI 包含 RVT 试点清单接口"
else
  fail "OpenAPI 缺少 RVT 试点清单接口"
fi

echo ""
echo "=== Summary ==="
echo "PASS=${PASS} FAIL=${FAIL}"
if [ "${FAIL}" -ne 0 ]; then
  exit 1
fi
echo "ALL PASS"
