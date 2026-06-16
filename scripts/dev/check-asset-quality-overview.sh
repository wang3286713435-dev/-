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

assert_not_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] != "OK", data' <<< "${response}" >/dev/null
}

assert_quality_issue_page() {
  local issue="$1"
  local response
  response="$(curl -sS "${BASE_URL}/api/data-steward/assets/files:page?assetSource=NAS_REAL*&qualityIssue=${issue}&pageSize=50" "${auth_header[@]}")"
  assert_ok "${response}"
  ISSUE="${issue}" parse_json '
import json, os, sys
issue=os.environ["ISSUE"]
payload=json.load(sys.stdin)["data"]
items=payload.get("items", [])
for item in items:
    if issue == "MISSING_CHECKSUM":
        assert not item.get("checksum"), item
    elif issue == "MISSING_CONFIDENCE":
        assert not item.get("confidenceLevel"), item
    elif issue == "MISSING_DISCIPLINE":
        assert item.get("discipline") in (None, "", "OTHER"), item
    elif issue == "MISSING_VERSION":
        assert not item.get("versionNo"), item
    elif issue == "MISSING_STORAGE_PATH":
        assert not item.get("storageUri"), item
    elif issue == "ZERO_SIZE_FILE":
        assert int(item.get("sizeBytes") or 0) <= 0, item
print("%s total=%s checked=%s" % (issue, payload.get("total"), len(items)))
' <<< "${response}"
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

echo "== current user menu includes quality page =="
me_response="$(curl -sS "${BASE_URL}/api/core/users/me" "${auth_header[@]}")"
assert_ok "${me_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; paths=[]; [paths.append(c["path"]) for m in data["menus"] for c in m.get("children", [])]; assert "/data-steward/quality" in paths, paths' \
  <<< "${me_response}" >/dev/null

echo "== quality overview for real NAS assets =="
quality_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/quality/overview?assetSource=NAS_REAL*" "${auth_header[@]}")"
assert_ok "${quality_response}"

parse_json '
import json, sys
payload=json.load(sys.stdin)["data"]
required_counts=[
  "riskSignalCount",
  "pendingReviewCount",
  "failedScanCount",
  "runningScanCount",
  "missingChecksumCount",
  "missingConfidenceCount",
  "missingDisciplineCount",
  "missingVersionCount",
  "missingStoragePathCount",
  "zeroSizeFileCount",
  "nonstandardPendingCount",
  "nonstandardApprovedCount",
]
for key in required_counts:
    assert isinstance(payload.get(key), int), (key, payload.get(key))
required_metrics={
  "PENDING_REVIEW",
  "FAILED_SCAN",
  "MISSING_STORAGE_PATH",
  "ZERO_SIZE_FILE",
  "MISSING_CHECKSUM",
  "MISSING_DISCIPLINE",
  "MISSING_VERSION",
  "MISSING_CONFIDENCE",
  "RUNNING_SCAN",
  "NONSTANDARD_PENDING",
  "NONSTANDARD_APPROVED",
}
codes={m.get("code") for m in payload.get("metrics", [])}
missing=required_metrics-codes
assert not missing, missing
assert isinstance(payload.get("topRiskProjects"), list), payload
assert isinstance(payload.get("recentEvents"), list), payload
print("riskSignalCount=%s" % payload["riskSignalCount"])
print("metrics=%s" % len(payload["metrics"]))
print("topRiskProjects=%s" % len(payload["topRiskProjects"]))
print("recentEvents=%s" % len(payload["recentEvents"]))
' <<< "${quality_response}"

echo "== project scoped quality overview =="
projects_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/projects?assetSource=NAS_REAL*" "${auth_header[@]}")"
assert_ok "${projects_response}"
project_id="$(parse_json '
import json, sys
projects=json.load(sys.stdin)["data"]
assert projects, "No real NAS projects found"
print(projects[0]["projectId"])
' <<< "${projects_response}")"

project_quality_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/quality/overview?assetSource=NAS_REAL*&projectId=${project_id}" "${auth_header[@]}")"
assert_ok "${project_quality_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert isinstance(data["riskSignalCount"], int), data; print("projectRiskSignalCount=%s" % data["riskSignalCount"])' \
  <<< "${project_quality_response}"

echo "== quality issue filters =="
assert_quality_issue_page "MISSING_CHECKSUM"
assert_quality_issue_page "MISSING_CONFIDENCE"
assert_quality_issue_page "MISSING_DISCIPLINE"
assert_quality_issue_page "MISSING_VERSION"
assert_quality_issue_page "MISSING_STORAGE_PATH"
assert_quality_issue_page "ZERO_SIZE_FILE"

echo "== unauthorized project quality is denied when 101 exists =="
project_101_id="$(parse_json '
import json, sys
projects=json.load(sys.stdin)["data"]
for project in projects:
    if project.get("code") == "101":
        print(project["projectId"])
        break
' <<< "${projects_response}")"

if [[ -n "${project_101_id}" ]]; then
  engineer_login_response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"delivery.engineer","password":"Engineer@123"}')"
  assert_ok "${engineer_login_response}"
  engineer_token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${engineer_login_response}")"
  engineer_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/quality/overview?assetSource=NAS_REAL*&projectId=${project_101_id}" \
    -H "Authorization: Bearer ${engineer_token}")"
  assert_not_ok "${engineer_response}"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "ASSET_PROJECT_ACCESS_DENIED", data; print("101 access denied OK")' \
    <<< "${engineer_response}"
else
  echo "Project 101 not found; skipped fixed unauthorized assertion"
fi

echo "asset quality overview ok"
