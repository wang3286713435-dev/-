# 风险红线

以下红线用于约束 Platform / Data Steward、Hermes Agent 和数字化交付标准子项目的共同边界。

1. Hermes 不直接查询 `data_file_resources` 等业务底表。
2. Hermes 不生成任意 SQL。
3. 平台不把 NAS catalog row 写入 Hermes memory。
4. catalog metadata 不得作为文件正文 evidence。
5. `storage_path` 默认不得暴露给客户、Agent memory、向量库或外部日志。
6. `updated_at` 不得解释为 NAS 文件本体 mtime。
7. `process_status` 不得解释为 semantic index status。
8. 当前不得承诺 DWG/RVT 内容理解。
9. 当前不新增生产级 NAS 向量库。
10. 所有正文类问题必须支持 Missing Evidence。
11. 标准草案不等于当前全部实现。
12. 数字化交付标准的规则必须区分 current / current-lineage / backlog / future。
13. `related_file_ids` 不得被解释为文件内容已读取或已索引。
14. 用户反馈不得把客户敏感内容、raw path、raw catalog row 或文件正文写入 Hermes memory。
15. 平台、Hermes、数据库 Agent、族库治理和项目交付治理不得维护脱离 `DigitalDeliveryProject` 的平行标准。
16. 未在共享标准中登记的字段、字典、枚举、View/API、Agent 回答能力或治理规则，不得作为正式开发依据。
17. 项目差异不得复制一套新标准，只能通过 `project_override` / `owner_override` 或项目标准绑定记录。

## 处理规则

- 破坏红线的建议必须标记为 Rejected 或 Needs Review。
- 涉及红线变更必须由用户 / 架构协调者确认。
- 涉及架构边界变更必须新增或更新 ADR。
