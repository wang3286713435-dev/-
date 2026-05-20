# 数字化交付标准同步包 V0.1-beta

本目录中的标准文件以 `DigitalDeliveryProject` 为 canonical source。平台和 Hermes 目录中的同名文件只是镜像，不允许形成长期分叉。

## 核心文件

| 文件 | 用途 |
|---|---|
| `digital_delivery_standard_v0.1.md` | 镜像自 `01 数字化交付总则/工程数字化交付标准V0.1.md`。 |
| `gws_standard_alignment_matrix.md` | 镜像自 `01 数字化交付总则/工务署标准对齐矩阵.md`。 |
| `standard_rule_matrix.md` | 镜像自 `03 数据标准/标准规则矩阵.md`。 |
| `standard_to_platform_mapping.md` | 镜像自 `03 数据标准/标准到平台字段映射.md`。 |
| `standard_to_agent_boundary.md` | 镜像自 `05 平台协同与流程标准/标准到Hermes边界.md`。 |
| `shared_standard_source_contract.md` | 平台、Hermes 与共享标准的同源契约。 |
| `standard_enforcement_workflow.md` | 无标准不开发、平台/Hermes/数据库 Agent/族库/项目交付的执行门禁。 |
| `project_standard_binding.md` | 新项目默认继承公司基础标准及 override 规则。 |
| `missing_evidence_policy.md` | Missing Evidence 策略。 |
| `catalog_tool_contract.md` | Catalog Tool 契约。 |
| `platform_to_hermes_contract.md` | Platform 到 Hermes 对接契约。 |
| `RISK_RED_LINES.md` | 风险红线。 |

## 使用边界

- 平台 Agent 重点使用 `03 数据标准/标准到平台字段映射.md`、`03 数据标准/标准规则矩阵.md`、`01 数字化交付总则/工务署标准对齐矩阵.md`、`05 平台协同与流程标准/标准强制对齐工作流.md` 和 `integration-contracts/shared_standard_source_contract.md`。
- Hermes Agent 重点使用 `05 平台协同与流程标准/标准到Hermes边界.md`、`integration-contracts/missing_evidence_policy.md`、`integration-contracts/catalog_tool_contract.md`、`integration-contracts/platform_to_hermes_contract.md`、`05 平台协同与流程标准/标准强制对齐工作流.md` 和 `RISK_RED_LINES.md`。
- Hermes 不需要把完整标准写入 memory；标准文件只作为开发/评审参考。
- 当前仍保持 `catalog-only / read-only / permission-aware / Missing Evidence / draft-only`。

## 强制对齐

- 新增平台字段、字典、枚举、View/API 前，必须先回链到 `03 数据标准/标准到平台字段映射.md`。
- 新增 Hermes 回答能力前，必须先回链到 `05 平台协同与流程标准/标准到Hermes边界.md`。
- 新增 NAS 治理规则前，必须先回链到 `03 数据标准/标准规则矩阵.md`。
- 新增项目模板、Manifest、目录、命名规则前，必须先回链到主标准和 Manifest 标准。
- 新增族库规则前，必须先回链到 `06 公司族库标准化管理专项/` 和工务署对齐矩阵。
- 每次同步后运行 `tools/verify_standard_package_sync.sh` 校验镜像一致。
