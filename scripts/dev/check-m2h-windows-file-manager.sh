#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
PROJECT_CODE="M2H-DIRECT-${RUN_ID}"
PROJECT_NAME="M2H目录直达子项测试${RUN_ID}"
NAS_ROOT="/tmp/delivery-m2h-direct-${RUN_ID}"

COMPONENT="${ROOT_DIR}/frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue"
API_FILE="${ROOT_DIR}/frontend/src/modules/data-steward/api/dataSteward.ts"
TREE_UTIL="${ROOT_DIR}/frontend/src/modules/data-steward/utils/directoryTree.ts"
CATALOG_CONTROLLER="${ROOT_DIR}/backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/CatalogController.java"
CATALOG_SERVICE="${ROOT_DIR}/backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java"

PASS=0
FAIL=0
ADMIN_TOKEN=""
PROJECT_ID=""

ok() {
  PASS=$((PASS + 1))
  printf '[PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '[FAIL] %s\n' "$1" >&2
}

require_file() {
  local path="$1"
  local label="$2"
  if [[ -f "${path}" ]]; then
    ok "${label}"
  else
    fail "${label}"
  fi
}

require_pattern() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  if rg -q "${pattern}" "${file}"; then
    ok "${label}"
  else
    fail "${label}"
  fi
}

forbid_pattern() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  if rg -q "${pattern}" "${file}"; then
    fail "${label}"
  else
    ok "${label}"
  fi
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
scope = {
    "data": data, "len": len, "any": any, "all": all, "set": set, "sorted": sorted
}
value = eval(os.environ["EXPR"], {"__builtins__": {}, **scope}, scope)
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

login() {
  local response
  response="$(curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
  assert_ok "${response}"
  json_expr "${response}" "data['data']['accessToken']"
}

switch_project() {
  local token="$1"
  local project_id="$2"
  local response
  response="$(curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}/api/core/projects/${project_id}:switch" \
    -H "Authorization: Bearer ${token}")"
  assert_ok "${response}"
  json_expr "${response}" "data['data']['accessToken']"
}

get_json() {
  local path="$1"
  curl -sS --connect-timeout 3 --max-time 30 "${BASE_URL}${path}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}"
}

