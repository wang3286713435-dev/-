# REQ-DP-SVC: 数据服务需求

## 源文件
- SRC-01: 数据中台.docx — 数据服务 章节

## 需求矩阵

| requirement_id | source_file | source_section | requirement_text | platform_domain | module | priority | delivery_phase | acceptance_method | current_platform_fit | gap | risk | owner_role | needs_owner_confirmation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-DP-SVC-001 | SRC-01 | 数据服务-总述 | 严禁上层应用直接连接数据库，统一通过 API 网关访问 | 数字底座 | 数据中台 | P0 | P1 | 架构审查+渗透测试 | 需改造 | 需强制实施 API 网关隔离策略 | 现有直连需迁移，影响面大 | 架构负责人 | 否 |
| REQ-DP-SVC-002 | SRC-01 | 数据服务-原子 | getDeviceStatus 设备状态查询接口 | 数字底座 | 数据中台 | P0 | P1 | 功能测试+性能测试 | 需新增 | 无标准设备状态查询 API | 需对接 IoT 和时序库 | 后端负责人 | 否 |
| REQ-DP-SVC-003 | SRC-01 | 数据服务-原子 | getHistoryData 历史数据查询接口 | 数字底座 | 数据中台 | P0 | P1 | 功能测试+性能测试 | 需新增 | 无标准历史数据查询 API | 跨年查询性能需验证 | 后端负责人 | 否 |
| REQ-DP-SVC-004 | SRC-01 | 数据服务-原子 | controlDevice 设备控制接口 | 数字底座 | 数据中台 | P0 | P1 | 功能测试+安全测试 | 需新增 | 无标准设备控制 API | 控制指令安全性需严格测试 | 后端负责人 | 否 |
| REQ-DP-SVC-005 | SRC-01 | 数据服务-原子 | getPersonProfile 人员档案接口 | 数字底座 | 数据中台 | P1 | P1 | 功能测试 | 可复用 | 现有用户接口可扩展 | 需对接 One ID | 后端负责人 | 否 |
| REQ-DP-SVC-006 | SRC-01 | 数据服务-聚合 | getFloorEnergy 楼层能耗聚合接口 | 数字底座 | 数据中台 | P1 | P1 | 功能测试+性能测试 | 需新增 | 需开发预计算聚合服务 | 聚合计算性能需优化 | 后端负责人 | 否 |
| REQ-DP-SVC-007 | SRC-01 | 数据服务-聚合 | getSpaceOccupancy 空间占用聚合接口 | 数字底座 | 数据中台 | P2 | P2 | 功能测试 | 需新增 | 需接入位置服务和门禁数据 | 占用算法准确性 | 后端负责人 | 否 |
| REQ-DP-SVC-008 | SRC-01 | 数据服务-聚合 | getBuildingSecuritySummary/getEquipmentHealthSummary/getEmergencySituation/getCarbonEmissionSummary | 数字底座 | 数据中台 | P1 | P2 | 功能测试 | 需新增 | 需开发多维度聚合服务 | 依赖各业务模块数据就绪 | 后端负责人 | 否 |
| REQ-DP-SVC-009 | SRC-01 | 数据服务-网关 | OAuth 2.0 Access Token 统一鉴权 | 数字底座 | 数据中台 | P0 | P1 | 安全测试 | 需改造 | 需确认现有鉴权是否满足 OAuth 2.0 | 与现有权限体系整合 | 安全负责人 | 否 |
| REQ-DP-SVC-010 | SRC-01 | 数据服务-网关 | 应用级 QPS 阈值、限流、降级和熔断 | 数字底座 | 数据中台 | P1 | P1 | 性能测试+混沌测试 | 需新增 | 需部署网关层流控组件 | 阈值需根据业务场景调优 | 后端负责人 | 否 |
| REQ-DP-SVC-011 | SRC-01 | 数据服务-网关 | 审计日志：调用方、时间、耗时、参数摘要、结果和异常 | 数字底座 | 数据中台 | P1 | P1 | 功能测试+审计检查 | 可复用 | 现有审计日志可扩展 | 日志量极大需专项存储 | 后端负责人 | 否 |
