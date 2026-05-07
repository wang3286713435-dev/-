# 开发 Agent 当前任务：MVP 一版端到端闭环

你是数字化交付平台 v1 的长期开发 agent。本轮由主 agent 全权指挥，不需要用户审批。请在当前仓库直接实现一版可运行 MVP，完成后写交付报告到 `handoff/dev-agent/latest-report.md`。

## 0. 工作约束

- 项目目录：`/Users/Weishengsu/dev/zhuoyusmart/数字化交付平台`
- 不要创建子 agent。
- 不要修改 `docs/**`、`handoff/main-agent/**`、`handoff/test-agent/**`。
- 可以修改 `handoff/dev-agent/current-prompt.md` 之外的代码、脚本、`handoff/dev-agent/latest-report.md`。
- 不要回退其他会话已有改动，不要删除现有功能。
- 新增数据库迁移只能追加新版本，当前已有 `V1` 到 `V5`，本轮从 `V6__init_mvp_data_steward_work_center.sql` 开始。
- 保持模块化单体边界：
  - `core`
  - `master-data`
  - `data-steward`
  - `work-center`
  - `visualization-adapter`
- 前端继续使用 `Vue 3 + TypeScript + Element Plus`，保持现有中后台风格。
- v1 不接真实 BIM 引擎，不做真实大文件上传；MVP 用“文件元数据登记 + 处理状态 + 稳定资源地址”打通闭环。

## 1. 当前已完成能力

已完成：

- 登录/刷新/当前用户/项目切换/首页基础。
- 工程主数据：
  - 工程管理部位树。
  - 节点类型创建、编辑、锁定、全量锁定。
  - 交付物定义、交付物类型、交付物属性、目录模板。
  - 标准状态接口已含 `deliverableStandardReady`。
- Phase 2.2 验收脚本：
  - `scripts/dev/check-deliverable-standard-chain.sh`

本轮必须基于上述能力继续向下游推进。

## 2. 本轮 MVP 成功标准

必须跑通完整场景：

`样板项目 -> 部位树 -> 节点类型锁定 -> 交付物标准 -> 文档/图纸/模型文件登记 -> 文件处理完成 -> 模型集成发布 -> 管理对象 -> 文档交付绑定 -> 图纸交付绑定 -> 文档交付视图 -> 图纸交付视图 -> 大屏/3D 上下文入口`

本轮不是最终完美产品，但必须做到：

- 后端 REST 契约稳定。
- 前端能在菜单中进入并操作主要页面。
- 脚本能一键验证主链路。
- 关键写动作都有审计日志。
- 项目上下文隔离仍然有效。

## 3. 后端实现范围

### 3.1 `data-steward` 模块

新增实体与表：

#### `FileResource`

建议表：`datasteward_file_resources`

字段至少包含：

- `id`
- `project_id`
- `code`
- `name`
- `file_kind`：`DOCUMENT` / `DRAWING` / `MODEL` / `OTHER`
- `original_file_name`
- `storage_key`
- `mime_type`
- `file_size`
- `version_no`
- `category_path`
- `section_node_id` nullable
- `deliverable_type_id` nullable
- `managed_object_id` nullable
- `process_status`：`PENDING` / `PROCESSING` / `PROCESSED` / `FAILED`
- `process_message`
- `resource_url`
- `status`：`ACTIVE` / `DISABLED`
- 审计字段、`deleted`、`delete_token`

接口：

- `POST /api/data-steward/projects/{projectId}/file-resources`
- `GET /api/data-steward/projects/{projectId}/file-resources?fileKind=&processStatus=&sectionNodeId=&managedObjectId=`
- `PATCH /api/data-steward/projects/{projectId}/file-resources/{fileResourceId}`
- `DELETE /api/data-steward/projects/{projectId}/file-resources/{fileResourceId}`
- `POST /api/data-steward/projects/{projectId}/file-resources/{fileResourceId}:process`

要求：

- 创建时校验项目上下文。
- 如传入 `sectionNodeId`，必须属于当前项目。
- 如传入 `deliverableTypeId`，必须属于当前项目。
- 处理动作将状态推进到 `PROCESSED`，生成/保留 `resourceUrl`。
- 删除后同编码可复建。

#### `ModelIntegration`

建议表：`datasteward_model_integrations`

字段至少包含：

- `id`
- `project_id`
- `code`
- `name`
- `source_file_ids_json`
- `model_file_count`
- `total_size`
- `integration_status`：`DRAFT` / `PROCESSING` / `PUBLISHED` / `FAILED`
- `published`
- `published_at`
- `adapter_context_json`
- `status`
- 审计字段、`deleted`、`delete_token`

