param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Username = "admin",
  [string]$Password = "123456",
  [string]$TargetProjectId = "2",
  [string]$Suffix = ([DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString())
)

. "$PSScriptRoot/common.ps1"

$accessToken = Start-ApiSession -BaseUrl $BaseUrl -Username $Username -Password $Password -TargetProjectId $TargetProjectId
$authHeaders = Get-AuthHeaders -AccessToken $accessToken

Write-Step "current user menu includes deliverable standard"
$meResponse = Invoke-ApiJson -Uri "$BaseUrl/api/core/users/me" -Headers $authHeaders
Assert-ApiOk $meResponse

$menuPaths = @()
foreach ($menu in @($meResponse.data.menus)) {
  foreach ($child in @($menu.children)) {
    if ($null -ne $child -and $child.path) {
      $menuPaths += [string]$child.path
    }
  }
}

if ($menuPaths -notcontains "/master-data/deliverable-standard") {
  throw "Current user menu is missing /master-data/deliverable-standard."
}

$nodeTypeCode = "DST-NODE-$Suffix"
$nodeTypeName = "交付标准节点-$Suffix"

Write-Step "create unlocked node type for precondition"
$createNodeTypeResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/node-types" -Headers $authHeaders -Body @{
  code       = $nodeTypeCode
  name       = $nodeTypeName
  scopeLevel = 1
  sortOrder  = 30
  status     = "ACTIVE"
}
Assert-ApiOk $createNodeTypeResponse
$nodeTypeId = Get-ApiDataId $createNodeTypeResponse

$definitionCode = "DST-DEF-$Suffix"
$definitionName = "交付物定义-$Suffix"

Write-Step "write is blocked before node types locked"
$preconditionResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-definitions" -Headers $authHeaders -Body @{
  nodeTypeId = [int64]$nodeTypeId
  code       = $definitionCode
  name       = $definitionName
  category   = "DOCUMENT"
  required   = $true
  sortOrder  = 1
  status     = "ACTIVE"
}
Assert-ApiCode $preconditionResponse "MASTERDATA_NODE_TYPES_NOT_LOCKED"

Write-Step "lock all node types"
$lockAllResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/node-types:lock" -Headers $authHeaders
Assert-ApiOk $lockAllResponse

Write-Step "standard status after lock"
$statusAfterLock = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/standard-status" -Headers $authHeaders
Assert-ApiOk $statusAfterLock

if (-not $statusAfterLock.data.nodeTypesLocked) {
  throw "Expected nodeTypesLocked to be true after lock."
}

Write-Step "parent ownership/existence validation"
$missingDefinitionResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-definitions" -Headers $authHeaders -Body @{
  nodeTypeId = 999999999
  code       = "$definitionCode-MISS"
  name       = "$definitionName-缺失节点类型"
  category   = "DOCUMENT"
  required   = $true
  sortOrder  = 0
  status     = "ACTIVE"
}
Assert-ApiCode $missingDefinitionResponse "MASTERDATA_NODE_TYPE_NOT_FOUND"

$missingTypeResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-types" -Headers $authHeaders -Body @{
  deliverableDefinitionId = 999999999
  code                    = "DST-TYPE-MISS-$Suffix"
  name                    = "缺失定义类型-$Suffix"
  fileKind                = "DOCUMENT"
  bindingStrategy         = "SECTION_NODE"
  sortOrder               = 0
  status                  = "ACTIVE"
}
Assert-ApiCode $missingTypeResponse "MASTERDATA_DELIVERABLE_DEF_NOT_FOUND"

$missingAttributeResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-attributes" -Headers $authHeaders -Body @{
  deliverableTypeId = 999999999
  code              = "DST-ATTR-MISS-$Suffix"
  name              = "缺失类型属性-$Suffix"
  valueType         = "TEXT"
  sortOrder         = 0
  status            = "ACTIVE"
}
Assert-ApiCode $missingAttributeResponse "MASTERDATA_DELIVERABLE_TYPE_NOT_FOUND"

Write-Step "create deliverable definition"
$createDefinitionResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-definitions" -Headers $authHeaders -Body @{
  nodeTypeId = [int64]$nodeTypeId
  code       = $definitionCode
  name       = $definitionName
  category   = "DOCUMENT"
  required   = $true
  sortOrder  = 1
  status     = "ACTIVE"
}
Assert-ApiOk $createDefinitionResponse
$definitionId = Get-ApiDataId $createDefinitionResponse

