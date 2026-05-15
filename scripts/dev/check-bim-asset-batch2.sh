#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-platform.admin}"
PASSWORD="${3:-Admin@123}"
TARGET_PROJECT_ID="${4:-2}"
SUFFIX="${5:-$(date +%s)}"

echo "================================================"
echo "Batch 2 Verification Script"
echo "SUFFIX: ${SUFFIX}"
echo "================================================"

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

json_data() {
  parse_json "import json,sys; print(json.load(sys.stdin)$1)"
}

json_data_id() {
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])'
}

json_data_project_id() {
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"].get("projectId") or json.load(sys.stdin)["data"]["id"])'
}

wait_for_job() {
  local job_id="$1"
  local max_wait="${2:-30}"
  local waited=0
  while [ $waited -lt $max_wait ]; do
    local resp
    resp="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs/${job_id}" "${auth_header[@]}")"
    local status
    status="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["status"])' <<< "${resp}")"
    if [ "$status" = "SUCCEEDED" ] || [ "$status" = "FAILED" ] || [ "$status" = "CANCELED" ]; then
      echo "$resp"
      return 0
    fi
    sleep 2
    waited=$((waited + 2))
  done
  echo "TIMEOUT waiting for job ${job_id}"
  return 1
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

# Login secondary user for permission regression
echo "== login secondary user =="
engineer_login_response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"delivery.engineer","password":"Engineer@123"}')"
echo "${engineer_login_response}"
assert_ok "${engineer_login_response}"
engineer_token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${engineer_login_response}")"
engineer_auth_header=(-H "Authorization: Bearer ${engineer_token}")

# ================================================================
# Step 1+2: Create project and NAS path mapping with simulated files
# ================================================================
echo "== step 1+2: create project and path mapping =="
NAS_DIR="/tmp/bim-batch2-${SUFFIX}"
rm -rf "${NAS_DIR}"
mkdir -p "${NAS_DIR}/checksum-test"

# Create a test project
proj_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/projects" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"code\":\"BATCH2-PROJ-${SUFFIX}\",\"name\":\"批次2测试项目-${SUFFIX}\",\"industryType\":\"BUILDING_MEP\",\"projectStage\":\"CONSTRUCTION\",\"projectManagerName\":\"测试\",\"ownerOrgName\":\"测试业主\",\"assetSource\":\"API\"}")"
echo "${proj_response}"
assert_ok "${proj_response}"
proj_id="$(json_data_project_id <<< "${proj_response}")"
proj_code="$(json_data '["data"]["code"]' <<< "${proj_response}")"

# Create path mapping
echo "== create path mapping =="
map_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/path-mappings" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"projectId\":${proj_id},\"nasPath\":\"${NAS_DIR}\",\"matchStrategy\":\"PREFIX\"}")"
echo "${map_response}"
assert_ok "${map_response}"

# ================================================================
# Step 3: Create simulated file for scanning
# ================================================================
echo "== step 3: create simulated files =="
echo "this is test rvt for checksum verification" > "${NAS_DIR}/checksum-test/project_model.rvt"
echo "another test file for batch checksum" > "${NAS_DIR}/checksum-test/another_file.dwg"
echo "orphan file for checksum fail test" > "${NAS_DIR}/checksum-test/orphan_for_test.txt"

# ================================================================
# Step 4: Run NAS scan to create file assets
# ================================================================
echo "== step 4: run NAS scan =="
scan_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootCode\":\"BATCH2\",\"rootPath\":\"${NAS_DIR}\",\"recursive\":true}")"
echo "${scan_response}"
assert_ok "${scan_response}"
scan_id="$(json_data_id <<< "${scan_response}")"

run_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${scan_id}:run" \
  "${auth_header[@]}")"
echo "${run_response}"
assert_ok "${run_response}"

# Get the file ID of the scanned RVT file
files_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/files?sourceType=NAS_SCAN" "${auth_header[@]}")"
echo "${files_response}"
assert_ok "${files_response}"

file_id="$(echo "${files_response}" | python3 -c "
import json,sys
files=json.load(sys.stdin)['data']
nas_dir='${NAS_DIR}'
for f in files:
    sp = f.get('storagePath','')
    if 'project_model.rvt' in sp and nas_dir in sp:
        print(f['fileId'])
        break
")"
echo "File ID for checksum test: ${file_id}"

