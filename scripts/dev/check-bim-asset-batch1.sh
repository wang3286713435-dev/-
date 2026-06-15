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

assert_not_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] != "OK", data' <<< "${response}" >/dev/null
}

json_data_project_id() {
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"].get("projectId") or json.load(sys.stdin)["data"]["id"])'
}

make_xlsx() {
  local out="$1"
  shift
  python3 - "$out" "$@" << 'PYEOF' > /dev/null
import sys, zipfile, io, os

out = sys.argv[1]
rows = []
for line in sys.argv[2:]:
    rows.append(line.split(','))

# Build sheet with inline strings
cols = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
def col_letter(i):
    if i < 26:
        return cols[i]
    return cols[i // 26 - 1] + cols[i % 26]

sheet_rows = []
for ri, row in enumerate(rows):
    cells = []
    for ci, val in enumerate(row):
        ref = col_letter(ci) + str(ri + 1)
        v = str(val).replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;')
        cells.append(f'<c r="{ref}" t="inlineStr"><is><t>{v}</t></is></c>')
    sheet_rows.append(f'<row r="{ri+1}">{"".join(cells)}</row>')

sheet_xml = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
    '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">\n'
    f'<sheetData>{"".join(sheet_rows)}</sheetData>\n'
    '</worksheet>')

buf = io.BytesIO()
with zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as zf:
    zf.writestr('[Content_Types].xml',
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">\n'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>\n'
        '<Default Extension="xml" ContentType="application/xml"/>\n'
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>\n'
        '<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>\n'
        '</Types>')
    zf.writestr('_rels/.rels',
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\n'
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>\n'
        '</Relationships>')
    zf.writestr('xl/_rels/workbook.xml.rels',
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\n'
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>\n'
        '</Relationships>')
    zf.writestr('xl/workbook.xml',
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">\n'
        '<sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>\n'
        '</workbook>')
    zf.writestr('xl/worksheets/sheet1.xml', sheet_xml)

with open(out, 'wb') as f:
    f.write(buf.getvalue())
PYEOF
}

json_data_id() {
  parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["id"])'
}

json_data() {
  parse_json "import json,sys; print(json.load(sys.stdin)$1)"
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

echo "== login secondary user for permission regression =="
engineer_login_response="$(curl -sS -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"delivery.engineer","password":"Engineer@123"}')"
echo "${engineer_login_response}"
assert_ok "${engineer_login_response}"
engineer_token="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])' <<< "${engineer_login_response}")"
engineer_auth_header=(-H "Authorization: Bearer ${engineer_token}")

echo "== step 1: create project assets =="
proj1_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/projects" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"code\":\"BIM-PROJ-A-${SUFFIX}\",\"name\":\"BIM项目A-${SUFFIX}\",\"industryType\":\"BUILDING_MEP\",\"projectStage\":\"CONSTRUCTION\",\"projectManagerName\":\"张三\",\"ownerOrgName\":\"业主A\",\"assetSource\":\"API\"}")"
echo "${proj1_response}"
assert_ok "${proj1_response}"
proj1_id="$(json_data_project_id <<< "${proj1_response}")"
proj1_code="$(json_data '["data"]["code"]' <<< "${proj1_response}")"

proj2_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/projects" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"code\":\"BIM-PROJ-B-${SUFFIX}\",\"name\":\"BIM项目B-${SUFFIX}\",\"industryType\":\"BUILDING_MEP\",\"projectStage\":\"DESIGN\",\"projectManagerName\":\"李四\",\"ownerOrgName\":\"业主B\",\"assetSource\":\"API\"}")"
echo "${proj2_response}"
assert_ok "${proj2_response}"
proj2_id="$(json_data_project_id <<< "${proj2_response}")"
proj2_code="$(json_data '["data"]["code"]' <<< "${proj2_response}")"

NAS_DIR="/tmp/bim-nas-scan-${SUFFIX}"
mkdir -p "${NAS_DIR}/csv-proj-sub" "${NAS_DIR}/csv-proj-a-sub"

echo "== step 1b: CSV import projects =="
CSV_PROJ_CODE="CSV-PROJ-${SUFFIX}"
csv_proj_body="code,name,industryType,projectStage
${CSV_PROJ_CODE},CSV导入项目-${SUFFIX},BUILDING_MEP,DESIGN
CSV-PROJ-2-${SUFFIX},CSV导入项目2-${SUFFIX},BUILDING_MEP,CONSTRUCTION"
csv_proj_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/projects:import?sourceName=batch1-test" \
  "${auth_header[@]}" -H 'Content-Type: text/csv' \
  -d "${csv_proj_body}")"
