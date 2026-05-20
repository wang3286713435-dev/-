#!/usr/bin/env bash
# check-phase2-batch6b-delivery-package.sh
# 二期批次 6B：批量挂接交付与交付包准备视图 — 验收脚本
#
# 自包含：如果当前项目缺少测试数据则通过 API 创建，使用 mock:// 路径。
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin@123}"

PASS=0
FAIL=0
TOKEN=""
PID=""
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"
# track created resources for optional cleanup
CREATED_FILES=""
CREATED_SECTIONS=""
CREATED_TYPES=""

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

api_post() {
  local url="$1" body="$2"
  curl -s --connect-timeout 3 --max-time 15 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

api_get() {
  local url="$1"
  curl -s --connect-timeout 3 --max-time 15 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

json_val() {
  python3 -c "
import sys,json
try:
    d=json.load(sys.stdin)
    data=d.get('data',d)
    keys='$1'.split('.')
    for k in keys:
        if isinstance(data,dict): data=data.get(k,'')
        elif isinstance(data,list): data=data[int(k)] if k.isdigit() else ''
        else: data=''
    if isinstance(data,bool): print('true' if data else 'false')
    else: print(data)
except: print('')
" 2>/dev/null
}

echo "=== Phase 2 Batch 6B: 批量挂接交付与交付包准备视图 ==="

# ---- 1. Login ----
echo ""
echo "--- 1. Login ---"
LOGIN_RESP=$(curl -s --connect-timeout 3 --max-time 10 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")
TOKEN=$(echo "$LOGIN_RESP" | json_val "accessToken")
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  pass "管理员登录成功"
else
  fail "管理员登录失败: $LOGIN_RESP"
  echo "无法继续，退出"
  exit 1
fi

# ---- 2. Get current project ----
echo ""
echo "--- 2. Get current project ---"
ME_RESP=$(api_get "/api/core/users/me")
PID=$(echo "$ME_RESP" | json_val "currentProject.id")
PNAME=$(echo "$ME_RESP" | json_val "currentProject.name")
if [ -n "$PID" ] && [ "$PID" != "" ] && [ "$PID" != "null" ]; then
  pass "当前项目: PID=$PID NAME=$PNAME"
else
  fail "无法获取当前项目"
  exit 1
fi

# ---- 3. Ensure section node exists ----
echo ""
echo "--- 3. Ensure section node ---"
SEC_RESP=$(api_get "/api/master-data/projects/${PID}/section-nodes/tree")
SEC_ID=$(echo "$SEC_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',d)
def find_first(nodes):
    if not nodes: return None
    for n in nodes:
        if n.get('status')=='ACTIVE': return n['id']
        r=find_first(n.get('children',[]))
        if r: return r
    return None
print(find_first(data) or '')
" 2>/dev/null)

if [ -z "$SEC_ID" ] || [ "$SEC_ID" = "" ]; then
  echo "  无可用部位节点，创建测试部位..."
  SEC_CREATE_RESP=$(api_post "/api/master-data/projects/${PID}/section-nodes" \
    '{"code":"TEST-BATCH6B","name":"测试部位6B","parentId":null,"status":"ACTIVE"}')
  SEC_ID=$(echo "$SEC_CREATE_RESP" | json_val "id")
  if [ -n "$SEC_ID" ] && [ "$SEC_ID" != "" ] && [ "$SEC_ID" != "null" ]; then
    CREATED_SECTIONS="$SEC_ID"
    pass "创建测试部位: SEC_ID=$SEC_ID"
  else
    fail "无法创建部位节点: $SEC_CREATE_RESP"
    exit 1
  fi
else
  pass "复用现有部位: SEC_ID=$SEC_ID"
fi

# ---- 4. Ensure deliverable types exist for DOCUMENT and DRAWING ----
echo ""
echo "--- 4. Ensure deliverable types ---"
DT_RESP=$(api_get "/api/master-data/projects/${PID}/deliverable-types")

DOC_DT_ID=$(echo "$DT_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',d)
for t in data:
    if t.get('fileKind')=='DOCUMENT': print(t['id']); break
" 2>/dev/null)

DWG_DT_ID=$(echo "$DT_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',d)
for t in data:
    if t.get('fileKind')=='DRAWING': print(t['id']); break
" 2>/dev/null)

# Find a deliverable definition to attach types to
DEF_RESP=$(api_get "/api/master-data/projects/${PID}/deliverable-definitions")
DEF_ID=$(echo "$DEF_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',d)
if data: print(data[0]['id'])
" 2>/dev/null)

if [ -z "$DOC_DT_ID" ] || [ "$DOC_DT_ID" = "" ]; then
  if [ -z "$DEF_ID" ] || [ "$DEF_ID" = "" ]; then
    fail "无交付物定义，无法创建类型"
    exit 1
  fi
  echo "  缺少 DOCUMENT 交付物类型，创建中..."
  DT_CREATE=$(api_post "/api/master-data/projects/${PID}/deliverable-types" \
    "{\"code\":\"TEST-BATCH6B-DOC\",\"name\":\"测试文档类型6B\",\"fileKind\":\"DOCUMENT\",\"deliverableDefinitionId\":${DEF_ID},\"status\":\"ACTIVE\",\"bindingStrategy\":\"SECTION_NODE\"}")
  DOC_DT_ID=$(echo "$DT_CREATE" | json_val "id")
  if [ -n "$DOC_DT_ID" ] && [ "$DOC_DT_ID" != "" ] && [ "$DOC_DT_ID" != "null" ]; then
    CREATED_TYPES="${CREATED_TYPES} $DOC_DT_ID"
    pass "创建 DOCUMENT 交付物类型: DOC_DT_ID=$DOC_DT_ID"
  else
    fail "创建 DOCUMENT 类型失败: $DT_CREATE"
    exit 1
  fi
else
  pass "复用现有 DOCUMENT 交付物类型: DOC_DT_ID=$DOC_DT_ID"
fi

if [ -z "$DWG_DT_ID" ] || [ "$DWG_DT_ID" = "" ]; then
  echo "  缺少 DRAWING 交付物类型，创建中..."
  DT_CREATE=$(api_post "/api/master-data/projects/${PID}/deliverable-types" \
    "{\"code\":\"TEST-BATCH6B-DWG\",\"name\":\"测试图纸类型6B\",\"fileKind\":\"DRAWING\",\"deliverableDefinitionId\":${DEF_ID},\"status\":\"ACTIVE\",\"bindingStrategy\":\"SECTION_NODE\"}")
  DWG_DT_ID=$(echo "$DT_CREATE" | json_val "id")
  if [ -n "$DWG_DT_ID" ] && [ "$DWG_DT_ID" != "" ] && [ "$DWG_DT_ID" != "null" ]; then
    CREATED_TYPES="${CREATED_TYPES} $DWG_DT_ID"
    pass "创建 DRAWING 交付物类型: DWG_DT_ID=$DWG_DT_ID"
  else
    fail "创建 DRAWING 类型失败: $DT_CREATE"
    exit 1
  fi
else
  pass "复用现有 DRAWING 交付物类型: DWG_DT_ID=$DWG_DT_ID"
fi

# ---- 5. Ensure file resources exist ----
echo ""
echo "--- 5. Ensure file resources ---"

# Check existing DOCUMENT files
DOC_FILES_RESP=$(api_get "/api/data-steward/projects/${PID}/file-resources?fileKind=DOCUMENT&processStatus=PROCESSED&pageNo=1&pageSize=5")
DOC_COUNT=$(echo "$DOC_FILES_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',d)
print(len(data.get('items',[])))
" 2>/dev/null)

if [ "${DOC_COUNT:-0}" -lt 2 ] 2>/dev/null; then
  echo "  缺少 DOCUMENT 文件，创建 2 个测试文件..."
  F1=$(api_post "/api/data-steward/projects/${PID}/file-resources" \
    "{\"originalName\":\"test-batch6b-doc-1-${RUN_ID}.pdf\",\"fileKind\":\"DOCUMENT\",\"storageUri\":\"mock://batch6b/${RUN_ID}/doc-1.pdf\",\"versionNo\":\"1.0\",\"processStatus\":\"PROCESSED\",\"mimeType\":\"application/pdf\",\"sizeBytes\":1024}")
  F2=$(api_post "/api/data-steward/projects/${PID}/file-resources" \
    "{\"originalName\":\"test-batch6b-doc-2-${RUN_ID}.pdf\",\"fileKind\":\"DOCUMENT\",\"storageUri\":\"mock://batch6b/${RUN_ID}/doc-2.pdf\",\"versionNo\":\"1.0\",\"processStatus\":\"PROCESSED\",\"mimeType\":\"application/pdf\",\"sizeBytes\":2048}")
  DOC_ID1=$(echo "$F1" | json_val "id")
  DOC_ID2=$(echo "$F2" | json_val "id")
  if [ -n "$DOC_ID1" ] && [ "$DOC_ID1" != "" ] && [ -n "$DOC_ID2" ] && [ "$DOC_ID2" != "" ]; then
    CREATED_FILES="${CREATED_FILES} $DOC_ID1 $DOC_ID2"
    pass "创建 DOCUMENT 测试文件: $DOC_ID1, $DOC_ID2"
  else
    fail "创建 DOCUMENT 文件失败: F1=$F1 F2=$F2"
  fi
else
  DOC_ID1=$(echo "$DOC_FILES_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
items=d.get('data',{}).get('items',[])
print(items[0]['id'] if len(items)>0 else '')
" 2>/dev/null)
  DOC_ID2=$(echo "$DOC_FILES_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
items=d.get('data',{}).get('items',[])
print(items[1]['id'] if len(items)>1 else '')
" 2>/dev/null)
  pass "复用现有 DOCUMENT 文件: $DOC_ID1, $DOC_ID2"
fi

# Check existing DRAWING files
DWG_FILES_RESP=$(api_get "/api/data-steward/projects/${PID}/file-resources?fileKind=DRAWING&processStatus=PROCESSED&pageNo=1&pageSize=5")
DWG_COUNT=$(echo "$DWG_FILES_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',d)
print(len(data.get('items',[])))
" 2>/dev/null)

if [ "${DWG_COUNT:-0}" -lt 2 ] 2>/dev/null; then
  echo "  缺少 DRAWING 文件，创建 2 个测试文件..."
  F3=$(api_post "/api/data-steward/projects/${PID}/file-resources" \
    "{\"originalName\":\"test-batch6b-dwg-1-${RUN_ID}.dwg\",\"fileKind\":\"DRAWING\",\"storageUri\":\"mock://batch6b/${RUN_ID}/dwg-1.dwg\",\"versionNo\":\"1.0\",\"processStatus\":\"PROCESSED\",\"mimeType\":\"application/acad\",\"sizeBytes\":4096}")
  F4=$(api_post "/api/data-steward/projects/${PID}/file-resources" \
    "{\"originalName\":\"test-batch6b-dwg-2-${RUN_ID}.dwg\",\"fileKind\":\"DRAWING\",\"storageUri\":\"mock://batch6b/${RUN_ID}/dwg-2.dwg\",\"versionNo\":\"1.0\",\"processStatus\":\"PROCESSED\",\"mimeType\":\"application/acad\",\"sizeBytes\":8192}")
  DWG_ID1=$(echo "$F3" | json_val "id")
  DWG_ID2=$(echo "$F4" | json_val "id")
  if [ -n "$DWG_ID1" ] && [ "$DWG_ID1" != "" ] && [ -n "$DWG_ID2" ] && [ "$DWG_ID2" != "" ]; then
    CREATED_FILES="${CREATED_FILES} $DWG_ID1 $DWG_ID2"
    pass "创建 DRAWING 测试文件: $DWG_ID1, $DWG_ID2"
  else
    fail "创建 DRAWING 文件失败: F3=$F3 F4=$F4"
  fi
else
  DWG_ID1=$(echo "$DWG_FILES_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
items=d.get('data',{}).get('items',[])
print(items[0]['id'] if len(items)>0 else '')
" 2>/dev/null)
  DWG_ID2=$(echo "$DWG_FILES_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
items=d.get('data',{}).get('items',[])
print(items[1]['id'] if len(items)>1 else '')
" 2>/dev/null)
  pass "复用现有 DRAWING 文件: $DWG_ID1, $DWG_ID2"
fi

# ---- 6. Batch bind documents ----
echo ""
echo "--- 6. Batch bind documents ---"
BATCH_DOC=$(api_post "/api/work-center/projects/${PID}/delivery-bindings:batch" \
  "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":${SEC_ID},\"deliverableTypeId\":${DOC_DT_ID},\"fileResourceIds\":[${DOC_ID1},${DOC_ID2}],\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"PENDING\",\"remark\":\"test-batch6b-doc\"}")
DOC_CREATED=$(echo "$BATCH_DOC" | json_val "createdCount")
DOC_SKIPPED=$(echo "$BATCH_DOC" | json_val "skippedCount")
DOC_FAILED=$(echo "$BATCH_DOC" | json_val "failedCount")

if [ "${DOC_CREATED:-0}" -ge 2 ] 2>/dev/null; then
  pass "文档批量绑定成功: created=$DOC_CREATED skipped=$DOC_SKIPPED failed=$DOC_FAILED"
elif [ "${DOC_CREATED:-0}" -ge 1 ] 2>/dev/null && [ "${DOC_SKIPPED:-0}" -ge 1 ] 2>/dev/null; then
  pass "文档批量绑定部分成功(幂等存在): created=$DOC_CREATED skipped=$DOC_SKIPPED"
elif [ "${DOC_SKIPPED:-0}" -ge 2 ] 2>/dev/null; then
  pass "文档批量绑定全部跳过(幂等): skipped=$DOC_SKIPPED"
else
  fail "文档批量绑定异常: created=$DOC_CREATED skipped=$DOC_SKIPPED failed=$DOC_FAILED resp=$(echo $BATCH_DOC | head -c 300)"
fi

# ---- 7. Idempotency: repeat batch same files ----
echo ""
echo "--- 7. Idempotency (repeat same files) ---"
BATCH_REPEAT=$(api_post "/api/work-center/projects/${PID}/delivery-bindings:batch" \
  "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":${SEC_ID},\"deliverableTypeId\":${DOC_DT_ID},\"fileResourceIds\":[${DOC_ID1},${DOC_ID2}],\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"PENDING\",\"remark\":\"test-batch6b-repeat\"}")
REP_CREATED=$(echo "$BATCH_REPEAT" | json_val "createdCount")
REP_SKIPPED=$(echo "$BATCH_REPEAT" | json_val "skippedCount")

if [ "${REP_CREATED:-0}" -eq 0 ] 2>/dev/null && [ "${REP_SKIPPED:-0}" -gt 0 ] 2>/dev/null; then
  pass "幂等验证通过: created=0 skipped=$REP_SKIPPED"
else
  fail "幂等失败: created=$REP_CREATED skipped=$REP_SKIPPED (expected created=0)"
fi

# ---- 8. Batch bind drawings ----
echo ""
echo "--- 8. Batch bind drawings ---"
BATCH_DWG=$(api_post "/api/work-center/projects/${PID}/delivery-bindings:batch" \
  "{\"viewType\":\"DRAWING\",\"sectionNodeId\":${SEC_ID},\"deliverableTypeId\":${DWG_DT_ID},\"fileResourceIds\":[${DWG_ID1},${DWG_ID2}],\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"PENDING\",\"remark\":\"test-batch6b-dwg\"}")
DWG_CREATED=$(echo "$BATCH_DWG" | json_val "createdCount")
DWG_SKIPPED=$(echo "$BATCH_DWG" | json_val "skippedCount")

if [ "${DWG_CREATED:-0}" -ge 1 ] 2>/dev/null || [ "${DWG_SKIPPED:-0}" -ge 1 ] 2>/dev/null; then
  pass "图纸批量绑定成功: created=$DWG_CREATED skipped=$DWG_SKIPPED"
else
  fail "图纸批量绑定失败: created=$DWG_CREATED resp=$(echo $BATCH_DWG | head -c 200)"
fi

# ---- 9. Wrong file kind rejection ----
echo ""
echo "--- 9. Wrong file kind rejection ---"
WRONG_HTTP=$(curl -s --connect-timeout 3 --max-time 10 -o /dev/null -w "%{http_code}" -X POST \
  "${BASE_URL}/api/work-center/projects/${PID}/delivery-bindings:batch" \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":${SEC_ID},\"deliverableTypeId\":${DOC_DT_ID},\"fileResourceIds\":[${DWG_ID1}],\"bindingStatus\":\"BOUND\"}")

if [ "$WRONG_HTTP" != "200" ]; then
  pass "错误文件类型绑定被拒绝: HTTP $WRONG_HTTP"
else
  # Even if 200, check that it didn't create
  WRONG_BODY=$(curl -s --connect-timeout 3 --max-time 10 -X POST \
    "${BASE_URL}/api/work-center/projects/${PID}/delivery-bindings:batch" \
    -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
    -d "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":${SEC_ID},\"deliverableTypeId\":${DOC_DT_ID},\"fileResourceIds\":[${DWG_ID1}],\"bindingStatus\":\"BOUND\"}")
  WC=$(echo "$WRONG_BODY" | json_val "createdCount")
  if [ "${WC:-0}" -eq 0 ] 2>/dev/null; then
    pass "错误文件类型绑定未创建记录: created=$WC"
  else
    fail "错误文件类型绑定错误地创建了 $WC 条记录"
  fi
fi

# ---- 10. Cross-project file rejection ----
echo ""
echo "--- 10. Cross-project file rejection ---"
OTHER_PROJECT_ID=$(echo "$ME_RESP" | python3 -c "
import sys,json
d=json.load(sys.stdin)
data=d.get('data',d)
pid=str('${PID}')
for p in data.get('projects',[]):
    if str(p.get('id')) != pid:
        print(p.get('id'))
        break
" 2>/dev/null)

if [ -z "${OTHER_PROJECT_ID:-}" ] || [ "$OTHER_PROJECT_ID" = "null" ]; then
  fail "缺少第二个可访问项目，无法验证跨项目文件拒绝"
else
  OTHER_SWITCH=$(curl -s --connect-timeout 3 --max-time 10 -X POST \
    "${BASE_URL}/api/core/projects/${OTHER_PROJECT_ID}:switch" \
    -H "Authorization: Bearer ${TOKEN}")
  OTHER_TOKEN=$(echo "$OTHER_SWITCH" | json_val "accessToken")
  OTHER_FILE_RESP=$(curl -s --connect-timeout 3 --max-time 15 -X POST \
    "${BASE_URL}/api/data-steward/projects/${OTHER_PROJECT_ID}/file-resources" \
    -H "Authorization: Bearer ${OTHER_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "{\"originalName\":\"test-batch6b-cross-${RUN_ID}.pdf\",\"fileKind\":\"DOCUMENT\",\"storageUri\":\"mock://batch6b/${RUN_ID}/cross-project.pdf\",\"versionNo\":\"1.0\",\"processStatus\":\"PROCESSED\",\"mimeType\":\"application/pdf\",\"sizeBytes\":512}")
  OTHER_FILE_ID=$(echo "$OTHER_FILE_RESP" | json_val "id")
  if [ -z "$OTHER_FILE_ID" ] || [ "$OTHER_FILE_ID" = "null" ]; then
    fail "无法创建跨项目测试文件: $OTHER_FILE_RESP"
  else
    CROSS_RESP=$(api_post "/api/work-center/projects/${PID}/delivery-bindings:batch" \
      "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":${SEC_ID},\"deliverableTypeId\":${DOC_DT_ID},\"fileResourceIds\":[${OTHER_FILE_ID}],\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"PENDING\",\"remark\":\"test-batch6b-cross-project\"}")
    CROSS_CREATED=$(echo "$CROSS_RESP" | json_val "createdCount")
    CROSS_FAILED=$(echo "$CROSS_RESP" | json_val "failedCount")
    if [ "${CROSS_CREATED:-0}" -eq 0 ] 2>/dev/null && [ "${CROSS_FAILED:-0}" -ge 1 ] 2>/dev/null; then
      pass "跨项目文件未创建挂接记录: created=$CROSS_CREATED failed=$CROSS_FAILED"
    else
      fail "跨项目文件绑定未被正确拒绝: $CROSS_RESP"
    fi
  fi
fi

# ---- 11. Delivery package summary ----
echo ""
echo "--- 11. Delivery package summary ---"
PKG_RESP=$(api_get "/api/work-center/projects/${PID}/delivery-package/summary")

DOC_TOTAL=$(echo "$PKG_RESP" | json_val "documentSummary.totalRequired")
DWG_TOTAL=$(echo "$PKG_RESP" | json_val "drawingSummary.totalRequired")
DOC_BOUND=$(echo "$PKG_RESP" | json_val "documentSummary.boundCount")
DWG_BOUND=$(echo "$PKG_RESP" | json_val "drawingSummary.boundCount")

if [ -n "$DOC_TOTAL" ] && [ -n "$DWG_TOTAL" ]; then
  pass "交付包准备视图可用: doc(total=$DOC_TOTAL bound=$DOC_BOUND) dwg(total=$DWG_TOTAL bound=$DWG_BOUND)"
else
  fail "交付包准备视图格式不正确: $(echo $PKG_RESP | head -c 200)"
fi

# ---- 12. No NAS path leak ----
echo ""
echo "--- 12. No NAS path leak ---"
LEAK=0
for pat in "/Volumes" "smb://" "nas://" "storage_path" "storageUri" "raw row" "SQL"; do
  if echo "$PKG_RESP" | grep -qi "$pat" 2>/dev/null; then
    echo "  LEAK: found '$pat' in package summary"
    LEAK=1
  fi
done
# Also check batch responses
for resp in "$BATCH_DOC" "$BATCH_DWG" "$BATCH_REPEAT" "${WRONG_BODY:-}" "${CROSS_RESP:-}"; do
  for pat in "/Volumes" "smb://" "nas://" "storage_path" "storageUri" "raw row" "SQL"; do
    if echo "$resp" | grep -qi "$pat" 2>/dev/null; then
      echo "  LEAK: found '$pat' in batch response"
      LEAK=1
    fi
  done
done
if [ "$LEAK" -eq 0 ]; then
  pass "响应不含 NAS 路径/存储信息"
else
  fail "响应泄露了敏感路径"
fi

# ---- 13. OpenAPI ----
echo ""
echo "--- 13. OpenAPI ---"
OPENAPI_RESP=$(api_get "/v3/api-docs")
if echo "$OPENAPI_RESP" | grep -q "delivery-bindings:batch" 2>/dev/null; then
  pass "OpenAPI 包含 delivery-bindings:batch"
else
  fail "OpenAPI 缺少 delivery-bindings:batch"
fi
if echo "$OPENAPI_RESP" | grep -q "delivery-package/summary" 2>/dev/null; then
  pass "OpenAPI 包含 delivery-package/summary"
else
  fail "OpenAPI 缺少 delivery-package/summary"
fi

# ---- 14. Audit ----
echo ""
echo "--- 14. Audit ---"
AUDIT_RESP=$(api_get "/api/core/projects/${PID}/audit-logs?limit=50")
if echo "$AUDIT_RESP" | grep -q "delivery-binding.batch-create" 2>/dev/null; then
  pass "审计事件包含 batch-create 写操作"
else
  fail "审计事件缺少 batch-create"
fi

# ---- Summary ----
echo ""
echo "============================================"
echo "Phase 2 Batch 6B 验收结果: PASS=$PASS FAIL=$FAIL"
echo "============================================"

if [ "$FAIL" -gt 0 ]; then
  exit 1
else
  echo "ALL PASS"
  exit 0
fi
