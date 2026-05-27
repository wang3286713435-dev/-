# M3G：NAS 侧 MinIO 对象存储接管真实项目文件计划

## 1. 批次定位

`M3G：NAS 侧 MinIO 对象存储接管真实项目文件`

M3G 是 M3F 之后的对象存储主线批次。它不再只是本机 MinIO 验证，而是把公司真实项目文件从“NAS 文件夹路径管理”升级为“NAS 侧 MinIO 对象存储治理”。

M3G 必须在 `M3F：新文件对象存储优先写入` 正式收口后才能启动。

## 2. 目标架构

```text
NAS 原项目资料区：冻结备份 / 回滚来源
NAS 侧 MinIO：正式文件本体
MySQL：业务台账、权限、版本、checksum、交付关系
平台后端：权限校验、审计、file-access、迁移任务
本机 Hermes：按平台授权复制 MinIO 文件副本到本机工作区解析
向量库：后续保存语义索引，本批不写入
```

## 3. 核心原则

- NAS 原项目资料区冻结为只读备份，不移动、不删除、不改名。
- NAS 侧 MinIO 使用独立对象数据区，不与原项目资料目录混放。
- 平台只通过 MinIO API 访问对象文件，不直接读 MinIO 数据目录。
- 员工不得直接进入 MinIO 数据目录修改、移动、删除文件。
- Hermes 不直接扫 NAS 项目资料目录。
- Hermes 不直接访问 MinIO 底层对象目录。
- Hermes 后续只能基于平台授权的 `assetUuid / objectVersion` 复制文件副本。
- 本批不做 Hermes 正文问答，不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。

## 4. 任务拆分

1. NAS 侧 MinIO 部署契约。
2. 平台 MinIO endpoint 切换。
3. 全项目资产盘点与容量预估。
4. 分批对象化迁移策略。
5. 历史文件对象化执行。
6. 平台读取链路切换。
7. 下载与预览性能策略。
8. Hermes 授权副本取用契约。
9. 验收、回滚与覆盖率报告。

## 5. 对象 key 规则

建议：

```text
objects/{assetUuid}/{objectVersion}/{checksum}
```

禁止 object key 包含：

- 真实 NAS 路径。
- 项目名。
- 楼栋名。
- 专业目录名。
- 用户名。
- 业务敏感目录。

## 6. 平台读取链路

M3G 完成后，平台文件读取优先级：

```text
active object version
-> NAS 侧 MinIO object
-> 受控 file-access
-> 必要时 NAS fallback
```

要求：

- 已对象化文件默认从 NAS 侧 MinIO 读取。
- NAS_ONLY 文件继续可用，但必须显示“未对象化”。
- 对象读取失败时，不得静默读取 NAS 并伪装成功。
- fallback 必须可配置、可审计、可提示。
- 文件列表、项目资产查询、交付绑定仍从 MySQL 查询，不直接扫描 MinIO。

## 7. 下载与预览性能

由于 MinIO 服务部署在 NAS 上，大文件下载应尽量走 NAS 侧 MinIO 直连，而不是本机平台后端全程转发。

要求：

- 小文件 / 预览可继续走平台受控 file-access。
- 大文件下载必须支持“平台审计后直连 NAS 侧 MinIO”或等价方案。
- 下载票据必须绑定 `userId / projectId / assetUuid / action / expiresAt / traceId`。
- 不得暴露真实 NAS 路径。
- 如使用 MinIO presigned URL，object key 必须是 opaque key，不能包含业务路径。
- 如不暴露 MinIO presigned URL，应提供平台 opaque download gateway 或内网反向代理方案。

## 8. Hermes 授权副本取用契约

本批只预留契约：

```text
员工提问
-> 平台判断权限
-> 平台根据 assetUuid 找到 active object version
-> 平台向 Hermes 发放受控文件取用任务
-> Hermes 通过万兆局域网从 NAS 侧 MinIO 复制文件副本到本机工作区
-> Hermes 解析 / 理解 / 生成 evidence
-> 后续回答引用 assetUuid + objectVersion + checksum + evidenceHash
```

M3G 不验收 AI 已理解文件，只验收 Hermes 后续取用文件副本的边界和状态预留。

## 9. 前端要求

- 对象迁移任务中心显示 NAS 侧 MinIO 状态。
- 增加项目级对象化覆盖率视图。
- 展示每个项目：
  - 总文件数。
  - 已对象化。
  - 未对象化。
  - 失败。
  - 跳过。
  - checksum 覆盖率。
  - 对象化容量。
- 文件详情优先展示平台资产 ID / `assetUuid`。
- 文件详情展示 `NAS_ONLY / OBJECT_STORED / MIGRATION_PENDING / MIGRATION_FAILED`。
- 不展示真实 NAS 路径、bucket、object key。
- 大文件下载时显示“平台已审计，正在通过对象存储下载”。
- NAS_ONLY 文件提示“仍在历史 NAS 链路，尚未对象化”。

## 10. 验收条件

- M3F 已正式收口。
- NAS 侧 MinIO 健康检查通过。
- 平台对象存储 endpoint 已切换到 NAS 侧 MinIO。
- 正式 bucket 已创建并可读写。
- 全项目对象化盘点报告已生成。
- 至少完成主 agent 确认的一批真实项目对象化，且方案可复用到全部真实项目。
- 每个对象化文件都有 `assetUuid`、checksum、active object version。
- 已对象化文件默认从 NAS 侧 MinIO 读取。
- NAS 原文件未被移动、删除、改名。
- 未对象化文件仍以 NAS_ONLY 状态解释，不假装对象化。
- 大文件下载支持平台审计后的 NAS 侧 MinIO 直连或等价加速方案。
- Hermes 文件副本取用契约已预留，但未写正文 evidence。
- 禁出字段扫描通过。
- M3F / M3E / M3D / M3C / file-access 回归通过。

## 11. 建议专项脚本

`scripts/dev/check-m3g-nas-minio-real-project-object-storage.sh`

至少验证：

1. NAS 侧 MinIO endpoint 健康。
2. 平台 provider health 指向 NAS 侧 MinIO。
3. 正式 bucket 可读写。
4. 项目对象化 dry-run 可生成计划。
5. 分批迁移任务可执行。
6. 重复迁移幂等。
7. 失败文件可重试。
8. 已对象化文件 `storage-status=OBJECT_STORED`。
9. file-access 可从 NAS 侧 MinIO 读取。
10. 大文件下载票据可生成且有审计。
11. Hermes 副本取用契约返回受控状态，不直接暴露 object key。
12. API / 前端响应禁出字段扫描通过。
13. NAS 原项目文件未被改动。
14. 覆盖率报告可生成。

## 12. 完成定义

M3G 不验收“AI 已理解文件”。

M3G 只验收：

> 公司真实项目文件本体已经可以由 NAS 侧 MinIO 统一治理，平台通过 MySQL 台账管理项目、权限、版本和交付关系，Hermes 后续可通过平台授权从 MinIO 复制文件副本到本机工作区进行理解。
