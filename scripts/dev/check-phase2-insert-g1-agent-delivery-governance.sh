#!/usr/bin/env bash
# Phase 2 Insert G1: Agent guided delivery governance MVP acceptance check.
# This script creates metadata-only fixtures through existing APIs. It does not
# read file bodies, create packages, move NAS files, or write Hermes memory.

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
RUN_CODE="PHASE2-G1-${RUN_ID}"

PASS=0
FAIL=0
TOKEN=""
PID=""
PNAME=""
BASELINE_G1_COUNT=""
FINAL_G1_COUNT=""
CLEANUP_DONE=0

SEC_ID=""
NT_ID=""
NT_CREATED=0
DD_ID=""
DT_DOC_ID=""
DT_DWG_ID=""
ATTR_ID=""
TMPL_ID=""
DOC_FILE_ID=""
DWG_FILE_ID=""
BINDING_IDS=""

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

json_get() {
  local path="$1"
  python3 -c '
import json, sys

path = sys.argv[1]
try:
    payload = json.load(sys.stdin)
except Exception:
    print("")
    raise SystemExit

parts = path.split(".")
if isinstance(payload, dict) and parts and parts[0] not in payload and "data" in payload:
    value = payload["data"]
else:
    value = payload
for part in parts:
    if not part:
        continue
    if isinstance(value, dict):
        value = value.get(part)
    elif isinstance(value, list) and part.isdigit() and int(part) < len(value):
        value = value[int(part)]
    else:
        value = None
    if value is None:
        break

if value is None:
    print("")
elif isinstance(value, bool):
    print("true" if value else "false")
else:
    print(value)
' "$path"
}

api_get() {
  local url="$1"
  curl -s --connect-timeout 3 --max-time 20 \
    -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_URL}${url}"
}

api_post() {
  local url="$1" body="$2"
  curl -s --connect-timeout 3 --max-time 20 -X POST \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}" \
    "${BASE_URL}${url}"
}

api_delete() {
  local url="$1"
  curl -s --connect-timeout 3 --max-time 20 -X DELETE \
    -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_URL}${url}"
}

count_section_matches() {
  python3 -c '
import json, sys

try:
    payload = json.load(sys.stdin)
except Exception:
    print(0)
    raise SystemExit
rows = payload.get("data", [])

def walk(nodes):
    total = 0
    for row in nodes or []:
        text = str(row.get("code", "")) + " " + str(row.get("name", ""))
        if "PHASE2-G1-" in text:
            total += 1
        total += walk(row.get("children", []))
    return total

print(walk(rows))
'
}

count_list_matches() {
  python3 -c '
import json, sys

try:
    payload = json.load(sys.stdin)
except Exception:
    print(0)
    raise SystemExit
rows = payload.get("data", [])
if isinstance(rows, dict) and "items" in rows:
    rows = rows.get("items", [])
count = 0
for row in rows or []:
    text = " ".join(str(row.get(key, "")) for key in ("code", "name", "originalName", "fileName", "remark"))
    if "PHASE2-G1-" in text:
        count += 1
print(count)
'
}

count_page_total() {
  python3 -c '
import json, sys

try:
    payload = json.load(sys.stdin)
except Exception:
    print(0)
    raise SystemExit
data = payload.get("data", {})
print(data.get("total", 0) if isinstance(data, dict) else 0)
'
}

count_g1_resources() {
  if [ -z "${TOKEN:-}" ] || [ -z "${PID:-}" ]; then
    echo "0"
    return
  fi

  local section_count node_type_count definition_count type_count template_count file_count
  section_count=$(api_get "/api/master-data/projects/${PID}/section-nodes/tree" | count_section_matches)
  node_type_count=$(api_get "/api/master-data/projects/${PID}/node-types" | count_list_matches)
  definition_count=$(api_get "/api/master-data/projects/${PID}/deliverable-definitions" | count_list_matches)
  type_count=$(api_get "/api/master-data/projects/${PID}/deliverable-types" | count_list_matches)
  template_count=$(api_get "/api/master-data/projects/${PID}/directory-templates" | count_list_matches)
  file_count=$(api_get "/api/data-steward/projects/${PID}/file-resources?pageNo=1&pageSize=1&keyword=PHASE2-G1" | count_page_total)
  echo $((section_count + node_type_count + definition_count + type_count + template_count + file_count))
}

