# Hermes 数据库与 NAS 接入交接文档

日期：2026-05-09  
提交方：数据库 / NAS / 数据治理开发团队  
接收方：Hermes 主仓库、Hermes_memory、Hermes Agent 开发团队  
用途：用于后续与 Hermes_memory / Hermes Agent 进行数据库、NAS、资产目录、索引治理与 MCP/工具协议耦合评审。

## 0. 结论

当前建议路线是：**NAS 和业务数据库继续作为 source of truth，数字化交付平台负责资产目录、扫描、审核、checksum、事件流和审计，Hermes_memory 作为 memory index / governance layer 接入稳定资产目录、派生索引、chunk、embedding、BM25、citation、权限快照、版本和 trace。**

不要让 Hermes_memory 复制整个 NAS，不要让 Hermes Agent 直接增删改 NAS，也不要默认把 10TB NAS 全量解析、全量 embedding、全量写入向量库。

推荐最小链路：

`NAS / 原始业务库 -> 资产目录与扫描治理 -> 稳定 SQL View / REST API / 事件流 -> Hermes_memory 索引治理 -> Hermes pre-model 检索上下文 -> Hermes runtime`

## 1. 当前数据库 / NAS 技术栈概览

### 1.1 当前数据库与基础设施

当前数字化交付平台为 Java + Spring Boot 模块化单体，数据库迁移使用 Flyway，接口文档使用 Springdoc/OpenAPI。

本地开发基础设施：

- MySQL：`mysql:8.0.39`
- Redis：`redis:7.4`
- MinIO：`RELEASE.2025-04-22T22-12-26Z`
- 后端默认端口：`8080`
- 数据库默认库名：`delivery_platform`
- 数据库默认连接：`jdbc:mysql://localhost:3306/delivery_platform`
- 数据库迁移目录：`backend/delivery-app/src/main/resources/db/migration`

当前已确认使用 MySQL 作为平台业务库。当前项目没有使用 PostgreSQL、SQL Server、MongoDB、Neo4j、PostGIS 作为生产依赖。OpenSearch / Qdrant 属于 Hermes_memory 侧已有能力，不属于本平台当前业务库。

Redis 和 MinIO 已在基础设施中预留。当前 NAS 接入路线仍是“NAS 原地接管 + 数据库保存元数据和路径”，不是默认把 NAS 文件搬到 MinIO。

### 1.2 NAS / 文件服务器

当前真实公司 NAS 通过 SMB 协议访问。

已观察到的挂载信息：

- SMB 服务：`smb://192.168.1.181/zyzn`
- 本机挂载点：`/Volumes/zyzn`
- 真实项目根路径：`/Volumes/zyzn/卓羽智能项目`
- 当前访问原则：只读分析、只读扫描，不移动、不改名、不删除原文件。

当前未发现 NFS、WebDAV、S3-compatible 作为 NAS source of truth。MinIO 当前只是平台未来对象存储适配的基础设施，不是公司真实 NAS 的替代。

### 1.3 身份、权限与业务表

当前平台已有本地身份与项目级 RBAC 基础：

- 用户表：`core_users`
- 角色表：`core_roles`
- 权限表：`core_permissions`
- 用户项目角色：`core_user_project_roles`
- 项目表：`core_projects`
- 审计表：`core_audit_logs`

当前身份系统为本地账号 + JWT。企业微信、LDAP/AD、OIDC 等统一身份系统只是后续扩展方向，尚不应作为当前 Hermes 对接前提。

当前文件资产相关表包括：

- `data_file_resources`
- `data_storage_roots`
- `data_asset_project_path_mappings`
- `data_asset_scan_tasks`
- `data_asset_scan_candidates`
- `data_asset_import_jobs`
- `data_asset_import_rows`

当前稳定读模型包括：

- `ProjectAssetView`
- `FileAssetView`
- `ModelAssetView`
- `AuditEventView`

### 1.4 备份、快照、归档与权限审计

当前开发环境使用 Docker volume 保存 MySQL、Redis、MinIO 数据。生产级数据库备份、NAS 快照、归档策略、ACL 审计策略尚未固化为平台交付能力。

