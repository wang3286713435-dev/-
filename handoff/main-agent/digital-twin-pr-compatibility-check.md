# 数字孪生 PR 并入兼容性检查报告

检查时间：2026-05-23

## 1. 合并状态

- 当前分支：`main`
- 当前本地 `HEAD`：`a5f7064 feat: add digital twin collaboration dashboard (#1)`
- 当前 `origin/main`：`a5f7064 feat: add digital twin collaboration dashboard (#1)`
- 结论：数字孪生 PR 已并入主线，本地 main 与远端 main 一致。

## 2. PR 主要变更范围

本次合并主要包含：

- 后端：
  - `delivery-visualization-adapter` 新增数字孪生驾驶舱聚合能力。
  - 新增 `/api/visualization-adapter/projects/{projectId}/digital-twin-dashboard`。
  - 菜单中新增 / 调整数字孪生相关入口。
  - CORS 增加 `5188` 本地预览端口。
- 前端：
  - 新增 `/digital-twin` 路由。
  - 新增数字孪生门户页和 BIM 协同驾驶舱页面。
  - 引入 React / Three / React Three Fiber / Drei 作为前端局部 3D 展示依赖。
  - `work/dashboard` 和 `visualization/workbench` 复用数字孪生驾驶舱页面。

## 3. 已执行兼容性检查

已执行：

```text
git fetch origin
git status --short --branch
./mvnw -pl delivery-app -am -DskipTests package
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2f-real-project-delivery-loop.sh
bash scripts/dev/check-m2e-real-project-masterdata-confirmation.sh
bash scripts/dev/check-m2c-delivery-package-archive.sh
bash scripts/dev/check-m2b-nas-write-trial.sh
git diff --check
```

结果：

- 后端构建：通过。
- 前端构建：通过。
- 健康检查：通过。
- M2F 回归：`PASS=6 FAIL=0`。
- M2E 回归：`PASS=11 FAIL=0`。
- M2C 回归：`PASS=11 FAIL=0`。
- M2B 回归：`PASS=18 FAIL=0`。
- `git diff --check`：通过。

## 4. 数字孪生接口抽查

已用 `platform.admin` 登录并切换到 `503 / 105` 后调用：

```text
GET /api/visualization-adapter/projects/503/digital-twin-dashboard
```

结果：

- 返回 `code=OK`。
- 返回 `traceId`。
- 返回项目、资产、交付、质量、模型、活动和安全边界聚合数据。
- 105 当前数字孪生聚合结果可读：
  - 文件数：2928。
  - 模型文件：198。
  - 图纸文件：2729。
  - 交付应交项：44。
  - 当前缺失项：44。

未登录访问同接口：

- 返回 `401`。
- 错误码：`CORE_AUTH_UNAUTHORIZED`。

## 5. 安全与边界检查

数字孪生接口响应未发现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `storagePath`
- `storageUri`
- raw row
- SQL
- token
- secret
- password

本次兼容检查未发现：

- 真实 NAS 文件写入、移动、删除、复制、改名。
- PDF / Office / DWG / RVT / IFC 正文读取。
- Hermes 新能力。
- parser / writer / indexing / 向量库新能力。
- 自动挂接、自动审核、自动整改。

## 6. 兼容性结论

结论：可以视为已兼容并入。

数字孪生 PR 当前没有打断：

- M2F 真实项目交付闭环。
- M2E 工程主数据人工确认。
- M2C 交付包 / 档案目录。
- M2B 受控 NAS 写入灰度。

## 7. 非阻塞风险

P2：

- 前端构建仍有既有 chunk size warning。
- 数字孪生新增 React / Three 依赖后，`DigitalTwinDashboardPage` 和主 `index` chunk 明显变大。
- 该问题不阻塞当前进入下一批次，但后续如果数字孪生继续增强，建议单独做前端 code splitting / manualChunks 优化。

## 8. 下一步建议

可以进入下一批次，但建议遵守以下边界：

- `M2G：真实 NAS 文件管理器灰度完善` 可以继续推进。
- 数字孪生暂时作为已并入能力保留，不要在 M2G 中继续扩展。
- 真实 BIM 引擎、构件级解析、文件正文读取仍然不进入当前批次。
