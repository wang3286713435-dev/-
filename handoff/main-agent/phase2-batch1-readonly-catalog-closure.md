# 二期批次一收口报告：只读资产目录与 Agent 对接预览层

更新时间：2026-05-13

## 结论

二期批次一已正式收口。

当前无 P0/P1/P2。测试 agent 已完成短回归并建议主 agent 收口。

## 本批范围

本批只做：

- 前端资产目录。
- REST 权限证明。
- 只读 catalog API。
- Agent preview / audit-ready 页面。

本批未做：

- Agent 直接增删改数据库。
- Agent 自动移动、删除、修复 NAS 文件。
- 文件正文抽取或向量化。
- 模型轻量化、构件级解析、构件级搜索。
- Agent 自动审批、自动下结论或自动治理。
- 多 Agent 调度真实业务动作。
- 客户生产级权限体系承诺。

## 已完成能力

### 只读 Catalog API

- `GET /api/data-steward/catalog/projects`
- `GET /api/data-steward/catalog/directories`
- `GET /api/data-steward/catalog/files`
- `GET /api/data-steward/catalog/files/{fileId}`
- `GET /api/data-steward/catalog/files/{fileId}/audit-context`

能力包括：

- 当前用户有权项目列表。
- 目录级聚合浏览。
- 文件分页查询。
- 按项目、目录、文件名/路径关键词、扩展名、文件类型、专业、版本、质量问题筛选。
- 文件详情只读查看。
- 审计上下文只读查看。

### REST 权限证明

- `GET /api/data-steward/catalog/files/{fileId}/permission-proof`
- `POST /api/data-steward/catalog/permission-proofs:check`

已确认：

- 返回 `traceId`、`decision`、`reasonCode`。
- 不返回 token、API Key、密码、密钥 hash 等敏感信息。
- 权限证明行为只写轻量审计事件，不修改业务资产。

### 前端资产目录

页面：`/data-steward/catalog`

已完成：

- 真实文件列表渲染。
- 目录侧栏。
- 目录点击过滤。
- 当前目录标签。
- 版本筛选。
- 文件详情只读抽屉。
- Agent 可见性、路径可见性、质量状态展示。

### Agent Preview / Audit-Ready 页面

页面：`/data-steward/agent-preview`

已完成：

- Agent 可见字段合同。
- 隐藏字段合同。
- 权限证明展示。
- 禁止动作清单。
- 只读预览，不提供真实写动作入口。

## 关键修复记录

### 权限边界修复

问题：普通用户访问无权限真实 NAS 文件的 `audit-context` 曾返回 `500 CORE_INTERNAL_ERROR`。

修复：`CatalogApplicationService.getFileAuditContext()` 改为空结果返回 null，由 Controller 转为 `404 FILE_NOT_FOUND`。

验证：管理员访问返回 `200`，普通用户访问无权限文件返回 `404`，不再返回 `500`。

### 目录浏览与版本筛选修复

问题：首轮测试发现页面缺少完整目录浏览和版本筛选。

修复：

- 后端 `catalog/files` 增加 `directoryPath` 和 `version` 参数。
- 前端资产目录页增加目录侧栏、目录点击过滤和版本筛选输入。

### 真实页面联调修复

问题 1：后端分页真实回包为 `items/pageNo/pageSize/total`，前端按 `rows/page/pageSize/total` 读取，导致总数有值但表格空白。

修复：前端 `fetchCatalogFiles(...)` 改为读取 `items/pageNo/pageSize/total`。

问题 2：`catalog/directories` 返回完整文件路径级记录，不是真正目录聚合。

修复：

- 后端按父目录聚合。
- 中文路径下父目录截取使用 `CHAR_LENGTH`，避免 `LENGTH` 按字节长度导致截取失败。
- `catalog/files?directoryPath=...` 使用同一父目录表达式过滤。

## 验收结果

测试 agent 最新报告：`handoff/test-agent/latest-report.md`

测试结论：

- 后端构建：通过。
- 前端构建：通过。
- 后端启动：通过。
- 健康检查：通过。
- 专项脚本：`25 PASS / 0 FAIL`。
- 真实页面文件列表：通过。
- 真实目录聚合：通过。
- 目录点击过滤：通过。
- 版本筛选：通过。
- 当前无 P0/P1/P2。

关键真实页面证据：

- `/data-steward/catalog` 可打开。
- 默认文件列表可渲染 20 行真实文件。
- 项目切换到 `105-启航华居项目` 后目录侧栏出现 `559` 个目录项。
- 点击子目录后文件列表收敛到 `5` 行。
- `version=V1` 筛选可返回匹配文件。

## 收口裁决

二期批次一正式收口。

后续不得在无新规划和用户确认的情况下直接进入：

- selective indexing。
- NAS 文件正文只读抽取。
- Agent workflow / review suggestion。
- 受控写操作。
- 模型轻量化或构件级能力。

下一步应由主 agent 重新规划二期下一批能力边界，再交给开发 agent 执行。