cleanup_delete() {
  local label="$1" url="$2" id="$3"
  if [ -z "${id:-}" ] || [ "$id" = "null" ]; then
    return
  fi
  local resp code
  resp=$(api_delete "$url" || true)
  code=$(echo "$resp" | json_get "code")
  if [ "$code" = "OK" ]; then
    echo "  [CLEANUP] ${label} ${id} deleted"
  else
    echo "  [CLEANUP-WARN] ${label} ${id} not deleted (code=${code:-empty})"
  fi
}

cleanup() {
  if [ "$CLEANUP_DONE" -eq 1 ]; then
    return
  fi
  CLEANUP_DONE=1

  if [ -z "${TOKEN:-}" ] || [ -z "${PID:-}" ]; then
    return
  fi

  echo ""
  echo "--- Cleanup: best-effort metadata cleanup ---"
  local binding_id
  for binding_id in $BINDING_IDS; do
    cleanup_delete "delivery binding" "/api/work-center/projects/${PID}/delivery-bindings/${binding_id}" "$binding_id"
  done
  cleanup_delete "document mock file" "/api/data-steward/projects/${PID}/file-resources/${DOC_FILE_ID}" "$DOC_FILE_ID"
  cleanup_delete "drawing mock file" "/api/data-steward/projects/${PID}/file-resources/${DWG_FILE_ID}" "$DWG_FILE_ID"
  cleanup_delete "directory template" "/api/master-data/projects/${PID}/directory-templates/${TMPL_ID}" "$TMPL_ID"
  cleanup_delete "deliverable attribute" "/api/master-data/projects/${PID}/deliverable-attributes/${ATTR_ID}" "$ATTR_ID"
  cleanup_delete "document deliverable type" "/api/master-data/projects/${PID}/deliverable-types/${DT_DOC_ID}" "$DT_DOC_ID"
  cleanup_delete "drawing deliverable type" "/api/master-data/projects/${PID}/deliverable-types/${DT_DWG_ID}" "$DT_DWG_ID"
  cleanup_delete "deliverable definition" "/api/master-data/projects/${PID}/deliverable-definitions/${DD_ID}" "$DD_ID"
  cleanup_delete "section node" "/api/master-data/projects/${PID}/section-nodes/${SEC_ID}" "$SEC_ID"

  if [ "$NT_CREATED" -eq 1 ] && [ -n "${NT_ID:-}" ]; then
    echo "  [CLEANUP-WARN] node type ${NT_ID} was created but no DELETE API exists; future runs will reuse existing node types to avoid continued growth"
  else
    echo "  [CLEANUP] node type reused; no cleanup needed"
  fi

  FINAL_G1_COUNT=$(count_g1_resources)
  echo "  [INFO] PHASE2-G1 visible resource count: before=${BASELINE_G1_COUNT:-unknown} after=${FINAL_G1_COUNT:-unknown}"
  if [ -n "${BASELINE_G1_COUNT:-}" ] && [ -n "${FINAL_G1_COUNT:-}" ] && [ "$FINAL_G1_COUNT" -le "$BASELINE_G1_COUNT" ] 2>/dev/null; then
    echo "  [PASS] 本轮可见 PHASE2-G1 资源未持续增长"
  else
    echo "  [WARN] 仍存在历史或不可清理 PHASE2-G1 资源；建议设置 G1_TEST_PROJECT_ID 使用隔离项目"
  fi
}

trap cleanup EXIT

