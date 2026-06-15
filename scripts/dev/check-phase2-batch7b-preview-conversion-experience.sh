#!/usr/bin/env bash
# check-phase2-batch7b-preview-conversion-experience.sh
# 二期批次 7B：文件预览转换体验增强 — 验收脚本
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"

PASS=0
FAIL=0
TOKEN=""
PID=""
CREATED_FILES=()
CREATED_FILE_ID=""

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

cleanup() {
  if [ -n "${TOKEN}" ] && [ -n "${PID}" ]; then
    for file_id in "${CREATED_FILES[@]:-}"; do
      curl -sS --connect-timeout 3 --max-time 10 \
        -X DELETE "${BASE_URL}/api/data-steward/projects/${PID}/file-resources/${file_id}" \
        -H "Authorization: Bearer ${TOKEN}" >/dev/null 2>&1 || true
    done
  fi
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
        if isinstance(data,dict): data=data.get(k,'')
        elif isinstance(data,list): data=data[int(k)] if k.isdigit() else ''
        else: data=''
    if isinstance(data,bool): print('true' if data else 'false')
    else: print(data)
except Exception:
    print('')
" 2>/dev/null
}

assert_ok() {
  local response="$1"
  python3 -c 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "OK", data' <<< "${response}" >/dev/null
}

