#!/usr/bin/env bash
# check-phase2-batch2-standard-delivery.sh
# 二期批次二：标准驱动交付闭环最小可用版 — 验收脚本
#
# 验证：
#   1. 标准未就绪时，完整率接口返回 standardReady=false 和缺口说明
#   2. 准备部位、节点类型、交付物定义、交付物类型后，完整率出现缺失项
#   3. 创建文档交付绑定后，文档完整率提升
#   4. 创建图纸交付绑定后，图纸完整率提升
#   5. 删除绑定后，完整率回落
#   6. 普通用户不能查询非当前项目上下文完整率
#   7. 普通用户不能创建非当前项目上下文绑定
#   8. 关键写动作可查审计
#   9. 新增接口出现在 OpenAPI
#
# 使用管理员当前项目 (from /api/core/users/me) 做测试，测试数据操作后清理。

set -euo pipefail

count_matches() {
  (set +o pipefail; echo "$1" | grep -o "$2" 2>/dev/null | wc -l | tr -d ' ') || echo "0"
}

extract_int() {
  (set +o pipefail; echo "$1" | grep -o "\"$2\":[0-9]*" 2>/dev/null | head -1 | sed "s/\"$2\"://") || echo ""
}

extract_str() {
  (set +o pipefail; echo "$1" | grep -o "\"$2\":\"[^\"]*\"" 2>/dev/null | head -1 | sed "s/\"$2\":\"//;s/\"$//") || echo ""
}

# extract boolean or string value for a JSON key
extract_val() {
  local json="$1" key="$2"
  echo "$json" | python3 -c "
import sys,json
try:
    d=json.load(sys.stdin)
    data=d.get('data',d)
    v=data.get('$key','')
    if isinstance(v,bool): print('true' if v else 'false')
    else: print(v)
except: print('')
" 2>/dev/null
}

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"
REGULAR_USER="${REGULAR_USER:-delivery.engineer}"
REGULAR_PASS="${REGULAR_PASS:-Engineer@123}"

PASS=0
FAIL=0
CREATED_IDS=""  # track created resources for cleanup

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

echo "=== Phase 2 Batch 2: 标准驱动交付闭环最小可用版 — 验收 ==="
echo ""

login() {
  local user="$1" pass="$2"
  curl -fsS -X POST "$BASE_URL/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$user\",\"password\":\"$pass\"}" 2>/dev/null
}

