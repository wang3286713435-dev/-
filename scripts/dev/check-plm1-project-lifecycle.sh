#!/usr/bin/env bash
# PLM-1: project lifecycle MVP smoke check.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"

PASS=0
FAIL=0
TOKEN=""
PROJECT_CODE="PLM1-$(date +%s)-$RANDOM"
PROJECT_NAME="PLM-1 生命周期验证项目"
PROJECT_ID=""

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

post_json() {
  local path="$1"
  local payload="$2"
  curl -sS -X POST "${BASE_URL}${path}" \
    -H "Content-Type: application/json" \
    ${TOKEN:+-H "Authorization: Bearer ${TOKEN}"} \
    -d "${payload}"
}

get_json() {
  local path="$1"
  curl -sS "${BASE_URL}${path}" \
    ${TOKEN:+-H "Authorization: Bearer ${TOKEN}"}
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
    "int": int,
    "str": str,
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
    r"\bSQL\b",
    r"raw row",
    r"token",
    r"secret",
    r"password",
]
hits = [pattern for pattern in patterns if re.search(pattern, payload, re.IGNORECASE)]
assert not hits, f"{label} leaked forbidden fields: {hits}\n{payload}"
PY
}

echo "--- 1. Login super administrator ---"
login_response="$(post_json "/api/core/auth/login" "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
if assert_ok "${login_response}"; then
  TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
  pass "超级管理员登录成功"
else
  fail "超级管理员登录失败"
fi

echo "--- 2. Create lifecycle project ---"
create_payload="$(python3 - <<PY
import json
print(json.dumps({
    "code": "${PROJECT_CODE}",
    "name": "${PROJECT_NAME}",
    "industryType": "BUILDING_MEP",
    "projectStage": "PREPARATION",
    "projectManagerName": "PLM Smoke",
    "ownerOrgName": "数字化交付平台",
    "assetSource": "MANUAL"
}, ensure_ascii=False))
PY
)"
create_response="$(post_json "/api/data-steward/assets/projects:lifecycle-create" "${create_payload}")"
if assert_ok "${create_response}" \
  && assert_no_forbidden "create project" "${create_response}"; then
  PROJECT_ID="$(json_expr "${create_response}" "data['data']['projectId']")"
  storage_status="$(json_expr "${create_response}" "data['data']['storageWorkspaceStatus']")"
  root_status="$(json_expr "${create_response}" "data['data']['sectionRootStatus']")"
  root_id="$(json_expr "${create_response}" "data['data']['sectionRootNodeId']")"
  if [[ -n "${PROJECT_ID}" && "${storage_status}" == "CREATED" && -n "${root_status}" && -n "${root_id}" ]]; then
    pass "创建项目返回项目 ID、对象存储工作区状态和工程树根节点"
  else
    fail "创建项目响应缺少生命周期字段"
  fi
else
  fail "创建项目接口失败或响应泄露禁止字段"
fi

if [[ -z "${PROJECT_ID}" ]]; then
  echo "PLM-1 project lifecycle check: PASS=${PASS} FAIL=${FAIL}"
  exit 1
fi

echo "--- 3. Verify project list, role, section root, storage marker ---"
list_response="$(get_json "/api/data-steward/assets/projects")"
if assert_ok "${list_response}" && RESPONSE="${list_response}" PROJECT_CODE="${PROJECT_CODE}" python3 - <<'PY'; then
import json
import os
data = json.loads(os.environ["RESPONSE"])
rows = data["data"]
assert any(row.get("code") == os.environ["PROJECT_CODE"] for row in rows), rows
PY
  pass "新项目出现在默认项目启动台列表"
else
  fail "新项目未出现在项目列表"
fi

role_count="$(mysql_exec "
SELECT COUNT(1)
FROM core_user_project_roles upr
JOIN core_users u ON u.id = upr.user_id AND u.deleted = 0
JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
WHERE u.username = '${ADMIN_USER}'
  AND upr.project_id = ${PROJECT_ID}
  AND upr.deleted = 0
  AND r.code = 'PROJECT_ADMIN';
")"
if [[ "${role_count}" == "1" ]]; then
  pass "创建人拥有新项目 PROJECT_ADMIN"
else
  fail "创建人 PROJECT_ADMIN 授权异常: ${role_count}"
fi

