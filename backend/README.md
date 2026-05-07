# Backend

数字化交付平台后端工程，采用 Spring Boot 3.3.x + Java 21 + Maven 多模块。

## 模块

- `delivery-app`：启动应用、运行时配置、Flyway 迁移。
- `delivery-shared`：统一返回、异常、traceId 等共享能力。
- `delivery-core`：登录、JWT、当前用户、项目切换、RBAC、审计。
- `delivery-master-data`：工程部位、节点类型、交付物标准、目录模板。
- `delivery-data-steward`：文件资源、模型集成、管理对象；正在进入一期 BIM 资产/NAS 接管能力。
- `delivery-work-center`：首页、文档/图纸交付视图、看板聚合。
- `delivery-visualization-adapter`：3D/BIM 适配层 mock 与后续 BIM 引擎接入边界。

## 当前注意事项

当前已存在未完成的一期 BIM 资产管理初稿：

- `delivery-app/src/main/resources/db/migration/V7__init_bim_asset_management.sql`
- `delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/**`

这些文件尚未完成服务层、Controller、前端页面和验收脚本。Windows 端接手后必须先确认构建和 Flyway 启动是否受影响。

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
