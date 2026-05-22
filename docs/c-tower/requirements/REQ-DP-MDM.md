# REQ-DP-MDM: 主数据管理需求

## 源文件
- SRC-01: 数据中台.docx — 主数据管理 章节

## 需求矩阵

| requirement_id | source_file | source_section | requirement_text | platform_domain | module | priority | delivery_phase | acceptance_method | current_platform_fit | gap | risk | owner_role | needs_owner_confirmation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-DP-MDM-001 | SRC-01 | 主数据管理-One ID | 建立全塔唯一 User UUID，实现"一人一档" | 数字底座 | 数据中台 | P0 | P1 | 数据校验 | 可复用 | 当前有用户体系，需扩展为 One ID | 跨系统 ID 映射依赖第三方接口 | 数据架构师 | 否 |
| REQ-DP-MDM-002 | SRC-01 | 主数据管理-One ID | 清洗映射门禁卡号、HR工号、OA账号、微信OpenID | 数字底座 | 数据中台 | P1 | P1 | 数据校验 | 需改造 | 需开发多源 ID 映射和清洗规则 | 第三方系统接口未提供 | 数据架构师 | 是(C-05) |
| REQ-DP-MDM-003 | SRC-01 | 主数据管理-One ID | 支持跨系统人员轨迹追踪与画像分析 | 数字底座 | 数据中台 | P2 | P2 | 功能测试 | 需新增 | 无轨迹分析和画像能力 | 涉及隐私合规 | 数据架构师 | 否 |
| REQ-DP-MDM-004 | SRC-01 | 主数据管理-One Thing | 建立全塔唯一 Device ID，编码格式[系统]-[设备类型]-[楼层]-[流水号] | 数字底座 | 数据中台 | P0 | P1 | 数据校验 | 需新增 | 无设备编码体系 | 编码规范需业主最终确认 | 数据架构师 | 是(C-04) |
| REQ-DP-MDM-005 | SRC-01 | 主数据管理-One Thing | IoT动态数据与资产台账自动关联（维保期、供应商） | 数字底座 | 数据中台 | P1 | P2 | 功能测试+数据校验 | 需新增 | 无资产台账与 IoT 数据关联 | 资产数据来源和格式不确定 | 数据架构师 | 否 |
| REQ-DP-MDM-006 | SRC-01 | 主数据管理-One Map | 建立全塔唯一 Space ID，基于 IFC/BOMA 标准 | 数字底座 | 数据中台 | P0 | P1 | 数据校验 | 需新增 | 无空间编码体系 | 编码规则需业主确认，IFC/BOMA 二选一 | 数据架构师 | 是(C-03) |
| REQ-DP-MDM-007 | SRC-01 | 主数据管理-One Map | IoT设备绑定 Space ID，映射到 BIM 模型房间，报警即定位 | 数字底座 | 数据中台 | P0 | P1 | 功能测试+数据校验 | 需新增 | 无 IoT 设备与 BIM 空间映射 | 依赖 BIM 模型质量和空间数据完整性 | 数据架构师 | 否 |
| REQ-DP-MDM-008 | SRC-01 | 主数据管理-One Map | 告警、工单、人员、设备、事件支持空间定位 | 数字底座 | 数据中台 | P1 | P2 | 功能测试 | 需新增 | 无统一空间定位能力 | 依赖 One Map 先落地 | 数据架构师 | 否 |
| REQ-DP-MDM-009 | SRC-01 | 主数据管理-扩展 | 建立组织、租户、楼栋、楼层、区域、接口、系统、事件、工单、风险源、能耗计量、机器人等扩展主数据 | 数字底座 | 数据中台 | P1 | P1-P2 | 数据校验+文档审查 | 部分可复用 | 组织、租户、楼栋等基础实体可能已有 | 各实体间关联关系复杂 | 数据架构师 | 否 |