root_count="$(mysql_exec "
SELECT COUNT(1)
FROM masterdata_section_nodes
WHERE project_id = ${PROJECT_ID}
  AND parent_id IS NULL
  AND deleted = 0;
")"
if [[ "${root_count}" == "1" ]]; then
  pass "工程树根节点存在且未重复"
else
  fail "工程树根节点数量异常: ${root_count}"
fi

storage_count_before="$(mysql_exec "
SELECT COUNT(1)
FROM data_storage_objects
WHERE deleted = 0
  AND source_provider = 'PLATFORM'
  AND object_key LIKE 'projects/${PROJECT_ID}/uploads/%/.workspace-keep';
")"
if [[ "${storage_count_before}" == "1" ]]; then
  pass "对象存储工作区占位记录存在"
else
  fail "对象存储工作区占位记录异常: ${storage_count_before}"
fi

echo "--- 4. Archive guardrails ---"
archive_false_response="$(post_json "/api/data-steward/assets/projects/${PROJECT_ID}:archive" "{\"confirmed\":false,\"confirmText\":\"${PROJECT_CODE}\"}")"
if assert_not_ok "${archive_false_response}"; then
  pass "confirmed=false 归档被拒绝"
else
  fail "confirmed=false 归档未被拒绝"
fi

archive_wrong_response="$(post_json "/api/data-steward/assets/projects/${PROJECT_ID}:archive" "{\"confirmed\":true,\"confirmText\":\"WRONG-${PROJECT_CODE}\"}")"
if assert_not_ok "${archive_wrong_response}"; then
  pass "确认文本错误归档被拒绝"
else
  fail "确认文本错误归档未被拒绝"
fi

archive_response="$(post_json "/api/data-steward/assets/projects/${PROJECT_ID}:archive" "{\"confirmed\":true,\"confirmText\":\"${PROJECT_CODE}\"}")"
if assert_ok "${archive_response}" \
  && assert_no_forbidden "archive project" "${archive_response}"; then
  archived="$(json_expr "${archive_response}" "data['data']['archived']")"
  object_deleted="$(json_expr "${archive_response}" "data['data']['objectStorageDeleted']")"
  nas_touched="$(json_expr "${archive_response}" "data['data']['nasTouched']")"
  if [[ "${archived}" == "true" && "${object_deleted}" == "false" && "${nas_touched}" == "false" ]]; then
    pass "正确确认后项目软归档，且声明未删除对象存储/未触碰 NAS"
  else
    fail "归档响应业务字段异常"
  fi
else
  fail "正确确认归档失败或响应泄露禁止字段"
fi

echo "--- 5. Verify archived project hidden and data retained ---"
list_after_archive="$(get_json "/api/data-steward/assets/projects")"
if assert_ok "${list_after_archive}" && RESPONSE="${list_after_archive}" PROJECT_CODE="${PROJECT_CODE}" python3 - <<'PY'; then
import json
import os
data = json.loads(os.environ["RESPONSE"])
rows = data["data"]
assert all(row.get("code") != os.environ["PROJECT_CODE"] for row in rows), rows
PY
  pass "归档项目默认不再出现在项目启动台列表"
else
  fail "归档项目仍出现在默认项目列表"
fi

project_archived_count="$(mysql_exec "SELECT COUNT(1) FROM core_projects WHERE id = ${PROJECT_ID} AND deleted = 1 AND asset_status = 'ARCHIVED';")"
storage_count_after="$(mysql_exec "
SELECT COUNT(1)
FROM data_storage_objects
WHERE deleted = 0
  AND source_provider = 'PLATFORM'
  AND object_key LIKE 'projects/${PROJECT_ID}/uploads/%/.workspace-keep';
")"
audit_count="$(mysql_exec "
SELECT COUNT(1)
FROM core_audit_logs
WHERE project_id = ${PROJECT_ID}
  AND action_code IN ('asset.project.create', 'asset.project.archive');
")"
if [[ "${project_archived_count}" == "1" && "${storage_count_after}" == "${storage_count_before}" && "${audit_count}" -ge 2 ]]; then
  pass "归档为软归档，存储占位未删除，审计记录存在"
else
  fail "归档后数据保留或审计异常: project=${project_archived_count}, storageBefore=${storage_count_before}, storageAfter=${storage_count_after}, audit=${audit_count}"
fi

echo "PLM-1 project lifecycle check: PASS=${PASS} FAIL=${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