admin_token=$(login "$ADMIN_USER" "$ADMIN_PASS" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' || echo "")
if [ -z "$admin_token" ]; then
  fail "管理员登录失败 (后端可能未运行)"
  echo "  请先启动后端: bash scripts/dev/start-backend.sh"
  exit 1
fi
pass "管理员登录成功"

regular_token=$(login "$REGULAR_USER" "$REGULAR_PASS" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' || echo "")
if [ -z "$regular_token" ]; then
  fail "普通用户登录失败"
else
  pass "普通用户登录成功"
fi

AUTH="Authorization: Bearer $admin_token"
REG_AUTH="Authorization: Bearer $regular_token"

# Get admin current project
admin_me=$(curl -fsS "$BASE_URL/api/core/users/me" -H "$AUTH" 2>/dev/null || echo '{}')
PID=$(extract_int "$admin_me" "\"id\"" | tail -1)
# The "currentProject" is nested; extract its id
PID=$(echo "$admin_me" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['currentProject']['id'])" 2>/dev/null || echo "1")

if [ -z "$PID" ] || [ "$PID" -le 0 ] 2>/dev/null; then
  PID=1
  echo "  [INFO] 使用默认项目 ID=1"
else
  echo "  [INFO] 管理员当前项目 ID=$PID"
fi

# ---------- 1. 后端健康检查 ----------
echo ""
echo "--- 1. 后端健康检查 ---"
health=$(curl -fsS "$BASE_URL/actuator/health" 2>/dev/null || echo '{"status":"DOWN"}')
if echo "$health" | grep -q '"status":"UP"'; then
  pass "后端健康检查通过"
else
  fail "后端健康检查失败: $health"
fi

# ---------- 2. OpenAPI 检查 ----------
echo ""
echo "--- 2. OpenAPI 检查 ---"
openapi=$(curl -fsS "$BASE_URL/v3/api-docs" 2>/dev/null || echo '{}')

for path in '/api/work-center/projects/{projectId}/delivery-completeness' \
  '/api/work-center/projects/{projectId}/delivery-bindings' \
  '/api/work-center/projects/{projectId}/delivery-views'; do
  if echo "$openapi" | grep -q "$path"; then
    pass "OpenAPI 包含 $path"
  else
    fail "OpenAPI 缺少 $path"
  fi
done

# ---------- 3. 标准未就绪时完整率返回缺口 ----------
echo ""
echo "--- 3. 标准未就绪时完整率返回缺口 ---"
comp_empty=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DOCUMENT" -H "$AUTH" 2>/dev/null || echo '{}')

std_ready=$(extract_val "$comp_empty" "standardReady")
if [ "$std_ready" = "false" ]; then
  pass "标准未就绪时 standardReady=false"
else
  fail "标准未就绪时 standardReady=$std_ready (expected false)"
fi

if echo "$comp_empty" | grep -q "缺少"; then
  pass "标准未就绪时返回缺口说明 (中文)"
else
  fail "标准未就绪时未返回缺口说明: $comp_empty"
fi

http_empty=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DOCUMENT" -H "$AUTH" 2>/dev/null || echo "000")
if [ "$http_empty" = "200" ]; then
  pass "标准未就绪时返回 HTTP 200 (非 500)"
else
  fail "标准未就绪时返回 HTTP $http_empty (expected 200)"
fi

# ---------- 4. 准备标准数据 ----------
echo ""
echo "--- 4. 准备标准数据 (在项目 $PID 下) ---"

# Clean up any previous PH2B2 test data in this project
echo "  [INFO] 清理历史测试数据..."
# (best-effort, ignore errors)

# 4a. 创建部位树节点
sn_code="PH2B2-SEC-$(date +%s)"
sn_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/section-nodes" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"code\":\"$sn_code\",\"name\":\"批次二专项测试-主体结构\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
SN_ID=$(extract_int "$sn_resp" "id")
if [ -n "$SN_ID" ] && [ "$SN_ID" -gt 0 ] 2>/dev/null; then
  pass "部位节点已创建 (id=$SN_ID)"
else
  fail "部位节点创建失败: $sn_resp"
fi

# 4b. 复用或创建节点类型
# 节点类型锁定后当前没有删除/解锁接口，脚本复跑时优先复用已有节点类型，避免堆积测试主数据。
node_types_resp=$(curl -fsS "$BASE_URL/api/master-data/projects/$PID/node-types" -H "$AUTH" 2>/dev/null || echo '{}')
NT_ID=$(echo "$node_types_resp" | python3 -c "
import json, sys
try:
    rows = json.load(sys.stdin).get('data') or []
except Exception:
    rows = []
preferred = [row for row in rows if str(row.get('code', '')).startswith('PH2B2-')]
pool = preferred or rows
print(pool[0].get('id', '') if pool else '')
" 2>/dev/null || echo "")

if [ -n "$NT_ID" ] && [ "$NT_ID" -gt 0 ] 2>/dev/null; then
  pass "节点类型可用，复用已有节点类型 (id=$NT_ID)"
else
  nt_code="PH2B2-NT-$(date +%s)"
  nt_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/node-types" \
    -H "$AUTH" -H 'Content-Type: application/json' \
    -d "{\"code\":\"$nt_code\",\"name\":\"批次二专项测试节点\",\"scopeLevel\":1,\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
  NT_ID=$(extract_int "$nt_resp" "id")
  if [ -n "$NT_ID" ] && [ "$NT_ID" -gt 0 ] 2>/dev/null; then
    pass "节点类型已创建 (id=$NT_ID)"
  else
    fail "节点类型创建失败: $nt_resp"
  fi
fi

# 4c. 锁定节点类型
lock_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/node-types:lock" \
  -H "$AUTH" -H 'Content-Type: application/json' -d '{}' 2>/dev/null || echo '{}')
all_locked=$(extract_val "$lock_resp" "allNodeTypesLocked")
if [ "$all_locked" = "true" ]; then
  pass "节点类型已锁定"
else
  fail "节点类型锁定失败 (allNodeTypesLocked=$all_locked): $lock_resp"
fi

# 4d. 创建交付物定义
dd_code="PH2B2-DD-$(date +%s)"
dd_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/deliverable-definitions" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"nodeTypeId\":$NT_ID,\"code\":\"$dd_code\",\"name\":\"批次二专项-施工图\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
DD_ID=$(extract_int "$dd_resp" "id")
if [ -n "$DD_ID" ] && [ "$DD_ID" -gt 0 ] 2>/dev/null; then
  pass "交付物定义已创建 (id=$DD_ID)"
else
  fail "交付物定义创建失败: $dd_resp"
fi

# 4e. 创建文档交付物类型
dt_doc_code="PH2B2-DT-DOC-$(date +%s)"
dt_doc_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/deliverable-types" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"deliverableDefinitionId\":$DD_ID,\"code\":\"$dt_doc_code\",\"name\":\"批次二专项-施工图PDF\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
DT_DOC_ID=$(extract_int "$dt_doc_resp" "id")
if [ -n "$DT_DOC_ID" ] && [ "$DT_DOC_ID" -gt 0 ] 2>/dev/null; then
  pass "文档交付物类型已创建 (id=$DT_DOC_ID)"
else
  fail "文档交付物类型创建失败: $dt_doc_resp"
fi

# 4f. 创建图纸交付物类型
dt_dwg_code="PH2B2-DT-DWG-$(date +%s)"
dt_dwg_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/deliverable-types" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"deliverableDefinitionId\":$DD_ID,\"code\":\"$dt_dwg_code\",\"name\":\"批次二专项-施工图DWG\",\"fileKind\":\"DRAWING\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
DT_DWG_ID=$(extract_int "$dt_dwg_resp" "id")
if [ -n "$DT_DWG_ID" ] && [ "$DT_DWG_ID" -gt 0 ] 2>/dev/null; then
  pass "图纸交付物类型已创建 (id=$DT_DWG_ID)"
else
  fail "图纸交付物类型创建失败: $dt_dwg_resp"
fi

# 4g. 创建目录模板
tmpl_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/directory-templates" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"templateType\":\"DOCUMENT\",\"name\":\"批次二专项-标准文档目录\",\"sourceType\":\"MANUAL\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
TMPL_ID=$(extract_int "$tmpl_resp" "id")
if [ -n "$TMPL_ID" ] && [ "$TMPL_ID" -gt 0 ] 2>/dev/null; then
  pass "目录模板已创建 (id=$TMPL_ID)"
else
  fail "目录模板创建失败: $tmpl_resp"
fi

# 4h. 创建文件资源 (文档)
file_doc_resp=$(curl -fsS -X POST "$BASE_URL/api/data-steward/projects/$PID/file-resources" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"originalName\":\"ph2b2-测试结构施工图.pdf\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":102400,\"storageUri\":\"nas:///tmp/ph2b2-test-doc-$PID.pdf\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}" 2>/dev/null || echo '{}')
FILE_DOC_ID=$(extract_int "$file_doc_resp" "id")
if [ -n "$FILE_DOC_ID" ] && [ "$FILE_DOC_ID" -gt 0 ] 2>/dev/null; then
  pass "文档文件资源已创建 (id=$FILE_DOC_ID)"
else
  fail "文档文件资源创建失败: $file_doc_resp"
fi

# 4i. 创建文件资源 (图纸)
file_dwg_resp=$(curl -fsS -X POST "$BASE_URL/api/data-steward/projects/$PID/file-resources" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"originalName\":\"ph2b2-测试结构施工图.dwg\",\"fileKind\":\"DRAWING\",\"mimeType\":\"application/acad\",\"sizeBytes\":204800,\"storageUri\":\"nas:///tmp/ph2b2-test-dwg-$PID.dwg\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}" 2>/dev/null || echo '{}')
FILE_DWG_ID=$(extract_int "$file_dwg_resp" "id")
if [ -n "$FILE_DWG_ID" ] && [ "$FILE_DWG_ID" -gt 0 ] 2>/dev/null; then
  pass "图纸文件资源已创建 (id=$FILE_DWG_ID)"
else
  fail "图纸文件资源创建失败: $file_dwg_resp"
fi

# ---------- 5. 标准就绪后完整率出现缺失项 ----------
echo ""
echo "--- 5. 标准就绪后完整率出现缺失项 ---"
comp_ready=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DOCUMENT" -H "$AUTH" 2>/dev/null || echo '{}')

std_ready2=$(extract_val "$comp_ready" "standardReady")
total_req=$(extract_int "$comp_ready" "totalRequired")
missing_cnt=$(extract_int "$comp_ready" "missingCount")

if [ "$std_ready2" = "true" ]; then pass "标准就绪后 standardReady=true"
else fail "标准就绪后 standardReady=$std_ready2 (expected true)"; fi

if [ -n "$total_req" ] && [ "$total_req" -gt 0 ] 2>/dev/null; then
  pass "标准就绪后 totalRequired=$total_req (>0)"
else
  fail "标准就绪后 totalRequired=$total_req (expected >0)"
fi

if [ -n "$missing_cnt" ] && [ "$missing_cnt" -gt 0 ] 2>/dev/null; then
  pass "标准就绪后 missingCount=$missing_cnt (>0，即存在缺失项)"
else
  fail "标准就绪后 missingCount=$missing_cnt (expected >0)"
fi

# Check onlyMissing filter
comp_miss=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DOCUMENT&onlyMissing=true" -H "$AUTH" 2>/dev/null || echo '{}')
miss_rows=$(count_matches "$comp_miss" '"completed":false')
if [ "$miss_rows" -gt 0 ]; then
  pass "onlyMissing=true 正确过滤出缺失项"
else
  fail "onlyMissing=true 未返回缺失项"
fi

# ---------- 6. 创建文档交付绑定后完整率提升 ----------
echo ""
echo "--- 6. 创建文档交付绑定后完整率提升 ---"
missing_before=$(extract_int "$comp_ready" "missingCount")

bind_doc_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/delivery-bindings" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":$SN_ID,\"deliverableTypeId\":$DT_DOC_ID,\"fileResourceId\":$FILE_DOC_ID,\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"PENDING\"}" 2>/dev/null || echo '{}')
BIND_DOC_ID=$(extract_int "$bind_doc_resp" "id")
if [ -n "$BIND_DOC_ID" ] && [ "$BIND_DOC_ID" -gt 0 ] 2>/dev/null; then
  pass "文档交付绑定已创建 (id=$BIND_DOC_ID)"
else
  fail "文档交付绑定创建失败: $bind_doc_resp"
fi

comp_after_doc=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DOCUMENT" -H "$AUTH" 2>/dev/null || echo '{}')
missing_after_doc=$(extract_int "$comp_after_doc" "missingCount")
completed_after_doc=$(extract_int "$comp_after_doc" "completedCount")

if [ -n "$completed_after_doc" ] && [ "$completed_after_doc" -gt 0 ] 2>/dev/null; then
  pass "文档绑定后 completedCount=$completed_after_doc (>0)"
else
  fail "文档绑定后 completedCount=$completed_after_doc (expected >0)"
fi

if [ -n "$missing_after_doc" ] && [ -n "$missing_before" ] && [ "$missing_after_doc" -lt "$missing_before" ] 2>/dev/null; then
  pass "文档绑定后缺失数减少 ($missing_before -> $missing_after_doc)"
else
  fail "文档绑定后缺失数未减少 ($missing_before -> $missing_after_doc)"
fi

# ---------- 7. 创建图纸交付绑定后完整率提升 ----------
echo ""
echo "--- 7. 创建图纸交付绑定后完整率提升 ---"
comp_dwg_before=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DRAWING" -H "$AUTH" 2>/dev/null || echo '{}')
missing_dwg_before=$(extract_int "$comp_dwg_before" "missingCount")

bind_dwg_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/delivery-bindings" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"viewType\":\"DRAWING\",\"sectionNodeId\":$SN_ID,\"deliverableTypeId\":$DT_DWG_ID,\"fileResourceId\":$FILE_DWG_ID,\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"PENDING\"}" 2>/dev/null || echo '{}')
BIND_DWG_ID=$(extract_int "$bind_dwg_resp" "id")
if [ -n "$BIND_DWG_ID" ] && [ "$BIND_DWG_ID" -gt 0 ] 2>/dev/null; then
  pass "图纸交付绑定已创建 (id=$BIND_DWG_ID)"
else
  fail "图纸交付绑定创建失败: $bind_dwg_resp"
fi

comp_after_dwg=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DRAWING" -H "$AUTH" 2>/dev/null || echo '{}')
missing_after_dwg=$(extract_int "$comp_after_dwg" "missingCount")
completed_after_dwg=$(extract_int "$comp_after_dwg" "completedCount")

if [ -n "$completed_after_dwg" ] && [ "$completed_after_dwg" -gt 0 ] 2>/dev/null; then
  pass "图纸绑定后 completedCount=$completed_after_dwg (>0)"
else
  fail "图纸绑定后 completedCount=$completed_after_dwg (expected >0)"
fi

if [ -n "$missing_after_dwg" ] && [ -n "$missing_dwg_before" ] && [ "$missing_after_dwg" -lt "$missing_dwg_before" ] 2>/dev/null; then
  pass "图纸绑定后缺失数减少 ($missing_dwg_before -> $missing_after_dwg)"
else
  fail "图纸绑定后缺失数未减少 ($missing_dwg_before -> $missing_after_dwg)"
fi

# ---------- 8. 删除绑定后完整率回落 ----------
echo ""
echo "--- 8. 删除绑定后完整率回落 ---"
if [ -n "${BIND_DOC_ID:-}" ] && [ "${BIND_DOC_ID:-0}" -gt 0 ] 2>/dev/null; then
  missing_before_del=$(extract_int "$comp_after_doc" "missingCount")

  del_http=$(curl -sS -o /dev/null -w "%{http_code}" -X DELETE \
    "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/$BIND_DOC_ID" \
    -H "$AUTH" 2>/dev/null || echo "000")

  if [ "$del_http" = "200" ]; then pass "删除绑定返回 HTTP 200"
  else fail "删除绑定返回 HTTP $del_http (expected 200)"; fi

  comp_after_del=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DOCUMENT" -H "$AUTH" 2>/dev/null || echo '{}')
  missing_after_del=$(extract_int "$comp_after_del" "missingCount")

  if [ -n "$missing_after_del" ] && [ -n "$missing_before_del" ] && [ "$missing_after_del" -gt "$missing_before_del" ] 2>/dev/null; then
    pass "删除绑定后缺失数回升 ($missing_before_del -> $missing_after_del)"
  else
    echo "  [INFO] missing_before_del=$missing_before_del missing_after_del=$missing_after_del"
    fail "删除绑定后缺失数未回升"
  fi
fi

# ---------- 9. 权限隔离 ----------
echo ""
echo "--- 9. 权限隔离：普通用户跨项目上下文测试 ---"

# Both admin and engineer have current project 1, so both can access PID
# For isolation test: use a different project ID that the engineer's token doesn't match
OTHER_PID=99999
reg_other_http=$(curl -sS -o /dev/null -w "%{http_code}" \
  "$BASE_URL/api/work-center/projects/$OTHER_PID/delivery-completeness?viewType=DOCUMENT" \
  -H "$REG_AUTH" 2>/dev/null || echo "000")

if [ "$reg_other_http" = "403" ] || [ "$reg_other_http" = "404" ]; then
  pass "普通用户无法查询非当前项目 ($OTHER_PID) 完整率 (HTTP $reg_other_http)"
else
  fail "普通用户越权查询了非当前项目完整率 (HTTP $reg_other_http)"
fi

reg_bind_other_http=$(curl -sS -o /dev/null -w "%{http_code}" -X POST \
  "$BASE_URL/api/work-center/projects/$OTHER_PID/delivery-bindings" \
  -H "$REG_AUTH" -H 'Content-Type: application/json' \
  -d '{"viewType":"DOCUMENT","sectionNodeId":1,"deliverableTypeId":1,"fileResourceId":1}' 2>/dev/null || echo "000")

if [ "$reg_bind_other_http" = "403" ] || [ "$reg_bind_other_http" = "404" ]; then
  pass "普通用户无法创建非当前项目绑定 (HTTP $reg_bind_other_http)"
else
  fail "普通用户越权创建了非当前项目绑定 (HTTP $reg_bind_other_http)"
fi

# ---------- 10. 审计验证 ----------
echo ""
echo "--- 10. 审计验证 ---"
audit_events=$(curl -fsS "$BASE_URL/api/core/projects/$PID/audit-logs?limit=50" -H "$AUTH" 2>/dev/null || echo '[]')

create_binding_audit=$(count_matches "$audit_events" "delivery-binding.create")
delete_binding_audit=$(count_matches "$audit_events" "delivery-binding.delete")

if [ "$create_binding_audit" -gt 0 ]; then
  pass "创建绑定写入审计 ($create_binding_audit 条)"
else
  fail "创建绑定未写入审计"
fi

if [ -n "${BIND_DOC_ID:-}" ] && [ "${BIND_DOC_ID:-0}" -gt 0 ] 2>/dev/null; then
  if [ "$delete_binding_audit" -gt 0 ]; then
    pass "删除绑定写入审计 ($delete_binding_audit 条)"
  else
    fail "删除绑定未写入审计"
  fi
fi

# ---------- 11. 文档和图纸两条链路独立 ----------
echo ""
echo "--- 11. 文档/图纸链路独立性 ---"
comp_doc_final=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DOCUMENT" -H "$AUTH" 2>/dev/null || echo '{}')
comp_dwg_final=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DRAWING" -H "$AUTH" 2>/dev/null || echo '{}')

doc_std_ready=$(extract_val "$comp_doc_final" "standardReady")
dwg_std_ready=$(extract_val "$comp_dwg_final" "standardReady")
doc_total=$(extract_int "$comp_doc_final" "totalRequired")
dwg_total=$(extract_int "$comp_dwg_final" "totalRequired")

if [ "$doc_std_ready" = "true" ] && [ "$dwg_std_ready" = "true" ]; then
  pass "文档和图纸链路标准均已就绪"
else
  fail "标准就绪不一致 (DOC=$doc_std_ready, DWG=$dwg_std_ready)"
fi

if [ "$doc_total" -gt 0 ] && [ "$dwg_total" -gt 0 ]; then
  pass "文档($doc_total)/图纸($dwg_total) 两条链路独立计算"
else
  fail "文档/图纸链路计算有误 (DOC=$doc_total, DWG=$dwg_total)"
fi

# ---------- 12. targetType=OBJECT 不500 ----------
echo ""
echo "--- 12. targetType=OBJECT 空结果不500 ---"
comp_obj=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DOCUMENT&targetType=OBJECT" -H "$AUTH" 2>/dev/null || echo '{}')
obj_http=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/api/work-center/projects/$PID/delivery-completeness?viewType=DOCUMENT&targetType=OBJECT" -H "$AUTH" 2>/dev/null || echo "000")
obj_ready=$(extract_val "$comp_obj" "standardReady")

if [ "$obj_http" = "200" ]; then
  pass "targetType=OBJECT 返回 HTTP 200 (非 500)"
else
  fail "targetType=OBJECT 返回 HTTP $obj_http (expected 200)"
fi

# ---------- Cleanup test data ----------
echo ""
echo "--- 清理测试数据 ---"
cleanup_failures=0

delete_if_id() {
  local label="$1" url="$2" id="$3"
  if [ -n "$id" ] && [ "$id" -gt 0 ] 2>/dev/null; then
    local code
    code=$(curl -sS -o /dev/null -w "%{http_code}" -X DELETE "$url" -H "$AUTH" 2>/dev/null || echo "000")
    if [ "$code" = "200" ]; then
      echo "  [OK] 已删除 $label (id=$id)"
    else
      echo "  [WARN] 删除 $label 返回 HTTP $code (id=$id)"
      cleanup_failures=$((cleanup_failures + 1))
    fi
  fi
}

# Delete remaining binding (DWG)
delete_if_id "图纸绑定" "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/${BIND_DWG_ID:-0}" "${BIND_DWG_ID:-0}"

# Delete master data in reverse dependency order
delete_if_id "目录模板" "$BASE_URL/api/master-data/projects/$PID/directory-templates/${TMPL_ID:-0}" "${TMPL_ID:-0}"
delete_if_id "文档交付类型" "$BASE_URL/api/master-data/projects/$PID/deliverable-types/${DT_DOC_ID:-0}" "${DT_DOC_ID:-0}"
delete_if_id "图纸交付类型" "$BASE_URL/api/master-data/projects/$PID/deliverable-types/${DT_DWG_ID:-0}" "${DT_DWG_ID:-0}"
delete_if_id "交付物定义" "$BASE_URL/api/master-data/projects/$PID/deliverable-definitions/${DD_ID:-0}" "${DD_ID:-0}"
delete_if_id "文件资源(文档)" "$BASE_URL/api/data-steward/projects/$PID/file-resources/${FILE_DOC_ID:-0}" "${FILE_DOC_ID:-0}"
delete_if_id "文件资源(图纸)" "$BASE_URL/api/data-steward/projects/$PID/file-resources/${FILE_DWG_ID:-0}" "${FILE_DWG_ID:-0}"
delete_if_id "部位节点" "$BASE_URL/api/master-data/projects/$PID/section-nodes/${SN_ID:-0}" "${SN_ID:-0}"

if [ "$cleanup_failures" -eq 0 ]; then
  pass "测试数据已清理 ($cleanup_failures 项失败)"
else
  echo "  [INFO] $cleanup_failures 项测试数据清理失败 (不影响主流程)"
fi

# ---------- Summary ----------
echo ""
echo "=== 验收总结 ==="
echo "通过: $PASS"
echo "失败: $FAIL"
echo ""

if [ "$FAIL" -gt 0 ]; then
  echo "[FAIL] 二期批次二验收未通过"
  exit 1
else
  echo "[PASS] 二期批次二验收通过"
fi