assert_ok() {
  local response="$1" label="$2"
  local code
  code=$(echo "$response" | json_get "code")
  if [ "$code" = "OK" ]; then
    pass "$label"
  else
    fail "$label failed: code=${code:-empty}"
  fi
}

contains_no_sensitive_path() {
  local label="$1"
  shift
  local leak=0
  local response pattern
  for response in "$@"; do
    for pattern in "/Volumes" "smb://" "nas://" "storage_path" "storageUri" "raw row" "SQL"; do
      if echo "$response" | grep -qi "$pattern" 2>/dev/null; then
        echo "  LEAK in ${label}: ${pattern}"
        leak=1
      fi
    done
  done
  if [ "$leak" -eq 0 ]; then
    pass "${label} has no NAS path/storage leakage"
  else
    fail "${label} leaked sensitive storage details"
  fi
}

extract_matching_missing_count() {
  local response="$1" target_id="$2" type_id="$3"
  TARGET_ID="$target_id" TYPE_ID="$type_id" python3 -c '
import json, os, sys

payload = json.load(sys.stdin)
rows = payload.get("data", {}).get("rows", [])
target_id = str(os.environ["TARGET_ID"])
type_id = str(os.environ["TYPE_ID"])
count = sum(1 for row in rows
            if str(row.get("targetId")) == target_id
            and str(row.get("deliverableTypeId")) == type_id)
print(count)
' <<< "$response"
}

