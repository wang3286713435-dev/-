. "$PSScriptRoot/common.ps1"

$repoRoot = Get-RepoRoot
$frontendDir = Join-Path $repoRoot "frontend"

Set-Location $frontendDir

& corepack pnpm install
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& corepack pnpm dev --host 0.0.0.0
exit $LASTEXITCODE