assert_safe_payload() {
  local name="$1" response="$2"
  local forbidden=""
  for pattern in "/Volumes" "smb://" "nas://" "minio://" "storage_path" "storageUri" "raw row" "SQL"; do
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

create_file() {
  local name="$1" kind="$2" mime="$3" ext="$4"
  local body response file_id
  body="{\"originalName\":\"${name}\",\"fileKind\":\"${kind}\",\"mimeType\":\"${mime}\",\"sizeBytes\":2048,\"storageUri\":\"minio://delivery/phase2-7b-${RUN_ID}${ext}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}"
  response="$(api_post "/api/data-steward/projects/${PID}/file-resources" "${body}")"
  assert_ok "${response}"
  file_id="$(echo "${response}" | json_val "id")"
  CREATED_FILES+=("${file_id}")
  CREATED_FILE_ID="${file_id}"
}

assert_preview() {
  local label="$1" file_id="$2" status="$3" mode="$4" conversion_required="$5" download_only="$6" risk="$7" hint_keyword="$8"
  local response
  response="$(api_get "/api/data-steward/assets/files/${file_id}/preview")"
  assert_ok "${response}"
  python3 -c '
import json, sys
payload = json.load(sys.stdin)["data"]
status, mode, conversion_required, download_only, risk, hint_keyword = sys.argv[1:]
assert payload["previewStatus"] == status, payload
assert payload["previewMode"] == mode, payload
assert str(payload["conversionRequired"]).lower() == conversion_required, payload
assert str(payload["downloadOnly"]).lower() == download_only, payload
assert payload["riskLevel"] == risk, payload
assert payload.get("statusLabel"), payload
assert hint_keyword in payload.get("actionHint", ""), payload
assert "storagePath" not in payload, payload
' "$status" "$mode" "$conversion_required" "$download_only" "$risk" "$hint_keyword" <<< "${response}" >/dev/null
  pass "${label}: ${status}/${mode} 表达正确"
  assert_safe_payload "${label} preview" "${response}"
}

echo "=== Phase 2 Batch 7B: 文件预览转换体验增强 ==="

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
echo "--- 3. Preview status matrix ---"
create_file "PHASE2-7B-CHECK-${RUN_ID}.pdf" "DRAWING" "application/pdf" ".pdf"; PDF_ID="${CREATED_FILE_ID}"
create_file "PHASE2-7B-CHECK-${RUN_ID}.docx" "DOCUMENT" "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ".docx"; OFFICE_ID="${CREATED_FILE_ID}"
create_file "PHASE2-7B-CHECK-${RUN_ID}.dwg" "DRAWING" "application/acad" ".dwg"; CAD_ID="${CREATED_FILE_ID}"
create_file "PHASE2-7B-CHECK-${RUN_ID}.rvt" "MODEL" "application/octet-stream" ".rvt"; BIM_ID="${CREATED_FILE_ID}"
create_file "PHASE2-7B-CHECK-${RUN_ID}.zip" "DOCUMENT" "application/zip" ".zip"; ARCHIVE_ID="${CREATED_FILE_ID}"
create_file "PHASE2-7B-CHECK-${RUN_ID}.bin" "DOCUMENT" "application/octet-stream" ".bin"; UNKNOWN_ID="${CREATED_FILE_ID}"

assert_preview "PDF" "${PDF_ID}" "AVAILABLE" "BROWSER_NATIVE" "false" "false" "SUCCESS" "受控预览入口"
assert_preview "Office" "${OFFICE_ID}" "NEEDS_CONVERSION" "OFFICE_CONVERSION" "true" "false" "WARNING" "Office 转换服务"
assert_preview "CAD" "${CAD_ID}" "NEEDS_CONVERSION" "CAD_CONVERSION" "true" "false" "WARNING" "CAD 图纸转换"
assert_preview "BIM" "${BIM_ID}" "NEEDS_CONVERSION" "BIM_LIGHTWEIGHT" "true" "false" "WARNING" "BIM 轻量化"
assert_preview "归档包" "${ARCHIVE_ID}" "UNSUPPORTED" "DOWNLOAD_ONLY" "false" "true" "INFO" "下载原文件"
assert_preview "未知格式" "${UNKNOWN_ID}" "UNSUPPORTED" "NONE" "false" "true" "INFO" "暂不支持在线预览"

echo ""
echo "--- 4. Unsupported preview ticket guard ---"
OFFICE_TICKET_RESP="$(api_post "/api/data-steward/assets/files/${OFFICE_ID}/access-tickets" '{"action":"PREVIEW"}')"
OFFICE_TICKET_CODE="$(echo "${OFFICE_TICKET_RESP}" | jq -r '.code // ""')"
if [ "${OFFICE_TICKET_CODE}" != "OK" ] \
  && grep -q "Office 转换服务" <<< "${OFFICE_TICKET_RESP}" \
  && ! grep -q "accessUrl" <<< "${OFFICE_TICKET_RESP}"; then
  pass "Office 未转换文件不会创建预览访问票据"
else
  fail "Office 未转换文件应阻止预览票据创建: ${OFFICE_TICKET_RESP}"
fi

ARCHIVE_TICKET_RESP="$(api_post "/api/data-steward/assets/files/${ARCHIVE_ID}/access-tickets" '{"action":"PREVIEW"}')"
ARCHIVE_TICKET_CODE="$(echo "${ARCHIVE_TICKET_RESP}" | jq -r '.code // ""')"
if [ "${ARCHIVE_TICKET_CODE}" != "OK" ] \
  && grep -q "下载原文件" <<< "${ARCHIVE_TICKET_RESP}" \
  && ! grep -q "accessUrl" <<< "${ARCHIVE_TICKET_RESP}"; then
  pass "归档包不会被误开在线预览票据"
else
  fail "归档包应阻止预览票据创建: ${ARCHIVE_TICKET_RESP}"
fi

echo ""
echo "--- 5. Export precheck display fields ---"
PRECHECK_RESP="$(api_get "/api/work-center/projects/${PID}/delivery-package/export-precheck?targetType=SECTION")"
assert_ok "${PRECHECK_RESP}"
DRY_RUN="$(echo "${PRECHECK_RESP}" | jq -r '.data.dryRun | tostring')"
PKG_GENERATED="$(echo "${PRECHECK_RESP}" | jq -r '.data.packageGenerated | tostring')"
if [ "${DRY_RUN}" = "true" ] && [ "${PKG_GENERATED}" = "false" ]; then
  pass "导出预检查仍为只读 dryRun=true/packageGenerated=false"
else
  fail "导出预检查只读标记错误: dryRun=${DRY_RUN}, packageGenerated=${PKG_GENERATED}"
fi
ROWS_LEN="$(echo "${PRECHECK_RESP}" | jq -r '(.data.rows | length) // 0')"
if [ "${ROWS_LEN}" -gt 0 ]; then
  echo "${PRECHECK_RESP}" | jq -e '
    .data.rows
    | all(has("downloadOnly") and has("statusLabel") and has("actionHint") and has("riskLevel"))
  ' >/dev/null
  pass "导出预检查 rows 已包含 7B 预览展示字段"
else
  pass "当前项目暂无导出预检查明细，跳过 rows 字段断言"
fi
assert_safe_payload "export-precheck" "${PRECHECK_RESP}"

echo ""
echo "--- 6. Frontend wording and shared utility ---"
if grep -q "previewFromFileName" frontend/src/modules/work-center/components/DeliveryViewPanel.vue \
  && grep -q "previewActionHint" frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue \
  && grep -q "previewActionHint" frontend/src/modules/data-steward/pages/AssetCatalogPage.vue \
  && grep -q "预览转换状态只影响在线查看体验" frontend/src/modules/work-center/components/DeliveryViewPanel.vue; then
  pass "前端页面复用统一预览状态工具并保留 7B 业务说明"
else
  fail "前端统一预览状态展示或 7B 业务说明缺失"
fi

echo ""
echo "=== Phase 2 Batch 7B Result: PASS=${PASS} FAIL=${FAIL} ==="
if [ "${FAIL}" -eq 0 ]; then
  echo "ALL PASS"
else
  echo "SOME FAILED"
  exit 1
fi
