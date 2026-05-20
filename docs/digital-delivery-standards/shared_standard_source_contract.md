# 共享数字化交付标准同源契约 V0.1-beta

本契约用于约束 DigitalDeliveryProject、数据库 / 平台 Agent、Hermes Agent 后续使用同一套数字化交付标准。它只约束标准文档、平台契约和 Agent 回答边界，不授权修改代码、数据库、NAS 或 Hermes memory。

## 1. Canonical Source

标准唯一源头为：

```text
/Users/Weishengsu/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/
```

核心文件：

```text
01 数字化交付总则/工程数字化交付标准V0.1.md
01 数字化交付总则/工务署标准对齐矩阵.md
03 数据标准/标准规则矩阵.md
03 数据标准/标准到平台字段映射.md
05 平台协同与流程标准/标准到Hermes边界.md
05 平台协同与流程标准/标准同步包说明.md
05 平台协同与流程标准/标准强制对齐工作流.md
05 平台协同与流程标准/项目标准绑定规则.md
integration-contracts/missing_evidence_policy.md
integration-contracts/catalog_tool_contract.md
integration-contracts/platform_to_hermes_contract.md
integration-contracts/shared_standard_source_contract.md
RISK_RED_LINES.md
```

## 2. Mirror Targets

数据库 / 平台 Agent 只读同步目录：

```text
/Volumes/vc/Documents/数字化交付平台/docs/digital-delivery-standards/
```

Hermes Agent 只读同步目录：

```text
/Users/Weishengsu/Hermes_memory/docs/digital-delivery-standards/
```

Hermes 目录中的标准文件只作为开发与回答边界参考，不得写入 Hermes long-term memory，不得作为 NAS 文件内容 evidence。

## 3. Source Of Truth Rule

- 若镜像目录与 `DigitalDeliveryProject` 中的标准冲突，以 `DigitalDeliveryProject` 为准。
- 平台和 Hermes 不得在各自目录内长期维护分叉标准。
- 平台字段、View/API、前端下拉框、Agent prompt、Tool description 后续调整时，应回链到本标准包。
- 工务署标准作为公司标准底座主干；工务署分类编码保持不变，公司扩展编码必须另设命名空间。
- 后续平台、Hermes、数据库 Agent、族库治理和项目交付治理均不得维护脱离本标准包的平行标准。

## 4. Enforcement Rule

强制执行“无标准不开发”：

| 变更类型 | 标准回链 |
|---|---|
| 平台字段、字典、枚举、View/API | `03 数据标准/标准到平台字段映射.md` |
| Hermes 回答能力、Tool、prompt | `05 平台协同与流程标准/标准到Hermes边界.md` |
| NAS 治理规则、质量标签、检查项 | `03 数据标准/标准规则矩阵.md` |
| 项目交付模板、Manifest、目录、命名 | `01 数字化交付总则/工程数字化交付标准V0.1.md` / `04 成果交付标准/交付清单Manifest标准.md` |
| 族库、构件、参数、族命名 | `06 公司族库标准化管理专项/` / `01 数字化交付总则/工务署标准对齐矩阵.md` |

缺少标准回链时，该变更只能标记为 `Needs Review` 或 backlog，不得作为 current 能力进入平台、Hermes 或项目交付流程。

## 5. Allowed Use

平台可使用：

- 标准到平台映射。
- 规则矩阵。
- 工务署标准对齐矩阵。
- catalog-only / Missing Evidence / 权限脱敏契约。
- 标准强制对齐工作流。
- 项目标准绑定规则。

Hermes 可使用：

- 标准到 Agent 回答边界。
- Missing Evidence 模板。
- Catalog Tool 契约。
- 风险红线。
- 工务署对齐矩阵中与回答边界有关的部分。
- 标准强制对齐工作流中的 Hermes 执行规则。

Hermes 通常不需要直接消费完整平台字段映射；如使用，也只能作为开发评审参考，不能把平台字段假定为当前可用能力。

## 6. Forbidden Use

- 不得把标准文件当作当前平台验收通过证明。
- 不得把工务署 BIM 数据交付标准中的模型单元、构件属性、合标性、完整性、一致性审核要求写成当前 Hermes 可回答能力。
- 不得把 catalog metadata、文件名、目录关键词或脱敏路径推断为 DWG/RVT/PDF/Office 内容证据。
- 不得将 raw `storage_path`、raw catalog row、NAS 文件正文、DWG/RVT 文件内容或客户敏感材料写入 Hermes memory、向量库、外部日志或标准镜像。
- 不得在项目、族库、平台字典或 Agent prompt 中复制一套与本标准包冲突的本地标准。

## 7. Sync Workflow

每次标准修订后执行：

1. 更新 `DigitalDeliveryProject` 中的 canonical 文件。
2. 更新 `01 数字化交付总则/标准版本与变更记录.md`。
3. 同步到平台镜像目录。
4. 按 Hermes 需要同步 Agent 边界相关文件到 Hermes 镜像目录。
5. 运行 `tools/verify_standard_package_sync.sh`。
6. 若平台或 Hermes 评审发现冲突，反馈回 `DigitalDeliveryProject` 修订，不在镜像目录直接分叉。

## 8. Current Phase

当前仍为：

```text
catalog-only / read-only / permission-aware / Missing Evidence / draft-only
```

本契约不改变当前开发主线，不要求平台或 Hermes 停止开发。