cleanup() {
  if [[ -n "${PROJECT_ID}" ]]; then
    mysql_exec "UPDATE data_file_resources SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_nas_directory_records SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_asset_project_path_mappings SET deleted=1, delete_token=id WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_user_project_roles SET deleted=1 WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    mysql_exec "UPDATE core_projects SET deleted=1 WHERE id=${PROJECT_ID};" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo '== M2H static checks =='
require_file "${COMPONENT}" "AssetProjectFileBrowser.vue exists"
require_file "${API_FILE}" "dataSteward API file exists"
require_file "${TREE_UTIL}" "directoryTree utility exists"
require_file "${CATALOG_CONTROLLER}" "CatalogController exists"
require_file "${CATALOG_SERVICE}" "CatalogApplicationService exists"

require_pattern "${COMPONENT}" "browserEntries" "Unified file/folder entry list exists"
require_pattern "${COMPONENT}" "selectedEntryKeys" "Selection state exists"
require_pattern "${COMPONENT}" "handleEntryClick" "Left-click selection handler exists"
require_pattern "${COMPONENT}" "event\\.metaKey \\|\\| event\\.ctrlKey" "Ctrl/Command multi-select is implemented"
require_pattern "${COMPONENT}" "event\\.shiftKey" "Shift range-select is implemented"
require_pattern "${COMPONENT}" "handleTableSurfaceClick" "Blank-area selection clearing is implemented"
require_pattern "${COMPONENT}" "handleEntryContextMenu" "Right-click context menu handler exists"
require_pattern "${COMPONENT}" "contextMenuItems" "Dynamic context menu items exist"
require_pattern "${COMPONENT}" "handleEntryDblClick" "Double-click handler exists"
require_pattern "${COMPONENT}" "runAsyncAction" "Native event handlers guard async errors"
require_pattern "${COMPONENT}" "isUserCancel" "User cancel events are handled without noisy errors"
require_pattern "${COMPONENT}" "previewFromFileName" "Preview policy utility is reused"
require_pattern "${COMPONENT}" "openControlledFileAccess\\(entry\\.file, 'PREVIEW'\\)" "Native preview uses controlled access ticket path"
require_pattern "${COMPONENT}" "handleContextMenuItemClick" "Context menu open/preview shares guarded command path"
require_pattern "${COMPONENT}" "fetchCatalogDirectoryChildren" "File manager requests current-directory direct children"
require_pattern "${API_FILE}" "directOnly\\?: boolean" "Catalog files API type supports directOnly"
require_pattern "${API_FILE}" "CatalogDirectoryChildrenQuery" "Catalog direct children API type exists"
require_pattern "${CATALOG_CONTROLLER}" "directory-children" "Catalog direct children endpoint exists"
require_pattern "${CATALOG_CONTROLLER}" "boolean directOnly" "Catalog files endpoint accepts directOnly"
require_pattern "${CATALOG_SERVICE}" "appendDirectDirectoryFilter" "Backend direct-only directory filter exists"
require_pattern "${TREE_UTIL}" "const prefixLength = 0" "Directory tree no longer hides common prefix directories"
require_pattern "${COMPONENT}" "modelPreviewDialogVisible" "Model preview placeholder exists"
require_pattern "${COMPONENT}" "createBatchDownloadTickets" "Batch download ticket list exists"
require_pattern "${COMPONENT}" "moveSelectedEntries" "Batch move entry point exists"
require_pattern "${COMPONENT}" "quarantineSelectedEntries" "Batch recycle-bin entry point exists"
require_pattern "${COMPONENT}" "不生成 ZIP" "Batch download clearly says no ZIP is generated"
require_pattern "${COMPONENT}" "不会永久删除" "Recycle-bin copy states no permanent delete"

for file in "${COMPONENT}" "${API_FILE}" "${TREE_UTIL}"; do
  forbid_pattern "${file}" "/Volumes/" "No hard-coded macOS NAS absolute path in ${file##*/}"
  forbid_pattern "${file}" "smb://" "No smb:// path exposure in ${file##*/}"
  forbid_pattern "${file}" "nas://" "No nas:// path exposure in ${file##*/}"
  forbid_pattern "${file}" "storage_path" "No storage_path output string in ${file##*/}"
  forbid_pattern "${file}" "storage_uri" "No storage_uri output string in ${file##*/}"
done

echo ''
echo '== M2H direct-only API smoke =='
ADMIN_ID="$(mysql_exec "SELECT id FROM core_users WHERE username='${ADMIN_USER}' AND deleted=0 ORDER BY id DESC LIMIT 1;")"
mysql_exec "INSERT INTO core_projects (code, name, industry_type, project_stage, project_manager_name, asset_status, asset_source, status, created_by, updated_by)
VALUES ('${PROJECT_CODE}', '${PROJECT_NAME}', 'BIM', 'SMOKE', 'M2H', 'ACTIVE', 'M2H_SMOKE', 'ACTIVE', 1, 1);"
PROJECT_ID="$(mysql_exec "SELECT id FROM core_projects WHERE code='${PROJECT_CODE}' AND deleted=0 ORDER BY id DESC LIMIT 1;")"
mysql_exec "INSERT INTO data_asset_project_path_mappings (project_id, provider_code, nas_path, match_strategy, enabled, sort_order, remark, created_by, updated_by)
VALUES (${PROJECT_ID}, 'NAS', '${NAS_ROOT}', 'PREFIX', 1, -100, 'M2H direct-only isolated root', 1, 1);"
mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${PROJECT_ID}, r.id FROM core_users u JOIN core_roles r ON r.code='PROJECT_ADMIN'
WHERE u.username='${ADMIN_USER}'
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);"
mysql_exec "INSERT INTO data_nas_directory_records (project_id, relative_path, display_name, parent_relative_path, status, created_by, updated_by)
VALUES
(${PROJECT_ID}, '01', '01', NULL, 'ACTIVE', ${ADMIN_ID}, ${ADMIN_ID}),
(${PROJECT_ID}, '01/A', 'A', '01', 'ACTIVE', ${ADMIN_ID}, ${ADMIN_ID}),
(${PROJECT_ID}, '01/A/deep', 'deep', '01/A', 'ACTIVE', ${ADMIN_ID}, ${ADMIN_ID}),
(${PROJECT_ID}, '02_empty', '02_empty', NULL, 'ACTIVE', ${ADMIN_ID}, ${ADMIN_ID});"
mysql_exec "INSERT INTO data_file_resources (
project_id, original_name, file_kind, mime_type, size_bytes, storage_uri, storage_provider, logical_path,
checksum, business_tag, version_no, process_status, review_status, confidence_level, created_by, updated_by
) VALUES
(${PROJECT_ID}, 'm2h-root.pdf', 'DRAWING', 'application/pdf', 101, 'nas://m2h-direct/${RUN_ID}/root.pdf', 'NAS', '${NAS_ROOT}/m2h-root.pdf', 'm2h-root', 'M2H', 'V1', 'PROCESSED', 'APPROVED', 'HIGH', ${ADMIN_ID}, ${ADMIN_ID}),
(${PROJECT_ID}, 'm2h-child.pdf', 'DRAWING', 'application/pdf', 102, 'nas://m2h-direct/${RUN_ID}/01/child.pdf', 'NAS', '${NAS_ROOT}/01/m2h-child.pdf', 'm2h-child', 'M2H', 'V1', 'PROCESSED', 'APPROVED', 'HIGH', ${ADMIN_ID}, ${ADMIN_ID}),
(${PROJECT_ID}, 'm2h-grandchild.pdf', 'DRAWING', 'application/pdf', 103, 'nas://m2h-direct/${RUN_ID}/01/A/grandchild.pdf', 'NAS', '${NAS_ROOT}/01/A/m2h-grandchild.pdf', 'm2h-grandchild', 'M2H', 'V1', 'PROCESSED', 'APPROVED', 'HIGH', ${ADMIN_ID}, ${ADMIN_ID}),
(${PROJECT_ID}, 'm2h-deep.pdf', 'DRAWING', 'application/pdf', 104, 'nas://m2h-direct/${RUN_ID}/01/A/deep/file.pdf', 'NAS', '${NAS_ROOT}/01/A/deep/m2h-deep.pdf', 'm2h-deep', 'M2H', 'V1', 'PROCESSED', 'APPROVED', 'HIGH', ${ADMIN_ID}, ${ADMIN_ID});"
ok "Prepared isolated catalog data"