当前已有平台审计表和事件流设计方向，但 NAS 原生 ACL 快照、NAS 变更事件、生产级备份恢复仍需要基础设施侧确认。

## 2. NAS 文件资产目录现状

### 2.1 总量与目录结构

业务目标按公司内部几百个 BIM 项目和约 `10TB` 文件资产设计。当前已抽样分析的真实 NAS 根路径为：

`/Volumes/zyzn/卓羽智能项目`

当前可见一级项目/资料目录约 27 个，包括：

- `101-C塔`
- `98-深圳口岸项目`
- `99-丰图既有建模项目`
- `115-深圳宝安国际机场T2航站区及配套设施工程航站区工程施工总承包2标段`
- `116-港中文（深圳）医学院智能化`
- `投标资料`
- `投标项目文件夹（实施方案参考）`

真实 NAS 同一共享盘还包含：

- `卓羽智能标准文件`
- `卓羽智能样板文件`
- `卓羽智能软件`
- `卓羽智能员工`
- `谷雨云资料`

Hermes_memory 当前不应把整个共享盘都视为项目知识库。建议先以 `卓羽智能项目` 为项目资产源，以 `标准文件/样板文件/谷雨云资料` 作为后续企业知识库候选源，分批治理。

### 2.2 主要文件类型

真实 NAS 包含：

- Office 文档：`.doc`、`.docx`、`.wps`
- PDF：`.pdf`
- 表格：`.xls`、`.xlsx`
- 汇报材料：`.ppt`、`.pptx`
- BIM / CAD：`.rvt`、`.ifc`、`.dwg`、`.dxf`、`.nwd`、`.nwc`
- 三维展示：`.glb`、`.gltf`
- 压缩包：`.zip`、`.rar`
- 图片、扫描件、倾斜摄影 / OSGB、音视频等后续可能存在。

当前首批平台扫描白名单是：

`.rvt`、`.dwg`、`.ifc`、`.nwd`、`.nwc`、`.dxf`、`.pdf`

真实 NAS 适配增强已建议扩展：

`.doc`、`.docx`、`.xls`、`.xlsx`、`.ppt`、`.pptx`、`.glb`、`.gltf`、`.zip`、`.rar`、`.wps`

### 2.3 组织规则与命名规范

真实项目目录存在一定规则，但不是完全规范化。

常见一级目录规则：

- `数字-项目名`，如 `101-C塔`
- `数字-长项目名`，如 `115-深圳宝安国际机场T2...`
- 弱编号或无编号目录，如 `深城交`、`投标资料`
- 参考/投标/样板类目录混在项目目录中。

常见二级目录规则：

- `00_工作进度`
- `01_文件收发` 或 `01-文件收发`
- `02_项目资源`
- `03_过程文件`
- `04_共享文件`
- `05_发布文件`
- `06_归档文件`
- `07_浏览动画`

当前文件名包含日期、项目名、专业、版本、提交批次的情况较多，但并非全量一致。大量文件适合先做资产目录和元数据索引，不适合立即进入全文/语义索引。

### 2.4 数据质量问题

已观察到或应默认考虑：

- 重复文件。
- 压缩包和解压目录并存。
- 临时目录，如 `临时文件`、`新建文件夹`、`转换`。
- Office 锁文件，如 `~$...docx`。
- 系统缓存文件，如 `.DS_Store`、`Thumbs.db`、`desktop.ini`。
- 旧版本文件、过程文件、整改文件、阶段汇报混放。
- 部分目录属于投标/参考资料，不应自动归入正式项目。

### 2.5 路径稳定性与 NAS 元数据

当前 NAS 路径不是强事务业务 ID，存在人工移动、重命名、删除的可能。路径可以作为 source_path，但不能作为唯一长期身份。

建议：

- 初期以 `source_path + size + modified_at` 识别变化。
- 对高价值文件异步计算 `content_hash`。
- 后续使用 `content_hash + source_system + version_key` 辅助识别移动/重命名。
- `last_seen_at` 用于判断 missing/stale。

当前尚未确认 NAS 是否可稳定提供 owner、ACL、hash 或变更事件。SMB 文件系统通常可读取基础 mtime/size，ACL 与 owner 是否可映射到企业身份需另行验证。

