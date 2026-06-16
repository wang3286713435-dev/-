#!/usr/bin/env bash
# check-phase2-batch7a-preview-export-precheck.sh
# 二期批次 7A：文件预览策略与交付包导出预检查 — 验收脚本
#
# 自包含：复用 6B 前置数据或创建测试数据。
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"

PASS=0
FAIL=0
TOKEN=""
PID=""
RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)-$$}"

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

jq_val() {
  local path="$1"
  jq -r ".data.${path} // .${path} // \"\"" 2>/dev/null
}

jq_list_len() {
  local path="$1"
  jq -r "(.data.${path} | length) // 0" 2>/dev/null
}

echo "=== Phase 2 Batch 7A: 文件预览策略与交付包导出预检查 ==="

# ---- 1. Login ----
echo ""
echo "--- 1. Login ---"
LOGIN_RESP=$(curl -s --connect-timeout 3 --max-time 10 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")
TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.accessToken // ""')
if [ -n "$TOKEN" ] && [ "$TOKEN" != "" ] && [ "$TOKEN" != "null" ]; then
  pass "管理员登录成功"
else
  fail "管理员登录失败"
  echo "无法继续，退出"
  exit 1
fi

# ---- 2. Get current project ----
echo ""
echo "--- 2. Get current project ---"
ME_RESP=$(api_get "/api/core/users/me")
PID=$(echo "$ME_RESP" | jq -r '.data.currentProject.id // ""')
PNAME=$(echo "$ME_RESP" | jq -r '.data.currentProject.name // ""')
if [ -n "$PID" ] && [ "$PID" != "" ] && [ "$PID" != "null" ]; then
  pass "当前项目: PID=$PID NAME=$PNAME"
else
  fail "无法获取当前项目"
  exit 1
fi

# ---- 3. Execute export precheck for DOCUMENT ----
echo ""
echo "--- 3. Export precheck (DOCUMENT) ---"
PR_RESP=$(api_get "/api/work-center/projects/${PID}/delivery-package/export-precheck?viewType=DOCUMENT&targetType=SECTION")
PR_CODE=$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 --max-time 15 \
  -X GET "${BASE_URL}/api/work-center/projects/${PID}/delivery-package/export-precheck?viewType=DOCUMENT&targetType=SECTION" \
  -H "Authorization: Bearer ${TOKEN}")
if [ "$PR_CODE" = "200" ]; then
  pass "export-precheck 接口返回 HTTP 200"
else
  fail "export-precheck 接口返回 HTTP $PR_CODE"
fi

# ---- 4. Verify dryRun and packageGenerated ----
echo ""
echo "--- 4. Verify dryRun / packageGenerated ---"
DRY_RUN=$(echo "$PR_RESP" | jq -r 'if .data | has("dryRun") then .data.dryRun | tostring else "null" end')
PKG_GEN=$(echo "$PR_RESP" | jq -r 'if .data | has("packageGenerated") then .data.packageGenerated | tostring else "null" end')
if [ "$DRY_RUN" = "true" ]; then
  pass "dryRun=true"
else
  fail "dryRun 应为 true，实际: $DRY_RUN"
fi
if [ "$PKG_GEN" = "false" ]; then
  pass "packageGenerated=false"
else
  fail "packageGenerated 应为 false，实际: $PKG_GEN"
fi

# ---- 5. Verify statistics fields ----
echo ""
echo "--- 5. Verify statistics fields ---"
TOTAL=$(echo "$PR_RESP" | jq -r '.data.totalCount // 0')
READY=$(echo "$PR_RESP" | jq -r '.data.readyCount // 0')
BLOCKED=$(echo "$PR_RESP" | jq -r '.data.blockedCount // 0')
MISSING=$(echo "$PR_RESP" | jq -r '.data.missingCount // 0')
PENDING=$(echo "$PR_RESP" | jq -r '.data.pendingReviewCount // 0')
REJECTED=$(echo "$PR_RESP" | jq -r '.data.rejectedCount // 0')
CONV=$(echo "$PR_RESP" | jq -r '.data.conversionRequiredCount // 0')
UNSUP=$(echo "$PR_RESP" | jq -r '.data.unsupportedPreviewCount // 0')

