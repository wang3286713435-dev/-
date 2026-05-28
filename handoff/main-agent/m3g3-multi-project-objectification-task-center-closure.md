# M3G-3 多真实项目分批对象化策略与任务中心增强收口记录

收口时间：2026-05-28

## 结论

`M3G-3：多真实项目分批对象化策略与任务中心增强` 正式收口。

当前 P0：无。

当前 P1：无。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
- 本轮未做大规模浏览器逐页点击，测试重点是多项目对象化规划、dry-run、任务中心增强和禁出字段。

## 本批完成内容

- 全项目对象化盘点增加真实 / 测试项目分类、待对象化容量、路径风险和文件类型分布。
- 新增多项目对象化 dry-run 接口：
  - `POST /api/data-steward/storage-objectification-plans:dry-run`
- dry-run 支持项目范围、真实项目过滤、总量限制、单项目限制、并发和限速策略字段。
- dry-run 固定只生成计划：
  - 不创建真实迁移任务。
  - 不复制文件。
  - 不修改 NAS。
- 文件服务页增加“多项目对象化规划”视图。
- 新增专项脚本：
  - `scripts/dev/check-m3g3-multi-project-objectification-planning.sh`

## 验收依据

开发报告：

- `handoff/dev-agent/latest-report.md`

测试报告：

- `handoff/test-agent/latest-report.md`

专项与回归：

- M3G-3 专项脚本通过，`PASS=11 FAIL=0`。
- M3G-1 readiness / inventory / dry-run 回归通过。
- M3E 预览与转换产物对象化回归通过。
- M3F 新文件对象存储优先写入回归通过。
- M3C 对象存储迁移任务中心回归通过。
- file-access 文件访问安全回归通过。
- 后端构建、前端构建、健康检查、`git diff --check` 均通过。

## 边界确认

本批明确没有做：

- 没有执行真实多项目对象化迁移。
- 没有运行 M3G-2 执行型脚本。
- 没有创建迁移任务。
- 没有移动、删除、重命名、覆盖真实 NAS 文件。
- 没有读取 PDF / Office / DWG / RVT / IFC 正文。
- 没有写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 没有新增 Hermes 正文问答、BIM 引擎、parser 或 indexing 能力。
- 没有暴露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。

## 当前裁决

M3G-3 已把“多真实项目对象化”推进到可规划、可估算、可控 dry-run 的阶段。

下一步不应直接全量迁移。建议候选：

1. `M3G-4：受控多项目小批对象化执行`
   - 只选择主 agent 和用户确认的少量真实项目 / 少量文件。
   - 继续保留数量、容量、并发、限速和失败可追踪边界。
2. `M4A：documents / chunks 语义证据契约`
   - 如果暂缓继续对象化执行，可先冻结语义证据层契约。
