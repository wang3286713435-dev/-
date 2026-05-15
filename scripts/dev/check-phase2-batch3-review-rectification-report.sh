#!/usr/bin/env bash
# check-phase2-batch3-review-rectification-report.sh
# 二期批次三：人工审核、整改闭环与基础报表导出 — 验收脚本
#
# 验证:
#   1. 创建最小标准和文件
#   2. 创建交付绑定
#   3. 提交审核 -> 状态变为 PENDING
#   4. 审核通过 -> 状态变为 APPROVED 并写审计
#   5. 另一条绑定驳回 -> 状态变为 REJECTED 并生成整改项
#   6. 整改项标记已处理 -> RESOLVED
#   7. 整改项关闭 -> CLOSED
#   8. 整改项重新打开 -> REOPENED
#   9. 三类 CSV 导出可下载且包含核心字段
#  10. 普通用户不能跨项目审核、整改或导出
#  11. OpenAPI 包含新增接口
#  12. 测试数据尽量清理

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
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin@123}"
REGULAR_USER="${REGULAR_USER:-delivery.engineer}"
REGULAR_PASS="${REGULAR_PASS:-Engineer@123}"

PASS=0
FAIL=0
CREATED_IDS=""

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

echo "=== Phase 2 Batch 3: 人工审核、整改闭环与基础报表导出 — 验收 ==="
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

admin_me=$(curl -fsS "$BASE_URL/api/core/users/me" -H "$AUTH" 2>/dev/null || echo '{}')
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

for path in \
  '/api/work-center/projects/{projectId}/delivery-bindings/{bindingId}:submit-review' \
  '/api/work-center/projects/{projectId}/delivery-bindings/{bindingId}:approve' \
  '/api/work-center/projects/{projectId}/delivery-bindings/{bindingId}:reject' \
  '/api/work-center/projects/{projectId}/delivery-bindings/{bindingId}/review-records' \
  '/api/work-center/projects/{projectId}/rectifications' \
  '/api/work-center/projects/{projectId}/rectifications/{rectificationId}' \
  '/api/work-center/projects/{projectId}/rectifications/{rectificationId}:resolve' \
  '/api/work-center/projects/{projectId}/rectifications/{rectificationId}:close' \
  '/api/work-center/projects/{projectId}/rectifications/{rectificationId}:reopen' \
  '/api/work-center/projects/{projectId}/reports/delivery-completeness.csv' \
  '/api/work-center/projects/{projectId}/reports/review-summary.csv' \
  '/api/work-center/projects/{projectId}/reports/rectifications.csv'; do
  if echo "$openapi" | grep -q "$path"; then
    pass "OpenAPI 包含 $path"
  else
    fail "OpenAPI 缺少 $path"
  fi
done

# ---------- 3. 准备标准数据 ----------
echo ""
echo "--- 3. 准备标准数据 ---"
echo "  [INFO] 复用或创建标准数据用于测试"

# 3a. 创建部位树节点
sn_code="PH2B3-SEC-$(date +%s)"
sn_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/section-nodes" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"code\":\"$sn_code\",\"name\":\"批次三专项测试-审核部位\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
SN_ID=$(extract_int "$sn_resp" "id")
if [ -n "$SN_ID" ] && [ "$SN_ID" -gt 0 ] 2>/dev/null; then
  pass "部位节点已创建 (id=$SN_ID)"
else
  fail "部位节点创建失败: $sn_resp"
fi

