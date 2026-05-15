# 一期主线验收收口报告

更新时间：2026-05-11

## 结论

一期主线验收收口通过，当前没有发现 P0/P1 阻塞。

本轮严格收住范围：不新增二期 BIM 能力，不继续扩展新功能，不继续推进全量 checksum，只验证公司内部 BIM 资产库一期主线是否可稳定运行。

## 本轮验收范围

本轮只覆盖一期主线：

1. 真实 NAS 资产已入库项目可查、可看、可追路径。
2. 非标准目录暂缓入库，不污染正式资产库。
3. 数据质量问题可解释、可定位、可处理。
4. 普通项目用户不能越权访问真实 NAS 项目资产。
5. 企业 agent 后续可通过稳定 MySQL View 接入，但当前不做 Hermes 联调。
6. checksum 能力已完成五轮试点，后续作为后台治理任务持续推进，不作为一期上线阻塞项。

## 构建与基础健康

已验证：

```text
curl -fsS http://localhost:8080/actuator/health
结果：{"status":"UP"}

backend ./mvnw -pl delivery-app -am -DskipTests package
结果：BUILD SUCCESS，8 个模块通过。

frontend corepack pnpm build
结果：通过。
备注：仍有 Vite 大 chunk 警告，不阻塞一期内部试点。
```

## 主线脚本验证

已通过：

```text
bash scripts/dev/check-minimal-chain.sh
bash scripts/dev/check-master-data-chain.sh
bash scripts/dev/check-deliverable-standard-chain.sh
bash scripts/dev/check-mvp-chain.sh
bash scripts/dev/check-scan-task-control.sh
bash scripts/dev/check-asset-quality-overview.sh
bash scripts/dev/check-agent-db2-contract.sh
```

结果：

```text
mvp chain ok
scan task control regression passed
asset quality overview ok
agent db2 contract ok
```

说明：

- 本轮没有重跑 `check-bim-asset-batch1/2/3` 的完整批次脚本。
- 原因：这些脚本已由测试 agent 复验通过，且会持续制造大量测试项目和测试资产；当前收口阶段更适合用主线脚本、专项脚本和真实 NAS 数据抽查确认状态。

## 真实 NAS 数据快照

真实 NAS 正式资产库：

```text
real_projects = 16
real_files    = 40935
models        = 2604
drawings      = 38331
total_gib     = 296.02
```

核心质量字段：

```text
missing_discipline = 0
missing_path       = 0
zero_or_missing_size = 0
pending_real_candidates = 0
```

扫描任务：

```text
real NAS scan tasks:
SUCCEEDED = 17
```

非标准目录治理：

```text
nonstandard_count = 11
pending_agent     = 11
```

这些非标准目录包括重复编号、投标/参考、未知编码和用户暂缓目录。它们仍保留在治理区，未进入正式项目、正式文件资产或稳定 SQL View 的正式资产口径。

## checksum 状态

checksum 五轮试点已完成，当前结论是“能力可用，但不继续抢主线资源”。

当前真实 NAS checksum 状态：

```text
has_checksum     = 1437
missing_checksum = 39498
```

已完成试点项目：

```text
112 歌剧院项目
96  深圳市前海蛇口自贸区医院科技大厦三期改造项目
111 蛇口影剧院项目
116 港中文（深圳）医学院智能化
115 深圳宝安国际机场T2航站区及配套设施工程航站区工程施工总承包2标段
```

裁决：

- checksum 是必要治理能力，但全量 checksum 不作为一期上线阻塞项。
- 后续新增项目可以先入库可查，checksum 状态显示为待计算。
- 重点项目、近期交付项目优先计算。
- 大批量 checksum 应放到低峰期分批执行。

## 页面接口抽查

本轮抽查页面背后的接口，不新增页面能力：

```text
资产总览：/api/data-steward/assets/projects?assetSource=NAS_REAL*
结果：16 个真实 NAS 项目。

数据质量：/api/data-steward/assets/quality/overview?assetSource=NAS_REAL*
结果：专业、路径、零大小文件等关键字段无缺失。

项目资产详情：/api/data-steward/assets/files:page?projectId=506&pageSize=5
结果：可返回文件资产列表。

文件详情：/api/data-steward/assets/files/{fileId}
结果：可返回 storagePath / logicalPath，支持路径追溯和前端复制路径。

扫描任务：/api/data-steward/assets/nas-scans?pageSize=5
结果：可返回任务列表；成功任务进度为 100%。

非标准资料治理：/api/data-steward/assets/nonstandard-directories?governanceStatus=PENDING_AGENT
结果：11 条待治理目录。

权限抽查：delivery.engineer 查询真实 101 项目文件
结果：total = 0，未越权。
```

## 企业 Agent 接入状态

一期已准备好企业 agent 后续接入所需的稳定边界，但当前不做联调：

1. 本机 dev 只读账号 `hermes_agent_ro` 已存在。
2. 只允许读取稳定 View：`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView`。
3. 只读账号不能读取业务底表：`core_projects`、`data_file_resources`、`core_audit_logs`。
4. 事件同步策略保持为 agent 按 `AuditEventView.event_id` 拉取。
5. 当前不走同步库、搜索引擎或向量库。

专项脚本验证已通过：

```text
scripts/dev/check-agent-db2-contract.sh
```

## 非阻塞项

以下事项不阻塞一期主线：

1. 真实 NAS 仍有 `39498` 个文件缺 checksum，后续按后台治理任务分批补齐。
2. 非标准目录 `11` 条仍待企业 agent 或人工治理，这是设计内状态，不污染正式资产库。
3. 前端构建存在 Vite 大 chunk 警告，不影响当前内部试点。
4. 本机 dev 数据库因长期回归脚本存在大量测试项目；真实业务页面默认使用 `assetSource=NAS_REAL*` 过滤，暂不影响一期试点。

## 主 Agent 裁决

一期当前应进入“用户验收/内部试运行准备”，不再继续扩功能。

允许做：

1. 修 P0/P1 缺陷。
2. 补操作说明、验收说明、交接说明。
3. 根据用户试用反馈做窄范围可用性修复。

暂缓做：

1. 全量 10TB checksum。
2. 企业 agent 深度联调。
3. 模型轻量化预览。
4. 构件级解析、构件搜索、碰撞检查。
5. 二期客户交付版功能。

下一步建议：

先让用户按页面进行一期试用验收：资产总览、项目详情、扫描任务、数据质量、非标准资料治理。主 agent 只处理验收中发现的 P0/P1，不主动扩展功能面。
