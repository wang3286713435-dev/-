# M2B 受控 NAS 写操作真实项目灰度试运行与安全开关复核报告

生成时间：2026-05-21 18:43 CST

## 1. 复核结论

结论：通过。

本轮按 M2B 范围复核“受控 NAS 写操作真实项目灰度试运行与安全开关”。M2B 专项脚本连续两次通过，M2A 底层受控写能力和 M1F / M1E / M1D / M1C 回归均未退化。真实项目 UI 仅做只读冒烟，未触发真实业务目录写入。

当前未发现 P0 / P1 / P2。建议主 agent 进入 M2B 收口判断。

## 2. 当前分支与阅读范围

- 当前目录：`/Users/vc/Documents/数字化交付平台`
- 当前分支：`codex/platform-m1e-file-task-continuity`
- 当前提交：`1e4f9e3`

已阅读并复核：

- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/m2b-nas-write-trial-plan.md`
- `scripts/dev/check-m2b-nas-write-trial.sh`

静态确认：

- 新增迁移为 `V23__m2b_nas_write_trial_config.sql`，表内仅保存项目 ID、开关、允许相对目录、允许角色、允许用户和提示文案，不保存真实 NAS 绝对路径。
- OpenAPI 包含 `GET /api/data-steward/projects/{projectId}/nas/write-trial` 和 `PUT /api/data-steward/projects/{projectId}/nas/write-trial`。
- OpenAPI 未发现 permanent / purge / hard-delete / batch 类永久删除或批量写接口。
- 未发现 `docs/**` 修改。
- 未发现 Hermes / G4 / 8B / 8C / 9A、parser / writer / indexing 相关新增扩展。

## 3. 自动化命令结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 后端健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- M2B 专项脚本第 1 次：`bash scripts/dev/check-m2b-nas-write-trial.sh`，通过，`PASS=18 FAIL=0`。
- M2B 专项脚本第 2 次：`bash scripts/dev/check-m2b-nas-write-trial.sh`，通过，`PASS=18 FAIL=0`。
- M2A 回归：`bash scripts/dev/check-m2a-controlled-nas-write.sh`，通过，`PASS=21 FAIL=0`。
- M1F 回归：`bash scripts/dev/check-m1f-employee-access-control.sh`，通过，`PASS=20 FAIL=0`。
- M1E 回归：`bash scripts/dev/check-m1e-file-task-continuity.sh`，通过，`PASS=10 FAIL=0`。
- M1D 回归：`bash scripts/dev/check-m1d-standard-delivery-loop.sh`，通过，`PASS=29 FAIL=0`。
- M1C 回归：`bash scripts/dev/check-m1c-real-project-masterdata.sh`，通过，`PASS=14 FAIL=0`。
- 空白字符检查：`git diff --check`，通过。

## 4. M2B 专项覆盖结果

结果：通过。

专项脚本使用独立临时项目和 `/tmp/delivery-m2b-nas-*` 临时 NAS 根目录，未触碰真实业务 NAS 目录。连续两次运行均通过，并在退出时清理灰度配置、测试项目、测试账号和临时目录。

已覆盖：

- 默认无灰度配置时，`enabled=false` 且 `canWrite=false`。
- 灰度关闭时写接口被拒绝，磁盘无新增。
- 管理员可开启项目级灰度配置。
- 灰度配置只允许 `trial-zone` 项目内相对目录。
- 项目根目录因不在白名单内不可写。
- 命中允许目录后，允许管理员创建目录和上传文件。
- 允许范围外新建目录被拒绝。
- 把允许目录内文件移动到目录外被拒绝。
- 查看者即使命中允许目录也不可写。
- 关闭开关后，允许目录内再次写入也被拒绝。
- 成功操作记录可查且不泄露真实路径。
- 隔离区查询可用且不泄露真实路径。
- 灰度配置和 NAS 成功操作均写入审计日志。

## 5. 真实项目默认关闭验证

结果：通过。

对真实项目 503 / 506 只读检查灰度状态：

- `GET /api/data-steward/projects/503/nas/write-trial?directoryPath=`：`code=OK`，有 `traceId`，`enabled=false`，`canWrite=false`，`directoryAllowed=false`，`roleAllowed=true`，`disabledReason=当前项目未开启真实 NAS 写入灰度。`
- `GET /api/data-steward/projects/506/nas/write-trial?directoryPath=`：`code=OK`，有 `traceId`，`enabled=false`，`canWrite=false`，`directoryAllowed=false`，`roleAllowed=true`，`disabledReason=当前项目未开启真实 NAS 写入灰度。`
- 数据库检查：`data_nas_write_trial_configs` 中 503 / 506 活跃配置数为 0。
- 两个响应均未发现 raw path、`storage_path`、`storage_uri`、NAS 绝对路径、SQL、raw DB row、secret / token / password。

## 6. 权限、目录范围与关闭开关判断

结果：通过。

已确认 M2B 满足“角色 + 用户 + 相对目录 + 开关”同时满足才允许写：

- 允许角色不满足时拒绝。
- 允许用户不满足时拒绝。
- 允许相对目录不满足时拒绝。
- 灰度开关关闭时拒绝。
- 允许目录内操作成功后，尝试移动到允许目录外被拒绝。
- 查看者写入被拒绝。
- M2A 的查看者 / 未授权用户 / 路径穿越 / 隔离恢复 / 审计边界仍通过。

## 7. 前端真实项目只读冒烟

结果：通过。

使用 `platform.admin / Admin@123` 打开真实项目：

- `/data-steward/assets/503?tab=files`

只读检查结果：

- 页面显示 `真实 NAS 写入灰度未开启`。
- 页面显示可写范围为 `未配置可写目录`。
- 页面显示允许角色为 `DELIVERY_ENGINEER、PROJECT_ADMIN`。
- 页面显示禁用原因：`当前项目未开启真实 NAS 写入灰度。`
- `上传文件`、`新建文件夹`、`重命名当前文件夹`、`移动当前文件夹`、`删除到隔离区` 均为禁用状态。
- `隔离区`、`操作记录` 查询入口仍可见。
- 文件列表和目录树正常加载。
- 页面未发现 raw NAS path、`storage_path`、`storage_uri`、`storageUri`、SQL、raw DB row、secret / token / password。

本轮未在真实项目 UI 上触发上传、移动、删除、重命名、隔离、恢复等写操作。

## 8. 禁止边界复核

结果：通过。

本轮未发现：

- 默认关闭状态下真实项目可写。
- 写出允许目录范围。
- 查看者或未授权用户可写。
- 永久删除真实文件。
- 批量删除 / 批量移动 / 批量重命名。
- 跨项目移动。
- 前端或接口响应泄露真实 NAS 绝对路径。
- Agent / Hermes 触发 NAS 写操作。
- parser / writer / indexing。
- G4 / 8B / 8C / 9A 扩展。

## 9. P0 / P1 / P2 问题列表

- P0：无。
- P1：无。
- P2：无。

非阻塞观察：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮 M2B 验收。
- 当前前端只显示灰度状态，不提供完整灰度配置管理页面；这与开发报告中的“后续可由管理中心补 UI”一致，不阻塞 M2B 安全底座收口。

## 10. 收口建议

建议主 agent 判定 M2B 收口。

M2B 已满足：

- 默认无灰度配置时，真实项目写操作关闭。
- M2B 脚本可重复运行并保持 `PASS=18 FAIL=0`。
- 灰度开启后，只有允许角色、允许用户、允许相对目录同时满足才允许写。
- 目录外写入、移动到目录外、查看者写入、关闭开关后写入均被拒绝。
- M2A 底层受控写能力未回归。
- M1F / M1E / M1D / M1C 回归不退化。
- 前端文件管理页可显示灰度状态、可写范围和禁用原因。
- 真实项目 UI 只读冒烟未触发真实业务目录写入。
- 所有抽查响应未出现 forbidden 字段。
