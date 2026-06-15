#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-admin}"
PASSWORD="${3:-123456}"
TARGET_PROJECT_ID="${4:-2}"

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "OK", data' <<< "${response}" >/dev/null
}

login_response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")"
assert_ok "${login_response}"

access_token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${login_response}")"
current_project_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["currentProjectId"])' <<< "${login_response}")"

if [[ "${current_project_id}" != "${TARGET_PROJECT_ID}" ]]; then
  switch_response="$(curl -sS -X POST "${BASE_URL}/api/core/projects/${TARGET_PROJECT_ID}:switch" \
    -H "Authorization: Bearer ${access_token}")"
  assert_ok "${switch_response}"
  access_token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${switch_response}")"
fi

auth_header=(-H "Authorization: Bearer ${access_token}")
suffix="$(date +%s)"

create_file() {
  local name="$1"
  local kind="$2"
  local mime="$3"
  local storage_uri="$4"
  curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${TARGET_PROJECT_ID}/file-resources" \
    "${auth_header[@]}" \
    -H 'Content-Type: application/json' \
    -d "{\"originalName\":\"${name}\",\"fileKind\":\"${kind}\",\"mimeType\":\"${mime}\",\"sizeBytes\":2048,\"storageUri\":\"${storage_uri}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}"
}

echo "== create PDF sample =="
pdf_response="$(create_file "预览外壳验证-${suffix}.pdf" "DRAWING" "application/pdf" "minio://delivery/preview-${suffix}.pdf")"
assert_ok "${pdf_response}"
pdf_file_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${pdf_response}")"

echo "== PDF preview metadata =="
pdf_preview="$(curl -sS "${BASE_URL}/api/data-steward/assets/files/${pdf_file_id}/preview" "${auth_header[@]}")"
assert_ok "${pdf_preview}"
parse_json '
import json, sys
payload=json.load(sys.stdin)["data"]
assert payload["fileId"] is not None, payload
assert payload["previewStatus"] == "AVAILABLE", payload
assert payload["previewMode"] == "BROWSER_NATIVE", payload
assert payload["previewAvailable"] is True, payload
assert payload["conversionStatus"] == "NOT_REQUIRED", payload
assert "storagePath" not in payload, payload
print("pdf preview shell ok")
' <<< "${pdf_preview}"

echo "== create BIM sample =="
bim_response="$(create_file "预览外壳验证-${suffix}.rvt" "MODEL" "application/octet-stream" "minio://delivery/preview-${suffix}.rvt")"
assert_ok "${bim_response}"
bim_file_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${bim_response}")"

echo "== BIM preview metadata =="
bim_preview="$(curl -sS "${BASE_URL}/api/data-steward/assets/files/${bim_file_id}/preview" "${auth_header[@]}")"
assert_ok "${bim_preview}"
parse_json '
import json, sys
payload=json.load(sys.stdin)["data"]
assert payload["previewStatus"] == "NEEDS_CONVERSION", payload
assert payload["previewMode"] == "BIM_LIGHTWEIGHT", payload
assert payload["previewAvailable"] is False, payload
assert payload["conversionRequired"] is True, payload
assert payload["conversionStatus"] == "NOT_STARTED", payload
assert "storagePath" not in payload, payload
print("bim preview shell ok")
' <<< "${bim_preview}"

echo "file preview shell ok"
