# Windows 端 Codex 项目上下文

本文档用于让 Windows 端长期 Codex 会话获得与当前主 agent 一致的项目记忆。Windows 端 Codex 接手后，必须先读本文档，再执行 `handoff/windows-agent/current-prompt.md`。

## 1. 当前项目定位

项目名称：数字化交付平台。

最终产品定位：面向 `建筑机电/BIM交付` 的单客户私有化数字化交付平台。

项目已经从早期 `v1 MVP` 口径升级为三期路线：

1. 一期：公司内部 BIM 资产管理试点。
2. 二期：可给客户交付的数字化交付平台完整版。
3. 三期：增值服务和客户持续服务。

当前下一步业务开发重点是一期：把公司内部几百个 BIM 项目和约 `10TB` 模型文件通过 NAS 原地接管纳入平台，并为企业内核级 agent 提供稳定数据库检索底座。

## 2. 最新文档基线

Windows 端 Codex 必须优先阅读以下文件：

1. `docs/README.md`
2. `docs/07-complete-delivery-prd.md`
3. `docs/08-acceptance-and-agent-integration.md`
4. `docs/03-architecture-and-system-design.md`
5. `docs/04-rollout-and-agent-prompts.md`
6. `handoff/main-agent/status.md`
7. `handoff/main-agent/decisions.md`

旧文档 `docs/02-v1-prd.md`、`docs/05-phase1-dev-baseline.md`、`docs/06-phase1-backlog-and-readiness.md` 仍有参考价值，但不能覆盖 `docs/07` 和 `docs/08` 的最新裁决。

## 3. 已实现能力

当前源码已经形成可运行 MVP 闭环：

`工程主数据/标准 -> 文件资源 -> 模型集成 -> 管理对象 -> 文档/图纸交付绑定 -> 交付视图 -> 项目首页/智慧大屏 -> 3D 适配层上下文`

已验证过的能力见：

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

样板账号：

- 用户名：`platform.admin`
- 密码：`Admin@123`
- 样板项目：`SAMPLE-MEP-001 / 机电交付样板项目`

## 4. 当前源码状态

仓库当前在 macOS 端还没有首个 commit：

```text
fatal: your current branch 'main' does not have any commits yet
```

因此交接时不能默认使用 `git clone` 就能拿到当前源码。必须二选一：

1. 先在 macOS 端创建 commit 并推送远端，再在 Windows 端 clone。
2. 使用过滤后的源码包交接，排除 `node_modules`、`dist`、`target` 等缓存和构建产物。

## 5. 已有但未完成的资产入库代码

当前源码里已经存在一部分一期 BIM 资产管理初稿，但它还不是完整交付：

- `backend/delivery-app/src/main/resources/db/migration/V7__init_bim_asset_management.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/AssetImportJobRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/BimAssetRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/StorageRootRepository.java`

这部分只完成了数据库迁移、DTO 和部分 repository。尚未完成：

- `StorageProvider` / NAS 访问适配实现。
- 应用服务层。
- REST Controller。
- 前端项目资产台账和模型资源库页面。
- 企业 agent 读模型视图。
- 一期 BIM 资产链路验收脚本。
- Windows 端构建验证。

Windows 端 Codex 接手后必须先确认这部分源码是否可编译。若编译失败，应优先修复到可编译状态，再继续一期功能开发。

## 6. 企业 Agent 对接裁决

企业内核级 agent 当前仍处于 MVP 阶段，后续要接入平台。

已锁定原则：

- agent 权限很高，可以把数据库检索作为自身能力之一。
- 平台必须提供稳定读模型或只读宽表，避免 agent 直接耦合易变业务底表。
- 一期搜索深度为 `元数据 + 路径`。
- 文档正文检索、模型属性检索、构件级检索放到二期或三期。
- 平台保留 REST/OpenAPI，方便未来 agent 工具调用。

一期需要固化的读模型：

- `ProjectAssetView`
- `FileAssetView`
- `ModelAssetView`
- `AuditEventView`

一期落地前，主 agent 还需要再次向企业 agent 项目确认：

- agent 技术栈。
- 读库方式。
- 是否需要同步库、搜索引擎或向量库。
- NAS 路径访问方式。
- 增量同步策略。
- 是否需要平台推送变更事件。

## 7. Windows 端交接目标

Windows 端第一轮只做迁移复核，不继续开发新功能。

必须确认：

- 源码和文档完整。
- Windows 原生 PowerShell 脚本可用。
- 前端可安装依赖并构建。
- 后端可用 `mvnw.cmd` 构建。
- Docker 基础设施可启动。
- 后端和前端可运行。
- 最小链路和 MVP 链路尽量通过。
- `V7` 资产初稿是否影响构建和启动。

完成后必须写入：

- `handoff/windows-agent/latest-report.md`

## 8. 协作规则

- 不要创建临时子 agent。
- 不要回退已有改动。
- 不要删除 `docs/07`、`docs/08` 或 `handoff/main-agent/**` 的最新裁决。
- 不要只按旧 v1 MVP 文档继续开发。
- 不要把企业 agent 直接绑定到业务底表，必须通过稳定读模型或只读宽表。
- Windows 端如果修复兼容性问题，必须在报告中写清楚修改范围和验证结果。