Write-Step "update deliverable definition"
$updateDefinitionResponse = Invoke-ApiJson -Method PATCH -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-definitions/$definitionId" -Headers $authHeaders -Body @{
  code      = $definitionCode
  name      = "$definitionName-已编辑"
  category  = "DOCUMENT"
  required  = $true
  sortOrder = 2
  status    = "ACTIVE"
}
Assert-ApiOk $updateDefinitionResponse

Write-Step "list deliverable definitions"
$definitionListResponse = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-definitions" -Headers $authHeaders
Assert-ApiOk $definitionListResponse

if (-not (@($definitionListResponse.data) | Where-Object { [string]$_.id -eq $definitionId })) {
  throw "Expected definition $definitionId in deliverable definition list."
}

Write-Step "delete and recreate deliverable definition with same code"
$deleteDefinitionResponse = Invoke-ApiJson -Method DELETE -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-definitions/$definitionId" -Headers $authHeaders
Assert-ApiOk $deleteDefinitionResponse

$recreateDefinitionResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-definitions" -Headers $authHeaders -Body @{
  nodeTypeId = [int64]$nodeTypeId
  code       = $definitionCode
  name       = "$definitionName-复建"
  category   = "DOCUMENT"
  required   = $true
  sortOrder  = 3
  status     = "ACTIVE"
}
Assert-ApiOk $recreateDefinitionResponse
$definitionId = Get-ApiDataId $recreateDefinitionResponse

$typeCode = "DST-TYPE-$Suffix"
$typeName = "交付物类型-$Suffix"

Write-Step "create deliverable type"
$createTypeResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-types" -Headers $authHeaders -Body @{
  deliverableDefinitionId = [int64]$definitionId
  code                    = $typeCode
  name                    = $typeName
  fileKind                = "DOCUMENT"
  bindingStrategy         = "SECTION_NODE"
  sortOrder               = 1
  status                  = "ACTIVE"
}
Assert-ApiOk $createTypeResponse
$typeId = Get-ApiDataId $createTypeResponse

Write-Step "update deliverable type"
$updateTypeResponse = Invoke-ApiJson -Method PATCH -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-types/$typeId" -Headers $authHeaders -Body @{
  code            = $typeCode
  name            = "$typeName-已编辑"
  fileKind        = "DOCUMENT"
  bindingStrategy = "SECTION_NODE"
  sortOrder       = 2
  status          = "ACTIVE"
}
Assert-ApiOk $updateTypeResponse

Write-Step "list deliverable types by definition"
$typeListResponse = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-types?definitionId=$definitionId" -Headers $authHeaders
Assert-ApiOk $typeListResponse

if (-not (@($typeListResponse.data) | Where-Object { [string]$_.id -eq $typeId })) {
  throw "Expected deliverable type $typeId in type list."
}

Write-Step "delete and recreate deliverable type with same code"
$deleteTypeResponse = Invoke-ApiJson -Method DELETE -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-types/$typeId" -Headers $authHeaders
Assert-ApiOk $deleteTypeResponse

$recreateTypeResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-types" -Headers $authHeaders -Body @{
  deliverableDefinitionId = [int64]$definitionId
  code                    = $typeCode
  name                    = "$typeName-复建"
  fileKind                = "DOCUMENT"
  bindingStrategy         = "SECTION_NODE"
  sortOrder               = 3
  status                  = "ACTIVE"
}
Assert-ApiOk $recreateTypeResponse
$typeId = Get-ApiDataId $recreateTypeResponse

$attrCode = "DST-ATTR-$Suffix"
$attrName = "交付物属性-$Suffix"

Write-Step "create deliverable attribute"
$createAttrResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-attributes" -Headers $authHeaders -Body @{
  deliverableTypeId = [int64]$typeId
  code              = $attrCode
  name              = $attrName
  valueType         = "TEXT"
  unit              = "mm"
  required          = $true
  exampleValue      = "A1"
  enumOptions       = "A1,A2"
  sortOrder         = 1
  status            = "ACTIVE"
}
Assert-ApiOk $createAttrResponse
$attrId = Get-ApiDataId $createAttrResponse