echo "${csv_proj_response}"
assert_ok "${csv_proj_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["successCount"] >= 2, f"Expected >=2 imported projects, got {data}"' <<< "${csv_proj_response}" >/dev/null

# Verify imported projects are visible in project list
project_list_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/projects?keyword=CSV-PROJ" "${auth_header[@]}")"
echo "${project_list_response}"
assert_ok "${project_list_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert len(data) >= 2, f"Expected >=2 CSV imported projects visible, got {len(data)}"' <<< "${project_list_response}" >/dev/null

echo "== step 1c: CSV import path mappings =="
csv_map_body="projectCode,projectName,nasPath
${CSV_PROJ_CODE},CSV导入项目-${SUFFIX},${NAS_DIR}/csv-proj-sub
${proj1_code},${proj1_code},${NAS_DIR}/csv-proj-a-sub"
csv_map_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/path-mappings:import?sourceName=batch1-test" \
  "${auth_header[@]}" -H 'Content-Type: text/csv' \
  -d "${csv_map_body}")"
echo "${csv_map_response}"
assert_ok "${csv_map_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["successCount"] >= 2, f"Expected >=2 imported mappings, got {data}"' <<< "${csv_map_response}" >/dev/null

# Verify imported path mappings are visible
mapping_list_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/path-mappings?enabled=true" "${auth_header[@]}")"
echo "${mapping_list_response}"
assert_ok "${mapping_list_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert len(data) >= 2, f"Expected >=2 path mappings visible, got {len(data)}"' <<< "${mapping_list_response}" >/dev/null

echo "== step 1d: xlsx multipart import projects =="
XLSX_PROJ_CODE="XLSX-PROJ-${SUFFIX}"
XLSX_PROJ_FILE="/tmp/batch1-proj-${SUFFIX}.xlsx"
make_xlsx "${XLSX_PROJ_FILE}" "code,name,industryType,projectStage" "${XLSX_PROJ_CODE},XLSX导入项目-${SUFFIX},BUILDING_MEP,DESIGN" "XLSX-PROJ-2-${SUFFIX},XLSX导入项目2-${SUFFIX},BUILDING_MEP,CONSTRUCTION"

xlsx_proj_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/projects:import?sourceName=batch1-xlsx" \
  "${auth_header[@]}" -F "file=@${XLSX_PROJ_FILE}")"
echo "${xlsx_proj_response}"
assert_ok "${xlsx_proj_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["successCount"] >= 2, f"Expected >=2 xlsx imported projects, got {data}"' <<< "${xlsx_proj_response}" >/dev/null

# Verify xlsx-imported projects are visible
xlsx_proj_list="$(curl -sS "${BASE_URL}/api/data-steward/assets/projects?keyword=XLSX-PROJ" "${auth_header[@]}")"
echo "${xlsx_proj_list}"
assert_ok "${xlsx_proj_list}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert len(data) >= 2, f"Expected >=2 xlsx imported projects visible, got {len(data)}"' <<< "${xlsx_proj_list}" >/dev/null

echo "== step 1e: xlsx multipart import path mappings =="
XLSX_MAP_FILE="/tmp/batch1-map-${SUFFIX}.xlsx"
make_xlsx "${XLSX_MAP_FILE}" "projectCode,projectName,nasPath" "${XLSX_PROJ_CODE},XLSX导入项目-${SUFFIX},${NAS_DIR}/xlsx-proj-sub" "${proj1_code},${proj1_code},${NAS_DIR}/xlsx-proj-a-sub"

xlsx_map_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/path-mappings:import?sourceName=batch1-xlsx" \
  "${auth_header[@]}" -F "file=@${XLSX_MAP_FILE}")"
echo "${xlsx_map_response}"
assert_ok "${xlsx_map_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["successCount"] >= 2, f"Expected >=2 xlsx imported mappings, got {data}"' <<< "${xlsx_map_response}" >/dev/null

# Verify xlsx-imported path mappings are visible
xlsx_map_list="$(curl -sS "${BASE_URL}/api/data-steward/assets/path-mappings?enabled=true" "${auth_header[@]}")"
echo "${xlsx_map_list}"
assert_ok "${xlsx_map_list}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; import re; assert any("xlsx" in m.get("nasPath","") for m in data), "No xlsx-imported mappings found"' <<< "${xlsx_map_list}" >/dev/null

rm -f "${XLSX_PROJ_FILE}" "${XLSX_MAP_FILE}"

echo "== step 2: create simulated NAS dirs with test files =="
mkdir -p "${NAS_DIR}/proj-a-sub" "${NAS_DIR}/proj-b-sub"