## 3. 建议的数据分层设计

### 3.1 分层定义

| 层级 | 定位 | 是否全量 | 当前建议 |
|---|---|---|---|
| `source_of_truth` | NAS / 原始业务数据库 | 是 | 保持原地，不复制 10TB 文件 |
| `asset_catalog` | 文件资产目录 | 应尽量全量 | 首批全量覆盖项目根目录文件元数据 |
| `metadata_index` | 文件名、路径、类型、项目、大小、mtime、hash、权限标签 | 应尽量全量 | Hermes_memory 可优先消费 |
| `preview_index` | 标题、目录、摘要、前几页、关键页 | 选择性 | 高价值目录和可解析文档优先 |
| `full_text_index` | 全文解析索引 | 选择性 | 文档/PDF/表格/汇报按目录和价值分批 |
| `semantic_index` | chunk + embedding + vector index | 严格选择性 | 不做 10TB 全量 embedding |
| `citation_index` | 页码、sheet/cell、slide、section、paragraph 定位 | 选择性 | 仅对进入 preview/full_text/semantic 的文件建立 |
| `audit_log` | 扫描、解析、索引、权限过滤、人工确认、操作计划记录 | 必须全量记录关键动作 | 平台与 Hermes_memory 均需保留 trace |
| `operation_plan` | Agent 生成但需人工审批的操作计划 | 按需 | 不直接执行真实文件写操作 |

### 3.2 哪些必须全量

短期建议尽量全量：

- `asset_catalog`
- `metadata_index`
- `audit_log` 的关键动作
- scan checkpoint
- `last_seen_at`
- lifecycle status

### 3.3 哪些必须选择性

必须选择性或按需：

- `preview_index`
- `full_text_index`
- `semantic_index`
- `citation_index`
- 大文件 checksum
- OCR
- BIM/CAD 解析
- 压缩包深度解包
- 音视频转写

原则：先知道“有什么、在哪、谁能看、是否最新”，再决定是否解析内容。

## 4. Asset Catalog 最小字段建议

### 4.1 推荐 Schema

```sql
CREATE TABLE asset_catalog (
    asset_id BIGINT PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL,
    source_path TEXT NOT NULL,
    storage_location VARCHAR(512) NULL,
    file_name VARCHAR(255) NOT NULL,
    file_ext VARCHAR(32) NULL,
    mime_type VARCHAR(128) NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    content_hash VARCHAR(128) NULL,
    created_at TIMESTAMP NULL,
    modified_at TIMESTAMP NULL,
    last_seen_at TIMESTAMP NOT NULL,
    owner VARCHAR(128) NULL,
    department_id VARCHAR(128) NULL,
    project_id BIGINT NULL,
    customer_id VARCHAR(128) NULL,
    permission_tags TEXT NULL,
    confidentiality_level VARCHAR(32) NULL,
    version_key VARCHAR(128) NULL,
    is_latest TINYINT NOT NULL DEFAULT 1,
    parent_asset_id BIGINT NULL,
    index_status VARCHAR(32) NOT NULL DEFAULT 'CATALOG_ONLY',
    parse_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED',
    semantic_index_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED',
    citation_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED',
    lifecycle_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    data_quality_flags TEXT NULL,
    derived_metadata_source VARCHAR(64) NOT NULL DEFAULT 'NAS_SCAN'
);
```

### 4.2 与当前已有字段映射

