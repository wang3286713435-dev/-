# Scripts

本目录提供本地开发与联调辅助脚本。

## 入口

- [`scripts/dev/bootstrap-infra.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/bootstrap-infra.sh)
- [`scripts/dev/bootstrap-infra.ps1`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/bootstrap-infra.ps1)
- [`scripts/dev/start-backend.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/start-backend.sh)
- [`scripts/dev/start-backend.ps1`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/start-backend.ps1)
- [`scripts/dev/start-frontend.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/start-frontend.sh)
- [`scripts/dev/start-frontend.ps1`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/start-frontend.ps1)
- [`scripts/dev/check-minimal-chain.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-minimal-chain.sh)
- [`scripts/dev/check-minimal-chain.ps1`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-minimal-chain.ps1)
- [`scripts/dev/check-master-data-chain.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-master-data-chain.sh)
- [`scripts/dev/check-master-data-chain.ps1`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-master-data-chain.ps1)
- [`scripts/dev/check-deliverable-standard-chain.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-deliverable-standard-chain.sh)
- [`scripts/dev/check-deliverable-standard-chain.ps1`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-deliverable-standard-chain.ps1)
- [`scripts/dev/check-mvp-chain.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-mvp-chain.sh)
- [`scripts/dev/check-mvp-chain.ps1`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-mvp-chain.ps1)
- [`scripts/dev/check-agent-db2-contract.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-agent-db2-contract.sh)
- [`scripts/dev/check-asset-quality-overview.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-asset-quality-overview.sh)
- [`scripts/dev/check-phase2-batch1-readonly-catalog.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/check-phase2-batch1-readonly-catalog.sh)

## 本地启动顺序

### 1. 启动基础设施

macOS / Linux:

```bash
bash scripts/dev/bootstrap-infra.sh
```

Windows PowerShell:

```powershell
.\scripts\dev\bootstrap-infra.ps1
```

该脚本会使用 `infra/.env.example` 拉起 MySQL 8、Redis 7 和 MinIO。

### 2. 启动后端

macOS / Linux:

```bash
bash scripts/dev/start-backend.sh
```

Windows PowerShell:

```powershell
.\scripts\dev\start-backend.ps1
```

优先使用仓库内的 `backend/mvnw` / `backend/mvnw.cmd` 构建并启动 `delivery-app`。如果本机缺少 Java 21，但已经安装 Docker Desktop，脚本会回退到 `maven:3.9-eclipse-temurin-21` Docker 镜像，并连接 `infra_default` 网络内的 MySQL。

Hermes 分支默认使用 `SERVER_PORT=18080` 启动后端，避免和平台主线本地 `8080` 冲突。需要改端口时可提前设置 `SERVER_PORT`。

### 3. 启动前端

macOS / Linux:

```bash
bash scripts/dev/start-frontend.sh
```

Windows PowerShell:

```powershell
.\scripts\dev\start-frontend.ps1
```

脚本使用 `corepack pnpm install` 安装依赖，并通过 Vite 在 `5174` 端口启动前端。Hermes 分支默认将 `/api` 代理到 `http://localhost:18080`，可用 `VITE_API_PROXY_TARGET` 覆盖；如需临时改前端端口，可设置 `VITE_FRONTEND_PORT`。

### 4. 执行最小链路检查

macOS / Linux:

```bash
bash scripts/dev/check-minimal-chain.sh http://localhost:8080 admin 123456 2
```

Windows PowerShell:

```powershell
.\scripts\dev\check-minimal-chain.ps1 http://localhost:8080 admin 123456 2
```

该脚本会依次验证登录、刷新 token、当前用户、项目切换和工作中心首页概览接口。

### 5. 执行 master-data 最小链路检查

macOS / Linux:

```bash
bash scripts/dev/check-master-data-chain.sh http://localhost:8080 admin 123456 2
```

Windows PowerShell:

```powershell
.\scripts\dev\check-master-data-chain.ps1 http://localhost:8080 admin 123456 2
```

该脚本会依次验证登录、项目切换、标准前置条件状态、创建部位节点、查询部位树、创建节点类型、锁定节点类型和查询锁定状态。

### 6. 执行交付标准链路检查

macOS / Linux:

```bash
bash scripts/dev/check-deliverable-standard-chain.sh http://localhost:8080 admin 123456 2
```

Windows PowerShell:

```powershell
.\scripts\dev\check-deliverable-standard-chain.ps1 http://localhost:8080 admin 123456 2
```

### 7. 执行 MVP 全链路检查

macOS / Linux:

```bash
bash scripts/dev/check-mvp-chain.sh http://localhost:8080 admin 123456 2
```

Windows PowerShell:

```powershell
.\scripts\dev\check-mvp-chain.ps1 http://localhost:8080 admin 123456 2
```

### 8. 执行企业 Agent DB-2 只读合同检查

macOS / Linux:

```bash
bash scripts/dev/check-agent-db2-contract.sh
```

该脚本会使用 `hermes_agent_ro` 只读账号验证企业 Agent 可接入的四个稳定 View、必要字段、事件游标，以及业务底表不可读。默认只做结构和数量校验，不打印真实项目名、文件名或 NAS 路径。

只读密码默认从 macOS 钥匙串读取：

```text
service: delivery-platform-hermes-agent-ro-local-dev
account: hermes_agent_ro
```

如在临时环境中运行，也可以通过环境变量传入：

```bash
READONLY_PASSWORD='***' bash scripts/dev/check-agent-db2-contract.sh
```

### 9. 执行一期数据质量体检检查

macOS / Linux:

```bash
bash scripts/dev/check-asset-quality-overview.sh http://localhost:8080 admin 123456 2
```

该脚本会验证“数据质量”页面对应的后端接口：真实 NAS 资产体检、项目级体检、菜单入口，以及固定项目的权限隔离。脚本只读取平台元数据，不读取文件正文，也不会修改或删除 NAS 文件。
