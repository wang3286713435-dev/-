# Frontend

数字化交付平台一期前端工程，采用 Vue 3 + TypeScript + Vite + Element Plus + Pinia。

## 页面骨架

- 登录页
- 主框架布局
- 顶部项目切换
- 左侧菜单
- 首页占位页

## 本地运行

优先使用 [`scripts/dev/start-frontend.sh`](/Users/vc/Documents/数字化交付平台/scripts/dev/start-frontend.sh)。

Windows PowerShell 优先使用 [`scripts/dev/start-frontend.ps1`](/Users/vc/Documents/数字化交付平台/scripts/dev/start-frontend.ps1)。

手动命令：

```bash
cd frontend
corepack pnpm install
corepack pnpm dev --host 0.0.0.0
```

Windows 手动命令：

```powershell
cd frontend
corepack pnpm install
corepack pnpm dev --host 0.0.0.0
```