Write-Step "update deliverable attribute"
$updateAttrResponse = Invoke-ApiJson -Method PATCH -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-attributes/$attrId" -Headers $authHeaders -Body @{
  code         = $attrCode
  name         = "$attrName-已编辑"
  valueType    = "TEXT"
  unit         = "m"
  required     = $true
  exampleValue = "B1"
  enumOptions  = "B1,B2"
  sortOrder    = 2
  status       = "ACTIVE"
}
Assert-ApiOk $updateAttrResponse

Write-Step "list deliverable attributes by type"
$attrListResponse = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-attributes?typeId=$typeId" -Headers $authHeaders
Assert-ApiOk $attrListResponse

if (-not (@($attrListResponse.data) | Where-Object { [string]$_.id -eq $attrId })) {
  throw "Expected deliverable attribute $attrId in attribute list."
}

Write-Step "delete and recreate deliverable attribute with same code"
$deleteAttrResponse = Invoke-ApiJson -Method DELETE -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-attributes/$attrId" -Headers $authHeaders
Assert-ApiOk $deleteAttrResponse

$recreateAttrResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-attributes" -Headers $authHeaders -Body @{
  deliverableTypeId = [int64]$typeId
  code              = $attrCode
  name              = "$attrName-复建"
  valueType         = "TEXT"
  unit              = "mm"
  required          = $true
  exampleValue      = "C1"
  enumOptions       = "C1,C2"
  sortOrder         = 3
  status            = "ACTIVE"
}
Assert-ApiOk $recreateAttrResponse
$attrId = Get-ApiDataId $recreateAttrResponse

$templateName = "交付目录模板-$Suffix"

Write-Step "create directory template"
$createTemplateResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/directory-templates" -Headers $authHeaders -Body @{
  templateType = "DOCUMENT"
  name         = $templateName
  rootNodeJson = '{"children":[]}'
  sourceType   = "MANUAL"
  sortOrder    = 1
  status       = "ACTIVE"
}
Assert-ApiOk $createTemplateResponse
$templateId = Get-ApiDataId $createTemplateResponse

Write-Step "update directory template"
$updateTemplateResponse = Invoke-ApiJson -Method PATCH -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/directory-templates/$templateId" -Headers $authHeaders -Body @{
  templateType = "DOCUMENT"
  name         = "$templateName-已编辑"
  rootNodeJson = '{"children":["doc"]}'
  sourceType   = "MANUAL"
  sortOrder    = 2
  status       = "ACTIVE"
}
Assert-ApiOk $updateTemplateResponse

Write-Step "list directory templates"
$templateListResponse = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/directory-templates" -Headers $authHeaders
Assert-ApiOk $templateListResponse

if (-not (@($templateListResponse.data) | Where-Object { [string]$_.id -eq $templateId })) {
  throw "Expected directory template $templateId in template list."
}

Write-Step "delete and recreate directory template with same name"
$deleteTemplateResponse = Invoke-ApiJson -Method DELETE -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/directory-templates/$templateId" -Headers $authHeaders
Assert-ApiOk $deleteTemplateResponse

$recreateTemplateResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/directory-templates" -Headers $authHeaders -Body @{
  templateType = "DOCUMENT"
  name         = $templateName
  rootNodeJson = '{"children":["final"]}'
  sourceType   = "MANUAL"
  sortOrder    = 3
  status       = "ACTIVE"
}
Assert-ApiOk $recreateTemplateResponse
$templateId = Get-ApiDataId $recreateTemplateResponse

Write-Step "final standard status"
$finalStatusResponse = Invoke-ApiJson -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/standard-status" -Headers $authHeaders
Assert-ApiOk $finalStatusResponse

if (-not $finalStatusResponse.data.deliverableStandardReady) {
  throw "Expected deliverableStandardReady to be true."
}

Write-Step "regression: minimal chain"
& "$PSScriptRoot/check-minimal-chain.ps1" $BaseUrl $Username $Password $TargetProjectId
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

Write-Step "regression: master-data chain"
& "$PSScriptRoot/check-master-data-chain.ps1" $BaseUrl $Username $Password $TargetProjectId "REG-$Suffix"
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

Write-Host "deliverable-standard chain ok: definition=$definitionId, type=$typeId, attribute=$attrId, template=$templateId"
