#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-platform.admin}"
PASSWORD="${3:-Admin@123}"
TARGET_PROJECT_ID="${4:-2}"
SUFFIX="${5:-$(date +%s)}"

parse_json() {
  python3 -c "$1"
}

assert_ok() {
  local response="$1"
  parse_json 'import json,sys; data=json.load(sys.stdin); assert data["code"] == "OK", data' <<< "${response}" >/dev/null
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

# ================================================================
# Step 1: Create mock NAS root directory
# ================================================================
echo "== step 1: create mock NAS root =="
MOCK_NAS="/tmp/bim-nas-98-${SUFFIX}"
rm -rf "${MOCK_NAS}"
mkdir -p "${MOCK_NAS}"

# ================================================================
# Step 2: Create first-level directories
# ================================================================
echo "== step 2: create first-level dirs =="
mkdir -p "${MOCK_NAS}/101-C塔"
mkdir -p "${MOCK_NAS}/98-深圳口岸项目"
mkdir -p "${MOCK_NAS}/99-丰图既有建模项目"
mkdir -p "${MOCK_NAS}/777-重复A"
mkdir -p "${MOCK_NAS}/777-重复B"
mkdir -p "${MOCK_NAS}/投标资料"
mkdir -p "${MOCK_NAS}/标准库"
mkdir -p "${MOCK_NAS}/综合资料"

# ================================================================
# Step 3: Create standard sub-directories for pilot directories
# ================================================================
echo "== step 3: create standard sub-folders =="
for proj_dir in "101-C塔" "98-深圳口岸项目" "99-丰图既有建模项目"; do
  mkdir -p "${MOCK_NAS}/${proj_dir}/00_工作进度"
  mkdir -p "${MOCK_NAS}/${proj_dir}/02_项目资源"
  mkdir -p "${MOCK_NAS}/${proj_dir}/05_发布文件"
  mkdir -p "${MOCK_NAS}/${proj_dir}/06_归档文件"
done

# ================================================================
# Step 4: Dry-run nas-projects:discover
# ================================================================
echo "== step 4: dry-run discover =="
dry_run_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-projects:discover" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootPath\":\"${MOCK_NAS}\",\"dryRun\":true,\"createMissingProjects\":true,\"createPathMappings\":true}")"
echo "${dry_run_response}"
assert_ok "${dry_run_response}"

# Assert dryRun flag is true and no projects created
echo "${dry_run_response}" | python3 -c "
import json,sys
d=json.load(sys.stdin)['data']
assert d['dryRun'] == True, 'dryRun should be True'
assert d['createdProjects'] == 0, 'dry-run should not create projects'
assert d['createdMappings'] == 0, 'dry-run should not create mappings'
assert d['totalDirectories'] == 8, 'expected 8 dirs'
" || exit 1

# Verify rows have expected fields
echo "${dry_run_response}" | python3 -c "
import json,sys
rows=json.load(sys.stdin)['data']['rows']
for r in rows:
    assert 'directoryType' in r, 'missing directoryType'
    assert 'requiresManualReview' in r, 'missing requiresManualReview'
    assert 'matchedStandardFolders' in r, 'missing matchedStandardFolders'
" || exit 1

# Verify pilot projects are READY
echo "${dry_run_response}" | python3 -c "
import json,sys
rows=json.load(sys.stdin)['data']['rows']
by_name={r['directoryName']:r for r in rows}
assert by_name['101-C塔']['status']=='READY', '101-C塔 not READY'
assert by_name['98-深圳口岸项目']['status']=='READY', '98 not READY'
assert by_name['99-丰图既有建模项目']['status']=='READY', '99 not READY'
" || exit 1

# Verify duplicate 777 directories are CONFLICT
echo "${dry_run_response}" | python3 -c "
import json,sys
rows=json.load(sys.stdin)['data']['rows']
p777=[r for r in rows if '777' in r['directoryName']]
assert len(p777)==2, 'expected 2 777 dirs'
assert all(r['status']=='CONFLICT' for r in p777), '777 dirs should be CONFLICT'
" || exit 1

# Verify reference directories
echo "${dry_run_response}" | python3 -c "
import json,sys
rows=json.load(sys.stdin)['data']['rows']
ref=[r for r in rows if r['directoryName'] in ['投标资料','标准库','综合资料']]
assert len(ref)==3, 'expected 3 reference dirs'
assert all(r['status'] in ['REFERENCE','NEEDS_CODE_REVIEW'] for r in ref), 'reference dirs should be REFERENCE'
" || exit 1

