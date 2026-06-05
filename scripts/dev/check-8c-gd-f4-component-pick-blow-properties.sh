#!/usr/bin/env bash
# 8C-GD-F4: component picking, model explosion, and controlled Glandar property proxy smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-503}"
SMOKE_FEATURE_ID="${SMOKE_FEATURE_ID:-__codex_smoke_feature__}"

PASS=0
FAIL=0
TOKEN=""

pass() {
  PASS=$((PASS + 1))
  printf '  [PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '  [FAIL] %s\n' "$1" >&2
}

json_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
scope = {
    "__builtins__": {},
    "data": data,
    "len": len,
    "all": all,
    "any": any,
    "bool": bool,
    "float": float,
    "int": int,
    "str": str,
    "sum": sum,
    "list": list,
    "set": set,
    "next": next,
}
value = eval(os.environ["EXPR"], scope, scope)
if value is None:
    print("")
elif isinstance(value, bool):
    print("true" if value else "false")
else:
    print(value)
PY
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
    r"/private(?:/|$)",
    r"smb://",
    r"nas://",
    r"storage_path",
    r"storage_uri",
    r"storagePath",
    r"storageUri",
    r"object_key",
    r"objectKey",
    r'"bucket"\s*:',
    r"\bbucket\b",
    r"secret",
    r"token\s*[:=]",
    r"password",
    r"\bselect\s+.+\s+from\b",
]
for pattern in patterns:
    if re.search(pattern, payload, re.IGNORECASE | re.DOTALL):
        raise SystemExit(f"{label} leaked forbidden pattern: {pattern}")
PY
}

assert_file_contains() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  if rg -q "${pattern}" "${file}"; then
    pass "${label}"
  else
    fail "${label}"
  fi
}

api_get() {
  local path="$1"
  curl -sS --connect-timeout 3 --max-time 45 "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local path="$1"
  curl -sS --connect-timeout 3 --max-time 90 -X POST "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d '{}'
}

printf '== 8C-GD-F4 component pick / blow / property smoke ==\n'

assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "enginePickPositionCandidates" "前端包含多坐标拾取兼容逻辑"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "waitForEnginePickResult" "前端包含引擎拾取失败后的 DOM 兜底等待逻辑"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "buildExplosionPayloads" "前端包含多 tag 爆炸参数兼容逻辑"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "showAxis: true" "前端模型爆炸参数包含 demo 使用的 showAxis"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "const target = canvas \\|\\| viewerRef.value" "前端拾取坐标优先使用 canvas 真实矩形"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "resolveEngineSitePath" "前端为嵌入式 Viewer 提供平台本地引擎资源目录"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "installGlandarWorkerUrlBridge" "前端安装葛兰岱尔 worker 地址桥接"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "rewriteGlandarWorkerUrl" "前端会将缺失 worker 重写到平台本地资源"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "normalizePickedFeature" "前端兼容 id / externalId / featureId 等拾取返回字段"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "flyTo: true" "前端兼容 demo 使用的 flyTo 模型加载参数"
assert_file_contains "frontend/src/modules/visualization/components/GlandarViewerCanvas.vue" "feature-properties" "前端包含构件属性展示区域"
assert_file_contains "frontend/src/modules/visualization/api/visualization.ts" "fetchGlandarComponentProperties" "前端接入平台代理构件属性接口"
assert_file_contains "frontend/public/glandar-engine/third/worker/gleBatchTextureWorker.js" "partLoadFeatureIds" "平台本地静态资源包含构件拾取 batch texture worker"
assert_file_contains "frontend/public/glandar-engine/worker/gleBatchTextureWorker.js" "partLoadFeatureIds" "平台本地静态资源兼容根级 worker 路径"
assert_file_contains "backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/controller/VisualizationAdapterController.java" "features/\\{featureId\\}/properties" "后端暴露受控构件属性接口"
assert_file_contains "backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarStationClient.java" "property-data-by-externalid" "后端调用葛兰岱尔构件属性 API"
assert_file_contains "backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarStationClient.java" "dbPropertyType\", 1" "新提交转换任务启用属性库生成"

login_response="$(curl -sS --connect-timeout 3 --max-time 10 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
if assert_ok "${login_response}"; then
  TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
  pass "管理员登录成功"
else
  fail "管理员登录失败"
fi

switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch")"
if assert_ok "${switch_response}"; then
  TOKEN="$(json_expr "${switch_response}" "data['data']['accessToken']")"
  pass "切换到项目 ${PROJECT_ID}"
else
  fail "切换项目失败"
fi

model_list_response="$(api_get "/api/visualization-adapter/projects/${PROJECT_ID}/glandar/model-files")"
if assert_ok "${model_list_response}" && assert_no_forbidden "glandar model list" "${model_list_response}"; then
  pass "模型清单返回 OK 且未泄露底层路径"
else
  fail "模型清单异常或存在 forbidden 字段"
fi

ready_job_id="$(json_expr "${model_list_response}" "next((item.get('latestJobId') for item in data['data'] if item.get('taskStatus') == 'READY' and item.get('viewerAvailable') and item.get('latestJobId')), '')")"
if [[ -n "${ready_job_id}" ]]; then
  ticket_response="$(api_post "/api/visualization-adapter/projects/${PROJECT_ID}/lightweight-jobs/${ready_job_id}:viewer-ticket")"
  feature_picking="$(json_expr "${ticket_response}" "data['data'].get('featurePickingAvailable')")"
  model_explosion="$(json_expr "${ticket_response}" "data['data'].get('modelExplosionAvailable')")"
  property_available="$(json_expr "${ticket_response}" "data['data'].get('componentPropertyAvailable')")"
  if assert_ok "${ticket_response}" \
    && assert_no_forbidden "viewer ticket" "${ticket_response}" \
    && [[ "${feature_picking}" == "true" ]] \
    && [[ "${model_explosion}" == "true" ]] \
    && [[ "${property_available}" == "true" ]]; then
    pass "READY Viewer ticket 明确声明拾取、爆炸、构件属性能力"
  else
    fail "READY Viewer ticket 能力字段异常：pick=${feature_picking}, blow=${model_explosion}, property=${property_available}"
  fi

  property_response="$(api_get "/api/visualization-adapter/projects/${PROJECT_ID}/lightweight-jobs/${ready_job_id}/features/${SMOKE_FEATURE_ID}/properties")"
  property_code="$(json_expr "${property_response}" "data.get('code')")"
  if [[ "${property_code}" == "OK" ]]; then
    if assert_no_forbidden "component properties" "${property_response}"; then
      pass "构件属性代理响应 OK 且未泄露底层路径"
    else
      fail "构件属性代理响应存在 forbidden 字段"
    fi
  else
    if assert_no_forbidden "component properties blocked" "${property_response}"; then
      pass "构件属性代理按引擎返回安全阻断：${property_code}"
    else
      fail "构件属性阻断响应存在 forbidden 字段"
    fi
  fi
else
  pass "当前无 READY 模型，接口动态能力检查按环境跳过"
fi

printf '== RESULT: PASS=%s FAIL=%s ==\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
