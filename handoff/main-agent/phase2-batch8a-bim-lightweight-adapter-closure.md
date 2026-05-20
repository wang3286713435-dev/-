# 二期 8A BIM 轻量化适配层与 Mock 预览入口收口报告

时间：2026-05-19

## 1. 主 Agent 收口结论

二期 8A 正式收口。

本批次目标是建立 BIM 轻量化能力的适配层合同、Mock 状态、轻量化准备计划和模型集成页入口，不接入真实 BIM 引擎，不执行真实轻量化转换，不读取模型正文，不生成三维预览产物。

开发 agent 已完成实现并写入 `handoff/dev-agent/latest-report.md`。测试 agent 已完成专项验收并写入 `handoff/test-agent/latest-report.md`。当前未发现新的 P0/P1，仅保留既有非阻塞 P2：前端生产构建 Vite chunk size warning。

## 2. 验收通过项

已通过：

- 后端构建：`./mvnw -pl delivery-app -am -DskipTests package`
- 前端构建：`corepack pnpm --dir frontend build`
- 后端健康检查：`/actuator/health`
- `git diff --check`
- 8A 专项脚本：`scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`，`PASS=11 FAIL=0`
- 7B 回归：`scripts/dev/check-phase2-batch7b-preview-conversion-experience.sh`，`PASS=20 FAIL=0`
- 7A 回归：`scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`，`PASS=18 FAIL=0`
- 4R 回归：`scripts/dev/check-phase2-batch4-file-access.sh`，`PASS=18 FAIL=0`
- 6B 回归：`scripts/dev/check-phase2-batch6b-delivery-package.sh`，`PASS=17 FAIL=0`
- 6A 回归：`scripts/dev/check-phase2-batch6a-project-initialization.sh`

## 3. 本批确认能力

后端已新增只读接口：

- `GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-status`
- `GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-plan`

已确认接口行为：

- `engineMode=MOCK`
- `engineConnected=false`
- `viewerAvailable=false`
- `taskStatus=NOT_CREATED`
- `previewMode=BIM_LIGHTWEIGHT`
- `dryRun=true`
- `taskCreated=false`
- `realConversionExecuted=false`
- `nasFileTouched=false`

前端模型集成页已新增：

- 轻量化状态列。
- 适配模式 / 引擎列。
- 查看轻量化准备入口。
- 打开 3D 预览入口。
- Mock 状态说明和安全提示。

`打开 3D 预览入口` 当前只提示 Mock 状态，不跳转伪造三维页面。

## 4. 红线复核

本批未发生：

- 真实 BIM 引擎接入。
- RVT / IFC / NWD / NWC 真实轻量化转换。
- GLB / 3D Tiles / SVF / cache 产物生成。
- 模型正文、构件、属性、楼层或视图读取。
- 真实 NAS 文件创建、移动、删除、改名或写入。
- MinIO / OpenSearch / Qdrant / documents / chunks 写入。
- Hermes memory 写入。
- 真实 NAS 路径或底层存储字段泄露。

专项脚本和手工抽查均确认响应不包含：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storageUri`
- `raw row`
- `SQL`

## 5. 已知风险

- 8A 是 Mock 适配与 dry-run 准备检查，不代表真实 3D 预览已经可用。
- 真实 BIM 引擎、转换队列、产物存储、Viewer 授权、失败重试和格式兼容仍未进入实现。
- 后续批次不能把 8A 的 Mock 入口包装成真实可用 Viewer。

## 6. 主 Agent 裁决

二期 8A 可正式收口，作为后续 BIM 轻量化真实接入的合同基线。

用户最新裁决：8A 收口后暂时暂停，不进入 8B。

后续是否进入 `8B：BIM 引擎接入前置与轻量化任务编排骨架`，等待用户再次明确确认。在确认前，不启动新开发批次，不扩展真实引擎、真实转换、构件级能力或客户交付运维能力。
