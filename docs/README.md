# 卓羽智能数据中台文档包

本目录用于沉淀卓羽智能数据中台的产品、架构、验收和 agent 协作文档。当前项目已经从早期 `v1 MVP` 口径升级为三期路线：一期内部 BIM 资产管理试点，二期客户交付完整版，三期增值服务与持续演进。

## 文档清单

1. [01-competitor-analysis.md](/Users/vc/Documents/数字化交付平台/docs/01-competitor-analysis.md)
   现有平台的功能拆解、信息架构、业务主链路、技术痕迹和可复刻点。
2. [02-v1-prd.md](/Users/vc/Documents/数字化交付平台/docs/02-v1-prd.md)
   v1 产品需求文档，明确目标用户、范围、业务流程、功能要求和验收口径。
3. [03-architecture-and-system-design.md](/Users/vc/Documents/数字化交付平台/docs/03-architecture-and-system-design.md)
   总体架构、模块设计、核心实体、REST 契约、部署方案和三维适配层定义。
4. [04-rollout-and-agent-prompts.md](/Users/vc/Documents/数字化交付平台/docs/04-rollout-and-agent-prompts.md)
   分阶段实施路线、风险控制、验收计划，以及开发 agent 的任务模板与协作规范。
5. [05-phase1-dev-baseline.md](/Users/vc/Documents/数字化交付平台/docs/05-phase1-dev-baseline.md)
   第一阶段开发基线，锁定工程结构、技术选型、通用规范和模块依赖规则。
6. [06-phase1-backlog-and-readiness.md](/Users/vc/Documents/数字化交付平台/docs/06-phase1-backlog-and-readiness.md)
   第一阶段开工清单、任务拆分、完成定义和仍需业务侧拍板的少量事项。
7. [07-complete-delivery-prd.md](/Users/vc/Documents/数字化交付平台/docs/07-complete-delivery-prd.md)
   完整 PRD 与三期路线，明确一期内部资产试点、二期客户交付完整版、三期增值服务的产品边界。
8. [08-acceptance-and-agent-integration.md](/Users/vc/Documents/数字化交付平台/docs/08-acceptance-and-agent-integration.md)
   三期验收矩阵、企业 agent 对接契约、稳定读模型、缺陷分级和交付文档验收口径。
9. [09-windows-dev-migration.md](/Users/vc/Documents/数字化交付平台/docs/09-windows-dev-migration.md)
   Windows 原生开发迁移说明，包含源码交接风险、启动命令、验收脚本和 Windows Codex 接手入口。
10. [10-phase2-development-roadmap.md](/Users/vc/Documents/数字化交付平台/docs/10-phase2-development-roadmap.md)
   二期客户交付版当前开发路线，明确一期收口后的二期主视角、已完成批次、后续批次、上下文减负规则和近期禁止事项。
11. [11-current-baseline-and-next-roadmap.md](/Users/vc/Documents/数字化交付平台/docs/11-current-baseline-and-next-roadmap.md)
   当前真实开发基线与后续路线，说明平台已经完成什么、不能承诺什么，以及 M3/M4/M5/8D/9A 的下一步顺序。
12. [12-api-contract-and-maintenance.md](/Users/vc/Documents/数字化交付平台/docs/12-api-contract-and-maintenance.md)
   API 契约与文档维护说明，保留运行期 OpenAPI 入口、接口分组、禁出字段和后续 API 同步规则。
13. [13-ux4-frontend-architecture-baseline.md](/Users/vc/Documents/数字化交付平台/docs/13-ux4-frontend-architecture-baseline.md)
   UX4 前端信息架构与交互基线，明确全局壳层、项目启动台、项目工作台、文件管理、工程主数据、交付闭环、BIM 协同和旧链接兼容规则。
14. [14-phase2-closure-requirements.md](/Users/vc/Documents/数字化交付平台/docs/14-phase2-closure-requirements.md)
   二期当前基线、收口分级、后续任务图和必须验收的红线清单。

## 当前版本结论

