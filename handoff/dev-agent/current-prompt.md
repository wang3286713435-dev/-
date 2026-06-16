# Dev Agent 当前任务：PLM-1 项目生命周期管理 MVP

你是数字化交付平台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

先阅读并遵守：

- `handoff/main-agent/status.md`
- `handoff/main-agent/decisions.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/post-ux4-project-lifecycle-todo.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

不要修改 `docs/**`。

## 0. 批次定位

批次名称：

`PLM-1：项目生命周期管理 MVP`

本批只做最小可用的项目创建与归档能力，不要扩展成完整 PLM 系统。

目标是让平台具备真实管理项目生命周期的基础能力：

1. 超级管理员可以创建项目。
2. 创建项目时同步初始化对象存储工作区和工程树根节点。
3. 超级管理员可以二次确认后归档项目。
4. 归档项目默认不再出现在项目启动台。
5. 不删除、移动、重命名真实 NAS 文件，也不删除 MinIO 对象。

## 1. 当前已有基础，必须优先复用

优先复用现有能力，不要重造一套项目系统：

- 项目台账：`core_projects`
- 项目角色：`core_user_project_roles`
- 审计日志：`core_audit_logs`
- 现有项目创建基础：
  - `AssetController`
  - `AssetApplicationService#createProject`
  - `BimAssetRepository#upsertProject`
  - `BimAssetRepository#grantProjectAdmin`
- 对象存储基础：
  - `StorageService`
  - `data_storage_objects`
  - `data_file_object_versions`
- 工程主数据基础：
  - `masterdata_section_nodes`
  - section node repository/service
- 前端项目入口：
  - `AssetOverviewPage.vue`

如果发现已有接口或服务已经能满足，不要新增重复接口。

## 2. 功能范围

### A. 创建项目

在资产总览页把“新建项目”从占位提示改成真实弹窗。

字段保持最小：

- 项目名称
- 项目编码
- 项目类型/行业类型，沿用现有字段或默认值
- 负责人/责任组织，如现有字段支持则保留；不支持不要强行加表

后端创建项目时必须做到：

1. 仅超级管理员可创建。
2. 校验项目编码和项目名称。
3. 写入 `core_projects`，优先复用现有 `createProject/upsertProject`。
4. 给创建人授予该项目 `PROJECT_ADMIN`。
5. 初始化一个对象存储工作区占位。
   - 只通过平台 StorageService / MinIO API。
   - 不暴露 bucket、object key、storage_uri。
   - object key 必须是不含真实业务路径的 opaque 规则。
6. 初始化工程树根节点。
   - 只创建根节点，不自动生成大量模板节点。
   - 如果根节点已存在，保持幂等，不重复创建。
7. 写审计日志。
8. 返回业务化结果：
   - projectId
   - projectCode
   - projectName
   - projectAdminGranted
   - storageWorkspaceStatus
   - sectionRootStatus / sectionRootNodeId

注意：

- 不要读取或扫描真实 NAS。
- 不要自动创建交付物标准。
- 不要自动生成完整工程树。
- 不要宣称项目已经完成工程主数据初始化。

### B. 归档项目

资产总览项目行增加“归档项目”操作，仅超级管理员可见。

归档必须：

1. 二次确认。
2. 要求 `confirmed=true`。
3. 要求输入项目编码或项目名称进行确认，避免误操作。
4. 只做软归档/软删除，不做物理删除。
5. 默认项目列表不再显示归档项目。
6. 不删除 MinIO 对象。
7. 不删除真实 NAS 原文件。
8. 不删除工程主数据、交付记录、审计记录。
9. 写审计日志。

建议后端语义：

- 如果现有项目列表依赖 `core_projects.deleted=0`，可以将归档项目设为 `deleted=1` 或使用现有归档状态字段，但必须保持软删除语义。
- 如果已有 `asset_status` / `status` 等字段，按当前项目约定标记为 `ARCHIVED` / `INACTIVE`。
- 不要物理删除任何业务数据。

### C. 权限边界

本批的“超级管理员”优先复用现有系统里已经实现的超级管理员判断。

如果当前后端已有：

- `admin` 超级管理员
- `platform.admin`
- `isSuperAdmin`
- 员工权限管理里的超级管理员判断

请直接复用，不要新建一套权限模型。

普通项目管理员不能创建/归档全局项目。

## 3. API 建议

优先少新增接口。

创建项目：

- 优先增强现有 `POST /api/data-steward/assets/projects`
- 如果兼容风险更低，也可以新增：
  - `POST /api/data-steward/projects:lifecycle-create`

归档项目：

- 建议新增：
  - `POST /api/data-steward/assets/projects/{projectId}:archive`

归档请求体建议：

```json
{
  "confirmed": true,
  "confirmText": "项目编码或项目名称"
}
```

响应禁止包含：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- bucket
- object_key
- SQL
- raw row
- token / secret

## 4. 前端要求

只改必要交互，不做大 UI 重构。

### 资产总览

1. “新建项目”按钮：
   - 超级管理员可见并可用。
   - 非超级管理员隐藏或禁用，并提示联系管理员。
2. 新建项目弹窗：
   - 简洁字段。
   - 创建成功后刷新项目列表。
   - 可选择进入新项目工作台。
3. 项目行操作：
   - 增加“归档项目”。
   - 仅超级管理员可见。
   - 二次确认时要求输入项目编码或名称。
4. 归档成功后刷新列表。
5. 不新增复杂页面。

## 5. 专项脚本

新增：

`scripts/dev/check-plm1-project-lifecycle.sh`

至少验证：

1. 超级管理员登录。
2. 创建唯一测试项目成功。
3. 创建结果包含项目 ID、项目编码、工程树根节点状态、对象存储工作区状态。
4. 新项目出现在项目列表。
5. 创建人拥有该项目 `PROJECT_ADMIN`。
6. 工程树根节点存在且不重复。
7. 响应不泄露 bucket/object_key/storage_uri/raw NAS path。
8. `confirmed=false` 归档被拒绝。
9. 确认文本错误归档被拒绝。
10. 正确确认后归档成功。
11. 归档项目默认不再出现在项目列表。
12. 归档不删除 MinIO 对象、不触碰真实 NAS 文件。

脚本可以创建并归档一个临时项目，但不得影响 105/503 等真实项目。

## 6. 必跑验证

完成后至少运行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-plm1-project-lifecycle.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如本地后端未运行，可按项目既有方式启动；不要把密钥写入仓库或报告。

## 7. 禁止事项

严禁：

- 修改 `docs/**`
- 新建完整 PLM 子系统
- 重做权限模型
- 物理删除项目数据
- 删除、移动、重命名真实 NAS 文件
- 删除 MinIO 对象
- 自动生成完整工程树或交付标准
- 引入 Hermes 新能力
- 引入 BIM 新能力
- 写 documents/chunks/Qdrant/OpenSearch/Hermes memory
- 暴露真实路径、bucket、object key、token、secret

## 8. 交付报告

完成后更新：

`handoff/dev-agent/latest-report.md`

报告必须写清：

1. 修改了哪些文件。
2. 是否新增迁移。
3. 创建项目如何复用现有项目台账。
4. 对象存储工作区如何初始化。
5. 工程树根节点如何创建且保持幂等。
6. 归档项目如何软归档。
7. 超级管理员权限如何判断。
8. 验证结果。
9. 是否存在风险或未完成项。

注意当前工作区可能已有其他 agent 的未提交改动。你只能处理 PLM-1 必需文件，不要混入无关 staged / unstaged / untracked 文件；如发现无关改动，报告即可，不要回退。
