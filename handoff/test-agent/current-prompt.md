# 测试 Agent 当前任务：二期批次四验收 - 文件预览与下载权限分离最小闭环

你是数字化交付平台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收二期批次四，不清理样板项目，不验证模型轻量化。

## 0. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/phase2-batch4-file-preview-download-permission-plan.md`
3. `handoff/dev-agent/latest-report.md`
4. `scripts/dev/check-phase2-batch4-file-access.sh`
5. `scripts/dev/check-file-preview-shell.sh`

## 1. 验收目标

验证本轮是否真正做到：

`文件详情 -> 判断预览/下载权限 -> 生成短时访问票据 -> 平台受控读取文件 -> 浏览器预览或下载 -> 审计留痕`

重点不是“页面看起来有按钮”，而是权限、路径隐藏、只读访问和审计都成立。

## 2. 必测项

### A. 构建与健康

必须执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://localhost:8080/actuator/health
git diff --check
```

### B. 专项脚本

必须执行：

```bash
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-file-preview-shell.sh
```

专项脚本至少应证明：

1. 管理员可预览。
2. 管理员可下载。
3. 查看者可预览但不可下载。
4. 跨项目用户不可预览或下载。
5. 文件不存在或不可读时错误清晰。
6. OpenAPI 包含新增接口。
7. 访问、拒绝、失败动作写审计。

### C. 页面验收

至少手动打开：

1. `http://localhost:5173/data-steward/assets/1`
2. `http://localhost:5173/data-steward/catalog`

验证：

1. 文件详情能看到预览/下载状态。
2. 可预览文件可以打开预览。
3. 可下载文件可以触发下载。
4. 无下载权限时下载按钮不可用或被明确拒绝。
5. 权限不足、路径失效、文件不可读时文案清楚。
6. 页面不横向撑爆。
7. 不直接显示真实 NAS 路径给普通用户。

### D. 回归

建议执行：

```bash
bash scripts/dev/check-phase2-batch1-readonly-catalog.sh
bash scripts/dev/check-phase2-batch2-standard-delivery.sh
bash scripts/dev/check-phase2-batch3-review-rectification-report.sh
```

重点确认：

1. 只读目录不回归。
2. 交付完整率不回归。
3. 审核整改不回归。
4. 旧 `Agent preview / permission proof` 不回归。

## 3. P0 判定

出现任一项即 P0，不允许收口：

1. 无项目权限用户可预览或下载文件。
2. 只有预览权限的用户可以下载。
3. 接口或页面暴露真实 NAS 路径给普通用户。
4. 访问票据绕过权限或过期仍可使用。
5. 真实 NAS 文件被移动、删除、改名或修改。
6. 文件不存在却返回成功。
7. 预览/下载关键动作无审计。
8. OpenAPI 不可访问或新增接口缺失。

## 4. P1 判定

以下问题为 P1，原则上不建议收口：

1. 页面按钮状态和后端权限不一致。
2. 错误文案无法让用户判断是无权限、路径失效还是格式不支持。
3. 预览或下载需要用户手动复制底层路径。
4. PDF/图片原生预览不可用，但后端声明可用。
5. 下载大文件时后端一次性读入内存。

## 5. 明确不测

本轮不要求：

1. 模型轻量化。
2. 构件级解析。
3. Office/CAD/BIM 在线转换。
4. 正文抽取。
5. 向量库。
6. Agent 自动审批或自动写库。
7. 客户生产级权限体系。
8. 样板项目数据清理。

## 6. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

1. 总结论：通过/不通过。
2. P0/P1/P2 列表。
3. 构建与健康检查结果。
4. 专项脚本结果。
5. 页面验收结果。
6. 权限分离验证结果。
7. NAS 路径隐藏验证结果。
8. 审计验证结果。
9. 是否可以收口二期批次四。
