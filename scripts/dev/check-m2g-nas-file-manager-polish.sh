#!/usr/bin/env bash
# M2G: real NAS file manager polish smoke, isolated project only
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
PROJECT_CODE="M2G-SMOKE-${RUN_ID}"
PROJECT_NAME="M2G文件管理器灰度测试${RUN_ID}"
VIEWER_USER="m2g.viewer.${RUN_ID}"
VIEWER_PASSWORD="Viewer@12345"
NAS_ROOT="${NAS_ROOT:-/tmp/delivery-m2g-nas-${RUN_ID}}"

PASS=0
FAIL=0
ADMIN_TOKEN=""
VIEWER_TOKEN=""
PROJECT_ID=""
ADMIN_ID=""
FILE_ID=""

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
value = eval(os.environ["EXPR"], {"__builtins__": {}}, {
    "data": data, "len": len, "any": any, "all": all, "set": set
})
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
    assert not re.search(pattern, payload, re.IGNORECASE), f"{label} contains forbidden pattern {pattern}: {payload[:1000]}"
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

put_json() {
  local token="$1"
  local url="$2"
  local body="$3"
  curl -sS --connect-timeout 3 --max-time 30 -X PUT "${BASE_URL}${url}" \
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
    mysql_exec "UPDATE data_nas_write_trial_configs SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_file_resources SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_nas_directory_records SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_asset_project_path_mappings SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_user_project_roles SET deleted=1 WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_projects SET deleted=1 WHERE id=${PROJECT_ID};" >/dev/null 2>&1 || true
  fi
  mysql_exec "UPDATE core_users SET deleted=1, status='DISABLED' WHERE username='${VIEWER_USER}';" >/dev/null 2>&1 || true
  rm -rf "${NAS_ROOT}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "=== M2G: NAS file manager polish smoke ==="

echo ""
echo "--- 1. Prepare isolated project and users ---"
mkdir -p "${NAS_ROOT}"
ADMIN_ID="$(mysql_exec "SELECT id FROM core_users WHERE username='${ADMIN_USER}' AND deleted=0 ORDER BY id DESC LIMIT 1;")"
mysql_exec "INSERT INTO core_projects (code, name, industry_type, project_stage, project_manager_name, asset_status, asset_source, status, created_by, updated_by)
VALUES ('${PROJECT_CODE}', '${PROJECT_NAME}', 'BIM', 'SMOKE', 'M2G', 'ACTIVE', 'M2G_SMOKE', 'ACTIVE', 1, 1);"
PROJECT_ID="$(mysql_exec "SELECT id FROM core_projects WHERE code='${PROJECT_CODE}' AND deleted=0 ORDER BY id DESC LIMIT 1;")"
mysql_exec "INSERT INTO data_asset_project_path_mappings (project_id, provider_code, nas_path, match_strategy, enabled, sort_order, remark, created_by, updated_by)
VALUES (${PROJECT_ID}, 'NAS', '${NAS_ROOT}', 'PREFIX', 1, -100, 'M2G smoke isolated root', 1, 1);"
mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${PROJECT_ID}, r.id FROM core_users u JOIN core_roles r ON r.code='PROJECT_ADMIN'
WHERE u.username='${ADMIN_USER}'
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);"
mysql_exec "INSERT INTO core_users (username, password_hash, display_name, status)
VALUES ('${VIEWER_USER}', '{noop}${VIEWER_PASSWORD}', 'M2G查看者', 'ACTIVE')
ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), status='ACTIVE', deleted=0;"
mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${PROJECT_ID}, r.id FROM core_users u JOIN core_roles r ON r.code='PROJECT_VIEWER'
WHERE u.username='${VIEWER_USER}'
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);"
pass "已准备 M2G 隔离测试项目和临时 NAS 根目录"

