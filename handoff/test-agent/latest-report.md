# M1D 信息结构与标准驱动交付闭环复核报告

生成时间：2026-05-20 18:42 CST

## 1. 测试结论

结论：通过。

本轮只复核 `M1D：标准驱动交付闭环强化`，未进入 G4 / Hermes / 8B / 8C / 9A，未做生产部署。重点验证了项目工作台信息结构、工程主数据未就绪提示、文档/图纸交付闭环可读性、缺失项解释、审核整改闭环、导出预检查 dry-run 和安全边界。

当前未发现 P0 / P1 / P2。建议主 agent 进入 M1D 收口判断。

## 2. 当前分支与工作区

- 当前目录：`/Users/vc/Documents/数字化交付平台`
- 当前分支：`codex/platform-m1d-delivery-loop`
- 当前提交：`b6bbb14`
- 工作区状态：存在开发 agent 本轮未提交改动和临时文件；测试 agent 未修改业务代码，未修改 `docs/**`，仅更新本测试报告。
- 关键新增脚本：`scripts/dev/check-m1d-standard-delivery-loop.sh`

本轮观察到 `127.0.0.1:5173` 曾被另一个旧目录 `数字化交付平台-hermes` 的 Vite 进程占用，已在浏览器验收时切回当前仓库前端后复核。该问题属于本机端口/旧进程干扰，不是产品代码缺陷。

## 3. 构建与健康检查

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 后端健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- 空白字符检查：`git diff --check`，通过。

## 4. 必跑脚本结果

- `bash scripts/dev/check-m1d-standard-delivery-loop.sh`：通过，`PASS=29 FAIL=0`。
- `bash scripts/dev/check-phase2-batch2-standard-delivery.sh`：默认项目 1 因历史标准数据污染导致“标准未就绪”前置断言失败；按提示词要求换干净隔离项目复跑通过，`PASS=38 FAIL=0`，隔离项目和用户已软删除清理。
- `bash scripts/dev/check-phase2-batch3-review-rectification-report.sh`：通过，`PASS=52 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `bash scripts/dev/check-phase2-batch6b-delivery-package.sh`：通过，`PASS=17 FAIL=0`。
- `bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`：通过，`PASS=18 FAIL=0`。
- `bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m1c-real-project-masterdata.sh`：通过，`PASS=14 FAIL=0`。

## 5. 信息结构复核

结果：通过。

- `/data-steward/assets/503` 项目工作台顶部清楚呈现 `项目资产 -> 工程主数据 -> 交付工作中心`。
- 顺序符合要求，且工作中心说明为“工作中心不是工程主数据子页面，它在主数据之后按规则推进交付”。
- 工程主数据未就绪时，项目工作台和项目内导航均出现 `请先生成 / 确认工程主数据草案`。
- 交付工作中心排在工程主数据之后，但没有被表达为工程主数据的子功能。
- 页面无 403 / 404 / 500，无页面级横向溢出。

## 6. 文档/图纸交付页复核

结果：通过。

已浏览器抽查：

- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`
- `/data-steward/assets/503/work/rectifications`
- `/data-steward/assets/506/work/document-delivery`
- `/data-steward/assets/506/work/drawing-delivery`

503 文档/图纸交付页：

- 可见 `标准驱动交付闭环` 说明。
- 可见 `当前下一步` 与动作按钮。
- 状态卡片包含 `应交`、`已补交`、`缺失`、`草稿`、`待审`、`已通过`、`已驳回`、`补交完整率`、`审核通过率`。
- 503 / DRAWING 接口返回 `totalRequired=8`、`missingCount=8`、`nextActionCode=BIND_MISSING_FILES`。
- 503 / DOCUMENT 接口返回 `nextActionCode=DEFINE_DELIVERABLES`，符合“当前视图无应交项，需确认交付物定义覆盖”的状态。

506 文档/图纸交付页：

- 页面可正常进入，无 403 / 404 / 500。
- 当前工程主数据/交付标准未就绪，页面和导航均提示 `请先生成 / 确认工程主数据草案`。
- 接口返回 `standardReady=false`、`nextActionCode=COMPLETE_STANDARD`。
- 因 506 未就绪，页面优先展示阻塞引导而不是完整交付状态卡片，符合当前数据状态。

## 7. 缺失项与远程分页补交

结果：通过。

在 503 图纸交付页切换 `缺失项` 后确认：

- 缺失项说明清楚表达“当前标准要求存在，但还没有选择文件完成交付”。
- 表格包含 `目标`、`交付定义`、`交付类型`、`文件类型`、`为什么缺失`。
- 缺失原因示例可解释为：部位需要交付某类图纸，当前尚未挂接图纸文件。
- 点击 `选择文件补交` 可打开补交弹窗。
- 弹窗明确提示：`文件选择保持远程分页查询，每次最多显示 20 条当前项目已处理完成的图纸文件`。
- 未发现回退到全量加载。

## 8. 审核整改闭环

结果：通过。

脚本已覆盖：

- 挂接后保存为草稿。
- 草稿可提交审核。
- 审核通过后完整率、可导出基础数量和审核通过率刷新。
- 审核驳回后生成整改项。
- 整改项可处理、关闭、重新打开。
- 驳回资料可复审通过并回到交付链路。

浏览器抽查 503 整改页可正常打开，无 403 / 404 / 500。

## 9. 导出预检查与安全边界

结果：通过。

- 交付包导出预检查仍为 dry-run。
- 接口返回 `dryRun=true`、`packageGenerated=false`。
- 页面说明明确：不生成真实文件包，不访问、不复制 NAS 文件。
- 未发生真实 NAS 创建、移动、删除、重命名、上传。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未生成真实交付包。
- 未做 BIM 轻量化、构件级解析、selective indexing。
- 未写 Hermes memory / OpenSearch / Qdrant / MinIO documents/chunks。
- 未触发 Agent 自动审批、自动整改、自动挂接。

## 10. Forbidden-Field Scan

结果：通过。

接口响应与前端可见文本未发现：

- raw NAS path
- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `storageUri`
- raw row
- SQL
- token / secret / password

## 11. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2：未发现。

非阻塞观察：

- 前端构建仍有既有 Vite chunk size warning。
- 批次 2 默认项目 1 存在历史数据污染，不适合作为“标准未就绪”前置断言环境；隔离项目复跑已通过。
- 本机 5173 端口可能被旧 `数字化交付平台-hermes` 前端进程占用，做浏览器验收前需要确认端口对应当前仓库。

## 12. 收口建议

- M1D 是否通过：通过。
- 是否发现 raw path / storage_path / storage_uri / NAS path 泄露：未发现。
- 是否发生真实 NAS 写操作：未发生。
- 是否误进入 Hermes / G4 / 8B / 8C / 9A：未发现。
- 是否建议主 agent 收口 M1D：建议进入收口判断。