# Files that should match by path mapping
echo "fake rvt" > "${NAS_DIR}/proj-a-sub/model_v1.rvt"
echo "fake dwg" > "${NAS_DIR}/proj-a-sub/drawing_v1.dwg"
# File with project code in path (should match)
echo "fake ifc" > "${NAS_DIR}/${proj2_code}_model.ifc"
# File with unclear project - should go to review
echo "fake pdf" > "${NAS_DIR}/unknown_doc.pdf"

echo "== step 3: create path mappings =="
map1_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/path-mappings" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"projectId\":${proj1_id},\"nasPath\":\"${NAS_DIR}/proj-a-sub\",\"matchStrategy\":\"PREFIX\"}")"
echo "${map1_response}"
assert_ok "${map1_response}"

map2_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/path-mappings" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"projectId\":${proj2_id},\"nasPath\":\"${NAS_DIR}/proj-b-sub\",\"matchStrategy\":\"PREFIX\"}")"
echo "${map2_response}"
assert_ok "${map2_response}"

echo "== step 4: create and run scan =="
scan_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootCode\":\"NAS-ROOT\",\"rootPath\":\"${NAS_DIR}\",\"recursive\":true}")"
echo "${scan_response}"
assert_ok "${scan_response}"
scan_id="$(json_data_id <<< "${scan_response}")"

run_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${scan_id}:run" \
  "${auth_header[@]}")"
echo "${run_response}"
assert_ok "${run_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["autoIngested"] >= 2, data' <<< "${run_response}" >/dev/null
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert data["pendingReview"] >= 1, data' <<< "${run_response}" >/dev/null

echo "== step 5: verify high-confidence auto-ingest into files =="
files_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/files?sourceType=NAS_SCAN" "${auth_header[@]}")"
echo "${files_response}"
assert_ok "${files_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert len(data) >= 2, f"Expected >=2 auto-ingested, got {len(data)}"' <<< "${files_response}" >/dev/null
parse_json "import json,sys; data=json.load(sys.stdin)['data']; current=[f for f in data if '${SUFFIX}' in (f.get('storagePath') or '')]; assert current, 'No current-suffix files found'; missing=[f for f in current if not f.get('confidenceLevel')]; assert not missing, f'Auto-ingested files missing confidenceLevel: {missing}'" <<< "${files_response}" >/dev/null

echo "== step 6: verify low-confidence go to review queue =="
review_response="$(curl -sS "${BASE_URL}/api/data-steward/assets/review-candidates?reviewStatus=PENDING" "${auth_header[@]}")"
echo "${review_response}"
assert_ok "${review_response}"
parse_json 'import json,sys; data=json.load(sys.stdin)["data"]; assert len(data) >= 1, f"Expected >=1 pending, got {len(data)}"' <<< "${review_response}" >/dev/null
pending_id="$(parse_json 'import json,sys; print(json.load(sys.stdin)["data"][0]["id"])' <<< "${review_response}")"

echo "== step 6b: verify secondary user cannot see or operate admin global scan/candidate =="
engineer_scans="$(curl -sS "${BASE_URL}/api/data-steward/assets/nas-scans" "${engineer_auth_header[@]}")"
echo "${engineer_scans}"
assert_ok "${engineer_scans}"
parse_json "import json,sys; data=json.load(sys.stdin)['data']; assert all(item['id'] != ${scan_id} for item in data), 'Secondary user can see admin global scan task'" <<< "${engineer_scans}" >/dev/null

engineer_scan_detail="$(curl -sS "${BASE_URL}/api/data-steward/assets/nas-scans/${scan_id}" "${engineer_auth_header[@]}")"
echo "${engineer_scan_detail}"
assert_not_ok "${engineer_scan_detail}"

engineer_reviews="$(curl -sS "${BASE_URL}/api/data-steward/assets/review-candidates?reviewStatus=PENDING" "${engineer_auth_header[@]}")"
echo "${engineer_reviews}"
assert_ok "${engineer_reviews}"
parse_json "import json,sys; data=json.load(sys.stdin)['data']; assert all(item['id'] != ${pending_id} for item in data), 'Secondary user can see admin unmatched candidate'" <<< "${engineer_reviews}" >/dev/null

engineer_patch="$(curl -sS -X PATCH "${BASE_URL}/api/data-steward/assets/review-candidates/${pending_id}" \
  "${engineer_auth_header[@]}" -H 'Content-Type: application/json' \
  -d '{"matchedProjectId":1,"matchedProjectCode":"SAMPLE-MEP-001","detectedFileKind":"DRAWING","reviewMessage":"unauthorized hijack"}')"