ADMIN_TOKEN="$(login "${ADMIN_USER}" "${ADMIN_PASSWORD}")"
VIEWER_TOKEN="$(login "${VIEWER_USER}" "${VIEWER_PASSWORD}")"
ADMIN_TOKEN="$(switch_project "${ADMIN_TOKEN}" "${PROJECT_ID}")"
VIEWER_TOKEN="$(switch_project "${VIEWER_TOKEN}" "${PROJECT_ID}")"
pass "管理员和查看者登录准备完成"

echo ""
echo "--- 2. Default disabled trial blocks writes ---"
status_default="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/write-trial?directoryPath=trial-zone")"
assert_ok "${status_default}"
assert_no_forbidden "default trial status" "${status_default}"
[[ "$(json_expr "${status_default}" "data['data']['enabled']")" == "false" ]]
[[ "$(json_expr "${status_default}" "data['data']['canWrite']")" == "false" ]]
pass "无灰度配置时默认关闭且不可写"

disabled_write="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"trial-zone","name":"blocked-before-trial"}')"
assert_not_ok "${disabled_write}"
assert_no_forbidden "disabled write response" "${disabled_write}"
[[ ! -e "${NAS_ROOT}/trial-zone/blocked-before-trial" ]]
pass "灰度关闭时写接口拒绝且不落盘"

echo ""
echo "--- 3. Enable scoped trial and create folders ---"
enable_body="{\"enabled\":true,\"allowedRelativeRoots\":[\"trial-zone\"],\"allowedRoleCodes\":[\"PROJECT_ADMIN\"],\"allowedUserIds\":[${ADMIN_ID}],\"trialModeNotice\":\"M2G smoke only allows trial-zone.\"}"
enabled_status="$(put_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/write-trial" "${enable_body}")"
assert_ok "${enabled_status}"
assert_no_forbidden "enabled trial status" "${enabled_status}"
[[ "$(json_expr "${enabled_status}" "data['data']['enabled']")" == "true" ]]
pass "管理员可开启项目级 NAS 写入灰度并配置相对目录范围"

root_status="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/write-trial?directoryPath=")"
assert_ok "${root_status}"
assert_no_forbidden "root trial status" "${root_status}"
[[ "$(json_expr "${root_status}" "data['data']['directoryAllowed']")" == "false" ]]
pass "项目根目录不在白名单内时显示不可写"

trial_root_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"","name":"trial-zone"}')"
assert_ok "${trial_root_response}"
assert_no_forbidden "trial root create response" "${trial_root_response}"
[[ -d "${NAS_ROOT}/trial-zone" ]]
pass "允许范围根目录可由受控接口创建"

inbox_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"trial-zone","name":"inbox"}')"
archive_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"trial-zone","name":"archive"}')"
assert_ok "${inbox_response}"
assert_ok "${archive_response}"
assert_no_forbidden "allowed directory responses" "${inbox_response}${archive_response}"
[[ -d "${NAS_ROOT}/trial-zone/inbox" ]]
[[ -d "${NAS_ROOT}/trial-zone/archive" ]]
pass "允许目录内可新建文件夹并刷新目录树来源"

echo ""
echo "--- 4. File upload, rename, move, recycle and restore ---"
sample_file="${NAS_ROOT}/sample-source.txt"
printf 'M2G smoke upload, no parser or index\n' >"${sample_file}"
upload_response="$(upload_file "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/files:upload" "trial-zone/inbox" "${sample_file}")"
assert_ok "${upload_response}"
assert_no_forbidden "upload response" "${upload_response}"
FILE_ID="$(json_expr "${upload_response}" "data['data']['fileId']")"
[[ -f "${NAS_ROOT}/trial-zone/inbox/sample-source.txt" ]]
pass "允许目录内可上传文件"