# Verify standard folders detected in pilot projects
echo "${dry_run_response}" | python3 -c "
import json,sys
rows=json.load(sys.stdin)['data']['rows']
by_name={r['directoryName']:r for r in rows}
sf=by_name['101-C塔'].get('matchedStandardFolders',[])
assert len(sf) >= 2, '101-C塔 should have >=2 standard folders'
" || exit 1

echo "dry-run assertions passed"

# ================================================================
# Step 5: Non-dry-run discover
# ================================================================
echo "== step 5: non-dry-run discover =="
discover_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-projects:discover" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootPath\":\"${MOCK_NAS}\",\"dryRun\":false,\"createMissingProjects\":true,\"createPathMappings\":true}")"
echo "${discover_response}"
assert_ok "${discover_response}"

# Only READY projects should be created or updated (101, 98, 99 = 3 projects)
echo "${discover_response}" | python3 -c "
import json,sys
d=json.load(sys.stdin)['data']
total = d['createdProjects'] + d['updatedProjects']
assert total >= 3, 'Expected >=3 created+updated projects, got ' + str(d)
" || exit 1

# No projects should be created for CONFLICT/REFERENCE dirs
echo "${discover_response}" | python3 -c "
import json,sys
rows=json.load(sys.stdin)['data']['rows']
for r in rows:
    if r['status'] in ['CONFLICT','REFERENCE','NEEDS_CODE_REVIEW','IGNORED']:
        assert not r.get('projectCreated'), r['directoryName'] + ' should not create project'
" || exit 1

echo "non-dry-run assertions passed"

# ================================================================
# Step 6: Place extended type files and scan
# ================================================================
echo "== step 6: place extended type files =="
SCAN_DIR="${MOCK_NAS}/101-C塔/05_发布文件"
echo "fake rvt content" > "${SCAN_DIR}/model.rvt"
echo "fake dwg content" > "${SCAN_DIR}/drawing.dwg"
echo "fake docx content" > "${SCAN_DIR}/report.docx"
echo "fake xlsx content" > "${SCAN_DIR}/table.xlsx"
echo "fake pptx content" > "${SCAN_DIR}/deck.pptx"
echo "fake glb content" > "${SCAN_DIR}/viewer.glb"
echo "fake zip content" > "${SCAN_DIR}/archive.zip"
# Token-match regression: stray.rvt at unmapped root with MOCK_NAS path containing "98"
echo "stray file for token match regression" > "${MOCK_NAS}/stray.rvt"

echo "== step 6b: create scan and run =="
scan_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootCode\":\"NAS-ROOT-TAIL\",\"rootPath\":\"${MOCK_NAS}\",\"recursive\":true}")"
echo "${scan_response}"
assert_ok "${scan_response}"
scan_id="$(json_data '["data"]["id"]' <<< "${scan_response}")"

run_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${scan_id}:run" \
  "${auth_header[@]}")"
echo "${run_response}"
assert_ok "${run_response}"

# Verify all file types were classified correctly
all_files="$(curl -sS "${BASE_URL}/api/data-steward/assets/files?sourceType=NAS_SCAN" "${auth_header[@]}")"
echo "${all_files}"
assert_ok "${all_files}"

echo "${all_files}" | python3 -c "
import json,sys
files=json.load(sys.stdin)['data']
mock_nas='${MOCK_NAS}'
mock_only=[f for f in files if f.get('storagePath','').startswith('nas://') and mock_nas in f.get('storagePath','')]
kinds=set(f['fileKind'] for f in mock_only)
expected={'MODEL','DRAWING','DOCUMENT','SPREADSHEET','PRESENTATION','MODEL_VIEWER','ARCHIVE'}
missing=expected-kinds
assert not missing, 'Missing file kinds in mock files: ' + str(missing) + '. Files: ' + str([f.get('storagePath','') for f in mock_only])
print('All file kinds present in mock files:', kinds)
" || exit 1

echo "extended file classification assertions passed"

# Token-match regression: stray.rvt must be PENDING not auto-ingested
echo "== step 6c: token-match regression for stray.rvt =="
echo "${all_files}" | python3 -c "
import json,sys
files=json.load(sys.stdin)['data']
mock_nas='${MOCK_NAS}'
stray_auto=[f for f in files if 'stray.rvt' in f.get('fileName','') and mock_nas in f.get('storagePath','')]
assert len(stray_auto)==0, 'stray.rvt must NOT be auto-ingested: ' + str(stray_auto)
print('OK: stray.rvt not auto-ingested')
" || exit 1

