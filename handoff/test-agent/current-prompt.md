# 测试 Agent 当前任务：UX4-B-R3 前端体验修复验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮验收开发 agent 最新报告中的 `UX4-B-R3`。

重点不是重新全量设计 UX4，而是确认这次三类修复是否真的可用，并且没有破坏 UX4-A-R1 主工作台结构。

## 0. 本轮验收重点

开发 agent 报告声称本轮修复了：

1. 项目启动台“项目总体状态”圆环内数字与说明文字重叠。
2. 任意项目进入工程主数据页后，卡片 / 表格横向无限延长。
3. 项目工作台 BIM 协同区域仍使用静态占位，没有读取葛兰岱尔轻量化 Viewer 状态。

同时保留：

- 项目启动台作为平台级父入口，不绑定具体项目。
- BIM 协同作为平台级父入口，不自动绑定当前项目。
- 文件详情面板不侵占文件列表，可调整大小。
- UX4-A-R1 的项目工作台结构不回退。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/development-log.md`

## 2. 必跑命令

执行并记录：

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 3. Git 与文件跟踪检查

必须检查：

```bash
git status --short
git diff --name-status
git ls-files --others --exclude-standard
```

重点确认：

- 新增 `frontend/src/modules/core/components/workspace/*.vue` 如被代码引用，不能保持 `??`。
- 新增 `frontend/src/assets/ux4/project-cover-reference.png` 如被代码引用，不能保持 `??`。
- 不得修改 `backend/**`。
- 不得新增数据库迁移。
- 不得修改 `docs/**`。

如果运行期需要的前端组件或图片仍是未跟踪文件，判 P1。

## 4. 浏览器页面检查

确认 5173 来自当前项目目录：

`/Users/vc/Documents/数字化交付平台/frontend`

至少检查：

1. `http://127.0.0.1:5173/data-steward/assets`
2. `http://127.0.0.1:5173/data-steward/assets/503`
3. `http://127.0.0.1:5173/data-steward/assets/503?tab=files`
4. `http://127.0.0.1:5173/data-steward/assets/503?tab=master-data`
5. `http://127.0.0.1:5173/data-steward/assets/503?tab=delivery`
6. `http://127.0.0.1:5173/data-steward/assets/503?tab=bim`
7. `http://127.0.0.1:5173/bim-collaboration?projectId=503`

## 5. 专项验收点

### A. 项目启动台状态圆环

检查 `/data-steward/assets`：

- “项目总体状态”或同类状态圆环内数字与说明文字不得重叠。
- 项目数量变大时，数字和说明仍居中、可读。
- 入口仍然聚焦项目选择，不回退成旧资产列表。

### B. 工程主数据横向撑宽

检查：

- `/data-steward/assets/503?tab=master-data`
- 至少再抽一个非 503 项目，如 `/data-steward/assets/505?tab=master-data`

要求：

- 页面级 `document.documentElement.scrollWidth - window.innerWidth` 应接近 0。
- 工程树、表格、节点属性不能把整页无限向右撑开。
- 宽表格只能在局部容器内处理，不影响整个项目工作台。

### C. 葛兰岱尔 Viewer 状态

检查 `/data-steward/assets/503?tab=bim`：

- 页面必须调用现有葛兰岱尔模型清单接口或展示其真实结果。
- 如果当前清单 `count=0`，页面必须显示清楚的空状态：
  - “暂无可嵌入轻量化 Viewer”
  - 或等价说明。
- 不得显示假的模型示意图冒充可用 Viewer。
- “打开独立 Viewer”在没有 `activeGlandarModel/latestJobId` 时应禁用或给出明确提示。
- 如果环境存在可用葛兰岱尔模型，则 Viewer iframe 应正常嵌入，不暴露 token。

### D. 文件详情浮窗

检查 `/data-steward/assets/503?tab=files`：

- 文件详情面板不应压缩或遮挡主文件列表到不可用。
- 如支持调整大小，调整过程中页面不应横向撑爆。
- 关闭后文件列表应恢复正常。

### E. UX4-A-R1 结构不回退

确认：

- 项目概览仍是项目工作台，不是旧页面加壳。
- 文件管理仍有左目录树 / 中间文件区 / 右详情或浮窗详情。
- 工程主数据仍有流程、完成度、阻塞项、工程树语义。
- 交付闭环仍有交付状态、待交付、审核进度和风险提醒。
- BIM 协同仍有 KPI、Viewer 状态、模型列表、轻量化状态。

## 6. 旧链接兼容

检查旧链接不白屏、不丢项目上下文：

- `/master-data/sections`
- `/work/document-delivery`
- `/work/drawing-delivery`
- `/data-steward/models`
- `/data-steward/objects`
- `/bim-collaboration?projectId=503`

## 7. 多分辨率

至少检查：

- 1280
- 1440
- 1920

不得出现：

- 页面级横向溢出
- 主按钮不可见
- 右侧详情面板遮挡主体
- 文字严重挤压
- 表格撑爆整页

## 8. 安全与越界

不得泄露：

- 真实 NAS 路径
- `/Volumes`
- `smb://`
- `nas://`
- bucket
- object key
- `storage_uri`
- SQL
- raw row
- token / secret

不得发生：

- 修改后端
- 修改数据库迁移
- 修改 `docs/**`
- 触碰真实 NAS 文件
- 读取文件正文
- 新增 Hermes evidence
- 新增 BIM 后端能力
- 写 parser / documents / chunks / Qdrant / OpenSearch

## 9. P0 / P1 / P2

P0：

- 任一主路径白屏。
- 修改 `backend/**` / DB / `docs/**` / 权限 / 对象存储 / Hermes / BIM 后端能力。
- M3G-8 或 file-access 回归失败。
- 泄露真实路径、bucket、object key、token、secret。

P1：

- 项目启动台状态圆环仍重叠。
- 工程主数据页仍有页面级横向无限延长。
- BIM 协同仍显示静态假 Viewer，未读取真实葛兰岱尔状态。
- 运行期需要的新增前端组件或图片未纳入 Git 跟踪。
- UX4-A-R1 项目工作台结构明显回退。
- 旧链接丢项目上下文。

P2：

- 既有 Vite chunk warning。
- 局部文案、间距、阴影、动效仍可优化但不阻塞主路径。

## 10. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

1. 结论：通过 / 不通过。
2. 三类修复是否关闭。
3. 必跑命令结果。
4. 浏览器页面结果。
5. Git 跟踪检查结果。
6. 旧链接兼容结果。
7. 多分辨率结果。
8. 越界和禁出字段检查结果。
9. P0 / P1 / P2 列表。
10. 是否建议主 agent 收口 UX4-B-R3。