catalog_directories="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/catalog/directories?projectId=${PROJECT_ID}")"
assert_ok "${catalog_directories}"
assert_no_forbidden "catalog directories response" "${catalog_directories}"
RESPONSE="${catalog_directories}" python3 - <<'PY'
import json, os
rows = json.loads(os.environ["RESPONSE"])["data"]
paths = {row["directoryPath"] for row in rows}
assert {"trial-zone", "trial-zone/inbox", "trial-zone/archive"}.issubset(paths), paths
PY
pass "目录树接口包含新建目录且不泄露真实路径"

catalog_files="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&directoryPath=trial-zone/inbox&pageSize=50")"
assert_ok "${catalog_files}"
assert_no_forbidden "catalog files response" "${catalog_files}"
RESPONSE="${catalog_files}" python3 - <<'PY'
import json, os
items = json.loads(os.environ["RESPONSE"])["data"]["items"]
assert any(item["fileName"] == "sample-source.txt" for item in items), items
for item in items:
    assert item["storagePathVisible"] is False, item
    assert item.get("storagePath") in (None, ""), item
PY
pass "文件列表刷新后可见上传文件且不泄露真实路径"

rename_response="$(patch_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/files/${FILE_ID}:rename" '{"newName":"renamed-m2g.txt"}')"
assert_ok "${rename_response}"
assert_no_forbidden "rename response" "${rename_response}"
[[ -f "${NAS_ROOT}/trial-zone/inbox/renamed-m2g.txt" ]]
pass "文件重命名成功"

move_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/files/${FILE_ID}:move" '{"targetDirectory":"trial-zone/archive"}')"
assert_ok "${move_response}"
assert_no_forbidden "move response" "${move_response}"
[[ -f "${NAS_ROOT}/trial-zone/archive/renamed-m2g.txt" ]]
pass "文件移动成功"

file_quarantine_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/files/${FILE_ID}:quarantine" '{"reason":"M2G smoke recycle file"}')"
assert_ok "${file_quarantine_response}"
assert_no_forbidden "file quarantine response" "${file_quarantine_response}"
FILE_QUARANTINE_ID="$(json_expr "${file_quarantine_response}" "data['data']['quarantineRecordId']")"
[[ ! -f "${NAS_ROOT}/trial-zone/archive/renamed-m2g.txt" ]]
pass "文件删除仅移入回收站"

file_restore_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/quarantine/${FILE_QUARANTINE_ID}:restore" "{}")"
assert_ok "${file_restore_response}"
assert_no_forbidden "file restore response" "${file_restore_response}"
[[ -f "${NAS_ROOT}/trial-zone/archive/renamed-m2g.txt" ]]
pass "回收站文件可恢复"

echo ""
echo "--- 5. Directory rename, move, recycle and restore ---"
folder_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"trial-zone","name":"folder-a"}')"
assert_ok "${folder_response}"
dir_rename_response="$(patch_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories:rename" '{"sourcePath":"trial-zone/folder-a","newName":"folder-b"}')"
assert_ok "${dir_rename_response}"
assert_no_forbidden "directory rename response" "${dir_rename_response}"
[[ -d "${NAS_ROOT}/trial-zone/folder-b" ]]
pass "文件夹重命名成功"

dir_move_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories:move" '{"sourcePath":"trial-zone/folder-b","targetDirectory":"trial-zone/archive"}')"
assert_ok "${dir_move_response}"
assert_no_forbidden "directory move response" "${dir_move_response}"
[[ -d "${NAS_ROOT}/trial-zone/archive/folder-b" ]]
pass "文件夹移动成功"

dir_quarantine_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories:quarantine" '{"sourcePath":"trial-zone/archive/folder-b","reason":"M2G smoke recycle directory"}')"
assert_ok "${dir_quarantine_response}"
assert_no_forbidden "directory quarantine response" "${dir_quarantine_response}"
DIR_QUARANTINE_ID="$(json_expr "${dir_quarantine_response}" "data['data']['quarantineRecordId']")"
[[ ! -d "${NAS_ROOT}/trial-zone/archive/folder-b" ]]
pass "文件夹删除仅移入回收站"

