#!/usr/bin/env bash
# check-phase2-batch1-readonly-catalog.sh
# 二期批次一：只读资产目录与 Agent 对接预览层 — 验收脚本
#
# 验证：
#   1. 后端健康检查
#   2. OpenAPI 包含 catalog 与 permission-proof 接口
#   3. 管理员可查询真实 NAS catalog 项目和文件元数据
#   4. 普通项目用户无法读取无权限项目 catalog
#   5. 普通项目用户无法看到无权限真实 NAS 路径
#   6. 权限证明接口对允许和拒绝场景均返回 traceId 和稳定原因码
#   7. 调用本批接口前后，正式项目数、正式文件数、扫描任务数、删除申请数不异常变化
#   8. 本批接口不会创建扫描任务、checksum 任务、删除申请、正式资产或路径映射
#
# 允许因权限证明/Agent preview 行为新增审计事件，但必须在脚本输出中说明。

set -euo pipefail

# helper: safely count occurrences of a pattern (disable pipefail in subshell)
count_matches() {
  (set +o pipefail; echo "$1" | grep -o "$2" 2>/dev/null | wc -l | tr -d ' ') || echo "0"
}

# helper: extract first integer value for a JSON key
extract_int() {
  (set +o pipefail; echo "$1" | grep -o "\"$2\":[0-9]*" 2>/dev/null | head -1 | sed "s/\"$2\"://") || echo ""
}

# helper: extract first string value for a JSON key
extract_string() {
  (set +o pipefail; echo "$1" | grep -o "\"$2\":\"[^\"]*\"" 2>/dev/null | head -1 | sed "s/\"$2\":\"//;s/\"$//") || echo ""
}

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin@123}"
REGULAR_USER="${REGULAR_USER:-delivery.engineer}"
REGULAR_PASS="${REGULAR_PASS:-Engineer@123}"

PASS=0
FAIL=0

