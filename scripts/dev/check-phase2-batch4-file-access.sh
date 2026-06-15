#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
ADMIN_USER="${2:-admin}"
ADMIN_PASSWORD="${3:-123456}"
PROJECT_ID="${4:-1}"
OTHER_PROJECT_ID="${5:-2}"
VIEWER_USER="${VIEWER_USER:-phase2.viewer}"
VIEWER_PASSWORD="${VIEWER_PASSWORD:-Viewer@123}"
REGULAR_USER="${REGULAR_USER:-delivery.engineer}"
REGULAR_PASSWORD="${REGULAR_PASSWORD:-Engineer@123}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"

PASS_COUNT=0
FAIL_COUNT=0
TMP_DIR="$(mktemp -d /tmp/delivery-file-access.XXXXXX)"

cleanup() {
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

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

assert_not_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] != "OK", data; print(data["code"])' <<< "${response}"
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

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

create_file_resource() {
  local token="$1"
  local project_id="$2"
  local name="$3"
  local storage_uri="$4"
  local response
  response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${project_id}/file-resources" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "{\"originalName\":\"${name}\",\"fileKind\":\"DRAWING\",\"mimeType\":\"application/pdf\",\"sizeBytes\":128,\"storageUri\":\"${storage_uri}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
  assert_ok "${response}"
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${response}"
}

create_ticket() {
  local token="$1"
  local file_id="$2"
  local action="$3"
  curl -sS -X POST "${BASE_URL}/api/data-steward/assets/files/${file_id}/access-tickets" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "{\"action\":\"${action}\"}"
}

delete_file_resource() {
  local token="$1"
  local project_id="$2"
  local file_id="$3"
  if [[ -z "${file_id}" || "${file_id}" == "0" ]]; then
    return
  fi
  curl -sS -X DELETE "${BASE_URL}/api/data-steward/projects/${project_id}/file-resources/${file_id}" \
    -H "Authorization: Bearer ${token}" >/dev/null || true
}

echo '== prepare local files and viewer user =='
sample_file="${TMP_DIR}/sample-preview.pdf"
missing_file="${TMP_DIR}/missing-preview.pdf"
printf 'PDF sample for phase2 batch4 file access\n' >"${sample_file}"

mysql_exec "INSERT INTO core_users (username, password_hash, display_name, status)
VALUES ('${VIEWER_USER}', '{noop}${VIEWER_PASSWORD}', '二期批次四查看者', 'ACTIVE')
ON DUPLICATE KEY UPDATE password_hash='{noop}${VIEWER_PASSWORD}', display_name='二期批次四查看者', status='ACTIVE', deleted=0;"
mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${PROJECT_ID}, r.id FROM core_users u JOIN core_roles r ON r.code='PROJECT_VIEWER'
WHERE u.username='${VIEWER_USER}'
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);"
pass "查看者用户已准备"

admin_token="$(login "${ADMIN_USER}" "${ADMIN_PASSWORD}")"
admin_token="$(switch_project "${admin_token}" "${PROJECT_ID}")"
viewer_token="$(login "${VIEWER_USER}" "${VIEWER_PASSWORD}")"
viewer_token="$(switch_project "${viewer_token}" "${PROJECT_ID}")"
regular_token="$(login "${REGULAR_USER}" "${REGULAR_PASSWORD}")"
regular_token="$(switch_project "${regular_token}" "${PROJECT_ID}")"
pass "管理员、查看者、普通用户登录成功"

file_id="$(create_file_resource "${admin_token}" "${PROJECT_ID}" "批次四访问验证.pdf" "nas://${sample_file}")"
missing_file_id="$(create_file_resource "${admin_token}" "${PROJECT_ID}" "批次四缺失文件.pdf" "nas://${missing_file}")"
other_admin_token="$(switch_project "${admin_token}" "${OTHER_PROJECT_ID}")"
other_file_id="$(create_file_resource "${other_admin_token}" "${OTHER_PROJECT_ID}" "批次四跨项目验证.pdf" "nas://${sample_file}")"
admin_token="$(switch_project "${other_admin_token}" "${PROJECT_ID}")"
pass "测试文件资源已登记"

