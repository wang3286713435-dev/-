# Windows 端 Codex 当前任务：接收项目并完成上下文一致性复核

你是 Windows 端的长期 Codex 会话，负责接收卓羽智能数据中台源码与文档，并确认 Windows 端后续开发上下文与 macOS 端主 agent 保持一致。

本轮不要继续开发新业务功能。你的任务是迁移复核、环境验证、上下文校准和报告。

## 0. 必须先读

按顺序阅读：

1. `handoff/windows-agent/project-context.md`
2. `handoff/windows-agent/migration-checklist.md`
3. `handoff/main-agent/status.md`
4. `handoff/main-agent/decisions.md`
5. `docs/README.md`
6. `docs/07-complete-delivery-prd.md`
7. `docs/08-acceptance-and-agent-integration.md`
8. `docs/09-windows-dev-migration.md`
9. `handoff/dev-agent/latest-report.md`
10. `handoff/test-agent/latest-report.md`

如果没有读完这些文件，不要开始构建或修改。

## 1. 本轮目标

完成以下事项：

1. 确认项目源码和文档完整接收。
2. 确认最新三期路线、一期 BIM 资产试点、企业 agent 对接原则已经被 Windows 端 Codex 理解。
3. 检查 Windows 原生环境是否具备开发条件。
4. 构建前端和后端。
5. 启动 MySQL、Redis、MinIO。
6. 启动后端和前端。
7. 执行现有 PowerShell 验收脚本。
8. 核查当前 `V7` BIM 资产初稿是否影响构建、启动和迁移。
9. 把完整结果写入 `handoff/windows-agent/latest-report.md`。

## 2. 协作规则

- 不要创建子 agent。
- 默认先检查、运行、记录，不要主动扩展功能。
- 不要回退或删除已有代码、文档和迁移。
- 不要修改 `docs/07-complete-delivery-prd.md`、`docs/08-acceptance-and-agent-integration.md`、`handoff/main-agent/**`，除非主 agent 明确要求。
- 如果必须修复 Windows 兼容问题，优先限制在：
  - `scripts/**`
  - `README.md`
  - `docs/09-windows-dev-migration.md`
  - `handoff/windows-agent/**`
- 如果发现 `V7` 资产初稿导致无法构建或启动，可以最小范围修复后继续验证，但必须记录。

## 3. 源码接收要求

当前 macOS 仓库没有首个 commit。不要假设 `git clone` 一定包含最新源码。

接收方式必须在报告中写明：

- `git clone + 已确认远端包含当前源码`
- 或 `过滤后的源码包复制`
- 或其他方式

如果通过源码包复制，必须确认未复制：

- `frontend/node_modules`
- `frontend/dist`
- `backend/**/target`
- `.pnpm-store`
- `.DS_Store`
- 本地日志、缓存、IDE 临时目录

推荐 Windows 路径：

```text
D:\dev\zhuoyusmart\digital-delivery-platform
```

尽量避免中文路径，减少 Maven、Node、Docker 和脚本兼容问题。

## 4. 环境检查

记录以下命令输出：

```powershell
git --version
docker --version
java -version
node -v
corepack --version
$PSVersionTable.PSVersion
```

建议：

- Docker Desktop 已启动。
- Node.js 使用 20 LTS 或兼容版本。
- Java 使用 JDK 21。
- PowerShell 使用 5.1+ 或 PowerShell 7。

## 5. 文件完整性检查

必须确认存在：

- `.gitattributes`
- `.gitignore`
- `README.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/09-windows-dev-migration.md`
- `handoff/windows-agent/project-context.md`
- `backend/mvnw.cmd`
- `backend/.mvn/wrapper/maven-wrapper.properties`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `infra/docker-compose.yml`
- `scripts/dev/bootstrap-infra.ps1`
- `scripts/dev/start-backend.ps1`
- `scripts/dev/start-frontend.ps1`
- `scripts/dev/check-minimal-chain.ps1`
- `scripts/dev/check-master-data-chain.ps1`
- `scripts/dev/check-deliverable-standard-chain.ps1`
- `scripts/dev/check-mvp-chain.ps1`

必须特别确认存在但尚未完成的资产初稿：

- `backend/delivery-app/src/main/resources/db/migration/V7__init_bim_asset_management.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/BimAssetRepository.java`

## 6. 构建检查

执行策略：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

前端：

```powershell
cd frontend
corepack pnpm install
corepack pnpm build
```

后端：

```powershell
cd backend
.\mvnw.cmd -pl delivery-app -am -DskipTests package
```

如果本机 Java 不是 21，可以记录失败原因后使用 Docker Maven 作为备选，但报告必须写清楚。

## 7. 启动与链路检查

回到仓库根目录：

```powershell
.\scripts\dev\bootstrap-infra.ps1
```

启动后端：

```powershell
.\scripts\dev\start-backend.ps1
```

确认：

```powershell
curl.exe -fsS http://localhost:8080/actuator/health
```

启动前端：

```powershell
.\scripts\dev\start-frontend.ps1
```

链路脚本：

```powershell
.\scripts\dev\check-minimal-chain.ps1 http://localhost:8080 platform.admin Admin@123 2
.\scripts\dev\check-master-data-chain.ps1 http://localhost:8080 platform.admin Admin@123 2
.\scripts\dev\check-deliverable-standard-chain.ps1 http://localhost:8080 platform.admin Admin@123 2
.\scripts\dev\check-mvp-chain.ps1 http://localhost:8080 platform.admin Admin@123 2
```

注意：这些脚本会写入冒烟数据，执行时不要并行运行会改变同一项目标准状态的脚本。

## 8. 成功标准

满足以下条件，即可判断 Windows 交接成功：

1. 源码和文档完整。
2. Windows 端 Codex 已读并理解 `docs/07`、`docs/08` 和 `project-context.md`。
3. 前端 `pnpm build` 通过。
4. 后端 `mvnw.cmd package` 通过。
5. Docker 基础设施可启动。
6. 后端健康检查通过。
7. 前端可启动。
8. `check-minimal-chain.ps1` 通过。
9. `check-mvp-chain.ps1` 尽量通过；如果失败，需要明确是否是环境问题、脚本问题还是当前 `V7` 未完成导致。

## 9. 报告要求

完成后写入：

```text
handoff/windows-agent/latest-report.md
```

报告必须包含：

1. 接收路径和接收方式。
2. 是否确认当前源码与 macOS 端一致。
3. 是否确认最新三期路线和企业 agent 对接要求。
4. 工具环境版本。
5. 关键文件检查结果。
6. 构建命令和结果。
7. 启动命令和结果。
8. 链路脚本结果。
9. `V7` 资产初稿状态。
10. Windows 端是否可继续开发。
11. 若不可继续，阻塞原因和建议修复顺序。
