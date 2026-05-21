# M2A NAS 受控文件操作安全底座复核报告

生成时间：2026-05-21 17:07 CST

## 1. 复核结论

结论：通过。

本轮复核 `M2A：NAS 受控文件操作安全底座`。已验证低风险受控写操作只在脚本创建的 `/tmp` 隔离项目根目录内执行，真实项目页面仅做只读人工检查，未在真实业务目录执行上传、移动、删除等写操作。

当前未发现 P0 / P1 / P2。建议主 agent 进入 M2A 收口判断。

## 2. 当前分支与阅读范围

- 当前目录：`/Users/vc/Documents/数字化交付平台`
- 当前分支：`codex/platform-m1e-file-task-continuity`
- 当前提交：`4a58365`

已阅读并复核：

- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/m2a-controlled-nas-write-plan.md`
- `scripts/dev/check-m2a-controlled-nas-write.sh`

静态确认：

- 新增迁移为 `V22__m2a_controlled_nas_write_foundation.sql`，未发现修改旧 Flyway migration。
- M2A 新接口集中在 `/api/data-steward/projects/{projectId}/nas/**`。
- OpenAPI 仅出现允许范围内 11 个 NAS 受控接口，未发现 permanent / purge / hard-delete 类永久删除接口。
- M2A 响应 DTO 不包含 `storageUri` / `storage_path` / `storage_uri` 字段。
- 服务端统一做项目权限、相对路径、符号链接/路径逃逸、冲突、隔离恢复状态校验。

## 3. 自动化命令结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 后端健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- M2A 专项脚本：`bash scripts/dev/check-m2a-controlled-nas-write.sh`，通过，`PASS=20 FAIL=0`。
- M1F 回归：`bash scripts/dev/check-m1f-employee-access-control.sh`，通过，`PASS=20 FAIL=0`。
- M1E 回归：`bash scripts/dev/check-m1e-file-task-continuity.sh`，通过，`PASS=10 FAIL=0`。
- M1D 回归：`bash scripts/dev/check-m1d-standard-delivery-loop.sh`，通过，`PASS=29 FAIL=0`。
- M1C 回归：`bash scripts/dev/check-m1c-real-project-masterdata.sh`，通过，`PASS=14 FAIL=0`。
- Phase2 batch4 文件访问回归：`bash scripts/dev/check-phase2-batch4-file-access.sh`，通过，`PASS=18 FAIL=0`，输出 `phase2 batch4 file access check ok`。
- 空白字符检查：`git diff --check`，通过。

## 4. M2A 专项脚本覆盖结果

结果：通过。

专项脚本使用独立临时项目和 `/tmp/delivery-m2a-nas-*` 临时 NAS 根目录，未触碰真实业务 NAS 目录。已覆盖：

- 管理员登录并切换到隔离测试项目。
- 管理员创建项目内文件夹。
- 管理员上传小文件。
- 新建空目录进入目录树。
- 文件重命名、移动、删除到隔离区、恢复。
- 文件夹重命名、移动、删除到隔离区、恢复。
- `PROJECT_VIEWER` 写操作被拒绝。
- 未授权员工写操作被拒绝。
- `../escape` 路径穿越被拒绝。
- 操作记录可查且不泄露真实路径。
- 隔离记录可查且不泄露真实路径。
- NAS 操作写入审计日志。
- 恢复后文件元数据回到 `PROCESSED`。

脚本结论为 `PASS=20 FAIL=0`。

## 5. 权限边界复核

结果：通过。

已确认：

- `PROJECT_VIEWER` 不允许新建文件夹等 NAS 写操作。
- 未授权员工不允许对项目执行 NAS 写操作。
- 删除到隔离区和恢复由服务端要求 `PROJECT_ADMIN`。
- 新建、上传、重命名、移动由服务端要求 `DELIVERY_ENGINEER` 或 `PROJECT_ADMIN`。
- 所有接口先校验当前登录用户、当前项目上下文和项目权限，不只依赖前端按钮隐藏。

## 6. 路径与操作安全复核

结果：通过。

已确认：

- 前端和接口按相对路径 / 文件 ID / 项目 ID 工作，不需要前端传真实 NAS 绝对路径。
- 服务端禁止 `..`、绝对路径、`~`、冒号、空字节、重复分隔符和 `.delivery-quarantine` 作为操作目标。
- 服务端通过 canonical/real path 校验项目根目录，阻止符号链接或路径逃逸。
- 重命名、移动、上传和新建目录都调用冲突校验，不覆盖同名文件或目录。
- 文件和文件夹删除均移动到 `.delivery-quarantine`，未发现永久删除入口。
- 恢复时若原位置存在冲突会拒绝，需要人工处理。
- 跨项目移动没有独立入口；文件操作按 `projectId + fileId` 查询当前项目文件，不属于当前项目的文件不会被操作。

## 7. 操作记录、隔离区与 raw path 检查

结果：通过。

M2A 脚本和真实项目 503 接口抽查均通过 forbidden-field scan。

真实项目 503 只读接口抽查：

- `GET /api/data-steward/projects/503/nas/operations?limit=20`：`code=OK`，存在 `traceId`，当前返回 0 条。
- `GET /api/data-steward/projects/503/nas/quarantine?limit=20`：`code=OK`，存在 `traceId`，当前返回 0 条。
- 两个响应均未发现 `/Volumes`、`/Users`、`/tmp`、`/private`、`/var`、`nas://`、`smb://`、`storage_path`、`storage_uri`、`storageUri`、`raw row`、`SQL`、`token`、`secret`、`password`。

脚本中的操作记录与隔离区响应也未发现上述 forbidden 字段。

## 8. 前端人工复核

结果：通过。

使用 `platform.admin / Admin@123` 登录后打开真实项目：

- `/data-steward/assets/503?tab=files`

只读检查结果：

- 文件管理页可见 M2A 入口：`上传文件`、`新建文件夹`、`重命名当前文件夹`、`移动当前文件夹`、`删除到隔离区`、`隔离区`、`操作记录`。
- 项目根目录下，`重命名当前文件夹`、`移动当前文件夹`、`删除到隔离区` 为禁用状态，避免直接操作项目根。
- 文件列表原有列仍存在：名称 / 平台文件 ID、类型、扩展名、专业、版本、大小、置信度、质量问题、更新时间、操作。
- `隔离区` 抽屉可打开，文案明确：`隔离区支持恢复，不提供永久删除；列表不展示真实 NAS 绝对路径。`
- `操作记录` 抽屉可打开，文案明确：`这里只展示受控操作记录，不展示真实 NAS 绝对路径。`
- 真实项目页面和两个抽屉中未发现 raw NAS 路径、`storage_path`、`storage_uri`、`storageUri`、SQL、token、secret、password。
- 本轮没有在真实项目 UI 上点击确认任何上传、移动、删除、重命名等写操作。

## 9. 禁止边界复核

结果：通过。

本轮未发现：

- 永久删除入口或永久删除执行。
- 批量移动、批量删除、批量重命名。
- 跨项目移动入口。
- parser / writer / indexing。
- Hermes / G4 / 8B / 8C / 9A 扩展。
- 前端或接口响应暴露 raw NAS 绝对路径。
- 在真实业务目录执行破坏性写操作。

本轮实际文件系统写操作仅发生在专项脚本创建的 `/tmp/delivery-m2a-nas-*` 隔离根目录内，脚本退出后会清理临时目录和测试数据。

## 10. P0 / P1 / P2 问题列表

- P0：无。
- P1：无。
- P2：无。

非阻塞观察：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮 M2A 验收。
- 项目 503 当前操作记录和隔离区为空，这符合未在真实项目执行写操作的复核边界。

## 11. 收口建议

建议主 agent 判定 M2A 收口。

M2A 已满足：

- 项目内新建文件夹、上传、重命名、移动、删除到隔离区、恢复在隔离项目内通过。
- 查看者和未授权用户写操作被拒绝。
- 路径穿越、覆盖冲突、跨项目操作边界具备服务端保护。
- 隔离删除不是永久删除，恢复后文件 ID / 元数据保持可用。
- 操作记录和隔离区响应不泄露 raw path。
- M1F、M1E、M1D、M1C、Phase2 batch4 无回归。