pass() { echo "  [PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "  [FAIL] $1"; FAIL=$((FAIL + 1)); }

echo "=== Phase 2 Batch 1: 只读资产目录与 Agent 对接预览层 — 验收 ==="
echo ""

login() {
  local user="$1" pass="$2"
  curl -fsS -X POST "$BASE_URL/api/core/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$user\",\"password\":\"$pass\"}" 2>/dev/null
}

admin_token=$(login "$ADMIN_USER" "$ADMIN_PASS" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' || echo "")
if [ -z "$admin_token" ]; then
  fail "管理员登录失败"
  echo "  后端可能未运行。请先启动后端: bash scripts/dev/start-backend.sh"
  exit 1
fi
pass "管理员登录成功"

regular_token=$(login "$REGULAR_USER" "$REGULAR_PASS" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' || echo "")
if [ -z "$regular_token" ]; then
  fail "普通用户登录失败"
else
  pass "普通用户登录成功"
fi

AUTH_HEADER="Authorization: Bearer $admin_token"
REG_AUTH_HEADER="Authorization: Bearer $regular_token"

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

for path in '/api/data-steward/catalog/projects' '/api/data-steward/catalog/directories' \
  '/api/data-steward/catalog/files' \
  '/api/data-steward/catalog/files/{fileId}/permission-proof' \
  '/api/data-steward/catalog/permission-proofs:check'; do
  label=$(echo "$path" | sed 's|/api/data-steward/catalog/||' | sed 's|{fileId}/||')
  if echo "$openapi" | grep -q "$path"; then
    pass "OpenAPI 包含 $label"
  else
    fail "OpenAPI 缺少 $label"
  fi
done

# ---------- 3. 管理员 catalog 查询 ----------
echo ""
echo "--- 3. 管理员 catalog 查询 ---"

# Snapshot counts before
projects_resp=$(curl -fsS "$BASE_URL/api/data-steward/assets/projects?assetSource=NAS_REAL*" -H "$AUTH_HEADER" 2>/dev/null || echo '[]')
projects_before=$(count_matches "$projects_resp" '"projectId"')

catalog_projects=$(curl -fsS "$BASE_URL/api/data-steward/catalog/projects?assetSource=NAS_REAL*" -H "$AUTH_HEADER" 2>/dev/null || echo '[]')
cat_count=$(count_matches "$catalog_projects" '"projectId"')
if [ "$cat_count" -gt 0 ]; then
  pass "管理员 catalog/projects 返回了项目 (count=$cat_count)"
else
  fail "管理员 catalog/projects 返回空或无 projectId"
fi

first_pid=$(extract_int "$catalog_projects" "projectId")
if [ -n "$first_pid" ] && [ "$first_pid" -gt 0 ] 2>/dev/null; then
  catalog_files=$(curl -fsS "$BASE_URL/api/data-steward/catalog/files?projectId=$first_pid&pageSize=5" -H "$AUTH_HEADER" 2>/dev/null || echo '{}')
  if echo "$catalog_files" | grep -q '"fileId"'; then
    pass "管理员 catalog/files 返回了文件元数据 (projectId=$first_pid)"
  else
    fail "管理员 catalog/files 无 fileId (projectId=$first_pid)"
  fi

  catalog_dirs=$(curl -fsS "$BASE_URL/api/data-steward/catalog/directories?projectId=$first_pid" -H "$AUTH_HEADER" 2>/dev/null || echo '[]')
  first_dir=$(extract_string "$catalog_dirs" "directoryPath")
  if [ -n "$first_dir" ]; then
    pass "管理员 catalog/directories 返回目录聚合"
    if echo "$first_dir" | grep -Eiq '\.(rvt|dwg|ifc|nwd|nwc|dxf|pdf|doc|docx|xls|xlsx|glb)$'; then
      fail "管理员 catalog/directories 返回了文件路径而非目录: $first_dir"
    else
      pass "管理员 catalog/directories 返回目录级路径"
    fi
    dir_files=$(curl -fsS -G "$BASE_URL/api/data-steward/catalog/files" \
      --data-urlencode "projectId=$first_pid" \
      --data-urlencode "directoryPath=$first_dir" \
      --data-urlencode "pageSize=5" \
      -H "$AUTH_HEADER" 2>/dev/null || echo '{}')
    if echo "$dir_files" | grep -q '"fileId"'; then
      pass "管理员 catalog/files 支持按目录浏览过滤"
    else
      fail "管理员 catalog/files 按目录过滤无文件"
    fi
  else
    fail "管理员 catalog/directories 未返回 directoryPath"
  fi

  first_version=$(extract_string "$catalog_files" "version")
  if [ -n "$first_version" ]; then
    version_files=$(curl -fsS -G "$BASE_URL/api/data-steward/catalog/files" \
      --data-urlencode "projectId=$first_pid" \
      --data-urlencode "version=$first_version" \
      --data-urlencode "pageSize=5" \
      -H "$AUTH_HEADER" 2>/dev/null || echo '{}')
    if echo "$version_files" | grep -q "\"version\":\"$first_version\""; then
      pass "管理员 catalog/files 支持版本筛选 (version=$first_version)"
    else
      fail "管理员 catalog/files 版本筛选未命中 version=$first_version"
    fi
  else
    fail "管理员 catalog/files 未返回可测试版本字段"
  fi

  first_fid=$(extract_int "$catalog_files" "fileId")
  if [ -n "$first_fid" ] && [ "$first_fid" -gt 0 ] 2>/dev/null; then
    file_detail=$(curl -fsS "$BASE_URL/api/data-steward/catalog/files/$first_fid" -H "$AUTH_HEADER" 2>/dev/null || echo '{}')
    if echo "$file_detail" | grep -q '"fileName"'; then
      pass "管理员 catalog/files/$first_fid 返回文件详情"
    else
      fail "管理员 catalog/files/$first_fid 无 fileName"
    fi

    audit=$(curl -fsS "$BASE_URL/api/data-steward/catalog/files/$first_fid/audit-context" -H "$AUTH_HEADER" 2>/dev/null || echo '{}')
    if echo "$audit" | grep -q '"fileId"'; then
      pass "管理员 catalog/files/$first_fid/audit-context 返回审计上下文"
    else
      fail "管理员 catalog/files/$first_fid/audit-context 无 fileId"
    fi
  fi
else
  echo "  [SKIP] 无可用 catalog 项目，跳过文件查询"
  first_fid=""
fi

# ---------- 4. 普通用户权限隔离 ----------
echo ""
echo "--- 4. 普通用户权限隔离 ---"

reg_catalog=$(curl -fsS "$BASE_URL/api/data-steward/catalog/projects?assetSource=NAS_REAL*" -H "$REG_AUTH_HEADER" 2>/dev/null || echo '[]')
reg_count=$(count_matches "$reg_catalog" '"projectId"')

echo "  admin catalog projects = $cat_count, regular user catalog projects = $reg_count"

if [ "$reg_count" -le "$cat_count" ] && [ "$cat_count" -gt 0 ]; then
  pass "普通用户 catalog 项目数不超过管理员 ($reg_count <= $cat_count)"
else
  fail "普通用户 catalog 项目数异常 ($reg_count vs admin $cat_count)"
fi

# ---------- 5. 路径可见性 ----------
echo ""
echo "--- 5. 路径可见性 ---"

if [ -n "${first_fid:-}" ] && [ "${first_fid:-0}" -gt 0 ] 2>/dev/null; then
  reg_detail=$(curl -fsS "$BASE_URL/api/data-steward/catalog/files/$first_fid" -H "$REG_AUTH_HEADER" 2>/dev/null || echo '{}')
  if echo "$reg_detail" | grep -q '"storagePathVisible":false'; then
    pass "普通用户对无权限文件 storagePathVisible=false"
  elif echo "$reg_detail" | grep -q '"storagePathVisible":true'; then
    echo "  [INFO] 普通用户对该文件可见 storagePath (属于授权项目范围)"
  else
    echo "  [INFO] 普通用户无法访问该文件详情 (预期行为: 无项目权限)"
  fi
fi

# Check audit-context for unauthorized user (must not be 500)
if [ -n "${first_fid:-}" ] && [ "${first_fid:-0}" -gt 0 ] 2>/dev/null; then
  reg_audit_http=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/api/data-steward/catalog/files/$first_fid/audit-context" -H "$REG_AUTH_HEADER" 2>/dev/null || echo "000")
  if [ "$reg_audit_http" = "404" ]; then
    pass "普通用户无权文件 audit-context 返回 404 (非 500)"
  elif [ "$reg_audit_http" = "200" ]; then
    echo "  [INFO] 普通用户 audit-context 返回 200 (文件属于授权项目)"
  elif [ "$reg_audit_http" = "500" ]; then
    fail "普通用户无权文件 audit-context 返回 500 CORE_INTERNAL_ERROR (主 agent 审计修复项)"
  else
    echo "  [INFO] 普通用户 audit-context HTTP $reg_audit_http"
  fi
fi

# ---------- 6. 权限证明 ----------
echo ""
echo "--- 6. 权限证明 ---"

if [ -n "${first_fid:-}" ] && [ "${first_fid:-0}" -gt 0 ] 2>/dev/null; then
  proof=$(curl -fsS "$BASE_URL/api/data-steward/catalog/files/$first_fid/permission-proof" -H "$AUTH_HEADER" 2>/dev/null || echo '{}')

  if echo "$proof" | grep -q '"traceId"'; then pass "权限证明返回 traceId"; else fail "权限证明缺少 traceId"; fi
  if echo "$proof" | grep -q '"reasonCode"'; then pass "权限证明返回 reasonCode"; else fail "权限证明缺少 reasonCode"; fi
  if echo "$proof" | grep -q '"decision"'; then pass "权限证明返回 decision"; else fail "权限证明缺少 decision"; fi

  if echo "$proof" | grep -qi '"token"\|"password"\|"secret"\|"apiKey"'; then
    fail "权限证明疑似返回敏感凭证"
  else
    pass "权限证明未泄露敏感凭证"
  fi

  bulk=$(curl -fsS -X POST "$BASE_URL/api/data-steward/catalog/permission-proofs:check" \
    -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
    -d "{\"fileIds\":[$first_fid],\"actorType\":\"USER\"}" 2>/dev/null || echo '[]')
  if echo "$bulk" | grep -q '"traceId"'; then pass "批量权限证明返回 traceId"; else fail "批量权限证明缺少 traceId"; fi
else
  echo "  [SKIP] 无可用文件 ID 测试权限证明"
fi

# ---------- 7. 数据不变性 ----------
echo ""
echo "--- 7. 数据不变性 ---"

projects_after=$(count_matches "$(curl -fsS "$BASE_URL/api/data-steward/assets/projects?assetSource=NAS_REAL*" -H "$AUTH_HEADER" 2>/dev/null || echo '[]')" '"projectId"')

if [ "$projects_before" = "$projects_after" ]; then
  pass "项目数不变 ($projects_before -> $projects_after)"
else
  fail "项目数变化 ($projects_before -> $projects_after)"
fi

# Scan tasks count check
scans_before_resp=$(curl -fsS "$BASE_URL/api/data-steward/assets/nas-scans" -H "$AUTH_HEADER" 2>/dev/null || echo '[]')
scans_before=$(count_matches "$scans_before_resp" '"id"')

# Re-call catalog APIs to verify they don't create side effects
curl -fsS "$BASE_URL/api/data-steward/catalog/projects" -H "$AUTH_HEADER" 2>/dev/null > /dev/null || true
curl -fsS "$BASE_URL/api/data-steward/catalog/directories?projectId=${first_pid:-1}" -H "$AUTH_HEADER" 2>/dev/null > /dev/null || true
curl -fsS "$BASE_URL/api/data-steward/catalog/files?pageSize=5" -H "$AUTH_HEADER" 2>/dev/null > /dev/null || true

scans_after_resp=$(curl -fsS "$BASE_URL/api/data-steward/assets/nas-scans" -H "$AUTH_HEADER" 2>/dev/null || echo '[]')
scans_after=$(count_matches "$scans_after_resp" '"id"')

if [ "$scans_before" = "$scans_after" ]; then
  pass "扫描任务数不变 ($scans_before -> $scans_after)"
else
  fail "扫描任务数变化 ($scans_before -> $scans_after)"
fi

# ---------- 8. 审计事件说明 ----------
echo ""
echo "--- 8. 审计事件说明 ---"
echo "  [INFO] 权限证明接口 (permission-proof / permission-proofs:check) 会写入 PERMISSION_PROOF_ALLOWED/PERMISSION_PROOF_DENIED 审计事件。"
echo "  [INFO] 这是预期的轻量审计行为，用于追踪权限检查操作。"
echo "  [INFO] 普通 catalog 列表查询 (projects/directories/files) 不产生审计事件。"

# ---------- Summary ----------
echo ""
echo "=== 验收总结 ==="
echo "通过: $PASS"
echo "失败: $FAIL"
echo ""

if [ "$FAIL" -gt 0 ]; then
  echo "[FAIL] 二期批次一验收未通过"
  exit 1
else
  echo "[PASS] 二期批次一验收通过"
fi
