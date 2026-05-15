# 真实 NAS checksum 第三轮试点报告

更新时间：2026-05-11

## 1. 结论

已完成真实 NAS 第三轮 checksum 试点。

本轮目标项目 `515 / 111-蛇口影剧院项目` 的 `290` 个文件已全部完成 SHA-256 checksum 写回。

本轮只读取 NAS 文件内容计算 SHA-256，不修改、不移动、不删除 NAS 原文件。

## 2. 试点范围

试点项目：

- `515 / 111-蛇口影剧院项目`

执行前：

```text
文件数：290
总容量：1,666,194,107 bytes
路径缺失：0
大小不一致：0
缺 checksum：290
```

## 3. 执行方式

通过平台 API 创建异步任务：

```text
POST /api/data-steward/assets/checksum-jobs/batch
{"projectId":515}
```

返回：

```text
OK
created = 290
traceId = 865c03594da24c918b93a8f845c98b8f
```

## 4. 执行结果

任务状态：

```text
CHECKSUM_CALC SUCCEEDED = 290
FAILED = 0
```

文件 checksum 写回：

```text
total = 290
valid_sha256 = 290
missing_checksum = 0
```

事件流：

```text
CHECKSUM checksum.success = 290
JOB      job.start        = 290
JOB      job.success      = 290
```

## 5. 数据质量验证

项目级数据质量：

```text
GET /api/data-steward/assets/quality/overview?assetSource=NAS_REAL*&projectId=515
missingChecksumCount = 0
missingDisciplineCount = 0
riskSignalCount = 11
```

文件质量筛选：

```text
GET /api/data-steward/assets/files:page?projectId=515&qualityIssue=MISSING_CHECKSUM&pageSize=50
OK 0 0
```

专项脚本：

```text
bash scripts/dev/check-asset-quality-overview.sh
asset quality overview ok
```

后端健康检查：

```text
GET /actuator/health
UP
```

## 6. 当前全局影响

真实 NAS 全局状态：

```text
真实 NAS 文件总数：40935
已完成 checksum：554
仍缺 checksum：40381
真实 NAS 总容量：296.02 GiB
仍缺 checksum 容量：291.41 GiB
```

已完成 checksum 的真实项目：

```text
112 歌剧院项目：21
96  深圳市前海蛇口自贸区医院科技大厦三期改造项目：243
111 蛇口影剧院项目：290
```

仍缺 checksum 的真实项目数：`13`

## 7. 下一步建议

建议继续按容量逐级扩大，不直接跑全量：

1. 下一轮优先 `116-港中文（深圳）医学院智能化`：`487` 个文件，约 `1.50 GiB`。
2. 再跑 `115-深圳宝安国际机场T2...`：`396` 个文件，约 `5.49 GiB`。
3. 然后进入 `109`、`110`、`114` 等 7GB 到 14GB 级别。

每轮仍按以下标准验收：

- 路径存在、大小一致。
- 任务全部成功，无失败。
- SHA-256 长度与格式正确。
- 项目级 `missingChecksumCount = 0`。
- 事件流和任务状态可查询。
- 后端健康检查正常。

