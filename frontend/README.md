# Frontend

数字化交付平台一期前端工程，采用 Vue 3 + TypeScript + Vite + Element Plus + Pinia。

本子项目默认使用前端 `5174` 独立运行，BIM报建先走前端 Mock 数据闭环，不要求连接后端。保留 `5173`、`8080` 和主线库 `delivery_platform` 给原卓羽智能数据中台主线环境；后续需要联调时再显式启用 C塔后端 `18080` 和开发库 `delivery_platform_ctower`。

## 页面骨架

- 登录页
- 主框架布局
- 顶部项目切换
- 左侧菜单
- 首页占位页
- BIM报建闭环

## 本地运行

优先使用 [`scripts/dev/start-frontend.sh`](../scripts/dev/start-frontend.sh)。

Windows PowerShell 优先使用 [`scripts/dev/start-frontend.ps1`](../scripts/dev/start-frontend.ps1)。

手动命令：

```bash
cd frontend
corepack pnpm install
VITE_DEV_PORT=5174 corepack pnpm dev --host 0.0.0.0 --port 5174
```

Windows 手动命令：

```powershell
cd frontend
corepack pnpm install
$env:VITE_DEV_PORT = "5174"
corepack pnpm dev --host 0.0.0.0 --port 5174
```

如后续进入平台后端联调，再显式启用：

```bash
VITE_C_TOWER_BACKEND_ENABLED=true VITE_API_TARGET=http://127.0.0.1:18080 corepack pnpm dev --host 0.0.0.0 --port 5174
```
