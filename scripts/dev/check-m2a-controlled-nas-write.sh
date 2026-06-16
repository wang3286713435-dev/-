#!/usr/bin/env bash
# M2A: NAS 受控文件操作安全底座 smoke
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
PROJECT_CODE="M2A-SMOKE-${RUN_ID}"
PROJECT_NAME="M2A受控NAS写操作测试${RUN_ID}"
VIEWER_USER="m2a.viewer.${RUN_ID}"
VIEWER_PASSWORD="Viewer@12345"
NO_ACCESS_USER="m2a.noaccess.${RUN_ID}"
NO_ACCESS_PASSWORD="NoAccess@12345"
NAS_ROOT="${NAS_ROOT:-/tmp/delivery-m2a-nas-${RUN_ID}}"

PASS=0
FAIL=0
ADMIN_TOKEN=""
VIEWER_TOKEN=""
NO_ACCESS_TOKEN=""
PROJECT_ID=""

pass() {
  echo "  [PASS] $1"
  PASS=$((PASS + 1))
}

fail() {
  echo "  [FAIL] $1"
  FAIL=$((FAIL + 1))
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

json_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
value = eval(os.environ["EXPR"], {"__builtins__": {}}, {"data": data, "len": len, "any": any, "all": all})
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

assert_not_ok() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
assert data.get("code") != "OK", data
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
    r"/tmp(?:/|$)",
    r"/private(?:/|$)",
    r"/var(?:/|$)",
    r"\bnas://",
    r"\bsmb://",
    r"\bstorage_path\b",
    r"\bstorage_uri\b",
    r"\bstoragePath\b",
    r"\bstorageUri\b",
    r"\braw row\b",
    r"\btoken\b",
    r"\bsecret\b",
    r"\bpassword\b",
    r"\bselect\s+.+\s+from\b",
    r"\binsert\s+into\b",
    r"\bupdate\s+.+\s+set\b",
    r"\bdelete\s+from\b",
]
for pattern in patterns:
    assert not re.search(pattern, payload, re.IGNORECASE), f"{label} contains forbidden pattern {pattern}: {payload[:800]}"
PY
}

post_json() {
  local token="$1"
  local url="$2"
  local body="$3"
  curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

patch_json() {
  local token="$1"
  local url="$2"
  local body="$3"
  curl -sS --connect-timeout 3 --max-time 30 -X PATCH "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

get_json() {
  local token="$1"
  local url="$2"
  curl -sS --connect-timeout 3 --max-time 30 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}"
}

upload_file() {
  local token="$1"
  local url="$2"
  local parent_path="$3"
  local file_path="$4"
  curl -sS --connect-timeout 3 --max-time 60 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}" \
    -F "parentPath=${parent_path}" \
    -F "fileKind=DOCUMENT" \
    -F "file=@${file_path};type=text/plain"
}

login() {
  local username="$1"
  local password="$2"
  local response
  response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${username}\",\"password\":\"${password}\"}")"
  assert_ok "${response}"
  json_expr "${response}" "data['data']['accessToken']"
}

switch_project() {
  local token="$1"
  local project_id="$2"
  local response
  response="$(post_json "${token}" "/api/core/projects/${project_id}:switch" "{}")"
  assert_ok "${response}"
  json_expr "${response}" "data['data']['accessToken']"
}

cleanup() {
  if [[ -n "${PROJECT_ID}" ]]; then
    mysql_exec "UPDATE data_nas_operation_records SET message=message WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_nas_write_trial_configs SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_file_resources SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_nas_directory_records SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_asset_project_path_mappings SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_user_project_roles SET deleted=1 WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_projects SET deleted=1 WHERE id=${PROJECT_ID};" >/dev/null 2>&1 || true
  fi
  mysql_exec "UPDATE core_users SET deleted=1, status='DISABLED' WHERE username IN ('${VIEWER_USER}', '${NO_ACCESS_USER}');" >/dev/null 2>&1 || true
  rm -rf "${NAS_ROOT}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "=== M2A: controlled NAS write smoke ==="

echo ""
echo "--- 1. Prepare isolated project and users ---"
mkdir -p "${NAS_ROOT}"
mysql_exec "INSERT INTO core_projects (code, name, industry_type, project_stage, project_manager_name, asset_status, asset_source, status, created_by, updated_by)
VALUES ('${PROJECT_CODE}', '${PROJECT_NAME}', 'BIM', 'SMOKE', 'M2A', 'ACTIVE', 'M2A_SMOKE', 'ACTIVE', 1, 1);"
PROJECT_ID="$(mysql_exec "SELECT id FROM core_projects WHERE code='${PROJECT_CODE}' AND deleted=0 ORDER BY id DESC LIMIT 1;")"
mysql_exec "INSERT INTO data_asset_project_path_mappings (project_id, provider_code, nas_path, match_strategy, enabled, sort_order, remark, created_by, updated_by)
VALUES (${PROJECT_ID}, 'NAS', '${NAS_ROOT}', 'PREFIX', 1, -100, 'M2A smoke isolated root', 1, 1);"
mysql_exec "INSERT INTO data_nas_write_trial_configs (
  project_id, enabled, allowed_relative_roots_json, allowed_role_codes_json,
  allowed_user_ids_json, trial_mode_notice, created_by, updated_by
) VALUES (
  ${PROJECT_ID}, 1, JSON_ARRAY(''), JSON_ARRAY('DELIVERY_ENGINEER', 'PROJECT_ADMIN'),
  JSON_ARRAY(), 'M2A regression enables the isolated temporary project root only.', 1, 1
);"
mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${PROJECT_ID}, r.id FROM core_users u JOIN core_roles r ON r.code='PROJECT_ADMIN'
WHERE u.username='${ADMIN_USER}'
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);"
mysql_exec "INSERT INTO core_users (username, password_hash, display_name, status)
VALUES ('${VIEWER_USER}', '{noop}${VIEWER_PASSWORD}', 'M2A查看者', 'ACTIVE'),
       ('${NO_ACCESS_USER}', '{noop}${NO_ACCESS_PASSWORD}', 'M2A无权限员工', 'ACTIVE')
ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), status='ACTIVE', deleted=0;"
mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${PROJECT_ID}, r.id FROM core_users u JOIN core_roles r ON r.code='PROJECT_VIEWER'
WHERE u.username='${VIEWER_USER}'
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);"
pass "已准备隔离测试项目和安全临时 NAS 根目录"

