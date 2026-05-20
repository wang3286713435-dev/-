# M1A 登录超时 P1 修复报告

时间：2026-05-20

## 问题

测试 agent 在 M1A 页面验收中发现：

`P1-M1A-LOGIN-TIMEOUT`

表现为 fresh login 后登录按钮进入提交态，但页面不跳转到 `/data-steward/assets`，浏览器显示 `timeout of 10000ms exceeded`。

## 复现与定位

主 agent 复现结果：

- 命令行访问 `POST /api/core/auth/login` 很快。
- 浏览器 fresh login 会卡住。
- 浏览器中经 Vite 代理发出的 `POST /api/core/auth/login` 会等待 10 秒后失败。
- 进一步验证发现：后端运行态对带 `Origin: http://127.0.0.1:5173` 的请求无响应；不带 `Origin` 的命令行请求正常。

判断：

- 不是账号密码问题。
- 不是登录业务逻辑慢。
- 是浏览器请求链路问题，具体集中在本地开发环境中的 `Origin` 请求处理 / Vite 代理 / 后端运行态组合。

## 修复

已做最小修复：

- 将 Vite 本地代理目标从 `http://localhost:8080` 固定为 `http://127.0.0.1:8080`。
- 重启后端运行态后，带 `Origin` 的请求恢复正常。

修改文件：

- `frontend/vite.config.ts`

## 验证

已验证：

- `curl -H 'Origin: http://127.0.0.1:5173' http://127.0.0.1:8080/actuator/health` 返回 200。
- 无痕浏览器 fresh login 成功。
- 登录后跳转到 `http://127.0.0.1:5173/data-steward/assets`。
- 登录链路返回：
  - `POST /api/core/auth/login` 200。
  - `GET /api/core/users/me` 200。
- 整体登录耗时约 1.6 秒。

## 后续复验要求

测试 agent 需做极短复验：

1. fresh login 能进入 `/data-steward/assets`。
2. 105 项目工作台可打开。
3. 506 项目工作台可打开。
4. 工程主数据和工作中心关键页面不回归。
5. 本轮两个路径脱敏 P1 仍通过。
6. G4 / Hermes 仍冻结，没有新增能力。

