# 数字化交付标准强制对齐工作流

本工作流用于确保平台、Hermes、数据库 Agent、族库治理和项目交付治理都以 `DigitalDeliveryProject` 中的标准包为准。

## 1. 标准源头

唯一标准源头：

```text
DigitalDeliveryProject/
```

镜像目录：

```text
/Volumes/vc/Documents/数字化交付平台/docs/digital-delivery-standards/
/Users/Weishengsu/Hermes_memory/docs/digital-delivery-standards/
```

镜像目录只读使用，不允许长期分叉。若发现冲突，以 `DigitalDeliveryProject` 为准。

## 2. 无标准不开发

任何开发项进入实施前，必须满足对应回链：

| 变更类型 | 必须回链文件 | 未回链处理 |
|---|---|---|
| 平台字段、字典、枚举、View/API | `03 数据标准/标准到平台字段映射.md` | 标记 Needs Review，不得写成 current。 |
| Hermes 回答能力、Tool、prompt | `05 平台协同与流程标准/标准到Hermes边界.md` | 只能 Missing Evidence 或保持禁用。 |
| NAS 治理规则、质量标签、检查项 | `03 数据标准/标准规则矩阵.md` | 不进入正式治理规则。 |
| 项目交付模板、Manifest、目录、命名 | `01 数字化交付总则/工程数字化交付标准V0.1.md`、`04 成果交付标准/交付清单Manifest标准.md` | 只能作为项目草案。 |
| 族库、构件、参数、族命名 | `06 公司族库标准化管理专项/`、`01 数字化交付总则/工务署标准对齐矩阵.md` | 保持 backlog/future，不宣称已支持自动合规检查。 |

## 3. 平台执行规则

- 平台新增字段必须记录标准来源，优先使用公司标准字段名。
- 工务署分类编码保持不变，公司扩展编码使用独立命名空间。
- `project_override` / `owner_override` 是项目差异的唯一表达方式。
- NAS 历史文件以识别、标注、提示整改为主，不强制立即重命名或迁移。
- 目录级治理可以 current；DWG/RVT/正文/构件级判断必须依证据返回 Missing Evidence。

## 4. Hermes 执行规则

- Hermes 只使用镜像标准作为开发/评审参考。
- Hermes 不把完整标准写入 long-term memory。
- Hermes 不能直接查 DB、生成 SQL、读取 NAS 文件正文、写 DB 或写 NAS。
- 专业、阶段、版本、图号默认是 `current-lineage`，回答时必须说明是“目录/文件名/路径线索”。
- DWG/RVT/PDF/Office 内容问题和 BIM 构件问题必须按 Missing Evidence 策略处理。

## 5. 数据库 Agent 执行规则

- 评审标准字段是否已有稳定 View/API 支撑。
- 不存在或不稳定字段标 backlog / future / placeholder。
- 不得把当前不存在的稳定字段写成 current。
- 对 `display_path/path_hint`、Manifest、DeliveryPackage、source_modified_at、checksum_status、preview_status 等字段继续按实际 View/API 状态分级。

## 6. 项目交付治理执行规则

- 新项目默认绑定公司基础标准版本。
- 甲方、工务署、地方、顾问差异只能登记为项目覆盖。
- 新项目命名、专业、阶段、Manifest、交付包和族库规则从标准底座继承。
- 历史项目不强制整改，只做识别、标注和整改建议。

## 7. 族库治理执行规则

- 族库标准不单独另起体系。
- 族库分类、命名、参数、制作、审核、发布、权限、版本管理均纳入 `06 公司族库标准化管理专项/`。
- `family_library_code` 依附工务署构件分类或运维设备分类，不改写工务署编码。
- 族库自动合规检查在没有 `rvt_parse_evidence` / `component_evidence` / `manual_evidence` 前保持 future。

## 8. 校验要求

标准变更后必须运行：

```bash
bash tools/verify_standard_package_sync.sh
```

校验必须确认：

- canonical source 与平台镜像一致。
- canonical source 与 Hermes 镜像一致。
- 标准包包含同源契约、规则矩阵、平台映射、Agent 边界、Missing Evidence、Catalog Tool 契约和风险红线。

失败时不得让平台或 Hermes 基于镜像继续开发。
