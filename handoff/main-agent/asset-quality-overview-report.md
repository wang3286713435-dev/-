# 一期数据质量体检能力交付报告

更新时间：2026-05-11  
执行人：Codex  
范围：一期后端数据治理与 NAS 资产接管范围内的数据质量体检能力。

## 1. 结论

已完成并通过验证。

本轮新增“一期数据质量体检”能力，用于在真实 NAS 资产入库后快速发现治理风险，覆盖：

- 待人工审核文件。
- 失败或运行中的扫描任务。
- 缺 checksum 的文件。
- 缺置信度的文件。
- 专业为空或为“其他”的文件。
- 版本号缺失的文件。
- 存储路径缺失的文件。
- 文件大小为 0 的记录。
- 非标准资料待治理与可导入状态。
- 风险项目排行。
- 最近治理事件。

该能力只读取平台元数据，不读取 PDF/Office 正文，不做模型轻量化，不做构件级解析，不修改或删除 NAS 文件，符合一期能力边界。

## 2. 后端变更

新增：

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/AssetQualityController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetQualityApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/AssetQualityRepository.java`

更新：

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-core/src/main/java/com/zhuoyu/delivery/core/user/application/CurrentUserApplicationService.java`

新增接口：

```text
GET /api/data-steward/assets/quality/overview
```

支持参数：

```text
projectId   可选，项目级体检
assetSource 可选，如 NAS_REAL*
```

权限规则：

- 项目级体检必须校验项目访问权限。
- 全局体检只统计当前用户可访问项目和本人创建的全局治理任务。
- 非标准资料治理只统计当前用户创建的治理记录。

## 3. 前端变更

新增：

- `frontend/src/modules/data-steward/pages/AssetQualityOverviewPage.vue`

更新：

- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/router/index.ts`

菜单入口：

```text
数据管家 / 数据质量
路由：/data-steward/quality
```

页面默认按 `NAS_REAL*` 过滤真实 NAS 资产，避免测试数据干扰真实试点判断。

## 4. 验收脚本

新增：

- `scripts/dev/check-asset-quality-overview.sh`

更新：

- `scripts/README.md`

脚本验证范围：

- 管理员可看到“数据质量”菜单入口。
- 真实 NAS 资产体检接口可用。
- 体检项字段完整。
- 项目级体检接口可用。
- `delivery.engineer` 无权访问固定真实项目 `101` 的项目级体检。

## 5. 验证结果

已通过：

```text
./mvnw -pl delivery-app -am -DskipTests package
corepack pnpm build
curl -fsS http://localhost:8080/actuator/health
bash scripts/dev/check-asset-quality-overview.sh http://localhost:8080 platform.admin Admin@123 2
bash scripts/dev/check-agent-db2-contract.sh
```

专项脚本结果：

```text
riskSignalCount=81881
metrics=11
topRiskProjects=10
recentEvents=10
projectRiskSignalCount=11835
101 access denied OK
asset quality overview ok
```

DB-2 合同复核结果：

```text
ProjectAssetView.count=519
FileAssetView.count=48842
ModelAssetView.count=10322
AuditEventView.count=59854
业务底表 core_projects / data_file_resources / core_audit_logs 均不可读
agent db2 contract ok
```

## 6. 运行状态

本轮收尾时已重新启动开发服务，方便用户直接查看页面：

- 后端：`http://localhost:8080`
- 前端：`http://localhost:5173`

页面入口：

```text
http://localhost:5173/data-steward/quality
```

## 7. 剩余事项

当前没有阻塞项。

“治理闭环增强”已完成：数据质量体检项现在可以跳转到对应待审核、扫描任务、非标准资料治理或文件列表筛选，进一步减少人工排查成本。

## 8. 治理跳转增强

更新时间：2026-05-11

本轮新增：

- 数据质量页体检项支持一键进入对应治理列表：
  - 失败扫描 / 运行中扫描：跳转扫描任务页并带入状态筛选。
  - 待审核：跳转扫描任务页并只看有待审核项的任务。
  - 非标准待治理 / 可导入：跳转非标准资料治理页并带入治理状态。
  - 缺 checksum / 缺置信度 / 专业待完善 / 版本缺失 / 路径缺失 / 零大小：跳转项目资产详情，并带入质量问题筛选。
- 项目资产详情页新增“质量问题”筛选。
- 后端文件资产列表新增 `qualityIssue` 查询参数，支持按质量问题筛选文件资产。
- 非标准资料治理页和扫描任务页支持读取 URL 查询参数，保证从体检页跳转后自动带好筛选条件。
- 回归脚本新增质量问题筛选断言，验证返回文件确实匹配对应问题类型。

本轮仍严格限定在一期元数据治理范围内：

- 不读取文件正文。
- 不做模型轻量化。
- 不做构件级解析。
- 不修改或删除 NAS 文件。

本轮补充验证通过：

```text
./mvnw -pl delivery-app -am -DskipTests package
corepack pnpm build
curl -fsS http://localhost:8080/actuator/health
bash scripts/dev/check-asset-quality-overview.sh http://localhost:8080 platform.admin Admin@123 2
OpenAPI 检查：/api/data-steward/assets/files:page 已包含 qualityIssue 参数
```

质量问题筛选脚本结果：

```text
MISSING_CHECKSUM total=40935 checked=50
MISSING_CONFIDENCE total=0 checked=0
MISSING_DISCIPLINE total=40935 checked=50
MISSING_VERSION total=0 checked=0
MISSING_STORAGE_PATH total=0 checked=0
ZERO_SIZE_FILE total=0 checked=0
asset quality overview ok
```
