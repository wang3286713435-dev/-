# 开发 Agent 报告：M2B 受控 NAS 写操作真实项目灰度试运行与安全开关

时间：2026-05-21 18:31 CST

`<promise>MAINLINE_M2B_NAS_WRITE_TRIAL_COMPLETE</promise>`

## 1. 本轮目标

完成 M2B：在 M2A 受控 NAS 写操作底座之上，增加真实项目灰度试运行安全开关。

本轮只做“是否允许真实项目执行 M2A 写接口”的服务端闸门与前端状态提示，不新增永久删除、批量操作、跨项目移动、Agent/Hermes 写入、parser / writer / indexing，也不做生产发布。

## 2. 读取与审计

已读取并对齐：

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/m2b-nas-write-trial-plan.md`
- `handoff/main-agent/m2a-controlled-nas-write-closure.md`
- `handoff/main-agent/m2a-current-project-context-bugfix-report.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

工作区审计结果：

- 当前分支：`codex/platform-m1e-file-task-continuity`
- 当前提交基线：`1e4f9e3`
- 未修改 `docs/**`。
- 本轮未创建子 agent，未调用 Claude。
- 现有 `tmp/**` 运行日志和临时文件为既有/运行态未跟踪文件，本轮未纳入交付。

## 3. 改动文件列表

后端：

- `backend/delivery-app/src/main/resources/db/migration/V23__m2b_nas_write_trial_config.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/controller/ControlledNasController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/application/ControlledNasApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/repository/ControlledNasRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/dto/ControlledNasDtos.java`

前端：

- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`

脚本：

- `scripts/dev/check-m2a-controlled-nas-write.sh`
- `scripts/dev/check-m2b-nas-write-trial.sh`

## 4. 数据库迁移

新增 `V23__m2b_nas_write_trial_config.sql`，未修改旧 Flyway migration。

新增表：

- `data_nas_write_trial_configs`

保存内容：

- 项目 ID。
- 灰度开关 `enabled`。
- 允许写入的项目内相对根目录列表。
- 允许角色列表。
- 可选允许用户 ID 列表。
- 试运行提示文案。
- 审计时间和操作者。

表内不保存真实 NAS 绝对路径，不保存 `storage_path`，不保存 `storage_uri`。

## 5. 新增 / 修改后端接口

新增：

- `GET /api/data-steward/projects/{projectId}/nas/write-trial?directoryPath=...`
- `PUT /api/data-steward/projects/{projectId}/nas/write-trial`

灰度状态响应包含：

- `projectId`
- `enabled`
- `canWrite`
- `directoryAllowed`
- `roleAllowed`
- `userAllowed`
- `allowedRelativeRoots`
- `allowedRoleCodes`
- `allowedUserIds`
- `disabledReason`
- `trialModeNotice`
- `traceId`

所有 M2A 写操作已接入灰度闸门：

- 创建目录。
- 上传文件。
- 文件重命名 / 移动 / 隔离。
- 目录重命名 / 移动 / 隔离。
- 隔离区恢复。

服务端规则：

- 默认无配置时 `enabled=false`，真实 NAS 写入关闭。
- `PROJECT_ADMIN` / `DELIVERY_ENGINEER` 仍需满足 M2A 基础角色规则。
- 灰度开启后，还必须同时满足允许角色、允许用户、允许项目内相对目录范围。
- 每个受影响路径都要落在允许相对根目录内。
- 不信任前端项目范围，仍使用服务端项目 ID 和角色校验。
- 响应只返回项目内展示路径、相对目录、安全状态和 traceId，不返回真实物理路径。

## 6. 前端改动

文件管理页新增真实 NAS 写入灰度状态提示：

- 未开启时显示“真实 NAS 写入灰度未开启”。
- 开启时显示“真实 NAS 写入灰度已开启”。
- 展示允许角色、可写范围、禁用原因。
- 加载失败时按不可写处理。

写操作入口已叠加灰度判断：

- 上传文件。
- 新建文件夹。
- 重命名当前文件夹。
- 移动当前文件夹。
- 删除到隔离区。
- 隔离区恢复。

UI 冒烟结果：

- 已在 `http://127.0.0.1:5173/data-steward/assets/503?tab=files` 登录 `platform.admin` 查看真实项目 503。
- 文件管理页正常展示真实文件列表。
- 页面提示“真实 NAS 写入灰度未开启”。
- 上传、新建文件夹、重命名、移动等按钮均为禁用状态。
- 未在真实项目页面触发任何写操作。

## 7. M2A 脚本兼容修复

由于 M2B 默认关闭真实 NAS 写入，已同步更新 `scripts/dev/check-m2a-controlled-nas-write.sh`：

- 脚本创建独立临时项目后，为该临时项目写入启用状态的灰度配置。
- 允许根目录为项目根。
- 允许角色为 `DELIVERY_ENGINEER`、`PROJECT_ADMIN`。
- 脚本清理时软删除对应灰度配置。