echo '== admin preview ticket and content =='
admin_preview_response="$(create_ticket "${admin_token}" "${file_id}" "PREVIEW")"
assert_ok "${admin_preview_response}"
admin_preview_url="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessUrl"])' <<< "${admin_preview_response}")"
preview_body="$(curl -fsS "${BASE_URL}${admin_preview_url}")"
if [[ "${preview_body}" == *"PDF sample for phase2 batch4"* ]]; then
  pass "管理员可通过短时票据打开预览"
else
  fail "管理员预览内容不匹配"
fi

echo '== admin download ticket and content =='
admin_download_response="$(create_ticket "${admin_token}" "${file_id}" "DOWNLOAD")"
assert_ok "${admin_download_response}"
admin_download_url="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessUrl"])' <<< "${admin_download_response}")"
headers_file="${TMP_DIR}/download.headers"
download_file="${TMP_DIR}/download.out"
curl -fsS -D "${headers_file}" -o "${download_file}" "${BASE_URL}${admin_download_url}"
if grep -qi 'content-disposition: attachment' "${headers_file}" && grep -q 'PDF sample for phase2 batch4' "${download_file}"; then
  pass "管理员可通过短时票据下载文件"
echo '== preview ticket cannot be used as download =='
preview_ticket_url="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessUrl"])' <<< "${admin_preview_response}")"
preview_as_download_headers="${TMP_DIR}/preview-as-download.headers"
curl -fsS -D "${preview_as_download_headers}" -o /dev/null "${BASE_URL}${preview_ticket_url}"
if grep -qi 'content-disposition: attachment' "${preview_as_download_headers}"; then
  fail "预览票据返回了下载响应"
else
  pass "预览票据不返回下载响应"
fi

else
  fail "管理员下载响应不符合预期"
fi

echo '== viewer preview allowed and download denied =='
viewer_preview_response="$(create_ticket "${viewer_token}" "${file_id}" "PREVIEW")"
assert_ok "${viewer_preview_response}"
viewer_preview_url="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessUrl"])' <<< "${viewer_preview_response}")"
curl -fsS "${BASE_URL}${viewer_preview_url}" >/dev/null
pass "查看者可预览"

viewer_download_response="$(create_ticket "${viewer_token}" "${file_id}" "DOWNLOAD" || true)"
viewer_download_code="$(assert_not_ok "${viewer_download_response}")"
if [[ "${viewer_download_code}" == "ASSET_FILE_DOWNLOAD_FORBIDDEN" ]]; then
  pass "查看者不能下载"
else
  fail "查看者下载拒绝错误码不正确: ${viewer_download_code}"
fi

echo '== delivery engineer can preview and download =='
regular_preview_response="$(create_ticket "${regular_token}" "${file_id}" "PREVIEW")"
assert_ok "${regular_preview_response}"
regular_preview_url="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessUrl"])' <<< "${regular_preview_response}")"
curl -fsS "${BASE_URL}${regular_preview_url}" >/dev/null
pass "交付工程师可预览"

regular_download_response="$(create_ticket "${regular_token}" "${file_id}" "DOWNLOAD")"
assert_ok "${regular_download_response}"
regular_download_url="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessUrl"])' <<< "${regular_download_response}")"
curl -fsS -o /dev/null "${BASE_URL}${regular_download_url}"
pass "交付工程师可下载"

echo '== catalog detail must hide NAS path for ordinary project users =='
viewer_catalog_detail="$(curl -sS "${BASE_URL}/api/data-steward/catalog/files/${file_id}" \
  -H "Authorization: Bearer ${viewer_token}")"
assert_ok "${viewer_catalog_detail}"
parse_json '
import json, sys
payload=json.load(sys.stdin)["data"]
assert payload["storagePathVisible"] is False, payload
assert payload.get("storagePath") in (None, ""), payload
assert payload["storagePathVisibilityReason"] != "LOCAL_DEV_ADMIN", payload
' <<< "${viewer_catalog_detail}" >/dev/null
pass "查看者目录详情不暴露真实 NAS 路径"