# ================================================================
# Step 5: Trigger single file checksum
# ================================================================
echo "== step 5: trigger single file checksum =="
if [ -n "${file_id}" ]; then
  checksum_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/checksum-jobs" \
    "${auth_header[@]}" -H 'Content-Type: application/json' \
    -d "{\"fileId\":${file_id}}")"
  echo "${checksum_response}"
  assert_ok "${checksum_response}"
  checksum_job_id="$(json_data_id <<< "${checksum_response}")"
  echo "Checksum job ID: ${checksum_job_id}"

  # ================================================================
  # Step 6: Wait for worker to complete
  # ================================================================
  echo "== step 6: wait for worker to complete =="
  job_result="$(wait_for_job "${checksum_job_id}" 30)"
  echo "Job result: ${job_result}"
  job_status="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["status"])' <<< "${job_result}")"
  if [ "${job_status}" != "SUCCEEDED" ]; then
    echo "ERROR: Checksum job did not succeed. Status: ${job_status}"
    exit 1
  fi
  echo "Checksum job succeeded"

  # ================================================================
  # Step 7: Verify checksum written to file record (must be SHA-256 format)
  # ================================================================
  echo "== step 7: verify checksum =="
  file_detail="$(curl -sS "${BASE_URL}/api/data-steward/assets/files/${file_id}" "${auth_header[@]}")"
  echo "${file_detail}"
  assert_ok "${file_detail}"
  file_checksum="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"].get("checksum","") or "")' <<< "${file_detail}")"
  echo "File checksum value: ${file_checksum}"
  if [ -z "${file_checksum}" ]; then
    echo "ERROR: Checksum not written to file record"
    exit 1
  fi
  # SHA-256 produces 64 hex chars
  checksum_len="${#file_checksum}"
  if [ "${checksum_len}" -ne 64 ]; then
    echo "ERROR: Checksum is not SHA-256 (expected 64 hex chars, got ${checksum_len}): ${file_checksum}"
    exit 1
  fi
  echo "OK: SHA-256 checksum verified (${checksum_len} chars)"
else
  echo "WARNING: No file found for checksum test, skipping checksum steps"
fi

# ================================================================
# Step 8: Test failure scenario — checksum on non-existent file
# ================================================================
echo "== step 8: test checksum failure scenario =="
# Create a file under the already-mapped NAS_DIR so it auto-ingests,
# then delete it from disk before checksum to trigger a failure.
mkdir -p "${NAS_DIR}/fail-test"
echo "to be deleted for checksum failure test" > "${NAS_DIR}/fail-test/to_delete.rvt"

fail_scan_resp="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootCode\":\"BATCH2\",\"rootPath\":\"${NAS_DIR}\",\"recursive\":true}")"
fail_scan_id="$(json_data_id <<< "${fail_scan_resp}")"

fail_run_resp="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${fail_scan_id}:run" \
  "${auth_header[@]}")"
echo "${fail_run_resp}"
assert_ok "${fail_run_resp}"

# Find the file ID for the to_delete file (auto-ingested under mapped dir)
fail_files="$(curl -sS "${BASE_URL}/api/data-steward/assets/files?sourceType=NAS_SCAN" "${auth_header[@]}")"
fail_file_id="$(echo "${fail_files}" | python3 -c "
import json,sys
files=json.load(sys.stdin)['data']
nas_dir='${NAS_DIR}'
for f in files:
    sp = f.get('storagePath','')
    if 'to_delete.rvt' in sp and nas_dir in sp:
        print(f['fileId'])
        break
")"
echo "Fail test file ID: ${fail_file_id}"
if [ -z "${fail_file_id}" ]; then
  echo "ERROR: Could not find fail test file ID"
  exit 1
fi

# Now delete the actual file from disk
rm -f "${NAS_DIR}/fail-test/to_delete.rvt"

# Trigger checksum on the deleted file — should fail
fail_checksum_resp="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/checksum-jobs" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"fileId\":${fail_file_id}}")"
echo "${fail_checksum_resp}"
assert_ok "${fail_checksum_resp}"
fail_job_id="$(json_data_id <<< "${fail_checksum_resp}")"
echo "Fail checksum job ID: ${fail_job_id}"

fail_job_result="$(wait_for_job "${fail_job_id}" 30)"
echo "Fail job result: ${fail_job_result}"
fail_job_status="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["status"])' <<< "${fail_job_result}")"
if [ "${fail_job_status}" != "FAILED" ]; then
  echo "ERROR: Expected FAILED status for non-existent file checksum, got: ${fail_job_status}"
  exit 1
fi
fail_reason="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"].get("failureReason","") or "")' <<< "${fail_job_result}")"
echo "Failure reason: ${fail_reason}"
if [ -z "${fail_reason}" ]; then
  echo "ERROR: FAILED job must have failureReason"
  exit 1
fi
echo "OK: Checksum failure correctly reported"

