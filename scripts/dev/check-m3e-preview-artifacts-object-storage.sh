#!/usr/bin/env bash
# M3E: preview/conversion artifacts object storage smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"

PASS=0
FAIL=0
TOKEN=""
PDF_FILE_ID=""
CONVERSION_FILE_ID=""

pass() {
  PASS=$((PASS + 1))
  printf '  [PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '  [FAIL] %s\n' "$1" >&2
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql --default-character-set=utf8mb4 -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
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

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"]).get("data")
scope = {
    "__builtins__": {},
    "data": data,
    "len": len,
    "all": all,
    "any": any,
    "int": int,
    "str": str,
    "bool": bool,
    "isinstance": isinstance,
    "list": list,
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
    r"\bnas://",
    r"\bsmb://",
    r"\bstorage_path\b",
    r"\bstorage_uri\b",
    r"\bstoragePath\b",
    r"\bstorageUri\b",
    r"\bobject_key\b",
    r"\bobjectKey\b",
    r"\bbucket\b",
    r"\braw DB row\b",
    r"\braw row\b",
    r"\bsecret\b",
    r"\bpassword\b",
    r"\btoken\b",
    r"\bselect\s+.+\s+from\b",
    r"\binsert\s+into\b",
    r"\bupdate\s+.+\s+set\b",
    r"\bdelete\s+from\b",
]
for pattern in patterns:
    assert not re.search(pattern, payload, re.IGNORECASE), f"{label} contains forbidden pattern {pattern}: {payload[:1200]}"
PY
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 45 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="${2:-{}}"
  curl -sS --connect-timeout 3 --max-time 60 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

echo "=== M3E: preview artifacts object storage ==="

echo ""
echo "--- 1. Login and switch to project ${PROJECT_ID} ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_data_expr "${login_response}" "data['accessToken']")"
switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch" '{}')"
assert_ok "${switch_response}"
TOKEN="$(json_data_expr "${switch_response}" "data['accessToken']")"
pass "管理员登录并切换项目成功"

echo ""
echo "--- 2. Select object-stored native preview sample ---"
PDF_FILE_ID="$(mysql_exec "
  SELECT f.id
  FROM data_file_resources f
  JOIN data_file_object_versions fov ON fov.file_id = f.id
    AND fov.active = 1
    AND fov.deleted = 0
    AND fov.storage_state = 'OBJECT_STORED'
  WHERE f.project_id = ${PROJECT_ID}
    AND f.deleted = 0
    AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) IN ('pdf','png','jpg','jpeg','webp','gif','bmp','svg')
  ORDER BY f.id
  LIMIT 1;
" 2>/dev/null | head -n 1 || true)"
if [[ -z "${PDF_FILE_ID}" ]]; then
  fail "未找到已对象化的 PDF/图片样本，请先完成 M3D 小范围对象镜像"
  echo ""
  echo "=== Summary ==="
  printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
  exit 1
else
  pass "已选择对象化原生预览样本 fileId=${PDF_FILE_ID}"
fi

echo ""
echo "--- 3. Prepare browser-native preview artifact ---"
native_prepare="$(api_post "/api/data-steward/assets/files/${PDF_FILE_ID}/preview-artifacts:prepare" '{}')"
assert_ok "${native_prepare}"
assert_no_forbidden "native_prepare" "${native_prepare}"
native_ok="$(json_data_expr "${native_prepare}" "data['artifactType']=='BROWSER_NATIVE_PREVIEW' and data['previewStatus']=='AVAILABLE' and data['storageState']=='OBJECT_STORED' and data['generationStatus']=='COMPLETED' and data['conversionRequired'] == False and bool(data['assetUuid'])")"
if [[ "${native_ok}" == "true" ]]; then
  pass "PDF/图片原生预览产物已对象化，未重复返回对象底层路径"
else
  fail "PDF/图片原生预览产物状态不符合预期：${native_prepare}"
fi