| 建议字段 | 当前已有/可映射 | 状态 |
|---|---|---|
| `asset_id` | `data_file_resources.id` | 已有 |
| `source_system` | `storage_provider/source_type` | 已有，需规范枚举 |
| `source_path` | `storage_uri/storage_key/logical_path` | 已有 |
| `storage_location` | NAS 根路径 / provider 信息 | 部分已有 |
| `file_name` | `original_name` | 已有 |
| `file_ext` | 可由文件名计算，View 已提供 | 已有 |
| `mime_type` | `mime_type` | 已有但多为默认值 |
| `file_size` | `size_bytes` | 已有 |
| `content_hash` | `checksum` | 已有字段，异步补齐 |
| `created_at` | `created_at` | 已有 |
| `modified_at` | 扫描候选 `last_modified_at`，正式资产需补齐字段或派生 | 部分已有 |
| `last_seen_at` | `last_verified_at` | 已有 |
| `owner` | NAS owner 尚未确认 | 待补 |
| `department_id` | 当前无统一组织映射 | 待补 |
| `project_id` | `project_id` | 已有 |
| `customer_id` | `owner_org_name` 可弱映射 | 待规范 |
| `permission_tags` | 当前无 NAS ACL 快照 | 待补 |
| `confidentiality_level` | 当前无密级字段 | 待补 |
| `version_key` | `version_no` | 部分已有 |
| `is_latest` | 当前未显式维护 | 待补 |
| `parent_asset_id` | 当前未显式维护 | 待补 |
| `index_status` | 可由 Hermes_memory 维护 | Hermes_memory 侧新增 |
| `parse_status` | 当前平台处理状态较粗 | Hermes_memory 侧新增 |
| `semantic_index_status` | 当前无 | Hermes_memory 侧新增 |
| `citation_status` | 当前无 | Hermes_memory 侧新增 |
| `lifecycle_status` | `deleted/process_status/review_status` 可映射 | 部分已有 |
| `data_quality_flags` | 低价值文件/临时目录规则已有方向 | 需规范 |
| `derived_metadata_source` | `source_type` | 已有 |

### 4.3 字段优先级

短期必须补齐：

- `asset_id`
- `source_system`
- `source_path`
- `file_name`
- `file_ext`
- `file_size`
- `modified_at`
- `last_seen_at`
- `project_id`
- `index_status`
- `lifecycle_status`
- `data_quality_flags`

中期补齐：

- `content_hash`
- `version_key`
- `is_latest`
- `permission_tags`
- `confidentiality_level`
- `citation_status`

后置：

- `owner`
- `department_id`
- `customer_id`
- `parent_asset_id`
- 完整 ACL snapshot

## 5. 权限与身份映射

### 5.1 当前权限现状

平台已有本地 RBAC 和项目级权限：

- 用户
- 角色
- 项目
- 用户项目角色
- 权限码

当前 NAS 权限还没有完整映射到平台用户、部门、项目、角色和密级。SMB ACL 是否可读取并稳定映射到企业身份，需要基础设施侧确认。

### 5.2 Hermes 查询权限原则

必须满足：

- 无权限内容不得进入 prompt。
- 权限过滤必须发生在 pre-model 检索阶段。
- Hermes_memory 检索结果返回给 Hermes 主链路前，必须完成权限过滤。
- SQL View / REST API 查询必须支持按用户或 API Key 项目范围过滤。
- 只读数据库账号不能绕过权限读取所有敏感资产再交给模型过滤。

### 5.3 权限过滤层级

推荐三层过滤：

1. 平台层：API Key / 用户项目授权过滤。
2. Hermes_memory 层：permission_tags / ACL snapshot / project scope 过滤。
3. Hermes runtime 层：answer guard 再检查 citation 与可见性。

不要只依赖模型自己判断权限。

### 5.4 最小可行方案

如果 NAS ACL 暂时无法完整映射：

- 以平台项目授权作为最小访问边界。
- 每个 asset 必须有 `project_id` 或明确进入待审核/隔离索引区。
- 无项目归属的文件默认不可进入 Hermes prompt。
- 参考资料、投标资料、员工目录、标准文件需单独授权，不默认全公司可见。
- `permission_tags` 初期可采用 `PROJECT:<id>`、`SOURCE:NAS`、`CONFIDENTIALITY:INTERNAL` 等粗粒度标签。

## 6. 增量同步与变更检测

### 6.1 当前能力

平台已经规划并推进：

- 扫描任务表。
- 扫描候选表。
- 正式资产表。
- `last_verified_at` / `last_seen_at` 类字段。
- checksum 异步补齐。
- 审计 / 事件流增量同步。

Batch 2 的异步任务、checksum、统计与事件流已开发完成并通过主 agent 审计，但仍需以测试 agent 复验结果作为最终准入。

### 6.2 变更发现方式

