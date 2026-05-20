# 主 Agent Backlog

## 已通过

### Phase 1：平台基础工程与 `core` 底座

状态：已通过验收。

通过内容：

- 工程骨架
- 基础设施
- 后端构建与启动
- 前端构建与启动
- 登录、刷新 token、当前用户、项目切换、未登录拦截
- 统一响应、`traceId`
- 样板数据、审计日志
- 前端登录到主框架、项目切换、刷新保持

## 非阻塞硬化项

这些事项不阻塞第二阶段，但需要在近期持续收口：

1. 清理 Flyway MySQL 弃用警告
2. 清理 MyBatis-Plus 无 mapper 启动提示
3. 清理 Spring Security 开发密码日志噪音
4. 在 `Node 20 LTS` 基线上补前端回归
5. 后续出现分页接口后补分页响应契约测试
6. 逐步补后端集成测试、前端冒烟测试和 CI 前置

## Catalog-only / Jarvis 契约补强项

状态：进入 backlog，不阻塞当前 `批次 6A`。

来源：

- `handoff/main-agent/catalog-only-architecture-suggestion-review.md`

补强项：

1. 文档化 `file_id` 为后续跨模块、Jarvis 查询结果、用户反馈、预览和未来索引回指的主关联键，避免使用 `storage_path` 作为关联键。
2. 后续补 `display_path / path_hint / masked_storage_path` 或 API 层 `include_sensitive_path=false`，客户环境默认不暴露真实 NAS 路径。
3. 后续补 `source_modified_at / file_modified_at`，避免把资产记录 `updated_at` 误说成文件本体 mtime。
4. 后续补 `checksum_status`，区分 `pending / available / failed / skipped`。
5. 后续补文件能力标记：`catalog_search / preview_available / full_text_search / semantic_search / component_search`。
6. 后续补 catalog query feedback 模型，记录 query、返回文件、用户选择和反馈，但不得写长期 memory、向量库、搜索库或外部日志。
7. 未来进入 selective indexing / semantic index 阶段时，再单独设计 `semantic_index_status / semantic_index_version / semantic_indexed_at / embedding_model`。
8. 前端和客户材料统一文案口径：当前只能称“资产目录助手 / 项目文件资产检索”，不得宣称已具备图纸理解、模型理解、BIM 构件问答或 NAS 全文智能问答。

当前禁止继续保持：

- 不把 DWG/RVT 当普通文本 chunk。
- 不把 catalog metadata 伪装成正文 evidence。
- 不写 OpenSearch、Qdrant、长期 memory 或生产级 NAS 向量库。
- 不让 Jarvis/Hermes 自动执行 DB CRUD、NAS CRUD、审批、删除、修复或扫描。

## 下一阶段

## 当前裁决：一期后端数据治理已收口

早期 `master-data` 与 MVP backlog 保留为历史记录。`一期内部 BIM 资产管理试点` 的后端数据治理批次已经完成并通过测试 agent 复验，当前不再继续扩大一期后端范围。下一步应从“真实 NAS 小批量试点导入”和“企业 agent 对接确认”中选择推进。

### Phase A：数据底座与 NAS 扫描入库

优先级：P0

状态：已通过测试 agent 复验。

范围：

1. 项目清单维护 API。
2. NAS 路径映射维护 API。
3. CSV/xlsx 项目清单 + 路径映射批量导入。
4. NAS 扫描任务表与任务状态。
5. 扫描候选记录/待审核队列。
6. 正式资产库扩展。
7. 高置信自动入库：路径映射命中，或路径/文件名精确包含项目编码。
8. 低置信进入待审核：人工修正项目归属、文件类型、专业、版本。
9. 文件白名单：`.rvt`、`.dwg`、`.ifc`、`.nwd`、`.nwc`、`.dxf`、`.pdf`。
10. 默认分类：`.rvt/.ifc/.nwd/.nwc` 为 `MODEL`，`.dwg/.dxf/.pdf` 为 `DRAWING`，`.dwg` 可人工改为 `MODEL`。
11. MySQL SQL View 基础：`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView`。

验收标准：

