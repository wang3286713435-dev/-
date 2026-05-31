# 开发 Agent 报告：DT-F1 BIM 协同 / 葛兰岱尔适配兼容收口

时间：2026-06-01 CST

## 1. 当前分支与 checkpoint

- 当前分支：`codex/m3g-nas-minio-real-project-object-storage`
- checkpoint commit：`e82d746 chore: checkpoint M3G before Glandar mainline sync`
- merge commit：`0c9a1ef merge: sync Glandar mainline into M3G`
- 合并来源：`origin/main`
- `origin/main` 最新葛兰岱尔提交：`d69cbc2 feat: integrate Glandar RVT lightweight preview pilot...`
- 合并前已确认：`d69cbc2` 存在于 `origin/main`
- 合并后已确认：`d69cbc2` 已进入当前 `HEAD`

checkpoint 已排除：

- `.claude/**`
- `CLAUDE.md`
- `tmp/**`
- 本地 jar / pid / log
- 本机密钥、token、license、NAS 真实路径配置

## 2. 合并与冲突处理

已执行 `git merge origin/main`。

冲突文件均为 handoff 文档：

- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/status.md`
- `handoff/test-agent/current-prompt.md`
- `handoff/test-agent/latest-report.md`

解决策略：

- 当前 prompt / report 保留本分支 DT-F1 与 M3G 收口上下文。
- `origin/main` 的葛兰岱尔 handoff 资料作为新增文件保留。
- 后端、前端业务代码未发生手工冲突。

## 3. 葛兰岱尔能力同步结果

已同步 `origin/main` 中的葛兰岱尔能力，包括：

- `V31__8b_gd2_glandar_lightweight_jobs.sql`
- `GlandarEngineSettings`
- `GlandarStationClient`
- `LightweightJobRepository`
- `GlandarViewerCanvas`
- `GlandarModelPreviewPage`
- `/api/visualization-adapter/projects/{projectId}/glandar/rvt-pilot-files`
- lightweight job / viewer ticket 相关接口
- `/bim-collaboration`
- `/visualization/glandar-viewer`
- 105 项目 10 个 RVT 试点模型清单

## 4. M3G 能力保留结果

已保留当前 M3G 主线能力：

- NAS 侧 MinIO provider health
- storage status
- 对象化 / migration task
- file-access 对象优先读取
- NAS fallback 显式状态
- 文件管理器对象化状态展示
- M3G-4 / M3G-8 等回归脚本

本轮未执行新的历史文件迁移。

## 5. 脚本兼容修复

合并后对两个回归脚本做了环境兼容修复：

- `scripts/dev/check-m3a-storage-service-foundation.sh`
  - 旧脚本默认向本地 MinIO 容器写 smoke 对象。
  - 当前后端实际使用 NAS 侧 MinIO，因此脚本改为优先读取 `tmp/local-env/nas-minio.env` / `DELIVERY_MINIO_*`，把 smoke 对象写入后端实际连接的 MinIO。

- `scripts/dev/check-8c-gd1-ten-rvt-platform-preview.sh`
  - 未注入 GLANDAR 真实配置时，脚本验证 10 个 RVT 清单、业务化阻断、禁出字段，而不是强制要求 10 个 READY。
  - 显式配置 `BIM_ENGINE_PROVIDER=GLANDAR` 且 Station 配置完整时，仍按严格 READY / Viewer ticket 验证。

## 6. 路由与接口验证

- 前端 `/bim-collaboration?projectId=503&preview=glandar` 可返回页面 HTML。
- 前端 `/visualization/glandar-viewer?projectId=503&jobId=7` 可返回页面 HTML。
- 后端 105 RVT 试点接口返回 10 个模型。
- 当前未注入 GLANDAR 真实配置，试点模型状态为非 READY，viewer 返回业务化阻断，不伪装成功。
- pilot list forbidden-field scan 通过。

## 7. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
Phase2 Batch4 文件访问安全回归                               PASS=18 FAIL=0
M3A storage foundation                                       PASS=8 FAIL=0
M3B object storage mirror trial                              PASS=11 FAIL=0
M3C storage migration task center                            PASS=9 FAIL=0
M3G-4 controlled multi-project objectification               PASS=21 FAIL=0
M3G-8 object-first read / fallback                           PASS=7 FAIL=0
8C-GD1 ten RVT platform preview default mode                 PASS=8 FAIL=0
```

已执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-8c-gd1-ten-rvt-platform-preview.sh
```

待完成 merge commit 前还需执行：

```bash
git diff --check
git diff --cached --check
```

## 8. 禁出字段检查

已检查脚本响应与 pilot list：

- 未发现 `/Volumes`
- 未发现 `smb://`
- 未发现 `nas://`
- 未发现 `storage_uri`
- 未发现 `bucket`
- 未发现 `object_key`
- 未发现 token / secret / password 真值
- 未发现 SQL / raw row

## 9. 未完成事项

- 当前未注入真实 GLANDAR Station 配置，因此只能验证默认安全模式和 10 个 RVT 清单，不能验证真实 READY Viewer。
- 如需验证葛兰岱尔真实模式，需要由主 agent 或用户在本机安全注入 `BIM_ENGINE_PROVIDER=GLANDAR`、Station API/Web 地址和安全凭据；凭据不得进入 Git、handoff、日志或前端响应。
- 已用 `git merge-base --is-ancestor d69cbc2 HEAD` 确认当前分支包含 `d69cbc2`。

## 10. 服务状态

- 后端保持运行：`http://127.0.0.1:8080`
- 前端 dev server 保持运行：`http://127.0.0.1:5173`
- 后端已重启到合并后的新 jar，Flyway schema 当前为 version `31`。