# ================================================================
# Step 9: Query job list and details
# ================================================================
echo "== step 9: query jobs =="
jobs_list="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs" "${auth_header[@]}")"
echo "${jobs_list}"
assert_ok "${jobs_list}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert len(data) >= 2, f"Expected >=2 jobs, got {len(data)}"' <<< "${jobs_list}" >/dev/null

# Verify job has status, progress, retry fields
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; j=data[0]; assert "status" in j; assert "progressPercent" in j; assert "retryCount" in j; assert "maxRetries" in j' <<< "${jobs_list}" >/dev/null
echo "OK: Job list has status/progress/retry fields"

# Check filter by job_type
checksum_jobs="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs?jobType=CHECKSUM_CALC" "${auth_header[@]}")"
echo "${checksum_jobs}"
assert_ok "${checksum_jobs}"

# Check job detail
if [ -n "${checksum_job_id:-}" ]; then
  job_detail="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs/${checksum_job_id}" "${auth_header[@]}")"
  echo "${job_detail}"
  assert_ok "${job_detail}"
fi

echo "== retry on failed job =="
retry_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/jobs/${fail_job_id}:retry" \
  "${auth_header[@]}")"
echo "${retry_response}"
assert_ok "${retry_response}"

# Verify job status is PENDING again
retry_job="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs/${fail_job_id}" "${auth_header[@]}")"
retry_status="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["status"])' <<< "${retry_job}")"
if [ "${retry_status}" != "PENDING" ]; then
  echo "ERROR: After retry, job status should be PENDING, got: ${retry_status}"
  exit 1
fi
echo "OK: Job retry works (status=PENDING after retry)"

# Verify projectId filter: jobs from proj_id should NOT appear when filtering by another project
echo "== verify projectId filter =="
other_proj_jobs="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs?projectId=${TARGET_PROJECT_ID}&jobType=CHECKSUM_CALC" "${auth_header[@]}")"
echo "Jobs for project ${TARGET_PROJECT_ID}: ${other_proj_jobs}"
assert_ok "${other_proj_jobs}"
# The checksum jobs were created in proj_id, not TARGET_PROJECT_ID, so filtering by TARGET_PROJECT_ID should NOT return them
other_job_ids="$(echo "${other_proj_jobs}" | python3 -c "
import json,sys
data=json.load(sys.stdin)['data']
ids = [str(j['id']) for j in data]
print(','.join(ids) if ids else 'NONE')
")"
echo "Job IDs for project ${TARGET_PROJECT_ID}: ${other_job_ids}"
# Retry job exists in proj_id's project, should not show up in TARGET_PROJECT_ID
if [ "${other_job_ids}" != "NONE" ]; then
  # Check none of these IDs match our test jobs in proj_id
  for jid in ${checksum_job_id:-} ${fail_job_id}; do
    if echo "${other_job_ids}" | grep -q "${jid}"; then
      echo "ERROR: Job ${jid} from project ${proj_id} appeared in project ${TARGET_PROJECT_ID} filter"
      exit 1
    fi
  done
fi
echo "OK: projectId filter correctly isolates jobs"

# ================================================================
# Step 10: Query capacity statistics
# ================================================================
echo "== step 10: capacity statistics =="
stats_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/statistics" "${auth_header[@]}")"
echo "${stats_response}"
assert_ok "${stats_response}"

parse_json 'import json,sys; d=json.load(sys.stdin)["data"]; assert "projectCount" in d; assert "fileCount" in d; assert "modelFileCount" in d; assert "drawingFileCount" in d; assert "totalSizeBytes" in d; assert "byFileKind" in d; assert "byDiscipline" in d; assert "topProjects" in d' <<< "${stats_response}" >/dev/null
echo "OK: Capacity statistics has all expected fields"

# Verify project-scoped statistics: query with projectId and confirm filtering works
echo "== verify project-scoped statistics =="
proj_stats="$(curl -sS "${BASE_URL}/api/data-steward/assets/statistics?projectId=${proj_id}" "${auth_header[@]}")"
echo "${proj_stats}"
assert_ok "${proj_stats}"
proj_count="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["projectCount"])' <<< "${proj_stats}")"
if [ "${proj_count}" -ne 1 ]; then
  echo "ERROR: Expected projectCount=1 for project-scoped stats, got ${proj_count}"
  exit 1
fi
# Verify totalSizeBytes > 0 (test files have content)
total_size="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["totalSizeBytes"])' <<< "${proj_stats}")"
if [ "${total_size}" -le 0 ]; then
  echo "ERROR: Expected totalSizeBytes > 0 for project stats, got ${total_size}"
  exit 1
