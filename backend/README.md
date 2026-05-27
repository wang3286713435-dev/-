# Backend

卓羽智能数据中台后端工程，采用 Spring Boot 3.3.x + Java 21 + Maven 多模块。

## 模块

- `delivery-app`：启动应用、运行时配置、Flyway 迁移。
- `delivery-shared`：统一返回、异常、traceId 等共享能力。
- `delivery-core`：登录、JWT、当前用户、项目切换、RBAC、审计。
- `delivery-master-data`：工程部位、节点类型、交付物标准、目录模板。
- `delivery-data-steward`：文件资源、模型集成、管理对象；正在进入一期 BIM 资产/NAS 接管能力。
- `delivery-work-center`：首页、文档/图纸交付视图、看板聚合。
- `delivery-visualization-adapter`：3D/BIM 适配层 mock 与后续 BIM 引擎接入边界。

## 当前注意事项

一期 BIM 资产管理已从初稿进入可验收版本，相关入口包括：

- `delivery-app/src/main/resources/db/migration/V7__init_bim_asset_management.sql`
- `delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/**`

后续接手仍需先确认构建、Flyway 启动和当前数据库版本，避免把历史测试数据误当成真实 NAS 接管数据。

当前一期数据治理开发裁决：

- 后端数据治理已先行稳定；前端已补齐资产总览和项目资产详情，人工审核、导入和删除运维仍先通过 REST API + Swagger/OpenAPI 完成。
- 按主线批次推进：数据底座与 NAS 扫描入库；真实 NAS 目录发现与分类治理增强；异步任务、checksum、统计与事件流；企业 agent、审批与受控物理删除。
- NAS 扫描白名单为 `.rvt`、`.dwg`、`.ifc`、`.nwd`、`.nwc`、`.dxf`、`.pdf`。
- 低置信扫描结果必须进入待审核队列，人工确认项目归属、文件类型、专业和版本。
- 项目资产与容量统计接口支持 `assetSource` 过滤，例如 `NAS_REAL*` 用于真实 NAS 试点口径。
- 文件资产新增分页查询接口 `/api/data-steward/assets/files:page`，用于几万到几十万条元数据的日常检索。
- 企业 agent 采用 API Key 按项目范围授权，并通过 SQL View 与 REST/OpenAPI 对接。
- 物理删除 NAS 文件必须申请、审核、隔离 30 天，不能直接永久删除。
- 公司真实 NAS 根路径为 `/Volumes/zyzn/卓羽智能项目`；接入策略为只读影子导入，不移动、不改名、不删除 NAS 原文件。

## 本地运行

优先使用脚本。

macOS / Linux：

```bash
bash scripts/dev/start-backend.sh
```

Windows PowerShell：

```powershell
.\scripts\dev\start-backend.ps1
```

手动命令：

```bash
cd backend
./mvnw -pl delivery-app -am -DskipTests package
java -jar delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar
```

Windows 手动命令：

```powershell
cd backend
.\mvnw.cmd -pl delivery-app -am -DskipTests package
java -jar .\delivery-app\target\delivery-app-1.0.0-SNAPSHOT.jar
```