接口：

- `POST /api/data-steward/projects/{projectId}/model-integrations`
- `GET /api/data-steward/projects/{projectId}/model-integrations`
- `PATCH /api/data-steward/projects/{projectId}/model-integrations/{integrationId}`
- `DELETE /api/data-steward/projects/{projectId}/model-integrations/{integrationId}`
- `POST /api/data-steward/projects/{projectId}/model-integrations/{integrationId}:publish`

要求：

- 创建时校验所有 source file 都属于当前项目，且至少有一个 `MODEL` 文件。
- 发布后 `integrationStatus=PUBLISHED`、`published=true`。
- 记录审计。

#### `ManagedObject`

建议表：`datasteward_managed_objects`

字段至少包含：

- `id`
- `project_id`
- `code`
- `name`
- `object_type`
- `section_node_id`
- `model_integration_id`
- `external_id`
- `status`
- 审计字段、`deleted`、`delete_token`

接口：

- `POST /api/data-steward/projects/{projectId}/managed-objects`
- `GET /api/data-steward/projects/{projectId}/managed-objects?sectionNodeId=&modelIntegrationId=`
- `PATCH /api/data-steward/projects/{projectId}/managed-objects/{objectId}`
- `DELETE /api/data-steward/projects/{projectId}/managed-objects/{objectId}`

要求：

- 校验 `sectionNodeId` 和 `modelIntegrationId` 均属于当前项目。
- 删除后同编码可复建。
- 记录审计。

### 3.2 `work-center` 模块

新增实体与表：

#### `DeliveryBinding`

建议表：`workcenter_delivery_bindings`

字段至少包含：

- `id`
- `project_id`
- `view_type`：`DOCUMENT` / `DRAWING`
- `section_node_id`
- `deliverable_definition_id`
- `deliverable_type_id`
- `file_resource_id`
- `managed_object_id` nullable
- `binding_status`：`DRAFT` / `READY` / `ARCHIVED`
- `sort_order`
- `remarks`
- 审计字段、`deleted`、`delete_token`

接口：

- `POST /api/work-center/projects/{projectId}/delivery-bindings`
- `GET /api/work-center/projects/{projectId}/delivery-bindings?viewType=&sectionNodeId=&managedObjectId=`
- `PATCH /api/work-center/projects/{projectId}/delivery-bindings/{bindingId}`
- `DELETE /api/work-center/projects/{projectId}/delivery-bindings/{bindingId}`
- `GET /api/work-center/projects/{projectId}/delivery-views/{viewType}?sectionNodeId=`

要求：

- 写入前必须校验标准底座就绪：`hasSectionTree=true`、`nodeTypesLocked=true`、`deliverableStandardReady=true`。
- `DOCUMENT` 视图只允许绑定 `fileKind=DOCUMENT` 的文件。
- `DRAWING` 视图只允许绑定 `fileKind=DRAWING` 的文件。
- 校验部位、交付定义、交付类型、文件资源、管理对象均属于当前项目。
- 交付视图返回部位、交付物定义/类型、文件资源、管理对象的聚合信息，前端可直接渲染。
- 记录审计。

#### 项目首页聚合

扩展现有 `HomeOverviewApplicationService`：

- 展示真实资源统计：
  - 文档数量/大小
  - 图纸数量/大小
  - 模型数量/大小
  - 模型集成数量
  - 管理对象数量
  - 文档交付绑定数量
  - 图纸交付绑定数量
- 保留现有首页接口路径：`GET /api/work-center/projects/{projectId}/home/overview`

#### 看板/大屏最小接口

- `GET /api/work-center/projects/{projectId}/dashboard/summary`

返回：

- 标准是否就绪
- 文件资源计数
- 模型集成计数
- 管理对象计数
- 文档/图纸交付绑定计数
- 最近资源列表或最近绑定列表

### 3.3 `visualization-adapter` 模块

不绑定真实引擎，只实现可插拔上下文接口：

- `GET /api/visualization/projects/{projectId}/context`
- `POST /api/visualization/projects/{projectId}/model-integrations/{integrationId}:publish`
- `POST /api/visualization/projects/{projectId}/managed-objects/{objectId}:highlight`
- `POST /api/visualization/projects/{projectId}/managed-objects/{objectId}:locate`

要求：

