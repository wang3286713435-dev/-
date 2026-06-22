#!/usr/bin/env bash
# PLM-2: project business profile MVP smoke check.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
RUN_ID="$(date +%s)-$RANDOM"
VIEWER_USER="plm2.viewer.${RUN_ID}"
ENGINEER_USER="plm2.engineer.${RUN_ID}"
NO_ACCESS_USER="plm2.noaccess.${RUN_ID}"
TEST_PASSWORD="Plm2@123"

PASS=0
FAIL=0
ADMIN_TOKEN=""
PROJECT_ID=""
OLD_PROFILE_EXISTS="0"
OLD_PROFILE_JSON=""

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
  local token="$1"
  local path="$2"
  local payload="$3"
  curl -sS -X POST "${BASE_URL}${path}" \
    -H "Content-Type: application/json" \
    ${token:+-H "Authorization: Bearer ${token}"} \
    -d "${payload}"
}

put_json() {
  local token="$1"
  local path="$2"
  local payload="$3"
  curl -sS -X PUT "${BASE_URL}${path}" \
    -H "Content-Type: application/json" \
    ${token:+-H "Authorization: Bearer ${token}"} \
    -d "${payload}"
}

get_json() {
  local token="$1"
  local path="$2"
  curl -sS "${BASE_URL}${path}" \
    ${token:+-H "Authorization: Bearer ${token}"}
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
    "next": next,
    "str": str,
    "int": int,
    "float": float,
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

profile_payload() {
  python3 - <<'PY'
import json
print(json.dumps({
    "budgetAmount": 1200000,
    "contractAmount": 1000000,
    "receivedAmount": 250000,
    "paymentStatus": "PARTIAL",
    "expectedPaymentDate": "2026-09-30",
    "plannedStartDate": "2026-06-01",
    "plannedDeliveryDate": "2026-12-31",
    "actualDeliveryDate": None,
    "currencyCode": "CNY",
    "businessRemark": "PLM-2 smoke profile; restored after check when previous profile exists."
}, ensure_ascii=False))
PY
}

restore_payload_from_response() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])["data"]
payload = {
    "budgetAmount": data.get("budgetAmount"),
    "contractAmount": data.get("contractAmount"),
    "receivedAmount": data.get("receivedAmount"),
    "paymentStatus": data.get("paymentStatus") or "UNSET",
    "expectedPaymentDate": data.get("expectedPaymentDate"),
    "plannedStartDate": data.get("plannedStartDate"),
    "plannedDeliveryDate": data.get("plannedDeliveryDate"),
    "actualDeliveryDate": data.get("actualDeliveryDate"),
    "currencyCode": data.get("currencyCode") or "CNY",
    "businessRemark": data.get("businessRemark"),
}
print(json.dumps(payload, ensure_ascii=False))
PY
}

cleanup() {
  if [[ -n "${PROJECT_ID}" && -n "${ADMIN_TOKEN}" ]]; then
    if [[ "${OLD_PROFILE_EXISTS}" == "1" && -n "${OLD_PROFILE_JSON}" ]]; then
      put_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile" "${OLD_PROFILE_JSON}" >/dev/null || true
    elif [[ "${OLD_PROFILE_EXISTS}" == "0" ]]; then
      mysql_exec "UPDATE core_project_business_profiles SET deleted=1 WHERE project_id=${PROJECT_ID};" >/dev/null 2>&1 || true
    fi
  fi
  mysql_exec "UPDATE core_users SET deleted=1, status='DISABLED' WHERE username IN ('${VIEWER_USER}', '${ENGINEER_USER}', '${NO_ACCESS_USER}');" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "--- 1. Admin login and select project ---"
login_response="$(post_json "" "/api/core/auth/login" "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
if assert_ok "${login_response}"; then
  ADMIN_TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
  pass "管理员登录成功"
else
  fail "管理员登录失败"
fi

projects_response="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/assets/projects")"
if assert_ok "${projects_response}" && assert_no_forbidden "asset projects before" "${projects_response}"; then
  PROJECT_ID="$(json_expr "${projects_response}" "next((row['projectId'] for row in data['data'] if row.get('projectCategory') == 'REAL_NAS_PROJECT' or row.get('projectSource') == 'REAL_NAS'), data['data'][0]['projectId'] if data['data'] else '')")"
  if [[ -n "${PROJECT_ID}" ]]; then
    pass "已获取一个可访问项目 ${PROJECT_ID}"
  else
    fail "没有可访问项目"
    exit 1
  fi