stray_review="$(curl -sS "${BASE_URL}/api/data-steward/assets/review-candidates?reviewStatus=PENDING" "${auth_header[@]}")"
echo "${stray_review}" | python3 -c "
import json,sys
candidates=json.load(sys.stdin)['data']
mock_nas='${MOCK_NAS}'
stray_pending=[c for c in candidates if 'stray.rvt' in c.get('fileName','') and mock_nas in c.get('rawPath','')]
assert len(stray_pending) >= 1, 'stray.rvt must be PENDING (token match prevention): ' + str(len(stray_pending))
assert stray_pending[0].get('confidenceLevel')=='LOW', 'stray.rvt must be LOW confidence'
print('OK: stray.rvt in review candidates with LOW confidence (token match prevention working)')
" || exit 1
echo "token-match regression assertions passed"

# ================================================================
# Step 7: Low-value file filtering
# ================================================================
echo "== step 7: low-value file filtering =="
mkdir -p "${MOCK_NAS}/101-C塔/02_项目资源/lowval-test"
echo -n "" > "${MOCK_NAS}/101-C塔/02_项目资源/lowval-test/.DS_Store"
echo -n "" > "${MOCK_NAS}/101-C塔/02_项目资源/lowval-test/Thumbs.db"
echo -n "" > "${MOCK_NAS}/101-C塔/02_项目资源/lowval-test/desktop.ini"
echo -n "" > "${MOCK_NAS}/101-C塔/02_项目资源/lowval-test/~\$lock.docx"
touch "${MOCK_NAS}/101-C塔/02_项目资源/lowval-test/empty.rvt"
echo "valid content" > "${MOCK_NAS}/101-C塔/02_项目资源/lowval-test/valid.rvt"

scan2_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootCode\":\"NAS-ROOT-LOWVAL\",\"rootPath\":\"${MOCK_NAS}\",\"recursive\":true}")"
echo "${scan2_response}"
assert_ok "${scan2_response}"
scan2_id="$(json_data '["data"]["id"]' <<< "${scan2_response}")"

run2_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${scan2_id}:run" \
  "${auth_header[@]}")"
echo "${run2_response}"
assert_ok "${run2_response}"

files2="$(curl -sS "${BASE_URL}/api/data-steward/assets/files?sourceType=NAS_SCAN" "${auth_header[@]}")"
echo "${files2}"
assert_ok "${files2}"

echo "${files2}" | python3 -c "
import json,sys
files=json.load(sys.stdin)['data']
mock_nas='${MOCK_NAS}'
mock_only=[f for f in files if f.get('storagePath','').startswith('nas://') and mock_nas in f.get('storagePath','')]
fnames={f['fileName'] for f in mock_only}
lowval={'.DS_Store','Thumbs.db','desktop.ini'}
assert lowval.isdisjoint(fnames), 'Low-value files should not be in assets: ' + str(lowval & fnames)
assert not any(fn.startswith('~\$') for fn in fnames), 'Office lock files should not be in assets'
assert 'valid.rvt' in fnames, 'valid.rvt should be ingested in mock files. Got: ' + str(fnames)
print('Low-value filtering OK (scoped to ' + str(len(mock_only)) + ' mock files)')
" || exit 1

echo "low-value file filtering assertions passed"

# ================================================================
# Step 8: Temp directory downgrade
# ================================================================
echo "== step 8: temp directory downgrade (incl. nested) =="
# Direct child of temp directory
mkdir -p "${MOCK_NAS}/101-C塔/临时文件"
echo "fake rvt in temp" > "${MOCK_NAS}/101-C塔/临时文件/temp_model.rvt"
# Deeply nested under temp directory — should also be downgraded
mkdir -p "${MOCK_NAS}/101-C塔/临时文件/转换/subdir"
echo "deep temp" > "${MOCK_NAS}/101-C塔/临时文件/转换/subdir/deep_temp.rvt"
# Another temp keyword at a different level
mkdir -p "${MOCK_NAS}/101-C塔/temp/nested"
echo "nested temp" > "${MOCK_NAS}/101-C塔/temp/nested/nested_temp.rvt"
# File under 新建文件夹 (another temp keyword)
mkdir -p "${MOCK_NAS}/101-C塔/新建文件夹"
echo "new folder file" > "${MOCK_NAS}/101-C塔/新建文件夹/newfolder_file.rvt"

scan3_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootCode\":\"NAS-ROOT-TEMP\",\"rootPath\":\"${MOCK_NAS}\",\"recursive\":true}")"
echo "${scan3_response}"
assert_ok "${scan3_response}"
scan3_id="$(json_data '["data"]["id"]' <<< "${scan3_response}")"

