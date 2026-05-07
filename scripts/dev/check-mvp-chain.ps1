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

Write-Step "menu includes mvp modules"
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

$requiredPaths = @(
  "/data-steward/files",
  "/data-steward/models",
  "/data-steward/objects",
  "/work/document-delivery",
  "/work/drawing-delivery",
  "/work/dashboard",
  "/visualization/workbench"
)
$missingPaths = @($requiredPaths | Where-Object { $menuPaths -notcontains $_ })
if ($missingPaths.Count -gt 0) {
  throw "Current user menu is missing paths: $($missingPaths -join ', ')."
}

Write-Step "create section node"
$sectionResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/section-nodes" -Headers $authHeaders -Body @{
  parentId  = $null
  code      = "MVP-SEC-$Suffix"
  name      = "MVP 样板部位 $Suffix"
  sortOrder = 1
  status    = "ACTIVE"
}
Assert-ApiOk $sectionResponse
$sectionId = Get-ApiDataId $sectionResponse

Write-Step "create and lock node type"
$nodeTypeResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/node-types" -Headers $authHeaders -Body @{
  code       = "MVP-NT-$Suffix"
  name       = "MVP 节点类型 $Suffix"
  scopeLevel = 1
  sortOrder  = 1
  status     = "ACTIVE"
}
Assert-ApiOk $nodeTypeResponse
$nodeTypeId = Get-ApiDataId $nodeTypeResponse

$lockResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/node-types:lock" -Headers $authHeaders
Assert-ApiOk $lockResponse

Write-Step "create deliverable standard"
$definitionResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-definitions" -Headers $authHeaders -Body @{
  nodeTypeId = [int64]$nodeTypeId
  code       = "MVP-DEF-$Suffix"
  name       = "MVP 交付物定义 $Suffix"
  category   = "DOCUMENT"
  required   = $true
  sortOrder  = 1
  status     = "ACTIVE"
}
Assert-ApiOk $definitionResponse
$definitionId = Get-ApiDataId $definitionResponse

$docTypeResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-types" -Headers $authHeaders -Body @{
  deliverableDefinitionId = [int64]$definitionId
  code                    = "MVP-DOC-TYPE-$Suffix"
  name                    = "MVP 文档类型 $Suffix"
  fileKind                = "DOCUMENT"
  bindingStrategy         = "SECTION_NODE"
  sortOrder               = 1
  status                  = "ACTIVE"
}
Assert-ApiOk $docTypeResponse
$docTypeId = Get-ApiDataId $docTypeResponse

$drawingTypeResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-types" -Headers $authHeaders -Body @{
  deliverableDefinitionId = [int64]$definitionId
  code                    = "MVP-DWG-TYPE-$Suffix"
  name                    = "MVP 图纸类型 $Suffix"
  fileKind                = "DRAWING"
  bindingStrategy         = "MANAGED_OBJECT"
  sortOrder               = 2
  status                  = "ACTIVE"
}
Assert-ApiOk $drawingTypeResponse
$drawingTypeId = Get-ApiDataId $drawingTypeResponse

$attrResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/deliverable-attributes" -Headers $authHeaders -Body @{
  deliverableTypeId = [int64]$docTypeId
  code              = "MVP-ATTR-$Suffix"
  name              = "MVP 属性 $Suffix"
  valueType         = "TEXT"
  unit              = ""
  required          = $true
  exampleValue      = "A1"
  enumOptions       = ""
  sortOrder         = 1
  status            = "ACTIVE"
}
Assert-ApiOk $attrResponse

$templateResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/master-data/projects/$TargetProjectId/directory-templates" -Headers $authHeaders -Body @{
  templateType = "DOCUMENT"
  name         = "MVP 目录模板 $Suffix"
  rootNodeJson = '{"children":["MVP"]}'
  sourceType   = "MANUAL"
  sortOrder    = 1
  status       = "ACTIVE"
}
Assert-ApiOk $templateResponse

Write-Step "create processed file resources"
$docFileResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/data-steward/projects/$TargetProjectId/file-resources" -Headers $authHeaders -Body @{
  originalName  = "MVP 文档 $Suffix.pdf"
  fileKind      = "DOCUMENT"
  mimeType      = "application/pdf"
  sizeBytes     = 1024
  storageUri    = "minio://delivery/mvp-doc-$Suffix.pdf"
  businessTag   = "mvp"
  versionNo     = "V1"
  processStatus = "PROCESSED"
}
Assert-ApiOk $docFileResponse
$docFileId = Get-ApiDataId $docFileResponse

$drawingFileResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/data-steward/projects/$TargetProjectId/file-resources" -Headers $authHeaders -Body @{
  originalName  = "MVP 图纸 $Suffix.dwg"
  fileKind      = "DRAWING"
  mimeType      = "application/acad"
  sizeBytes     = 2048
  storageUri    = "minio://delivery/mvp-drawing-$Suffix.dwg"
  businessTag   = "mvp"
  versionNo     = "V1"
  processStatus = "PROCESSED"
}
Assert-ApiOk $drawingFileResponse
$drawingFileId = Get-ApiDataId $drawingFileResponse

$modelFileResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/data-steward/projects/$TargetProjectId/file-resources" -Headers $authHeaders -Body @{
  originalName  = "MVP 模型 $Suffix.rvt"
  fileKind      = "MODEL"
  mimeType      = "application/octet-stream"
  sizeBytes     = 4096
  storageUri    = "minio://delivery/mvp-model-$Suffix.rvt"
  businessTag   = "mvp"
  versionNo     = "V1"
  processStatus = "PROCESSED"
}
Assert-ApiOk $modelFileResponse
$modelFileId = Get-ApiDataId $modelFileResponse

