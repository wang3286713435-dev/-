#!/usr/bin/env bash
# M3G-6T: 105 objectified files -> engineering tree draft -> delivery mapping smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
PROJECT_CODE="${PROJECT_CODE:-105}"

PASS=0
FAIL=0
TOKEN=""

pass() {
  echo "  [PASS] $1"
  PASS=$((PASS + 1))
}

fail() {
  echo "  [FAIL] $1"
  FAIL=$((FAIL + 1))
}

assert_ok() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
assert data.get("code") == "OK", data
assert data.get("traceId"), data
PY
}

assert_not_ok() {
  local response="$1"
  RESPONSE="${response}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
assert data.get("code") != "OK", data
assert data.get("traceId"), data
PY
}

assert_data() {
  local label="$1"
  local response="$2"
  local expr="$3"
  if RESPONSE="${response}" python3 - "${expr}" <<'PY'
import json
import os
import sys
payload = json.loads(os.environ["RESPONSE"])
data = payload.get("data")
assert eval(sys.argv[1], {}, {"data": data, "payload": payload}), payload
PY
  then
    pass "${label}"
  else
    fail "${label}"
  fi
}

assert_no_forbidden() {
  local label="$1"
  local payload="$2"
  if PAYLOAD="${payload}" python3 - <<'PY'
import os
import re
payload = os.environ["PAYLOAD"]
patterns = [
    r"/Volumes/",
    r"smb://",
    r"nas://",
    r"storage_uri",
    r"storage_path",
    r"object_key",
    r'"bucket"\s*:',
    r"raw db row",
    r"\bselect\s+.+\s+from\b",
    r"\binsert\s+into\b",
    r"\bupdate\s+.+\s+set\b",
    r"\bdelete\s+from\b",
    r"token[^A-Za-z0-9_]",
    r"secret",
    r"password",
]
lower = payload.lower()
for pattern in patterns:
    if re.search(pattern, lower, flags=re.IGNORECASE | re.DOTALL):
        raise AssertionError(pattern)
PY
  then
    pass "${label}"
  else
    fail "${label}"
  fi
}

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" python3 - "${expr}" <<'PY'
import json
import os
import sys
payload = json.loads(os.environ["RESPONSE"])
data = payload.get("data")
value = eval(sys.argv[1], {}, {"data": data, "payload": payload})
if isinstance(value, (dict, list)):
    print(json.dumps(value, ensure_ascii=False))
elif value is None:
    print("")
else:
    print(value)
PY
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 60 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="$2"
  curl -sS --connect-timeout 3 --max-time 80 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

api_delete() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 30 -X DELETE "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

echo "=== M3G-6T: 105 engineering tree delivery mapping ==="

echo ""
echo "--- 1. Login and switch project ${PROJECT_ID}/${PROJECT_CODE} ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_data_expr "${login_response}" "data['accessToken']")"
pass "管理员登录成功"

switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch" '{}')"
assert_ok "${switch_response}"
TOKEN="$(json_data_expr "${switch_response}" "data['accessToken']")"
pass "已切换到 105 真实项目"

echo ""
echo "--- 2. 105 baseline objectification and ownership state ---"
coverage_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/coverage")"
storage_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-objectification-long-run")"
tree_before="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/tree")"
for payload in "${coverage_response}" "${storage_response}" "${tree_before}"; do
  assert_ok "${payload}"
  assert_no_forbidden "baseline response has no forbidden fields" "${payload}"
done
assert_data "105 ownership is fully covered" "${coverage_response}" "data['projectId'] == ${PROJECT_ID} and data['totalFiles'] == 2928 and data['assignedFiles'] == data['totalFiles'] and data['unassignedFiles'] == 0"
assert_data "105 objectification is complete" "${storage_response}" "data['projectId'] == ${PROJECT_ID} and data['totalFileCount'] == 2928 and data['objectStoredCount'] == 2928 and float(data['checksumCoverageRate']) == 100.0"

echo ""
echo "--- 3. Engineering tree draft is generated but does not overwrite formal tree ---"
tree_draft_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/tree-draft")"
tree_after_preview="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/tree")"
tree_apply_without_confirm="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-ownership/tree-draft:apply" '{"confirmed":false}')"
tree_apply_confirmed="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-ownership/tree-draft:apply" '{"confirmed":true}')"
for payload in "${tree_draft_response}" "${tree_after_preview}" "${tree_apply_confirmed}"; do
  assert_ok "${payload}"
  assert_no_forbidden "tree draft response has no forbidden fields" "${payload}"
done
assert_not_ok "${tree_apply_without_confirm}"
assert_no_forbidden "tree draft rejection has no forbidden fields" "${tree_apply_without_confirm}"
assert_data "tree draft has required node metrics" "${tree_draft_response}" "data['draftOnly'] is True and data['formalTreeOverwritten'] is False and data['nodeCount'] >= 12 and all({'fileCount','drawingCount','modelCount','confirmedOwnershipCount','pendingReviewCount','formalDeliveryCandidateCount','currentMissingDeliverableCount','recommendationReason','riskHints'} <= set(row.keys()) for row in data['nodes'])"
assert_data "tree preview did not mutate ownership totals" "${tree_after_preview}" "data['totalFiles'] == 2928 and data['assignedFiles'] == 2928 and data['unassignedFiles'] == 0"
assert_data "tree draft apply is explicit non-overwrite acknowledgement" "${tree_apply_confirmed}" "data['confirmed'] is True and data['formalTreeOverwritten'] is False and data['assignmentUpdatedCount'] == 0"

