# 测试 Agent 当前任务：8B-GD2 葛兰岱尔 RVT PoC 转换闭环验收

你是数字化交付平台测试 agent。本轮只测试独立 3D 引擎分支，不测试主 M 系列对象存储分支。

工作目录：

`/Users/vc/Documents/数字化交付平台-8b-gd`

当前分支必须是：

`codex/8b-gd2-rvt-poc`

## 1. 本轮目标

验收 `8B-GD2：105 RVT PoC 转换闭环`。

本批应能对 105 项目 1-3 个 RVT 小样本提交葛兰岱尔 Station 转换任务，并通过平台接口查询状态。

## 2. 必读文件

- `handoff/main-agent/8b-gd-task-graph.md`
- `handoff/main-agent/8b-gd2-glandar-rvt-poc-plan.md`
- `handoff/dev-agent/latest-report.md`
- `scripts/dev/check-8b-gd2-glandar-rvt-poc.sh`
- `/Users/vc/Downloads/葛兰岱尔StationManagement平台接入API文档.md`

## 3. 禁止事项

测试中禁止：

- 不要修改业务代码。
- 不要修改 `docs/**`。
- 不要提交 Git。
- 不要打印 token。
- 不要上传全量模型。
- 不要触碰真实 NAS 文件增删改名。
- 不要写 Hermes memory / documents / chunks / Qdrant / OpenSearch。
- 不要停止主线 8080 服务；如需后端，使用 18088。

## 4. 必测项

1. 缺少 GLANDAR 配置时，接口业务化阻断，不 500。
2. GLANDAR 配置完整时，105 RVT 样本可提交转换任务。
3. 平台任务记录可查询。
4. Station 查询结果可映射为平台状态。
5. 如果 Station 成功，状态应进入 READY 或 viewer 可用状态。
6. 如果 Station 失败，失败原因必须可见。
7. API 响应不得包含：
   - `/Volumes`
   - `smb://`
   - `nas://`
   - `storage_uri`
   - bucket
   - object_key
   - Token
   - secret
   - raw row
   - SQL
8. 8B-GD1 / 8A / file-access 回归通过。

## 5. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台-8b-gd

git branch --show-current
git status --short

cd backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台-8b-gd
mkdir -p tmp/run-logs
SERVER_PORT=18088 java -jar backend/delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar > tmp/run-logs/8b-gd2-test-backend.log 2>&1 &
APP_PID=$!

for i in {1..60}; do
  if curl -fsS http://127.0.0.1:18088/actuator/health >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

curl -fsS http://127.0.0.1:18088/actuator/health

BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-8b-gd2-glandar-rvt-poc.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-phase2-batch4-file-access.sh

corepack pnpm --dir frontend build

git diff --check

kill $APP_PID || true
```

如果本轮要测试真实 Station 转换，必须先由操作者在本地安全 shell 注入：

```bash
export BIM_ENGINE_PROVIDER=GLANDAR
export GLANDAR_STATION_API_BASE=http://192.168.1.37:18086
export GLANDAR_STATION_WEB_BASE=http://192.168.1.37:18087
export GLANDAR_TOKEN='<secure-token>'
```

不要把 token 写入报告。

## 6. P0 / P1 判定

P0：

- token / secret 泄露。
- 真实 NAS 路径、bucket、object key 泄露。
- 误触发全量模型上传。
- 修改或删除真实 NAS 文件。
- 破坏 file-access 安全链路。

P1：

- 105 RVT 样本无法提交任务且不是 Station/凭据环境问题。
- Station 查询接口不可用且没有业务化失败原因。
- 新增接口不在 OpenAPI。
- 新增关键文件未纳入 Git 跟踪。
- 缺配置时返回 500。

P2：

- 既有前端 chunk warning。
- Station 转换耗时较长但状态可查。

## 7. 报告要求

写入：

`handoff/test-agent/latest-report.md`

报告必须说明：

- 是否真的执行 Station 转换。
- 使用的 fileId，不写真实路径。
- 是否拿到 lightweightName。
- Station 状态映射结果。
- 禁出字段扫描结果。
- 是否建议主 agent 收口 8B-GD2。
