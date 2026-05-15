# 测试 Agent 报告：DB-2 只读账号与四个 View 复核

更新时间：2026-05-11  
执行人：测试 agent（Codex）  
范围：仅复核运维/数据库负责人生成的 DB-2 只读接入材料与本机 dev MySQL 实况，不修改业务代码。

## 1. 结论摘要

结论：本机 dev 上的 `hermes_agent_ro` 已创建并可用，四个稳定 View 已存在且与文档口径一致，业务底表读取已被 MySQL 权限拒绝。  
从“只读接入是否成立”这个角度看，`enterprise-agent-db2-ready-package.md` 和 `ops-db2-readonly-account-report.md` 的主结论基本可信。

本轮未发现“企业 Agent 实际被引导去使用平台应用主账号”的证据。  
但我发现 1 个非阻塞文档卫生风险：

- `ops-db2-readonly-account-report.md` 曾明文写入本机应用账号，虽然上下文明确标注“禁止使用/仅供本机开发验证”，但这仍然提高了误用和扩散风险。
- 两份材料也把本机 dev 的 `hermes_agent_ro` 密码直接写入了仓库文档。它们反复声明“仅限本机 dev、不得用于 staging / production”，但如果这些文档会继续扩散，建议主 agent 评估是否改成占位符 + 安全渠道交付。

## 2. 我实际复核了什么

### 2.1 文档复核

我检查了以下文件：

- `handoff/main-agent/enterprise-agent-db2-ready-package.md`
- `handoff/main-agent/ops-db2-readonly-account-report.md`
- `handoff/main-agent/enterprise-agent-db-connection-contract.md`
- `handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md`
- `backend/delivery-app/src/main/resources/db/migration/V9__batch1_sql_views_and_perms.sql`
- `backend/delivery-app/src/main/resources/db/migration/V13__model_asset_view_include_nas_files.sql`
- `infra/docker-compose.yml`

### 2.2 实库复核

我对本机 Docker MySQL `delivery-mysql` 做了只读验证，重点核对：

- `hermes_agent_ro` 是否存在
- 它的 grants 是否只包含四个 View 的 `SELECT`
- 用 `hermes_agent_ro` 连接后，是否只能看到四个 View
- 四个 View 是否真实可读
- 业务底表是否被拒绝

## 3. 实际验证结果

### 3.1 `hermes_agent_ro` 已创建

实库查询结果显示：

- `delivery@%`
- `hermes_agent_ro@%`

说明本机 dev 库里确实存在 `hermes_agent_ro`。

### 3.2 `hermes_agent_ro` 的 grants 只到四个 View

实库 `SHOW GRANTS FOR 'hermes_agent_ro'@'%'` 结果为：

```text
GRANT USAGE ON *.* TO `hermes_agent_ro`@`%`
GRANT SELECT ON `delivery_platform`.`AuditEventView` TO `hermes_agent_ro`@`%`
GRANT SELECT ON `delivery_platform`.`FileAssetView` TO `hermes_agent_ro`@`%`
GRANT SELECT ON `delivery_platform`.`ModelAssetView` TO `hermes_agent_ro`@`%`
GRANT SELECT ON `delivery_platform`.`ProjectAssetView` TO `hermes_agent_ro`@`%`
```

这与运维报告中的授权摘要一致，当前未看到额外业务表 `SELECT` 权限。

### 3.3 只读账号连接后只看到四个 View

使用 `hermes_agent_ro` 执行：

```sql
SELECT DATABASE();
SHOW FULL TABLES WHERE Table_type='VIEW';
```

返回结果：

```text
delivery_platform
AuditEventView
FileAssetView
ModelAssetView
ProjectAssetView
```

当前在 `delivery_platform` 下，`hermes_agent_ro` 能看到的就是这四个 View。

### 3.4 四个 View 可读，且行数与文档一致

用 `hermes_agent_ro` 实测：

```sql
SELECT COUNT(*) FROM ProjectAssetView;
SELECT COUNT(*) FROM FileAssetView;
SELECT COUNT(*) FROM ModelAssetView;
SELECT COUNT(*) FROM AuditEventView;
```

结果：

```text
ProjectAssetView -> 518
FileAssetView    -> 46342
ModelAssetView   -> 7822
AuditEventView   -> 57301
```

这与两份运维材料里的数字完全一致。

### 3.5 业务底表读取被拒绝

我用 `hermes_agent_ro` 直接尝试读取：

```sql
SELECT COUNT(*) FROM core_projects;
SELECT COUNT(*) FROM data_file_resources;
SELECT COUNT(*) FROM core_audit_logs;
```

三条都返回 `ERROR 1142 SELECT command denied`。

结论：当前只读账号确实不能读业务底表。

### 3.6 四个 View 的字段合同存在且可见

我还执行了：

```sql
SHOW COLUMNS FROM ProjectAssetView;
SHOW COLUMNS FROM FileAssetView;
SHOW COLUMNS FROM ModelAssetView;
SHOW COLUMNS FROM AuditEventView;
```

