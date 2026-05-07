#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-platform.admin}"
PASSWORD="${3:-Admin@123}"
TARGET_PROJECT_ID="${4:-2}"
SUFFIX="${5:-$(date +%s)}"

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "OK", data' <<< "${response}" >/dev/null
}

assert_code() {
  local response="$1"
  local expected="$2"
  EXPECTED="${expected}" parse_json 'import json,sys,os; data=json.load(sys.stdin); assert data["code"] == os.environ["EXPECTED"], data' \
    <<< "${response}" >/dev/null
}

json_id() {
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])'
}

echo "== login =="
login_response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")"
echo "${login_response}"
assert_ok "${login_response}"

access_token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${login_response}")"
current_project_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["currentProjectId"])' <<< "${login_response}")"

if [[ "${current_project_id}" != "${TARGET_PROJECT_ID}" ]]; then
  echo "== switch project =="
  switch_response="$(curl -sS -X POST "${BASE_URL}/api/core/projects/${TARGET_PROJECT_ID}:switch" \
    -H "Authorization: Bearer ${access_token}")"
  echo "${switch_response}"
  assert_ok "${switch_response}"
  access_token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${switch_response}")"
fi

auth_header=(-H "Authorization: Bearer ${access_token}")

echo "== current user menu includes deliverable standard =="
me_response="$(curl -sS "${BASE_URL}/api/core/users/me" "${auth_header[@]}")"
echo "${me_response}"
assert_ok "${me_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; paths=[]; [paths.append(c["path"]) for m in data["menus"] for c in m.get("children", [])]; assert "/master-data/deliverable-standard" in paths, paths' \
  <<< "${me_response}" >/dev/null

node_type_code="DST-NODE-${SUFFIX}"
node_type_name="交付标准节点-${SUFFIX}"

echo "== create unlocked node type for precondition =="
create_node_type_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/node-types" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${node_type_code}\",\"name\":\"${node_type_name}\",\"scopeLevel\":1,\"sortOrder\":30,\"status\":\"ACTIVE\"}")"
echo "${create_node_type_response}"
assert_ok "${create_node_type_response}"
node_type_id="$(json_id <<< "${create_node_type_response}")"

definition_code="DST-DEF-${SUFFIX}"
definition_name="交付物定义-${SUFFIX}"

echo "== write is blocked before node types locked =="
precondition_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-definitions" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"nodeTypeId\":${node_type_id},\"code\":\"${definition_code}\",\"name\":\"${definition_name}\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${precondition_response}"
EXPECTED="MASTERDATA_NODE_TYPES_NOT_LOCKED" assert_code "${precondition_response}" "MASTERDATA_NODE_TYPES_NOT_LOCKED"

echo "== lock all node types =="
lock_all_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/node-types:lock" \
  "${auth_header[@]}")"
echo "${lock_all_response}"
assert_ok "${lock_all_response}"

echo "== standard status after lock =="
status_after_lock="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/standard-status" "${auth_header[@]}")"
echo "${status_after_lock}"
assert_ok "${status_after_lock}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["nodeTypesLocked"] is True, data' \
  <<< "${status_after_lock}" >/dev/null

echo "== parent ownership/existence validation =="
missing_definition_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-definitions" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"nodeTypeId\":999999999,\"code\":\"${definition_code}-MISS\",\"name\":\"${definition_name}-缺失节点类型\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":0,\"status\":\"ACTIVE\"}")"
echo "${missing_definition_response}"
assert_code "${missing_definition_response}" "MASTERDATA_NODE_TYPE_NOT_FOUND"

missing_type_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-types" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deliverableDefinitionId\":999999999,\"code\":\"DST-TYPE-MISS-${SUFFIX}\",\"name\":\"缺失定义类型-${SUFFIX}\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":0,\"status\":\"ACTIVE\"}")"
echo "${missing_type_response}"
assert_code "${missing_type_response}" "MASTERDATA_DELIVERABLE_DEF_NOT_FOUND"

missing_attribute_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-attributes" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deliverableTypeId\":999999999,\"code\":\"DST-ATTR-MISS-${SUFFIX}\",\"name\":\"缺失类型属性-${SUFFIX}\",\"valueType\":\"TEXT\",\"sortOrder\":0,\"status\":\"ACTIVE\"}")"
echo "${missing_attribute_response}"
assert_code "${missing_attribute_response}" "MASTERDATA_DELIVERABLE_TYPE_NOT_FOUND"

