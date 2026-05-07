# Windows 开发迁移说明

本文档用于把数字化交付平台从当前 macOS 工作区交接到 Windows 原生开发环境。

注意：当前项目已经升级为三期路线，一期目标是 `内部 BIM 资产管理试点 + NAS 原地接管 + 企业 agent 可检索底座`。Windows 端不能只按旧 v1 MVP 口径继续开发。

## 1. 迁移前必须理解的当前状态

- 平台基础工程、主数据、交付标准、数据管家 MVP、工作中心 MVP、智慧大屏和 3D 适配层 mock 已经形成可运行闭环。
- 最新产品与验收口径见：
  - `docs/07-complete-delivery-prd.md`
  - `docs/08-acceptance-and-agent-integration.md`
- Windows 端 Codex 接手入口见：
  - `handoff/windows-agent/project-context.md`
  - `handoff/windows-agent/current-prompt.md`
  - `handoff/windows-agent/migration-checklist.md`

## 2. 当前源码交接风险

当前 macOS 仓库还没有首个 commit。若没有先提交并推送远端，Windows 端不能假设 `git clone` 能拿到当前源码。

推荐二选一：

1. 先在 macOS 端创建 commit 并推送，再在 Windows 端 clone。
2. 使用过滤后的源码包复制到 Windows。

如果使用源码包复制，不要复制：

- `.claude/`
- `.DS_Store`
- `frontend/node_modules/`
- `frontend/dist/`
- `backend/**/target/`
- `.pnpm-store/`
- `infra/data/`
- `infra/logs/`
- `minio-data/`
- 本地日志、缓存和 IDE 临时目录。

必须保留：

- `.gitattributes`
- `.gitignore`
- `README.md`
- `docs/**`
- `backend/**`
- `frontend/src/**`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `infra/**`
- `scripts/**`
- `handoff/**`

## 3. Windows 环境准备

建议准备：

1. Git for Windows
2. Docker Desktop
3. Node.js 20 LTS 或兼容版本
4. JDK 21
5. PowerShell 5.1+ 或 PowerShell 7

执行：

```powershell
corepack enable
Set-ExecutionPolicy -Scope Process Bypass
```

建议将项目放在英文路径：

```text
D:\dev\zhuoyusmart\digital-delivery-platform
```

## 4. Windows 端 Codex 接手顺序

Windows 端 Codex 打开项目后，先读：

```text
handoff/windows-agent/current-prompt.md
```

再读：

```text
handoff/windows-agent/project-context.md
handoff/windows-agent/migration-checklist.md
docs/07-complete-delivery-prd.md
docs/08-acceptance-and-agent-integration.md
handoff/main-agent/status.md
handoff/main-agent/decisions.md
```

完成迁移复核后，必须写：

```text
handoff/windows-agent/latest-report.md
```

## 5. 启动顺序

在 PowerShell 中进入仓库根目录后：

```powershell
.\scripts\dev\bootstrap-infra.ps1
.\scripts\dev\start-backend.ps1
.\scripts\dev\start-frontend.ps1
```

如遇执行策略限制：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

## 6. 手动构建命令

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

基础设施：

```powershell
cd infra
docker compose --env-file .env.example up -d
```

## 7. 联调检查

后端启动后执行：

```powershell
.\scripts\dev\check-minimal-chain.ps1 http://localhost:8080 platform.admin Admin@123 2
.\scripts\dev\check-master-data-chain.ps1 http://localhost:8080 platform.admin Admin@123 2
.\scripts\dev\check-deliverable-standard-chain.ps1 http://localhost:8080 platform.admin Admin@123 2
.\scripts\dev\check-mvp-chain.ps1 http://localhost:8080 platform.admin Admin@123 2
```

不要并行运行会改变同一项目主数据状态的脚本。

## 8. 需要特别检查的 V7 初稿

当前源码中已经有一期 BIM 资产管理的未完成初稿：

- `backend/delivery-app/src/main/resources/db/migration/V7__init_bim_asset_management.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/AssetImportJobRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/BimAssetRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/StorageRootRepository.java`

这些文件尚未完成应用服务、Controller、前端页面、agent 读模型和验收脚本。Windows 端首先确认它们是否影响构建和 Flyway 启动；若影响，先做最小修复。

## 9. 常见注意事项

- 不要复用 macOS 上的 `node_modules`。
- Windows 首次运行前端时，必须重新执行 `corepack pnpm install`。
- 后端默认数据库地址是 `localhost:3306`。
- `infra/.env.example` 可作为本地默认配置。
- 如果更习惯 Bash，可使用 WSL2；但本仓库已经支持原生 PowerShell。
- Windows 端交接成功后，后续开发 prompt 必须引用 `docs/07` 和 `docs/08`。