1. 20 个项目清单可导入。
2. NAS 路径映射可导入和维护。
3. 模拟 NAS 扫描可产生自动入库和待审核两类结果。
4. 待审核记录审核后进入正式资产库。
5. OpenAPI 可看到全部接口。
6. 关键动作有审计。

当前交接入口：

- 开发 agent：`handoff/dev-agent/current-prompt.md`
- 测试 agent：`handoff/test-agent/current-prompt.md`

通过记录：

1. `./mvnw -pl delivery-app -am -DskipTests package` 通过。
2. `/actuator/health` 返回 `{"status":"UP"}`。
3. Flyway V8/V9 已应用，4 个 SQL View 在 MySQL 中存在。
4. `scripts/dev/check-bim-asset-batch1.sh` 通过并输出 `bim asset batch1 ok`。
5. Claude Code 通过 Ralph Loop 完成开发，主 agent 监控记录见 `handoff/main-agent/development-log.md`。
6. 测试 agent 复验报告确认上一轮 `P0-1` 扫描任务越权、`P0-2` 待审核候选越权、`P1` `confidenceLevel`、`P2` OpenAPI 参数名全部通过复验，当前无 P0，建议进入批次 2。

本批明确不包含：

- 前端资产页面。
- checksum 异步 worker。
- 容量统计完整看板。
- 事件流增量同步 API。
- agent API Key。
- 删除审批、隔离、恢复和永久删除。

### Phase A.1：NAS 项目目录发现与真实结构适配

优先级：P0

状态：已并入 `Phase A-Tail` 并通过验收。

范围：

1. 真实 NAS 根路径为 `/Volumes/zyzn/卓羽智能项目`。
2. 支持一级目录发现 dry-run，输出项目编码建议、项目名称建议、路径映射建议、目录类型和需人工确认原因。
3. `数字-项目名` 目录优先使用数字作为项目编码。
4. 数字前缀重复时生成稳定短哈希编码或进入人工确认。
5. 无编号、弱编号、投标、参考、样板、标准、临时、专题目录不得直接自动成为正式项目。
6. 识别标准二级目录：`00_工作进度`、`01_文件收发`、`02_项目资源`、`03_过程文件`、`04_共享文件`、`05_发布文件`、`06_归档文件`、`07_浏览动画`。
7. 首批试点目录：`101-C塔`、`98-深圳口岸项目`、`99-丰图既有建模项目`。
8. 只读影子导入，不移动、不改名、不删除 NAS 文件。

验收标准：

1. dry-run 能识别模拟或真实的三个试点目录。
2. 能输出项目台账建议和路径映射建议，但 dry-run 不写正式资产。
3. 重复编号和无编号目录进入人工确认，不发生静默覆盖。
4. 执行导入后可在 `core_projects` 与 `data_asset_project_path_mappings` 查到已确认项目和路径。
5. 现有 `check-bim-asset-batch1.sh` 不回归。

### Phase A.2：文件类型扩展、低价值文件治理与幂等扫描

优先级：P0

状态：已并入 `Phase A-Tail` 并通过验收。

范围与验收标准见 `Phase A-Tail`。

### Phase A-Tail：批次 1 尾巴收口

优先级：P0

状态：已通过测试 agent 复验。

范围：

1. 审计并完善已有 `/api/data-steward/assets/nas-projects:discover` 能力。
2. 支持真实 NAS 一级目录 dry-run，输出项目编码建议、项目名称建议、路径映射建议、目录类型、标准二级目录线索和人工确认原因。
3. 非 dry-run 只对高置信 `READY` 目录创建或更新项目与路径映射；重复编号、参考资料、无编号和临时专题目录不得静默成为正式项目。
4. 扩展资产文件分类：文档、表格、汇报、展示模型和归档包。
5. 低价值文件、锁文件、系统缓存文件和空文件不得自动进入正式资产库。
6. 临时/转换目录中的文件即使位于项目路径下，也不得高置信自动入库。
7. 同项目同 `storage_uri` 重扫必须幂等，不产生重复正式资产，只更新元数据和 `last_verified_at`。
8. 补齐批次 1 历史资产中缺失的 `confidence_level`，避免后续 agent 读取到新旧混杂口径。
9. 增加 `scripts/dev/check-bim-asset-batch1-tail.sh`，并保证 `scripts/dev/check-bim-asset-batch1.sh` 继续通过。

