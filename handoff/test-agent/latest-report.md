# M2E P1 selectedDraftItemIds 契约极短复验报告

生成时间：2026-05-22 23:20 CST

## 1. 复验结论

结论：通过。

上一轮 P1 `selectedDraftItemIds 未生效` 已修复。后端现在会校验并使用 `selectedDraftItemIds`：

- 空选择被拒绝。
- 不存在或失效的草案项 ID 被拒绝。
- 合法小选择只处理所选草案项及必要依赖，不再全量返回 40 项。
- 依赖项通过 `DEPENDENCY+...` 标识，没有伪装成用户直接选择。

当前未发现 P0 / P1。本轮仅记录 1 个非阻塞 P2：前端构建仍有既有 Vite chunk size warning。

建议主 agent 收口 M2E。

## 2. 必读文件

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/status.md`

开发报告与主 agent 状态均说明本轮只修复 M2E P1，不扩展 Hermes / BIM / parser / indexing / NAS 写能力。

## 3. 必跑命令结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- M2E 专项脚本：`bash scripts/dev/check-m2e-real-project-masterdata-confirmation.sh`，通过，`PASS=11 FAIL=0`。
- 空白字符检查：`git diff --check`，通过。

## 4. 三类契约复验结果

### 空选择

请求：`selectedDraftItemIds=[]`

结果：

- `code=REAL_PROJECT_DRAFT_SELECTION_REQUIRED`
- `message=请至少选择一个草案项`
- 返回 `traceId`
- 未生成 `generatedItems`
- 未发现 forbidden fields

结论：通过。

### 非法选择

请求：`selectedDraftItemIds=["NOT_A_REAL_DRAFT_ITEM_ID"]`

结果：

- `code=REAL_PROJECT_DRAFT_SELECTION_INVALID`
- `message=选择的草案项不存在或已失效`
- 返回 `traceId`
- 未生成 `generatedItems`
- 未发现 forbidden fields

结论：通过。

### 合法小选择

从 preview 中取合法草案项：

- `DISCIPLINE_CANDIDATE:建筑:CATALOG_DISCIPLINE_DISTRIBUTION`

结果：

- `code=OK`
- 返回 `traceId`
- `generatedItemsCount=2`
- `createdTotal=0`
- `skippedTotal=2`
- 依赖来源包含 `DEPENDENCY+MANUAL_REVIEW`
- 未再全量返回 40 项
- 未发现 forbidden fields

结论：通过。

## 5. 页面短验结果

按本轮极短复验范围，只打开：

- `/data-steward/assets/503/master-data/initialization`

结果：

- 页面可打开，非白屏，非 404。
- 已确认状态文案显示：`M2E 工程主数据人工确认`、`工程主数据已生成，仍需人工复核和维护`。
- 未再看到误导性的 `真实资产已接入，工程主数据未确认`。
- 页面可见 `查看节点类型` 入口。
- 页面可见真实项目数据：目录文件 `2,928`、模型 `198`、图纸 `2,729`、文档 `242`。
- 页面可见不读取正文、不解析 BIM 构件、不访问或复制 NAS 文件的说明。
- 页面可见文本未发现 raw path 或敏感字段泄露。

结论：通过。

## 6. 红线检查

接口响应、页面文本和静态扫描未发现：

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

静态扫描命中仅出现在：

- 专项脚本 forbidden-field 断言列表。
- 登录测试参数。
- 脱敏 / forbidden 判断函数。
- 说明“前端和 Hermes 不返回 NAS 原始路径”的文案。

未发现：

- 真实 NAS 写入、移动、删除、复制。
- 文件正文读取。
- Hermes / BIM / parser / indexing 新能力。
- 自动挂接、自动审核、自动整改。

## 7. P0 / P1 / P2

P0：无。

P1：无。上一轮 `selectedDraftItemIds 未生效` 已修复。

P2：

- 既有 Vite chunk size warning，未阻塞构建和本轮复验。

## 8. 是否建议主 agent 收口 M2E

建议主 agent 收口 M2E。

理由：本轮 P1 的空选、非法选、合法小选三类契约均已通过；专项脚本、构建、健康检查、`git diff --check` 均通过；页面已确认状态文案和 `查看节点类型` 入口均到位；未发现真实 NAS / 正文读取 / Hermes / BIM / parser / indexing 越界行为。
