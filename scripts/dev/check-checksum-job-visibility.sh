#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-platform.admin}"
PASSWORD="${3:-Admin@123}"
TARGET_PROJECT_ID="${4:-2}"
SUFFIX="${SUFFIX:-$(date +%s)}"

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "OK", data' <<< "${response}" >/dev/null
}

wait_job_status() {
  local job_id="$1"
  local expected="$2"
  local response status
  for _ in $(seq 1 40); do
    response="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs/${job_id}" "${auth_header[@]}")"
    assert_ok "${response}"
    status="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["status"])' <<< "${response}")"
    if [[ "${status}" == "${expected}" ]]; then
      echo "${response}"
      return 0
    fi
    if [[ "${status}" == "FAILED" && "${expected}" != "FAILED" ]]; then
      echo "${response}"
      return 1
    fi
    sleep 1
  done
  response="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs/${job_id}" "${auth_header[@]}")"
  echo "${response}"
  return 1
}

echo "== login =="
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

tmp_dir="/tmp/delivery-checksum-${SUFFIX}"
success_file="${tmp_dir}/existing-${SUFFIX}.rvt"
missing_file="${tmp_dir}/missing-${SUFFIX}.rvt"
mkdir -p "${tmp_dir}"
printf 'checksum-visible-%s\n' "${SUFFIX}" > "${success_file}"

create_file_asset() {
  local name="$1"
  local storage_uri="$2"
  curl -sS -X POST "${BASE_URL}/api/data-steward/projects/${TARGET_PROJECT_ID}/file-resources" \
    "${auth_header[@]}" \
    -H 'Content-Type: application/json' \
    -d "{\"originalName\":\"${name}\",\"fileKind\":\"MODEL\",\"mimeType\":\"application/octet-stream\",\"sizeBytes\":128,\"storageUri\":\"${storage_uri}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}"
}

echo "== create success fixture asset =="
success_asset_response="$(create_file_asset "checksum成功验证-${SUFFIX}.rvt" "nas://${success_file}")"
assert_ok "${success_asset_response}"
success_file_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${success_asset_response}")"

echo "== create checksum job and verify it is visible =="
success_job_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/checksum-jobs" \
  "${auth_header[@]}" -H 'Content-Type: application/json' -d "{\"fileId\":${success_file_id}}")"
assert_ok "${success_job_response}"
success_job_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${success_job_response}")"

jobs_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs?jobType=CHECKSUM_CALC&projectId=${TARGET_PROJECT_ID}&limit=20" "${auth_header[@]}")"
assert_ok "${jobs_response}"
JOB_ID="${success_job_id}" parse_json '
import json, os, sys
job_id=int(os.environ["JOB_ID"])
jobs=json.load(sys.stdin)["data"]
assert any(j["id"] == job_id for j in jobs), jobs
print("success job visible")
' <<< "${jobs_response}"

echo "== wait success job =="
success_done="$(wait_job_status "${success_job_id}" "SUCCEEDED")"
assert_ok "${success_done}"
parse_json '
import json, sys
job=json.load(sys.stdin)["data"]
assert job["status"] == "SUCCEEDED", job
assert job["progressPercent"] == 100 or float(job["progressPercent"]) == 100.0, job
print("success job completed")
' <<< "${success_done}"

detail_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/files/${success_file_id}" "${auth_header[@]}")"
assert_ok "${detail_response}"
parse_json '
import json, re, sys
file=json.load(sys.stdin)["data"]
checksum=file.get("checksum")
assert checksum and re.fullmatch(r"[0-9a-f]{64}", checksum), file
print("checksum written back")
' <<< "${detail_response}"

echo "== create failure fixture asset =="
failure_asset_response="$(create_file_asset "checksum失败验证-${SUFFIX}.rvt" "nas://${missing_file}")"
assert_ok "${failure_asset_response}"
failure_file_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${failure_asset_response}")"

echo "== create failure checksum job =="
failure_job_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/checksum-jobs" \
  "${auth_header[@]}" -H 'Content-Type: application/json' -d "{\"fileId\":${failure_file_id}}")"
assert_ok "${failure_job_response}"
failure_job_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])' <<< "${failure_job_response}")"

echo "== wait failure job =="
failure_done="$(wait_job_status "${failure_job_id}" "FAILED")"
assert_ok "${failure_done}"
MISSING_FILE="${missing_file}" parse_json '
import json, sys
job=json.load(sys.stdin)["data"]
reason=job.get("failureReason") or ""
assert job["status"] == "FAILED", job
assert "底层路径已隐藏" in reason, job
assert "/tmp/" not in reason and "nas://" not in reason and "storagePath" not in str(job.get("requestPayload") or ""), job
print("failure reason sanitized")
' <<< "${failure_done}"

rm -rf "${tmp_dir}"
echo "checksum job visibility ok"