短期默认使用定时扫描 / 手动触发扫描：

- 新增：扫描发现新路径。
- 修改：mtime、size 或 checksum 变化。
- 删除/缺失：上次见过，本轮未见，标记 `MISSING` 或 `STALE`。
- 移动/重命名：相同 hash + 不同路径，标记 suspected moved。
- 旧版本：同 project + 相似文件名 + 新版本号 / 新 mtime，标记 version candidate。

当前不应假设 SMB 已提供可靠事件监听。若后续能接入 NAS 事件或快照差异，可作为优化。

### 6.3 Scan Checkpoint

建议 checkpoint 至少包含：

- `scan_id`
- `root_path`
- `started_at`
- `completed_at`
- `last_seen_path`
- `total_seen`
- `success_count`
- `failed_count`
- `missing_count`
- `rate_limit`
- `cursor`
- `failure_reason`

### 6.4 10TB 扫描策略

建议：

- 分根目录、分项目、分扩展名扫描。
- 默认低速率、可暂停、可恢复。
- 首轮只采集 catalog 元数据，不计算所有文件 hash。
- hash 异步按优先级计算。
- 单文件失败不阻断任务。
- 失败记录可重试。
- 扫描任务必须可审计。
- 大目录先 dry-run，输出预估数量、容量、类型分布。

## 7. 索引策略建议

### 7.1 索引等级

| 等级 | 含义 | 适用 |
|---|---|---|
| `catalog_only` | 只保存资产目录 | 所有可见文件默认等级 |
| `metadata_indexed` | 文件名、路径、项目、类型、mtime、hash 索引 | 大部分文件 |
| `preview_indexed` | 标题、目录、摘要、前几页/关键页 | 高价值 PDF/Office/PPT |
| `full_text_indexed` | 全文解析后进入稀疏索引 | 合同、方案、周报、报告、规范 |
| `semantic_indexed` | chunk + embedding + vector | 高价值、常问、已授权资料 |
| `curated_memory` | 人工确认的企业记忆事实 | Hermes_memory facts governance |
| `archived` | 归档低频资料 | 旧版本、历史包 |
| `excluded` | 不进入索引 | 锁文件、缓存、敏感/不可授权文件 |

### 7.2 文件类型策略

适合 full-text：

- `.doc`、`.docx`
- `.pdf`，前提是可解析或 OCR 成本可接受
- `.ppt`、`.pptx`
- `.xls`、`.xlsx` 中的说明页、清单页、关键 sheet
- `.wps`，视解析器能力而定

适合先 catalog：

- `.rvt`
- `.dwg`
- `.dxf`
- `.nwd`
- `.nwc`
- `.ifc`
- 大型 `.zip`、`.rar`
- 音视频
- 扫描件
- OSGB / 点云 / 大体量三维资产

需要人工确认后索引：

- 投标文件。
- 合同、报价、成本、内部评审。
- 员工目录。
- 客户敏感资料。
- 未明确项目归属资料。

### 7.3 BIM / CAD / 音视频 / 扫描件

短期：

- 只进入 catalog / metadata。
- 保存路径、格式、大小、项目、专业、版本、mtime、checksum。

中期：

- CAD/PDF 图纸可抽标题栏、图号、专业、版本。
- Office/PDF 可做 preview/full-text。
- 扫描件按需 OCR。
- 音视频按需转写。

后期：

- BIM 轻量化、构件级属性、空间索引、building ontology 后置，不进入当前 Hermes_memory MVP 接入。

### 7.4 避免全量 embedding 膨胀

必须禁止默认全量 embedding。

建议 embedding 条件：

- 用户高频访问。
- 业务方标记为高价值。
- 通过权限确认。
- 文件解析质量高。
- 文档结构清晰且 citation 可定位。
- 进入 curated memory 或专题知识包。

Hermes_memory 可对 catalog 和 full-text 检索结果再按需补 embedding，不应一开始对 10TB 文件全量 chunk。

## 8. 与 Hermes_memory 的接口边界

### 8.1 推荐提供的接口 / 表

短期优先：

