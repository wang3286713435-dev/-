# 扫描任务可控性补强报告

日期：2026-05-09

## 1. 背景

真实 NAS 第二批导入已经验证平台能管理 `100`、`101`、`105`、`93`、`96`、`104` 共 6 个真实项目，但也暴露出一个治理风险：大目录扫描耗时较长，如果任务进度不可见、不能中断、不能续扫，后续全量接入会难以运维。

本轮按用户确认的方向，优先补强扫描任务，不扩大前端，不修改真实 NAS 文件。

## 2. 本轮完成能力

- 扫描任务进度可见：返回当前进度、总量、百分比、最后扫描路径。
- 扫描任务可取消：运行中的任务可请求取消，后端在遍历和处理文件时主动检查取消标记。
- 扫描任务可续扫：已取消或失败的任务可重新启动。
- 续扫去重：同一扫描任务下已生成候选的文件路径不会重复生成候选或正式资产。
- 低价值目录跳过：创建扫描时可开启默认低价值目录跳过，也可传入自定义跳过关键词。
- 扫描报告：任务完成、取消或失败后保留报告字段，包含扫描数、自动入库数、待审核数、失败数、跳过数和失败原因。

## 3. 数据库与接口变更

新增迁移：

- `backend/delivery-app/src/main/resources/db/migration/V14__scan_task_control_and_progress.sql`

主要新增字段：

- `progress_current`
- `progress_total`
- `progress_percent`
- `cancel_requested`
- `skipped_low_value`
- `skipped_directories`
- `last_scanned_path`
- `skip_low_value_directories`
- `skip_directory_keywords`
- `scan_report_json`

新增或增强接口：

- 创建扫描：支持 `skipLowValueDirectories`、`skipDirectoryKeywords`
- `POST /api/data-steward/assets/nas-scans/{scanTaskId}:cancel`
- `POST /api/data-steward/assets/nas-scans/{scanTaskId}:resume`
- `GET /api/data-steward/assets/nas-scans/{scanTaskId}/report`
- 原扫描详情接口返回进度、取消状态、跳过统计和报告摘要。

## 4. 验证结果

- 后端原生构建：通过。
- 后端健康检查：通过。
- 专项回归脚本：`scripts/dev/check-scan-task-control.sh` 通过。

专项脚本覆盖：

- 创建本地临时扫描目录。
- 创建项目与路径映射。
- 启动扫描后能看到运行中进度。
- 中途取消扫描。
- 取消后续扫成功。
- 默认低价值目录与自定义目录均被跳过。
- 空文件不进入正式资产。
- 扫描报告可查询。
- 续扫后正式资产不重复，数据库中目标项目资产数量为预期值。

## 5. 风险与后续建议

- 当前扫描仍是接口触发后同步执行，适合本阶段人工运维和受控试点；全量 10TB 接入前，建议再将真实 NAS 扫描接入后台任务队列和定时巡检策略。
- 真实 NAS 下一步建议选择 1 个 10GB 以内项目，用新能力做一次真实的取消/续扫演练。
- 全量接管前，应固定默认跳过目录清单，并让项目负责人确认哪些目录只做参考资料、哪些目录进入正式资产。

## 6. 结论

扫描任务可控性已达到下一轮真实试点要求。现在可以在不盲目扩大导入范围的前提下，继续做更大项目的小批量验证。

## 7. 页面显示修复

更新时间：2026-05-11

本轮针对扫描任务页发现的两个展示问题完成修复：

- 历史成功任务显示 `0%`：原因是早期扫描任务执行时还没有进度字段，数据库里 `progress_percent` 默认是 `0`，但任务实际已经成功并有扫描/入库统计。
- 项目字段为空：原因是部分早期测试任务或全局扫描任务没有绑定具体项目；另有少量任务只写了项目 ID，未写项目编码。

修复内容：

- 新增迁移 `V16__backfill_scan_task_display_fields.sql`：
  - 回填已绑定项目任务的 `project_code`。
  - 将历史 `SUCCEEDED` 任务的进度回填为完成态。
- 后端扫描任务查询增加兜底：
  - 有 `project_id` 时优先从项目表补出项目编码。
  - 历史成功任务即使进度字段异常，也按完成态返回。
- 前端扫描任务页增加显示兜底：
  - 成功任务始终显示 `100%`。
  - 未绑定项目任务显示为 `全局扫描 / 未绑定具体项目`，不再空白。
- 项目详情页中的扫描任务进度同样增加成功态兜底。
- `scripts/dev/check-scan-task-control.sh` 增加断言：接口返回的成功扫描任务必须显示为完成态。

验证结果：

```text
./mvnw -pl delivery-app -am -DskipTests package
corepack pnpm build
curl -fsS http://localhost:8080/actuator/health
bash scripts/dev/check-scan-task-control.sh
```

数据库抽查：

```text
succeeded_zero_progress 0
project_id_without_code 0
```

接口抽查：

```text
returned 50 bad_succeeded_progress 0
```

边界说明：本轮只修平台扫描任务元数据和前端展示，不修改、不移动、不删除任何 NAS 文件。
