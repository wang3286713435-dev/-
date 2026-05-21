#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "${ROOT_DIR}/frontend"
corepack pnpm install
export VITE_API_PROXY_TARGET="${VITE_API_PROXY_TARGET:-http://localhost:18080}"
export VITE_FRONTEND_PORT="${VITE_FRONTEND_PORT:-5174}"
corepack pnpm dev --host 0.0.0.0 --port "${VITE_FRONTEND_PORT}" --strictPort
