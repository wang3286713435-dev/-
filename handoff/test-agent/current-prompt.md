# 测试 Agent 当前任务：8B-GD1 平台侧葛兰岱尔适配骨架验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台-8b-gd`

当前验收批次：

`8B-GD1：平台侧葛兰岱尔适配骨架`

## 0. 验收目标

本轮只验收 8B-GD1：

- 默认 MOCK 模式不回归。
- GLANDAR provider 配置骨架存在。
- 新增 lightweight job / viewer ticket 平台接口。
- 未配置或 MOCK 状态下返回业务化不可用，不 500。
- 不真实调用 Station `SplitUploadFile`。
- 不上传真实 RVT。
- 不暴露 Station token、真实路径、真实 bucket、真实 object key。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/8b-gd-task-graph.md`
- `handoff/main-agent/8b-gd1-glandar-adapter-skeleton-plan.md`
- `handoff/main-agent/8b-gd0-glandar-api-review.md`
- `scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh`

## 2. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台-8b-gd/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台-8b-gd
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如后端未运行，可按项目已有方式启动后重试健康检查和脚本。

## 3. Git 范围检查

允许包含：

- `backend/delivery-visualization-adapter/**`
- `frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/visualization/**`
- `scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh`
- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/current-prompt.md`
- `handoff/main-agent/8b-gd-task-graph.md`
- `handoff/main-agent/8b-gd1-glandar-adapter-skeleton-plan.md`

不允许包含：

- `docs/**`
- Flyway 迁移
- `backend/delivery-data-steward/**` 对象存储迁移主线改动
- Hermes / Qdrant / OpenSearch / documents / chunks
- 真实 Station token / secret / password

## 4. 核心验收点

1. 默认 `BIM_ENGINE_PROVIDER=MOCK`。
2. `lightweight-status` 返回 MOCK 安全状态。
3. `lightweight-plan` 仍为 dry-run，不创建任务。
4. `POST lightweight-jobs` 在 MOCK 下不真实创建转换任务，不 500。
5. `viewer-ticket` 未 ready 时不可用，不返回真实 viewer URL。
6. OpenAPI 包含新增 job / viewer-ticket 接口。
7. 未调用 `SplitUploadFile`、`upload-file` 等真实 Station 上传接口。
8. 8A 回归通过。
9. file-access 回归通过。

## 5. 禁出字段扫描

接口响应、脚本输出和前端可见数据不得包含：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- 真实 bucket 名
- 真实 object key 值
- Station token
- vendor token
- raw row
- SQL
- secret
- password

## 6. P0 / P1 判定

P0：

- 真实调用 Station 上传接口。
- 上传真实 RVT。
- 泄露 Station token / secret / password。
- 引擎直连 NAS / MinIO 底层目录。
- 修改 `docs/**` 或 M3G 对象存储主线。
- file-access 权限链路回归失败。

P1：

- 默认 MOCK 回归失败。
- 新增接口不在 OpenAPI。
- MOCK / 未配置状态返回 500。
- 8B-GD1 专项脚本失败。
- 8A 回归失败。
- 脚本未纳入 Git。

P2：

- 既有 Vite chunk warning。
- UI 文案粗糙但不影响主链路。

## 7. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 测试结论。
- P0 / P1 / P2。
- 必跑命令结果。
- 8B-GD1 专项脚本结果。
- 8A / file-access 回归结果。
- Git 范围检查。
- 禁出字段扫描结果。
- 是否建议主 agent 收口 8B-GD1。
