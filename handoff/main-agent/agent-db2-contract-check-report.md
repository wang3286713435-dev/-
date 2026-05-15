# 企业 Agent DB-2 只读合同自检报告

更新时间：2026-05-11  
执行人：主 agent / Codex  
范围：本机 dev MySQL `delivery_platform`，验证企业 Agent DB-2 只读接入合同是否可被一键复核。

## 1. 结论

结论：通过。

已新增并通过 `scripts/dev/check-agent-db2-contract.sh`。该脚本可以作为企业 Agent / Hermes 接入前的平台侧自检工具，用于确认：

- `hermes_agent_ro` 能连接本机 dev MySQL。
- 只读账号只能看到四个稳定 View。
- 四个 View 的核心字段存在。
- 事件游标可用。
- 业务底表不可读。
- 默认不打印真实项目名、文件名、NAS 路径或样本明细。

## 2. 本轮验证结果

执行命令：

```bash
bash scripts/dev/check-agent-db2-contract.sh
```

验证结果：

```text
database=delivery_platform
visible views OK: AuditEventView, FileAssetView, ModelAssetView, ProjectAssetView
ProjectAssetView: 11 columns OK
FileAssetView: 18 columns OK
ModelAssetView: 12 columns OK
AuditEventView: 9 columns OK
ProjectAssetView.count=519
FileAssetView.count=48842
ModelAssetView.count=10322
AuditEventView.count=59849
AuditEventView cursor OK
core_projects: forbidden OK
data_file_resources: forbidden OK
core_audit_logs: forbidden OK
ALLOW_LIMIT_30=false: using structure/count validation only; no sample rows printed
agent db2 contract ok
```

## 3. 自检脚本边界

脚本默认从 macOS 钥匙串读取本机 `hermes_agent_ro` 密码：

```text
service: delivery-platform-hermes-agent-ro-local-dev
account: hermes_agent_ro
```

如后续在临时环境运行，也可以通过 `READONLY_PASSWORD` 传入。脚本不会把密码写入仓库文档。

默认 `ALLOW_LIMIT_30=false`，只验证结构、数量和权限，不打印样本行。只有在内部授权确认后，才允许临时设置 `ALLOW_LIMIT_30=true` 做样本查询可执行性验证，且脚本仍不会把样本行打印到终端。

## 4. 对企业 Agent 的交接意义

企业 Agent 接入前，应先在平台侧跑通该脚本。通过后，Hermes 可以继续执行 DB-2 字段握手和只读 mirror schema 准备。

如果该脚本失败，应按失败类型处理：

- 四个 View 缺失或字段缺失：先修平台数据库迁移或 View 定义。
- 业务底表可读：立即停止联调，撤销 `hermes_agent_ro` 多余权限。
- 事件游标不可用：先检查审计事件写入和 `AuditEventView`。
- 密码缺失：通过安全渠道重新交付本机 dev 只读密码，不写入仓库。

## 5. 文档同步

已更新：

- `scripts/README.md`
- `scripts/dev/check-agent-db2-contract.sh`
- `handoff/main-agent/status.md`

## 6. 剩余事项

当前没有阻塞项。

后续企业 Agent 真正接入时，仍需坚持已冻结的 DB-2 协议：

- Agent 只读四个 View。
- 不读取业务底表。
- 不写数据库。
- 不写 NAS。
- 不把真实项目名、文件名、NAS 路径写入长期 memory、向量库、搜索库、外部日志或客户材料。
