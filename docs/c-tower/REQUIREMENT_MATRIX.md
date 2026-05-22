# C塔需求覆盖矩阵

版本：v1.0
日期：2026-05-22
状态：P0 可验收

## 1. 说明

本矩阵逐条映射两份正式需求文件中的平台要求到 C塔平台模块、优先级、交付阶段、验收方式和风险。本矩阵为 P0 阶段可验收版本，覆盖两个源文件中的所有核心项。

不纳入专业工程评分表，不把当前 C塔原型或演示样例描述为正式交付成果。

## 2. 源文件

| 编号 | 文件 | 简称 |
| --- | --- | --- |
| SRC-01 | `/Users/vc/Downloads/数据中台.docx` | 数据中台 |
| SRC-02 | `/Users/vc/Downloads/智慧建筑平台清单及系统配置.xlsx` | 平台清单 |

## 3. 字段定义

| 字段 | 说明 |
| --- | --- |
| `requirement_id` | 需求编号，格式 `REQ-{domain}-{seq}` |
| `source_file` | 来源文件编号 |
| `source_section` | 来源文件中的章节/Sheet/行号 |
| `requirement_text` | 需求的简要描述 |
| `platform_domain` | 平台一级能力域 |
| `module` | 平台模块 |
| `priority` | P0(必须)/P1(重要)/P2(一般)/P3(增强) |
| `delivery_phase` | P0/P1/P2/P3/P4 |
| `acceptance_method` | 验收方式：功能测试/性能测试/安全测评/文档审查/业主确认/PoC验证 |
| `current_platform_fit` | 当前平台匹配度：已具备/可复用/需改造/需新增/不适用 |
| `gap` | 差距描述 |
| `risk` | 风险 |
| `owner_role` | 责任角色 |
| `needs_owner_confirmation` | 是/否 |

## 4. 覆盖摘要

| 源文件 | 域 | 需求条目数 |
| --- | --- | --- |
| 数据中台 | 多模态存储 | 见 REQ-DP-STORAGE |
| 数据中台 | 主数据管理 | 见 REQ-DP-MDM |
| 数据中台 | 数据治理 | 见 REQ-DP-GOV |
| 数据中台 | 数据服务 | 见 REQ-DP-SVC |
| 数据中台 | 隐私保护 | 见 REQ-DP-PRIV |
| 平台清单 | 性能指标 | 见 REQ-PF-PERF |
| 平台清单 | 软件清单-智慧管理系统 | 见 [REQ-PF-SW.md](requirements/REQ-PF-SW.md) |
| 平台清单 | 软件清单-经营服务系统 | 见 [REQ-PF-SW.md](requirements/REQ-PF-SW.md) |
| 平台清单 | 软件清单-数字底座 | 见 [REQ-PF-SW.md](requirements/REQ-PF-SW.md) |
| 平台清单 | 硬件配置 | 见 REQ-PF-HW |
| 平台清单 | 基础设施 | 见 REQ-PF-INFRA |
| 平台清单 | 数字化可视化 | 见 REQ-PF-VIS |
| 平台清单 | 驾驶舱交付 | 见 REQ-PF-DEL |

## 5. 按模块索引

详细需求条目按模块拆分到以下子文件：

| 域 | 子文件 | 条目数 |
| --- | --- | --- |
| 数据中台-多模态存储 | [REQ-DP-STORAGE.md](requirements/REQ-DP-STORAGE.md) | 12 |
| 数据中台-主数据管理 | [REQ-DP-MDM.md](requirements/REQ-DP-MDM.md) | 9 |
| 数据中台-数据治理 | [REQ-DP-GOV.md](requirements/REQ-DP-GOV.md) | 10 |
| 数据中台-数据服务 | [REQ-DP-SVC.md](requirements/REQ-DP-SVC.md) | 11 |
| 数据中台-隐私安全 | [REQ-DP-PRIV.md](requirements/REQ-DP-PRIV.md) | 6 |
| 平台清单-性能指标 | [REQ-PF-PERF.md](requirements/REQ-PF-PERF.md) | 8 |
| 平台清单-软件清单 | [REQ-PF-SW.md](requirements/REQ-PF-SW.md) | 69 |
| 平台清单-硬件配置 | [REQ-PF-HW.md](requirements/REQ-PF-HW.md) | 8 |
| 平台清单-基础设施 | [REQ-PF-INFRA.md](requirements/REQ-PF-INFRA.md) | 10 |
| 平台清单-可视化 | [REQ-PF-VIS.md](requirements/REQ-PF-VIS.md) | 35 |
| 平台清单-驾驶舱交付 | [REQ-PF-DEL.md](requirements/REQ-PF-DEL.md) | 15 |
| 平台清单-配套服务 | [REQ-PF-SVC.md](requirements/REQ-PF-SVC.md) | 5 |

总计：**198 条需求映射**

## 6. 全局待确认事项

以下事项影响多个模块，需由业主统一确认：

| 编号 | 待确认事项 | 影响范围 | 建议时机 |
| --- | --- | --- | --- |
| C-01 | 国产数据库选型（达梦/金仓/OceanBase） | 数据中台全部模块 | P0 冻结 |
| C-02 | 时序数据库选型（TDengine/其他） | 多模态存储、数据治理 | P0 冻结 |
| C-03 | 空间编码规则（IFC/BOMA/自定义） | One Map、设备管理 | P0 冻结 |
| C-04 | 设备编码规范最终确认 | One Thing、资产管理 | P0 冻结 |
| C-05 | 第三方系统接口清单与协议 | 集成中台、数据接入 | P1 前 |
| C-06 | AI 算法清单（不少于 30 种）和验收标准 | AI 中台 | P2 前 |
| C-07 | 云渲染并发数（5-10 路 4K）和 GPU 资源 | 可视化、基础设施 | P3 前 |
| C-08 | 移动端优先级（企业微信/小程序/原生 App） | 移动端 | P2 前 |
| C-09 | 等保二级费用承担方和测评安排 | 安全、预算 | P0 冻结 |
| C-10 | 开源组件、第三方授权和源代码范围 | 知识产权、交付 | P0 冻结 |
