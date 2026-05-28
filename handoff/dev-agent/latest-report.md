# 开发报告：8B-GD1 葛兰岱尔轻量化引擎适配骨架

时间：2026-05-27 CST

## 1. 本轮目标

本轮执行 `8B-GD1：平台侧葛兰岱尔适配骨架`。

目标是在不影响 M 系列对象存储主线的前提下，为后续葛兰岱尔轻量化引擎 PoC 建立平台侧接口骨架：

- 保持默认 `MOCK` 模式，不改变现有 8A Mock 轻量化能力。
- 增加 `GLANDAR` provider 配置读取边界。
- 增加 lightweight job / viewer ticket 平台接口。
- 不调用葛兰岱尔 Station 上传接口。
- 不读取模型正文，不上传 RVT，不打开真实 Viewer。

## 2. 开发方式说明

本批原计划通过 CMUX Claude Code 开发。已尝试启动：

```bash
/Applications/cmux.app/Contents/Resources/bin/claude --permission-mode bypassPermissions --name 8B-GD1-GLANDAR-ADAPTER
```

Claude/CMUX 在读取上下文后因模型 API 报错中断：

```text
API Error: 400 The content[].thinking in the thinking mode must be passed back to the API.
```

已停止该 Claude 进程，随后由主 agent 按用户授权兜底实现。本轮仍保持独立 8B-GD worktree，不触碰主 M 系列工作区。

## 3. 改动文件

- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarEngineSettings.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/controller/VisualizationAdapterController.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/dto/VisualizationDtos.java`
- `scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh`
- `handoff/main-agent/8b-gd-task-graph.md`
- `handoff/dev-agent/latest-report.md`

未修改：

- `backend/**` 之外的业务后端模块
- `docs/**`
- 数据库迁移
- M3 对象存储模型
- Hermes / parser / indexing 相关代码

## 4. 后端实现

新增 `GlandarEngineSettings`：

- 读取 `BIM_ENGINE_PROVIDER` / `delivery.bim.engine.provider`。
- 读取 `GLANDAR_STATION_API_BASE`、`GLANDAR_STATION_WEB_BASE` 和安全凭据是否注入。
- 默认 provider 为 `MOCK`。
- `GLANDAR` 配置缺失时只返回业务化不可用原因，不抛 500。
- 不把凭据值返回给 API、前端或日志。

扩展现有轻量化状态：

- `lightweight-status` 默认返回 `engineMode=MOCK`。
- `lightweight-plan` 保持 `dryRun=true`，不创建任务。
- 原有 8A Mock 语义保持兼容。

新增平台侧接口：

```text
POST /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

接口行为：

- 校验当前项目上下文。
- 返回平台业务状态。
- MOCK 下不创建真实转换任务。
- GLANDAR 配置就绪时只返回“可进入 8B-GD2”的骨架状态，不真实上传模型。
- viewer ticket 在本批不签发真实入口。
- 响应不包含 Station API URL、Station Web URL、凭据、真实 NAS 路径、bucket、object key 或 SQL。

## 5. 禁止事项执行情况

本轮未执行：

- 葛兰岱尔 `SplitUploadFile` 分片上传。
- 葛兰岱尔 `query-model-info` 任务查询。
- RVT / IFC / DWG 模型正文读取。
- NAS / MinIO 底层路径直连。
- 轻量化产物写入。
- Viewer 真实跳转。
- Hermes memory / documents / chunks / Qdrant / OpenSearch 写入。

## 6. 验证结果

```text
后端构建                                           PASS
8B-GD1 专项脚本                                    PASS=16 FAIL=0
8A Mock 轻量化回归                                  PASS=11 FAIL=0
Phase2 batch4 文件访问安全回归                       PASS=18 FAIL=0
前端构建                                           PASS（仅既有 chunk size warning）
后端健康检查                                       PASS {"status":"UP"}
git diff --check                                   PASS
```

执行命令：

```bash
cd /Users/vc/Documents/数字化交付平台-8b-gd/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台-8b-gd
SERVER_PORT=18088 java -jar backend/delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-phase2-batch4-file-access.sh
corepack pnpm --dir frontend install --frozen-lockfile
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:18088/actuator/health
git diff --check
```

说明：

- 为避免干扰 M 系列主线，本轮临时后端运行在 `18088`。
- `frontend/node_modules` 在独立 worktree 初始不存在，已按锁文件安装依赖后完成前端构建。

## 7. 当前状态

`8B-GD1` 已完成开发自测，建议进入测试 agent 复核。

下一批建议：

- `8B-GD2：105 RVT PoC 转换闭环`
- 前置条件：测试 agent 收口 8B-GD1，并由引擎团队确认可用 Station Token、RVT 样本限制、任务查询状态码和 Viewer URL 拼接方式。
