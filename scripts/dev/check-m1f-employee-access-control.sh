#!/usr/bin/env bash
# M1F: 员工注册、项目权限管理与局域网试运行准备 smoke
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
PHONE="${PHONE:-139$(date +%H%M%S)$((RANDOM % 90 + 10))}"
DISPLAY_NAME="${DISPLAY_NAME:-M1F试用员工${RUN_ID}}"
DEPARTMENT_NAME="${DEPARTMENT_NAME:-数字化试运行组}"
EMPLOYEE_PASSWORD="${EMPLOYEE_PASSWORD:-M1f@12345}"

PASS=0
FAIL=0
ADMIN_TOKEN=""
EMPLOYEE_TOKEN=""
EMPLOYEE_USER_ID=""
ASSIGNED_PROJECT_ID=""
UNAUTHORIZED_PROJECT_ID=""

pass() {
  echo "  [PASS] $1"
  PASS=$((PASS + 1))
}

fail() {
  echo "  [FAIL] $1"
  FAIL=$((FAIL + 1))
}

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY' >/dev/null
import json
import os
data = json.loads(os.environ["RESPONSE"])
assert data.get("code") == "OK", data
assert data.get("traceId"), data
PY
}

assert_not_ok() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY' >/dev/null
import json
import os
data = json.loads(os.environ["RESPONSE"])
assert data.get("code") != "OK", data
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
value = eval(os.environ["EXPR"], {"__builtins__": {}}, {"data": data, "len": len, "any": any, "all": all, "str": str, "int": int})
if isinstance(value, bool):
    print("true" if value else "false")
elif value is None:
    print("")
else:
    print(value)
PY
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

post_json() {
  local token="$1"
  local url="$2"
  local body="$3"
  if [[ -n "${token}" ]]; then
    curl -sS --connect-timeout 3 --max-time 25 -X POST "${BASE_URL}${url}" \
      -H "Authorization: Bearer ${token}" \
      -H 'Content-Type: application/json' \
      -d "${body}"
  else
    curl -sS --connect-timeout 3 --max-time 25 -X POST "${BASE_URL}${url}" \
      -H 'Content-Type: application/json' \
      -d "${body}"
  fi
}

get_json() {
  local token="$1"
  local url="$2"
  curl -sS --connect-timeout 3 --max-time 25 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}"
}