字段可正常返回，说明 DB-2 的结构握手前提具备。  
另外，`ModelAssetView` 的现状已经包含 V13 的补丁逻辑，因此并不是停留在最初 V9 的老定义。

## 4. 对用户关心问题的逐项判断

### 4.1 运维/数据库负责人是否创建/确认了 `hermes_agent_ro`

是。

判断依据：

- 文档中明确写出 `hermes_agent_ro`
- 实库中确实存在 `hermes_agent_ro@%`
- 实库 grants 也与文档吻合

### 4.2 是否验证了四个 View

是，而且我做了二次独立复核。

判断依据：

- 文档内给出了四个 View 的发现、列结构和行数验证口径
- 我在本机 dev 实库里重新跑了一次
- 行数和文档完全对上

### 4.3 有没有误用平台应用主账号

没有发现“把企业 Agent 接入错误地指向应用账号”的证据。  
两份主材料都明确写了“禁止使用平台应用主账号”。

但有一个需要提醒主 agent 的非阻塞问题：

- `ops-db2-readonly-account-report.md` 曾把平台应用账号明文写进文档，虽然是为了说明“这是应用账号，不可用于企业 Agent”，但这本身会增加误抄、误用和密码扩散的风险。

我的建议是：

- 若这些材料会继续流转给更多人，最好把应用账号密码改成占位符，或直接删掉密码，只保留“禁止使用应用账号”这层结论。

### 4.4 只读账号是否只能读四个 View

从当前 grants 和实测结果看，是。

证据：

- `SHOW GRANTS` 只有四条 View 的 `SELECT`
- `SHOW FULL TABLES WHERE Table_type='VIEW'` 只返回四个 View
- 业务底表读取被拒绝

说明：

- 我本轮验证的是业务库 `delivery_platform` 的实际访问边界。
- 没有继续扩展去做系统库级别渗透式枚举；但对当前 DB-2 只读接入判断来说，现有证据已经足够支持“业务数据面只开放四个 View”。

### 4.5 是否明确禁止读业务底表

是，且不只是文档说了，实库也真的禁止。

### 4.6 是否明确写出真实项目名 / NAS 路径暴露风险

是，两份主材料都写得比较清楚。

包括但不限于：

- 本机 dev 样例可能包含真实项目名、真实文件名、真实 NAS 路径
- 不得外发
- 不得写入外部云服务
- 不得进入客户材料
- 如果日志、调试面板、向量化或观测链路会持久化样本，则必须改为 `STRUCTURE_ONLY`

这一点我认为写得是充分的。

### 4.7 shared-dev / staging 缺口是否写清楚

是，写得比较完整。

我看到的关键信号包括：

- 测试时口径为“远程环境未交付前阻塞”。
- 后续主 agent 已根据用户裁决更新为：本机同环境联调是当前一期路径，shared-dev / staging 改为后置触发项。
- shared-dev / staging 默认 `STRUCTURE_ONLY`。
- 未经业务负责人和数据负责人书面确认，不允许 `LIMIT 30` 读取真实样例。
- 目标环境 DSN、只读账号、密码交付、反向权限验证都列成了后置开通门禁。

这一部分的门禁和悬空项描述是清晰的。

## 5. 我给主 agent 的判断

### 5.1 可以确认的事

- 本机 dev 的 `hermes_agent_ro` 已创建并可用。
- 当前权限确实只开放四个稳定 View。
- 业务底表访问已被拒绝。
- 四个 View 的字段合同和行数验证口径与文档一致。
- 文档已明确写出真实项目名 / 文件名 / NAS 路径的暴露风险。
- shared-dev / staging 的 DSN、只读账号、样例读取和默认 `STRUCTURE_ONLY` 缺口已经写清楚；后续主 agent 已将它们改为本机联调后的后置触发项。

### 5.2 我建议主 agent 追加处理的点

非阻塞建议：

- 清理或收敛文档中的明文密码暴露，至少包括：
  - `ops-db2-readonly-account-report.md` 中的平台应用账号明文密码
  - `enterprise-agent-db2-ready-package.md` 和 `ops-db2-readonly-account-report.md` 中的本机 dev 只读密码
- 如果这些文档将继续跨团队流转，建议改成：
  - 账号名可保留
  - 密码改 `<dev-secret-from-safe-channel>`
  - 并保留“禁止使用应用账号 / 正式环境密码不得入仓”的结论

这不影响我对“当前本机 dev 只读接入已成立”的判断，但会影响后续材料扩散时的安全卫生。

## 6. 最终结论

给主 agent 的最终结论是：

- `hermes_agent_ro` 已创建并确认可用。
- 四个 View 已独立复核通过。
- 没有发现运维把企业 Agent 实际引导到平台应用主账号的证据。
- 只读账号当前确实只能读四个 View，且无法读业务底表。
- 真实项目名 / NAS 路径暴露风险和 shared-dev / staging 缺口都写清楚了。
- 需要主 agent 关注的主要是文档里的明文密码暴露问题，这更像材料卫生风险，不是本轮 DB-2 只读权限本身失效。