echo "== create deliverable definition =="
create_definition_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-definitions" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"nodeTypeId\":${node_type_id},\"code\":\"${definition_code}\",\"name\":\"${definition_name}\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${create_definition_response}"
assert_ok "${create_definition_response}"
definition_id="$(json_id <<< "${create_definition_response}")"

echo "== update deliverable definition =="
update_definition_response="$(curl -sS -X PATCH "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-definitions/${definition_id}" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${definition_code}\",\"name\":\"${definition_name}-已编辑\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":2,\"status\":\"ACTIVE\"}")"
echo "${update_definition_response}"
assert_ok "${update_definition_response}"

echo "== list deliverable definitions =="
definition_list_response="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-definitions" "${auth_header[@]}")"
echo "${definition_list_response}"
assert_ok "${definition_list_response}"
TARGET_ID="${definition_id}" parse_json 'import json,sys,os; data=json.load(sys.stdin)["data"]; assert any(str(item["id"]) == os.environ["TARGET_ID"] for item in data), data' \
  <<< "${definition_list_response}" >/dev/null

echo "== delete and recreate deliverable definition with same code =="
delete_definition_response="$(curl -sS -X DELETE "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-definitions/${definition_id}" \
  "${auth_header[@]}")"
echo "${delete_definition_response}"
assert_ok "${delete_definition_response}"
recreate_definition_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-definitions" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"nodeTypeId\":${node_type_id},\"code\":\"${definition_code}\",\"name\":\"${definition_name}-复建\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":3,\"status\":\"ACTIVE\"}")"
echo "${recreate_definition_response}"
assert_ok "${recreate_definition_response}"
definition_id="$(json_id <<< "${recreate_definition_response}")"

type_code="DST-TYPE-${SUFFIX}"
type_name="交付物类型-${SUFFIX}"

echo "== create deliverable type =="
create_type_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-types" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deliverableDefinitionId\":${definition_id},\"code\":\"${type_code}\",\"name\":\"${type_name}\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${create_type_response}"
assert_ok "${create_type_response}"
type_id="$(json_id <<< "${create_type_response}")"

echo "== update deliverable type =="
update_type_response="$(curl -sS -X PATCH "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-types/${type_id}" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${type_code}\",\"name\":\"${type_name}-已编辑\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":2,\"status\":\"ACTIVE\"}")"
echo "${update_type_response}"
assert_ok "${update_type_response}"

echo "== list deliverable types by definition =="
type_list_response="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-types?definitionId=${definition_id}" "${auth_header[@]}")"
echo "${type_list_response}"
assert_ok "${type_list_response}"
TARGET_ID="${type_id}" parse_json 'import json,sys,os; data=json.load(sys.stdin)["data"]; assert any(str(item["id"]) == os.environ["TARGET_ID"] for item in data), data' \
  <<< "${type_list_response}" >/dev/null

echo "== delete and recreate deliverable type with same code =="
delete_type_response="$(curl -sS -X DELETE "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-types/${type_id}" \
  "${auth_header[@]}")"
echo "${delete_type_response}"
assert_ok "${delete_type_response}"
recreate_type_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-types" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deliverableDefinitionId\":${definition_id},\"code\":\"${type_code}\",\"name\":\"${type_name}-复建\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":3,\"status\":\"ACTIVE\"}")"
echo "${recreate_type_response}"
assert_ok "${recreate_type_response}"
type_id="$(json_id <<< "${recreate_type_response}")"

attr_code="DST-ATTR-${SUFFIX}"
attr_name="交付物属性-${SUFFIX}"

echo "== create deliverable attribute =="
create_attr_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-attributes" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deliverableTypeId\":${type_id},\"code\":\"${attr_code}\",\"name\":\"${attr_name}\",\"valueType\":\"TEXT\",\"unit\":\"mm\",\"required\":true,\"exampleValue\":\"A1\",\"enumOptions\":\"A1,A2\",\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${create_attr_response}"
assert_ok "${create_attr_response}"
attr_id="$(json_id <<< "${create_attr_response}")"

echo "== update deliverable attribute =="
update_attr_response="$(curl -sS -X PATCH "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-attributes/${attr_id}" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${attr_code}\",\"name\":\"${attr_name}-已编辑\",\"valueType\":\"TEXT\",\"unit\":\"m\",\"required\":true,\"exampleValue\":\"B1\",\"enumOptions\":\"B1,B2\",\"sortOrder\":2,\"status\":\"ACTIVE\"}")"
echo "${update_attr_response}"
assert_ok "${update_attr_response}"