# 3b. 复用或创建节点类型
node_types_resp=$(curl -fsS "$BASE_URL/api/master-data/projects/$PID/node-types" -H "$AUTH" 2>/dev/null || echo '{}')
NT_ID=$(echo "$node_types_resp" | python3 -c "
import json, sys
try:
    rows = json.load(sys.stdin).get('data') or []
except Exception:
    rows = []
preferred = [row for row in rows if str(row.get('code', '')).startswith('PH2B3-')]
pool = preferred or rows
print(pool[0].get('id', '') if pool else '')
" 2>/dev/null || echo "")

if [ -n "$NT_ID" ] && [ "$NT_ID" -gt 0 ] 2>/dev/null; then
  pass "节点类型可用，复用已有节点类型 (id=$NT_ID)"
else
  nt_code="PH2B3-NT-$(date +%s)"
  nt_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/node-types" \
    -H "$AUTH" -H 'Content-Type: application/json' \
    -d "{\"code\":\"$nt_code\",\"name\":\"批次三专项测试节点\",\"scopeLevel\":1,\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
  NT_ID=$(extract_int "$nt_resp" "id")
  if [ -n "$NT_ID" ] && [ "$NT_ID" -gt 0 ] 2>/dev/null; then
    pass "节点类型已创建 (id=$NT_ID)"
  else
    fail "节点类型创建失败: $nt_resp"
  fi
fi

# 3c. 锁定节点类型
lock_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/node-types:lock" \
  -H "$AUTH" -H 'Content-Type: application/json' -d '{}' 2>/dev/null || echo '{}')
all_locked=$(extract_val "$lock_resp" "allNodeTypesLocked")
if [ "$all_locked" = "true" ]; then
  pass "节点类型已锁定"
else
  fail "节点类型锁定失败 (allNodeTypesLocked=$all_locked)"
fi

# 3d. 创建交付物定义
dd_code="PH2B3-DD-$(date +%s)"
dd_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/deliverable-definitions" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"nodeTypeId\":$NT_ID,\"code\":\"$dd_code\",\"name\":\"批次三专项-审核测试图\",\"category\":\"DOCUMENT\",\"required\":true,\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
DD_ID=$(extract_int "$dd_resp" "id")
if [ -n "$DD_ID" ] && [ "$DD_ID" -gt 0 ] 2>/dev/null; then
  pass "交付物定义已创建 (id=$DD_ID)"
else
  fail "交付物定义创建失败: $dd_resp"
fi

# 3e. 创建文档交付物类型
dt_doc_code="PH2B3-DT-DOC-$(date +%s)"
dt_doc_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/deliverable-types" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"deliverableDefinitionId\":$DD_ID,\"code\":\"$dt_doc_code\",\"name\":\"批次三专项-审核PDF\",\"fileKind\":\"DOCUMENT\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
DT_DOC_ID=$(extract_int "$dt_doc_resp" "id")
if [ -n "$DT_DOC_ID" ] && [ "$DT_DOC_ID" -gt 0 ] 2>/dev/null; then
  pass "文档交付物类型已创建 (id=$DT_DOC_ID)"
else
  fail "文档交付物类型创建失败: $dt_doc_resp"
fi

# 3f. 创建图纸交付物类型
dt_dwg_code="PH2B3-DT-DWG-$(date +%s)"
dt_dwg_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/deliverable-types" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"deliverableDefinitionId\":$DD_ID,\"code\":\"$dt_dwg_code\",\"name\":\"批次三专项-审核DWG\",\"fileKind\":\"DRAWING\",\"bindingStrategy\":\"SECTION_NODE\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
DT_DWG_ID=$(extract_int "$dt_dwg_resp" "id")
if [ -n "$DT_DWG_ID" ] && [ "$DT_DWG_ID" -gt 0 ] 2>/dev/null; then
  pass "图纸交付物类型已创建 (id=$DT_DWG_ID)"
else
  fail "图纸交付物类型创建失败: $dt_dwg_resp"
fi

# 3g. 创建目录模板
tmpl_resp=$(curl -fsS -X POST "$BASE_URL/api/master-data/projects/$PID/directory-templates" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"templateType\":\"DOCUMENT\",\"name\":\"批次三专项-审核标准目录\",\"sourceType\":\"MANUAL\",\"sortOrder\":9999,\"status\":\"ACTIVE\"}" 2>/dev/null || echo '{}')
TMPL_ID=$(extract_int "$tmpl_resp" "id")
if [ -n "$TMPL_ID" ] && [ "$TMPL_ID" -gt 0 ] 2>/dev/null; then
  pass "目录模板已创建 (id=$TMPL_ID)"
else
  fail "目录模板创建失败: $tmpl_resp"
fi

# 3h. 创建文件资源 (两份，用于两条绑定)
file1_resp=$(curl -fsS -X POST "$BASE_URL/api/data-steward/projects/$PID/file-resources" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"originalName\":\"ph2b3-审核通过测试文件.pdf\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":102400,\"storageUri\":\"nas:///tmp/ph2b3-approve-$PID.pdf\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}" 2>/dev/null || echo '{}')
FILE1_ID=$(extract_int "$file1_resp" "id")
if [ -n "$FILE1_ID" ] && [ "$FILE1_ID" -gt 0 ] 2>/dev/null; then
  pass "文件资源1已创建 (id=$FILE1_ID)"
else
  fail "文件资源1创建失败: $file1_resp"
fi

file2_resp=$(curl -fsS -X POST "$BASE_URL/api/data-steward/projects/$PID/file-resources" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"originalName\":\"ph2b3-审核驳回测试文件.pdf\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":102400,\"storageUri\":\"nas:///tmp/ph2b3-reject-$PID.pdf\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}" 2>/dev/null || echo '{}')
FILE2_ID=$(extract_int "$file2_resp" "id")
if [ -n "$FILE2_ID" ] && [ "$FILE2_ID" -gt 0 ] 2>/dev/null; then
  pass "文件资源2已创建 (id=$FILE2_ID)"
else
  fail "文件资源2创建失败: $file2_resp"
fi

# ---------- 4. 创建两条交付绑定 ----------
echo ""
echo "--- 4. 创建两条交付绑定 ---"

bind1_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/delivery-bindings" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":$SN_ID,\"deliverableTypeId\":$DT_DOC_ID,\"fileResourceId\":$FILE1_ID,\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"DRAFT\"}" 2>/dev/null || echo '{}')
BIND1_ID=$(extract_int "$bind1_resp" "id")
if [ -n "$BIND1_ID" ] && [ "$BIND1_ID" -gt 0 ] 2>/dev/null; then
  pass "交付绑定1已创建 (id=$BIND1_ID, reviewStatus=DRAFT)"
else
  fail "交付绑定1创建失败: $bind1_resp"
fi

bind2_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/delivery-bindings" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"viewType\":\"DOCUMENT\",\"sectionNodeId\":$SN_ID,\"deliverableTypeId\":$DT_DOC_ID,\"fileResourceId\":$FILE2_ID,\"bindingStatus\":\"BOUND\",\"reviewStatus\":\"DRAFT\"}" 2>/dev/null || echo '{}')
BIND2_ID=$(extract_int "$bind2_resp" "id")
if [ -n "$BIND2_ID" ] && [ "$BIND2_ID" -gt 0 ] 2>/dev/null; then
  pass "交付绑定2已创建 (id=$BIND2_ID, reviewStatus=DRAFT)"