ADMIN_TOKEN="$(login)"
ADMIN_TOKEN="$(switch_project "${ADMIN_TOKEN}" "${PROJECT_ID}")"
ok "Admin login and project switch succeeded"

directories_response="$(get_json "/api/data-steward/catalog/directories?projectId=${PROJECT_ID}")"
assert_ok "${directories_response}"
assert_no_forbidden "catalog directories" "${directories_response}"
[[ "$(json_expr "${directories_response}" "all(path in [item['directoryPath'] for item in data['data']] for path in ['01', '01/A', '01/A/deep', '02_empty'])")" == "true" ]]
ok "Directory endpoint keeps directory records and ancestors"

root_direct_response="$(get_json "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&directOnly=true&pageSize=50")"
assert_ok "${root_direct_response}"
assert_no_forbidden "root direct catalog files" "${root_direct_response}"
[[ "$(json_expr "${root_direct_response}" "data['data']['total']")" == "1" ]]
[[ "$(json_expr "${root_direct_response}" "data['data']['items'][0]['fileName']")" == "m2h-root.pdf" ]]
ok "Root direct-only returns only root files"

child_direct_response="$(get_json "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&directoryPath=01&directOnly=true&pageSize=50")"
assert_ok "${child_direct_response}"
assert_no_forbidden "child direct catalog files" "${child_direct_response}"
[[ "$(json_expr "${child_direct_response}" "data['data']['total']")" == "1" ]]
[[ "$(json_expr "${child_direct_response}" "data['data']['items'][0]['fileName']")" == "m2h-child.pdf" ]]
ok "Child directory direct-only excludes grandchildren"

recursive_response="$(get_json "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&directoryPath=01&pageSize=50")"
assert_ok "${recursive_response}"
assert_no_forbidden "recursive catalog files" "${recursive_response}"
[[ "$(json_expr "${recursive_response}" "data['data']['total']")" == "3" ]]
[[ "$(json_expr "${recursive_response}" "set(item['fileName'] for item in data['data']['items']) == set(['m2h-child.pdf', 'm2h-grandchild.pdf', 'm2h-deep.pdf'])")" == "true" ]]
ok "Default recursive directory behavior is preserved"

printf 'M2H Windows file manager check complete: PASS=%d FAIL=%d\n' "${PASS}" "${FAIL}"

if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
