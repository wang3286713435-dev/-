# 非标准 NAS 资料治理清单收尾报告

日期：2026-05-11

## 结论

已完成一期边界内的“非标准资料治理清单”能力。该能力只登记需要治理的一级 NAS 目录，不创建正式项目、不创建路径映射、不扫描文件、不写入 `ProjectAssetView`、`FileAssetView`、`ModelAssetView`，因此不会污染当前真实资产库和企业 agent 的稳定检索视图。

## 本轮新增能力

1. 新增数据库表 `data_asset_nonstandard_directories`，用于记录暂缓入库、重复编号、投标/参考、临时资料、未知编码等目录。
2. 新增 REST API：
   - `GET /api/data-steward/assets/nonstandard-directories`
   - `POST /api/data-steward/assets/nonstandard-directories:discover`
   - `PATCH /api/data-steward/assets/nonstandard-directories/{directoryId}`
3. 新增前端入口：
   - 菜单：`数据管家 / 非标准资料治理`
   - 路由：`/data-steward/nonstandard-directories`
4. 治理记录支持维护：
   - 治理状态
   - 风险类型
   - 建议项目编码与名称
   - 负责人
   - 治理原因
   - Agent 建议
   - 人工结论与依据
5. 重复编号优先级已高于用户暂缓编码，便于 `95`、`99` 这类重复编号问题被清楚识别。

## 风险分类

- `USER_DEFERRED`：用户明确裁决暂缓入库，例如 `98`、`95`、`99` 中不适合直接入库的目录。
- `DUPLICATE_CODE`：一级目录出现重复项目编号，需要人工分配唯一编码。
- `REFERENCE`：投标、参考、样板、标准、专题、综合、资料类目录。
- `TEMP`：临时、转换、中间资料目录，默认标记为忽略。
- `UNKNOWN_CODE`：目录名没有可识别项目编号。
- `OTHER`、`MIXED_COLLECTION`：预留给后续 agent 治理建议和人工分类。

## 已验证

1. 前端构建通过：`corepack pnpm build`。
2. 后端构建通过：`./mvnw -pl delivery-app -am -DskipTests package`。
3. 后端启动与健康检查通过：`/actuator/health` 返回 `UP`。
4. Flyway 已应用到 V15。
5. 临时 NAS 目录发现验证通过：
   - 发现 `6` 条非标准目录。
   - 覆盖 `USER_DEFERRED`、`DUPLICATE_CODE`、`REFERENCE`、`TEMP`、`UNKNOWN_CODE`。
   - 正常项目目录未进入治理清单。
   - 正式项目泄露数 `0`。
   - 正式文件资产泄露数 `0`。
   - 审计和事件流均有记录。
6. 临时测试治理记录已清理，未保留在真实治理清单中。

## 当前边界

1. 本轮没有读取真实 NAS 文件正文。
2. 本轮没有移动、改名、删除 NAS 文件。
3. 本轮没有创建正式项目、路径映射、扫描任务或文件资产。
4. 企业 agent 尚未接入；当前只提供承接 agent 建议和人工结论的数据结构与页面。
5. 后续真实运行时，可在页面中对 `/Volumes/zyzn/卓羽智能项目` 执行“发现目录”，将 `98`、`95`、`99`、投标、参考、未知命名目录登记到治理清单。

## 后续建议

1. 先在真实 NAS 根路径运行一次非标准目录发现，生成真实治理台账。
2. 暂不导入 `98`、`95`、`99` 等目录到正式资产库。
3. 企业 agent 接入后，优先让 agent 对治理清单中的目录生成项目拆分、编码、导入范围、忽略目录和风险原因建议。
4. 人工确认后，再按既有项目资产导入链路创建项目、路径映射和扫描任务。
