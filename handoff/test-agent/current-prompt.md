# Test Agent 当前任务：PLM-2 项目属性与经营台账 MVP 验收

你是卓羽智能数据中台 v1 的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

先阅读：

- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/dev-agent/latest-report.md`

不要修改 `docs/**`。

## 0. 验收目标

本轮只验收：

`PLM-2：项目属性与经营台账 MVP`

核心判断：

1. 每个项目可以维护预算、合同、回款、交付时间等项目属性。
2. 项目管理员和超级管理员可以编辑；普通员工只读。
3. 项目启动台 / 项目工作台能展示关键项目属性。
4. 不把本批扩成完整财务系统或权限大重构。
5. 完成后可由主 agent 收口并合并回 `main`，不得长期保留混乱支线。

## 1. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-plm2-project-business-profile.sh
bash scripts/dev/check-plm1-project-lifecycle.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-phase2-batch4-file-access.sh http://localhost:8080 admin 123456 503 506
git diff --check
git diff --cached --check
```

## 2. 接口验收

检查：

1. `GET /api/data-steward/projects/{projectId}/business-profile` 可返回默认项目属性。
2. `PUT /api/data-steward/projects/{projectId}/business-profile` 可更新：
   - 预算金额
   - 合同金额
   - 已回款金额
   - 回款状态
   - 预计回款时间
   - 计划开始时间
   - 计划交付时间
   - 实际交付时间
   - 备注
3. 回款比例正确。
4. 已回款金额大于合同金额时拒绝。
5. 未授权项目读取 / 更新被拒绝。
6. `DELIVERY_ENGINEER / VIEWER` 更新被拒绝。
7. 项目管理员可编辑自己管理的项目。
8. 超级管理员可编辑所有项目。
9. 更新写审计日志。

## 3. 页面轻验

至少打开：

- `http://127.0.0.1:5173/data-steward/assets`
- `http://127.0.0.1:5173/data-steward/assets/503`

确认：

1. 项目启动台展示项目属性关键指标，不堆财务明细。
2. 项目工作台能看到项目属性 / 经营信息摘要。
3. 项目管理员 / 超级管理员能看到编辑入口。
4. 普通员工看不到或无法使用编辑入口。
5. 编辑弹窗 / 抽屉可保存并刷新展示。
6. 页面无白屏、无横向溢出。

## 4. 安全与边界

确认响应和页面不泄露：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- bucket
- object key
- SQL
- raw row
- token
- secret

确认本批没有：

- 修改真实 NAS 文件。
- 修改对象化后台队列策略。
- 引入 Hermes / BIM / parser / indexing。
- 做合同附件、发票、利润核算等完整财务系统。
- 大改员工权限界面。

## 5. Git 范围检查

重点看：

1. 新增迁移是否纳入 Git。
2. 新增后端包 / DTO / controller / service 是否纳入 Git。
3. 新增前端运行期文件是否纳入 Git。
4. 新增专项脚本是否纳入 Git。
5. `.claude/**`、`tmp/**`、无关 demo 文件不要混入本批。

## 6. P0 / P1 / P2

P0：

- 项目启动台或项目工作台白屏。
- 普通员工可以修改项目经营信息。
- 未授权项目可读取或修改项目经营信息。
- 真实 NAS 文件被移动、删除、改名、覆盖。

P1：

- 项目属性无法保存或刷新后丢失。
- 项目管理员无法编辑自己项目。
- 回款比例或金额校验明显错误。
- 新增运行期文件 / 迁移 / 脚本未纳入 Git。
- PLM-1、M3G-8、file-access 任一回归失败。

P2：

- 文案、间距、字段排序不够顺。
- 既有 Vite chunk warning。

## 7. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告写清：

1. 结论：通过 / 不通过。
2. P0 / P1 / P2。
3. 接口验收结果。
4. 页面轻验结果。
5. 权限与安全结果。
6. 必跑命令结果。
7. 是否建议主 agent 收口并合并回 `main`。
