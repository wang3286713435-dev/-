# M3G-5-F1 文件管理器搜索返工极短验收报告

生成时间：2026-05-28 18:44:14 CST

## 1. 测试结论

结论：M3G-5-F1 极短验收通过，建议主 agent 补充 M3G-5-F1 收口记录。

本轮只验收 `M3G-5-F1：文件管理器搜索仍显示文件夹` 返工修复。重点验证用户反馈路径：

`/data-steward/assets/503?tab=files&fileKeyword=宝安`

验收结果：

- 页面可正常打开，不白屏，项目上下文未丢失。
- 搜索框自动显示 `宝安`。
- 页面自动进入“整个项目中搜索”模式。
- 右侧表格不再显示根目录文件夹行。
- 搜索结果为匹配文件，并显示“所在位置”。
- 手动清空关键词后，URL 回到 `?tab=files`，根目录文件夹恢复显示。
- `OBJECT_STORED` / `NAS_ONLY` 存储展示不回归。
- 未触发对象化迁移，未触碰真实 NAS 文件。

## 2. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮验收。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项仍存在，未纳入本轮阻塞判断。

## 3. 必读文件检查

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/current-prompt.md`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

## 4. 必跑命令结果

- `corepack pnpm --dir frontend build`：通过，仅有既有 chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-m3g5-file-manager-search-storage-display.sh`：通过，`PASS=13 FAIL=0`。
- `git diff --check`：通过。

## 5. 浏览器极短验收

打开：

`http://127.0.0.1:5173/data-steward/assets/503?tab=files&fileKeyword=%E5%AE%9D%E5%AE%89`

结果：

- 搜索框显示 `宝安`：通过。
- 页面出现“正在整个项目中搜索，结果会显示所在位置；不会暴露真实 NAS 路径。”：通过。
- 右侧表格不显示 `00_工作进度`、`01_文件收发`、`02_项目资源`、`03_过程文件` 等文件夹行：通过。
- 右侧表格显示匹配文件，例如 `宝安_启航华居_1栋_AR_A1户型.rvt`、`宝安_启航华居_地下室 - B3机房出图.dwg`：通过。
- 搜索结果显示“所在位置”列与项目内相对路径：通过。
- 表格可见存储状态，包含 `已对象化 / NAS 侧 MinIO` 与 `历史 NAS / 尚未对象化`：通过。

清空关键词复核：

- 在搜索框中全选删除 `宝安` 后点击查询。
- URL 回到 `http://127.0.0.1:5173/data-steward/assets/503?tab=files`。
- “整个项目中搜索”提示消失。
- 根目录文件夹恢复显示。

结论：上一轮 P1“带 fileKeyword 进入页面仍展示文件夹”已关闭。

## 6. 红线检查

本轮未触发：

- 对象化迁移执行。
- 真实 NAS 移动、删除、重命名、覆盖。
- Hermes 正文问答。
- 文件正文读取。
- 语义索引写入。

M3G-5 专项脚本同时确认：

- 未创建对象化迁移任务。
- NAS 原文件 `size/mtime` 未变化。
- 接口响应未泄露 raw NAS path、`storage_uri`、bucket、object key、SQL、raw row、token、secret。

## 7. 是否建议收口

建议主 agent 补充 M3G-5-F1 收口记录，并可将本次返工与 M3G-5 一起提交推送。

理由：

- 用户反馈路径已实测修复。
- 专项脚本通过且覆盖前端搜索模式契约。
- 清空关键词后目录浏览恢复。
- 未发现 P0 / P1。