ADMIN_TOKEN="$(login "${ADMIN_USER}" "${ADMIN_PASSWORD}")"
pre_switch_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"","name":"pre-switch"}')"
assert_ok "${pre_switch_response}"
assert_no_forbidden "pre-switch mkdir response" "${pre_switch_response}"
[[ -d "${NAS_ROOT}/pre-switch" ]]
pass "项目工作台路由项目可直接执行 NAS 操作，不要求 JWT 当前项目先切换"
ADMIN_TOKEN="$(switch_project "${ADMIN_TOKEN}" "${PROJECT_ID}")"
VIEWER_TOKEN="$(login "${VIEWER_USER}" "${VIEWER_PASSWORD}")"
VIEWER_TOKEN="$(switch_project "${VIEWER_TOKEN}" "${PROJECT_ID}")"
NO_ACCESS_TOKEN="$(login "${NO_ACCESS_USER}" "${NO_ACCESS_PASSWORD}")"
pass "管理员、查看者、无项目员工登录准备完成"

echo ""
echo "--- 2. Directory create and file upload ---"
mkdir_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"","name":"inbox"}')"
assert_ok "${mkdir_response}"
assert_no_forbidden "mkdir response" "${mkdir_response}"
[[ -d "${NAS_ROOT}/inbox" ]]
pass "管理员可创建项目内文件夹，响应不泄露真实路径"

sample_file="${NAS_ROOT}/sample-source.txt"
printf 'M2A smoke upload, no parser or index\n' >"${sample_file}"
upload_response="$(upload_file "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/files:upload" "inbox" "${sample_file}")"
assert_ok "${upload_response}"
assert_no_forbidden "upload response" "${upload_response}"
FILE_ID="$(json_expr "${upload_response}" "data['data']['fileId']")"
[[ -f "${NAS_ROOT}/inbox/sample-source.txt" ]]
pass "管理员可上传小文件到项目内文件夹"

directories_response="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/catalog/directories?projectId=${PROJECT_ID}")"
assert_ok "${directories_response}"
assert_no_forbidden "catalog directories response" "${directories_response}"
RESPONSE="${directories_response}" python3 - <<'PY'
import json, os
rows = json.loads(os.environ["RESPONSE"])["data"]
assert any(row["directoryPath"] == "inbox" for row in rows), rows
PY
pass "新建空目录可进入目录树"

echo ""
echo "--- 3. Rename, move, quarantine and restore file ---"
rename_response="$(patch_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/files/${FILE_ID}:rename" '{"newName":"renamed.txt"}')"
assert_ok "${rename_response}"
assert_no_forbidden "rename response" "${rename_response}"
[[ -f "${NAS_ROOT}/inbox/renamed.txt" ]]
pass "文件重命名成功"

move_dir_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"","name":"moved"}')"
assert_ok "${move_dir_response}"
move_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/files/${FILE_ID}:move" '{"targetDirectory":"moved"}')"
assert_ok "${move_response}"
assert_no_forbidden "move response" "${move_response}"
[[ -f "${NAS_ROOT}/moved/renamed.txt" ]]
pass "文件移动成功"

quarantine_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/files/${FILE_ID}:quarantine" '{"reason":"M2A smoke quarantine"}')"
assert_ok "${quarantine_response}"
assert_no_forbidden "file quarantine response" "${quarantine_response}"
QUARANTINE_ID="$(json_expr "${quarantine_response}" "data['data']['quarantineRecordId']")"
[[ ! -f "${NAS_ROOT}/moved/renamed.txt" ]]
[[ -d "${NAS_ROOT}/.delivery-quarantine" ]]
pass "文件删除仅进入回收站，不做永久删除"

