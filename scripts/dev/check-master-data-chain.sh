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

echo "== standard status before =="
status_before="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/standard-status" "${auth_header[@]}")"
echo "${status_before}"
assert_ok "${status_before}"

section_code="SMOKE-SEC-${SUFFIX}"
section_name="冒烟部位-${SUFFIX}"

echo "== create section node =="
create_section_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${section_code}\",\"name\":\"${section_name}\",\"sortOrder\":10,\"status\":\"ACTIVE\"}")"
echo "${create_section_response}"
assert_ok "${create_section_response}"
section_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${create_section_response}")"

echo "== update section node =="
update_section_response="$(curl -sS -X PATCH "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes/${section_id}" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${section_code}\",\"name\":\"${section_name}-已编辑\",\"sortOrder\":11,\"status\":\"ACTIVE\"}")"
echo "${update_section_response}"
assert_ok "${update_section_response}"

child_section_code="SMOKE-SEC-CHILD-${SUFFIX}"
child_section_name="冒烟子部位-${SUFFIX}"

echo "== create child section node =="
create_child_section_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"parentId\":${section_id},\"code\":\"${child_section_code}\",\"name\":\"${child_section_name}\",\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${create_child_section_response}"
assert_ok "${create_child_section_response}"
child_section_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${create_child_section_response}")"

echo "== disable child section node =="
delete_child_section_response="$(curl -sS -X DELETE "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes/${child_section_id}" \
  "${auth_header[@]}")"
echo "${delete_child_section_response}"
assert_ok "${delete_child_section_response}"

recycle_code="SMOKE-RECYCLE-${SUFFIX}"
recycle_name="冒烟回收测试-${SUFFIX}"

echo "== create recycle section node (first) =="
create_recycle_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${recycle_code}\",\"name\":\"${recycle_name}\",\"sortOrder\":0,\"status\":\"ACTIVE\"}")"
echo "${create_recycle_response}"
assert_ok "${create_recycle_response}"
recycle_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${create_recycle_response}")"

echo "== delete recycle section node (first) =="
delete_recycle_response="$(curl -sS -X DELETE "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes/${recycle_id}" \
  "${auth_header[@]}")"
echo "${delete_recycle_response}"
assert_ok "${delete_recycle_response}"

echo "== create recycle section node again (same code) =="
create_recycle2_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${recycle_code}\",\"name\":\"${recycle_name}\",\"sortOrder\":0,\"status\":\"ACTIVE\"}")"
echo "${create_recycle2_response}"
assert_ok "${create_recycle2_response}"
recycle_id2="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${create_recycle2_response}")"

echo "== delete recycle section node again (same code, second delete) =="
delete_recycle2_response="$(curl -sS -X DELETE "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes/${recycle_id2}" \
  "${auth_header[@]}")"
echo "${delete_recycle2_response}"
assert_ok "${delete_recycle2_response}"

echo "== section tree =="
tree_response="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes/tree" "${auth_header[@]}")"
echo "${tree_response}"
assert_ok "${tree_response}"

node_type_code="SMOKE-TYPE-${SUFFIX}"
node_type_name="冒烟节点类型-${SUFFIX}"

echo "== create node type =="
create_node_type_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/node-types" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${node_type_code}\",\"name\":\"${node_type_name}\",\"scopeLevel\":1,\"sortOrder\":10,\"status\":\"ACTIVE\"}")"
echo "${create_node_type_response}"
assert_ok "${create_node_type_response}"
node_type_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${create_node_type_response}")"

echo "== update node type =="
update_node_type_response="$(curl -sS -X PATCH "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/node-types/${node_type_id}" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"${node_type_code}\",\"name\":\"${node_type_name}-已编辑\",\"scopeLevel\":2,\"sortOrder\":11,\"status\":\"ACTIVE\"}")"
echo "${update_node_type_response}"
assert_ok "${update_node_type_response}"

echo "== node type list =="
node_type_list_response="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/node-types" "${auth_header[@]}")"
echo "${node_type_list_response}"
assert_ok "${node_type_list_response}"

echo "== lock node type =="
lock_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/node-types/${node_type_id}:lock" \
  "${auth_header[@]}")"
echo "${lock_response}"
assert_ok "${lock_response}"

echo "== node type lock status =="
lock_status_response="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/node-types/${node_type_id}/lock-status" \
  "${auth_header[@]}")"
echo "${lock_status_response}"
assert_ok "${lock_status_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["locked"] is True, data' <<< "${lock_status_response}" >/dev/null

echo "== standard status after =="
status_after="$(curl -sS "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/standard-status" "${auth_header[@]}")"
echo "${status_after}"
assert_ok "${status_after}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["hasSectionTree"] is True and data["hasNodeTypes"] is True, data' <<< "${status_after}" >/dev/null

echo "master-data chain ok: section=${section_id}, nodeType=${node_type_id}"