run3_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${scan3_id}:run" \
  "${auth_header[@]}")"
echo "${run3_response}"
assert_ok "${run3_response}"

review_candidates="$(curl -sS "${BASE_URL}/api/data-steward/assets/review-candidates?reviewStatus=PENDING" "${auth_header[@]}")"
echo "${review_candidates}"
assert_ok "${review_candidates}"

echo "${review_candidates}" | python3 -c "
import json,sys
candidates=json.load(sys.stdin)['data']
mock_nas='${MOCK_NAS}'
mock_only=[c for c in candidates if mock_nas in c.get('rawPath','')]
temp_files=['temp_model.rvt','deep_temp.rvt','nested_temp.rvt','newfolder_file.rvt']
mock_high=[c for c in mock_only if any(tf in c.get('fileName','') for tf in temp_files) and c.get('confidenceLevel')=='HIGH']
assert len(mock_high)==0, 'Temp directory files should not be HIGH confidence: ' + str(mock_high)
# Verify all 4 temp files are found as PENDING (i.e. they were scanned but downgraded)
mock_fnames={c.get('fileName','') for c in mock_only}
for tf in temp_files:
    assert tf in mock_fnames, tf + ' should be in review candidates, got: ' + str(mock_fnames)
print('Temp directory downgrade OK (incl. nested, ' + str(len(mock_only)) + ' mock candidates, files: ' + str(mock_fnames) + ')')
" || exit 1

echo "temp directory downgrade assertions passed"

# ================================================================
# Step 9: Rescan idempotency
# ================================================================
echo "== step 9: rescan idempotency =="
scan4_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans" \
  "${auth_header[@]}" -H 'Content-Type: application/json' \
  -d "{\"rootCode\":\"NAS-ROOT-IDEM\",\"rootPath\":\"${MOCK_NAS}/101-C塔/05_发布文件\",\"recursive\":false}")"
echo "${scan4_response}"
assert_ok "${scan4_response}"
scan4_id="$(json_data '["data"]["id"]' <<< "${scan4_response}")"

run4_response="$(curl -sS -X POST "${BASE_URL}/api/data-steward/assets/nas-scans/${scan4_id}:run" \
  "${auth_header[@]}")"
echo "${run4_response}"
assert_ok "${run4_response}"

files4="$(curl -sS "${BASE_URL}/api/data-steward/assets/files" "${auth_header[@]}")"
echo "${files4}"
assert_ok "${files4}"

echo "${files4}" | python3 -c "
import json,sys
from collections import Counter
files=json.load(sys.stdin)['data']
mock_nas='${MOCK_NAS}'
mock_paths=[f.get('storagePath','') for f in files if f.get('storagePath','').startswith('nas://') and mock_nas in f.get('storagePath','')]
dupes={p:c for p,c in Counter(mock_paths).items() if c>1}
assert not dupes, 'Duplicate storage paths found after rescan (mock only): ' + str(dupes)
print('Rescan idempotency OK (scoped to ' + str(len(mock_paths)) + ' mock paths)')
" || exit 1

echo "rescan idempotency assertions passed"

# ================================================================
# Step 10: OpenAPI verification
# ================================================================
echo "== step 10: OpenAPI verification =="
openapi_doc="$(curl -sS "${BASE_URL}/v3/api-docs")"
echo "OpenAPI fetched, size: $(echo "${openapi_doc}" | wc -c)"

for endpoint in \
  "/api/data-steward/assets/nas-projects:discover" \
  "/api/data-steward/assets/nas-scans" \
  "/api/data-steward/assets/review-candidates" \
  "/api/data-steward/assets/files"; do
  if echo "${openapi_doc}" | python3 -c "import json,sys; d=json.load(sys.stdin); paths=[p for p in d.get('paths',{}) if '${endpoint}' in p]; assert paths, '${endpoint} not found'" 2>/dev/null; then
    echo "OK: ${endpoint} in OpenAPI"
  else
    echo "ERROR: ${endpoint} NOT in OpenAPI"
    exit 1
  fi
done

echo "${openapi_doc}" | python3 -c "
import json,sys
d=json.load(sys.stdin)
paths=d.get('paths',{})
discover=[p for p in paths if 'nas-projects:discover' in p]
assert discover, 'nas-projects:discover not found in OpenAPI paths'
assert 'post' in paths[discover[0]], 'nas-projects:discover should have POST method'
print('OK: nas-projects:discover registered in OpenAPI')
" || exit 1

# Cleanup
rm -rf "${MOCK_NAS}"

echo ""
echo "bim asset batch1 tail ok"
