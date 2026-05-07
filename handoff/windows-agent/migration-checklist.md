# Windows 项目迁移清单

这份清单用于把项目源码、文档和后续开发上下文交接到 Windows 端。它不迁移 Codex 会话数据库，只迁移仓库文件和项目记忆。

## 1. macOS 端交接前检查

当前 macOS 工作区没有首个 commit，因此必须先决定源码交接方式。

### 方式 A：提交并推送

适合有远端仓库时：

1. 确认不包含缓存和构建产物。
2. 创建首个 commit。
3. 推送远端。
4. Windows 端使用 `git clone`。

### 方式 B：源码包复制

适合暂时没有远端仓库时：

只复制源码和文档，排除：

- `.git/`，除非明确要保留本地 Git 历史。
- `.claude/`
- `.DS_Store`
- `frontend/node_modules/`
- `frontend/dist/`
- `backend/**/target/`
- `.pnpm-store/`
- `infra/data/`
- `infra/logs/`
- `minio-data/`
- 本地日志、缓存、IDE 临时目录。

必须保留：

- `.gitattributes`
- `.gitignore`
- `README.md`
- `docs/**`
- `backend/**` 源码、`pom.xml`、`mvnw.cmd`、`.mvn/wrapper/**`
- `frontend/src/**`、`package.json`、`pnpm-lock.yaml`
- `infra/**`
- `scripts/**`
- `handoff/**`

## 2. Windows 端准备

安装：

1. Git for Windows
2. Docker Desktop
3. Node.js 20 LTS 或兼容版本
4. JDK 21
5. PowerShell 5.1+ 或 PowerShell 7

执行：

```powershell
corepack enable
Set-ExecutionPolicy -Scope Process Bypass
```

推荐路径：

```text
D:\dev\zhuoyusmart\digital-delivery-platform
```

不要放在中文路径下，避免脚本、Maven、Node 或 Docker 出现路径兼容问题。

## 3. Windows 端打开后第一步

让 Windows 端 Codex 先读取：

```text
handoff/windows-agent/current-prompt.md
```

随后必须读取：

```text
handoff/windows-agent/project-context.md
docs/07-complete-delivery-prd.md
docs/08-acceptance-and-agent-integration.md
handoff/main-agent/status.md
handoff/main-agent/decisions.md
```

## 4. 文件完整性清单

Windows 端必须确认以下文件存在：

- `.gitattributes`
- `.gitignore`
- `README.md`
- `docs/README.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/09-windows-dev-migration.md`
- `handoff/windows-agent/project-context.md`
- `handoff/windows-agent/current-prompt.md`
- `backend/mvnw.cmd`
- `backend/.mvn/wrapper/maven-wrapper.properties`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `infra/docker-compose.yml`
- `scripts/dev/*.ps1`

## 5. 一期 BIM 资产初稿清单

这些文件已经存在，但还没有完成整条功能链：

- `backend/delivery-app/src/main/resources/db/migration/V7__init_bim_asset_management.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/AssetImportJobRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/BimAssetRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/StorageRootRepository.java`

Windows 端需要确认这些文件不会导致编译失败。若失败，优先修复编译，再继续开发。

## 6. Windows 端验收目标

至少确认：

1. 项目目录完整。
2. 前端依赖可安装。
3. 前端可构建。
4. 后端可构建。
5. Docker 基础设施可启动。
6. 后端可启动并通过健康检查。
7. 前端可启动。
8. 最小链路通过。
9. MVP 全链路尽量通过。
10. 当前三期文档和企业 agent 对接要求被正确理解。

## 7. 交接报告

Windows 端必须写入：

```text
handoff/windows-agent/latest-report.md
```

报告必须能让 macOS 端主 agent 继续判断：

- Windows 端是否可继续开发。
- 哪些命令已经通过。
- 哪些命令失败。
- 是否存在 Windows 专属问题。
- `V7` 资产初稿是否可继续承接。
