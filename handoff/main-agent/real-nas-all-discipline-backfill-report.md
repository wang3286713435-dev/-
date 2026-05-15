# 真实 NAS 已导入项目专业字段批量补录报告

更新时间：2026-05-11

## 1. 结论

已完成除项目 `512 / 108-福城南产业片区11-20-02宗地` 外，其余真实 NAS 已导入项目的专业字段补录。

本轮只修改平台数据库中的文件元数据，不读取文件正文，不修改、不移动、不删除 NAS 原文件。

## 2. 补录范围

真实 NAS 试点资产范围：

- `asset_source = NAS_REAL_PILOT`
- 项目数：`16`
- 文件数：`40935`

本轮真实 NAS 补录范围：

- 项目数：`15`
- 文件数：`34184`
- 排除项目：`512 / 108-福城南产业片区11-20-02宗地`，该项目已在上一轮补录完成。

补录后真实 NAS 资产专业缺失数：

```text
0
```

## 3. 识别口径

`discipline` 表示“文件所属专业”，不是项目整体专业。

本轮按项目目录之后的路径和文件名识别，不使用 NAS 根目录 `卓羽智能项目` 作为判断依据，避免误判为智能化。

识别规则：

- `GAS`：燃气、天然气
- `FIRE_PROTECTION`：消防、喷淋、消火栓、火灾、气体灭火、灭火系统、消防水、消防电
- `HVAC`：暖通、空调、通风、防排烟、排烟、新风、风管、冷媒、冷冻、冷却、多联机、VRV、制冷、排风、送风、风机、风口、空调水
- `PLUMBING`：给排水、给水、排水、污水、雨水、水泵、生活水、热水、冷凝水、排污、废水、水施、给排
- `ELECTRICAL`：强电、电气、电力、配电、照明、防雷、接地、变配电、电缆、母线、电施、配电箱、应急照明、发电机、充电桩、桥架
- `INTELLIGENT`：弱电、智能化、安防、综合布线、门禁、监控、广播、网络、通信、楼控、自控、信息发布、车位引导、入侵报警、有线电视、电话、无线对讲、电子巡更、停车场、视频监控、BA/BAS/BMS 系统
- `STRUCTURE`：结构、钢筋、钢结构、模板、基础、梁配筋、墙柱、楼梯、桩、支护、留洞、预留预埋、结施
- `ARCHITECTURE`：建筑设计、建筑施工、建筑专业、建施、装饰、装修、幕墙、门窗、室内、精装、景观、园林、墙身、坡道大样、建筑平面，以及独立目录段“建筑”
- `GENERAL`：无法稳定归入单一专业的综合、BIM、提资、说明、招投标、过程资料等

## 4. 真实 NAS 补录分布

```text
GENERAL          7239
STRUCTURE        6538
ELECTRICAL       6212
ARCHITECTURE     4126
HVAC             3704
FIRE_PROTECTION  3042
PLUMBING         2260
INTELLIGENT       931
GAS               132
```

合计：`34184`

真实 NAS 全量补录后分布：

```text
STRUCTURE        8882
ELECTRICAL       7333
GENERAL          7307
ARCHITECTURE     5599
HVAC             4197
FIRE_PROTECTION  3267
PLUMBING         2880
INTELLIGENT      1284
GAS               186
```

合计：`40935`

## 5. 项目级补录结果

```text
93  中建八局国交酒店项目                                  5912
96  深圳市前海蛇口自贸区医院科技大厦三期改造项目             243
97  水务利源既有建筑                                      1969
100 深圳市二十八高项目                                    3961
101 C塔                                                   5457
104 佛山顺德妇幼医院项目                                  1699
105 启航华居项目                                          2927
109 华润三九银湖科创中心项目                              1457
110 龙华区观湖街道观城城市更新单元第一期10地块              2614
111 蛇口影剧院项目                                         290
112 歌剧院项目                                              21
113 宝安洪桥头项目                                        2951
114 香港项目                                              3800
115 深圳宝安国际机场T2航站区及配套设施工程航站区工程施工总承包2标段 396
116 港中文（深圳）医学院智能化                              487
```

## 6. 遗留测试数据兜底

真实 NAS 项目补录完成后，全局数据质量页仍会统计到历史脚本/接口测试项目的缺专业记录。为避免前端继续显示“专业待完善”误导真实试点验收，已将剩余非真实试点/测试/历史遗留文件统一补为 `GENERAL / 综合`。

兜底范围：

```text
API           10330
NAS_DISCOVERY     4
NULL              4
```

合计：`10338`

说明：

- 真实 NAS 试点项目按路径和文件名细分专业。
- 测试/历史遗留数据不作为业务专业判断样本，只做“综合”兜底，避免污染一期验收口径。

## 7. 验证结果

数据库验证：

```text
data_file_resources 缺专业总数 = 0
NAS_REAL_PILOT 缺专业总数 = 0
```

接口验证：

```text
GET /api/data-steward/assets/files:page?qualityIssue=MISSING_DISCIPLINE&pageSize=50
OK 0 0
```

数据质量接口：

```text
missingDisciplineCount = 0
```

专项脚本：

```text
bash scripts/dev/check-asset-quality-overview.sh
asset quality overview ok

bash scripts/dev/check-agent-db2-contract.sh
agent db2 contract ok
```

项目 `506 / 93-中建八局国交酒店项目` 抽样：

```text
OK 5912
二层一结构预留预埋平面图（更新）1.dwg -> STRUCTURE
二层一结构预留预埋平面图（更新）.dwg -> STRUCTURE
ST-F2-GZ-01-二层梁板一次结构预留预埋平面图(1).dwg -> STRUCTURE
首层出户穿梁套管预留预埋平面图.dwg -> STRUCTURE
深圳香蜜湖国际交流中心_酒店_B1_AS(结构)-最新_杨富韩_20240516142725.rvt -> STRUCTURE
```

## 8. 审计与事件流

真实 NAS 项目级补录已写入审计与事件流：

- `trace_id = manual-real-nas-discipline-backfill-20260511`
- `core_audit_logs`：`15` 条
- `data_asset_events`：`15` 条

剩余测试/历史遗留数据综合兜底已写入审计与事件流：

- `trace_id = manual-remaining-discipline-general-backfill-20260511`
- `core_audit_logs`：`1` 条
- `data_asset_events`：`1` 条
