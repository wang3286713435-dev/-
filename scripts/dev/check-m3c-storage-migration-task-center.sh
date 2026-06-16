#!/usr/bin/env bash
# M3C: object storage migration task center smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"
MINIO_CONTAINER="${MINIO_CONTAINER:-delivery-minio}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin123}"

PASS=0
FAIL=0
TOKEN=""
TMP_DIR="$(mktemp -d /tmp/delivery-m3c.XXXXXX)"
SMOKE_FILE_ID=""
MISSING_FILE_ID=""
TASK_IDS=()

pass() {
  PASS=$((PASS + 1))
  printf '  [PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '  [FAIL] %s\n' "$1" >&2
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
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

json_data_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"]).get("data")
scope = {
    "__builtins__": {},
    "data": data,
    "len": len,
    "all": all,
    "any": any,
    "int": int,
    "str": str,
    "isinstance": isinstance,
    "list": list,
}
value = eval(os.environ["EXPR"], scope, scope)
if value is None:
    print("")
elif isinstance(value, bool):
    print("true" if value else "false")
else:
    print(value)
PY
}

assert_no_forbidden() {
  local label="$1"
  local payload="$2"
  LABEL="${label}" PAYLOAD="${payload}" python3 - <<'PY'
import os
import re
label = os.environ["LABEL"]
payload = os.environ["PAYLOAD"]
patterns = [
    r"/Volumes(?:/|$)",
    r"/Users(?:/|$)",
    r"/private(?:/|$)",
    r"\bnas://",
    r"\bsmb://",
    r"\bstorage_path\b",
    r"\bstorage_uri\b",
    r"\bstoragePath\b",
    r"\bstorageUri\b",
    r"\bobject_key\b",
    r"\bobjectKey\b",
    r"\bbucket\b",
    r"\braw DB row\b",
    r"\braw row\b",
    r"\bsecret\b",
    r"\bpassword\b",
    r"\bselect\s+.+\s+from\b",
    r"\binsert\s+into\b",
    r"\bupdate\s+.+\s+set\b",
    r"\bdelete\s+from\b",
]
for pattern in patterns:
    assert not re.search(pattern, payload, re.IGNORECASE), f"{label} contains forbidden pattern {pattern}: {payload[:1200]}"
PY
}

