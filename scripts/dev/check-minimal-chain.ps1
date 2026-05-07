param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Username = "platform.admin",
  [string]$Password = "Admin@123",
  [string]$TargetProjectId = "2"
)

. "$PSScriptRoot/common.ps1"

Write-Step "login"
$loginResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/core/auth/login" -Body @{
  username = $Username
  password = $Password
}

$accessToken = [string]$loginResponse.data.accessToken
$refreshToken = [string]$loginResponse.data.refreshToken

Write-Step "refresh token"
$refreshResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/core/auth/refresh" -Body @{
  refreshToken = $refreshToken
}

Write-Step "current user"
$meResponse = Invoke-ApiJson -Uri "$BaseUrl/api/core/users/me" -Headers (Get-AuthHeaders -AccessToken $accessToken)

Write-Step "switch project"
$switchResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/core/projects/$($TargetProjectId):switch" -Headers (Get-AuthHeaders -AccessToken $accessToken)
$switchedAccessToken = [string]$switchResponse.data.accessToken

Write-Step "home overview"
$overviewResponse = Invoke-ApiJson -Uri "$BaseUrl/api/work-center/projects/$TargetProjectId/home/overview" -Headers (Get-AuthHeaders -AccessToken $switchedAccessToken)

Assert-ApiOk $loginResponse
Assert-ApiOk $refreshResponse
Assert-ApiOk $meResponse
Assert-ApiOk $switchResponse
Assert-ApiOk $overviewResponse
