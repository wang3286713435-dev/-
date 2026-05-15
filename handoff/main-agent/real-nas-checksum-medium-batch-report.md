# 真实 NAS checksum 中等规模试点报告

更新时间：2026-05-11

## 1. 结论

已完成真实 NAS 第二轮 checksum 试点。

本轮目标项目 `507 / 96-深圳市前海蛇口自贸区医院科技大厦三期改造项目` 的 `243` 个文件已全部完成 SHA-256 checksum 写回。

本轮只读取 NAS 文件内容计算 SHA-256，不修改、不移动、不删除 NAS 原文件。

## 2. 试点范围

试点项目：

- `507 / 96-深圳市前海蛇口自贸区医院科技大厦三期改造项目`

执行前：

```text
文件数：243
总容量：3,039,265,913 bytes
路径缺失：0
大小不一致：0
缺 checksum：243
```

## 3. 过程记录

第一轮创建任务：

```text
POST /api/data-steward/assets/checksum-jobs/batch
{"projectId":507}

created = 200
traceId = 982d6c6c2aca48a7af74c0824eb1d80e
```

第一轮结果：

```text
CHECKSUM_CALC SUCCEEDED = 200
剩余缺 checksum = 43
FAILED = 0
```

发现并修复批量接口问题：

- 问题：批量接口原逻辑先取最近更新的 `200` 个文件，再在内存中过滤缺 checksum。
- 影响：第一轮完成后，最近更新的 `200` 个文件已经都有 checksum，第二次调用会返回 `0`，但项目中实际仍有 `43` 个文件缺 checksum。
- 修复：批量接口改为直接按 `MISSING_CHECKSUM` 条件查询数据库，保证后续批次能拿到真实剩余文件。
- 构建验证：`./mvnw -pl delivery-app -am -DskipTests package` 通过。
- 后端已重启并通过健康检查。

第二轮创建任务：

```text
POST /api/data-steward/assets/checksum-jobs/batch
{"projectId":507}

created = 43
traceId = 5cd71d5a6cdc4b299d504b58f426ac6c
```

第二轮结果：

```text
CHECKSUM_CALC SUCCEEDED = 43
FAILED = 0
```

## 4. 最终结果

项目 `507 / 96` 最终状态：

```text
total = 243
valid_sha256 = 243
missing_checksum = 0
```

任务状态：

```text
CHECKSUM_CALC SUCCEEDED = 243
FAILED = 0
```

事件流：

```text
CHECKSUM checksum.success = 243
JOB      job.start        = 243
JOB      job.success      = 243
```

## 5. 数据质量验证

项目级数据质量：

```text
GET /api/data-steward/assets/quality/overview?assetSource=NAS_REAL*&projectId=507
missingChecksumCount = 0
missingDisciplineCount = 0
riskSignalCount = 11
```

文件质量筛选：

```text
GET /api/data-steward/assets/files:page?projectId=507&qualityIssue=MISSING_CHECKSUM&pageSize=50
OK 0 0
```

批量接口空跑验证：

```text
POST /api/data-steward/assets/checksum-jobs/batch
{"projectId":507}

OK 0
```

专项脚本：

```text
bash scripts/dev/check-asset-quality-overview.sh
asset quality overview ok
```

## 6. 当前全局影响

真实 NAS 全局缺 checksum：

```text
40671
```

说明：

- 小批量试点 `112` 已完成 `21` 个文件。
- 中等规模试点 `96` 已完成 `243` 个文件。
- 真实 NAS 总文件 `40935`，当前剩余 `40671` 个文件未补 checksum。

## 7. 下一步建议

建议继续按容量逐级扩大，不直接跑全量：

1. 第三轮：`111-蛇口影剧院项目`，`290` 个文件，约 `1.55 GiB`。
2. 第四轮：`116-港中文（深圳）医学院智能化`，`487` 个文件，约 `1.50 GiB`。
3. 第五轮：选择 `109` 或 `104`，进入 7GB 到 30GB 级别验证。

每轮仍按以下标准验收：

- 路径存在、大小一致。
- 任务全部成功，无失败。
- SHA-256 长度与格式正确。
- 项目级 `missingChecksumCount = 0`。
- 事件流和任务状态可查询。
- 后端健康检查正常。

