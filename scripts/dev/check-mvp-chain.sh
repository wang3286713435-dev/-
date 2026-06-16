#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-admin}"
PASSWORD="${3:-123456}"
TARGET_PROJECT_ID="${4:-2}"
SUFFIX="${5:-$(date +%s)}"

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "OK", data' <<< "${response}" >/dev/null
}

json_data_id() {
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

echo "== menu includes mvp modules =="
me_response="$(curl -sS "${BASE_URL}/api/core/users/me" "${auth_header[@]}")"
echo "${me_response}"
assert_ok "${me_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; paths=[]; [paths.append(c["path"]) for m in data["menus"] for c in m.get("children", [])]; required={"/data-steward/files","/data-steward/models","/data-steward/objects","/work/document-delivery","/work/drawing-delivery","/work/dashboard","/visualization/workbench"}; missing=required-set(paths); assert not missing, missing' \
  <<< "${me_response}" >/dev/null

echo "== create section node =="
section_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/section-nodes" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"parentId\":null,\"code\":\"MVP-SEC-${SUFFIX}\",\"name\":\"MVP 样板部位 ${SUFFIX}\",\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${section_response}"
assert_ok "${section_response}"
section_id="$(json_data_id <<< "${section_response}")"

echo "== create and lock node type =="
node_type_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/node-types" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\"MVP-NT-${SUFFIX}\",\"name\":\"MVP 节点类型 ${SUFFIX}\",\"scopeLevel\":1,\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${node_type_response}"
assert_ok "${node_type_response}"
node_type_id="$(json_data_id <<< "${node_type_response}")"

lock_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/node-types:lock" "${auth_header[@]}")"
echo "${lock_response}"
assert_ok "${lock_response}"

echo "== create deliverable standard =="
definition_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-definitions" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"nodeTypeId\":${node_type_id},\"code\":\"MVP-DEF-${SUFFIX}\",\"name\":\"MVP 交付物定义 ${SUFFIX}\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${definition_response}"
assert_ok "${definition_response}"
definition_id="$(json_data_id <<< "${definition_response}")"

doc_type_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-types" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deliverableDefinitionId\":${definition_id},\"code\":\"MVP-DOC-TYPE-${SUFFIX}\",\"name\":\"MVP 文档类型 ${SUFFIX}\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${doc_type_response}"
assert_ok "${doc_type_response}"
doc_type_id="$(json_data_id <<< "${doc_type_response}")"

drawing_type_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-types" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deliverableDefinitionId\":${definition_id},\"code\":\"MVP-DWG-TYPE-${SUFFIX}\",\"name\":\"MVP 图纸类型 ${SUFFIX}\",\"fileKind\":\"DRAWING\",\"bindingStrategy\":\"MANAGED_OBJECT\",\"sortOrder\":2,\"status\":\"ACTIVE\"}")"
echo "${drawing_type_response}"
assert_ok "${drawing_type_response}"
drawing_type_id="$(json_data_id <<< "${drawing_type_response}")"

attr_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/deliverable-attributes" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"deliverableTypeId\":${doc_type_id},\"code\":\"MVP-ATTR-${SUFFIX}\",\"name\":\"MVP 属性 ${SUFFIX}\",\"valueType\":\"TEXT\",\"unit\":\"\",\"required\":true,\"exampleValue\":\"A1\",\"enumOptions\":\"\",\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${attr_response}"
assert_ok "${attr_response}"

template_response="$(curl -sS -X POST "${BASE_URL}/api/master-data/projects/${TARGET_PROJECT_ID}/directory-templates" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"templateType\":\"DOCUMENT\",\"name\":\"MVP 目录模板 ${SUFFIX}\",\"rootNodeJson\":\"{\\\"children\\\":[\\\"MVP\\\"]}\",\"sourceType\":\"MANUAL\",\"sortOrder\":1,\"status\":\"ACTIVE\"}")"
echo "${template_response}"
assert_ok "${template_response}"

