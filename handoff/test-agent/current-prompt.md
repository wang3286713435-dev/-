# Test Agent 当前任务：PLM-1 项目生命周期管理 MVP 轻量验收

你是数字化交付平台 v1 的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

先阅读：

- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/dev-agent/latest-report.md`

不要修改 `docs/**`。

## 0. 验收目标

本轮只验收：

`PLM-1：项目生命周期管理 MVP`

重点看：

1. 超级管理员能创建项目。
2. 创建项目会初始化对象存储工作区和工程树根节点。
3. 超级管理员能二次确认后归档项目。
4. 归档项目不会物理删除 NAS / MinIO 数据。
5. 普通用户不能执行全局项目创建/归档。
6. 响应不泄露真实路径、bucket、object key、storage_uri、token、secret。

不要做全量浏览器逐页点击，保持轻量。

## 1. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-plm1-project-lifecycle.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 2. API/脚本验收点

重点确认：

1. 创建唯一临时项目成功。
2. 新项目能在项目列表中查询到。
3. 创建人获得 `PROJECT_ADMIN`。
4. 工程树根节点存在。
5. 重复初始化不会产生重复根节点。
6. 对象存储工作区状态返回业务化结果。
7. `confirmed=false` 归档被拒绝。
8. 确认文本错误归档被拒绝。
9. 确认文本正确归档成功。
10. 归档项目默认从项目列表隐藏。
11. 普通用户创建/归档被拒绝。
12. 禁出字段扫描通过。

禁出字段：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `bucket`
- `object_key`
- SQL
- raw row
- token
- secret

## 3. 前端轻验

只做极短浏览器确认：

1. 资产总览页能打开。
2. 超级管理员能看到“新建项目”入口。
3. 项目行能看到归档入口或更多菜单中的归档入口。
4. 非超级管理员不应看到全局归档入口，或点击后被拒绝。
5. 页面不白屏、不横向溢出。

不需要逐个点击工程主数据、文件管理、BIM、交付页面。

## 4. P0 / P1 判定

P0：

- 创建项目导致真实 NAS 文件被移动/删除/重命名。
- 归档项目物理删除业务数据。
- 普通用户可创建/归档全局项目。
- API/前端泄露 token/secret 或真实存储路径。
- 主链路白屏。

P1：

- 超级管理员不能创建项目。
- 创建项目没有工程树根节点。
- 创建项目没有对象存储工作区状态。
- 归档不需要二次确认。
- 归档后项目仍默认显示在项目启动台。
- PLM-1 脚本失败。
- 运行期新增文件未纳入 Git。

P2：

- 文案、间距、按钮位置等不影响主链路的小问题。
- 既有 Vite chunk warning。

## 5. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告写清：

1. 结论：通过 / 不通过。
2. P0 / P1 / P2。
3. 必跑命令结果。
4. PLM-1 创建/归档链路结果。
5. 禁出字段扫描结果。
6. 是否建议主 agent 收口。
