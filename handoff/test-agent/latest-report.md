# 测试 Agent 报告：M2I P1 Git 跟踪范围极短复验

时间：2026-05-25 CST

## 1. 复验结论

结论：通过。

上一轮 P1 `M2I 新增关键交付文件未纳入 Git 跟踪范围` 已修复。当前无 M2I 关键交付文件处于 `??` 未跟踪状态，M2I 专项脚本通过，`git diff --check` 通过。

当前无 P0 / P1。

## 2. P0 / P1 / P2

P0：无。

P1：无。

P2：

- 非交付新增项仍为未跟踪状态，未纳入本轮 M2I 交付范围：
  - `.claude/skills/frontend-design/`
  - `CLAUDE.md`
  - `tmp/backups/`
  - `tmp/jar-inspect/`
  - `tmp/run-logs/*`
- 仓库历史中已有少量 `.claude/**`、`tmp/run-logs/*.pid` 被 Git 跟踪；本次复验确认 M2I P1 修复没有新增纳入这些非交付项。

## 3. Git 跟踪范围检查

`git status --short` 显示 M2I 关键文件已进入 Git 跟踪 / 暂存范围：

```text
A  backend/delivery-app/src/main/resources/db/migration/V26__ctower_demo_validation_boundary.sql
A  backend/delivery-app/src/main/resources/db/migration/V27__m2i_file_ownership_governance.sql
A  backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/ownership/FileOwnershipApplicationService.java
A  backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/ownership/FileOwnershipController.java
A  frontend/src/modules/data-steward/components/FileOwnershipTreePanel.vue
A  scripts/dev/check-m2i-105-file-ownership-governance.sh
```

逐项 `git ls-files --error-unmatch` 结果：

```text
TRACKED backend/delivery-app/src/main/resources/db/migration/V26__ctower_demo_validation_boundary.sql
TRACKED backend/delivery-app/src/main/resources/db/migration/V27__m2i_file_ownership_governance.sql
TRACKED backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/ownership/FileOwnershipController.java
TRACKED backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/ownership/FileOwnershipApplicationService.java
TRACKED frontend/src/modules/data-steward/components/FileOwnershipTreePanel.vue
TRACKED scripts/dev/check-m2i-105-file-ownership-governance.sh
```

`git diff --cached --name-only` 未包含本轮新增的非交付项 `.claude/skills/frontend-design/`、`CLAUDE.md`、`tmp/backups/`、`tmp/jar-inspect/`、`tmp/run-logs/*`。

## 4. 命令结果

```text
git status --short
结果：M2I 关键交付文件均为 A/M，不再是 ??

git diff --check
结果：PASS

bash scripts/dev/check-m2i-105-file-ownership-governance.sh
结果：PASS=8 FAIL=0
```

M2I 专项脚本关键输出：

```text
[PASS] 管理员登录成功
[PASS] 已切换到 105 真实项目
[PASS] 归属覆盖率、工程树和未归属列表可查且安全脱敏
[PASS] 推荐结果可解释，未确认时不能写入
[PASS] 105 所有文件均进入工程树归属或待判定/归档节点
[PASS] 工程树节点能查看对应已归属文件
[PASS] 文件管理器可拿到归属节点和归属状态
[PASS] 2928 个归属记录未污染正式交付包应交项
=== Result: PASS=8 FAIL=0 ===
```

## 5. 最终建议

M2I P1 已关闭。建议主 agent 进入 M2I 收口判断。
