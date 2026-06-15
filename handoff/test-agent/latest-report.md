# PLM-1 项目生命周期管理 MVP 轻量验收报告

测试时间：2026-06-15

## 1. 结论

本轮 PLM-1 功能链路通过，但当前不建议主 agent 直接收口。

原因：核心创建 / 归档 / 权限 / 安全边界均通过；但工作区仍存在 1 个被运行期页面引用的未跟踪前端组件，按本轮 prompt 的 P1 规则，应先纳入 Git 后再收口。

## 2. P0 / P1 / P2

P0：无。

P1：

- `frontend/src/modules/master-data/components/MasterDataStepNav.vue` 仍为 `??` 未跟踪文件。
- 该文件已被以下运行期页面引用：
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
- `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
- `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
- 因此不能作为非交付临时文件忽略。若未纳入 Git，其他机器拉取后这些页面会缺组件。

P2：

- `handoff/main-agent/post-ux4-project-lifecycle-todo.md` 为未跟踪文档类交接文件，不影响运行期，但建议主 agent 判断是否纳入或清理。
- 前端构建仍有既有 Vite chunk size warning。
- `git diff --check` 通过，但输出既有 PowerShell 脚本 CRLF 提示。

## 3. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过。
- `corepack pnpm --dir frontend build`：通过，仅既有 Vite chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-plm1-project-lifecycle.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3g8-object-first-read-fallback.sh`：通过，`PASS=7 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。

## 4. PLM-1 创建 / 归档链路结果

专项脚本已验证：

- 超级管理员登录成功。
- 创建唯一临时项目成功。
- 新项目出现在默认项目启动台列表。
- 创建人获得新项目 `PROJECT_ADMIN`。
- 工程树根节点存在且未重复。
- 对象存储工作区占位记录存在。
- `confirmed=false` 归档被拒绝。
- 确认文本错误归档被拒绝。
- 确认文本正确后项目软归档成功。
- 归档响应声明 `objectStorageDeleted=false`、`nasTouched=false`。
- 归档项目默认不再出现在项目启动台列表。
- 归档后项目为软归档，存储占位未删除，审计记录存在。

补充 API 负向验证：

- 使用 `delivery.engineer / Engineer@123` 调用项目生命周期创建，被拒绝，返回 `ASSET_PROJECT_LIFECYCLE_FORBIDDEN`。
- 使用同一普通用户归档管理员创建的临时项目，被拒绝，返回 `ASSET_PROJECT_LIFECYCLE_FORBIDDEN`。
- 使用 `admin / 123456` 对该临时项目完成软归档，避免遗留可见测试项目。
- 补充验证响应未命中禁出字段。

说明：

- `platform.admin / Admin@123` 在当前环境返回用户名或密码错误，因此普通用户负向验证改用当前可登录的 `delivery.engineer`。

## 5. 前端轻验结果

页面：`http://127.0.0.1:5173/data-steward/assets`

超级管理员视角：

- 页面可打开，不白屏。
- 页面级横向溢出为 0。
- 可见 `超级管理员 admin`。
- 可见并可用 `新建项目` 入口。
- 项目行可见 `归档` 入口。
- 页面文本未发现禁出字段。

普通用户权限：

- 已通过 API 负向验证证明普通用户不能创建 / 归档全局项目。
- 本轮未在浏览器里切换普通用户逐页点击，因为 prompt 要求保持轻量，且权限边界已由接口直接验证。

## 6. 禁出字段扫描结果

PLM-1 专项脚本与补充 API 负向验证均未发现以下字段泄露：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- `bucket`
- `object_key`
- `objectKey`
- SQL
- raw row
- token
- secret
- password

浏览器资产总览页文本同样未发现上述禁出字段。

## 7. 安全边界确认

未发现：

- 创建项目导致真实 NAS 文件移动 / 删除 / 重命名。
- 归档项目物理删除业务数据。
- 归档项目删除 MinIO 对象。
- 普通用户可创建或归档全局项目。
- API 或前端泄露 token / secret / 真实存储路径。
- 主链路白屏。

回归脚本确认：

- M3G-8 对象优先读取链路未回归。
- Phase2 batch4 受控文件访问链路未回归。

## 8. Git 范围检查

PLM-1 专项脚本 `scripts/dev/check-plm1-project-lifecycle.sh` 已纳入 Git，状态为 `A`。

但仍有运行期未跟踪文件：

- `frontend/src/modules/master-data/components/MasterDataStepNav.vue`

另有非运行期未跟踪交接文件：

- `handoff/main-agent/post-ux4-project-lifecycle-todo.md`

当前工作区还包含大量前序批次改动，本轮测试未回退、未整理这些改动。

## 9. 是否建议收口

暂不建议主 agent 收口 PLM-1。

建议先处理 P1：

- 将 `frontend/src/modules/master-data/components/MasterDataStepNav.vue` 纳入 Git 跟踪 / 暂存，或确认它不属于本次交付并移除所有运行期引用。

处理后建议极短复验：

- `git status --short frontend/src/modules/master-data/components/MasterDataStepNav.vue`
- `corepack pnpm --dir frontend build`
- `git diff --check`

若该文件不再是 `??` 且构建仍通过，可建议主 agent 进入 PLM-1 收口判断。