dir_restore_response="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/quarantine/${DIR_QUARANTINE_ID}:restore" "{}")"
assert_ok "${dir_restore_response}"
assert_no_forbidden "directory restore response" "${dir_restore_response}"
[[ -d "${NAS_ROOT}/trial-zone/archive/folder-b" ]]
pass "回收站文件夹可恢复"

echo ""
echo "--- 6. Permission and scope boundaries ---"
viewer_denied="$(post_json "${VIEWER_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"trial-zone","name":"viewer-denied"}')"
assert_not_ok "${viewer_denied}"
assert_no_forbidden "viewer denied response" "${viewer_denied}"
[[ ! -e "${NAS_ROOT}/trial-zone/viewer-denied" ]]
pass "查看者不能执行写操作"

outside_create="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories" '{"parentPath":"","name":"outside-denied"}')"
assert_not_ok "${outside_create}"
assert_no_forbidden "outside create denied response" "${outside_create}"
[[ ! -e "${NAS_ROOT}/outside-denied" ]]
pass "允许范围外新建目录被拒绝"

move_outside="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/files/${FILE_ID}:move" '{"targetDirectory":""}')"
assert_not_ok "${move_outside}"
assert_no_forbidden "move outside denied response" "${move_outside}"
[[ -f "${NAS_ROOT}/trial-zone/archive/renamed-m2g.txt" ]]
pass "允许范围内文件移出白名单被拒绝"

self_move="$(post_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/directories:move" '{"sourcePath":"trial-zone/archive","targetDirectory":"trial-zone/archive/folder-b"}')"
assert_not_ok "${self_move}"
assert_no_forbidden "directory self move denied response" "${self_move}"
[[ -d "${NAS_ROOT}/trial-zone/archive/folder-b" ]]
pass "文件夹不能移动到自身或子文件夹内"

echo ""
echo "--- 7. Records, recycle bin, audit and forbidden-field scan ---"
operations_response="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/operations?limit=100")"
assert_ok "${operations_response}"
assert_no_forbidden "operations response" "${operations_response}"
RESPONSE="${operations_response}" python3 - <<'PY'
import json, os
rows = json.loads(os.environ["RESPONSE"])["data"]
types = {row["operationType"] for row in rows}
required = {
    "DIRECTORY_CREATE", "FILE_UPLOAD", "FILE_RENAME", "FILE_MOVE",
    "FILE_QUARANTINE", "DIRECTORY_RENAME", "DIRECTORY_MOVE",
    "DIRECTORY_QUARANTINE", "QUARANTINE_RESTORE"
}
assert required.issubset(types), types
for row in rows:
    assert row.get("sourceDisplayPath") is None or not row["sourceDisplayPath"].startswith("/"), row
    assert row.get("targetDisplayPath") is None or not row["targetDisplayPath"].startswith("/"), row
PY
pass "操作记录覆盖文件和文件夹闭环且不泄露真实路径"

quarantine_response="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/nas/quarantine?limit=100")"
assert_ok "${quarantine_response}"
assert_no_forbidden "quarantine response" "${quarantine_response}"
RESPONSE="${quarantine_response}" python3 - <<'PY'
import json, os
rows = json.loads(os.environ["RESPONSE"])["data"]
target_types = {row["targetType"] for row in rows}
assert {"FILE", "DIRECTORY"}.issubset(target_types), rows
for row in rows:
    assert not row["originalDisplayPath"].startswith("/"), row
PY
pass "回收站同时包含文件和文件夹记录且不泄露真实路径"

audit_count="$(mysql_exec "SELECT COUNT(1) FROM core_audit_logs WHERE project_id=${PROJECT_ID} AND module_code='data-steward' AND action_code LIKE 'nas.%';")"
if [[ "${audit_count}" -lt 10 ]]; then
  fail "NAS 操作审计不足: ${audit_count}"
  exit 1
fi
pass "灰度配置和文件管理操作已写入审计日志"

printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