regular_catalog_detail="$(curl -sS "${BASE_URL}/api/data-steward/catalog/files/${file_id}" \
  -H "Authorization: Bearer ${regular_token}")"
assert_ok "${regular_catalog_detail}"
parse_json '
import json, sys
payload=json.load(sys.stdin)["data"]
assert payload["storagePathVisible"] is False, payload
assert payload.get("storagePath") in (None, ""), payload
assert payload["storagePathVisibilityReason"] != "LOCAL_DEV_ADMIN", payload
' <<< "${regular_catalog_detail}" >/dev/null
pass "交付工程师目录详情不暴露真实 NAS 路径"

echo '== cross project and missing file denied =='
cross_response="$(create_ticket "${regular_token}" "${other_file_id}" "PREVIEW" || true)"
cross_code="$(assert_not_ok "${cross_response}")"
if [[ "${cross_code}" == "ASSET_FILE_NOT_FOUND" || "${cross_code}" == "ASSET_FILE_ACCESS_DENIED" ]]; then
  pass "无项目权限用户不能创建跨项目访问票据"
else
  fail "跨项目拒绝错误码不正确: ${cross_code}"
fi
echo '== catalog file list must hide NAS path =='
viewer_catalog_list="$(curl -sS "${BASE_URL}/api/data-steward/catalog/files?projectId=${PROJECT_ID}&pageSize=50"   -H "Authorization: Bearer ${viewer_token}")"
assert_ok "${viewer_catalog_list}"
parse_json '
import json, sys
payload=json.load(sys.stdin)["data"]
items=payload["items"]
assert len(items) > 0, "no files in catalog list"
for item in items:
    assert item["storagePathVisible"] is False, item
    assert item["storagePathVisibilityReason"] not in ("", "PROJECT_ADMIN"), item
' <<< "${viewer_catalog_list}" >/dev/null
pass "查看者catalog列表不暴露NAS路径"


missing_response="$(create_ticket "${admin_token}" "${missing_file_id}" "PREVIEW" || true)"
missing_code="$(assert_not_ok "${missing_response}")"
if [[ "${missing_code}" == "ASSET_FILE_NOT_READABLE" || "${missing_code}" == "ASSET_FILE_PATH_INVALID" ]]; then
  pass "路径失效文件不能创建访问票据且错误清晰"
else
  fail "路径失效错误码不正确: ${missing_code}"
fi

echo '== openapi and audit =='
openapi="$(curl -fsS "${BASE_URL}/v3/api-docs")"
if grep -q 'access-tickets' <<< "${openapi}" && grep -q 'file-access' <<< "${openapi}"; then
  pass "OpenAPI 包含文件访问票据接口"
else
  fail "OpenAPI 缺少文件访问票据接口"
fi

audit_response="$(curl -sS "${BASE_URL}/api/core/projects/${PROJECT_ID}/audit-logs?moduleCode=data-steward&limit=80" \
  -H "Authorization: Bearer ${admin_token}")"
assert_ok "${audit_response}"
parse_json '
import json, sys
rows=json.load(sys.stdin)["data"]
actions={r["actionCode"] for r in rows}
required={"asset.file.preview.ticket.create","asset.file.download.ticket.create","asset.file.preview.open","asset.file.download.open","asset.file.access.denied","asset.file.access.failed"}
missing=required-actions
assert not missing, (missing, actions)
' <<< "${audit_response}" >/dev/null
pass "预览、下载、拒绝、失败动作均有审计"

echo '== cleanup test file resources =='
delete_file_resource "${admin_token}" "${PROJECT_ID}" "${file_id:-0}"
delete_file_resource "${admin_token}" "${PROJECT_ID}" "${missing_file_id:-0}"
other_admin_token="$(switch_project "${admin_token}" "${OTHER_PROJECT_ID}")"
delete_file_resource "${other_admin_token}" "${OTHER_PROJECT_ID}" "${other_file_id:-0}"
pass "本轮测试文件资源已清理"

echo "== result =="
echo "PASS=${PASS_COUNT} FAIL=${FAIL_COUNT}"
if [[ "${FAIL_COUNT}" -ne 0 ]]; then
  exit 1
fi
echo "phase2 batch4 file access check ok"
