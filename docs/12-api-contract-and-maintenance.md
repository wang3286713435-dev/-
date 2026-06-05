# API 契约与文档维护说明

更新时间：2026-06-04

## 1. 文档定位

本文档说明卓羽智能数据中台 API 文档如何保留、查看和维护。

重点：

- API 运行期文档继续由 Springdoc/OpenAPI 生成。
- 本文档不逐字段复制所有接口，避免和代码生成文档不一致。
- 本文档维护接口分组、稳定契约、禁出字段和后续同步规则。

## 2. 运行期 API 文档入口

后端启动后可访问：

- Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI JSON：`http://127.0.0.1:8080/v3/api-docs`

说明：

- 这两个入口是接口字段、路径参数、请求体、响应体的最终运行期依据。
- 如果新增接口没有出现在 OpenAPI 中，测试 agent 应按 P1 或 P0 判定，具体级别取决于该接口是否为当前批次核心接口。

## 3. 当前稳定 API 分组

### 3.1 core

用途：

- 登录、注册、当前用户、项目列表、员工管理、项目授权、角色权限、审计。

当前边界：

- 员工注册和项目授权已可用于局域网试运行。
- 普通员工只能访问授权项目。
- 停用账号不可继续使用。

### 3.2 data-steward

用途：

- 项目资产、文件管理、真实 NAS 接管、对象存储、file-access、文件归属、工程树、质量治理。

关键能力：

- 文件目录和项目全局搜索。
- 受控 NAS 写操作。
- 回收站。
- 对象存储状态查询。
- 对象化任务、覆盖率报告和 file-access。
- 文件归属和工程树映射。

关键接口族：

- `/api/data-steward/assets/**`
- `/api/data-steward/catalog/**`
- `/api/data-steward/projects/{projectId}/file-ownership/**`
- `/api/data-steward/storage/**`
- `/api/data-steward/storage-objectification-coverage`
- `/api/data-steward/assets/file-access/**`

### 3.3 work-center

用途：

- 文档交付、图纸交付、缺失项、批量挂接、审核整改、交付包 dry-run、档案目录。

关键接口族：

- `/api/work-center/projects/{projectId}/delivery/**`
- `/api/work-center/projects/{projectId}/delivery-package/**`
- `/api/work-center/projects/{projectId}/delivery-candidates/**`

### 3.4 visualization-adapter

用途：

- BIM 协同、葛兰岱尔轻量化任务、模型列表、Viewer ticket、综合驾驶舱。

关键接口族：

- `/api/visualization-adapter/projects/{projectId}/digital-twin-dashboard`
- `/api/visualization-adapter/projects/{projectId}/glandar/model-files`
- `/api/visualization-adapter/projects/{projectId}/lightweight-jobs/**`

当前边界：

- READY 模型可签发受控 Viewer 入口。
- 当前不是完整构件级 BIM 平台。
- 构件属性、构件搜索、图模联动和碰撞检查仍属后续批次。

### 3.5 Hermes / Agent Gateway

用途：

- 只读 catalog 辅助、权限证明、后续 Evidence API 预留。

当前边界：

- Hermes 不能直连 MySQL、NAS、MinIO、Qdrant、OpenSearch。
- 当前不能把 catalog-only 元数据当成正文 evidence。
- M5 前不应宣称 Hermes 已具备正文问答。

## 4. 禁止返回字段

以下字段或内容不得返回给普通前端、Hermes 或未授权调用方：

- 真实 NAS 路径：`/Volumes`、`smb://`、`nas://`
- `storage_uri`
- bucket 名和 object key
- raw row
- SQL
- token、secret、password
- 未授权文件正文、chunk、图纸解析内容、模型解析内容

例外：

- 高权限运维接口若必须展示诊断信息，应使用脱敏值、摘要值、hash 或诊断编号，不展示原始路径和密钥。

## 5. API 变更维护规则

新增或修改接口时，开发 agent 必须：

1. 确认接口出现在 `/v3/api-docs`。
2. 保持统一响应结构和 `traceId`。
3. 明确权限校验位置。
4. 明确是否写审计。
5. 明确是否可能返回敏感字段。
6. 更新专项脚本或回归脚本。
7. 更新 `handoff/dev-agent/latest-report.md`。

主 agent 必须判断是否同步：

- `docs/12-api-contract-and-maintenance.md`
- `docs/11-current-baseline-and-next-roadmap.md`
- `docs/03-architecture-and-system-design.md`
- `docs/07-complete-delivery-prd.md`
- `handoff/main-agent/status.md`

## 6. 批次验收 API 要求

每个涉及接口的批次至少验证：

- 后端构建通过。
- 后端健康检查通过。
- OpenAPI 可访问。
- 核心接口响应包含 `traceId`。
- 权限越权请求被拒绝或不可见。
- 禁出字段扫描通过。
- 写操作有人工确认或受控条件。
- 高风险写操作有审计。

## 7. 后续 API 规划

### M3X 全项目对象化

需要继续维护：

- 覆盖率报告。
- 对象化 dry-run。
- 分批执行。
- 失败治理。
- 对象优先读取状态。

### M4 语义证据层

预计新增：

- semantic documents 查询。
- semantic chunks 查询。
- evidence hash 查询。
- evidence extraction task。

这些接口必须严格区分 catalog metadata 和正文 evidence。

### M5 Hermes Evidence API

预计新增：

- evidence search。
- evidence answer context。
- Missing Evidence reason。
- permission proof。

Hermes 只能通过平台 Gateway 调用这些接口，不得直连底层系统。
