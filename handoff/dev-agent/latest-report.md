# 开发 Agent 报告：M2I 105 全文件工程树归属治理与 Hermes 辅助

时间：2026-05-25 CST

## 1. 本轮目标

本轮按 `M2I：105 全文件工程树归属治理与 Hermes 辅助` 执行。目标是接管并审计主 agent 留下的 M2I 草稿，把 105 项目已登记文件纳入可解释的工程树归属治理，同时保持正式交付挂接、真实 NAS 操作和 Hermes 写动作边界不被扩大。

核心边界：

- 文件归属不等于正式交付挂接。
- 105 全文件进入已确认归属、待确认推荐、待判定或归档/参考等治理状态。
- 正式文档/图纸交付仍走现有交付闭环，不把全部文件强行变成交付项。
- Hermes 只作为辅助建议/解释入口，不自动写库、不读取正文、不移动 NAS 文件。

## 2. 已阅读关键材料

- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- M2I 草稿相关迁移、后端 ownership 包、catalog 接口、前端文件管理器、工程树面板和专项脚本

## 3. 草稿审计结果

- `V26__ctower_demo_validation_boundary.sql` 是 no-op 边界迁移，用于避免 V26 被不同分支重复占用；保留。
- `V27__m2i_file_ownership_governance.sql` 新增文件归属表，字段不包含真实 NAS 路径、`storage_path` 或 `storage_uri`；保留并继续使用。
- 草稿写接口已要求 `confirmed=true`，但项目成员只要有项目访问就可能写入；已修正为仅 `DELIVERY_ENGINEER` / `PROJECT_ADMIN` 可写。
- 草稿信任前端传入的 recommendation 行，存在伪造 fileName/nodePath/reason/evidence 写入风险；已改为后端按 fileId 重新查询当前项目文件，并只保留脱敏后的节点和说明字段。
- 草稿工程树节点点击后只展示推荐项，不展示已归属文件；已新增节点文件查询接口和前端列表。
- 草稿文案容易把资产推导树误读成最终工程结构；已调整为“资产推导工程树”，并明确“不等于正式交付完成”。

## 4. 修改文件列表

- `backend/delivery-app/src/main/resources/db/migration/V26__ctower_demo_validation_boundary.sql`
- `backend/delivery-app/src/main/resources/db/migration/V27__m2i_file_ownership_governance.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/CatalogController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/ownership/FileOwnershipController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/ownership/FileOwnershipApplicationService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/FileOwnershipTreePanel.vue`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `scripts/dev/check-m2i-105-file-ownership-governance.sh`
- `handoff/dev-agent/latest-report.md`

说明：工作区中仍有 `.claude/**`、`CLAUDE.md`、`tmp/**` 等既有未跟踪项，本轮未纳入交付文件。

## 5. 后端改动

- 新增 `data_file_ownership_assignments`，保存文件归属记录、归属类型、状态、置信度、来源、推荐理由、证据摘要和人工确认信息。
- 新增/补齐文件归属接口：
  - `GET /api/data-steward/projects/{projectId}/file-ownership/coverage`
  - `GET /api/data-steward/projects/{projectId}/file-ownership/tree`
  - `GET /api/data-steward/projects/{projectId}/file-ownership/files`
  - `GET /api/data-steward/projects/{projectId}/file-ownership/unassigned`
  - `POST /api/data-steward/projects/{projectId}/file-ownership/recommendations`
  - `POST /api/data-steward/projects/{projectId}/file-ownership/recommendations:apply`
  - `PUT /api/data-steward/projects/{projectId}/file-ownership/assignments:batch`
- 写接口要求 `confirmed=true`，并要求当前用户在项目内具备 `DELIVERY_ENGINEER` 或 `PROJECT_ADMIN` 角色。
- 推荐写入时后端按 fileId 重新查询当前项目文件，不信任前端传入的文件名、路径、证据摘要。
- 响应字段增加归属状态、归属节点、来源、置信度等目录级信息，并继续执行 raw path / secret / SQL 等 forbidden-field 脱敏防线。
- `catalog/files` 支持 `ownershipStatus` 筛选，文件列表和详情可展示归属节点。

## 6. 前端改动

- 项目详情新增“资产推导工程树”入口。
- `FileOwnershipTreePanel.vue` 支持查看覆盖率、归属树、待确认推荐、当前节点已归属文件和分页。
- 工程树节点点击后可看到该节点及子节点下的已归属文件。
- 文件管理器增加归属节点、归属状态展示，并支持按归属状态筛选。
- 右键菜单保留“询问 Hermes 归属建议”入口，仅打开辅助建议面板，不触发自动写库。
- 页面文案明确：资产归属用于治理和复核，不代表正式交付挂接完成。

## 7. 脚本改动

- 新增并执行 `scripts/dev/check-m2i-105-file-ownership-governance.sh`。
- 脚本覆盖：
  - 管理员登录和切换 105 项目。
  - 归属覆盖率统计。
  - 生成推荐和人工确认应用。
  - 105 未归属文件数为 0。
  - 工程树和节点文件查询可用。
  - forbidden-field scan 不出现真实 NAS 路径、storage 字段、SQL 或 secret。
  - 正式交付 dry-run 未被 2928 个文件污染。

## 8. 105 当前结果

当前 105 对应本地项目 ID：`503`。

```text
105 文件总数：2928
归属记录数：2928
未归属文件数：0
当前归属依据：目录级 metadata / catalog 规则
正式交付关系：仍由原文档/图纸交付闭环控制
```

## 9. 安全边界

- 未移动、删除、重命名、上传真实 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未新增 parser / writer / indexing / 向量库。
- 未写入 OpenSearch / Qdrant / MinIO documents/chunks。
- 未让 Hermes 写库、写 memory、自动挂接、自动审批或自动整改。
- 未返回真实 `storage_path`、`storage_uri`、`/Volumes`、`smb://`、raw DB row、SQL、token、secret。
- 未修改 `docs/**`。

## 10. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M2I 专项脚本                                                 PASS=8 FAIL=0
M2H Windows 文件管理器回归                                   PASS=51 FAIL=0
M2G 文件管理 polish 回归                                     PASS=26 FAIL=0
M2B NAS 写入灰度回归                                         PASS=18 FAIL=0
M2F 真实项目交付闭环回归                                     PASS=6 FAIL=0
Phase2 batch4 文件访问安全回归                               PASS=18 FAIL=0
git diff --check                                             PASS
```

执行命令：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-m2g-nas-file-manager-polish.sh
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-m2f-real-project-delivery-loop.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 11. 风险与未完成事项

- 当前归属推荐是目录级 metadata 规则，不等于 BIM/图纸正文理解；高价值文件仍需要项目负责人复核。
- 工程树是“资产推导工程树”，不是最终施工/楼栋/系统级工程树。
- Hermes 当前只是辅助入口；如需让 Hermes 参与更深治理，需要另开批次并保持人工确认防线。
- 前端构建存在既有 Vite chunk size warning，不影响本轮构建通过。
