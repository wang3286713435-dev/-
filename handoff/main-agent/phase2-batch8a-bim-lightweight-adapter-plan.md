# 二期 8A 规划：BIM 轻量化适配层与 Mock 预览入口

## 1. 批次定位

8A 是 7A/7B 之后的窄批次。

7A/7B 已完成：

- 文件预览策略统一。
- 文件预览转换状态表达。
- Office / CAD / BIM 等不可直接预览文件的业务提示。
- 交付包预检查的只读 dry-run 能力。

8A 不做真实 BIM 轻量化转换，不绑定具体厂商，不做构件级解析。

本批只解决一个问题：

> 平台需要具备 BIM 轻量化接入的稳定适配层合同、Mock 状态和前端入口，让后续接入第三方 BIM 引擎时不推倒现有数据管家和工作中心。

## 2. 本批目标

- 明确 BIM 轻量化适配层的接口、状态字段和返回边界。
- 在模型集成和可视化适配层之间建立稳定的只读预览上下文。
- 在前端模型集成页展示轻量化准备状态、适配模式和预览入口占位。
- 所有新增能力使用 `MOCK / NOT_CONNECTED / NOT_STARTED` 等安全状态，不执行真实转换。
- 继续不读取 RVT / IFC / NWD / NWC 文件正文。
- 继续不写 NAS、不生成真实轻量化产物、不写对象存储、不绑定厂商 SDK。

## 3. 推荐技术设计

### 3.1 后端适配层合同

优先扩展 `delivery-visualization-adapter`，保持 3D/BIM 引擎可插拔。

建议新增只读接口：

```http
GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-status
```

响应建议包含：

- `projectId`
- `integrationId`
- `modelFileId`
- `modelName`
- `modelFormat`
- `integrationStatus`
- `engineMode`：`MOCK`
- `engineConnected`：`false`
- `lightweightStatus`：`NOT_STARTED / NOT_CONNECTED / UNSUPPORTED / READY`
- `viewerAvailable`：`false`
- `taskStatus`：`NOT_CREATED`
- `conversionRequired`：`true`
- `componentIndexStatus`：`NOT_STARTED`
- `previewMode`：`BIM_LIGHTWEIGHT`
- `statusLabel`
- `actionHint`
- `blockedReason`
- `supportedOperations`
- `forbiddenOperations`

建议新增只读计划接口：

```http
GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-plan
```

用于告诉用户如果后续接入真实引擎，将需要做什么。本接口只能返回 dry-run / plan，不创建任务。

响应建议包含：

- `dryRun=true`
- `taskCreated=false`
- `packageGenerated=false`
- `engineBindingRequired=true`
- `requiredInputs`
- `futureSteps`
- `riskWarnings`

### 3.2 与现有模型集成的关系

现有 `data_model_integrations` 和 `ModelIntegrationResponse` 已有：

- `modelFileId`
- `versionNo`
- `status`
- `componentCount`
- `adapterPayloadJson`

8A 默认不新增数据库迁移。

如果需要展示轻量化状态，优先由适配层根据：

- 模型集成状态
- 模型文件扩展名
- 7B 预览策略
- `adapterPayloadJson`

动态生成只读状态。

不得把 `adapterPayloadJson` 当作真实引擎状态源，也不得写入真实转换结果。

### 3.3 前端模型集成页

在 `ModelIntegrationsPage.vue` 中增强：

- 新增“轻量化状态”列。
- 新增“适配模式 / 引擎状态”展示。
- 行操作增加：
  - `查看轻量化准备`
  - `打开 3D 预览入口`（未接入引擎时禁用或展示说明）
- 增加抽屉或弹窗展示：
  - 当前模型文件。
  - 当前集成状态。
  - 轻量化状态。
  - 引擎连接状态。
  - 后续接入真实引擎前需要的条件。
  - 明确“不读取模型正文、不执行真实转换、不生成轻量化产物”。

### 3.4 与文件预览状态的关系

7B 已将 BIM 文件标记为：

- `NEEDS_CONVERSION`
- `BIM_LIGHTWEIGHT`

8A 不能改变这个判断为“已可预览”。

只有未来真实引擎接入并产出可用预览物后，才允许返回：

- `viewerAvailable=true`
- `lightweightStatus=READY`

本批默认应返回：

- `engineMode=MOCK`
- `engineConnected=false`
- `viewerAvailable=false`
- `lightweightStatus=NOT_CONNECTED` 或 `NOT_STARTED`

## 4. 明确不做

- 不接入真实 BIM 引擎。
- 不绑定 Forge / Autodesk / Cesium / Xeokit / 任何厂商 SDK。
- 不执行真实 RVT / IFC / NWD / NWC 转换。
- 不生成 GLB / 3D Tiles / SVF / 轻量化缓存。
- 不读取模型正文、族、构件、属性、楼层、视图或图纸。
- 不做构件级解析、构件搜索、构件高亮真实能力。
- 不写 NAS。
- 不写 MinIO、OpenSearch、Qdrant、documents/chunks。
- 不做 Hermes 写操作或模型语义索引。
- 不把 Mock 状态包装成真实预览可用。

## 5. 验收标准

- 新增适配层接口纳入 OpenAPI。
- 接口按项目上下文校验权限。
- 模型集成不存在或跨项目访问必须失败。
- 响应明确 `engineMode=MOCK`、`engineConnected=false`、`viewerAvailable=false`。
- 响应明确不执行真实转换、不读取模型正文、不生成轻量化产物。
- 响应不得包含真实 NAS 路径或底层存储字段。
- 模型集成页面展示轻量化状态和准备说明。
- `打开 3D 预览入口` 在引擎未接入时不能误导用户为真实可用。
- 7B、7A、4R 回归脚本继续通过。

## 6. 建议回归脚本

新增：

`scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`

建议覆盖：

- 管理员登录。
- 创建或复用安全 mock/minio 模型文件元数据。
- 创建模型集成。
- 查询 lightweight status。
- 查询 lightweight plan。
- 验证 OpenAPI 包含新增接口。
- 验证 forbidden fields：
  - `/Volumes`
  - `smb://`
  - `nas://`
  - `storage_path`
  - `storageUri`
  - `raw row`
  - `SQL`
- 验证没有创建真实轻量化产物或访问票据。
- 回归：
  - `check-phase2-batch7b-preview-conversion-experience.sh`
  - `check-phase2-batch7a-preview-export-precheck.sh`
  - `check-phase2-batch4-file-access.sh`

## 7. 当前裁决

可以进入 8A 开发。

开发前必须再次强调：

- 8A 是“BIM 轻量化适配层合同与 Mock 入口”，不是“真实 BIM 轻量化上线”。
- 真实引擎选型、真实转换、构件级解析和图模联动都留到后续批次。