更新后 M2A 仍可验证底层受控写能力，同时不改变真实项目默认关闭策略。

## 8. M2B 专项脚本

新增 `scripts/dev/check-m2b-nas-write-trial.sh`。

覆盖项：

- 管理员登录。
- 创建独立临时项目和临时 NAS 根目录。
- 默认无灰度配置时写入被拒绝，磁盘无新增。
- 管理员开启项目灰度配置。
- 仅允许 `trial-zone` 项目内相对目录。
- 根目录状态不可写。
- 允许目录内创建目录和上传文件。
- 允许目录外创建、移动被拒绝。
- 查看者被拒绝。
- 关闭开关后写入再次被拒绝。
- 响应 forbidden-field scan。
- 审计记录存在。

结果：`PASS=18 FAIL=0`。

## 9. 自测命令与结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过。
- `corepack pnpm --dir frontend build`：通过。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：`{"status":"UP"}`。
- `bash scripts/dev/check-m2b-nas-write-trial.sh`：`PASS=18 FAIL=0`。
- `bash scripts/dev/check-m2a-controlled-nas-write.sh`：`PASS=21 FAIL=0`。
- `bash scripts/dev/check-m1f-employee-access-control.sh`：`PASS=20 FAIL=0`。
- `bash scripts/dev/check-m1e-file-task-continuity.sh`：`PASS=10 FAIL=0`。
- `bash scripts/dev/check-m1d-standard-delivery-loop.sh`：`PASS=29 FAIL=0`。
- `bash scripts/dev/check-m1c-real-project-masterdata.sh`：`PASS=14 FAIL=0`。
- `git diff --check`：通过。

后端当前已重新启动并通过健康检查。

## 10. raw path / forbidden-field scan

M2B 专项脚本对灰度状态、操作记录、隔离区响应执行 forbidden-field scan。

结果：

- 未发现 raw path。
- 未发现 `storage_path`。
- 未发现 `storage_uri`。
- 未发现 NAS 绝对路径。
- 未发现 SQL。
- 未发现 raw DB row。
- 未发现 secret / token / password 真值。

前端真实项目 503 冒烟只显示项目内目录名、文件名、平台文件 ID、灰度状态和相对可写范围；未展示真实 NAS 绝对路径。

## 11. 边界确认

- 是否触碰真实项目 NAS 文件：否。
- 是否对真实项目默认开放写入：否，默认关闭。
- 是否新增可控灰度开关：是。
- 是否允许配置项目内相对写入范围：是。
- 是否保留 M2A 角色边界：是。
- 是否永久删除：否。
- 是否批量操作：否。
- 是否跨项目移动：否。
- 是否读取 PDF / Office / DWG / RVT 正文：否。
- 是否 parser / writer / indexing：否。
- 是否修改 Hermes：否。
- 是否开放 Agent/Hermes 写操作：否。
- 是否修改 `docs/**`：否。
- 是否生产发布：否。

## 12. 已知风险

- M2B 只是灰度试运行安全闸门，不代表生产 rollout。
- 真实项目要开放写入前，需要主 agent 明确项目、账号、角色、可写相对目录和回滚方式。
- 当前前端只展示灰度状态，不提供完整配置管理台；灰度配置接口和脚本已具备，后续可由管理中心补 UI。
- 如果允许根目录尚不存在，前端在项目根目录处会因当前目录不可写而不能直接创建该根目录；真实试运行建议先配置已有安全目录，或由管理员按计划临时放宽后再收紧。
- 既有 M2A 操作仍会真实修改被允许目录内的 NAS 文件，因此测试 agent 不应在真实业务目录上随意做写入验证。

## 13. 未完成事项

- 未做永久删除。
- 未做批量上传 / 批量移动 / 批量删除。
- 未做目录级细粒度权限模型。
- 未做灰度配置管理前端页面。
- 未做 NAS 扫描任务扩展。
- 未做 parser / writer / indexing。
- 未做 Hermes、G4、8B、8C、9A。
- 未做生产部署。

## 14. 建议给测试 Agent 的重点

请测试 agent 按 M2B 范围复核，不要把 M2B 解释为真实项目全量开放 NAS 写入。

重点：

- `scripts/dev/check-m2b-nas-write-trial.sh` 可重复运行并保持 `PASS=18 FAIL=0`。
- 默认无灰度配置时，真实项目写操作全部关闭。
- 灰度开启后，只有允许角色、允许用户、允许相对目录同时满足时才允许写。
- 目录外写入、移动到目录外、查看者写入、关闭开关后写入均被拒绝。
- M2A 脚本仍可通过，证明底层受控写能力未回归。
- M1F / M1E / M1D / M1C 回归不退化。
- 前端文件管理页能清楚显示灰度状态、可写范围和禁用原因。
- 真实项目 UI 冒烟只读查看，不触发真实写操作。
- 所有响应 forbidden-field scan 不出现 raw path / `storage_path` / `storage_uri` / NAS 绝对路径 / SQL / raw DB row / secret。
