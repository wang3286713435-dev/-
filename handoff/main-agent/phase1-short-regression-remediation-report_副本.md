# 一期短回归 P1/P2 修复报告

更新时间：2026-05-13

## 结论

已根据 `handoff/test-agent/latest-report.md` 处理本轮短回归遗留问题。

当前结论：

- P1 已修复：`check-agent-db2-contract.sh` 重新通过。
- P2 已修复：首页项目下拉不再显示 `SCANCTRL-*` 测试项目，项目数回到 `18`。
- 未改业务逻辑代码，未修改真实 NAS 文件。

## P1：企业 Agent 只读账号密码口径不一致

测试报告中的失败：

```text
ERROR 1045 (28000): Access denied for user 'hermes_agent_ro'@'127.0.0.1'
```

处理：

- 重新生成本机 dev 的 `hermes_agent_ro` 只读密码。
- 同步更新 MySQL 账号密码。
- 同步更新 macOS 钥匙串：

```text
service = delivery-platform-hermes-agent-ro-local-dev
account = hermes_agent_ro
```

安全说明：

- 新密码未写入仓库、文档或报告。
- 权限仍只授予四个稳定 View：
  - `ProjectAssetView`
  - `FileAssetView`
  - `ModelAssetView`
  - `AuditEventView`
- 业务底表仍不可读。

验证结果：

```text
bash scripts/dev/check-agent-db2-contract.sh
结果：agent db2 contract ok
```

## P2：首页残留 SCANCTRL 测试项目

测试报告中的残留：

```text
SCANCTRL-1778661866 | 扫描控制回归-1778661866
```

处理：

- 已将残留 `SCANCTRL-*` 测试项目归档并软删除。
- 已同步删除该测试项目在 `core_user_project_roles` 中的可见授权。
- 未删除扫描任务历史，不影响扫描任务回归证据。
- 未触碰真实 NAS 项目和真实 NAS 文件。

同时修复根因：

- `scripts/dev/check-scan-task-control.sh` 增加退出清理逻辑。
- 后续脚本无论成功或失败，都会清理临时扫描目录，并归档本轮创建的 `SCANCTRL-*` 测试项目，避免再次污染首页项目下拉。

验证结果：

```text
bash scripts/dev/check-scan-task-control.sh
结果：scan task control regression passed

管理员 /api/core/users/me：
project_count = 18
scanctrl_visible = 0
```

## 回归验证

已复跑：

```text
curl -fsS http://localhost:8080/actuator/health
bash scripts/dev/check-agent-db2-contract.sh
bash scripts/dev/check-scan-task-control.sh
bash scripts/dev/check-asset-quality-overview.sh
bash scripts/dev/check-file-preview-shell.sh
git diff --check
```

结果：

- 后端健康检查通过：`{"status":"UP"}`
- 企业 agent DB-2 合同脚本通过。
- 扫描任务控制脚本通过，且不再留下可见测试项目。
- 数据质量脚本通过。
- 文件预览安全外壳脚本通过。
- `git diff --check` 通过。

## 当前状态

基于本轮修复，测试报告中的 `1 个 P1` 与 `1 个 P2` 均已处理。

建议测试 agent 做一次极短复验，只需重点确认：

1. `check-agent-db2-contract.sh` 通过。
2. 首页项目下拉为 `18` 个项目，且无 `SCANCTRL-*`。
3. `check-scan-task-control.sh` 复跑后不会再次留下可见 `SCANCTRL-*`。
