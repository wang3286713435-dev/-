# 项目标准绑定规则

本规则用于避免不同项目各自复制一套标准，确保所有项目默认继承公司数字化交付标准底座。

## 1. 默认绑定

每个新项目默认绑定：

| 字段 | 默认值 |
|---|---|
| `standard_source` | `DigitalDeliveryProject` |
| `company_standard_version` | `V0.1-beta` |
| `base_standard_profile` | 公司基础数字化交付标准 |
| `gws_alignment` | 工务署数字化交付系列标准主干 |
| `override_policy` | 只允许 `project_override` / `owner_override` |

## 2. 项目覆盖

项目存在甲方、工务署、地方、顾问或合同要求时，不复制新标准，必须登记为覆盖：

| 覆盖字段 | 含义 | 当前阶段 |
|---|---|---|
| `project_override` | 项目级特殊要求。 | backlog |
| `owner_override` | 甲方/业主/地方/顾问标准覆盖。 | backlog |
| `override_source` | 覆盖来源文件或标准名称。 | backlog |
| `override_scope` | 覆盖范围，如专业、阶段、命名、Manifest、族库。 | backlog |
| `override_evidence` | 人工确认或合同/标准依据。 | backlog |

Hermes 不裁决冲突，只能说明存在覆盖或返回 Needs Review / Missing Evidence。

## 3. 项目初始化检查

新项目初始化时应检查：

- 是否绑定公司基础标准版本。
- 是否需要工务署、甲方、地方或顾问覆盖。
- 是否继承标准专业编码和中文别名。
- 是否继承工务署阶段映射和公司阶段别名。
- 是否启用推荐命名、目录、Manifest 和交付包模板。
- 是否需要族库标准、企业资产编码、成本编码或采购编码扩展。

## 4. 历史项目处理

历史 NAS 项目不立即强制整改：

- 文件名、路径、目录只作为 `current-lineage` 线索。
- 平台可标记弱命名、泛名文件、缺专业、缺阶段、缺版本。
- 需要整改时输出整改建议，不自动重命名、不自动迁移目录。
- 有旧标准或项目标准时，记录为 `legacy_source_id` 或 `project_override`。

## 5. 平台落地字段建议

| 标准字段 | 用途 | 当前阶段 |
|---|---|---|
| `standard_binding_id` | 项目绑定的标准记录 ID。 | backlog |
| `company_standard_version` | 公司基础标准版本。 | backlog |
| `standard_profile_id` | 标准配置档案。 | backlog |
| `project_override` | 项目覆盖规则。 | backlog |
| `owner_override` | 甲方/业主/地方/顾问覆盖。 | backlog |
| `legacy_source_id` | 历史标准或旧项目依据。 | backlog |
| `replacement_status` | legacy / active / replaced / reference。 | backlog |

这些字段未进入稳定 View/API 前，Hermes 不得宣称项目标准绑定已正式落地。
