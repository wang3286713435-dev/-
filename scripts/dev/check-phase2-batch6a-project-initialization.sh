#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-${1:-http://localhost:8080}}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
SUFFIX="${SUFFIX:-$(date +%s)}"
PROJECT_CODE="${PROJECT_CODE:-B6A-SMOKE-${SUFFIX}}"
TEMPLATE_CODE="${TEMPLATE_CODE:-MEP_BIM_BASIC}"

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "OK", data' <<< "${response}" >/dev/null
}

assert_not_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] != "OK", data' <<< "${response}" >/dev/null
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

sum_counts() {
  parse_json '
import json, sys
data=json.load(sys.stdin)["data"]
counts=data
for key in ("willCreate", "created", "willSkip", "skipped", "counts"):
    if key in data:
        counts=data[key]
        break
print(sum(int(counts.get(k, 0) or 0) for k in ("sectionNodes","nodeTypes","deliverableDefinitions","deliverableTypes","deliverableAttributes","directoryTemplates")))
'
}

echo "== create isolated smoke project =="
mysql_exec "
INSERT INTO core_projects (code, name, industry_type, owner_org_name, status, created_by, updated_by)
VALUES ('${PROJECT_CODE}', '二期6A初始化冒烟项目', 'BUILDING_MEP', '验收脚本', 'ACTIVE', 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), deleted = 0, status = 'ACTIVE';
SET @project_id = (SELECT id FROM core_projects WHERE code = '${PROJECT_CODE}' AND deleted = 0 LIMIT 1);
INSERT IGNORE INTO core_user_project_roles (user_id, project_id, role_id, created_by, updated_by)
VALUES (1, @project_id, 1, 1, 1);
SELECT @project_id;
" >/tmp/phase2-batch6a-project-id.txt
PROJECT_ID="$(tail -1 /tmp/phase2-batch6a-project-id.txt)"
echo "project=${PROJECT_ID} code=${PROJECT_CODE}"

echo "== login =="
login_response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${login_response}")"

echo "== switch project =="
switch_response="$(curl -sS -X POST "${BASE_URL}/api/core/projects/${PROJECT_ID}:switch" \
  -H "Authorization: Bearer ${token}")"
assert_ok "${switch_response}"
token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${switch_response}")"
auth_header=(-H "Authorization: Bearer ${token}")

echo "== initialization status before =="
status_before="$(curl -sS "${BASE_URL}/api/master-data/projects/${PROJECT_ID}/initialization/status" "${auth_header[@]}")"
echo "${status_before}"
assert_ok "${status_before}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["ready"] is False, data' <<< "${status_before}" >/dev/null

echo "== standard templates =="
templates="$(curl -sS "${BASE_URL}/api/master-data/standard-templates" "${auth_header[@]}")"
echo "${templates}"
assert_ok "${templates}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert any(item["templateCode"] == "MEP_BIM_BASIC" for item in data), data' <<< "${templates}" >/dev/null

echo "== template detail =="
detail="$(curl -sS "${BASE_URL}/api/master-data/standard-templates/${TEMPLATE_CODE}" "${auth_header[@]}")"
echo "${detail}"
assert_ok "${detail}"
detail_count="$(sum_counts <<< "${detail}")"
if [[ "${detail_count}" -le 0 ]]; then
  echo "template detail is empty" >&2
  exit 1
fi

echo "== preview before apply =="
preview_before="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${PROJECT_ID}/initialization:preview-template" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"templateCode\":\"${TEMPLATE_CODE}\"}")"
echo "${preview_before}"
assert_ok "${preview_before}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["blocked"] is False, data; assert sum(data["willCreate"].values()) > 0, data' <<< "${preview_before}" >/dev/null

echo "== apply without confirm must fail =="
apply_without_confirm="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${PROJECT_ID}/initialization:apply-template" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmApply\":false}")"
echo "${apply_without_confirm}"
assert_not_ok "${apply_without_confirm}"

echo "== apply template =="
apply_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${PROJECT_ID}/initialization:apply-template" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"templateCode\":\"${TEMPLATE_CODE}\",\"confirmApply\":true}")"
echo "${apply_response}"
assert_ok "${apply_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert sum(data["created"].values()) > 0, data; assert data["standardStatus"]["deliverableStandardReady"] is True, data' <<< "${apply_response}" >/dev/null

echo "== preview after apply must be idempotent =="
preview_after="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${PROJECT_ID}/initialization:preview-template" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"templateCode\":\"${TEMPLATE_CODE}\"}")"
echo "${preview_after}"
assert_ok "${preview_after}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["blocked"] is False, data; assert sum(data["willCreate"].values()) == 0, data; assert sum(data["willSkip"].values()) > 0, data' <<< "${preview_after}" >/dev/null

echo "== status after =="
status_after="$(curl -sS "${BASE_URL}/api/master-data/projects/${PROJECT_ID}/initialization/status" "${auth_header[@]}")"
echo "${status_after}"
assert_ok "${status_after}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["ready"] is True, data; assert data["standardStatus"]["nodeTypesLocked"] is True, data' <<< "${status_after}" >/dev/null

echo "== audit event =="
audit_count="$(mysql_exec "SELECT COUNT(1) FROM core_audit_logs WHERE project_id = ${PROJECT_ID} AND module_code = 'master-data' AND action_code = 'masterdata.initialization.template-apply';")"
if [[ "${audit_count}" -lt 1 ]]; then
  echo "missing template apply audit event" >&2
  exit 1
fi
echo "audit_count=${audit_count}"

echo "phase2 batch6a project initialization ok: project=${PROJECT_ID}"
