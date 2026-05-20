# M1A 平台主线功能基线审计收口报告

时间：2026-05-20

## 收口结论

`M1A：平台主线功能基线审计与交付闭环缺口收束` 已通过测试 agent 复验，可以收口。

当前无 P0 / P1。

保留非阻塞 P2：

- 前端构建仍有既有 Vite chunk size warning。
- `/data-steward/assets` 默认真实项目统计显示为 0，测试 agent 仅记录为非本轮阻塞观察项，建议放入 M1B 可用性收口继续处理。

## 本轮修复

### 1. Catalog 详情路径脱敏

`CatalogApplicationService` 不再因项目管理员权限返回真实底层路径。

catalog-only 文件详情统一返回：

- `storagePath=null`
- `storagePathVisible=false`
- `storagePathVisibilityReason=PATH_NOT_EXPOSABLE_CATALOG_ONLY`

### 2. 旧文件资源列表路径脱敏

`FileResourceApplicationService` 对外响应清空 `storageUri`。

前端 `/data-steward/files` 不再展示真实存储地址，改为显示：

`底层路径已隐藏`

### 3. Fresh login 超时修复

修复 `P1-M1A-LOGIN-TIMEOUT`：

- 将 Vite 本地代理目标从 `http://localhost:8080` 固定为 `http://127.0.0.1:8080`。
- 重启后端运行态后，fresh login 已可正常进入 `/data-steward/assets`。

## 验收结果

测试 agent 复验通过：

- Fresh login 约 2 秒进入 `/data-steward/assets`。
- 503 / 业务编码 105 项目页面抽查通过。
- 506 / 业务编码 93 项目页面抽查通过。
- Catalog 详情路径脱敏通过。
- 旧文件资源接口路径脱敏通过。
- `/data-steward/files` 页面显示 `底层路径已隐藏`。
- 文件访问安全脚本通过，`PASS=18 FAIL=0`。
- 前端构建通过。
- 后端健康检查通过。
- `git diff --check` 通过。

## 主线健康判断

M1A 收口后，主线健康度从：

`黄灯可控`

调整为：

`绿灯可继续主线开发`

注意：这不代表平台已可客户交付，也不代表进入 9A。它只表示主线从 Hermes / G 线偏移中恢复，平台本体可以继续按 M 系列推进。

## 后续建议

下一批建议进入：

`M1B：项目工作台与数据管家可用性收口`

M1B 重点：

- 优化资产总览 Hero 区和项目状态表达。
- 解释各功能入口用途，减少“页面很多但不知道怎么用”的问题。
- 处理真实项目统计显示、项目分类、接入状态和交付状态的可读性。
- 不新增大模块，不进入 Hermes / 8B / 9A。

后续仍禁止：

- 继续 G4。
- 新增 Hermes 能力。
- 进入 8B / 8C / 9A。
- 真实 NAS 增删改查。
- 文件正文抽取。
- 真实 BIM 轻量化。
- Agent 自动治理。

