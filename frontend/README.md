# Frontend

卓羽智能数据中台一期前端工程，采用 Vue 3 + TypeScript + Vite + Element Plus + Pinia。

## 页面骨架

- 登录页
- 主框架布局
- 顶部项目切换
- 左侧菜单
- 首页占位页

## 本地运行

优先使用 [`scripts/dev/start-frontend.sh`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/start-frontend.sh)。

Windows PowerShell 优先使用 [`scripts/dev/start-frontend.ps1`](/Users/vc/Documents/数字化交付平台-hermes/scripts/dev/start-frontend.ps1)。

Hermes 分支默认在 `5174` 端口启动，并将 `/api` 代理到 `http://localhost:18080`，用于隔离平台主线前端和后端。需要改目标时设置：

```bash
VITE_API_PROXY_TARGET=http://localhost:18080 bash scripts/dev/start-frontend.sh
```

手动命令：

```bash
cd frontend
corepack pnpm install
VITE_FRONTEND_PORT=5174 corepack pnpm dev --host 0.0.0.0 --port 5174 --strictPort
```

Windows 手动命令：

```powershell
cd frontend
corepack pnpm install
$env:VITE_FRONTEND_PORT = "5174"
corepack pnpm dev --host 0.0.0.0 --port 5174 --strictPort
```
