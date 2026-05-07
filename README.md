# 数字化交付平台

本仓库用于承载数字化交付平台的需求文档、工程源码、测试资产和多 agent 交接文件。

## 当前阶段

项目已经从早期 `v1 MVP` 升级为三期路线：

1. 一期：公司内部 BIM 资产管理试点。
2. 二期：可给客户交付的数字化交付平台完整版。
3. 三期：增值服务和客户持续服务。

下一步业务开发重点是一期：

- 几百个 BIM 项目统一建档。
- NAS 原地接管，不搬迁大文件。
- 模型文件元数据、路径、版本、专业、checksum 可检索。
- 提供项目资产台账、模型资源库、容量看板。
- 为企业内核级 agent 提供稳定读模型和 REST/OpenAPI。

## 目录说明

- `docs/`：需求、架构、验收、实施路线和 Windows 迁移文档。
- `backend/`：Java + Spring Boot 模块化单体后端。
- `frontend/`：Vue 3 + TypeScript + Element Plus 前端。
- `infra/`：MySQL、Redis、MinIO、Nginx 和部署辅助配置。
- `scripts/`：macOS/Linux Bash 与 Windows PowerShell 开发脚本。
- `handoff/`：主 agent、开发 agent、测试 agent、Windows agent 交接文件。

## 关键文档

- 最新 PRD：[docs/07-complete-delivery-prd.md](/Users/Weishengsu/dev/zhuoyusmart/数字化交付平台/docs/07-complete-delivery-prd.md)
- 验收与企业 agent 对接：[docs/08-acceptance-and-agent-integration.md](/Users/Weishengsu/dev/zhuoyusmart/数字化交付平台/docs/08-acceptance-and-agent-integration.md)
- 架构设计：[docs/03-architecture-and-system-design.md](/Users/Weishengsu/dev/zhuoyusmart/数字化交付平台/docs/03-architecture-and-system-design.md)
- Windows 迁移：[docs/09-windows-dev-migration.md](/Users/Weishengsu/dev/zhuoyusmart/数字化交付平台/docs/09-windows-dev-migration.md)
- Windows Codex 接手入口：[handoff/windows-agent/current-prompt.md](/Users/Weishengsu/dev/zhuoyusmart/数字化交付平台/handoff/windows-agent/current-prompt.md)

## 本地启动

macOS / Linux：

```bash
bash scripts/dev/bootstrap-infra.sh
bash scripts/dev/start-backend.sh
bash scripts/dev/start-frontend.sh
```

Windows PowerShell：

```powershell
.\scripts\dev\bootstrap-infra.ps1
.\scripts\dev\start-backend.ps1
.\scripts\dev\start-frontend.ps1
```

样板账号：

- 用户名：`platform.admin`
- 密码：`Admin@123`

## 协作方式

- 主 agent：负责需求冻结、架构守门、任务拆分、prompt 编写、验收裁决。
- 开发 agent：负责按授权目录实现代码。
- 测试 agent：负责按验收口径执行测试、记录缺陷和回归结论。
- Windows agent：负责接收项目、验证 Windows 环境和回写迁移报告。

Agent 协作细则见 [docs/agents/00-session-governance.md](/Users/Weishengsu/dev/zhuoyusmart/数字化交付平台/docs/agents/00-session-governance.md)。
