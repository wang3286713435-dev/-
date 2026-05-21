# 开发 Agent 报告：M2A NAS 受控文件操作安全底座

时间：2026-05-21 16:49 CST

## 1. 本轮目标

完成 M2A：在已完成 M1F 的基础上，补齐真实项目 NAS 受控文件操作安全底座第一批能力。

本轮仅开放低风险受控写操作：项目内新建文件夹、上传文件、重命名、移动、删除到隔离区、从隔离区恢复、操作记录与元数据同步。未开放永久删除、批量操作、跨项目移动、parser / writer / indexing、Hermes 扩展、生产发布。

## 2. 读取与审计

已读取并对齐：

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/main-agent/m2a-controlled-nas-write-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

工作区审计结果：

- 当前分支：`codex/platform-m1e-file-task-continuity`
- 基线提交：`4a58365`
- M1F 员工注册、权限管理与局域网试运行相关改动已在工作区内存在，本轮未回退。
- 本轮新增 M2A 文件与改动均保持在 NAS 受控操作范围内。
- 未修改 `docs/**`。

## 3. 改动文件列表

后端：

- `backend/delivery-app/src/main/resources/db/migration/V22__m2a_controlled_nas_write_foundation.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/controller/ControlledNasController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/application/ControlledNasApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/repository/ControlledNasRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/dto/ControlledNasDtos.java`

前端：

- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`

脚本：

- `scripts/dev/check-m2a-controlled-nas-write.sh`

## 4. 数据库迁移

新增 `V22__m2a_controlled_nas_write_foundation.sql`，未修改旧 Flyway migration。

新增表：

- `data_nas_directory_records`：记录平台创建和管理的目录，使用项目内相对路径、路径哈希和状态，不保存前端可见真实绝对路径。
- `data_nas_quarantine_records`：记录隔离区删除与恢复状态。
- `data_nas_operation_records`：记录受控 NAS 操作、操作人、目标类型、安全展示路径、traceId 和状态。

本地第一次迁移因 MySQL 索引长度限制失败，已将目录唯一索引改为生成列 `relative_path_hash` 后重新执行，当前 V21 / V22 Flyway 状态均为成功。

## 5. 新增后端接口

新增受控 NAS 接口：

- `POST /api/data-steward/projects/{projectId}/nas/directories`
- `PATCH /api/data-steward/projects/{projectId}/nas/directories:rename`
- `POST /api/data-steward/projects/{projectId}/nas/directories:move`
- `POST /api/data-steward/projects/{projectId}/nas/directories:quarantine`
- `POST /api/data-steward/projects/{projectId}/nas/files:upload`
- `PATCH /api/data-steward/projects/{projectId}/nas/files/{fileId}:rename`
- `POST /api/data-steward/projects/{projectId}/nas/files/{fileId}:move`
- `POST /api/data-steward/projects/{projectId}/nas/files/{fileId}:quarantine`
- `POST /api/data-steward/projects/{projectId}/nas/quarantine/{recordId}:restore`
- `GET /api/data-steward/projects/{projectId}/nas/quarantine`
- `GET /api/data-steward/projects/{projectId}/nas/operations`

权限规则：

- `DELIVERY_ENGINEER` / `PROJECT_ADMIN`：新建文件夹、上传、重命名、移动。
- `PROJECT_ADMIN`：删除到隔离区、从隔离区恢复。
- 普通查看者和未授权用户拒绝。
- 项目上下文由服务端校验，不信任前端项目范围。

安全规则：

- 禁止 `..`、绝对路径、`~`、`:`、空字节、重复分隔符、保留目录 `.delivery-quarantine`。
- 禁止跨项目移动。
- 冲突时不覆盖现有文件或目录。
- 隔离删除只移动到项目内 `.delivery-quarantine`，不做永久删除。
- 上传只写入用户选择的文件字节并登记目录元数据，不读取 PDF / Office / DWG / RVT 正文，不做解析、不做索引。
- 响应只返回项目内展示路径、操作编号、traceId、状态等安全字段，不返回真实 NAS 绝对路径。

## 6. 前端改动

文件管理页新增 M2A 入口：

- 上传文件
- 新建文件夹
- 重命名当前文件夹
- 移动当前文件夹
- 删除到隔离区
- 隔离区
- 操作记录
- 文件行级重命名 / 移动 / 删除到隔离区

前端行为：

- 按当前项目角色显示或禁用操作。
- 写操作前弹出确认，明确“将直接操作公司 NAS 文件；不会读取文件正文；不会永久删除；不会展示真实 NAS 绝对路径”。
- 成功后展示操作编号和 traceId。
- 隔离区与操作记录抽屉只显示脱敏路径。
- UI 冒烟已确认项目 503 文件管理页入口存在；操作记录抽屉文案显示“不展示真实 NAS 绝对路径”。未在真实项目 UI 上触发任何写操作。

## 7. 脚本验证

新增 `scripts/dev/check-m2a-controlled-nas-write.sh`。

脚本使用独立临时项目和临时目录，不触碰真实项目 NAS 数据，覆盖：

- 管理员登录。
- 创建测试项目与路径映射。
- 创建文件夹。
- 上传文件。
- 文件重命名、移动、删除到隔离区、恢复。
- 目录重命名、移动、删除到隔离区、恢复。
- 查看者写操作拒绝。
- 未授权用户拒绝。
- 路径穿越拒绝。
- 操作记录和隔离区响应 forbidden-field scan。
- 审计记录检查。
- 恢复后文件状态回到 `PROCESSED`。

结果：`PASS=20 FAIL=0`。

## 8. 自测命令与结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过。
- `corepack pnpm --dir frontend build`：通过。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：`{"status":"UP"}`。
- `bash scripts/dev/check-m2a-controlled-nas-write.sh`：`PASS=20 FAIL=0`。
- `bash scripts/dev/check-m1f-employee-access-control.sh`：`PASS=20 FAIL=0`。
- `bash scripts/dev/check-m1e-file-task-continuity.sh`：`PASS=10 FAIL=0`。
- `bash scripts/dev/check-m1d-standard-delivery-loop.sh`：`PASS=29 FAIL=0`。
- `bash scripts/dev/check-m1c-real-project-masterdata.sh`：`PASS=14 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=18 FAIL=0`。
- `git diff --check`：通过。

## 9. raw path / forbidden-field scan

脚本执行过程中曾发现目录响应里把 `/private/tmp/...` 识别成可展示路径的风险，已修复路径判定规则，将 `/Users/`、`/tmp/`、`/private/`、`/var/` 等本机物理路径纳入物理路径识别。

最终结果：

- M2A 操作记录响应：未发现 raw path / storage path / NAS 绝对路径 / SQL / secret。
- M2A 隔离区响应：未发现 raw path / storage path / NAS 绝对路径 / SQL / secret。
- 前端只显示 `displayPath` / 项目内相对路径。

说明：平台既有 `data_file_resources` 内部仍保存 `storage_uri` 作为资产元数据模型的一部分，本轮未重构该历史内部存储；M2A 新接口和文件管理展示不把它返回到前端。

## 10. 边界确认

- 是否触碰真实项目 NAS 文件：否，验证只使用临时目录。
- 是否开放真实项目受控 NAS 写接口：是，仅限 M2A 允许的低风险操作。
- 是否永久删除：否。
- 是否批量操作：否。
- 是否跨项目移动：否。
- 是否读取文件正文：否。
- 是否 parser / writer / indexing：否。
- 是否修改 Hermes：否。
- 是否修改 `docs/**`：否。
- 是否生产发布：否。

## 11. 已知风险

- 当前 M2A 是受控写操作底座，不代表生产 rollout；真实项目试用前仍建议由主 agent 和测试 agent 明确项目、账号、角色、可操作目录和回滚方式。
- 有多个路径映射的项目会按启用映射的排序选择根目录，正式放开前建议复核真实项目路径映射配置。
- 既有扫描导入的空物理目录如果没有文件元数据，只有被平台创建或登记后才会稳定出现在目录树。
- 隔离恢复遇到目标路径冲突会拒绝，需要人工处理冲突。
- 旧批次中已有删除审批等能力仍保留；M2A 新接口没有新增永久删除入口。

## 12. 未完成事项

- 未做永久删除。
- 未做批量上传 / 批量移动 / 批量删除。
- 未做目录级细粒度权限。
- 未做 NAS 扫描任务扩展。
- 未做 parser / writer / indexing。
- 未做 Hermes、G4、8B、8C、9A。
- 未做生产部署。

## 13. 建议给测试 Agent 的重点

请测试 agent 使用真实后端和前端复核 M2A，但所有写操作优先使用脚本创建的临时项目或专门测试目录，不要在真实业务目录上直接做破坏性验证。

重点复核：

- M2A 脚本 `PASS=20 FAIL=0` 可重复运行。
- 管理员、交付工程师、查看者、未授权用户权限边界。
- 上传、新建目录、重命名、移动、隔离、恢复的文件系统结果和数据库元数据同步。
- 操作记录、隔离区、文件管理页不展示真实 NAS 绝对路径。
- 禁止路径穿越、跨项目移动、覆盖冲突。
- M1F / M1E / M1D / M1C / Phase2 batch4 回归不退化。