- 产品焦点：`建筑机电/BIM交付`
- 部署模式：`单客户私有化部署`
- 一期状态：`已收口，后续仅处理 P0/P1 回归或真实 NAS 数据治理必要修复`
- 当前主线：`M3 对象存储主链路收口，105 样板项目已完成全量对象化，后续全项目对象化需单开批次`
- 后续主线：`M4 语义证据层 -> M5 Hermes 受控证据问答 -> BIM 构件级深化 -> 9A 客户交付准备`
- 后端架构：`Java + Spring Boot 模块化单体`
- 存储底座：`StorageService / StorageProvider 抽象，当前已接 NAS 侧 MinIO 对象存储；105 已 100% 对象化，非 105 仍按覆盖率报告分批推进`
- 一期文件范围：`.rvt`、`.dwg`、`.ifc`、`.nwd`、`.nwc`、`.dxf`、`.pdf`
- 一期治理链路：`项目清单/路径映射 -> NAS扫描 -> 自动入库/待审核 -> 资产库 -> SQL View -> 事件流`
- 真实 NAS 试点：`/Volumes/zyzn/卓羽智能项目`，采用只读影子导入，不移动、不改名、不删除原文件
- 真实 NAS 治理裁决：标准项目可入库，`98`、`95`、`99` 等非标准目录暂不自动进入正式资产库
- agent 接入：`API Key + 项目范围授权 + SQL View + REST/OpenAPI`
- 删除策略：`逻辑删除不碰 NAS，物理删除需申请审核并隔离 30 天`
- 三维底座：`可插拔适配层；当前已接葛兰岱尔 READY Viewer 小样本能力，但未完成构件级 BIM 能力`
- 首要差异化：`标准驱动交付`

## 使用方式

- 产品负责人先读完整 PRD 与验收文档，再读竞品拆解报告，对齐业务口径。
- 架构师和开发负责人以系统设计文档为主，统一模块边界与接口契约。
- 开发 agent 必须先读实施与 agent 手册，再接收模块级 prompt 开工。
- Windows 端 Codex 必须先读 `handoff/windows-agent/project-context.md` 和 `handoff/windows-agent/current-prompt.md`。
- 第一阶段开工前，研发负责人必须先过开发基线和 readiness 清单。
- 所有需求变更以本目录文档更新为准，不以口头约定或聊天记录替代。

## 二期日常上下文建议

后续二期开发默认先读：

- [07-complete-delivery-prd.md](/Users/vc/Documents/数字化交付平台/docs/07-complete-delivery-prd.md)
- [08-acceptance-and-agent-integration.md](/Users/vc/Documents/数字化交付平台/docs/08-acceptance-and-agent-integration.md)
- [03-architecture-and-system-design.md](/Users/vc/Documents/数字化交付平台/docs/03-architecture-and-system-design.md)
- [10-phase2-development-roadmap.md](/Users/vc/Documents/数字化交付平台/docs/10-phase2-development-roadmap.md)
- [11-current-baseline-and-next-roadmap.md](/Users/vc/Documents/数字化交付平台/docs/11-current-baseline-and-next-roadmap.md)
- [12-api-contract-and-maintenance.md](/Users/vc/Documents/数字化交付平台/docs/12-api-contract-and-maintenance.md)
- [13-ux4-frontend-architecture-baseline.md](/Users/vc/Documents/数字化交付平台/docs/13-ux4-frontend-architecture-baseline.md)
- [14-phase2-closure-requirements.md](/Users/vc/Documents/数字化交付平台/docs/14-phase2-closure-requirements.md)
- [phase2-current-roadmap.md](/Users/vc/Documents/数字化交付平台/handoff/main-agent/phase2-current-roadmap.md)
- [hermes-jarvis-coupling-roadmap.md](/Users/vc/Documents/数字化交付平台/handoff/main-agent/hermes-jarvis-coupling-roadmap.md)

`01` 到 `06` 主要作为历史和一期资料保留，除非追溯竞品、早期 MVP 或迁移问题，不再默认加载。
