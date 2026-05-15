# 真实 NAS checksum 小批量试点报告

更新时间：2026-05-11

## 1. 结论

已完成真实 NAS 小批量 checksum 试点。

本轮只读取 NAS 文件内容计算 SHA-256，不修改、不移动、不删除 NAS 原文件；只将计算结果写回平台数据库元数据字段 `data_file_resources.checksum`。

## 2. 试点范围

试点项目：

- `516 / 112-歌剧院项目`

选择原因：

- 文件数最少：`21`
- 总容量约 `246,694,735 bytes`，约 `235 MB`
- 适合作为本机 + NAS 小压力试运行样本

执行前确认：

```text
文件数：21
总容量：246,694,735 bytes
路径缺失：0
大小不一致：0
缺 checksum：21
```

## 3. 执行方式

通过平台 API 创建异步任务：

```text
POST /api/data-steward/assets/checksum-jobs/batch
{"projectId":516}
```

返回：

```text
OK
created = 21
traceId = 810156c89b0f404b876e250dfd09e428
```

任务由平台 `data_asset_jobs` + 应用内 worker 执行，不直接脚本写 checksum。

## 4. 执行结果

任务状态：

```text
CHECKSUM_CALC SUCCEEDED = 21
FAILED = 0
```

文件 checksum 写回：

```text
total = 21
valid_sha256 = 21
missing_checksum = 0
```

抽样：

```text
深圳歌剧院北区方案设计说明.pdf -> af50afcc2b14b64f... len=64
250905_base plans_Stage.dwg -> 157b41e2ec8bf336... len=64
250926_base plans 地下室未更新.dwg -> 4df641f6757621c9... len=64
XREF_CORE-250807 - Update.dwg -> 6a24fc20ac6a80a6... len=64
XREF_CORE-250923 - Update.dwg -> 18548e0302368d75... len=64
```

## 5. 数据质量验证

项目级质量接口：

```text
GET /api/data-steward/assets/quality/overview?assetSource=NAS_REAL*&projectId=516
missingChecksumCount = 0
missingDisciplineCount = 0
```

文件质量筛选：

```text
GET /api/data-steward/assets/files:page?projectId=516&qualityIssue=MISSING_CHECKSUM&pageSize=50
OK 0 0
```

## 6. 事件流与任务留痕

近 15 分钟项目事件：

```text
CHECKSUM checksum.success = 21
JOB      job.start        = 21
JOB      job.success      = 21
```

任务表：

```text
data_asset_jobs CHECKSUM_CALC SUCCEEDED = 21
```

## 7. 风险与下一步

本轮小批量试点通过，说明：

- 平台异步 checksum worker 可正常读取 NAS 挂载路径。
- SHA-256 可正常计算并写回数据库。
- 任务状态、事件流、质量筛选可闭环。

下一步建议不要直接全量跑 10TB，建议按容量逐级扩大：

1. 第二轮：`96-深圳市前海蛇口自贸区医院科技大厦三期改造项目`，`243` 个文件，约 `2.83 GiB`。
2. 第三轮：选择 `111` 或 `116`，验证几百文件量级。
3. 再进入 10GB、30GB、80GB 级项目。

每轮都应记录：

- 创建任务数
- 成功/失败数
- 缺 checksum 是否归零
- NAS 读取是否稳定
- 后端和前端是否仍响应正常

