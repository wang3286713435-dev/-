# 开发 Agent 当前任务：M3G-1 P1 回归修复

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3g-nas-minio-real-project-object-storage`

## 0. 当前结论

M3G-1 核心能力已通过：

- readiness 能识别 `NAS_SIDE_MINIO / READY`。
- 全项目对象化覆盖率可查。
- 503 / 105 项目 dry-run 可生成计划。
- dry-run 未启动真实迁移、未复制文件。

但测试 agent 判定 M3G-1 暂不收口，当前有 2 个 P1：

1. M3E 回归失败：切到 NAS 侧 MinIO 后，既有对象化预览产物通过 file-access 读取失败，返回 `ASSET_FILE_NOT_READABLE`。
2. M3F 回归脚本失败：脚本仍假设对象存储是本机 Docker MinIO，通过停止本机 MinIO 模拟不可用；当前实际对象存储已切到 NAS 侧 MinIO，因此停止本机 MinIO 不会让对象存储不可用，脚本误判。

本轮只修这两个 P1 和一个测试报告记录的 P2：

- 单项目对象化盘点接口 `projectCode / projectName` 返回 `null`，应与全项目盘点保持一致。

## 1. 必须先阅读

- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/current-prompt.md`
- `handoff/main-agent/m3g1-task-graph.md`
- `handoff/main-agent/m3g1-nas-minio-ops-preparation.md`
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`
- `scripts/dev/check-m3f-object-storage-first-write.sh`
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`

## 2. 严格边界

本轮允许：

- 修复 M3E 在 NAS 侧 MinIO 下的预览产物 / file-access 回归。
- 修复 M3F 脚本对 `LOCAL_DEV_MINIO` 和 `NAS_SIDE_MINIO` 的环境判断。
- 修复单项目对象化盘点展示字段。
- 更新专项脚本和 `handoff/dev-agent/latest-report.md`。

本轮禁止：

- 不执行历史文件真实批量迁移。
- 不移动、删除、重命名、覆盖真实 NAS 原项目文件。
- 不做 Hermes 正文问答。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不接入真实 BIM 引擎。
- 不把 file-access 改成静默绕过对象存储失败后读取 NAS 并伪装成功。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object key、endpoint 原文、SQL、raw row、token、secret。
- 不修改 `docs/**`。

## 3. P1-1：M3E 预览产物在 NAS MinIO 下不可读

### 问题

M3E 脚本选择了一个已对象化原生预览样本 `fileId=993`，但当前平台对象存储 endpoint 已切到 NAS 侧 MinIO。该文件的历史 object metadata 可能指向旧本机 MinIO 对象，NAS MinIO 中没有对应对象本体，所以 file-access 返回：

`ASSET_FILE_NOT_READABLE`

### 修复目标

M3E 回归在 NAS 侧 MinIO 下必须通过。

允许的修复方向：

1. `preview-artifacts:prepare` 对 PDF / 图片原生预览样本应保证当前 active object version 在当前对象存储 provider 下可读。
2. 如果已有 active object version 不可读，不要让 file-access 静默伪装成功；应由 prepare 流程基于受控源文件重新对象化到当前 NAS 侧 MinIO，并创建新的 active object version 或修复预览产物关联。
3. 如选择脚本修复，脚本必须显式准备一个“当前 provider 可读”的对象化样本，而不是随机拿历史 `OBJECT_STORED` 记录。

不允许：

- 不允许把对象读取失败静默 fallback 到 NAS 并仍称 `OBJECT_STORED`。
- 不允许暴露对象 bucket / object key。
- 不允许读取文件正文内容。
- 不允许批量迁移整个项目。

## 4. P1-2：M3F 脚本本机 MinIO 停止模拟不适配 NAS MinIO

### 问题

`check-m3f-object-storage-first-write.sh` 通过停止 / 暂停本机 `delivery-minio` 容器来模拟对象存储不可用。

当前后端实际连接的是 NAS 侧 MinIO，所以本机 MinIO 停止后对象存储仍可用，脚本期望失败但实际上传成功。

### 修复目标

M3F 回归脚本必须适配两类环境：

- `LOCAL_DEV_MINIO`：可以按旧逻辑暂停本机 MinIO 容器验证 fail-closed。
- `NAS_SIDE_MINIO`：不能停止 NAS MinIO；脚本应跳过本机容器暂停模拟，明确记录“NAS MinIO 环境下不执行本机容器不可用模拟”，并继续验证对象存储优先上传、active object version、file-access、禁出字段。

建议实现：

- 脚本先调用 readiness 接口读取 `endpointType / readinessStatus`。
- 当 `endpointType=LOCAL_DEV_MINIO` 且本机容器存在时，执行旧的 unavailable 模拟。
- 当 `endpointType=NAS_SIDE_MINIO` 时，不暂停本机 MinIO，不要求上传失败；改为通过 readiness `READY` 和正常上传链路证明当前对象存储可用。
- 输出中要明确这是环境分支，不是跳过 M3F 主链路。

不允许：

- 不允许为了让脚本通过去关闭 NAS 侧 MinIO。
- 不允许要求用户提供密钥到脚本、报告或仓库。
- 不允许把测试环境假设写死为本机 MinIO。

## 5. P2：单项目对象化盘点字段

修复：

- `/api/data-steward/projects/{projectId}/storage-objectification-inventory`
- `projectCode / projectName` 不应为 `null`。
- 应与全项目盘点中的同项目值一致。

## 6. 自测要求

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
git diff --cached --check
```

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须说明：

1. M3E file-access 失败根因。
2. 采用的修复方式。
3. M3F 脚本如何区分 `LOCAL_DEV_MINIO` 与 `NAS_SIDE_MINIO`。
4. 单项目盘点字段是否修复。
5. 是否执行真实历史文件迁移，必须明确回答“否”。
6. 是否触碰真实 NAS 文件，必须明确回答“否”。
7. 自测结果。
8. 未完成事项。

## 8. 完成定义

只有同时满足以下条件，才能标记完成：

- M3E 回归通过。
- M3F 回归通过。
- M3G-1 专项通过。
- file-access 回归通过。
- 单项目盘点字段不再为 null。
- 未发生真实历史文件迁移。
- 未触碰真实 NAS 原项目文件。
- 未修改 `docs/**`。
