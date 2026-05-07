param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Username = "platform.admin",
  [string]$Password = "Admin@123",
  [string]$TargetProjectId = "2",
  [string]$Suffix = ([DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString())
)

. "$PSScriptRoot/common.ps1"

$accessToken = Start-ApiSession -BaseUrl $BaseUrl -Username $Username -Password $Password -TargetProjectId $TargetProjectId
$authHeaders = Get-AuthHeaders -AccessToken $accessToken

Write-Step "standard status before"
$statusBefore = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/standard-status" -Headers $authHeaders
Assert-ApiOk $statusBefore

$sectionCode = "SMOKE-SEC-$Suffix"
$sectionName = "冒烟部位-$Suffix"

Write-Step "create section node"
$createSectionResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes" -Headers $authHeaders -Body @{
  code      = $sectionCode
  name      = $sectionName
  sortOrder = 10
  status    = "ACTIVE"
}
Assert-ApiOk $createSectionResponse
$sectionId = Get-ApiDataId $createSectionResponse

Write-Step "update section node"
$updateSectionResponse = Invoke-ApiJson -Method PATCH -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes/$sectionId" -Headers $authHeaders -Body @{
  code      = $sectionCode
  name      = "$sectionName-已编辑"
  sortOrder = 11
  status    = "ACTIVE"
}
Assert-ApiOk $updateSectionResponse

$childSectionCode = "SMOKE-SEC-CHILD-$Suffix"
$childSectionName = "冒烟子部位-$Suffix"

Write-Step "create child section node"
$createChildSectionResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes" -Headers $authHeaders -Body @{
  parentId  = [int64]$sectionId
  code      = $childSectionCode
  name      = $childSectionName
  sortOrder = 1
  status    = "ACTIVE"
}
Assert-ApiOk $createChildSectionResponse
$childSectionId = Get-ApiDataId $createChildSectionResponse

Write-Step "disable child section node"
$deleteChildSectionResponse = Invoke-ApiJson -Method DELETE -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes/$childSectionId" -Headers $authHeaders
Assert-ApiOk $deleteChildSectionResponse

$recycleCode = "SMOKE-RECYCLE-$Suffix"
$recycleName = "冒烟回收测试-$Suffix"

Write-Step "create recycle section node (first)"
$createRecycleResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes" -Headers $authHeaders -Body @{
  code      = $recycleCode
  name      = $recycleName
  sortOrder = 0
  status    = "ACTIVE"
}
Assert-ApiOk $createRecycleResponse
$recycleId = Get-ApiDataId $createRecycleResponse

Write-Step "delete recycle section node (first)"
$deleteRecycleResponse = Invoke-ApiJson -Method DELETE -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes/$recycleId" -Headers $authHeaders
Assert-ApiOk $deleteRecycleResponse

Write-Step "create recycle section node again (same code)"
$createRecycle2Response = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes" -Headers $authHeaders -Body @{
  code      = $recycleCode
  name      = $recycleName
  sortOrder = 0
  status    = "ACTIVE"
}
Assert-ApiOk $createRecycle2Response
$recycleId2 = Get-ApiDataId $createRecycle2Response

Write-Step "delete recycle section node again (same code, second delete)"
$deleteRecycle2Response = Invoke-ApiJson -Method DELETE -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes/$recycleId2" -Headers $authHeaders
Assert-ApiOk $deleteRecycle2Response

Write-Step "section tree"
$treeResponse = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes/tree" -Headers $authHeaders
Assert-ApiOk $treeResponse

$nodeTypeCode = "SMOKE-TYPE-$Suffix"
$nodeTypeName = "冒烟节点类型-$Suffix"

Write-Step "create node type"
$createNodeTypeResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/node-types" -Headers $authHeaders -Body @{
  code       = $nodeTypeCode
  name       = $nodeTypeName
  scopeLevel = 1
  sortOrder  = 10
  status     = "ACTIVE"
}
Assert-ApiOk $createNodeTypeResponse
$nodeTypeId = Get-ApiDataId $createNodeTypeResponse

Write-Step "update node type"
$updateNodeTypeResponse = Invoke-ApiJson -Method PATCH -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/node-types/$nodeTypeId" -Headers $authHeaders -Body @{
  code       = $nodeTypeCode
  name       = "$nodeTypeName-已编辑"
  scopeLevel = 2
  sortOrder  = 11
  status     = "ACTIVE"
}
Assert-ApiOk $updateNodeTypeResponse

Write-Step "node type list"
$nodeTypeListResponse = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/node-types" -Headers $authHeaders
Assert-ApiOk $nodeTypeListResponse

Write-Step "lock node type"
$lockResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/node-types/$($nodeTypeId):lock" -Headers $authHeaders
Assert-ApiOk $lockResponse

Write-Step "node type lock status"
$lockStatusResponse = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/node-types/$nodeTypeId/lock-status" -Headers $authHeaders
Assert-ApiOk $lockStatusResponse
if (-not $lockStatusResponse.data.locked) {
  throw "Expected node type $nodeTypeId to be locked."
}

Write-Step "standard status after"
$statusAfter = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/standard-status" -Headers $authHeaders
Assert-ApiOk $statusAfter

if (-not ($statusAfter.data.hasSectionTree -and $statusAfter.data.hasNodeTypes)) {
  throw "Expected standard status to report both section tree and node types as ready."
}

Write-Host "master-data chain ok: section=$sectionId, nodeType=$nodeTypeId"
