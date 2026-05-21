. "$PSScriptRoot/common.ps1"

$repoRoot = Get-RepoRoot
$frontendDir = Join-Path $repoRoot "frontend"

Set-Location $frontendDir

& corepack pnpm install
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

if (-not $env:VITE_API_PROXY_TARGET) {
  $env:VITE_API_PROXY_TARGET = "http://localhost:18080"
}

if (-not $env:VITE_FRONTEND_PORT) {
  $env:VITE_FRONTEND_PORT = "5174"
}

& corepack pnpm dev --host 0.0.0.0 --port $env:VITE_FRONTEND_PORT --strictPort
exit $LASTEXITCODE