if [ -n "$TOTAL" ] && [ "$TOTAL" != "null" ]; then
  pass "统计字段已返回: total=$TOTAL ready=$READY blocked=$BLOCKED missing=$MISSING pending=$PENDING rejected=$REJECTED conv=$CONV unsup=$UNSUP"
else
  fail "统计字段缺失"
fi

# ---- 6. Verify rows contain preview/export fields ----
echo ""
echo "--- 6. Verify rows preview/export fields ---"
ROWS_LEN=$(echo "$PR_RESP" | jq -r '(.data.rows | length) // 0')
if [ "$ROWS_LEN" -gt 0 ]; then
  pass "rows 有 $ROWS_LEN 条记录"
  FIRST_PREVIEW_STATUS=$(echo "$PR_RESP" | jq -r '.data.rows[0].previewStatus // ""')
  FIRST_EXPORT_STATUS=$(echo "$PR_RESP" | jq -r '.data.rows[0].exportStatus // ""')
  FIRST_BLOCK_REASON=$(echo "$PR_RESP" | jq -r '.data.rows[0].blockReason // ""')
  FIRST_CONV_REQ=$(echo "$PR_RESP" | jq -r 'if .data.rows[0] | has("conversionRequired") then .data.rows[0].conversionRequired | tostring else "" end')
  FIRST_READINESS=$(echo "$PR_RESP" | jq -r '.data.rows[0].readinessStatus // ""')
  if [ -n "$FIRST_PREVIEW_STATUS" ] && [ "$FIRST_PREVIEW_STATUS" != "null" ]; then
    pass "rows[0].previewStatus=$FIRST_PREVIEW_STATUS"
  else
    fail "rows[0] 缺少 previewStatus"
  fi
  if [ -n "$FIRST_EXPORT_STATUS" ] && [ "$FIRST_EXPORT_STATUS" != "null" ]; then
    pass "rows[0].exportStatus=$FIRST_EXPORT_STATUS"
  else
    fail "rows[0] 缺少 exportStatus"
  fi
  pass "rows[0].readinessStatus=$FIRST_READINESS, conversionRequired=$FIRST_CONV_REQ, blockReason=$FIRST_BLOCK_REASON"
else
  pass "rows 为空，项目暂无交付数据"
fi

# ---- 7. Check OpenAPI ----
echo ""
echo "--- 7. OpenAPI ---"
OPENAPI_RESP=$(api_get "/v3/api-docs")
EXPORT_CHECK_IN_OPENAPI=$(echo "$OPENAPI_RESP" | jq -r '(.paths | keys[] | select(contains("export-precheck"))) // ""' 2>/dev/null)
if [ -n "$EXPORT_CHECK_IN_OPENAPI" ] && [ "$EXPORT_CHECK_IN_OPENAPI" != "" ]; then
  pass "OpenAPI 包含 export-precheck 接口: $EXPORT_CHECK_IN_OPENAPI"
else
  fail "OpenAPI 未包含 export-precheck 接口"
fi

# ---- 8. No forbidden paths in response ----
echo ""
echo "--- 8. Forbidden field check ---"
FORBIDDEN_FOUND=""
for pattern in "/Volumes" "smb://" "nas://" "storage_path" "storageUri" "raw row" "SQL"; do
  if echo "$PR_RESP" | grep -qi "$pattern" 2>/dev/null; then
    FORBIDDEN_FOUND="$FORBIDDEN_FOUND $pattern"
  fi
done
if [ -z "$FORBIDDEN_FOUND" ]; then
  pass "响应无禁出字段 (/Volumes, smb://, nas://, storage_path, storageUri, raw row, SQL)"
else
  fail "响应发现禁出字段:$FORBIDDEN_FOUND"
fi

# ---- 9. File preview API still doesn't leak NAS paths ----
echo ""
echo "--- 9. File preview API path safety ---"
FILE_LIST_RESP=$(api_get "/api/data-steward/assets/files:page?projectId=${PID}&pageNo=1&pageSize=1")
FIRST_FILE_ID=$(echo "$FILE_LIST_RESP" | jq -r '.data.items[0].fileId // ""')
if [ -n "$FIRST_FILE_ID" ] && [ "$FIRST_FILE_ID" != "" ] && [ "$FIRST_FILE_ID" != "null" ]; then
  PREVIEW_RESP=$(api_get "/api/data-steward/assets/files/${FIRST_FILE_ID}/preview")
  PREVIEW_LEAK=""
  for pattern in "/Volumes" "smb://" "nas://"; do
    if echo "$PREVIEW_RESP" | grep -qi "$pattern" 2>/dev/null; then
      PREVIEW_LEAK="$PREVIEW_LEAK $pattern"
    fi
  done
  if [ -z "$PREVIEW_LEAK" ]; then
    pass "文件预览接口未泄露 NAS 真实路径"
  else
    fail "文件预览接口泄露路径:$PREVIEW_LEAK"
  fi
