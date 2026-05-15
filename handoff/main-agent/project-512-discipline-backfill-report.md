# 项目 108 专业字段补录报告

更新时间：2026-05-11

## 1. 结论

已完成项目 `512 / 108-福城南产业片区11-20-02宗地` 的文件专业字段补录。

本次只修改平台数据库中的文件元数据，不读取文件正文，不修改、不移动、不删除 NAS 原文件。

## 2. 字段口径

`discipline` 表示“文件所属专业”，不是项目整体专业。

本项目按路径目录和图纸编号补录，采用平台内置专业编码：

- `ARCHITECTURE`：建筑
- `STRUCTURE`：结构
- `PLUMBING`：给排水
- `HVAC`：暖通
- `ELECTRICAL`：电气
- `FIRE_PROTECTION`：消防
- `INTELLIGENT`：智能化
- `GENERAL`：综合、总图、提资、过程文件、计划等通用资料

本项目额外登记项目扩展专业：

- `GAS`：燃气

## 3. 补录结果

补录前缺专业文件数：`6751`

补录后缺专业文件数：`0`

补录分布：

```text
STRUCTURE        2344
ARCHITECTURE     1473
ELECTRICAL       1121
PLUMBING          620
HVAC              493
INTELLIGENT       353
FIRE_PROTECTION   225
GENERAL            68
GAS                54
```

## 4. 验证结果

已验证：

```text
GET /api/data-steward/assets/files:page?projectId=512&qualityIssue=MISSING_DISCIPLINE&pageSize=50
```

返回结果：

```text
OK 0 0
```

项目级数据质量接口：

```text
missingDisciplineCount = 0
```

前端项目详情页已将专业编码显示为中文标签，例如 `STRUCTURE` 显示为“结构”，`GAS` 显示为“燃气”。

前端二次复核：

- `http://localhost:5173/data-steward/assets/512` 在“全部质量”下可看到真实专业标签，例如“结构”。
- `http://localhost:5173/data-steward/assets/512?qualityIssue=MISSING_DISCIPLINE` 保留“专业待完善”筛选时返回空列表，这是补录完成后的预期结果。
- 已修复项目详情页在同组件内切换项目或切换质量筛选时可能残留上一项目表格数据的问题：路由项目编号或质量筛选变化后会清空旧数据并重新加载。
- 前端构建 `corepack pnpm build` 通过。

## 5. 审计

已写入平台审计与事件流：

- `core_audit_logs.action_code = asset.file.discipline.backfill`
- `data_asset_events.action_code = file.discipline.backfill`
- `trace_id = manual-project-512-discipline-backfill`