make_apply_body() {
  local confirmed="$1" row_json="$2"
  CONFIRMED="$confirmed" ROW_JSON="$row_json" python3 - <<'PY'
import json, os

row = json.loads(os.environ["ROW_JSON"])
body = {
    "confirmed": os.environ["CONFIRMED"].lower() == "true",
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
}

echo "=== Phase 2 Insert G1: Agent guided delivery governance MVP ==="

echo ""
echo "--- 1. Login and current project ---"
LOGIN_RESP=$(curl -s --connect-timeout 3 --max-time 10 -X POST \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" \
  "${BASE_URL}/api/core/auth/login")
TOKEN=$(echo "$LOGIN_RESP" | json_get "accessToken")
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  pass "Admin login"
else
  fail "Admin login failed; backend may not be running"
  exit 1
fi

ME_RESP=$(api_get "/api/core/users/me")
PID=$(echo "$ME_RESP" | json_get "currentProject.id")
PNAME=$(echo "$ME_RESP" | json_get "currentProject.name")
if [ -n "$PID" ] && [ "$PID" != "null" ]; then
  pass "Current project resolved: PID=${PID} NAME=${PNAME}"
else
  fail "Unable to resolve current project"
  exit 1
fi

if [ -n "${G1_TEST_PROJECT_ID:-}" ] && [ "$G1_TEST_PROJECT_ID" != "$PID" ]; then
  echo "  [INFO] G1_TEST_PROJECT_ID=${G1_TEST_PROJECT_ID}; switching to isolated test project"
  SWITCH_RESP=$(curl -s --connect-timeout 3 --max-time 10 -X POST \
    -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_URL}/api/core/projects/${G1_TEST_PROJECT_ID}:switch")
  SWITCH_TOKEN=$(echo "$SWITCH_RESP" | json_get "accessToken")
  if [ -n "$SWITCH_TOKEN" ] && [ "$SWITCH_TOKEN" != "null" ]; then
    TOKEN="$SWITCH_TOKEN"
    ME_RESP=$(api_get "/api/core/users/me")
    PID=$(echo "$ME_RESP" | json_get "currentProject.id")
    PNAME=$(echo "$ME_RESP" | json_get "currentProject.name")
    pass "Isolated test project selected: PID=${PID} NAME=${PNAME}"
  else
    fail "Unable to switch to G1_TEST_PROJECT_ID=${G1_TEST_PROJECT_ID}"
    exit 1
  fi
elif [ -n "${G1_TEST_PROJECT_ID:-}" ]; then
  echo "  [INFO] G1_TEST_PROJECT_ID=${G1_TEST_PROJECT_ID}; current token already uses isolated test project"
else
  echo "  [INFO] G1_TEST_PROJECT_ID not set; using current project PID=${PID}"
fi

BASELINE_G1_COUNT=$(count_g1_resources)
echo "  [INFO] PHASE2-G1 visible resource count before this run: ${BASELINE_G1_COUNT}"

echo ""
echo "--- 2. Health and OpenAPI ---"
HEALTH_RESP=$(curl -s --connect-timeout 3 --max-time 10 "${BASE_URL}/actuator/health" || echo '{}')
if echo "$HEALTH_RESP" | grep -q '"status":"UP"'; then
  pass "Backend health is UP"
else
  fail "Backend health is not UP"
fi

UNAUTH_HTTP=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 --max-time 10 \
  "${BASE_URL}/api/work-center/projects/${PID}/agent-governance/overview" || true)
if [ "$UNAUTH_HTTP" != "200" ]; then
  pass "Overview endpoint requires authentication"
else
  fail "Overview endpoint allowed anonymous access"
fi

OPENAPI_RESP=$(api_get "/v3/api-docs")
for path in \
  "/api/work-center/projects/{projectId}/agent-governance/overview" \
  "/api/work-center/projects/{projectId}/agent-governance/missing-items" \
  "/api/work-center/projects/{projectId}/agent-governance/recommend-bindings" \
  "/api/work-center/projects/{projectId}/agent-governance/recommendations:apply"; do
  if echo "$OPENAPI_RESP" | grep -q "$path"; then
    pass "OpenAPI contains ${path}"
  else
    fail "OpenAPI missing ${path}"
  fi
done

echo ""
echo "--- 3. Prepare metadata-only delivery fixtures ---"
SEC_RESP=$(api_post "/api/master-data/projects/${PID}/section-nodes" \
  "{\"code\":\"${RUN_CODE}-SEC\",\"name\":\"${RUN_CODE} Governance Section\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}")
SEC_ID=$(echo "$SEC_RESP" | json_get "id")
if [ -n "$SEC_ID" ]; then
  pass "Section fixture created"
else
  fail "Section fixture creation failed"
  exit 1
fi

NODE_TYPES_RESP=$(api_get "/api/master-data/projects/${PID}/node-types")
NT_ID=$(echo "$NODE_TYPES_RESP" | python3 -c '
import json, sys

try:
    rows = json.load(sys.stdin).get("data", [])
except Exception:
    rows = []
active = [row for row in rows if row.get("status") == "ACTIVE"]
pool = [row for row in active if not str(row.get("code", "")).startswith("PHASE2-G1-")] or active
print(pool[0].get("id", "") if pool else "")
')
if [ -n "$NT_ID" ]; then
  pass "Node type fixture reused (id=${NT_ID})"
else
  NT_RESP=$(api_post "/api/master-data/projects/${PID}/node-types" \
    "{\"code\":\"${RUN_CODE}-NT\",\"name\":\"${RUN_CODE} Governance Node\",\"scopeLevel\":1,\"sortOrder\":9999,\"status\":\"ACTIVE\"}")
  NT_ID=$(echo "$NT_RESP" | json_get "id")
  if [ -n "$NT_ID" ]; then
    NT_CREATED=1
    pass "Node type fixture created (id=${NT_ID})"
  else
    fail "Node type fixture creation failed"
    exit 1
  fi
fi

LOCK_RESP=$(api_post "/api/master-data/projects/${PID}/node-types:lock" '{}')
ALL_LOCKED=$(echo "$LOCK_RESP" | json_get "allNodeTypesLocked")
if [ "$ALL_LOCKED" = "true" ]; then
  pass "Node types locked"
else
  fail "Node type lock failed"
fi

DD_RESP=$(api_post "/api/master-data/projects/${PID}/deliverable-definitions" \
  "{\"nodeTypeId\":${NT_ID},\"code\":\"${RUN_CODE}-DEF\",\"name\":\"${RUN_CODE} Governance Definition\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":9999,\"status\":\"ACTIVE\"}")
DD_ID=$(echo "$DD_RESP" | json_get "id")
if [ -n "$DD_ID" ]; then
  pass "Deliverable definition fixture created"
else
  fail "Deliverable definition creation failed"
  exit 1
fi

DT_DOC_RESP=$(api_post "/api/master-data/projects/${PID}/deliverable-types" \
  "{\"deliverableDefinitionId\":${DD_ID},\"code\":\"${RUN_CODE}-DOC\",\"name\":\"${RUN_CODE} Governance Document\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}")
DT_DOC_ID=$(echo "$DT_DOC_RESP" | json_get "id")
if [ -n "$DT_DOC_ID" ]; then
  pass "Document deliverable type fixture created"
else
  fail "Document deliverable type creation failed"
  exit 1
fi

DT_DWG_RESP=$(api_post "/api/master-data/projects/${PID}/deliverable-types" \
  "{\"deliverableDefinitionId\":${DD_ID},\"code\":\"${RUN_CODE}-DWG\",\"name\":\"${RUN_CODE} Governance Drawing\",\"fileKind\":\"DRAWING\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":10000,\"status\":\"ACTIVE\"}")
DT_DWG_ID=$(echo "$DT_DWG_RESP" | json_get "id")
if [ -n "$DT_DWG_ID" ]; then
  pass "Drawing deliverable type fixture created"
else
  fail "Drawing deliverable type creation failed"
  exit 1
fi

ATTR_RESP=$(api_post "/api/master-data/projects/${PID}/deliverable-attributes" \
  "{\"deliverableTypeId\":${DT_DOC_ID},\"code\":\"${RUN_CODE}-ATTR\",\"name\":\"${RUN_CODE} Metadata Attribute\",\"valueType\":\"TEXT\",\"required\":false,\"sortOrder\":9999,\"status\":\"ACTIVE\"}")
ATTR_ID=$(echo "$ATTR_RESP" | json_get "id")
if [ -n "$ATTR_ID" ]; then
  pass "Deliverable attribute fixture created"
else
  fail "Deliverable attribute creation failed"
fi

TMPL_RESP=$(api_post "/api/master-data/projects/${PID}/directory-templates" \
  "{\"templateType\":\"DOCUMENT\",\"name\":\"${RUN_CODE} Directory Template\",\"rootNodeJson\":\"{\\\"children\\\":[]}\",\"sourceType\":\"MANUAL\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}")
TMPL_ID=$(echo "$TMPL_RESP" | json_get "id")
if [ -n "$TMPL_ID" ]; then
  pass "Directory template fixture created"
else
  fail "Directory template creation failed"
fi

DOC_FILE_RESP=$(api_post "/api/data-steward/projects/${PID}/file-resources" \
  "{\"originalName\":\"${RUN_CODE} Governance Section Governance Document.pdf\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":1024,\"storageUri\":\"mock://phase2-g1/${RUN_ID}/document.pdf\",\"checksum\":\"${RUN_CODE}-DOC-CHECKSUM\",\"businessTag\":\"governance document\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")
DOC_FILE_ID=$(echo "$DOC_FILE_RESP" | json_get "id")
if [ -n "$DOC_FILE_ID" ]; then
  pass "Document candidate file metadata created"
else
  fail "Document candidate file creation failed"
  exit 1
fi

DWG_FILE_RESP=$(api_post "/api/data-steward/projects/${PID}/file-resources" \
  "{\"originalName\":\"${RUN_CODE} Governance Section Governance Drawing.dwg\",\"fileKind\":\"DRAWING\",\"mimeType\":\"application/acad\",\"sizeBytes\":2048,\"storageUri\":\"mock://phase2-g1/${RUN_ID}/drawing.dwg\",\"checksum\":\"${RUN_CODE}-DWG-CHECKSUM\",\"businessTag\":\"governance drawing\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")
DWG_FILE_ID=$(echo "$DWG_FILE_RESP" | json_get "id")
if [ -n "$DWG_FILE_ID" ]; then
  pass "Drawing candidate file metadata created"
else
  fail "Drawing candidate file creation failed"
fi

echo ""
echo "--- 4. Overview and missing items ---"
OVERVIEW_RESP=$(api_get "/api/work-center/projects/${PID}/agent-governance/overview")
assert_ok "$OVERVIEW_RESP" "Overview endpoint returns OK"
SUMMARY_TEXT=$(echo "$OVERVIEW_RESP" | json_get "summaryText")
if [ -n "$SUMMARY_TEXT" ]; then
  pass "Overview contains agent summary text"
else
  fail "Overview missing summary text"
fi

MISSING_BEFORE_RESP=$(api_get "/api/work-center/projects/${PID}/agent-governance/missing-items?viewType=DOCUMENT&targetType=SECTION")
assert_ok "$MISSING_BEFORE_RESP" "Missing items endpoint returns OK"
BEFORE_MATCH=$(extract_matching_missing_count "$MISSING_BEFORE_RESP" "$SEC_ID" "$DT_DOC_ID")
if [ "${BEFORE_MATCH:-0}" -ge 1 ] 2>/dev/null; then
  pass "Fixture is reported as a document missing item"
else
  fail "Fixture missing item not found before apply"
fi

echo ""
echo "--- 5. Recommendation and human confirmation guard ---"
REC_RESP=$(api_post "/api/work-center/projects/${PID}/agent-governance/recommend-bindings" \
  '{"viewType":"DOCUMENT","targetType":"SECTION","limitPerMissingItem":1}')
assert_ok "$REC_RESP" "Recommendation endpoint returns OK"

REC_ROW_JSON=$(TARGET_ID="$SEC_ID" TYPE_ID="$DT_DOC_ID" FILE_ID="$DOC_FILE_ID" python3 -c '
import json, os, sys

payload = json.load(sys.stdin)
rows = payload.get("data", {}).get("rows", [])
target_id = str(os.environ["TARGET_ID"])
type_id = str(os.environ["TYPE_ID"])
file_id = str(os.environ["FILE_ID"])
for row in rows:
    if (str(row.get("targetId")) == target_id
            and str(row.get("deliverableTypeId")) == type_id
            and str(row.get("fileResourceId")) == file_id):
        print(json.dumps(row, ensure_ascii=False))
        break
' <<< "$REC_RESP"
)
if [ -n "$REC_ROW_JSON" ]; then
  pass "Recommendation contains the metadata-only candidate for the fixture"
else
  fail "Recommendation did not include the expected candidate"
  exit 1
fi

UNCONFIRMED_BODY=$(make_apply_body "false" "$REC_ROW_JSON")
UNCONFIRMED_RESP_FILE=$(mktemp)
UNCONFIRMED_HTTP=$(curl -s --connect-timeout 3 --max-time 20 -o "$UNCONFIRMED_RESP_FILE" -w "%{http_code}" -X POST \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "$UNCONFIRMED_BODY" \
  "${BASE_URL}/api/work-center/projects/${PID}/agent-governance/recommendations:apply")
UNCONFIRMED_RESP=$(cat "$UNCONFIRMED_RESP_FILE")
rm -f "$UNCONFIRMED_RESP_FILE"
if [ "$UNCONFIRMED_HTTP" != "200" ]; then
  pass "Unconfirmed apply is rejected"
else
  fail "Unconfirmed apply was accepted"
fi

MISSING_AFTER_REJECT_RESP=$(api_get "/api/work-center/projects/${PID}/agent-governance/missing-items?viewType=DOCUMENT&targetType=SECTION")
AFTER_REJECT_MATCH=$(extract_matching_missing_count "$MISSING_AFTER_REJECT_RESP" "$SEC_ID" "$DT_DOC_ID")
if [ "${AFTER_REJECT_MATCH:-0}" -ge 1 ] 2>/dev/null; then
  pass "Unconfirmed apply did not create a binding"
else
  fail "Missing item disappeared after rejected apply"
fi

echo ""
echo "--- 6. Confirmed apply and completeness refresh ---"
CONFIRMED_BODY=$(make_apply_body "true" "$REC_ROW_JSON")
APPLY_RESP=$(api_post "/api/work-center/projects/${PID}/agent-governance/recommendations:apply" "$CONFIRMED_BODY")
assert_ok "$APPLY_RESP" "Confirmed apply endpoint returns OK"
BINDING_IDS=$(echo "$APPLY_RESP" | python3 -c '
import json, sys

try:
    data = json.load(sys.stdin).get("data", {})
except Exception:
    data = {}
ids = []
for row in data.get("results", []) or []:
    value = row.get("bindingId")
    if value:
        ids.append(str(value))
for row in data.get("createdBindings", []) or []:
    value = row.get("id")
    if value:
        ids.append(str(value))
print(" ".join(dict.fromkeys(ids)))
')
CREATED_COUNT=$(echo "$APPLY_RESP" | json_get "createdCount")
if [ "${CREATED_COUNT:-0}" -ge 1 ] 2>/dev/null; then
  pass "Confirmed apply creates a delivery binding"
else
  fail "Confirmed apply did not create a binding"
fi

MISSING_AFTER_RESP=$(api_get "/api/work-center/projects/${PID}/agent-governance/missing-items?viewType=DOCUMENT&targetType=SECTION")
AFTER_MATCH=$(extract_matching_missing_count "$MISSING_AFTER_RESP" "$SEC_ID" "$DT_DOC_ID")
if [ "${AFTER_MATCH:-0}" -eq 0 ] 2>/dev/null; then
  pass "Completeness refresh no longer reports the applied fixture as missing"
else
  fail "Applied fixture is still reported as missing"
fi

OVERVIEW_AFTER_RESP=$(api_get "/api/work-center/projects/${PID}/agent-governance/overview")
if echo "$OVERVIEW_AFTER_RESP" | grep -q '"packageStatus"'; then
  pass "Overview refresh includes package status"
else
  fail "Overview refresh missing package status"
fi

echo ""
echo "--- 7. Audit, leakage, and frontend wiring ---"
AUDIT_RESP=$(api_get "/api/core/projects/${PID}/audit-logs?limit=100")
if echo "$AUDIT_RESP" | grep -q "work.agent-governance.recommend"; then
  pass "Audit contains recommend action"
else
  fail "Audit missing recommend action"
fi
if echo "$AUDIT_RESP" | grep -q "work.agent-governance.apply"; then
  pass "Audit contains apply action"
else
  fail "Audit missing apply action"
fi

contains_no_sensitive_path "G1 API responses" \
  "$OVERVIEW_RESP" "$MISSING_BEFORE_RESP" "$REC_RESP" "$UNCONFIRMED_RESP" "$APPLY_RESP" "$MISSING_AFTER_RESP" "$OVERVIEW_AFTER_RESP"

if grep -q "AgentDeliveryGovernancePage" frontend/src/router/index.ts \
  && grep -q "project-work-agent-governance" frontend/src/router/index.ts \
  && grep -q "交付治理助手" frontend/src/modules/core/components/ProjectWorkspaceNav.vue \
  && grep -q "开始交付治理" frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue \
  && grep -q "不移动 NAS 文件" frontend/src/modules/work-center/pages/AgentDeliveryGovernancePage.vue; then
  pass "Frontend route, nav, entry, and red-line copy are wired"
else
  fail "Frontend wiring is incomplete"
fi

cleanup

echo ""
echo "============================================"
echo "Phase 2 Insert G1 result: PASS=${PASS} FAIL=${FAIL}"
echo "============================================"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi

echo "ALL PASS"
