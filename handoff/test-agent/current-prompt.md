# 测试 Agent 当前任务：M3G-5-F1 文件管理器搜索返工极短验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收 `M3G-5-F1：文件管理器搜索仍显示文件夹` 的返工修复。

## 1. 必读

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/current-prompt.md`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

## 2. 必测问题

用户反馈：

`/data-steward/assets/503?tab=files&fileKeyword=宝安`

搜索框显示 `宝安`，但右侧仍显示根目录文件夹。

正确结果：

- 页面应进入搜索模式。
- 右侧应展示匹配文件。
- 右侧不应继续展示当前目录文件夹。
- 搜索结果应显示所在位置。

## 3. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g5-file-manager-search-storage-display.sh
git diff --check
```

## 4. 浏览器极短验收

打开：

`http://127.0.0.1:5173/data-steward/assets/503?tab=files&fileKeyword=%E5%AE%9D%E5%AE%89`

检查：

1. 搜索框显示 `宝安`。
2. 页面出现“整个项目中搜索”或等价业务提示。
3. 右侧表格不显示 `文件夹` 行。
4. 右侧表格如有结果，必须是文件，并显示所在位置。
5. 清空关键词后，根目录文件夹恢复显示。
6. `OBJECT_STORED` / `NAS_ONLY` 存储展示不回归。

## 5. 红线

测试过程中不得触发：

- 对象化迁移执行。
- 真实 NAS 移动、删除、重命名、覆盖。
- Hermes 正文问答。
- 文件正文读取。
- 语义索引写入。

## 6. 判定

P0：

- 页面白屏。
- 项目上下文丢失。
- 真实 NAS 文件被触碰。

P1：

- 带 `fileKeyword` 进入页面仍展示文件夹。
- 搜索模式没有自动生效。
- 清空关键词后目录浏览无法恢复。

P2：

- 文案或视觉细节粗糙，但不影响搜索链路。

报告写入：

`handoff/test-agent/latest-report.md`
