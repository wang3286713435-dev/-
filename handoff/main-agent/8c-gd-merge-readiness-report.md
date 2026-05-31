# 8C-GD 葛兰岱尔轻量化引擎分支合主线准备报告

更新时间：2026-06-01

## 1. 当前结论

当前葛兰岱尔轻量化引擎分支已推进到“可提 PR / 可进入主线合并评审”的准备状态。

本次收口口径必须准确：

- 可以合入：葛兰岱尔 RVT 轻量化任务、10 个 RVT 试点模型、平台 BIM 协同页内嵌 Viewer、整体模型浏览、鼠标旋转/平移/缩放、主视角/适配、距离/面积测量入口、真实数据页签、构件级能力诚实兜底。
- 不能宣称：构件级 BIM Viewer 已完成、构件拾取稳定可用、模型爆炸稳定可用、完整 Revit 属性已接入。
- 原因：当前试点 `root.glt` 产物可渲染整体模型，但尚未稳定提供构件索引 / feature picking / RevitId 属性映射。

## 2. 当前分支

```text
工作区：/Users/vc/Documents/数字化交付平台-8b-gd
当前分支：codex/8b-gd2-rvt-poc
当前基线：287a338 feat: embed Glandar RVT preview in BIM workspace
对比主线：main 当前包含 c57a5c7 Revert "merge: integrate BIM submission center"
```

主线存在 BIM submission 相关的提交与回滚记录，因此最终合并前建议走 PR 审查，不建议直接本地强合。

## 3. 已完成能力

### 后端

- 新增 `visualization_lightweight_jobs` 轻量化任务表。
- 新增葛兰岱尔配置读取与安全缺省：
  - 默认 `BIM_ENGINE_PROVIDER=MOCK`。
  - 只有显式配置 `GLANDAR` 且 Station 地址/token 完整时才提交真实转换。
- 新增葛兰岱尔 Station 客户端：
  - 小文件 `upload-file`。
  - 大文件 `SplitUploadFile`。
  - `query-model-info` 状态查询。
- 新增 / 完善平台轻量化接口：
  - `POST /api/visualization-adapter/projects/{projectId}/files/{fileId}/lightweight-jobs`
  - `GET /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}`
  - `POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket`
  - `GET /api/visualization-adapter/projects/{projectId}/glandar/rvt-pilot-files`
  - `POST /api/visualization-adapter/projects/{projectId}/glandar/rvt-pilot-files:submit`
- Viewer ticket 签发前执行平台权限校验与审计。

### 前端

- BIM 协同管理页接入 105 项目 10 个 RVT 试点模型。
- 已轻量化模型可在原模型预览框内嵌显示，不再跳到孤立演示页。
- Viewer iframe 只渲染模型画布和极简工具层，不渲染平台侧栏/顶部栏。
- BIM 协同页页签已改为平台真实数据驱动：
  - 综合驾驶舱
  - BIM 场景
  - 设备设施
  - 房屋空间
  - 告警监控
  - 轻量化模型列表
  - 专业系统
  - 工单巡检保养
- 构件级按钮在无构件索引时会明确禁用或提示，不再假装可用。

### 文档

- 新增轻量化引擎接入指南：
  - `handoff/main-agent/lightweight-engine-integration-guide.md`
- 该文档可供其他平台或其他轻量化引擎团队参考接入。

## 4. 本轮已跑验证

```text
后端构建：PASS
前端构建：PASS，仅保留既有 Vite chunk warning
后端健康检查：PASS，http://127.0.0.1:18088/actuator/health -> {"status":"UP"}
8C-GD1 十个 RVT 试点：PASS=11 FAIL=0
文件访问安全回归：PASS=18 FAIL=0
git diff --check：PASS
git diff --cached --check：PASS
```

另外执行了主线合并树预检查：

```text
git merge-tree --write-tree main HEAD
```

命令返回合成 tree hash，未报告文本冲突。但由于本分支仍有未提交改动，最终 PR 合并前仍建议由 GitHub 或本地临时分支再做一次正式合并检查。

## 5. 安全边界

当前分支必须保持这些边界：

- 前端不接触葛兰岱尔长期 token。
- 不返回真实 NAS 路径。
- 不返回 `storage_uri`。
- 不返回 bucket / object key。
- 不返回 SQL / raw row。
- 不改 M3 对象存储主线。
- 不做 Hermes 正文问答。
- 不写 documents / chunks / Qdrant / OpenSearch。
- 不移动、删除、重命名真实 NAS 文件。

## 6. 当前已知限制

1. 构件拾取仍不能作为完成项。
   - 当前模型可以整体渲染。
   - 但缺少可稳定消费的构件索引。
   - 需要葛兰岱尔团队确认转换配置如何输出 feature/batch/RevitId/property 索引。

2. 模型爆炸仍不能作为完成项。
   - 平台已按 API 尝试调用。
   - 但当前产物缺少爆炸分组或构件索引时，爆炸不会产生预期效果。

3. Viewer 启动当前只返回平台短时 `viewerTicket`。
   - Station 长期 token 不返回前端。
   - 后续正式版仍建议由葛兰岱尔提供短时 Viewer Session，或由平台做反向代理 ticket。

4. 当前只覆盖 105 项目 10 个 RVT 试点。
   - 不代表全部项目、全部 RVT、DWG、IFC、NWD/NWC 已接入。

## 7. 合主线建议

建议下一步按以下顺序推进：

1. 将当前分支所有交付文件暂存并提交为一个收口 commit。
2. 推送 `codex/8b-gd2-rvt-poc`。
3. 发起 PR 到 `main`。
4. PR 标题建议：

```text
feat: integrate Glandar RVT lightweight preview pilot
```

5. PR 描述必须写清：
   - 这是 RVT 轻量化预览试点。
   - 不是构件级 BIM Viewer 完整交付。
   - 不影响 M3 对象存储主线。
   - 合入后默认仍可用 `BIM_ENGINE_PROVIDER=MOCK` 安全关闭真实引擎。

## 8. 合入后建议下一批

合入主线后不建议立刻扩大到全量模型，建议先启动：

```text
8C-GD-MAINLINE-VERIFY：主线环境葛兰岱尔 Viewer 验收
```

验收内容：

- 主线 `5173` 前端可打开 BIM 协同管理。
- 主线后端以 `MOCK` 默认模式不报错。
- 注入 `GLANDAR` 配置后 105 的 10 个 RVT 仍可打开。
- 不泄露 token、NAS 路径、bucket、object key。
- M3 对象存储回归不受影响。

确认主线稳定后，再规划：

```text
8C-GD-COMPONENT-INDEX：构件索引与属性对接
```

该批必须等葛兰岱尔团队明确构件索引输出和属性接口后再启动。