验收标准：

1. 模拟 NAS 根目录能覆盖 `101-C塔`、`98-深圳口岸项目`、`99-丰图既有建模项目` 三个试点目录。
2. dry-run 不写正式项目和路径映射。
3. 非 dry-run 只写入高置信目录；重复编号和参考目录必须留在需人工确认或参考状态。
4. 扩展文件类型可正确进入正式资产分类。
5. `.DS_Store`、`Thumbs.db`、`desktop.ini`、`~$` 锁文件、空文件不进入正式资产。
6. 重扫后同一 `storage_uri` 只有一条正式资产。
7. OpenAPI 包含 NAS 项目发现接口和相关响应字段。
8. 权限、审计、批次 1 基础链路不回归。

### Phase B：异步任务、checksum、统计与事件流

优先级：P0

状态：已通过测试 agent 复验。

范围：

1. 数据库任务表 + 应用内 worker。
2. 任务类型：`NAS_SCAN`、`CHECKSUM_CALC`、`QUARANTINE_CLEANUP`、`PERMANENT_DELETE`。
3. 任务状态、进度、失败原因、重试次数。
4. checksum 异步补齐。
5. 容量统计 API。
6. 审计/事件流增量同步 API。

验收标准：

1. 大文件首次扫描不阻塞 checksum。
2. checksum 任务可追踪、失败可重试。
3. 容量统计可按项目、专业、模型/图纸类型聚合。
4. agent 或外部系统可按事件 ID 或时间窗口拉取变化。

### Phase C：企业 agent、审批与受控物理删除

优先级：P0

状态：已通过测试 agent 复验。首轮验收发现 `ALL_PROJECTS` API Key 创建权限 P0，修复后再次验证通过，当前无 P0/P1/P2。

范围：

1. agent API Key 接入。
2. API Key 按项目范围授权。
3. agent 查询、触发扫描/checksum 任务、提交标注或删除申请。
4. 标注申请和删除申请人工审核。
5. 逻辑删除审计。
6. 物理删除申请、审批、隔离、恢复和永久删除。
7. 隔离区保留 `30 天`。

验收标准：

1. agent 不能访问未授权项目。
2. agent 不能直接修改正式资产。
3. agent 不能绕过审核物理删除 NAS 文件。
4. 审核通过后文件先进入隔离区，隔离期内可恢复。
5. 删除申请、审批、隔离、恢复、永久删除全链路可审计。

### Phase D：真实 NAS 试点后数据治理补强

优先级：P1

状态：部分完成。`100` 待审核候选已处理，`ModelAssetView` P1 已修复；`101` 导入策略和只读挂载仍待处理。

背景：

- `105-启航华居项目` 与 `100-深圳市二十八高项目` 已完成真实 NAS 元数据写入。
- `100` 曾产生 `293` 条低置信待审核候选，已全部审核通过并保留 `LOW` 置信度。
- 开发库中历史测试项目编码 `101` 与真实 `101-C塔` 冲突，真实导入前需要清洁数据库、换临时唯一编码，或先清理旧测试项目。
- `ModelAssetView` 已通过 `V13__model_asset_view_include_nas_files.sql` 补强，覆盖 NAS 扫描登记的 `file_kind='MODEL'` 文件。

范围：

1. 已完成：处理 `100` 的低置信待审核候选，形成可复用审核规则。
2. 为真实 `101-C塔` 制定导入策略：清洁数据库、临时唯一编码或测试数据清理。
3. 已完成：增强企业 agent 稳定读模型，让 `ModelAssetView` 覆盖 NAS 自动/审核入库模型文件，并同步文档。
4. 增加真实 NAS 试点验收脚本，覆盖扫描任务、待审核、SQL View、事件流和审计。
5. 明确全量扫描前的只读 NAS 账号或只读挂载要求。

验收标准：

