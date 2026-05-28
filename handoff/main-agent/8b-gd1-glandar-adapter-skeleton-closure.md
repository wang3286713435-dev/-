# 8B-GD1 葛兰岱尔适配骨架收口记录

日期：2026-05-28

## 结论

`8B-GD1：平台侧葛兰岱尔适配骨架` 正式收口。

当前无 P0 / P1。仅保留既有 P2：前端构建 chunk size warning。

## 本批完成内容

- 默认 `MOCK` 轻量化模式保持兼容。
- 新增 `GLANDAR` provider 配置读取边界。
- 新增 lightweight job / viewer ticket 平台接口骨架：

```text
POST /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

- `GLANDAR` 未配置或安全凭据缺失时返回业务化阻断，不返回 500。
- 响应中不暴露真实 NAS 路径、bucket、object key、token、secret、raw row、SQL。
- 新增专项脚本：

```text
scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh
```

## 验收结果

测试 agent 已完成两轮验收：

1. 完整验收：
   - 后端构建通过。
   - `18088` 健康检查通过。
   - 8B-GD1 专项脚本通过，`PASS=16 FAIL=0`。
   - 8A Mock 回归通过，`PASS=11 FAIL=0`。
   - file-access 回归通过，`PASS=18 FAIL=0`。
   - 前端构建通过。
   - `git diff --check` 通过。
   - 额外验证 `BIM_ENGINE_PROVIDER=GLANDAR` 且未配置时返回业务化阻断，不是 500。

2. P1 极短复验：
   - `GlandarEngineSettings.java` 已纳入 Git 跟踪。
   - `scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh` 已纳入 Git 跟踪。
   - `git diff --check` 和 `git diff --cached --check` 均通过。
   - `git ls-files --others --exclude-standard` 无输出。

测试报告：

```text
handoff/test-agent/latest-report.md
```

## 明确未做

- 未调用葛兰岱尔 Station `SplitUploadFile`。
- 未上传 RVT / IFC / DWG。
- 未读取模型正文。
- 未生成真实轻量化产物。
- 未签发真实 Viewer ticket。
- 未新增数据库迁移。
- 未修改 `docs/**`。
- 未触碰 M3G 对象存储主线。

## 后续建议

下一批为：

```text
8B-GD2：105 RVT PoC 转换闭环
```

进入 8B-GD2 前必须确认：

- 葛兰岱尔 Station API 可访问。
- Station Token 已通过安全环境变量注入，不进入 Git / handoff / 前端。
- 105 项目 RVT 样本已选定。
- 引擎团队确认 `SplitUploadFile`、`query-model-info`、viewer `root.glt` 地址规则和错误码。
- 不影响 M3 对象存储主线。