Write-Step "create and publish model integration"
$modelResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/data-steward/projects/$TargetProjectId/model-integrations" -Headers $authHeaders -Body @{
  name               = "MVP 模型集成 $Suffix"
  modelFileId        = [int64]$modelFileId
  versionNo          = "V1"
  componentCount     = 8
  adapterPayloadJson = '{"adapter":"mock-bim"}'
}
Assert-ApiOk $modelResponse
$modelId = Get-ApiDataId $modelResponse

$publishResponse = Invoke-ApiJson -Method PATCH -Uri "$BaseUrl/api/data-steward/projects/$TargetProjectId/model-integrations/$($modelId):publish" -Headers $authHeaders
Assert-ApiOk $publishResponse
if ([string]$publishResponse.data.status -ne "PUBLISHED") {
  throw "Expected model integration $modelId to be published."
}

Write-Step "create managed object"
$objectResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/data-steward/projects/$TargetProjectId/managed-objects" -Headers $authHeaders -Body @{
  modelIntegrationId = [int64]$modelId
  sectionNodeId      = [int64]$sectionId
  code               = "MVP-OBJ-$Suffix"
  name               = "MVP 管理对象 $Suffix"
  objectType         = "EQUIPMENT"
  externalId         = "EXT-$Suffix"
  discipline         = "MEP"
  status             = "ACTIVE"
  propertiesJson     = '{"level":"B1"}'
}
Assert-ApiOk $objectResponse
$objectId = Get-ApiDataId $objectResponse

Write-Step "bind document and drawing delivery views"
$docBindingResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/work-center/projects/$TargetProjectId/delivery-bindings" -Headers $authHeaders -Body @{
  viewType          = "DOCUMENT"
  sectionNodeId     = [int64]$sectionId
  managedObjectId   = $null
  deliverableTypeId = [int64]$docTypeId
  fileResourceId    = [int64]$docFileId
  bindingStatus     = "BOUND"
  reviewStatus      = "PENDING"
  sortOrder         = 1
  remark            = "mvp document"
}
Assert-ApiOk $docBindingResponse

$drawingBindingResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/work-center/projects/$TargetProjectId/delivery-bindings" -Headers $authHeaders -Body @{
  viewType          = "DRAWING"
  sectionNodeId     = $null
  managedObjectId   = [int64]$objectId
  deliverableTypeId = [int64]$drawingTypeId
  fileResourceId    = [int64]$drawingFileId
  bindingStatus     = "BOUND"
  reviewStatus      = "PENDING"
  sortOrder         = 1
  remark            = "mvp drawing"
}
Assert-ApiOk $drawingBindingResponse

Write-Step "delivery views"
$docViewResponse = Invoke-ApiJson -Uri "$BaseUrl/api/work-center/projects/$TargetProjectId/delivery-views?viewType=DOCUMENT" -Headers $authHeaders
Assert-ApiOk $docViewResponse
if (-not (@($docViewResponse.data.rows) | Where-Object { [string]$_.fileResourceId -eq $docFileId })) {
  throw "Expected document delivery view to include file $docFileId."
}

$drawingViewResponse = Invoke-ApiJson -Uri "$BaseUrl/api/work-center/projects/$TargetProjectId/delivery-views?viewType=DRAWING" -Headers $authHeaders
Assert-ApiOk $drawingViewResponse
if (-not (@($drawingViewResponse.data.rows) | Where-Object { [string]$_.fileResourceId -eq $drawingFileId })) {
  throw "Expected drawing delivery view to include file $drawingFileId."
}

Write-Step "dashboard and visualization context"
$dashboardResponse = Invoke-ApiJson -Uri "$BaseUrl/api/work-center/projects/$TargetProjectId/dashboard/summary" -Headers $authHeaders
Assert-ApiOk $dashboardResponse
if (($dashboardResponse.data.publishedModelCount -lt 1) -or ($dashboardResponse.data.managedObjectCount -lt 1)) {
  throw "Expected dashboard summary to include published models and managed objects."
}

$contextResponse = Invoke-ApiJson -Uri "$BaseUrl/api/visualization-adapter/projects/$TargetProjectId/context" -Headers $authHeaders
Assert-ApiOk $contextResponse
if (-not (@($contextResponse.data.objects) | Where-Object { [string]$_.id -eq $objectId })) {
  throw "Expected visualization context to include object $objectId."
}

$locateResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/visualization-adapter/projects/$TargetProjectId/managed-objects/$($objectId):locate" -Headers $authHeaders
Assert-ApiOk $locateResponse

$highlightResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/visualization-adapter/projects/$TargetProjectId/managed-objects/$($objectId):highlight" -Headers $authHeaders -Body @{
  color           = "#2563eb"
  durationSeconds = 5
}
Assert-ApiOk $highlightResponse

$injectResponse = Invoke-ApiJson -Method POST -Uri "$BaseUrl/api/visualization-adapter/projects/$TargetProjectId/context:inject" -Headers $authHeaders -Body @{
  sectionNodeId   = [int64]$sectionId
  managedObjectId = [int64]$objectId
  source          = "MVP_SCRIPT"
}
Assert-ApiOk $injectResponse

Write-Step "audit logs"
$auditResponse = Invoke-ApiJson -Uri "$BaseUrl/api/core/projects/$TargetProjectId/audit-logs?moduleCode=work-center&limit=20" -Headers $authHeaders
Assert-ApiOk $auditResponse
if (@($auditResponse.data).Count -lt 1) {
  throw "Expected at least one audit log entry."
}

Write-Host "mvp chain ok"