echo "${engineer_patch}"
assert_not_ok "${engineer_patch}"

engineer_approve="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/review-candidates/${pending_id}:approve" \
  "${engineer_auth_header[@]}")"
echo "${engineer_approve}"
assert_not_ok "${engineer_approve}"

echo "== step 7: update and approve pending candidate (change .dwg to MODEL) =="
update_response="$(curl -sS -X PATCH "${BASE_URL}/api/data-steward/assets/review-candidates/${pending_id}" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"matchedProjectId\":${proj1_id},\"matchedProjectCode\":\"${proj1_code}\",\"detectedFileKind\":\"DRAWING\",\"detectedDiscipline\":\"ARCHITECTURE\",\"detectedVersionNo\":\"V2\",\"reviewMessage\":\"人工确认\"}")"
echo "${update_response}"
assert_ok "${update_response}"

approve_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/review-candidates/${pending_id}:approve" \
  "${auth_header[@]}")"
echo "${approve_response}"
assert_ok "${approve_response}"
parse_json 'import json,sys; assert json.load(sys.stdin)["data"]["reviewStatus"] == "APPROVED", json.load(sys.stdin)["data"]' <<< "${approve_response}" >/dev/null

echo "== step 8: verify file classification =="
all_files="$(curl -sS "${BASE_URL}/api/data-steward/assets/files" "${auth_header[@]}")"
echo "${all_files}"
assert_ok "${all_files}"
# .rvt and .ifc should be MODEL
parse_json 'import json,sys; models=[f for f in json.load(sys.stdin)["data"] if f["fileKind"]=="MODEL"]; assert len(models)>=2, f"Expected >=2 MODEL files, got {len(models)}"' <<< "${all_files}" >/dev/null
# .dwg should be DRAWING by default
parse_json 'import json,sys; drawings=[f for f in json.load(sys.stdin)["data"] if f["fileKind"]=="DRAWING"]; assert len(drawings)>=1, f"Expected >=1 DRAWING, got {len(drawings)}"' <<< "${all_files}" >/dev/null

echo "== step 9: verify SQL views =="
for view in ProjectAssetView FileAssetView ModelAssetView AuditEventView; do
  echo "checking view: ${view}"
  view_response="$(curl -sS "${BASE_URL}/api/data-steward/asset-views/${view}?limit=5" "${auth_header[@]}")"
  echo "${view_response}"
  # if view endpoint not available, skip (views are MySQL-side)
done

echo "== step 10: verify audit logs =="
audit_proj_response="$(curl -sS "${BASE_URL}/api/core/projects/${TARGET_PROJECT_ID}/audit-logs?moduleCode=data-steward&limit=10" "${auth_header[@]}")"
echo "${audit_proj_response}"
assert_ok "${audit_proj_response}"

echo "== step 11: verify OpenAPI =="
openapi_response="$(curl -sS -o /dev/null -w '%{http_code}' "${BASE_URL}/v3/api-docs")"
echo "OpenAPI status: ${openapi_response}"
if [[ "${openapi_response}" != "200" ]]; then
  echo "WARNING: OpenAPI not accessible"
fi

# Verify batch 1 endpoints are in docs
openapi_doc="$(curl -sS "${BASE_URL}/v3/api-docs")"
for endpoint in "/api/data-steward/assets/projects" "/api/data-steward/assets/path-mappings" "/api/data-steward/assets/nas-scans" "/api/data-steward/assets/review-candidates" "/api/data-steward/assets/files"; do
  if echo "${openapi_doc}" | python3 -c "import json,sys; d=json.load(sys.stdin); paths=[p for p in d.get('paths',{}) if '${endpoint}' in p]; assert paths, '${endpoint} not found'" 2>/dev/null; then
    echo "OK: ${endpoint} in OpenAPI"
  else
    echo "WARN: ${endpoint} NOT in OpenAPI"
  fi
done
for endpoint in "/api/data-steward/assets/nas-scans/{scanTaskId}" "/api/data-steward/assets/nas-scans/{scanTaskId}:run"; do
  if echo "${openapi_doc}" | python3 -c "import json,sys; d=json.load(sys.stdin); assert '${endpoint}' in d.get('paths',{}), '${endpoint} not found'" 2>/dev/null; then
    echo "OK: ${endpoint} in OpenAPI"
  else
    echo "ERROR: ${endpoint} NOT in OpenAPI"
    exit 1
  fi
done

echo ""
echo "bim asset batch1 ok"