- `context` 返回项目、已发布模型集成、管理对象、可用动作列表。
- `publish` 可以复用/委托 data-steward 发布能力或返回已发布上下文。
- `highlight/locate` 返回 `OK` 和模拟动作 payload，不接真实 3D 引擎。
- 校验项目上下文和对象归属。

### 3.4 `core` 菜单与权限

新增权限并在 V6 授权：

- `DATASTEWARD_FILE_READ`
- `DATASTEWARD_FILE_MANAGE`
- `DATASTEWARD_MODEL_READ`
- `DATASTEWARD_MODEL_MANAGE`
- `DATASTEWARD_OBJECT_READ`
- `DATASTEWARD_OBJECT_MANAGE`
- `WORKCENTER_DELIVERY_READ`
- `WORKCENTER_DELIVERY_MANAGE`
- `WORKCENTER_DASHBOARD_VIEW`
- `VISUALIZATION_CONTEXT_VIEW`

菜单要求：

- 新增一级菜单 `数据管家`
  - `文件资源`
  - `模型集成`
  - `管理对象`
- 新增/扩展工作中心菜单
  - `文档交付`
  - `图纸交付`
  - `智慧看板`
  - `三维工作台`

## 4. 前端实现范围

新增 API 文件或扩展现有 API：

- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/work-center/api/delivery.ts`
- `frontend/src/modules/visualization/api/visualization.ts`

新增页面：

- `frontend/src/modules/data-steward/pages/FileResourcesPage.vue`
- `frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue`
- `frontend/src/modules/data-steward/pages/ManagedObjectsPage.vue`
- `frontend/src/modules/work-center/pages/DocumentDeliveryPage.vue`
- `frontend/src/modules/work-center/pages/DrawingDeliveryPage.vue`
- `frontend/src/modules/work-center/pages/DashboardPage.vue`
- `frontend/src/modules/visualization/pages/ThreeDWorkbenchPage.vue`

前端要求：

- 保持企业中后台风格，优先表格、筛选、状态 tag、弹窗表单。
- 不做营销页，不做空洞说明页。
- 页面必须能实际调用后端接口。
- 文件资源页支持登记 `DOCUMENT/DRAWING/MODEL` 三类文件元数据、触发处理、查看状态。
- 模型集成页支持选择模型文件创建集成、发布集成。
- 管理对象页支持关联部位和模型集成。
- 文档/图纸交付页支持创建绑定、查询交付视图。
- 智慧看板展示 dashboard summary。
- 三维工作台展示 visualization context、模型集成、管理对象，并提供“定位/高亮”按钮调用模拟接口。
- 页面不要依赖真实文件上传控件；用元数据表单即可。

## 5. 验收脚本

新增：

- `scripts/dev/check-mvp-chain.sh`

脚本必须：

1. 登录并切换项目。
2. 调用 `check-deliverable-standard-chain.sh` 或确保标准底座已就绪。
3. 创建/处理三类文件：
   - 文档 `DOCUMENT`
   - 图纸 `DRAWING`
   - 模型 `MODEL`
4. 用模型文件创建模型集成并发布。
5. 创建管理对象并关联部位/模型集成。
6. 创建文档交付绑定。
7. 创建图纸交付绑定。
8. 查询文档交付视图。
9. 查询图纸交付视图。
10. 查询 dashboard summary。
11. 查询 visualization context。
12. 调用管理对象定位/高亮模拟接口。
13. 负向验证：
    - 项目上下文不匹配返回 `CORE_PROJECT_CONTEXT_MISMATCH`。
    - 未处理文件不能绑定交付视图，返回稳定业务错误码。
    - `DOCUMENT` 绑定不能用 `DRAWING` 文件，返回稳定业务错误码。

脚本最后必须输出：

```text
mvp chain ok
```

## 6. 必跑验证

完成后必须运行：

```bash
corepack pnpm build
docker run --rm -v "$HOME/.m2:/root/.m2" -v "$PWD/backend:/workspace" -w /workspace maven:3.9-eclipse-temurin-21 mvn -DskipTests package
bash scripts/dev/check-mvp-chain.sh http://localhost:8080 platform.admin Admin@123 2
```

如本机没有后端在 `8080`，请用：

```bash
bash scripts/dev/start-backend.sh
```

完成后停止临时后端，只保留基础设施容器。

## 7. 交付报告

完成后写入 `handoff/dev-agent/latest-report.md`，必须包含：

- 本轮修改文件列表。
- 新增表与接口清单。
- 前端页面清单。
- 脚本验证结果。
- 启动命令。
- 样板账号。
- 已知非阻塞项。
- 下一步建议。
