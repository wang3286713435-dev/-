# 真实 NAS checksum 第五轮试点报告

更新时间：2026-05-11

## 结论

第五轮真实 NAS checksum 试点已完成并通过。

本轮只读取 NAS 文件内容计算 SHA-256，不修改、不移动、不删除 NAS 原文件；只将 checksum 结果写回平台数据库元数据字段。

## 试点范围

试点项目：

- `519 / 115-深圳宝安国际机场T2航站区及配套设施工程航站区工程施工总承包2标段`

执行前：

```text
文件数：396
总容量：5,897,285,554 bytes（约 5.49 GiB）
路径缺失：0
大小不一致：0
缺 checksum：396
```

## 执行方式

通过平台 API 创建异步任务：

```text
POST /api/data-steward/assets/checksum-jobs/batch
{"projectId":519}
```

返回：

```text
OK
created = 396
traceId = 8780ef4a275c40698dffd1a52f3716ab
```

任务时间窗口：

```text
first_job_created_at = 2026-05-11 08:05:25
last_job_updated_at  = 2026-05-11 08:18:12
```

## 执行结果

任务状态：

```text
CHECKSUM_CALC SUCCEEDED = 396
FAILED = 0
```

文件 checksum 写回：

```text
total = 396
valid_sha256 = 396
missing_checksum = 0
```

数据质量接口：

```text
GET /api/data-steward/assets/quality/overview?assetSource=NAS_REAL*&projectId=519
missingChecksumCount = 0
missingDisciplineCount = 0
```

文件质量筛选：

```text
GET /api/data-steward/assets/files:page?projectId=519&qualityIssue=MISSING_CHECKSUM&pageSize=50
OK 0 0
```

事件流：

```text
CHECKSUM checksum.success = 396
JOB      job.start        = 396
JOB      job.success      = 396
```

专项脚本：

```text
curl -fsS http://localhost:8080/actuator/health
{"status":"UP"}

bash scripts/dev/check-asset-quality-overview.sh
asset quality overview ok
```

## 当前全局状态

真实 NAS 已登记文件：

```text
total_real_files = 40935
total_gib        = 296.02
```

checksum 覆盖情况：

```text
has_checksum     = 1437
missing_checksum = 39498
missing_gib      = 284.42
```

剩余缺 checksum 项目：

```text
109 华润三九银湖科创中心项目                         1457 files /  7.11 GiB
110 龙华区观湖街道观城城市更新单元第一期10地块       2614 files /  8.08 GiB
108 福城南产业片区11-20-02宗地                       6751 files / 10.49 GiB
114 香港项目                                         3800 files / 13.53 GiB
97  水务利源既有建筑                                 1969 files / 20.10 GiB
113 宝安洪桥头项目                                   2951 files / 25.99 GiB
101 C塔                                              5457 files / 26.53 GiB
104 佛山顺德妇幼医院项目                             1699 files / 28.12 GiB
105 启航华居项目                                     2927 files / 32.91 GiB
100 深圳市二十八高项目                               3961 files / 38.99 GiB
93  中建八局国交酒店项目                             5912 files / 72.57 GiB
```

## 判断

本轮从 `1.50 GiB`、`1.55 GiB`、`2.83 GiB` 逐步扩大到 `5.49 GiB`，仍保持零失败，说明当前 checksum worker、任务状态、事件流和质量筛选链路可以支撑中等规模项目试点。

但任务执行耗时约 13 分钟，且当前 worker 以保守方式逐个读取文件；后续不建议直接对 10TB 全量一次性创建 checksum 任务。下一步应继续按项目分批推进，并在进入大项目之前评估任务并发、限速、暂停/恢复和失败重试策略。

建议下一轮优先：

1. `109-华润三九银湖科创中心项目`：`1457` 个文件，约 `7.11 GiB`。
2. 或 `110-龙华区观湖街道观城城市更新单元第一期10地块`：`2614` 个文件，约 `8.08 GiB`。

继续保持原则：只读 NAS 原文件，不修改、不移动、不删除。