- asset catalog read table/view：优先 `FileAssetView`。
- project asset read table/view：`ProjectAssetView`。
- model asset read table/view：`ModelAssetView`。
- audit / changed feed：`AuditEventView` 或事件流 API。
- file metadata query API。
- indexing job status API，若 Hermes_memory 发起解析/索引请求。
- audit log write API，记录 Hermes_memory 的索引、检索、权限过滤 trace。

中期：

- file content fetch API。
- permission check API。
- operation plan write API。
- parse job request API。
- index request API。

### 8.2 只读与写入边界

只读：

- `ProjectAssetView`
- `FileAssetView`
- `ModelAssetView`
- `AuditEventView`
- 文件元数据查询。
- 已授权内容读取。

允许写入但需审计：

- Hermes_memory 索引状态回写。
- 解析任务申请。
- 索引任务申请。
- 数据质量诊断结果。
- operation plan。
- audit / trace。

需人工审批：

- 启动大范围解析。
- 启动大范围 semantic indexing。
- 标记 stale / archived。
- 修复资产归属。
- 删除申请。
- 权限修正。

禁止直接写：

- 原始 NAS 文件。
- 正式资产表核心字段。
- NAS ACL。
- 原始业务数据库。

### 8.3 对接方式建议

当前阶段推荐：

1. Hermes_memory 通过只读 MySQL View 获取 asset catalog 和增量线索。
2. Hermes_memory 通过 REST/OpenAPI 申请解析、索引、审计和操作计划。
3. 后续如 Hermes Agent 需要工具化调用，再通过 MCP connector 封装 SQL View 与 REST API。

不建议当前直接用消息队列作为唯一协议，因为平台侧事件流和 API Key 授权尚在批次推进中。消息队列可以后置作为性能优化，不作为最小可行接口。

## 9. Agent 操作边界

### 9.1 默认允许

- 查询 asset catalog。
- 查询已索引内容。
- 读取可见文件元数据。
- 生成数据质量诊断。
- 生成操作建议。
- 生成检索 trace 和 citation。

### 9.2 需人工确认

- 启动解析任务。
- 启动索引任务。
- 标记 stale。
- 建议归档。
- 生成修复计划。
- 导出报告。
- 大范围重建索引。
- 读取敏感目录或投标/合同资料正文。

### 9.3 默认禁止

- 删除文件。
- 覆盖文件。
- 移动真实文件。
- 修改原始 NAS 权限。
- 自动修复真实业务数据。
- 全量 reindex。
- 默认扫描真实敏感 reports / reviews。
- 绕过权限读取文件。
- 直接写平台业务底表。

## 10. 推荐技术路径

### 10.1 短期：只读 NAS catalog + metadata sync

目标：

- 建立 Hermes_memory 可消费的资产目录。
- 不解析全部内容。
- 不做全量 embedding。

组件：

- MySQL `ProjectAssetView` / `FileAssetView` / `ModelAssetView`
- `AuditEventView` 或事件流 API
- Hermes_memory metadata sync job
- OpenSearch sparse metadata index
- Qdrant 暂不全量写入，仅保留高价值索引入口

数据流：

`NAS 扫描 -> data_file_resources -> FileAssetView -> Hermes_memory asset catalog sync -> metadata_index`

### 10.2 中期：高价值目录 preview index + full text index

目标：

- 对高价值目录建立可检索正文。
- 保持 citation 可追溯。

优先目录：

- `05_发布文件`
- `06_归档文件`
- `成果文件`
- `周报`
- `每周汇报文件`
- 标准文件中已授权的规范/模板

索引策略：

- Office/PDF/PPT 优先 preview。
- 可解析且授权明确的文件进入 full-text。
- citation 定位到页码、slide、sheet/cell、section 或 paragraph。

### 10.3 后期：按需 semantic index + operation plan + audit

目标：

- 对高价值知识包建立 semantic index。
- Agent 可生成操作计划，但不直接执行真实写操作。

组件：

- Hermes_memory chunk/embedding/Qdrant。
- OpenSearch sparse + dense hybrid retrieval。
- rerank + citation。
- operation_plan 表/API。
- audit / trace。

### 10.4 再后期：Data Steward / BIM 后置能力

后置，不进入当前实现：

