# 测试 Agent 当前任务：M2B 受控 NAS 写操作真实项目灰度试运行与安全开关验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`M2B：受控 NAS 写操作真实项目灰度试运行与安全开关`

## 0. 必须先阅读

先阅读：

1. `handoff/dev-agent/latest-report.md`
2. `handoff/dev-agent/current-prompt.md`
3. `handoff/main-agent/m2b-nas-write-trial-plan.md`
4. `handoff/main-agent/m2a-controlled-nas-write-closure.md`
5. `handoff/main-agent/m2a-current-project-context-bugfix-report.md`
6. `handoff/main-agent/status.md`
7. `handoff/main-agent/phase2-current-roadmap.md`

## 1. 验收目标

确认 M2B 做到：

1. 未开启灰度时，真实项目不能执行 NAS 写操作。
2. 开启灰度后，只能在允许的项目内相对目录执行 M2A 已允许操作。
3. 允许目录外写操作被服务端拒绝。
4. 查看者和未授权用户仍不能写。
5. 页面清楚显示灰度状态、可写范围和禁用原因。
6. 操作记录、审计和路径脱敏不回归。

## 2. 必跑命令

执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-m2a-controlled-nas-write.sh
bash scripts/dev/check-m1f-employee-access-control.sh
bash scripts/dev/check-m1e-file-task-continuity.sh
bash scripts/dev/check-m1d-standard-delivery-loop.sh
bash scripts/dev/check-m1c-real-project-masterdata.sh
git diff --check
```

## 3. 页面验收

检查真实项目文件管理页：

- 灰度关闭时，写按钮禁用并说明原因。
- 灰度开启时，页面展示允许写入的项目内相对目录。
- 在允许目录内执行小范围写操作。
- 在允许目录外写操作被拒绝。
- 操作记录和隔离区不展示真实 NAS 绝对路径。

## 4. P0 / P1 / P2 判定

P0：

- 未开启灰度也能写真实 NAS。
- 可以写出允许目录范围。
- 查看者或未授权用户可写。
- 永久删除真实文件。
- 泄露真实 NAS 路径。
- Agent / Hermes 触发 NAS 写操作。

P1：

- 页面没有显示灰度状态或可写范围。
- 灰度开启后主链路不可用。
- 灰度关闭错误提示不清楚。
- M2A / M1F / M1E / M1D / M1C 回归失败。

P2：

- 文案或样式仍可优化，但不影响灰度安全和主链路。

## 5. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

必须包含：

1. 测试结论。
2. 当前分支和 commit。
3. 是否确认 M2B 当前 active。
4. P0 / P1 / P2 列表。
5. 必跑命令结果。
6. 灰度关闭验收结果。
7. 灰度开启与允许目录验收结果。
8. 权限隔离结果。
9. 路径脱敏结果。
10. 是否建议收口 M2B。