echo ""
echo "--- 4. File-manager jump fields are still present and safe ---"
catalog_response="$(api_get "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&page=1&pageSize=20&directOnly=true")"
assert_ok "${catalog_response}"
assert_no_forbidden "catalog files response has no forbidden fields" "${catalog_response}"
assert_data "catalog files expose ownership jump fields" "${catalog_response}" "data['total'] > 0 and all('ownershipNodePath' in row and 'ownershipNodeLabel' in row and 'ownershipType' in row and 'ownershipStatus' in row for row in data['items'])"

echo ""
echo "--- 5. Model/drawing gap analysis is catalog-only and includes both gap directions ---"
gap_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/file-ownership/model-drawing-gap")"
assert_ok "${gap_response}"
assert_no_forbidden "model drawing gap response has no forbidden fields" "${gap_response}"
assert_data "gap analysis declares catalog-only boundary" "${gap_response}" "data['evidenceMode'] == 'catalog_metadata_only' and '不是 BIM 构件级解析' in data['analysisBoundary']"
assert_data "gap analysis lists drawing-missing-model" "${gap_response}" "data['drawingMissingModelCount'] > 0 and any(row['gapStatus'] == 'DRAWING_MISSING_MODEL' for row in data['rows'])"
assert_data "gap analysis lists model-missing-drawing" "${gap_response}" "data['modelMissingDrawingCount'] > 0 and any(row['gapStatus'] == 'MODEL_MISSING_DRAWING' for row in data['rows'])"

echo ""
echo "--- 6. Delivery candidates are dry-run until confirmed ---"
bindings_before="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-views?viewType=DOCUMENT")"
candidates_response="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-candidates?targetType=SECTION")"
bindings_after_preview="$(api_get "/api/work-center/projects/${PROJECT_ID}/delivery-views?viewType=DOCUMENT")"
apply_candidates_without_confirm="$(api_post "/api/work-center/projects/${PROJECT_ID}/delivery-candidates:apply" '{"confirmed":false,"viewType":"DOCUMENT","targetType":"SECTION","items":[]}')"
for payload in "${bindings_before}" "${candidates_response}" "${bindings_after_preview}"; do
  assert_ok "${payload}"
  assert_no_forbidden "delivery candidate response has no forbidden fields" "${payload}"
done
assert_not_ok "${apply_candidates_without_confirm}"
assert_no_forbidden "delivery candidate rejection has no forbidden fields" "${apply_candidates_without_confirm}"
assert_data "delivery candidates are preview only" "${candidates_response}" "data['dryRun'] is True and data['bindingCreated'] is False and data['missingCount'] > 0 and data['candidateCount'] > 0"
DOC_BEFORE="$(json_data_expr "${bindings_before}" "data['totalCount']")"
DOC_AFTER="$(json_data_expr "${bindings_after_preview}" "data['totalCount']")"
if [[ "${DOC_BEFORE}" == "${DOC_AFTER}" ]]; then
  pass "delivery candidates preview did not auto-bind"
else
  fail "delivery candidates preview did not auto-bind"
fi

first_candidate_payload="$(RESPONSE="${candidates_response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
rows = payload["data"]["rows"]
if not rows:
    print("")
else:
    row = rows[0]
    body = {
        "confirmed": True,
        "viewType": row["viewType"],
        "targetType": row["targetType"],
        "items": [{
            "recommendationId": row["recommendationId"],
            "missingItemKey": row["missingItemKey"],
            "targetType": row["targetType"],
            "targetId": row["targetId"],
            "deliverableTypeId": row["deliverableTypeId"],
            "fileResourceId": row["fileResourceId"],
        }],
    }
    print(json.dumps(body, ensure_ascii=False))
PY
)"
if [[ -n "${first_candidate_payload}" ]]; then
  apply_candidates_confirmed="$(api_post "/api/work-center/projects/${PROJECT_ID}/delivery-candidates:apply" "${first_candidate_payload}")"
  assert_ok "${apply_candidates_confirmed}"
  assert_no_forbidden "confirmed delivery candidate apply has no forbidden fields" "${apply_candidates_confirmed}"
  assert_data "confirmed delivery candidate apply uses batch binding guardrails" "${apply_candidates_confirmed}" "data['requestedCount'] == 1 and data['failedCount'] == 0 and data['createdCount'] + data['skippedCount'] >= 1"
  created_binding_ids="$(RESPONSE="${apply_candidates_confirmed}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
ids = []
for row in payload.get("data", {}).get("results", []) or []:
    if row.get("status") == "CREATED" and row.get("bindingId"):
        ids.append(str(row["bindingId"]))
print(" ".join(ids))
PY
)"
  if [[ -n "${created_binding_ids}" ]]; then
    for binding_id in ${created_binding_ids}; do
      cleanup_response="$(api_delete "/api/work-center/projects/${PROJECT_ID}/delivery-bindings/${binding_id}")"
      assert_ok "${cleanup_response}"
      assert_no_forbidden "confirmed delivery candidate cleanup has no forbidden fields" "${cleanup_response}"
    done
    pass "confirmed delivery candidate apply cleaned up script-created bindings"
  else
    pass "confirmed delivery candidate apply reused existing binding without cleanup"
  fi
else
  fail "confirmed delivery candidate apply has a candidate to test"
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
