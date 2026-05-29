#!/usr/bin/env bash
# M3G-5: file manager project-wide search and storage display smoke.
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USER="${ADMIN_USER:-platform.admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PROJECT_ID="${PROJECT_ID:-503}"
DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root123}"

PASS=0
FAIL=0
TOKEN=""

pass() {
  PASS=$((PASS + 1))
  printf '  [PASS] %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf '  [FAIL] %s\n' "$1" >&2
}

mysql_exec() {
  docker exec "${DB_CONTAINER}" mysql --default-character-set=utf8mb4 -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -N -B -e "$1"
}

json_expr() {
  local response="$1"
  local expr="$2"
  RESPONSE="${response}" EXPR="${expr}" python3 - <<'PY'
import json
import os
data = json.loads(os.environ["RESPONSE"])
scope = {
    "__builtins__": {},
    "data": data,
    "len": len,
    "all": all,
    "any": any,
    "bool": bool,
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
    r"\bSQL\b",
    r"\btoken\b",
    r"\bsecret\b",
    r"\bpassword\b",
    r"\baccess[_-]?key\b",
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
  curl -sS --connect-timeout 3 --max-time 60 -X GET "${BASE_URL}${url}" \
    -H "Authorization: Bearer ${TOKEN}"
}

urlencode() {
  VALUE="$1" python3 - <<'PY'
import os
import urllib.parse
print(urllib.parse.quote(os.environ["VALUE"], safe=""))
PY
}

decode_hex() {
  VALUE="$1" python3 - <<'PY'
import os
print(bytes.fromhex(os.environ["VALUE"]).decode("utf-8"))
PY
}

nas_path_from_uri() {
  local uri="$1"
  local path="${uri#nas://}"
  if [[ "${path}" != /* ]]; then
    path="/${path}"
  fi
  printf '%s' "${path}"
}

file_stat_signature() {
  local path="$1"
  PATH_VALUE="${path}" python3 - <<'PY'
import os
path = os.environ["PATH_VALUE"]
st = os.stat(path)
print(f"{st.st_size}:{int(st.st_mtime)}")
PY
}

task_count() {
  mysql_exec "SELECT COUNT(1) FROM data_object_migration_tasks WHERE project_id = ${PROJECT_ID} AND deleted = 0;" 2>/dev/null | head -n 1
}

echo "=== M3G-5: file manager search and storage display ==="

echo ""
echo "--- 1. Login administrator ---"
login_response="$(curl -sS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/core/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
assert_ok "${login_response}"
TOKEN="$(json_expr "${login_response}" "data['data']['accessToken']")"
if [[ -n "${TOKEN}" ]]; then
  pass "管理员登录成功"
else
  fail "管理员登录失败"
fi

TASKS_BEFORE="$(task_count)"

echo ""
echo "--- 2. Inventory shows mixed storage states for project ${PROJECT_ID} ---"
inventory_response="$(api_get "/api/data-steward/storage-objectification-inventory")"
assert_ok "${inventory_response}"
assert_no_forbidden "inventory" "${inventory_response}"
inventory_ok="$(PROJECT_ID="${PROJECT_ID}" RESPONSE="${inventory_response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
project_id = int(os.environ["PROJECT_ID"])
for item in payload["data"]["projects"]:
    if int(item["projectId"]) == project_id:
        print("true" if int(item.get("objectStoredFiles") or 0) > 0 and int(item.get("nasOnlyFiles") or 0) > 0 else "false")
        raise SystemExit(0)
print("false")
PY
)"
if [[ "${inventory_ok}" == "true" ]]; then
  pass "105/503 仍能区分已对象化文件和历史 NAS 文件"
else
  fail "105/503 对象化覆盖率不符合 M3G-5 混合状态预期"
fi

echo ""
echo "--- 3. Root directory children stay direct-only and sanitized ---"
direct_response="$(api_get "/api/data-steward/catalog/directory-children?projectId=${PROJECT_ID}&page=1&pageSize=50")"
assert_ok "${direct_response}"
assert_no_forbidden "root direct children" "${direct_response}"
direct_ok="$(json_expr "${direct_response}" "len(data['data']['directories']) > 0 and all(item.get('physicalDirectory') in [True, False] for item in data['data']['directories'])")"
if [[ "${direct_ok}" == "true" ]]; then
  pass "根目录直接子项接口返回直接目录且不泄露底层路径"
else
  fail "根目录直接子项接口结果异常：${direct_response}"
fi

echo ""
echo "--- 4. Pick nested search sample and verify global/current-folder scopes ---"
sample_row="$(mysql_exec "
  SELECT f.id, HEX(f.original_name), HEX(f.logical_path)
  FROM data_file_resources f
  WHERE f.project_id = ${PROJECT_ID}
    AND f.deleted = 0
    AND f.logical_path LIKE '%/%'
  ORDER BY f.id
  LIMIT 1;
" 2>/dev/null | head -n 1 || true)"
if [[ -z "${sample_row}" ]]; then
  fail "未找到项目内嵌套目录文件样本"
else
  pass "已选择嵌套文件样本用于搜索范围验证"
fi
read -r SAMPLE_FILE_ID SAMPLE_NAME_HEX SAMPLE_PATH_HEX <<< "${sample_row}"
SAMPLE_NAME="$(decode_hex "${SAMPLE_NAME_HEX}")"
SAMPLE_PATH="$(decode_hex "${SAMPLE_PATH_HEX}")"
SAMPLE_PARENT="$(SAMPLE_PATH="${SAMPLE_PATH}" python3 - <<'PY'
import os
path = os.environ["SAMPLE_PATH"].strip("/").replace("\\", "/")
parts = [item for item in path.split("/") if item]
print("/".join(parts[:-1]))
PY
)"
SAMPLE_KEYWORD="$(urlencode "${SAMPLE_NAME}")"
SAMPLE_PARENT_QUERY="$(urlencode "${SAMPLE_PARENT}")"

global_response="$(api_get "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&keyword=${SAMPLE_KEYWORD}&page=1&pageSize=20")"
assert_ok "${global_response}"
assert_no_forbidden "global search" "${global_response}"
global_ok="$(SAMPLE_FILE_ID="${SAMPLE_FILE_ID}" RESPONSE="${global_response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
sample = int(os.environ["SAMPLE_FILE_ID"])
rows = payload["data"]["items"]
print("true" if any(int(item.get("fileId") or 0) == sample and item.get("logicalPath") for item in rows) else "false")
PY
)"
if [[ "${global_ok}" == "true" ]]; then
  pass "默认关键词搜索不受当前目录限制，并返回项目内位置提示"
else
  fail "默认关键词搜索未找到全项目样本：${global_response}"
fi

current_response="$(api_get "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&keyword=${SAMPLE_KEYWORD}&directoryPath=${SAMPLE_PARENT_QUERY}&page=1&pageSize=20")"
assert_ok "${current_response}"
assert_no_forbidden "current folder search" "${current_response}"
current_ok="$(SAMPLE_FILE_ID="${SAMPLE_FILE_ID}" RESPONSE="${current_response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE"])
sample = int(os.environ["SAMPLE_FILE_ID"])
print("true" if any(int(item.get("fileId") or 0) == sample for item in payload["data"]["items"]) else "false")
PY
)"
if [[ "${current_ok}" == "true" ]]; then
  pass "当前文件夹及子目录搜索可命中对应目录下文件"
else
  fail "当前文件夹及子目录搜索未命中样本：${current_response}"
fi

miss_dir="$(urlencode "__m3g5_no_such_dir__")"
miss_response="$(api_get "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&keyword=${SAMPLE_KEYWORD}&directoryPath=${miss_dir}&page=1&pageSize=20")"
assert_ok "${miss_response}"
assert_no_forbidden "current folder miss search" "${miss_response}"
if [[ "$(json_expr "${miss_response}" "int(data['data']['total']) == 0")" == "true" ]]; then
  pass "当前文件夹开关会限制搜索范围"
else
  fail "当前文件夹限制未生效：${miss_response}"
fi

echo ""
echo "--- 5. Frontend search-mode contract is explicit ---"
FRONTEND_FILE="frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue"
python3 - <<'PY'
from pathlib import Path
source = Path("frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue").read_text()
assert "if (hasKeyword.value) return fileEntries.value;" in source, "search mode must exclude directory entries"
assert "route.query.fileKeyword" in source, "fileKeyword query must be watched"
assert "applyBrowserState(state, true)" in source, "query changes must re-apply browser state"
PY
pass "前端搜索模式只渲染文件行，且 fileKeyword query 可驱动状态刷新"

echo ""
echo "--- 6. Storage display fields are business-level and sanitized ---"
object_row="$(mysql_exec "
  SELECT f.id, HEX(f.original_name)
  FROM data_file_resources f
  JOIN data_file_object_versions fov ON fov.file_id = f.id
    AND fov.active = 1
    AND fov.deleted = 0
    AND fov.storage_state = 'OBJECT_STORED'
  WHERE f.project_id = ${PROJECT_ID}
    AND f.deleted = 0
  ORDER BY f.id
  LIMIT 1;
" 2>/dev/null | head -n 1 || true)"
nas_row="$(mysql_exec "
  SELECT f.id, HEX(f.original_name), f.storage_uri
  FROM data_file_resources f
  LEFT JOIN data_file_object_versions fov ON fov.file_id = f.id
    AND fov.active = 1
    AND fov.deleted = 0
    AND fov.storage_state = 'OBJECT_STORED'
  WHERE f.project_id = ${PROJECT_ID}
    AND f.deleted = 0
    AND fov.file_id IS NULL
    AND f.storage_uri IS NOT NULL
    AND f.storage_uri <> ''
  ORDER BY f.id
  LIMIT 1;
" 2>/dev/null | head -n 1 || true)"
read -r OBJECT_FILE_ID OBJECT_NAME_HEX <<< "${object_row}"
read -r NAS_FILE_ID NAS_NAME_HEX NAS_STORAGE_URI <<< "${nas_row}"
OBJECT_NAME="$(decode_hex "${OBJECT_NAME_HEX}")"
NAS_NAME="$(decode_hex "${NAS_NAME_HEX}")"
object_search="$(api_get "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&keyword=$(urlencode "${OBJECT_NAME}")&page=1&pageSize=20")"
nas_search="$(api_get "/api/data-steward/catalog/files?projectId=${PROJECT_ID}&keyword=$(urlencode "${NAS_NAME}")&page=1&pageSize=20")"
assert_ok "${object_search}"
assert_ok "${nas_search}"
assert_no_forbidden "object stored search" "${object_search}"
assert_no_forbidden "nas only search" "${nas_search}"
storage_ok="$(OBJECT_FILE_ID="${OBJECT_FILE_ID}" NAS_FILE_ID="${NAS_FILE_ID}" OBJECT_RESPONSE="${object_search}" NAS_RESPONSE="${nas_search}" python3 - <<'PY'
import json
import os
object_id = int(os.environ["OBJECT_FILE_ID"])
nas_id = int(os.environ["NAS_FILE_ID"])
object_rows = json.loads(os.environ["OBJECT_RESPONSE"])["data"]["items"]
nas_rows = json.loads(os.environ["NAS_RESPONSE"])["data"]["items"]
object_ok = any(int(item.get("fileId") or 0) == object_id and item.get("storageState") == "OBJECT_STORED" and item.get("accessSource") == "NAS_SIDE_MINIO" for item in object_rows)
nas_ok = any(int(item.get("fileId") or 0) == nas_id and item.get("storageState") == "NAS_ONLY" for item in nas_rows)
print("true" if object_ok and nas_ok else "false")
PY
)"
if [[ "${storage_ok}" == "true" ]]; then
  pass "catalog/files 返回 OBJECT_STORED 与 NAS_ONLY 的脱敏业务字段"
else
  fail "catalog/files 存储状态字段异常"
fi

detail_response="$(api_get "/api/data-steward/catalog/files/${OBJECT_FILE_ID}")"
assert_ok "${detail_response}"
assert_no_forbidden "catalog detail" "${detail_response}"
if [[ "$(json_expr "${detail_response}" "data['data'].get('storagePath') in [None, ''] and data['data'].get('storageState') == 'OBJECT_STORED'")" == "true" ]]; then
  pass "文件详情不返回真实存储地址，只返回存储状态"
else
  fail "文件详情仍可能返回真实存储地址：${detail_response}"
fi

echo ""
echo "--- 7. No migration tasks and no NAS original mutation ---"
NAS_PATH="$(nas_path_from_uri "${NAS_STORAGE_URI}")"
if [[ -f "${NAS_PATH}" ]]; then
  NAS_STAT_BEFORE="$(file_stat_signature "${NAS_PATH}")"
else
  NAS_STAT_BEFORE=""
fi
TASKS_AFTER="$(task_count)"
if [[ "${TASKS_AFTER}" == "${TASKS_BEFORE}" ]]; then
  pass "脚本未创建对象化迁移任务"
else
  fail "对象化迁移任务数量发生变化：before=${TASKS_BEFORE} after=${TASKS_AFTER}"
fi
if [[ -z "${NAS_STAT_BEFORE}" || "$(file_stat_signature "${NAS_PATH}")" == "${NAS_STAT_BEFORE}" ]]; then
  pass "NAS 原文件 size/mtime 未变化"
else
  fail "NAS 原文件状态发生变化"
fi

echo ""
echo "--- 8. Script tracking ---"
if git ls-files --error-unmatch scripts/dev/check-m3g5-file-manager-search-storage-display.sh >/dev/null 2>&1; then
  pass "M3G-5 专项脚本已纳入 Git 跟踪"
else
  fail "M3G-5 专项脚本尚未纳入 Git 跟踪"
fi

echo ""
echo "=== Result: PASS=${PASS} FAIL=${FAIL} ==="
if [[ "${FAIL}" -ne 0 ]]; then
  exit 1
fi