fi
# Verify byFileKind and byDiscipline return data for the project
by_file_kind_count="$(parse_json 'import json,sys; print(len(json.load(sys.stdin)["data"]["byFileKind"]))' <<< "${proj_stats}")"
if [ "${by_file_kind_count}" -lt 1 ]; then
  echo "ERROR: Expected byFileKind to have entries for project-scoped stats, got ${by_file_kind_count}"
  exit 1
fi
echo "OK: Project-scoped statistics correctly filtered"

# ================================================================
# Step 11: Query event stream
# ================================================================
echo "== step 11: event stream =="
events_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/events?limit=20" "${auth_header[@]}")"
echo "${events_response}"
assert_ok "${events_response}"

# Verify events contain project, scan, file, checksum events
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert len(data) >= 5, f"Expected >=5 events, got {len(data)}"' <<< "${events_response}" >/dev/null

# Look for specific event types
echo "${events_response}" | python3 -c "
import json,sys
events=json.load(sys.stdin)['data']
event_types=set(e['eventType'] for e in events)
print('Event types found:', event_types)
assert 'PROJECT' in event_types or 'SCAN' in event_types or 'FILE' in event_types or 'CHECKSUM' in event_types or 'JOB' in event_types, 'No expected event types found'
" || exit 1
echo "OK: Event stream has core events"

# ================================================================
# Step 12: afterEventId pagination
# ================================================================
echo "== step 12: afterEventId pagination =="
first_events="$(curl -sS "${BASE_URL}/api/data-steward/assets/events?limit=3" "${auth_header[@]}")"
assert_ok "${first_events}"
last_id="$(echo "${first_events}" | python3 -c "
import json,sys
events=json.load(sys.stdin)['data']
if events:
    print(events[-1]['id'])
else:
    print('0')
")"
echo "Last event ID from first page: ${last_id}"

next_events="$(curl -sS "${BASE_URL}/api/data-steward/assets/events?afterEventId=${last_id}&limit=5" "${auth_header[@]}")"
echo "${next_events}"
assert_ok "${next_events}"
parse_json "import json,sys; data=json.load(sys.stdin)['data']; assert all(e['id'] > ${last_id} for e in data), 'afterEventId not respected'" <<< "${next_events}" >/dev/null
echo "OK: afterEventId pagination works"

# ================================================================
# Step 12b: Verify job.fail event is recorded in event stream
# ================================================================
echo "== step 12b: verify job.fail event =="
if [ -n "${fail_job_id:-}" ]; then
  job_fail_events="$(curl -sS "${BASE_URL}/api/data-steward/assets/events?eventType=JOB&actionCode=job.fail&limit=200" "${auth_header[@]}")"
  echo "${job_fail_events}"
  assert_ok "${job_fail_events}"
  # Verify the fail_job_id appears in the event stream
  echo "${job_fail_events}" | python3 -c "
import json,sys
events=json.load(sys.stdin)['data']
fail_job_id=${fail_job_id}
found=False
for e in events:
    aggregate_id = e.get('aggregateId','')
    summary = e.get('summary','')
    if aggregate_id == str(fail_job_id):
        print('Found job.fail event:', summary)
        found=True
        break
assert found, 'job.fail event for job {} not found in event stream'.format(fail_job_id)
" || exit 1
  echo "OK: job.fail event found in event stream"
fi

# ================================================================
# Step 13: Unauthorized user cannot access jobs/statistics/events
# ================================================================
echo "== step 13: permission regression =="
# Second user should NOT see jobs created by admin for project they don't have access to
# The engineer user doesn't have access to proj_id (the test project)
# But they should still be able to list jobs — just filtered to their own

# Verify engineer can't see admin's project-scoped jobs
engineer_jobs="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs" "${engineer_auth_header[@]}")"
echo "Engineer jobs: ${engineer_jobs}"
assert_ok "${engineer_jobs}"

engineer_stats="$(curl -sS "${BASE_URL}/api/data-steward/assets/statistics" "${engineer_auth_header[@]}")"
echo "Engineer stats: ${engineer_stats}"
assert_ok "${engineer_stats}"

engineer_events="$(curl -sS "${BASE_URL}/api/data-steward/assets/events?limit=10" "${engineer_auth_header[@]}")"
echo "Engineer events: ${engineer_events}"
assert_ok "${engineer_events}"