else
  fail "交付绑定2创建失败: $bind2_resp"
fi

# ---------- 5. 提交审核 ----------
echo ""
echo "--- 5. 提交审核 ---"

submit1_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/$BIND1_ID:submit-review" \
  -H "$AUTH" 2>/dev/null || echo '{}')
rv1=$(extract_val "$submit1_resp" "reviewStatus")
if [ "$rv1" = "PENDING" ]; then
  pass "绑定1提交审核后 reviewStatus=PENDING"
else
  fail "绑定1提交审核后 reviewStatus=$rv1 (expected PENDING)"
fi

submit2_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/$BIND2_ID:submit-review" \
  -H "$AUTH" 2>/dev/null || echo '{}')
rv2=$(extract_val "$submit2_resp" "reviewStatus")
if [ "$rv2" = "PENDING" ]; then
  pass "绑定2提交审核后 reviewStatus=PENDING"
else
  fail "绑定2提交审核后 reviewStatus=$rv2 (expected PENDING)"
fi

# ---------- 6. 审核通过 ----------
echo ""
echo "--- 6. 审核通过 ---"

approve_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/$BIND1_ID:approve" \
  -H "$AUTH" 2>/dev/null || echo '{}')
av=$(extract_val "$approve_resp" "reviewStatus")
if [ "$av" = "APPROVED" ]; then
  pass "绑定1审核通过后 reviewStatus=APPROVED"
