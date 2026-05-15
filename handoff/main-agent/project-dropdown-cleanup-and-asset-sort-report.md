# 当前项目下拉清理与资产总览排序报告

更新时间：2026-05-11

## 结论

已完成本机 dev 环境的测试项目软归档，并在资产总览页增加项目 ID 升序/降序选择。

本轮属于一期试运行可用性修复，不扩展二期能力，不修改真实 NAS 文件。

## 问题

`/home` 页面右上角“当前项目”下拉出现大量非真实 NAS 项目，例如：

- `BIM-PROJ-*`
- `CSV-PROJ-*`
- `XLSX-*`
- `SCANCTRL-*`
- 其他批次验收脚本生成的临时项目

这些项目来自此前开发、测试、回归脚本，不是公司真实 NAS 项目。继续显示会干扰一期试用。

## 处理方式

保留：

```text
SAMPLE-MEP-001 / 机电交付样板项目
SAMPLE-MEP-002 / 机电交付扩展项目
16 个 asset_source = NAS_REAL_PILOT 的真实 NAS 项目
```

软归档：

```text
503 个本机测试项目
```

归档方式：

1. 不物理删除数据库记录。
2. 不删除关联测试数据。
3. 将测试项目标记为 `deleted = 1`、`status = INACTIVE`、`asset_status = ARCHIVED`。
4. 将测试项目编码改为 `ARCHIVED-TEST-{projectId}`，释放原测试编码。
5. 原始项目 ID、编码、名称、来源和状态写入备份表：

```text
dev_archived_test_projects_20260511
```

因此后续如需追溯或恢复，仍可从备份表查回原始信息。

## 当前结果

当前活跃项目：

```text
active_projects = 18
active_sample   = 2
active_real_nas = 16
archived_count  = 503
```

`/api/core/users/me` 当前返回项目数：

```text
projects = 18
```

保留项目清单：

```text
1   SAMPLE-MEP-001  机电交付样板项目
2   SAMPLE-MEP-002  机电交付扩展项目
503 105             启航华居项目
504 100             深圳市二十八高项目
505 101             C塔
506 93              中建八局国交酒店项目
507 96              深圳市前海蛇口自贸区医院科技大厦三期改造项目
508 104             佛山顺德妇幼医院项目
511 97              水务利源既有建筑
512 108             福城南产业片区11-20-02宗地
513 109             华润三九银湖科创中心项目
514 110             龙华区观湖街道观城城市更新单元第一期10地块
515 111             蛇口影剧院项目
516 112             歌剧院项目
517 113             宝安洪桥头项目
518 114             香港项目
519 115             深圳宝安国际机场T2航站区及配套设施工程航站区工程施工总承包2标段
520 116             港中文（深圳）医学院智能化
```

## 代码修复

后端：

- `CurrentUserApplicationService`：当 token 中的当前项目已不可访问时，自动回退到第一个可访问项目，避免归档测试项目后旧登录态直接报错。
- `AuthApplicationService.refresh`：刷新 token 时同样处理当前项目失效场景，避免用户必须手动清理浏览器登录态。

前端：

- `AssetOverviewPage.vue`：在资产总览页顶部增加 `项目ID升序 / 项目ID降序` 选择。
- 默认按项目 ID 升序展示。
- 仅做前端本地排序，不扩大后端接口范围。

## 验证

已通过：

```text
backend ./mvnw -pl delivery-app -am -DskipTests package
frontend corepack pnpm build
curl -fsS http://localhost:8080/actuator/health
```

接口验证：

```text
GET /api/core/users/me
projects = 18

GET /api/data-steward/assets/projects?assetSource=NAS_REAL*
realProjects = 16

GET /api/data-steward/assets/files:page?projectId=506&pageSize=1
OK 5912 1
```

页面访问：

```text
GET http://localhost:5173/home
HTTP 200

GET http://localhost:5173/data-steward/assets
HTTP 200
```

## 注意事项

1. 本轮软归档只清理本机 dev 数据库的试用体验，不影响真实 NAS 原文件。
2. 若后续需要重跑批次脚本，脚本可能再次生成测试项目；这属于测试环境行为，试用前可再次执行同类软归档。
3. 当前项目下拉保留样板项目，是为了不影响主数据、交付标准、MVP 样板链路演示。