echo "== create processed file resources =="
doc_file_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${TARGET_PROJECT_ID}/file-resources" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"originalName\":\"MVP 文档 ${SUFFIX}.pdf\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":1024,\"storageUri\":\"minio://delivery/mvp-doc-${SUFFIX}.pdf\",\"businessTag\":\"mvp\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
echo "${doc_file_response}"
assert_ok "${doc_file_response}"
doc_file_id="$(json_data_id <<< "${doc_file_response}")"

drawing_file_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${TARGET_PROJECT_ID}/file-resources" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"originalName\":\"MVP 图纸 ${SUFFIX}.dwg\",\"fileKind\":\"DRAWING\",\"mimeType\":\"application/acad\",\"sizeBytes\":2048,\"storageUri\":\"minio://delivery/mvp-drawing-${SUFFIX}.dwg\",\"businessTag\":\"mvp\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
echo "${drawing_file_response}"
assert_ok "${drawing_file_response}"
drawing_file_id="$(json_data_id <<< "${drawing_file_response}")"

model_file_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${TARGET_PROJECT_ID}/file-resources" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"originalName\":\"MVP 模型 ${SUFFIX}.rvt\",\"fileKind\":\"MODEL\",\"mimeType\":\"application/octet-stream\",\"sizeBytes\":4096,\"storageUri\":\"minio://delivery/mvp-model-${SUFFIX}.rvt\",\"businessTag\":\"mvp\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
echo "${model_file_response}"
assert_ok "${model_file_response}"
model_file_id="$(json_data_id <<< "${model_file_response}")"

echo "== create and publish model integration =="
model_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${TARGET_PROJECT_ID}/model-integrations" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"MVP 模型集成 ${SUFFIX}\",\"modelFileId\":${model_file_id},\"versionNo\":\"V1\",\"componentCount\":8,\"adapterPayloadJson\":\"{\\\"adapter\\\":\\\"mock-bim\\\"}\"}")"
echo "${model_response}"
assert_ok "${model_response}"
model_id="$(json_data_id <<< "${model_response}")"

publish_response="$(curl -sS -X PATCH "${BASE_URL}/api/data-steward/projects/${TARGET_PROJECT_ID}/model-integrations/${model_id}:publish" \
  "${auth_header[@]}")"
echo "${publish_response}"
assert_ok "${publish_response}"
parse_json 'import json,sys; assert json.load(sys.stdin)["data"]["status"] == "PUBLISHED"' <<< "${publish_response}" >/dev/null

echo "== create managed object =="
object_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${TARGET_PROJECT_ID}/managed-objects" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"modelIntegrationId\":${model_id},\"sectionNodeId\":${section_id},\"code\":\"MVP-OBJ-${SUFFIX}\",\"name\":\"MVP 管理对象 ${SUFFIX}\",\"objectType\":\"EQUIPMENT\",\"externalId\":\"EXT-${SUFFIX}\",\"discipline\":\"MEP\",\"status\":\"ACTIVE\",\"propertiesJson\":\"{\\\"level\\\":\\\"B1\\\"}\"}")"
echo "${object_response}"
assert_ok "${object_response}"
object_id="$(json_data_id <<< "${object_response}")"

echo "== bind document and drawing delivery views =="
doc_binding_response="$(curl -sS -X POST "${BASE_URL}/api/work-center/projects/${TARGET_PROJECT_ID}/delivery-bindings" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":${section_id},\"managedObjectId\":null,\"deliverableTypeId\":${doc_type_id},\"fileResourceId\":${doc_file_id},\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"PENDING\",\"sortOrder\":1,\"remark\":\"mvp document\"}")"
echo "${doc_binding_response}"
assert_ok "${doc_binding_response}"