restore_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/quarantine/${QUARANTINE_ID}:restore" "{}")"
assert_ok "${restore_response}"
assert_no_forbidden "file restore response" "${restore_response}"
[[ -f "${NAS_ROOT}/moved/renamed.txt" ]]
pass "回收站文件可恢复到原位置"

echo ""
echo "--- 4. Directory rename, move, quarantine and restore ---"
folder_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"","name":"folder-a"}')"
assert_ok "${folder_response}"
dir_rename_response="$(patch_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories:rename" '{"sourcePath":"folder-a","newName":"folder-b"}')"
assert_ok "${dir_rename_response}"
assert_no_forbidden "directory rename response" "${dir_rename_response}"
[[ -d "${NAS_ROOT}/folder-b" ]]
pass "文件夹重命名成功"

target_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"","name":"target"}')"
assert_ok "${target_response}"
dir_move_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories:move" '{"sourcePath":"folder-b","targetDirectory":"target"}')"
assert_ok "${dir_move_response}"
assert_no_forbidden "directory move response" "${dir_move_response}"
[[ -d "${NAS_ROOT}/target/folder-b" ]]
pass "文件夹移动成功"

dir_quarantine_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories:quarantine" '{"sourcePath":"target/folder-b","reason":"M2A smoke directory quarantine"}')"
assert_ok "${dir_quarantine_response}"
assert_no_forbidden "directory quarantine response" "${dir_quarantine_response}"
DIR_QUARANTINE_ID="$(json_expr "${dir_quarantine_response}" "data['data']['quarantineRecordId']")"
[[ ! -d "${NAS_ROOT}/target/folder-b" ]]
pass "文件夹删除仅进入回收站"

dir_restore_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/quarantine/${DIR_QUARANTINE_ID}:restore" "{}")"
assert_ok "${dir_restore_response}"
assert_no_forbidden "directory restore response" "${dir_restore_response}"
[[ -d "${NAS_ROOT}/target/folder-b" ]]
pass "回收站文件夹可恢复"

echo ""
echo "--- 5. Permission and path safety ---"
viewer_denied="$(post_json "${VIEWER_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"","name":"viewer-denied"}')"
assert_not_ok "${viewer_denied}"
assert_no_forbidden "viewer denied response" "${viewer_denied}"
[[ ! -e "${NAS_ROOT}/viewer-denied" ]]
pass "PROJECT_VIEWER 写操作被拒绝"

no_access_denied="$(post_json "${NO_ACCESS_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"","name":"no-access-denied"}')"
assert_not_ok "${no_access_denied}"
assert_no_forbidden "no access denied response" "${no_access_denied}"
[[ ! -e "${NAS_ROOT}/no-access-denied" ]]
pass "未授权员工写操作被拒绝"

traversal_denied="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"../escape","name":"bad"}')"
assert_not_ok "${traversal_denied}"
assert_no_forbidden "path traversal denied response" "${traversal_denied}"
[[ ! -e "${NAS_ROOT}/../escape/bad" ]]
pass "路径穿越被拒绝"

echo ""
echo "--- 6. Operation, quarantine, audit and forbidden-field scan ---"
operations_response="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/operations?limit=50")"
assert_ok "${operations_response}"
assert_no_forbidden "operations response" "${operations_response}"
RESPONSE="${operations_response}" python3 - <<'PY'
import json, os
rows = json.loads(os.environ["RESPONSE"])["data"]
types = {row["operationType"] for row in rows}
required = {"DIRECTORY_CREATE", "FILE_UPLOAD", "FILE_RENAME", "FILE_MOVE", "FILE_QUARANTINE", "QUARANTINE_RESTORE"}
assert required.issubset(types), types
PY
pass "操作记录可查且不泄露真实路径"

quarantine_list_response="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/quarantine?limit=50")"
assert_ok "${quarantine_list_response}"
assert_no_forbidden "quarantine list response" "${quarantine_list_response}"
pass "回收站记录可查且不泄露真实路径"

audit_count="$(mysql_exec "SELECT COUNT(1) FROM core_audit_logs WHERE project_id=${PROJECT_ID} AND module_code='data-steward' AND action_code LIKE 'nas.%';")"
if [[ "${audit_count}" -lt 8 ]]; then
  fail "NAS 操作审计不足: ${audit_count}"
  exit 1
fi
pass "NAS 操作已写入审计日志"

file_status="$(mysql_exec "SELECT process_status FROM data_file_resources WHERE id=${FILE_ID} AND project_id=${PROJECT_ID};")"
if [[ "${file_status}" != "PROCESSED" ]]; then
  fail "恢复后文件状态异常: ${file_status}"
  exit 1
fi
pass "恢复后文件元数据重新可用"

printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
