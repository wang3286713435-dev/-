#!/usr/bin/env bash
set -euo pipefail

DB_CONTAINER="${DB_CONTAINER:-delivery-mysql}"
DB_NAME="${DB_NAME:-delivery_platform}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
READONLY_USER="${READONLY_USER:-hermes_agent_ro}"
READONLY_PASSWORD="${READONLY_PASSWORD:-}"
KEYCHAIN_SERVICE="${KEYCHAIN_SERVICE:-delivery-platform-hermes-agent-ro-local-dev}"
KEYCHAIN_ACCOUNT="${KEYCHAIN_ACCOUNT:-hermes_agent_ro}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
ALLOW_LIMIT_30="${ALLOW_LIMIT_30:-false}"

required_views=(ProjectAssetView FileAssetView ModelAssetView AuditEventView)
forbidden_tables=(core_projects data_file_resources core_audit_logs)

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

info() {
  echo "== $* =="
}

if [[ -z "${READONLY_PASSWORD}" ]] && command -v security >/dev/null 2>&1; then
  READONLY_PASSWORD="$(security find-generic-password -s "${KEYCHAIN_SERVICE}" -a "${KEYCHAIN_ACCOUNT}" -w 2>/dev/null || true)"
fi

if [[ -z "${READONLY_PASSWORD}" ]]; then
  fail "READONLY_PASSWORD is empty. Set READONLY_PASSWORD or store it in macOS Keychain service ${KEYCHAIN_SERVICE}, account ${KEYCHAIN_ACCOUNT}."
fi

use_docker=0
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -qx "${DB_CONTAINER}"; then
  use_docker=1
fi

mysql_ro() {
  local sql="$1"
  if [[ "${use_docker}" == "1" ]]; then
    docker exec -i -e MYSQL_PWD="${READONLY_PASSWORD}" "${DB_CONTAINER}" \
      mysql --protocol=TCP -h127.0.0.1 -P3306 --default-character-set=utf8mb4 \
      --batch --raw --silent -u"${READONLY_USER}" "${DB_NAME}" -e "${sql}"
  else
    MYSQL_PWD="${READONLY_PASSWORD}" "${MYSQL_BIN}" --protocol=TCP -h"${DB_HOST}" -P"${DB_PORT}" \
      --default-character-set=utf8mb4 --batch --raw --silent -u"${READONLY_USER}" "${DB_NAME}" -e "${sql}"
  fi
}

expect_forbidden() {
  local sql="$1"
  local label="$2"
  local tmp
  tmp="$(mktemp)"
  if mysql_ro "${sql}" >"${tmp}" 2>&1; then
    cat "${tmp}" >&2
    rm -f "${tmp}"
    fail "${label} unexpectedly succeeded"
  fi
  rm -f "${tmp}"
}

assert_columns() {
  local view="$1"
  shift
  local output
  output="$(mysql_ro "SHOW COLUMNS FROM ${view};")"
  python3 -c '
import sys

view = sys.argv[1]
required = set(sys.argv[2:])
lines = [line.split("\t", 1)[0] for line in sys.stdin.read().splitlines() if line.strip()]
actual = set(lines)
missing = sorted(required - actual)
if missing:
    raise SystemExit(f"{view} missing columns: {missing}; actual={sorted(actual)}")
print(f"{view}: {len(actual)} columns OK")
' "${view}" "$@" <<<"${output}"
}

info "connect with readonly user"
database_name="$(mysql_ro "SELECT DATABASE();")"
if [[ "${database_name}" != "${DB_NAME}" ]]; then
  fail "Unexpected database: ${database_name}, expected ${DB_NAME}"
fi
echo "database=${database_name}"

info "verify visible views"
visible_views="$(mysql_ro "SHOW FULL TABLES WHERE Table_type='VIEW';")"
python3 -c '
import sys

required = set(sys.argv[1:])
actual = set()
for line in sys.stdin.read().splitlines():
    if not line.strip():
        continue
    actual.add(line.split("\t", 1)[0])
missing = sorted(required - actual)
extra = sorted(actual - required)
if missing:
    raise SystemExit(f"missing required views: {missing}; actual={sorted(actual)}")
if extra:
    raise SystemExit(f"readonly user can see unexpected views: {extra}; expected only={sorted(required)}")
print("visible views OK: " + ", ".join(sorted(actual)))
' "${required_views[@]}" <<<"${visible_views}"

info "verify required columns"
assert_columns ProjectAssetView \
  project_id project_code project_name project_stage discipline_scope manager_name owner_org_name asset_status model_file_count total_size_bytes last_asset_updated_at
assert_columns FileAssetView \
  file_id project_id project_code project_name file_name file_ext file_kind discipline version_no size_bytes checksum storage_provider storage_path logical_path source_type process_status created_at updated_at
assert_columns ModelAssetView \
  model_id file_id project_code model_name model_format discipline version_no preview_available lightweight_status component_index_status storage_path updated_at
assert_columns AuditEventView \
  event_id project_id module_code action_code target_type target_id operator_id summary created_at

info "verify view counts without printing sample rows"
for view in "${required_views[@]}"; do
  count="$(mysql_ro "SELECT COUNT(*) FROM ${view};")"
  [[ "${count}" =~ ^[0-9]+$ ]] || fail "${view} count is not numeric: ${count}"
  echo "${view}.count=${count}"
done

info "verify event cursor"
cursor_row="$(mysql_ro "SELECT event_id, created_at FROM AuditEventView ORDER BY event_id DESC LIMIT 1;")"
if [[ -z "${cursor_row}" ]]; then
  fail "AuditEventView has no cursor row"
fi
echo "AuditEventView cursor OK"

info "verify business tables are forbidden"
for table in "${forbidden_tables[@]}"; do
  expect_forbidden "SELECT COUNT(*) FROM ${table};" "${table}"
  echo "${table}: forbidden OK"
done

info "sample policy"
if [[ "${ALLOW_LIMIT_30}" == "true" ]]; then
  echo "ALLOW_LIMIT_30=true: verifying LIMIT 30 queries execute, but not printing rows"
  for view in "${required_views[@]}"; do
    mysql_ro "SELECT * FROM ${view} LIMIT 30;" >/dev/null
    echo "${view}.limit30=OK"
  done
else
  echo "ALLOW_LIMIT_30=false: using structure/count validation only; no sample rows printed"
fi

echo "agent db2 contract ok"