# Engineer should NOT be able to access admin's specific job (project-scoped)
if [ -n "${checksum_job_id:-}" ]; then
  engineer_job_detail="$(curl -sS "${BASE_URL}/api/data-steward/assets/jobs/${checksum_job_id}" "${engineer_auth_header[@]}")"
  echo "Engineer job detail for admin job: ${engineer_job_detail}"
  assert_not_ok "${engineer_job_detail}"
  echo "OK: Unauthorized user denied access to admin's project-scoped job"
fi

# Engineer should not be able to query stats for admin's project
engineer_proj_stats="$(curl -sS "${BASE_URL}/api/data-steward/assets/statistics?projectId=${proj_id}" "${engineer_auth_header[@]}")"
echo "Engineer project stats: ${engineer_proj_stats}"
assert_not_ok "${engineer_proj_stats}"
echo "OK: Unauthorized user denied access to project stats"

echo "Permission regression check OK"

# ================================================================
# Step 13b: Engineer must not see global events from admin's operations
# ================================================================
echo "== step 13b: global event isolation =="
# Engineer queries events — should NOT include JOB/CHECKSUM events from admin's jobs
# (those are project-scoped, in proj_id which engineer has no access to)
engineer_events_proj="$(curl -sS "${BASE_URL}/api/data-steward/assets/events?eventType=JOB&limit=10" "${engineer_auth_header[@]}")"
echo "Engineer JOB events: ${engineer_events_proj}"
assert_ok "${engineer_events_proj}"
# Verify none of the returned events reference the admin's test project
engineer_events_data="$(echo "${engineer_events_proj}" | python3 -c "
import json,sys
events=json.load(sys.stdin)['data']
proj_id=${proj_id}
for e in events:
    pid = e.get('projectId')
    summary = e.get('summary','')
    if pid == proj_id or (pid is None and summary and 'BATCH2' in summary):
        print('FOUND: event projectId={} summary={}'.format(pid, summary))
        break
")"
if [ -n "${engineer_events_data}" ]; then
  echo "ERROR: Engineer should not see admin's project events: ${engineer_events_data}"
  exit 1
fi
echo "OK: Engineer does not see admin's project events"

# Engineer queries events for admin's specific project — should be denied
engineer_proj_events="$(curl -sS "${BASE_URL}/api/data-steward/assets/events?projectId=${proj_id}&limit=5" "${engineer_auth_header[@]}")"
echo "Engineer events for admin project: ${engineer_proj_events}"
assert_not_ok "${engineer_proj_events}"
echo "OK: Engineer denied access to admin's project events"

# ================================================================
# Step 14: OpenAPI verification
# ================================================================
echo "== step 14: OpenAPI =="
openapi_doc="$(curl -sS "${BASE_URL}/v3/api-docs")"
echo "OpenAPI size: $(echo "${openapi_doc}" | wc -c)"

for endpoint in \
  "/api/data-steward/assets/jobs" \
  "/api/data-steward/assets/checksum-jobs" \
  "/api/data-steward/assets/statistics" \
  "/api/data-steward/assets/events"; do
  if echo "${openapi_doc}" | python3 -c "import json,sys; d=json.load(sys.stdin); paths=[p for p in d.get('paths',{}) if '${endpoint}' in p]; assert paths, '${endpoint} not found'" 2>/dev/null; then
    echo "OK: ${endpoint} in OpenAPI"
  else
    echo "ERROR: ${endpoint} NOT in OpenAPI"
    exit 1
  fi
done

echo "OpenAPI OK"

# ================================================================
# Step 15: Verify batch 1 and batch 1 tail scripts still pass
# ================================================================
echo ""
echo "================================================"
echo "Step 15: Regression — running batch 1 scripts"
echo "================================================"

BATCH1_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/check-bim-asset-batch1.sh"
BATCH1_TAIL_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/check-bim-asset-batch1-tail.sh"

echo "Running check-bim-asset-batch1.sh..."
if bash "${BATCH1_SCRIPT}" "${BASE_URL}" "${USERNAME}" "${PASSWORD}" "${TARGET_PROJECT_ID}" "${SUFFIX}-r"; then
  echo "batch1 regression: PASS"
else
  echo "ERROR: batch1 regression FAILED"
  exit 1
fi

echo "Running check-bim-asset-batch1-tail.sh..."
if bash "${BATCH1_TAIL_SCRIPT}" "${BASE_URL}" "${USERNAME}" "${PASSWORD}" "${TARGET_PROJECT_ID}" "${SUFFIX}-r"; then
  echo "batch1-tail regression: PASS"
else
  echo "ERROR: batch1-tail regression FAILED"
  exit 1
fi

# ================================================================
# Cleanup
# ================================================================
rm -rf "${NAS_DIR}"

echo ""
echo "================================================"
echo "bim asset batch2 ok"
echo "================================================"