patch_json() {
  local token="$1"
  local url="$2"
  local body="$3"
  curl -sS --connect-timeout 3 --max-time 25 -X PATCH "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

put_json() {
  local token="$1"
  local url="$2"
  local body="$3"
  curl -sS --connect-timeout 3 --max-time 25 -X PUT "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

delete_json() {
  local token="$1"
  local url="$2"
  curl -sS --connect-timeout 3 --max-time 25 -X DELETE "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${token}"
}

echo "=== M1F: employee registration and project access control ==="

echo ""
echo "--- 1. Admin login ---"
admin_login="$(post_json "" "/api/core/auth/login" "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${admin_login}"
ADMIN_TOKEN="$(json_data_expr "${admin_login}" "data['accessToken']")"
pass "管理员登录成功"

echo ""
echo "--- 2. Register isolated employee ---"
register_response="$(post_json "" "/api/core/auth/register" "{\"phoneNumber\":\"${PHONE}\",\"displayName\":\"${DISPLAY_NAME}\",\"departmentName\":\"${DEPARTMENT_NAME}\",\"password\":\"${EMPLOYEE_PASSWORD}\"}")"
assert_ok "${register_response}"
EMPLOYEE_USER_ID="$(json_data_expr "${register_response}" "data['userId']")"
pass "随机手机号员工注册成功: ${PHONE}"

echo ""
echo "--- 3. New employee can login without project ---"
employee_login="$(post_json "" "/api/core/auth/login" "{\"username\":\"${PHONE}\",\"password\":\"${EMPLOYEE_PASSWORD}\"}")"
assert_ok "${employee_login}"
EMPLOYEE_TOKEN="$(json_data_expr "${employee_login}" "data['accessToken']")"
employee_current_project_id="$(json_data_expr "${employee_login}" "data['currentProjectId']")"
if [[ -n "${employee_current_project_id}" ]]; then
  fail "无项目员工登录不应返回 currentProjectId"
  exit 1
fi
pass "无项目员工登录成功，currentProjectId 为空"

employee_me="$(get_json "${EMPLOYEE_TOKEN}" "/api/core/users/me")"
assert_ok "${employee_me}"
RESPONSE="${employee_me}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])["data"]
assert data["currentProject"] is None, data
assert data["projects"] == [], data
assert data["permissions"] == [], data
assert data["menus"] == [], data
PY
pass "无项目员工 /users/me 返回空项目、空权限、空菜单"

employee_projects="$(get_json "${EMPLOYEE_TOKEN}" "/api/data-steward/assets/projects")"
assert_ok "${employee_projects}"
RESPONSE="${employee_projects}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])["data"]
assert data == [], data
PY
pass "无项目员工看不到资产项目数据"

employee_admin_attempt="$(get_json "${EMPLOYEE_TOKEN}" "/api/core/users")"
assert_not_ok "${employee_admin_attempt}"
pass "普通员工调用管理员接口被拒绝"

echo ""
echo "--- 4. Admin list, assignable projects and project role grant ---"
employees_response="$(get_json "${ADMIN_TOKEN}" "/api/core/users?keyword=${PHONE}")"
assert_ok "${employees_response}"
RESPONSE="${employees_response}" PHONE="${PHONE}" python3 - <<'PY'
import json
import os
rows = json.loads(os.environ["RESPONSE"])["data"]
phone = os.environ["PHONE"]
assert any((row.get("phoneNumber") or row.get("username")) == phone for row in rows), rows
PY
pass "管理员员工列表能看到新员工"

assignable_response="$(get_json "${ADMIN_TOKEN}" "/api/core/projects/assignable")"
assert_ok "${assignable_response}"
ASSIGNED_PROJECT_ID="$(json_data_expr "${assignable_response}" "data[0]['id'] if data else ''")"
if [[ -z "${ASSIGNED_PROJECT_ID}" ]]; then
  fail "管理员没有可授权项目"
  exit 1
fi
UNAUTHORIZED_PROJECT_ID="$(json_data_expr "${assignable_response}" "data[1]['id'] if len(data) > 1 else ''")"
if [[ -z "${UNAUTHORIZED_PROJECT_ID}" ]]; then
  UNAUTHORIZED_PROJECT_ID="$((ASSIGNED_PROJECT_ID + 999999))"
fi
pass "管理员可授权项目已返回"

roles_response="$(get_json "${ADMIN_TOKEN}" "/api/core/roles/project-assignable")"
assert_ok "${roles_response}"
RESPONSE="${roles_response}" python3 - <<'PY'
import json
import os
rows = json.loads(os.environ["RESPONSE"])["data"]
codes = {row["code"] for row in rows}
assert {"PROJECT_VIEWER", "DELIVERY_ENGINEER", "PROJECT_ADMIN"}.issubset(codes), codes
descriptions = {row["code"]: row.get("description", "") for row in rows}
assert "NAS" in descriptions["DELIVERY_ENGINEER"], descriptions
assert "NAS" in descriptions["PROJECT_ADMIN"], descriptions
PY
pass "可授权项目角色为三档角色，并说明真实 NAS 权限边界"

grant_response="$(put_json "${ADMIN_TOKEN}" "/api/core/users/${EMPLOYEE_USER_ID}/project-roles" "{\"assignments\":[{\"projectId\":${ASSIGNED_PROJECT_ID},\"roleCode\":\"PROJECT_VIEWER\"}]}")"
assert_ok "${grant_response}"
RESPONSE="${grant_response}" PROJECT_ID="${ASSIGNED_PROJECT_ID}" python3 - <<'PY'
import json
import os
project_id = int(os.environ["PROJECT_ID"])
roles = json.loads(os.environ["RESPONSE"])["data"]["projectRoles"]
assert len(roles) == 1, roles
assert roles[0]["projectId"] == project_id, roles
assert roles[0]["roleCode"] == "PROJECT_VIEWER", roles
PY
pass "管理员已给新员工分配单项目 PROJECT_VIEWER"

echo ""
echo "--- 5. Employee access after grant ---"
employee_login_after_grant="$(post_json "" "/api/core/auth/login" "{\"username\":\"${PHONE}\",\"password\":\"${EMPLOYEE_PASSWORD}\"}")"
assert_ok "${employee_login_after_grant}"
EMPLOYEE_TOKEN="$(json_data_expr "${employee_login_after_grant}" "data['accessToken']")"

employee_me_after_grant="$(get_json "${EMPLOYEE_TOKEN}" "/api/core/users/me")"
assert_ok "${employee_me_after_grant}"
RESPONSE="${employee_me_after_grant}" PROJECT_ID="${ASSIGNED_PROJECT_ID}" python3 - <<'PY'
import json
import os
project_id = int(os.environ["PROJECT_ID"])
data = json.loads(os.environ["RESPONSE"])["data"]
assert len(data["projects"]) == 1, data
assert data["projects"][0]["id"] == project_id, data
assert data["currentProject"]["id"] == project_id, data
assert data["projects"][0]["roleCode"] == "PROJECT_VIEWER", data
PY
pass "员工重新登录后只看到被授权项目"

switch_unauthorized="$(post_json "${EMPLOYEE_TOKEN}" "/api/core/projects/${UNAUTHORIZED_PROJECT_ID}:switch" "{}")"
assert_not_ok "${switch_unauthorized}"
pass "员工切换未授权项目失败"

grant_admin_response="$(put_json "${ADMIN_TOKEN}" "/api/core/users/${EMPLOYEE_USER_ID}/project-roles" "{\"assignments\":[{\"projectId\":${ASSIGNED_PROJECT_ID},\"roleCode\":\"PROJECT_ADMIN\"}]}")"
assert_ok "${grant_admin_response}"
employee_login_admin_role="$(post_json "" "/api/core/auth/login" "{\"username\":\"${PHONE}\",\"password\":\"${EMPLOYEE_PASSWORD}\"}")"
assert_ok "${employee_login_admin_role}"
EMPLOYEE_TOKEN="$(json_data_expr "${employee_login_admin_role}" "data['accessToken']")"
employee_me_admin_role="$(get_json "${EMPLOYEE_TOKEN}" "/api/core/users/me")"
assert_ok "${employee_me_admin_role}"
RESPONSE="${employee_me_admin_role}" PROJECT_ID="${ASSIGNED_PROJECT_ID}" python3 - <<'PY'
import json
import os
project_id = int(os.environ["PROJECT_ID"])
data = json.loads(os.environ["RESPONSE"])["data"]
assert len(data["projects"]) == 1, data
assert data["projects"][0]["id"] == project_id, data
assert data["projects"][0]["roleCode"] == "PROJECT_ADMIN", data
permissions = set(data["permissions"])
required = {
    "DATA_STEWARD_ASSET_SCAN",
    "DATA_STEWARD_ASSET_MANAGE",
    "DATA_STEWARD_FILE_MANAGE",
    "DATA_STEWARD_FILE_DOWNLOAD",
    "CORE_PROJECT_ROLE_MANAGE",
}
assert required.issubset(permissions), permissions
PY
pass "管理员可给员工授予包含真实 NAS 资产维护的项目管理员权限"

echo ""
echo "--- 6. Disable, delete employee and verify audit ---"
disable_response="$(patch_json "${ADMIN_TOKEN}" "/api/core/users/${EMPLOYEE_USER_ID}/status" "{\"status\":\"DISABLED\"}")"
assert_ok "${disable_response}"
pass "管理员已停用员工"

disabled_login="$(post_json "" "/api/core/auth/login" "{\"username\":\"${PHONE}\",\"password\":\"${EMPLOYEE_PASSWORD}\"}")"
assert_not_ok "${disabled_login}"
pass "停用员工登录失败"

delete_response="$(delete_json "${ADMIN_TOKEN}" "/api/core/users/${EMPLOYEE_USER_ID}")"
assert_ok "${delete_response}"
pass "管理员已软删除员工账号"

deleted_login="$(post_json "" "/api/core/auth/login" "{\"username\":\"${PHONE}\",\"password\":\"${EMPLOYEE_PASSWORD}\"}")"
assert_not_ok "${deleted_login}"
pass "删除后员工不能登录"

deleted_token_me="$(get_json "${EMPLOYEE_TOKEN}" "/api/core/users/me")"
assert_not_ok "${deleted_token_me}"
pass "删除后员工旧 token 不能继续使用"

employees_after_delete="$(get_json "${ADMIN_TOKEN}" "/api/core/users?keyword=${PHONE}")"
assert_ok "${employees_after_delete}"
RESPONSE="${employees_after_delete}" PHONE="${PHONE}" python3 - <<'PY'
import json
import os
rows = json.loads(os.environ["RESPONSE"])["data"]
phone = os.environ["PHONE"]
assert not any((row.get("phoneNumber") or row.get("username")) == phone for row in rows), rows
PY
pass "删除后员工不再出现在管理列表"

audit_count="$(mysql_exec "
SELECT COUNT(1)
FROM core_audit_logs
WHERE target_id = '${EMPLOYEE_USER_ID}'
  AND action_code IN ('core.auth.register', 'core.user.project_roles.update', 'core.user.status.update', 'core.user.delete');
")"
if [[ "${audit_count}" -lt 4 ]]; then
  fail "注册、授权、停用、删除审计日志不足"
  exit 1
fi
pass "注册、授权、停用、删除审计日志可查"

echo ""
echo "--- 7. Summary ---"
printf 'PASS=%s FAIL=%s\n' "${PASS}" "${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