native_get="$(api_get "/api/data-steward/assets/files/${PDF_FILE_ID}/preview-artifacts")"
assert_ok "${native_get}"
assert_no_forbidden "native_get" "${native_get}"
native_get_ok="$(json_data_expr "${native_get}" "any(item['artifactType']=='BROWSER_NATIVE_PREVIEW' and item['previewStatus']=='AVAILABLE' and item['storageState']=='OBJECT_STORED' for item in data)")"
if [[ "${native_get_ok}" == "true" ]]; then
  pass "GET preview-artifacts 能返回已准备的原生预览产物"
else
  fail "GET preview-artifacts 未返回原生预览产物：${native_get}"
fi

ticket_response="$(api_post "/api/data-steward/assets/files/${PDF_FILE_ID}/access-tickets" '{"action":"PREVIEW"}')"
assert_ok "${ticket_response}"
assert_no_forbidden "native_access_ticket" "${ticket_response}"
access_url="$(json_data_expr "${ticket_response}" "data['accessUrl']")"
curl -fsS --connect-timeout 3 --max-time 45 "${BASE_URL}${access_url}" -o /tmp/delivery-m3e-preview.bin
rm -f /tmp/delivery-m3e-preview.bin
pass "原生预览仍通过受控 file-access 入口打开"

echo ""
echo "--- 4. Prepare conversion-needed placeholder ---"
CONVERSION_FILE_ID="$(mysql_exec "
  SELECT f.id
  FROM data_file_resources f
  WHERE f.project_id = ${PROJECT_ID}
    AND f.deleted = 0
    AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) IN ('dwg','dxf','dgn','rvt','ifc','nwd','nwc','glb','gltf','doc','docx','xls','xlsx','ppt','pptx')
  ORDER BY FIELD(LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)), 'dwg','rvt','ifc','nwd','nwc','glb','gltf','doc','docx','xls','xlsx','ppt','pptx'), f.id
  LIMIT 1;
" 2>/dev/null | head -n 1 || true)"
if [[ -z "${CONVERSION_FILE_ID}" ]]; then
  fail "未找到需转换占位样本"
  echo ""
  echo "=== Summary ==="
  printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
  exit 1
else
  pass "已选择需转换占位样本 fileId=${CONVERSION_FILE_ID}"
fi

conversion_prepare="$(api_post "/api/data-steward/assets/files/${CONVERSION_FILE_ID}/preview-artifacts:prepare" '{}')"
assert_ok "${conversion_prepare}"
assert_no_forbidden "conversion_prepare" "${conversion_prepare}"
conversion_ok="$(json_data_expr "${conversion_prepare}" "data['artifactType'] in ['OFFICE_PREVIEW_PLACEHOLDER','CAD_PREVIEW_PLACEHOLDER','BIM_LIGHTWEIGHT_PLACEHOLDER'] and data['previewStatus']=='NEEDS_CONVERSION' and data['storageState']=='PENDING' and data['generationStatus']=='NOT_STARTED' and data['conversionRequired'] == True")"
if [[ "${conversion_ok}" == "true" ]]; then
  pass "DWG/RVT/Office 等仅生成转换占位，未伪造预览产物"
else
  fail "转换占位状态不符合预期：${conversion_prepare}"
fi

echo ""
echo "--- 5. Delivery export-precheck compatibility and forbidden-field scan ---"
precheck_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-package/export-precheck?targetType=SECTION")"
assert_ok "${precheck_response}"
assert_no_forbidden "export_precheck" "${precheck_response}"
precheck_ok="$(json_data_expr "${precheck_response}" "'conversionRequiredCount' in data and 'unsupportedPreviewCount' in data and data['dryRun'] == True and data['packageGenerated'] == False")"
if [[ "${precheck_ok}" == "true" ]]; then
  pass "交付包导出预检查字段未回归"
else
  fail "交付包导出预检查字段异常：${precheck_response}"
fi

echo ""
echo "=== Summary ==="
printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
