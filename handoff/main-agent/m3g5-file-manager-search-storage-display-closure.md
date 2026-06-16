# M3G-5 文件管理器项目全局搜索与存储展示修复收口记录

收口日期：2026-05-28

## 结论

`M3G-5：文件管理器项目全局搜索与存储展示修复` 正式收口。

- P0：无
- P1：无
- P2：既有 Vite chunk size warning；`.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付项继续排除

## 已完成

- 文件管理器关键词搜索默认改为项目全局搜索。
- 保留“仅当前文件夹及子目录”搜索范围，用于需要在当前目录内收窄时使用。
- 未输入关键词时继续保持当前目录 direct-only 浏览，不再铺满深层文件。
- 文件表与详情增加业务化存储状态展示，区分 `OBJECT_STORED` 与 `NAS_ONLY`。
- 文件详情不再直观暴露真实 NAS 路径、bucket、object key 或 `storage_uri`。
- M3G-4 回归脚本支持只读模式，避免本轮测试越界触发对象化迁移。

## 验收依据

- 开发报告：`handoff/dev-agent/latest-report.md`
- 测试报告：`handoff/test-agent/latest-report.md`
- 专项脚本：`scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

测试 agent 已确认：

- M3G-5 专项脚本通过，`PASS=12 FAIL=0`。
- M3G-4 只读回归通过，未触发对象化执行。
- M3G-3 / M3E / file-access 回归通过。
- 后端构建、前端构建、健康检查、`git diff --check` 均通过。
- 未创建迁移任务。
- 未触碰真实 NAS 原文件。
- 禁出字段扫描通过。

## 边界确认

本批没有做：

- 未扩大对象化迁移范围。
- 未移动、删除、重命名、覆盖真实 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未新增 Hermes 正文问答。
- 未接入真实 BIM 引擎。
- 未修改 `docs/**`。

## 下一步建议

短期建议先让用户手工确认 105 文件管理器搜索和存储状态展示是否符合预期。

后续路线二选一：

1. 继续 M3G，按更大但仍受控的批次扩大真实项目对象化覆盖率。
2. 启动 M4A，先冻结 documents / chunks 语义证据契约，为后续 Hermes 受控证据问答做准备。
