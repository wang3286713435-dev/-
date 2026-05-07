Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
  return Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
}

function Write-Step {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Name
  )

  Write-Host "== $Name =="
}

function Get-AuthHeaders {
  param(
    [Parameter(Mandatory = $true)]
    [string]$AccessToken
  )

  return @{
    Authorization = "Bearer $AccessToken"
  }
}

function Invoke-ApiJson {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Uri,

    [ValidateSet("GET", "POST", "PATCH", "PUT", "DELETE")]
    [string]$Method = "GET",

    [hashtable]$Headers = @{},

    $Body
  )

  $request = @{
    Uri     = $Uri
    Method  = $Method
    Headers = $Headers
  }

  if ($PSBoundParameters.ContainsKey("Body")) {
    $request["ContentType"] = "application/json"
    $request["Body"] = if ($Body -is [string]) {
      $Body
    } else {
      $Body | ConvertTo-Json -Depth 20 -Compress
    }
  }

  $response = Invoke-RestMethod @request
  Write-Host ($response | ConvertTo-Json -Depth 100 -Compress)
  return $response
}

function Assert-ApiOk {
  param(
    [Parameter(Mandatory = $true)]
    $Response
  )

  if ($Response.code -ne "OK") {
    throw "Expected response code OK, got '$($Response.code)'."
  }
}

function Assert-ApiCode {
  param(
    [Parameter(Mandatory = $true)]
    $Response,

    [Parameter(Mandatory = $true)]
    [string]$ExpectedCode
  )

  if ($Response.code -ne $ExpectedCode) {
    throw "Expected response code '$ExpectedCode', got '$($Response.code)'."
  }
}

function Get-ApiDataId {
  param(
    [Parameter(Mandatory = $true)]
    $Response
  )

  return [string]$Response.data.id
}

function Get-DefaultSuffix {
  return [DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString()
}

function Start-ApiSession {
  param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,

    [Parameter(Mandatory = $true)]
    [string]$Username,

    [Parameter(Mandatory = $true)]
    [string]$Password,

    [Parameter(Mandatory = $true)]
    [string]$TargetProjectId
  )

  Write-Step "login"
  $loginResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/core/auth/login" -Body @{
    username = $Username
    password = $Password
  }
  Assert-ApiOk $loginResponse

  $accessToken = [string]$loginResponse.data.accessToken
  $currentProjectId = [string]$loginResponse.data.currentProjectId

  if ($currentProjectId -ne $TargetProjectId) {
    Write-Step "switch project"
    $switchResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/core/projects/$($TargetProjectId):switch" -Headers (Get-AuthHeaders -AccessToken $accessToken)
    Assert-ApiOk $switchResponse
    $accessToken = [string]$switchResponse.data.accessToken
  }

  return $accessToken
}