else
  fail "项目列表接口失败或泄露禁止字段"
fi

echo ""
echo "--- 2. Read default/current business profile ---"
OLD_PROFILE_EXISTS="$(mysql_exec "SELECT COUNT(1) FROM core_project_business_profiles WHERE project_id=${PROJECT_ID} AND deleted=0;")"
profile_before="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile")"
if assert_ok "${profile_before}" && assert_no_forbidden "business profile before" "${profile_before}"; then
  OLD_PROFILE_JSON="$(restore_payload_from_response "${profile_before}")"
  RESPONSE="${profile_before}" PROJECT_ID="${PROJECT_ID}" python3 - <<'PY'
import json
import os
project_id = int(os.environ["PROJECT_ID"])
data = json.loads(os.environ["RESPONSE"])["data"]
assert data["projectId"] == project_id, data
assert "membersSummary" in data, data
assert data["paymentStatus"] in {"UNSET", "NOT_STARTED", "PARTIAL", "COMPLETED", "OVERDUE"}, data
PY
  pass "business-profile 返回默认/当前 profile 和成员摘要"
else
  fail "business-profile 默认读取失败"
fi

echo ""
echo "--- 3. Admin update and ratio validation ---"
update_response="$(put_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile" "$(profile_payload)")"
if assert_ok "${update_response}" && assert_no_forbidden "business profile update" "${update_response}"; then
  RESPONSE="${update_response}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])["data"]
assert float(data["contractAmount"]) == 1000000.0, data
assert float(data["receivedAmount"]) == 250000.0, data
assert round(float(data["paymentProgressPercent"]), 2) == 25.0, data
assert data["paymentStatus"] == "PARTIAL", data
assert data["plannedDeliveryDate"] == "2026-12-31", data
assert data["editable"] is True, data
PY
  pass "项目管理员/超级管理员可更新经营信息，回款比例计算正确"
else
  fail "管理员更新经营信息失败"
fi

invalid_response="$(put_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile" '{"contractAmount":100,"receivedAmount":101,"paymentStatus":"PARTIAL"}')"
if assert_not_ok "${invalid_response}"; then
  pass "已回款大于合同金额被拒绝"
else
  fail "已回款大于合同金额未被拒绝"
fi

echo ""
echo "--- 4. Project list and workbench API fields ---"
projects_after="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/assets/projects")"
if assert_ok "${projects_after}" && assert_no_forbidden "asset projects after" "${projects_after}"; then
  RESPONSE="${projects_after}" PROJECT_ID="${PROJECT_ID}" python3 - <<'PY'
import json
import os
project_id = int(os.environ["PROJECT_ID"])
rows = json.loads(os.environ["RESPONSE"])["data"]
row = next(item for item in rows if item["projectId"] == project_id)
assert row["businessProfile"]["contractAmount"] == 1000000.00, row
assert round(float(row["businessProfile"]["paymentProgressPercent"]), 2) == 25.0, row
assert row["businessProfile"]["plannedDeliveryDate"] == "2026-12-31", row
assert row["membersSummary"]["memberCount"] >= 1, row
PY
  pass "项目启动台 API 已返回经营摘要和成员摘要"
else
  fail "项目列表经营摘要验证失败"
fi

