#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-platform.admin}"
PASSWORD="${3:-Admin@123}"
TARGET_PROJECT_ID="${4:-2}"

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
assert_ok "${login_response}"

access_token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${login_response}")"
current_project_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["currentProjectId"])' <<< "${login_response}")"

if [[ "${current_project_id}" != "${TARGET_PROJECT_ID}" ]]; then
  echo "== switch project =="
  switch_response="$(curl -sS -X POST "${BASE_URL}/api/core/projects/${TARGET_PROJECT_ID}:switch" \
    -H "Authorization: Bearer ${access_token}")"
  assert_ok "${switch_response}"
  access_token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${switch_response}")"
fi

auth_header=(-H "Authorization: Bearer ${access_token}")
suffix="$(date +%s)"
file_name="治理补录验证-${suffix}.pdf"
storage_uri="minio://delivery/governance-${suffix}.pdf"

echo "== create synthetic file resource =="
create_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${TARGET_PROJECT_ID}/file-resources" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"originalName\":\"${file_name}\",\"fileKind\":\"DRAWING\",\"mimeType\":\"application/pdf\",\"sizeBytes\":1024,\"storageUri\":\"${storage_uri}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
assert_ok "${create_response}"
file_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${create_response}")"

echo "== file appears in paged asset list =="
page_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/files:page?projectId=${TARGET_PROJECT_ID}&keyword=${suffix}&pageSize=20" "${auth_header[@]}")"
assert_ok "${page_response}"
parse_json "import json,sys; data=json.load(sys.stdin)['data']; items=data['items']; assert any(str(item['fileId']) == '${file_id}' for item in items), data; print('matched file ${file_id}')" \
  <<< "${page_response}"

echo "== update file governance metadata =="
update_response="$(curl -sS -X PATCH "${BASE_URL}/api/data-steward/assets/files/${file_id}/metadata" \
  "${auth_header[@]}" \
  -H 'Content-Type: application/json' \
  -d '{"discipline":"HVAC","versionNo":"V2","confidenceLevel":"HIGH","reviewStatus":"APPROVED"}')"
assert_ok "${update_response}"
parse_json '
import json, sys
file=json.load(sys.stdin)["data"]
assert file["discipline"] == "HVAC", file
assert file["versionNo"] == "V2", file
assert file["confidenceLevel"] == "HIGH", file
assert file["reviewStatus"] == "APPROVED", file
print("metadata updated")
' <<< "${update_response}"

echo "== file detail reflects governance metadata =="
detail_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/files/${file_id}" "${auth_header[@]}")"
assert_ok "${detail_response}"
parse_json '
import json, sys
file=json.load(sys.stdin)["data"]
assert file["discipline"] == "HVAC", file
assert file["versionNo"] == "V2", file
assert file["confidenceLevel"] == "HIGH", file
assert file["reviewStatus"] == "APPROVED", file
assert file["storagePath"], file
print("detail ok")
' <<< "${detail_response}"

echo "== metadata update event exists =="
events_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/events?projectId=${TARGET_PROJECT_ID}&eventType=FILE&actionCode=file.metadata.update&limit=20" "${auth_header[@]}")"
assert_ok "${events_response}"
parse_json "import json,sys; events=json.load(sys.stdin)['data']; assert any(e.get('aggregateId') == '${file_id}' for e in events), events; print('event ok')" \
  <<< "${events_response}"

echo "asset detail governance ok"
