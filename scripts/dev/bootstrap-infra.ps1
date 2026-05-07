. "$PSScriptRoot/common.ps1"

$repoRoot = Get-RepoRoot
$infraDir = Join-Path $repoRoot "infra"

Set-Location $infraDir
& docker compose --env-file .env.example up -d
exit $LASTEXITCODE