- BIM asset catalog 深度治理。
- 构件级属性解析。
- building ontology。
- spatial index。
- PostGIS。
- Neo4j。
- 生产级 scheduler。
- BIM 模型轻量化与空间联动。

### 10.5 失败恢复策略

- 任务表记录状态、进度、失败原因、重试次数。
- 单文件失败不影响全局任务。
- 大文件解析超时进入失败队列。
- checksum 与解析分离。
- 索引写入失败可从 checkpoint 重放。
- Hermes_memory 维护独立 sync checkpoint，不覆盖平台 checkpoint。

### 10.6 权限策略

- 短期以项目授权为最小边界。
- 无项目归属资产不进 prompt。
- permission_tags 先粗粒度，后续补 ACL snapshot。
- Hermes_memory retrieval 必须在 pre-model 过滤。
- answer guard 检查 citation 权限。

### 10.7 审计策略

必须记录：

- 扫描任务。
- 解析任务。
- 索引任务。
- 权限过滤。
- 检索 trace。
- citation 使用。
- operation plan 生成。
- 人工审批。
- 删除申请。

Hermes_memory 的检索与索引行为也应回写审计或 trace，不只保存在 agent 本地日志。

## 11. 风险与待确认问题

### 11.1 主要风险

权限映射风险：

- NAS ACL 暂未完整映射到平台用户/部门/项目。
- 如果 Hermes_memory 先索引后过滤，容易发生无权限内容进入 prompt。

NAS 扫描性能风险：

- 10TB 文件量不能全量 hash、全量解析、全量 embedding。
- SMB 扫描可能受网络、锁文件、权限、长路径影响。

文件重复与版本混乱风险：

- 压缩包和解压目录并存。
- 临时目录、过程文件、旧版本、整改文件混杂。
- 仅靠文件名难以判断 latest。

大文件解析风险：

- RVT、IFC、DWG、压缩包、扫描 PDF、音视频不适合短期全文解析。

全量 embedding 膨胀风险：

- 10TB 全量 chunk 会造成向量库成本、噪声和召回质量问题。

误删 / 误改风险：

- Agent 不得直接删除、移动、覆盖 NAS 文件。
- 删除必须走申请、审核、隔离、恢复、永久删除。

路径变更风险：

- 人工移动/重命名会导致 source_path 失效。
- 需要 last_seen、missing、moved、stale 状态。

敏感数据泄露风险：

- 投标、合同、报价、内部评审、员工资料不应默认进入 prompt。
- citation 必须可追踪。

接口摇摆风险：

- Hermes_memory 不应依赖业务底表。
- 平台必须稳定 SQL View / REST API / 事件流字段含义。

### 11.2 待业务方确认

- 哪些 NAS 根目录属于 Hermes_memory 首批接入范围。
- 是否允许 Hermes_memory 读取真实 NAS 路径。
- 是否有企业统一身份系统可映射 NAS ACL。
- 是否有部门、项目、客户、密级主数据。
- 哪些目录默认禁止正文解析。
- 哪些目录优先建立 preview/full-text/semantic index。
- 是否允许对合同、投标、报价、内部评审做检索。
- 是否需要 OCR、音视频转写、压缩包解包。
- Hermes Agent 运行环境是否能访问 SMB NAS。
- Hermes_memory 是否通过 MySQL View、REST API、MCP connector 或文件清单对接。
- 增量同步是拉取事件流、平台推送，还是定时同步。
- 索引结果是否需要回写平台状态。

## 12. 给 Hermes_memory 的最终接入建议

短期不要重建平台资产库，也不要复制 NAS。Hermes_memory 应做三件事：

1. 从平台稳定 View 同步 asset catalog 和 metadata index。
2. 根据授权和价值选择性建立 preview/full-text/semantic/citation index。
3. 将检索、索引、权限过滤、citation 和 operation plan 全部纳入 audit/trace。

Hermes 主链路继续保持 pre-model 检索上下文注入，动态检索上下文只服务当前请求，不写入持久 system prompt。

这样 Hermes_memory 能成为 NAS / 数据库的 memory index + governance layer，而不是原始数据副本，也不会迫使当前平台数据治理能力推倒重来。
