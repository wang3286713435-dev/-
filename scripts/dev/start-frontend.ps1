. "$PSScriptRoot/common.ps1"

$repoRoot = Get-RepoRoot
$frontendDir = Join-Path $repoRoot "frontend"
$devPort = if ($env:VITE_DEV_PORT) { $env:VITE_DEV_PORT } else { "5174" }
$backendEnabled = if ($env:VITE_C_TOWER_BACKEND_ENABLED) { $env:VITE_C_TOWER_BACKEND_ENABLED } else { "false" }
$apiTarget = if ($env:VITE_API_TARGET) { $env:VITE_API_TARGET } else { "http://127.0.0.1:18080" }

Set-Location $frontendDir
$env:VITE_DEV_PORT = $devPort
$env:VITE_C_TOWER_BACKEND_ENABLED = $backendEnabled
$env:VITE_API_TARGET = $apiTarget

& corepack pnpm install
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& corepack pnpm dev --host 0.0.0.0 --port $devPort
exit $LASTEXITCODE