1. 已通过：agent 能通过稳定 SQL View 查到 `100`、`105` 的 NAS 模型文件。
2. 已通过：`100` 的待审核候选已审核通过，且动作可审计。
3. `101` 不再与历史测试数据混用。
4. 全量试点前不再存在可误触发的真实 NAS 待运行扫描任务。

### Phase E：非标准 NAS 资料 Agent 辅助治理

优先级：P1

状态：治理清单底座已完成；Agent 建议生成与自动治理仍待企业 agent 接入后启动。当前不导入正式资产库。

背景：

- 用户已确认 `98`、`95`、`99` 等非标准目录暂时不导入数据库。
- `98-深圳口岸项目` 可能需要按口岸或子目录拆分项目。
- `95-*` 和 `99-*` 存在重复编号，必须先制定唯一编码和项目归属。
- 投标、参考、未知命名目录需要判断是否属于正式项目资产，不能直接进入项目资产台账。
- 这类目录后续作为平台“数据治理能力”的重要场景：由企业 agent 辅助识别，人工监控确认后再入库。

范围：

1. 已完成：非标准目录只做只读发现和治理清单登记，不写正式 `core_projects`、`data_file_resources`。
2. 企业 agent 读取 NAS 目录结构、文件名、扩展名、路径线索、历史项目编码和已有项目资产，生成治理建议。
3. 治理建议至少包含：建议项目编码、建议项目名称、是否拆分、建议导入范围、忽略目录、风险原因、置信度。
4. 已完成：治理清单可记录治理状态、风险类型、Agent 建议、人工结论和依据。
5. 人工审核治理建议后，才能创建项目、路径映射和扫描任务。
6. 已确认导入的范围必须可追溯到 agent 建议和人工确认记录。
7. 未确认目录继续留在非标准资料清单，不进入正式资产库和稳定 SQL View。

验收标准：

1. `98` 可输出“单项目导入”或“拆成多个项目”的建议及依据。
2. `95`、`99` 可输出唯一编码建议，且不会覆盖已有项目编码。
3. 投标、参考、未知目录默认不进入正式资产库。
4. agent 建议和人工确认记录可审计。
5. 未经人工确认的非标准目录不能被自动扫描成正式资产。
6. 治理完成后再入库的文件可进入现有 `ProjectAssetView`、`FileAssetView`、`ModelAssetView`，并保留来源链路。

## 历史 Backlog：早期 master-data / MVP 路线

以下内容保留历史验收记录和上下文，不作为下一轮开发优先级。下一轮以 `当前裁决：一期后端数据治理主线批次与真实 NAS 增强批次` 为准。

### Phase 2.1：`master-data` 最小闭环

优先级：P0

状态：已通过测试 agent 验收。

范围：

1. 工程管理部位树
2. 节点类型配置
3. 节点类型锁定
4. 标准状态查询
5. 前端页面入口与前置条件提示

验收前返工：

1. 修复 `masterdata_section_nodes` 逻辑删除同编码重建后的二次删除唯一键冲突风险
2. 补充脚本覆盖“同编码创建、删除、重建、再次删除”

验收结果：

1. Flyway V4 已应用，`delete_token` 方案通过验证
2. 同编码“创建 -> 删除 -> 重建 -> 再删除”通过
3. 第一阶段最小链路无回归
4. 第二阶段 master-data 最小闭环无回归

不包含：

- 交付物定义完整业务
- 交付物类型完整业务
- 交付物属性完整业务
- 目录模板完整业务
- 关联模型完整业务

### Phase 2.2：交付物标准最小闭环

优先级：P0

状态：待开发 agent 执行。

范围：

1. 交付物定义 `DeliverableDefinition`
2. 交付物类型 `DeliverableType`
3. 交付物属性 `DeliverableAttribute`
4. 目录模板 `DirectoryTemplate`
5. 标准状态接口扩展
6. 前端“交付物标准”页面入口与最小管理界面

前置规则：

1. 节点类型必须全部锁定后，才能创建或维护交付物定义、类型、属性和目录模板
2. 查询接口允许返回空列表和前置条件状态，便于前端展示

不包含：

- 文件上传
- 文件资源管理
- 模型集成
- 管理对象
- 关联模型正式业务
- 文档/图纸交付视图