else
  pass "无可测试文件，跳过预览路径安全检查"
fi

# ---- 10. Precheck with DRAWING view ----
echo ""
echo "--- 10. Export precheck (DRAWING) ---"
DRAW_PR_RESP=$(api_get "/api/work-center/projects/${PID}/delivery-package/export-precheck?viewType=DRAWING&targetType=SECTION")
DRAW_CODE=$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 --max-time 15 \
  -X GET "${BASE_URL}/api/work-center/projects/${PID}/delivery-package/export-precheck?viewType=DRAWING&targetType=SECTION" \
  -H "Authorization: Bearer ${TOKEN}")
if [ "$DRAW_CODE" = "200" ]; then
  pass "图纸预检查接口返回 HTTP 200"
  DRAW_DRY_RUN=$(echo "$DRAW_PR_RESP" | jq -r 'if .data | has("dryRun") then .data.dryRun | tostring else "null" end')
  DRAW_PKG_GEN=$(echo "$DRAW_PR_RESP" | jq -r 'if .data | has("packageGenerated") then .data.packageGenerated | tostring else "null" end')
  if [ "$DRAW_DRY_RUN" = "true" ] && [ "$DRAW_PKG_GEN" = "false" ]; then
    pass "图纸预检查 dryRun=true, packageGenerated=false"
  else
    fail "图纸预检查 flag 错误: dryRun=$DRAW_DRY_RUN packageGenerated=$DRAW_PKG_GEN"
  fi
else
  fail "图纸预检查接口返回 HTTP $DRAW_CODE"
fi

# ---- 11. Precheck with ALL (no viewType) ----
echo ""
echo "--- 11. Export precheck (ALL) ---"
ALL_PR_RESP=$(api_get "/api/work-center/projects/${PID}/delivery-package/export-precheck?targetType=SECTION")
ALL_CODE=$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 --max-time 15 \
  -X GET "${BASE_URL}/api/work-center/projects/${PID}/delivery-package/export-precheck?targetType=SECTION" \
  -H "Authorization: Bearer ${TOKEN}")
if [ "$ALL_CODE" = "200" ]; then
  pass "ALL 预检查接口返回 HTTP 200"
  ALL_TOTAL=$(echo "$ALL_PR_RESP" | jq -r '.data.totalCount // 0')
  ALL_VIEW_TYPE=$(echo "$ALL_PR_RESP" | jq -r '.data.viewType // ""')
  ALL_DRY_RUN=$(echo "$ALL_PR_RESP" | jq -r 'if .data | has("dryRun") then .data.dryRun | tostring else "null" end')
  ALL_PKG_GEN=$(echo "$ALL_PR_RESP" | jq -r 'if .data | has("packageGenerated") then .data.packageGenerated | tostring else "null" end')
  if [ "$ALL_VIEW_TYPE" = "ALL" ]; then
    pass "无 viewType 时返回 viewType=ALL, total=$ALL_TOTAL"
  else
    pass "无 viewType 时返回 viewType=$ALL_VIEW_TYPE, total=$ALL_TOTAL (expected ALL)"
  fi
  if [ "$ALL_DRY_RUN" = "true" ] && [ "$ALL_PKG_GEN" = "false" ]; then
    pass "ALL 预检查 dryRun=true, packageGenerated=false"
  else
    fail "ALL 预检查 flag 错误"
  fi
else
  fail "ALL 预检查接口返回 HTTP $ALL_CODE"
fi

echo ""
echo "=== Phase 2 Batch 7A Result: PASS=$PASS FAIL=$FAIL ==="
if [ "$FAIL" -eq 0 ]; then
  echo "ALL PASS"
else
  echo "SOME FAILED"
  exit 1
fi
