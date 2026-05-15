#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
USERNAME="${USERNAME:-platform.admin}"
PASSWORD="${PASSWORD:-Admin@123}"
SUFFIX="${SUFFIX:-$(date +%s)}"
TARGET_PROJECT_ID="${TARGET_PROJECT_ID:-1}"
NAS_DIR=""
run_output=""
project_id=""

cleanup() {
  if [[ -n "${NAS_DIR}" ]]; then
    rm -rf "${NAS_DIR}" 2>/dev/null || true
  fi
  if [[ -n "${run_output}" ]]; then
    rm -f "${run_output}" 2>/dev/null || true
  fi
  if [[ -n "${project_id}" ]] && command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -qx delivery-mysql; then
    docker exec -i -e MYSQL_PWD=root123 delivery-mysql mysql --protocol=TCP -h127.0.0.1 -uroot delivery_platform >/dev/null 2>&1 <<SQL || true
UPDATE core_projects
SET status = 'ARCHIVED', asset_status = 'ARCHIVED', deleted = 1, updated_by = 1, updated_at = CURRENT_TIMESTAMP
WHERE id = ${project_id} AND code LIKE 'SCANCTRL-%';
UPDATE core_user_project_roles
SET deleted = 1, updated_by = 1, updated_at = CURRENT_TIMESTAMP
WHERE project_id = ${project_id};
SQL
  fi
}

trap cleanup EXIT

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  parse_json 'import json,sys; obj=json.load(sys.stdin); assert obj["code"] == "OK", obj' <<< "${response}" >/dev/null
}

json_data() {
  parse_json "import json,sys; print(json.load(sys.stdin)$1)"
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

NAS_DIR="/tmp/scan-control-${SUFFIX}"
rm -rf "${NAS_DIR}"
mkdir -p "${NAS_DIR}/models" "${NAS_DIR}/临时文件" "${NAS_DIR}/customskip"

echo "== prepare local scan fixture =="
for i in $(seq 1 2500); do
  printf 'rvt-%s\n' "${i}" > "${NAS_DIR}/models/model_${i}.rvt"
done
printf 'skip me\n' > "${NAS_DIR}/临时文件/temp_model.rvt"
printf 'skip custom\n' > "${NAS_DIR}/customskip/custom_model.rvt"
: > "${NAS_DIR}/models/empty.rvt"

echo "== create project and mapping =="
project_code="SCANCTRL-${SUFFIX}"
project_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/projects" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"code\":\"${project_code}\",\"name\":\"扫描控制回归-${SUFFIX}\",\"industryType\":\"BUILDING_MEP\",\"projectStage\":\"INTERNAL_PILOT\",\"assetSource\":\"API\"}")"
assert_ok "${project_response}"
project_id="$(json_data '["data"]["projectId"]' <<< "${project_response}")"

mapping_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/path-mappings" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"projectId\":${project_id},\"nasPath\":\"${NAS_DIR}\",\"matchStrategy\":\"PREFIX\"}")"
assert_ok "${mapping_response}"

echo "== create scan with low-value directory skipping =="
scan_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootCode\":\"SCANCTRL-${SUFFIX}\",\"rootPath\":\"${NAS_DIR}\",\"projectId\":${project_id},\"projectCode\":\"${project_code}\",\"recursive\":true,\"extensions\":[\".rvt\"],\"skipLowValueDirectories\":true,\"skipDirectoryKeywords\":[\"customskip\"]}")"
assert_ok "${scan_response}"
scan_id="$(json_data '["data"]["id"]' <<< "${scan_response}")"

echo "== run scan in background and verify visible progress =="
run_output="/tmp/scan-control-run-${SUFFIX}.json"
curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${scan_id}:run" \
  "${auth_header[@]}" > "${run_output}" &
run_pid=$!

progress_seen=0
for _ in $(seq 1 20); do
  detail="$(curl -sS "${BASE_URL}/api/data-steward/assets/nas-scans/${scan_id}" "${auth_header[@]}")"
  assert_ok "${detail}"
  status="$(json_data '["data"]["status"]' <<< "${detail}")"
  progress_current="$(json_data '["data"]["progressCurrent"]' <<< "${detail}")"
  if [[ "${status}" == "RUNNING" && "${progress_current}" -gt 0 ]]; then
    progress_seen=1
    break
  fi
  sleep 0.2
done

if [[ "${progress_seen}" != "1" ]]; then
  echo "Expected RUNNING progress before cancellation"
  cat "${run_output}" 2>/dev/null || true
  exit 1
fi

echo "== cancel scan =="
cancel_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${scan_id}:cancel" "${auth_header[@]}")"
assert_ok "${cancel_response}"
wait "${run_pid}" || true
run_response="$(cat "${run_output}")"
assert_ok "${run_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["status"] == "CANCELED", data; assert data["cancelRequested"] is True, data' <<< "${run_response}" >/dev/null

echo "== resume scan =="
resume_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${scan_id}:resume" "${auth_header[@]}")"
assert_ok "${resume_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["status"] == "SUCCEEDED", data; assert data["totalScanned"] >= 2500, data; assert data["skippedDirectories"] >= 2, data; assert data["skippedLowValue"] >= 1, data; assert data["scanReportJson"], data' <<< "${resume_response}" >/dev/null

echo "== verify report endpoint and no duplicate file assets =="
report_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/nas-scans/${scan_id}/report" "${auth_header[@]}")"
assert_ok "${report_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["status"] == "SUCCEEDED", data; assert data["totalScanned"] >= 2500, data; assert data["skippedDirectories"] >= 2, data; assert data["scanReportJson"], data' <<< "${report_response}" >/dev/null

echo "== verify succeeded scans display as complete =="
scans_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/nas-scans" "${auth_header[@]}")"
assert_ok "${scans_response}"
parse_json 'import json,sys; tasks=json.load(sys.stdin)["data"]; bad=[t for t in tasks if t["status"]=="SUCCEEDED" and int(round(float(t.get("progressPercent") or 0))) != 100]; assert not bad, bad[:5]' <<< "${scans_response}" >/dev/null

files_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/files?projectId=${project_id}&fileKind=MODEL" "${auth_header[@]}")"
assert_ok "${files_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert len(data) == 200, f"API page cap should return first 200, got {len(data)}"' <<< "${files_response}" >/dev/null

db_count="$(docker exec -i delivery-mysql mysql --default-character-set=utf8mb4 -uroot -proot123 --batch --raw --skip-column-names delivery_platform -e "SELECT COUNT(*) FROM data_file_resources WHERE project_id=${project_id} AND deleted=0;")"
if [[ "${db_count}" != "2500" ]]; then
  echo "Expected exactly 2500 file assets, got ${db_count}"
  exit 1
fi

echo "scan task control regression passed"