echo "== list deliverable attributes by type =="
attr_list_response="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-attributes?typeId=${type_id}" "${auth_header[@]}")"
echo "${attr_list_response}"
assert_ok "${attr_list_response}"
TARGET_ID="${attr_id}" parse_json 'import json,sys,os; data=json.load(sys.stdin)["data"]; assert any(str(item["id"]) == os.environ["TARGET_ID"] for item in data), data' \
  <<< "${attr_list_response}" >/dev/null

echo "== delete and recreate deliverable attribute with same code =="
delete_attr_response="$(curl -sS -X DELETE "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-attributes/${attr_id}" \
  "${auth_header[@]}")"
echo "${delete_attr_response}"
assert_ok "${delete_attr_response}"
recreate_attr_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-attributes" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deliverableTypeId\":${type_id},\"code\":\"${attr_code}\",\"name\":\"${attr_name}-复建\",\"valueType\":\"TEXT\",\"unit\":\"mm\",\"required\":true,\"exampleValue\":\"C1\",\"enumOptions\":\"C1,C2\",\"sortOrder\":3,\"status\":\"ACTIVE\"}")"
echo "${recreate_attr_response}"
assert_ok "${recreate_attr_response}"
attr_id="$(json_id <<< "${recreate_attr_response}")"

template_name="交付目录模板-${SUFFIX}"

echo "== create directory template =="
create_template_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/directory-templates" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"templateType\":\"DOCUMENT\",\"name\":\"${template_name}\",\"rootNodeJson\":\"{\\\"children\\\":[]}\",\"sourceType\":\"MANUAL\",\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${create_template_response}"
assert_ok "${create_template_response}"
template_id="$(json_id <<< "${create_template_response}")"

echo "== update directory template =="
update_template_response="$(curl -sS -X PATCH "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/directory-templates/${template_id}" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"templateType\":\"DOCUMENT\",\"name\":\"${template_name}-已编辑\",\"rootNodeJson\":\"{\\\"children\\\":[\\\"doc\\\"]}\",\"sourceType\":\"MANUAL\",\"sortOrder\":2,\"status\":\"ACTIVE\"}")"
echo "${update_template_response}"
assert_ok "${update_template_response}"

echo "== list directory templates =="
template_list_response="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/directory-templates" "${auth_header[@]}")"
echo "${template_list_response}"
assert_ok "${template_list_response}"
TARGET_ID="${template_id}" parse_json 'import json,sys,os; data=json.load(sys.stdin)["data"]; assert any(str(item["id"]) == os.environ["TARGET_ID"] for item in data), data' \
  <<< "${template_list_response}" >/dev/null

echo "== delete and recreate directory template with same name =="
delete_template_response="$(curl -sS -X DELETE "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/directory-templates/${template_id}" \
  "${auth_header[@]}")"
echo "${delete_template_response}"
assert_ok "${delete_template_response}"
recreate_template_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/directory-templates" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"templateType\":\"DOCUMENT\",\"name\":\"${template_name}\",\"rootNodeJson\":\"{\\\"children\\\":[\\\"final\\\"]}\",\"sourceType\":\"MANUAL\",\"sortOrder\":3,\"status\":\"ACTIVE\"}")"
echo "${recreate_template_response}"
assert_ok "${recreate_template_response}"
template_id="$(json_id <<< "${recreate_template_response}")"

echo "== final standard status =="
final_status_response="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/standard-status" "${auth_header[@]}")"
echo "${final_status_response}"
assert_ok "${final_status_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["deliverableStandardReady"] is True, data' \
  <<< "${final_status_response}" >/dev/null

echo "== regression: minimal chain =="
"$(dirname "$0")/check-minimal-chain.sh" "${BASE_URL}" "${USERNAME}" "${PASSWORD}" "${TARGET_PROJECT_ID}"

echo "== regression: master-data chain =="
"$(dirname "$0")/check-master-data-chain.sh" "${BASE_URL}" "${USERNAME}" "${PASSWORD}" "${TARGET_PROJECT_ID}" "REG-${SUFFIX}"

echo "deliverable-standard chain ok: definition=${definition_id}, type=${type_id}, attribute=${attr_id}, template=${template_id}"