else
  fail "绑定1审核通过后 reviewStatus=$av (expected APPROVED)"
fi

# Check review records
records_resp=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/$BIND1_ID/review-records" \
  -H "$AUTH" 2>/dev/null || echo '[]')
submit_count=$(count_matches "$records_resp" '"action":"SUBMITTED"')
approve_count=$(count_matches "$records_resp" '"action":"APPROVED"')
if [ "$submit_count" -gt 0 ] && [ "$approve_count" -gt 0 ]; then
  pass "审核记录包含提交和通过记录 (SUBMITTED=$submit_count, APPROVED=$approve_count)"
else
  fail "审核记录不完整: SUBMITTED=$submit_count, APPROVED=$approve_count"
fi

# ---------- 7. 审核驳回并生成整改项 ----------
echo ""
echo "--- 7. 审核驳回并生成整改项 ---"

reject_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/$BIND2_ID:reject" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"reason":"测试驳回：文件版本不符合交付标准，请更新到最新版次"}' 2>/dev/null || echo '{}')
rv_reject=$(extract_val "$reject_resp" "reviewStatus")
if [ "$rv_reject" = "REJECTED" ]; then
  pass "绑定2驳回后 reviewStatus=REJECTED"
else
  fail "绑定2驳回后 reviewStatus=$rv_reject (expected REJECTED)"
fi

