# M3G-5-F1 文件管理器搜索 Query 返工收口记录

收口日期：2026-05-29

## 结论

`M3G-5-F1：文件管理器搜索模式仍显示文件夹返工` 正式收口。

- P0：无
- P1：无
- P2：既有 Vite chunk size warning；`.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付项继续排除

## 问题背景

用户在 105 / `projectId=503` 文件管理器中访问：

`/data-steward/assets/503?tab=files&fileKeyword=宝安`

搜索框显示 `宝安`，但右侧仍展示项目根目录文件夹，没有进入项目全局搜索结果。

## 已完成

- 修复搜索模式和目录浏览模式混用问题。
- 有关键词时，右侧表格只渲染文件结果，不再混入目录项。
- 同项目内 URL query 变化会同步驱动文件管理器状态刷新。
- 带 `fileKeyword=宝安` 直接进入页面时，会自动进入项目全局搜索。
- 搜索结果展示“所在位置”和存储状态。
- 清空关键词后恢复当前目录 direct-only 浏览。
- M3G-5 专项脚本补强搜索契约检查，结果为 `PASS=13 FAIL=0`。

## 验收依据

- 开发报告：`handoff/dev-agent/latest-report.md`
- 测试报告：`handoff/test-agent/latest-report.md`
- 专项脚本：`scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

测试 agent 已确认：

- 带 `fileKeyword=宝安` 进入页面会自动进入搜索模式。
- 右侧不再显示根目录文件夹。
- 清空关键词后目录浏览恢复。
- 前端构建、健康检查、`git diff --check` 均通过。

## 边界确认

本批没有做：

- 未执行对象化迁移。
- 未创建对象化迁移任务。
- 未移动、删除、重命名、覆盖真实 NAS 文件。
- 未读取文件正文。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未新增 Hermes 正文问答。
- 未接入真实 BIM 引擎。
- 未修改 `docs/**`。

## 下一步建议

短期建议用户继续在 105 文件管理器中按真实文件名、项目关键词、专业关键词做手工搜索体验验证。

后续路线继续回到 M3G 主线：

1. 继续扩大真实项目对象化覆盖率，但仍保持受控批次。
2. 或启动 M4A，冻结 documents / chunks 语义证据契约。
