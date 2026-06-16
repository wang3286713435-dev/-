# PLM-1 项目生命周期管理 MVP 开发报告

## 1. 本轮目标

本轮按 `handoff/dev-agent/current-prompt.md` 完成 `PLM-1：项目生命周期管理 MVP`：

- 超级管理员可以在项目启动台创建项目。
- 创建项目时写入现有项目台账，给创建人授予 `PROJECT_ADMIN`。
- 创建项目时初始化对象存储工作区占位和工程树根节点。
- 超级管理员可以二次确认后软归档项目。
- 归档项目默认不再出现在项目启动台。
- 不删除、移动、重命名真实 NAS 文件，不删除 MinIO 对象。

## 2. 改动文件列表

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/AssetController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `scripts/dev/check-plm1-project-lifecycle.sh`
- `handoff/dev-agent/latest-report.md`

## 3. 是否新增迁移

否。

本批复用现有表：

- `core_projects`
- `core_user_project_roles`
- `core_audit_logs`
- `data_storage_objects`
- `masterdata_section_nodes`

没有修改旧 Flyway migration，也没有新增数据库迁移。

## 4. 创建项目实现说明

后端保留原有 `POST /api/data-steward/assets/projects` 兼容入口，并新增生命周期创建入口：

- `POST /api/data-steward/assets/projects:lifecycle-create`

创建逻辑复用现有项目台账能力：

- 使用 `BimAssetRepository#upsertProject` 写入 `core_projects`。
- 使用 `BimAssetRepository#grantProjectAdmin` 给创建人授予 `PROJECT_ADMIN`。
- 仅允许超级管理员 `admin` 执行创建。
- 校验项目编码、项目名称，并拒绝重复项目编码。

创建响应只返回业务字段：

- `projectId`
- `projectCode`
- `projectName`
- `projectAdminGranted`
- `storageWorkspaceStatus`
- `sectionRootStatus`
- `sectionRootNodeId`
- `project`

响应不返回 bucket、object key、storage URI 或真实 NAS 路径。

## 5. 对象存储工作区初始化

创建项目时通过 `StorageService#writeUploadToObject` 写入一个平台内部 `.workspace-keep` 占位对象。

占位对象规则：

- 使用项目 ID 和稳定 UUID 生成内部对象定位。
- 不包含真实 NAS 路径。
- 通过 `data_storage_objects` 记录对象存储元数据。
- 前端和 API 响应只看到 `storageWorkspaceStatus=CREATED`。

本批不做历史文件迁移，不读取文件正文，不扫描真实 NAS。

## 6. 工程树根节点初始化

创建项目时写入 `masterdata_section_nodes` 根节点：

- 只创建根节点 `ROOT`。
- 不自动生成大量模板节点。
- 如果该项目已经存在根节点，则返回 `sectionRootStatus=EXISTING`，不会重复创建。
- 新建根节点后补齐 `path=/{nodeId}`。

## 7. 归档项目实现说明

新增接口：

- `POST /api/data-steward/assets/projects/{projectId}:archive`

归档要求：

- 仅超级管理员 `admin` 可归档。
- 请求必须包含 `confirmed=true`。
- `confirmText` 必须等于项目编码或项目名称。
- 只做软归档：`core_projects.asset_status=ARCHIVED`、`status=INACTIVE`、`deleted=1`。
- 不删除项目角色、工程主数据、交付记录、审计记录。
- 不删除 MinIO 对象。
- 不触碰真实 NAS 文件。

归档响应包含：

- `archived=true`
- `archiveStatus=ARCHIVED`
- `objectStorageDeleted=false`
- `nasTouched=false`

## 8. 前端变化

项目启动台：

- `新建项目` 按钮从占位提示改为真实弹窗。
- 非超级管理员禁用按钮，并提示联系管理员。
- 弹窗字段保持最小：项目名称、项目编码、项目类型、项目阶段、负责人、责任组织。
- 创建成功后刷新项目列表，可选择进入新项目工作台。
- 项目行新增 `归档` 操作，仅超级管理员可见。
- 归档前要求输入项目编码或项目名称确认。

## 9. 权限规则

- 本批沿用现有超级管理员判断：用户名为 `admin` 且账号启用。
- 普通项目管理员不能创建全局项目。
- 普通项目管理员不能归档项目。
- 创建后的项目管理员权限只授予创建人。

## 10. 验证结果

已执行：

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

结果：

- 后端构建通过。
- 前端构建通过，仅保留既有 Vite chunk size warning。
- 后端健康检查通过：`{"status":"UP"}`。
- PLM-1 专项脚本通过：`PASS=11 FAIL=0`。
- M3G-8 对象优先读取回归通过：`PASS=7 FAIL=0`。
- Phase2 batch4 文件访问安全回归通过：`PASS=18 FAIL=0`。
- `git diff --check` 通过，仅有既有 PowerShell 脚本换行提示。
- 浏览器短验通过：项目启动台可打开，超级管理员可见 `新建项目`，归档按钮可见，新建弹窗字段和“不操作真实 NAS”提示可见。

验证过程中发现本地测试账号 `delivery.engineer` 被历史数据置为 `DISABLED/deleted=1`，且残留项目 2 权限会破坏 batch4 跨项目拒绝断言。已按种子数据预期恢复该测试账号，仅用于回归脚本，不涉及真实 NAS 文件。

## 11. 边界确认

- 未修改 `docs/**`。
- 未新增数据库迁移。
- 未新建完整 PLM 子系统。
- 未删除、移动、重命名真实 NAS 文件。
- 未删除 MinIO 对象。
- 未执行历史文件迁移。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未引入 Hermes / BIM / parser / indexing / 向量库新能力。
- API 响应未暴露 bucket、object key、storage URI、真实 NAS 路径、token 或 secret。

## 12. 运行状态

- 后端已用当前构建产物重启，当前 `http://127.0.0.1:8080/actuator/health` 为 UP。
- 前端 `http://127.0.0.1:5173` 保持运行，未关闭，方便用户继续实际检查。

## 13. 风险和未完成项

- 当前归档使用 `core_projects.deleted=1` 的软归档语义，归档历史列表尚未单独提供；本批只要求默认启动台隐藏归档项目。
- 创建项目会写对象存储 `.workspace-keep` 占位对象；如 MinIO 未配置或不可用，创建会失败，避免产生底座不完整的新项目。
- 当前工作区存在大量前序批次未提交改动，本批未回退、未整理这些无关改动。