api_get() {
  local url="$1"
  curl -sS --connect-timeout 3 --max-time 30 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

api_post() {
  local url="$1"
  local body="${2:-{}}"
  curl -sS --connect-timeout 3 --max-time 90 -X POST "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "${body}"
}

cleanup() {
  local ids=()
  [[ -n "${SMOKE_FILE_ID}" ]] && ids+=("${SMOKE_FILE_ID}")
  [[ -n "${MISSING_FILE_ID}" ]] && ids+=("${MISSING_FILE_ID}")

  if [[ "${#ids[@]}" -gt 0 ]]; then
    local id_csv
    id_csv="$(IFS=,; echo "${ids[*]}")"
    local object_rows
    object_rows="$(mysql_exec "SELECT so.bucket, so.object_key FROM data_storage_objects so JOIN data_file_object_versions fov ON fov.storage_object_id = so.id WHERE fov.file_id IN (${id_csv}) AND so.deleted = 0;" 2>/dev/null || true)"
    if [[ -n "${object_rows}" ]] && docker ps --format '{{.Names}}' | grep -qx "${MINIO_CONTAINER}"; then
      while IFS=$'\t' read -r bucket object_key; do
        [[ -z "${bucket}" || -z "${object_key}" ]] && continue
        docker exec \
          -e MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY}" \
          -e MINIO_SECRET_KEY="${MINIO_SECRET_KEY}" \
          -e BUCKET="${bucket}" \
          -e OBJECT_KEY="${object_key}" \
          "${MINIO_CONTAINER}" sh -c '
            mc alias set local http://127.0.0.1:9000 "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null 2>&1 || exit 0
            mc rm --force "local/${BUCKET}/${OBJECT_KEY}" >/dev/null 2>&1 || true
          ' >/dev/null 2>&1 || true
      done <<< "${object_rows}"
    fi
    mysql_exec "UPDATE data_file_object_versions SET deleted = 1, delete_token = id WHERE file_id IN (${id_csv});" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_object_migration_tasks SET deleted = 1, delete_token = id WHERE file_id IN (${id_csv});" >/dev/null 2>&1 || true
    mysql_exec "UPDATE data_storage_objects so SET so.deleted = 1, so.delete_token = so.id WHERE so.id IN (SELECT storage_object_id FROM data_file_object_versions WHERE file_id IN (${id_csv}));" >/dev/null 2>&1 || true
  fi

  if [[ "${#TASK_IDS[@]}" -gt 0 ]]; then
    local task_csv
    task_csv="$(IFS=,; echo "${TASK_IDS[*]}")"
    mysql_exec "UPDATE data_object_migration_task_batches SET deleted = 1, delete_token = id WHERE id IN (${task_csv});" >/dev/null 2>&1 || true
  fi

  if [[ -n "${TOKEN}" ]]; then
    [[ -n "${SMOKE_FILE_ID}" ]] && curl -sS --connect-timeout 3 --max-time 20 -X DELETE "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/file-resources/${SMOKE_FILE_ID}" -H "Authorization: Bearer ${TOKEN}" >/dev/null || true
    [[ -n "${MISSING_FILE_ID}" ]] && curl -sS --connect-timeout 3 --max-time 20 -X DELETE "${BASE_URL}/api/data-steward/projects/${PROJECT_ID}/file-resources/${MISSING_FILE_ID}" -H "Authorization: Bearer ${TOKEN}" >/dev/null || true
  fi
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

create_file_resource() {
  local name="$1"
  local path="$2"
  local size_bytes="$3"
  local response
  response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/file-resources" \
    "{\"originalName\":\"${name}\",\"fileKind\":\"DOCUMENT\",\"mimeType\":\"application/pdf\",\"sizeBytes\":${size_bytes},\"storageUri\":\"nas://${path}\",\"versionNo\":\"V1\",\"processStatus\":\"PROCESSED\"}")"
  assert_ok "${response}"
  json_data_expr "${response}" "data['id']"
}

echo "=== M3C: storage migration task center ==="

echo ""
echo "--- 1. Login and switch to project ${PROJECT_ID} ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 20 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_data_expr "${login_response}" "data['accessToken']")"
switch_response="$(api_post "/api/core/projects/${PROJECT_ID}:switch" '{}')"
assert_ok "${switch_response}"
TOKEN="$(json_data_expr "${switch_response}" "data['accessToken']")"
pass "管理员登录并切换项目成功"

echo ""
echo "--- 2. Summary endpoint is sanitized and task-center ready ---"
summary_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-migration-summary")"
assert_ok "${summary_response}"
assert_no_forbidden "migration summary response" "${summary_response}"
if [[ "$(json_data_expr "${summary_response}" "data['projectId'] == int(${PROJECT_ID}) and data['totalFileCount'] >= data['objectStoredCount'] and data['nasOnlyCount'] >= 0 and data['maxFilesPerTask'] == 10")" == "true" ]]; then
  pass "对象存储迁移总览接口可访问且不暴露底层路径"
else
  fail "对象存储迁移总览接口返回异常"
fi

echo ""
echo "--- 3. Prepare isolated files for migration task center ---"
sample_file="${TMP_DIR}/m3c-task-center.pdf"
printf '%s\n' "M3C object storage migration task center smoke file" > "${sample_file}"
SMOKE_FILE_ID="$(create_file_resource "M3C-Storage-Migration-Task-Center.pdf" "${sample_file}" "$(wc -c < "${sample_file}")")"
MISSING_FILE_ID="$(create_file_resource "M3C-Storage-Migration-Missing.pdf" "${TMP_DIR}/missing.pdf" 64)"
pass "已准备隔离测试文件资源，不触碰真实业务 NAS 目录"

echo ""
echo "--- 4. Create explicit-file migration task and inspect detail ---"
create_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks" "{\"fileIds\":[${SMOKE_FILE_ID}],\"targetProvider\":\"MINIO\"}")"
assert_ok "${create_response}"
assert_no_forbidden "migration create response" "${create_response}"
TASK_ID="$(json_data_expr "${create_response}" "data['taskId']")"
TASK_IDS+=("${TASK_ID}")
if [[ "$(json_data_expr "${create_response}" "data['taskStatus'] == 'COMPLETED' and data['successCount'] == 1 and data['rows'][0]['assetUuid'] and data['rows'][0]['storageState'] == 'OBJECT_STORED' and data['rows'][0]['resultCode'] == 'MIRRORED'")" == "true" ]]; then
  pass "显式文件迁移任务完成，行级返回 assetUuid 与对象化状态"
else
  fail "显式文件迁移任务结果不符合预期"
fi

detail_response="$(api_get "/api/data-steward/storage-migration-tasks/${TASK_ID}")"
list_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks")"
summary_after_response="$(api_get "/api/data-steward/projects/${PROJECT_ID}/storage-migration-summary")"
assert_ok "${detail_response}"
assert_ok "${list_response}"
assert_ok "${summary_after_response}"
assert_no_forbidden "migration detail response" "${detail_response}"
assert_no_forbidden "migration list response" "${list_response}"
assert_no_forbidden "migration summary after response" "${summary_after_response}"
if [[ "$(json_data_expr "${list_response}" "any(item['taskId'] == int(${TASK_ID}) and item['storageState'] == 'OBJECT_STORED' for item in data)")" == "true" ]]; then
  pass "任务列表包含新建任务且仅展示业务状态"
else
  fail "任务列表未包含新建任务"
fi

echo ""
echo "--- 5. Idempotent rerun keeps one active object version ---"
version_count_before="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE file_id = ${SMOKE_FILE_ID} AND active = 1 AND deleted = 0;")"
rerun_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks" "{\"fileIds\":[${SMOKE_FILE_ID}],\"targetProvider\":\"MINIO\"}")"
assert_ok "${rerun_response}"
assert_no_forbidden "migration rerun response" "${rerun_response}"
RERUN_TASK_ID="$(json_data_expr "${rerun_response}" "data['taskId']")"
TASK_IDS+=("${RERUN_TASK_ID}")
version_count_after="$(mysql_exec "SELECT COUNT(1) FROM data_file_object_versions WHERE file_id = ${SMOKE_FILE_ID} AND active = 1 AND deleted = 0;")"
if [[ "$(json_data_expr "${rerun_response}" "data['taskStatus'] == 'COMPLETED' and data['skippedCount'] == 1 and data['rows'][0]['resultCode'] == 'ALREADY_STORED'")" == "true" ]] \
  && [[ "${version_count_before}" == "1" ]] \
  && [[ "${version_count_after}" == "1" ]]; then
  pass "重复迁移被幂等跳过，未重复创建活跃对象版本"