# Check that rectification was created
rects_resp=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/rectifications" -H "$AUTH" 2>/dev/null || echo '[]')
RECT_ID=$(echo "$rects_resp" | python3 -c "
import sys, json
try:
    rows = json.load(sys.stdin).get('data') or []
    for r in rows:
        if r.get('bindingId') == ${BIND2_ID}:
            print(r['id'])
            break
    else:
        print('')
except: print('')
" 2>/dev/null || echo "")

if [ -n "$RECT_ID" ] && [ "$RECT_ID" -gt 0 ] 2>/dev/null; then
  pass "驳回后生成整改项 (rectId=$RECT_ID, bindingId=$BIND2_ID)"
else
  fail "驳回后未生成整改项: $rects_resp"
fi

# Check reject without reason fails
reject_no_reason_code=$(curl -sS -o /dev/null -w "%{http_code}" -X POST \
  "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/$BIND1_ID:reject" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"reason":""}' 2>/dev/null || echo "000")
if [ "$reject_no_reason_code" = "400" ]; then
  pass "驳回无原因返回 HTTP 400"
else
  fail "驳回无原因返回 HTTP $reject_no_reason_code (expected 400)"
fi

# ---------- 8. 整改项状态闭环 ----------
echo ""
echo "--- 8. 整改项状态闭环 ---"

# 8a. 标记已处理
resolve_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/rectifications/$RECT_ID:resolve" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"resolutionNote":"文件版本已更新至V2，重新上传完成"}' 2>/dev/null || echo '{}')
resolve_status=$(extract_val "$resolve_resp" "status")
if [ "$resolve_status" = "RESOLVED" ]; then
  pass "整改项标记已处理 -> RESOLVED"
else
  fail "整改项标记已处理后 status=$resolve_status (expected RESOLVED)"
fi

# resolve without note fails
resolve_no_note_code=$(curl -sS -o /dev/null -w "%{http_code}" -X POST \
  "$BASE_URL/api/work-center/projects/$PID/rectifications/$RECT_ID:resolve" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"resolutionNote":""}' 2>/dev/null || echo "000")
if [ "$resolve_no_note_code" = "400" ]; then
  pass "标记已处理无说明返回 HTTP 400"
else
  fail "标记已处理无说明返回 HTTP $resolve_no_note_code (expected 400)"
fi

# 8b. 关闭
close_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/rectifications/$RECT_ID:close" \
  -H "$AUTH" 2>/dev/null || echo '{}')
close_status=$(extract_val "$close_resp" "status")
if [ "$close_status" = "CLOSED" ]; then
  pass "整改项关闭 -> CLOSED"
else
  fail "整改项关闭后 status=$close_status (expected CLOSED)"
fi

# 8c. 重新打开
reopen_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/rectifications/$RECT_ID:reopen" \
  -H "$AUTH" 2>/dev/null || echo '{}')
reopen_status=$(extract_val "$reopen_resp" "status")
if [ "$reopen_status" = "REOPENED" ]; then
  pass "整改项重新打开 -> REOPENED"
else
  fail "整改项重新打开后 status=$reopen_status (expected REOPENED)"
fi

# 8d. 将测试整改项重新处理并关闭，避免反复执行脚本后留下隐藏的开放整改数据。
cleanup_resolve_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/rectifications/$RECT_ID:resolve" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"resolutionNote":"专项脚本复跑清理：整改项重新关闭"}' 2>/dev/null || echo '{}')
cleanup_resolve_status=$(extract_val "$cleanup_resolve_resp" "status")
cleanup_close_resp=$(curl -fsS -X POST "$BASE_URL/api/work-center/projects/$PID/rectifications/$RECT_ID:close" \
  -H "$AUTH" 2>/dev/null || echo '{}')
cleanup_close_status=$(extract_val "$cleanup_close_resp" "status")
if [ "$cleanup_resolve_status" = "RESOLVED" ] && [ "$cleanup_close_status" = "CLOSED" ]; then
  pass "测试整改项重开后已重新处理并关闭"
else
  fail "测试整改项清理关闭失败 (resolve=$cleanup_resolve_status, close=$cleanup_close_status)"
fi

# ---------- 9. 三类 CSV 导出 ----------
echo ""
echo "--- 9. 三类 CSV 导出 ---"

# 9a. delivery-completeness.csv
csv1=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/reports/delivery-completeness.csv" -H "$AUTH" 2>/dev/null || echo "")
csv1_http=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/api/work-center/projects/$PID/reports/delivery-completeness.csv" -H "$AUTH" 2>/dev/null || echo "000")
if [ "$csv1_http" = "200" ] && echo "$csv1" | grep -q "项目ID"; then
  pass "delivery-completeness.csv 可下载且包含表头 (HTTP $csv1_http)"
else
  fail "delivery-completeness.csv 下载失败或缺少表头 (HTTP $csv1_http)"
fi

# 9b. review-summary.csv
csv2=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/reports/review-summary.csv" -H "$AUTH" 2>/dev/null || echo "")
csv2_http=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/api/work-center/projects/$PID/reports/review-summary.csv" -H "$AUTH" 2>/dev/null || echo "000")
if [ "$csv2_http" = "200" ] && echo "$csv2" | grep -q "绑定ID"; then
  pass "review-summary.csv 可下载且包含表头 (HTTP $csv2_http)"
else
  fail "review-summary.csv 下载失败或缺少表头 (HTTP $csv2_http)"
fi

# 9c. rectifications.csv
csv3=$(curl -fsS "$BASE_URL/api/work-center/projects/$PID/reports/rectifications.csv" -H "$AUTH" 2>/dev/null || echo "")
csv3_http=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/api/work-center/projects/$PID/reports/rectifications.csv" -H "$AUTH" 2>/dev/null || echo "000")
if [ "$csv3_http" = "200" ] && echo "$csv3" | grep -q "整改ID"; then
  pass "rectifications.csv 可下载且包含表头 (HTTP $csv3_http)"
else
  fail "rectifications.csv 下载失败或缺少表头 (HTTP $csv3_http)"
fi

# ---------- 10. 权限隔离 ----------
echo ""
echo "--- 10. 权限隔离 ---"

OTHER_PID=99999

# 普通用户不能审核非当前项目
reg_review_code=$(curl -sS -o /dev/null -w "%{http_code}" -X POST \
  "$BASE_URL/api/work-center/projects/$OTHER_PID/delivery-bindings/1:approve" \
  -H "$REG_AUTH" 2>/dev/null || echo "000")
if [ "$reg_review_code" = "403" ] || [ "$reg_review_code" = "404" ]; then
  pass "普通用户无法跨项目审核 (HTTP $reg_review_code)"
else
  fail "普通用户越权审核了非当前项目 (HTTP $reg_review_code)"
fi

# 普通用户不能查看非当前项目整改
reg_rect_code=$(curl -sS -o /dev/null -w "%{http_code}" \
  "$BASE_URL/api/work-center/projects/$OTHER_PID/rectifications" \
  -H "$REG_AUTH" 2>/dev/null || echo "000")
if [ "$reg_rect_code" = "403" ] || [ "$reg_rect_code" = "404" ]; then
  pass "普通用户无法跨项目查看整改项 (HTTP $reg_rect_code)"
else
  fail "普通用户越权查看了非当前项目整改项 (HTTP $reg_rect_code)"
fi

# 普通用户不能导出非当前项目报表
reg_csv_code=$(curl -sS -o /dev/null -w "%{http_code}" \
  "$BASE_URL/api/work-center/projects/$OTHER_PID/reports/delivery-completeness.csv" \
  -H "$REG_AUTH" 2>/dev/null || echo "000")
if [ "$reg_csv_code" = "403" ] || [ "$reg_csv_code" = "404" ]; then
  pass "普通用户无法跨项目导出报表 (HTTP $reg_csv_code)"
else
  fail "普通用户越权导出了非当前项目报表 (HTTP $reg_csv_code)"
fi

# ---------- 11. 审计验证 ----------
echo ""
echo "--- 11. 审计验证 ---"
audit_events=$(curl -fsS "$BASE_URL/api/core/projects/$PID/audit-logs?limit=50" -H "$AUTH" 2>/dev/null || echo '[]')

for action in "work.review.submit" "work.review.approve" "work.review.reject" \
  "work.rectification.resolve" "work.rectification.close" "work.rectification.reopen" \
  "work.report.export"; do
  cnt=$(count_matches "$audit_events" "$action")
  if [ "$cnt" -gt 0 ]; then
    pass "审计包含 $action ($cnt 条)"
  else
    fail "审计缺少 $action"
  fi
done

# ---------- Cleanup ----------
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

# Delete bindings (DB cascade: review records soft-deleted separately; rectifications follow)
# Delete in reverse dependency order
delete_if_id "绑定1" "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/${BIND1_ID:-0}" "${BIND1_ID:-0}"
delete_if_id "绑定2" "$BASE_URL/api/work-center/projects/$PID/delivery-bindings/${BIND2_ID:-0}" "${BIND2_ID:-0}"
delete_if_id "目录模板" "$BASE_URL/api/master-data/projects/$PID/directory-templates/${TMPL_ID:-0}" "${TMPL_ID:-0}"
delete_if_id "文档交付类型" "$BASE_URL/api/master-data/projects/$PID/deliverable-types/${DT_DOC_ID:-0}" "${DT_DOC_ID:-0}"
delete_if_id "图纸交付类型" "$BASE_URL/api/master-data/projects/$PID/deliverable-types/${DT_DWG_ID:-0}" "${DT_DWG_ID:-0}"
delete_if_id "交付物定义" "$BASE_URL/api/master-data/projects/$PID/deliverable-definitions/${DD_ID:-0}" "${DD_ID:-0}"
delete_if_id "文件资源1" "$BASE_URL/api/data-steward/projects/$PID/file-resources/${FILE1_ID:-0}" "${FILE1_ID:-0}"
delete_if_id "文件资源2" "$BASE_URL/api/data-steward/projects/$PID/file-resources/${FILE2_ID:-0}" "${FILE2_ID:-0}"
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
  echo "[FAIL] 二期批次三验收未通过"
  exit 1
else
  echo "[PASS] 二期批次三验收通过"
fi