drawing_binding_response="$(curl -sS -X POST "${BASE_URL}/api/work-center/projects/${TARGET_PROJECT_ID}/delivery-bindings" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"viewType\":\"DRAWING\",\"sectionNodeId\":null,\"managedObjectId\":${object_id},\"deliverableTypeId\":${drawing_type_id},\"fileResourceId\":${drawing_file_id},\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"PENDING\",\"sortOrder\":1,\"remark\":\"mvp drawing\"}")"
echo "${drawing_binding_response}"
assert_ok "${drawing_binding_response}"

echo "== delivery views =="
doc_view_response="$(curl -sS "${BASE_URL}/api/work-center/projects/${TARGET_PROJECT_ID}/delivery-views?viewType=DOCUMENT" "${auth_header[@]}")"
echo "${doc_view_response}"
assert_ok "${doc_view_response}"
TARGET_FILE_ID="${doc_file_id}" parse_json 'import json,sys,os; rows=json.load(sys.stdin)["data"]["rows"]; assert any(str(r["fileResourceId"]) == os.environ["TARGET_FILE_ID"] for r in rows), rows' \
  <<< "${doc_view_response}" >/dev/null

drawing_view_response="$(curl -sS "${BASE_URL}/api/work-center/projects/${TARGET_PROJECT_ID}/delivery-views?viewType=DRAWING" "${auth_header[@]}")"
echo "${drawing_view_response}"
assert_ok "${drawing_view_response}"
TARGET_FILE_ID="${drawing_file_id}" parse_json 'import json,sys,os; rows=json.load(sys.stdin)["data"]["rows"]; assert any(str(r["fileResourceId"]) == os.environ["TARGET_FILE_ID"] for r in rows), rows' \
  <<< "${drawing_view_response}" >/dev/null

echo "== dashboard and visualization context =="
dashboard_response="$(curl -sS "${BASE_URL}/api/work-center/projects/${TARGET_PROJECT_ID}/dashboard/summary" "${auth_header[@]}")"
echo "${dashboard_response}"
assert_ok "${dashboard_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["publishedModelCount"] >= 1 and data["managedObjectCount"] >= 1, data' \
  <<< "${dashboard_response}" >/dev/null

context_response="$(curl -sS "${BASE_URL}/api/visualization-adapter/projects/${TARGET_PROJECT_ID}/context" "${auth_header[@]}")"
echo "${context_response}"
assert_ok "${context_response}"
TARGET_OBJECT_ID="${object_id}" parse_json 'import json,sys,os; data=json.load(sys.stdin)["data"]; assert any(str(o["id"]) == os.environ["TARGET_OBJECT_ID"] for o in data["objects"]), data' \
  <<< "${context_response}" >/dev/null

locate_response="$(curl -sS -X POST "${BASE_URL}/api/visualization-adapter/projects/${TARGET_PROJECT_ID}/managed-objects/${object_id}:locate" "${auth_header[@]}")"
echo "${locate_response}"
assert_ok "${locate_response}"

highlight_response="$(curl -sS -X POST "${BASE_URL}/api/visualization-adapter/projects/${TARGET_PROJECT_ID}/managed-objects/${object_id}:highlight" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d '{"color":"#2563eb","durationSeconds":5}')"
echo "${highlight_response}"
assert_ok "${highlight_response}"

inject_response="$(curl -sS -X POST "${BASE_URL}/api/visualization-adapter/projects/${TARGET_PROJECT_ID}/context:inject" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"sectionNodeId\":${section_id},\"managedObjectId\":${object_id},\"source\":\"MVP_SCRIPT\"}")"
echo "${inject_response}"
assert_ok "${inject_response}"

echo "== audit logs =="
audit_response="$(curl -sS "${BASE_URL}/api/core/projects/${TARGET_PROJECT_ID}/audit-logs?moduleCode=work-center&limit=20" "${auth_header[@]}")"
echo "${audit_response}"
assert_ok "${audit_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert len(data) >= 1, data' <<< "${audit_response}" >/dev/null

echo "mvp chain ok"
