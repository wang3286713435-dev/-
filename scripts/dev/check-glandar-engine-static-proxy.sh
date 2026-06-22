#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
FRONTEND_FILE="frontend/src/modules/visualization/components/GlandarViewerCanvas.vue"

PASS=0
FAIL=0

pass() {
  PASS=$((PASS + 1))
  printf '  [PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '  [FAIL] %s\n' "$1" >&2
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
    r"GLANDAR_TOKEN",
    r"Token\s*[:=]",
    r"Bearer\s+",
    r"secret",
    r"password",
    r"storage_uri",
    r"storage_path",
    r"/Volumes(?:/|$)",
    r"smb://",
    r"nas://",
]
for pattern in patterns:
    if re.search(pattern, payload, re.IGNORECASE):
        raise SystemExit(f"{label} leaked forbidden pattern: {pattern}")
PY
}

printf '== Glandar engine static proxy smoke ==\n'

health="$(curl -fsS --connect-timeout 3 --max-time 8 "${BASE_URL}/actuator/health")"
if [[ "${health}" == *'"UP"'* ]]; then
  pass "后端健康检查 UP"
else
  fail "后端健康检查异常：${health}"
fi

status="$(curl -sS -o /tmp/glandar-engine-proxy.js -w '%{http_code}' --connect-timeout 3 --max-time 20 \
  "${BASE_URL}/api/visualization-adapter/glandar/static/ThreeJsEngine/glendale.v1.umd.js")"
body_head="$(head -c 1200 /tmp/glandar-engine-proxy.js 2>/dev/null || true)"
if [[ "${status}" == "200" ]] && [[ -s /tmp/glandar-engine-proxy.js ]]; then
  pass "平台代理可读取 glendale.v1.umd.js"
else
  fail "平台代理读取 glendale.v1.umd.js 失败：HTTP ${status}"
fi

if assert_no_forbidden "engine proxy body" "${body_head}"; then
  pass "引擎脚本代理响应未泄露敏感配置"
else
  fail "引擎脚本代理响应存在敏感字段"
fi

if rg -q "/api/visualization-adapter/glandar/static/ThreeJsEngine" "${FRONTEND_FILE}" \
  && rg -q "platformEngineStaticBase" "${FRONTEND_FILE}"; then
  pass "前端 Viewer 已切换为平台内脚本代理"
else
  fail "前端 Viewer 未使用平台内脚本代理"
fi

if rg -q "glandar-engine/" "${FRONTEND_FILE}"; then
  pass "Viewer worker/wasm 资源继续走平台静态目录"
else
  fail "Viewer sitePath 未指向平台静态目录"
fi

printf '== RESULT: PASS=%s FAIL=%s ==\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
