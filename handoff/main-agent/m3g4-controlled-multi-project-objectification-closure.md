# M3G-4 受控多项目小批对象化执行收口记录

收口时间：2026-05-28

## 结论

`M3G-4：受控多项目小批对象化执行` 正式收口。

当前 P0：无。

当前 P1：无。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。

## 本批完成内容

- 新增受控执行接口：
  - `POST /api/data-steward/storage-objectification-plans:execute`
- 增加后端硬上限：
  - 单次最多 `3` 个真实项目。
  - 单项目最多 `3` 个文件。
  - 总文件数最多 `9` 个。
  - 单项目容量最多 `50MB`。
  - 总容量最多 `100MB`。
  - 必须 `confirmed=true`。
- 文件服务页新增“确认执行小批对象化”入口和执行结果摘要。
- 新增专项脚本：
  - `scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`

## 真实执行摘要

开发 agent 自测执行：

- 真实项目数：`3`
- 文件数：`3`
- 总容量：`7,711,565 bytes`
- 首次执行：`3` 个文件进入 `OBJECT_STORED`
- 重复执行：同一批 `3` 个文件幂等跳过

测试 agent 正式验收再次执行：

- 真实项目数：`3`
- 文件数：`3`
- 总容量：`4,166,710 bytes`
- 首次执行：`created=3 / skipped=0 / failed=0`
- 重复执行：同一批文件幂等跳过

说明：测试 agent 执行的样本与开发 agent 自测样本不同，因为脚本会从当前真实项目 `NAS_ONLY` 小样本中选择下一批最多 `3` 个文件。这符合 M3G-4 “允许真实小批对象化”的验收范围。

## 验收依据

开发报告：

- `handoff/dev-agent/latest-report.md`

测试报告：

- `handoff/test-agent/latest-report.md`

专项与回归：

- M3G-4 专项脚本通过，`PASS=21 FAIL=0`。
- M3G-3 多项目对象化规划回归通过。
- M3G-1 readiness / inventory / dry-run 回归通过。
- M3F 新文件对象存储优先写入回归通过。
- M3E 预览与转换产物对象化回归通过。
- M3C 对象存储迁移任务中心回归通过。
- file-access 文件访问安全回归通过。
- 后端构建、前端构建、健康检查、`git diff --check` 均通过。

## 边界确认

本批已确认：

- `confirmed=false` 被拒绝。
- 超限执行被拒绝。
- 小批真实对象化执行成功。
- 重复执行幂等，不污染 active object version。
- 已对象化文件显示 `OBJECT_STORED`，并可通过受控 `file-access` 读取。
- NAS 原文件仍存在，且 `size/mtime` 未变化。
- 响应未泄露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。

本批明确没有做：

- 没有全量迁移所有项目。
- 没有迁移 NAS 根目录。
- 没有移动、删除、重命名、覆盖真实 NAS 文件。
- 没有读取 PDF / Office / DWG / RVT / IFC 正文。
- 没有写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 没有新增 Hermes 正文问答、BIM 引擎、parser 或 indexing 能力。

## 当前裁决

M3G-4 已证明平台具备“受控多项目小批对象化执行”能力。

它仍不代表：

- 105 项目已经全量对象化。
- 所有真实项目已经对象化。
- 平台已经具备正文语义理解。
- Hermes 已经可以进行正文证据问答。

下一步建议先处理用户已发现的文件管理器体验问题：

1. 文件管理器搜索默认改为项目全局搜索。
2. 用户界面不再直观暴露真实 NAS 路径，改为展示项目内路径、存储状态和访问来源。
