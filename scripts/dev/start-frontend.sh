#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VITE_DEV_PORT="${VITE_DEV_PORT:-5174}"
VITE_C_TOWER_BACKEND_ENABLED="${VITE_C_TOWER_BACKEND_ENABLED:-false}"
VITE_API_TARGET="${VITE_API_TARGET:-http://127.0.0.1:18080}"
export VITE_DEV_PORT VITE_C_TOWER_BACKEND_ENABLED VITE_API_TARGET

cd "${ROOT_DIR}/frontend"
corepack pnpm install
corepack pnpm dev --host 0.0.0.0 --port "${VITE_DEV_PORT}"