members_response="$(get_json "${ADMIN_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/members-summary")"
if assert_ok "${members_response}" && assert_no_forbidden "members summary" "${members_response}"; then
  pass "项目工作台成员摘要接口可见"
else
  fail "成员摘要接口失败"
fi

echo ""
echo "--- 5. Read-only users cannot update ---"
mysql_exec "INSERT INTO core_users (username, password_hash, display_name, status)
VALUES ('${VIEWER_USER}', '{noop}${TEST_PASSWORD}', 'PLM2 查看者', 'ACTIVE'),
       ('${ENGINEER_USER}', '{noop}${TEST_PASSWORD}', 'PLM2 交付工程师', 'ACTIVE'),
       ('${NO_ACCESS_USER}', '{noop}${TEST_PASSWORD}', 'PLM2 未授权用户', 'ACTIVE')
ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), status='ACTIVE', deleted=0;"
mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${PROJECT_ID}, r.id FROM core_users u JOIN core_roles r ON r.code='PROJECT_VIEWER'
WHERE u.username='${VIEWER_USER}'
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);"
mysql_exec "INSERT INTO core_user_project_roles (user_id, project_id, role_id)
SELECT u.id, ${PROJECT_ID}, r.id FROM core_users u JOIN core_roles r ON r.code='DELIVERY_ENGINEER'
WHERE u.username='${ENGINEER_USER}'
ON DUPLICATE KEY UPDATE deleted=0, role_id=VALUES(role_id);"

viewer_login="$(post_json "" "/api/core/auth/login" "{\"username\":\"${VIEWER_USER}\",\"password\":\"${TEST_PASSWORD}\"}")"
engineer_login="$(post_json "" "/api/core/auth/login" "{\"username\":\"${ENGINEER_USER}\",\"password\":\"${TEST_PASSWORD}\"}")"
no_access_login="$(post_json "" "/api/core/auth/login" "{\"username\":\"${NO_ACCESS_USER}\",\"password\":\"${TEST_PASSWORD}\"}")"
assert_ok "${viewer_login}"
assert_ok "${engineer_login}"
assert_ok "${no_access_login}"
VIEWER_TOKEN="$(json_expr "${viewer_login}" "data['data']['accessToken']")"
ENGINEER_TOKEN="$(json_expr "${engineer_login}" "data['data']['accessToken']")"
NO_ACCESS_TOKEN="$(json_expr "${no_access_login}" "data['data']['accessToken']")"

viewer_read="$(get_json "${VIEWER_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile")"
engineer_read="$(get_json "${ENGINEER_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile")"
assert_ok "${viewer_read}"
assert_ok "${engineer_read}"
pass "DELIVERY_ENGINEER / VIEWER 可只读经营信息"

viewer_update="$(put_json "${VIEWER_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile" "$(profile_payload)")"
engineer_update="$(put_json "${ENGINEER_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile" "$(profile_payload)")"
assert_not_ok "${viewer_update}"
assert_not_ok "${engineer_update}"
pass "DELIVERY_ENGINEER / VIEWER 更新经营信息被拒绝"

no_access_read="$(get_json "${NO_ACCESS_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile")"
no_access_update="$(put_json "${NO_ACCESS_TOKEN}" "/api/data-steward/projects/${PROJECT_ID}/business-profile" "$(profile_payload)")"
assert_not_ok "${no_access_read}"
assert_not_ok "${no_access_update}"
pass "未授权用户读取 / 更新经营信息被拒绝"

echo ""
echo "--- 6. Audit and forbidden field scan ---"
audit_count="$(mysql_exec "SELECT COUNT(1) FROM core_audit_logs WHERE project_id=${PROJECT_ID} AND action_code='project.business-profile.update';")"
if [[ "${audit_count}" -ge 1 ]]; then
  pass "经营信息更新已写审计日志"
else
  fail "经营信息更新审计日志缺失"
fi

for payload in "${profile_before}" "${update_response}" "${projects_after}" "${members_response}" "${viewer_update}" "${no_access_read}"; do
  assert_no_forbidden "PLM-2 payload" "${payload}"
done
pass "PLM-2 响应未包含 raw path / bucket / object key / SQL / secret 等禁出字段"

echo ""
echo "PLM-2 project business profile check: PASS=${PASS} FAIL=${FAIL}"
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
