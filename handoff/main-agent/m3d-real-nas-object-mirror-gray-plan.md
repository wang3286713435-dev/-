# M3D：真实 NAS 小范围灰度镜像计划

日期：2026-05-26

## 目标

在 M3C 迁移任务中心已经收口后，选择 105 项目少量真实业务文件执行对象存储镜像灰度。

本批验证真实业务文件链路，而不是扩大迁移范围。

## 范围

- 项目：105 / `projectId=503`
- 文件来源：真实 NAS 业务文件台账
- 选择方式：显式 fileIds / assetUuid
- 覆盖类型：PDF / DWG / RVT 或模型类小样本
- 存储目标：MinIO / S3-compatible
- 访问方式：继续通过受控 file-access

## 禁止

- 不做全量 NAS 搬迁。
- 不做目录一键迁移。
- 不按后缀批量迁移整个项目。
- 不移动、删除、改名真实 NAS 文件。
- 不读取文件正文。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不做 Hermes 正文问答。
- 不做 BIM parser 或真实轻量化。
- 不暴露 raw NAS path、bucket、object key、`storage_uri`、raw row、SQL、token、secret。
- 不修改仓库 `docs/**`。

## 验收

- M3D 专项脚本通过。
- 105 真实业务小样本完成镜像。
- 成功样本显示 `OBJECT_STORED`。
- file-access 受控访问不回归。
- NAS 原文件未被移动、删除、改名。
- 灰度报告记录成功数、失败数、跳过数、checksum 覆盖率和禁出字段扫描结果。