else
  fail "重复迁移幂等结果不符合预期"
fi

echo ""
echo "--- 6. Failure and retry stay safe and path-free ---"
failure_response="$(api_post "/api/data-steward/projects/${PROJECT_ID}/storage-migration-tasks" "{\"fileIds\":[${MISSING_FILE_ID}],\"targetProvider\":\"MINIO\"}")"
assert_ok "${failure_response}"
assert_no_forbidden "migration failure response" "${failure_response}"
FAIL_TASK_ID="$(json_data_expr "${failure_response}" "data['taskId']")"
TASK_IDS+=("${FAIL_TASK_ID}")
if [[ "$(json_data_expr "${failure_response}" "data['taskStatus'] == 'FAILED' and data['failureCount'] == 1 and data['rows'][0]['migrationStatus'] == 'FAILED' and len(data['rows'][0]['message']) > 0")" == "true" ]]; then
  pass "缺失源文件失败为业务化原因，未暴露真实路径"
else
  fail "缺失源文件失败结果不符合预期"
fi

retry_response="$(api_post "/api/data-steward/storage-migration-tasks/${FAIL_TASK_ID}:retry" '{}')"
assert_ok "${retry_response}"
assert_no_forbidden "migration retry response" "${retry_response}"
RETRY_TASK_ID="$(json_data_expr "${retry_response}" "data['taskId']")"
TASK_IDS+=("${RETRY_TASK_ID}")
if [[ "$(json_data_expr "${retry_response}" "data['taskStatus'] == 'FAILED' and data['failureCount'] == 1 and len(data['rows'][0]['assetUuid']) > 0")" == "true" ]]; then
  pass "失败任务可重试，结果仍只返回安全字段"
else
  fail "失败任务重试结果不符合预期"
fi

echo ""
echo "--- 7. Deliverable files are tracked by git ---"
if git ls-files --error-unmatch scripts/dev/check-m3c-storage-migration-task-center.sh >/dev/null 2>&1; then
  pass "M3C 专项脚本已纳入 Git 跟踪"
else
  fail "M3C 专项脚本未纳入 Git 跟踪"
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
