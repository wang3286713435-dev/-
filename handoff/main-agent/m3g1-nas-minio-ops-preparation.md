# M3G-1 NAS 侧 MinIO 配置交接清单

更新时间：2026-05-27

## 用途

这份文件给运维 / NAS 管理员 / 平台开发 agent 使用，用于把公司 NAS 上的真实项目资料治理升级到 NAS 侧 MinIO 对象存储。

注意：本文件不包含任何密钥。密钥必须通过本机安全存储、环境变量或运维密钥管理方式注入，禁止写入仓库、聊天记录、审计日志或前端。

## 当前探测结果

已对现有 NAS 地址做非侵入式探测：

- NAS 地址：`192.168.1.181`
- MinIO API 端口 `9000`：未响应
- MinIO Console 端口 `9001`：未响应

结论：当前尚未确认 NAS 侧 MinIO 已部署或已开放给平台使用。

## 目标拓扑

```text
员工浏览器
  -> 平台前端
  -> 平台后端
  -> MySQL 台账 / 权限 / 审计
  -> NAS 侧 MinIO API
  -> MinIO 专用对象数据区

原 NAS 项目资料目录
  -> 冻结为只读备份和回滚来源

Hermes
  -> 后续只能通过平台授权任务复制对象副本
  -> 不直接扫 NAS 原项目目录
  -> 不直接访问 MinIO 底层数据目录
```

## NAS 侧目录要求

必须把“原项目资料目录”和“MinIO 对象数据目录”分开。

建议结构：

```text
原项目资料目录：由现有 NAS 继续保留，只读冻结
MinIO 数据目录：/volume*/zhuoyu-minio-data 或等价独立目录
MinIO 配置目录：/volume*/zhuoyu-minio-config 或等价独立目录
MinIO 日志目录：/volume*/zhuoyu-minio-logs 或等价独立目录
```

要求：

- 员工不得直接进入 MinIO 数据目录改文件。
- 平台不得直接读写 MinIO 数据目录，只能走 MinIO API。
- 原 NAS 项目资料目录不得被迁移脚本移动、删除、重命名或覆盖。
- MinIO 数据目录容量要按“双份占用”预估：原 NAS 备份 + MinIO 对象副本。

## 服务部署要求

MinIO 服务必须部署在 NAS 上或 NAS 同机/同存储域的受控服务环境中。

端口建议：

- API：`9000`
- Console：`9001`

网络要求：

- 仅局域网开放，不公网暴露。
- 平台后端所在机器可访问 API 端口。
- Console 仅运维可访问。
- 如后续使用 HTTPS / 反向代理，应先保持 endpoint 可配置，不写死。

## bucket 与账号策略

正式 bucket 推荐：

```text
zy-datahub-assets-prod
```

禁止继续使用测试语义 bucket，例如：

```text
delivery-m3a-smoke
```

账号要求：

- 创建平台专用 service account。
- service account 只授权业务 bucket 所需的最小读写权限。
- 管理员 root 凭据不提供给平台应用。
- access key / secret key 不写入仓库，不发聊天。

## 平台后端环境变量

平台后端最终需要通过安全方式注入：

```bash
DELIVERY_MINIO_ENABLED=true
DELIVERY_MINIO_ENDPOINT=http://<NAS-IP>:9000
DELIVERY_MINIO_ACCESS_KEY=<platform-service-access-key>
DELIVERY_MINIO_SECRET_KEY=<from-secure-storage>
DELIVERY_MINIO_DEFAULT_BUCKET=zy-datahub-assets-prod
```

如 endpoint 仍指向以下地址，只能判定为本机开发对象存储，不能判定为 NAS 侧 MinIO：

```text
127.0.0.1
localhost
0.0.0.0
host.docker.internal
```

## 平台重启与验证

注入环境变量后，需要重启平台后端。

运维侧基础验证：

```bash
curl -fsS http://<NAS-IP>:9000/minio/health/live
curl -fsS http://<NAS-IP>:9000/minio/health/ready
```

平台侧验证由 M3G-1 开发批次提供：

- 对象存储 readiness 能显示 NAS 侧 MinIO 状态。
- readiness 响应不返回 raw endpoint、bucket、object key、secret。
- 如果 endpoint 仍是本机开发 MinIO，平台必须显示 `LOCAL_DEV_MINIO` 或等价提示。

## M3G-1 前不得执行的动作

- 不直接全量复制 NAS 文件到 MinIO。
- 不移动、删除、重命名 NAS 原项目资料。
- 不让 Hermes 扫 NAS 或访问 MinIO 底层目录。
- 不把 MinIO 数据目录当普通共享文件夹给员工使用。
- 不在前端或 API 中暴露 bucket、object key、真实 NAS 路径。

## M3G-1 允许的动作

- 部署 NAS 侧 MinIO。
- 创建正式 bucket。
- 创建平台 service account。
- 平台读取 MinIO 健康状态。
- 平台做全项目对象化盘点。
- 平台生成 dry-run 迁移计划。
- 在专用 smoke prefix 下做读写探测，但不得触碰真实项目资料目录。

## 回退方案

如果 NAS 侧 MinIO 不可用：

- 平台应保持历史 NAS_ONLY 文件可读。
- 新迁移任务不得假成功。
- 新对象化计划可以继续 dry-run，但不能执行真实迁移。
- 运维可临时切回本机开发 MinIO 仅用于开发验证，但页面和 API 必须明确标识为本机开发环境。
