# 二期当前路线交接

更新时间：2026-05-28

## 当前主线

当前分支：`codex/m3g-nas-minio-real-project-object-storage`

当前 active 批次：

`M3G-4：受控多项目小批对象化执行`

主线健康度：

`绿灯`

说明：

- M1 / M2 / UX 系列主线能力已形成有效基线。
- M2H / M2I / M2J 已围绕文件管理器和 105 文件归属治理完成收口。
- Hermes 继续冻结，不作为当前主线继续扩展。
- 真实 BIM 引擎未确定，8B / 8C 不启动。
- 9A 客户交付准备不启动。
- M3A / M3B / M3C-0 / M3C-1 / M3C / M3D / M3E 已收口。
- M3F 已收口，新增上传文件已优先进入对象存储。
- M3G-1 已收口，NAS 侧 MinIO readiness、全项目对象化盘点和单项目 dry-run 已可用。
- M3G-2 已收口，105 项目历史文件完成受控小批对象化灰度。
- M3G-3 已收口，多真实项目对象化规划和 dry-run 已可用。
- 当前 active 批次为 `M3G-4：受控多项目小批对象化执行`。

## 当前最新裁决

`M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划` 已正式收口。

M3G-1 只做：

- NAS 侧 MinIO readiness。
- 全项目对象化覆盖率盘点。
- 单项目对象化 dry-run 计划。

M3G-1 不做：

- 不执行真实历史文件对象化迁移。
- 不移动、删除、重命名真实 NAS 文件。
- 不做 Hermes 正文问答。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不读取文件正文。

M3G-1 收口依据：

- 收口记录：`handoff/main-agent/m3g1-nas-minio-readiness-inventory-closure.md`
- 开发报告：`handoff/dev-agent/latest-report.md`
- 测试报告：`handoff/test-agent/latest-report.md`
- 配置交接：`handoff/main-agent/m3g1-nas-minio-ops-preparation.md`

以下为历史收口记录：

`M3A：对象存储与 StorageService 基线` 已正式收口。

M3A 已完成：

- 新增对象存储元数据模型。
- 新增统一 StorageService。
- 现有 `file-access` 内部改走 StorageService，保持外部契约兼容。
- 新增 provider health 和 file storage status API。
- 保持 NAS 文件原有访问不回归。
- 不暴露真实 NAS 路径、bucket、object key、storage URI。

M3A 明确未做：

- 全量 NAS 迁移。
- 语义解析。
- Hermes 正文问答。
- 真实 BIM 轻量化。
- 真实 NAS 文件移动、删除、重命名。

M3A 收口依据：

- 计划：`handoff/main-agent/m3a-storage-service-foundation-plan.md`
- 收口报告：`handoff/main-agent/m3a-storage-service-foundation-closure.md`
- 开发报告：`handoff/dev-agent/latest-report.md`
- 测试报告：`handoff/test-agent/latest-report.md`

M3B 已完成：

- 只选 105 少量安全样本文件。
- NAS 原文件保留不动。
- 上传 MinIO / S3-compatible 镜像。
- 校验 etag / checksum / size。
- 写入对象版本记录。
- storage-status 显示对象已存储。
- 迁移后仍走受控 file-access。

M3B 明确未做：

- 全量 NAS 迁移。
- 语义解析。
- Hermes 正文问答。
- 真实 BIM 引擎接入。

M3B 收口依据：

- 计划：`handoff/main-agent/m3b-object-storage-mirror-trial-plan.md`
- 收口报告：`handoff/main-agent/m3b-object-storage-mirror-trial-closure.md`
- 开发报告：`handoff/dev-agent/latest-report.md`
- 测试报告：`handoff/test-agent/latest-report.md`

后续阶段顺序：

1. `M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划`（当前）
2. `M4：语义证据层`
3. `M5：Hermes 受控证据问答`

后续 M3-M5 统一任务图：

- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- 后续 M3 系列批次应以该任务图为准，完成一项勾选一项。
- 未完成契约冻结、资产 UUID 与证据边界前，不得直接进入 Hermes 正文问答。

当前任务图进度：

- `M3C-0：资产存储与证据链契约冻结` 已完成。
- `M3C-1：资产 UUID 与存储状态统一` 已收口。
- `M3C：对象存储迁移任务中心与批量策略` 已完成。
- `M3D：真实 NAS 小范围灰度镜像` 已完成。
- `M3E：预览与转换产物对象化` 已完成。
- `M3F：新文件对象存储优先写入与 NAS 兼容回退` 已收口。
- 下一步优先级：
  1. `M3G-4：受控多项目小批对象化执行`。
  2. `M4A：documents / chunks 语义证据契约`

以下为历史收口记录：

`M2G：真实 NAS 文件管理器灰度完善` 已通过测试 agent 轻量验收并由主 agent 判定正式收口。

`M2H：Windows 风格文件管理器交互升级 + 目录直达子项返工` 已通过测试 agent 极短复验并由主 agent 判定正式收口。

M2G 已完成：

- 文件管理器按路由项目判断 NAS 写权限，不再用全局 `currentProject` 兜底。
- 当前文件夹可写 / 不可写状态和原因可见。
- 当前文件夹重命名、移动、移入回收站收进下拉。
- 移动目标增加项目内相对目录校验。
- 操作记录和回收站状态改成业务语言。
- M2G 专项脚本只在隔离测试项目和 `/tmp/delivery-m2g-nas-*` 临时目录执行真实写闭环。

M2G 收口依据：

- 计划：`handoff/main-agent/m2g-real-nas-file-manager-polish-plan.md`
- 开发报告：`handoff/dev-agent/latest-report.md`
- 测试报告：`handoff/test-agent/latest-report.md`
- 验收通过：
  - 后端构建。
  - 前端构建。
  - 健康检查。
  - M2G 专项脚本，`PASS=26 FAIL=0`。
  - M2B 回归，`PASS=18 FAIL=0`。
  - 文件访问安全回归，`PASS=18 FAIL=0`。
  - `git diff --check`。

M2H 已完成：

- 文件管理器右侧主列表同时承载文件夹和文件。
- 支持单选、多选、连选、右键菜单、双击进入。
- 将原“更多”里的常用操作自然迁移到上下文菜单。
- PDF / 图片走受控预览；Office / CAD 走现有预览状态；模型文件只打开占位，不伪装成真实 BIM 渲染。
- 批量下载只创建受控下载票据，不生成 ZIP，不复制 NAS 文件。
- P1 修复后，PDF 双击和右键 `打开 / 预览` 均走受控 `file-access` 入口，不再出现未处理 `cancel`。
- 返工后，文件管理器右侧按 Windows 资源管理器口径显示当前目录直接子文件夹和直接文件。
- `catalog/files` 新增 `directOnly` 参数；文件管理器使用 `directOnly=true`，旧递归目录查询默认兼容保留。
- 左侧目录树不再公共前缀压缩，105 / 503 真实项目目录层级完整展示。

原建议的“多真实项目复制试点”顺延，后续如需要可作为 `M2I` 单独启动。

105 项目演示模板数据已重置。`M2D：真实项目工程主数据接入草案增强` 已通过测试 agent 轻量验收并由主 agent 判定收口。`M2E：真实项目工程主数据人工确认与交付规则落地` 已完成开发、P1 修复和测试 agent 极短复验，主 agent 判定正式收口。

M2E 目标：

- 将 105 的真实资产草案通过人工确认转成正式工程主数据。
- 生成可维护的部位树、节点类型、交付物定义、交付物类型和目录线索。
- 明确来源：资产线索、人工确认、行业参考。
- 生成后让文档/图纸交付和交付包 / 档案目录基于正式规则工作。

M2E 不做：

- 真实 NAS 文件操作。
- 文件正文读取。
- Hermes 新能力。
- BIM 引擎接入。
- 自动挂接、自动审核、自动整改。
- 客户交付准备。

M2E 收口依据：

- 计划：`handoff/main-agent/m2e-real-project-masterdata-confirmation-plan.md`
- 开发报告：`handoff/dev-agent/latest-report.md`
- 测试报告：`handoff/test-agent/latest-report.md`
- P1 `selectedDraftItemIds` 契约已修复并通过极短复验：
  - 空选择被拒绝。
  - 非法选择被拒绝。
  - 合法小选择只处理所选草案项及必要依赖。
  - 不再全量返回 40 项。

下一批次：

`M2F：真实项目交付闭环试运行`

M2F 已收口：

- 以 `105 / 503` 为代表项目，已验证正式工程主数据能驱动交付。
- 文档 22 个应交项、图纸 22 个应交项，当前均待人工补交。
- 缺失项解释已能说明目标部位、交付定义、交付类型和需要补交的文件类型。
- 推荐挂接仍需要人工确认。
- 审核 / 整改查询和交付包草案 dry-run 链路不回归。
- 未触碰真实 NAS 文件，未读取正文，未新增 Hermes / BIM / parser / indexing。

M2F 收口依据：

- 计划：`handoff/main-agent/m2f-real-project-delivery-loop-trial-plan.md`
- 开发报告：`handoff/dev-agent/latest-report.md`
- 测试报告：`handoff/test-agent/latest-report.md`

下一批次建议：

`M2G：真实 NAS 文件管理器灰度完善`

M2G 当前目标：

- 打磨文件管理器作为平台核心能力。
- 修复新建文件夹等操作的项目上下文误判。
- 让上传、新建、重命名、移动、删除到回收站、恢复形成顺畅闭环。
- 当前目录可写状态、失败原因、操作记录和回收站体验更清楚。
- 自动化写入只在隔离测试项目或隔离目录执行，不写真实业务目录。

M2G 不做：

- 普通用户永久删除。
- 文件正文读取。
- Hermes 新能力。
- BIM 轻量化或构件解析。
- parser / writer / indexing。
- 数字孪生继续扩展。

M2G 交接：

- 计划：`handoff/main-agent/m2g-real-nas-file-manager-polish-plan.md`
- 开发 prompt：`handoff/dev-agent/current-prompt.md`
- 测试 prompt：`handoff/test-agent/current-prompt.md`

M2D 已完成：

- 105 / 503 保持 `deliverableStandardReady=false`，没有重新生成模板假交付标准。
- 接入评估返回真实资产统计、扩展名分布、专业分布、治理风险和 Missing Evidence。
- 草案预览返回 catalog-only 证据、证据来源、置信度、风险提示和人工确认标记。
- 文档 / 图纸交付与交付包接口未生成虚假应交项或虚假交付包。
- 未泄露真实 NAS 路径，未触碰真实 NAS 文件，未新增 Hermes / BIM / parser / writer / indexing 能力。

M2D 验收已通过：

- 后端构建。
- 前端构建。
- 健康检查。
- M2D 专项脚本，`PASS=8 FAIL=0`。
- M1C 回归。
- M2C 回归。
- `git diff --check`。

`M2C：交付包与档案目录能力` 已通过开发自测和测试 agent 验收，主 agent 判定可以正式收口。

M2C 已完成：

- 交付包草案。
- 档案目录。
- 交付清单导出。
- 交付项阻塞原因解释。
- 项目内交付成果组织。

M2C 已确认未做：

- 真实 NAS 文件复制。
- 真实 NAS 文件移动、删除、重命名。
- 真实压缩包生成。
- 真实 BIM 轻量化。
- Hermes 新能力。
- 文件正文读取。
- 向量库 / 搜索库写入。

## 真实 NAS 写入灰度当前口径

项目 `504 / 100 / 深圳市二十八高项目` 进入受控灰度试运行。

灰度原则：

- 只通过项目级配置开启。
- 只允许配置的相对目录范围。
- 只允许配置的项目角色和账号。
- 所有操作必须经过后端权限、路径、审计校验。
- 回收站恢复可用，永久删除仍不作为普通试运行能力开放。

用户可见命名统一为：

`回收站`

内部接口、数据库字段、状态码仍保留 `quarantine` / `QUARANTINED`，用于兼容既有接口和测试脚本。

## M2C 交付要求

M2C 已满足以下交付要求：

1. 打开 `交付包 / 档案目录`。
2. 查看当前交付包准备状态。
3. 区分可交付、缺失、待审核、已驳回、阻塞项。
4. 生成交付包草案。
5. 查看草案条目。
6. 查看档案目录。
7. 导出交付清单。

交付清单必须明确：

- `dryRun=true`
- `physicalPackageGenerated=false`
- `nasFileCopied=false`

## M2C 验收基线

已通过：

- 后端构建。
- 前端构建。
- 健康检查。
- M2B 回归。
- M2A 回归。
- M1F 回归。
- M1E 回归。
- M1D 回归。
- M1C 回归。
- `git diff --check`。

已验证：

- 105/503 与 93/506 或另一个真实 NAS 项目。
- 不泄露真实 NAS 路径。
- 不复制真实 NAS 文件。
- 不生成真实压缩包。
- OpenAPI 包含新增接口。

## M2D 交付要求

M2D 完成后，105 项目应能清楚展示：

1. 真实资产已接入。
2. 工程主数据未确认。
3. 文件类型、专业、目录和质量风险来自 catalog metadata。
4. 草案项区分资产线索和模板骨架。
5. 每条草案有证据来源、证据模式、置信度、风险提示和人工确认标记。
6. 真实 NAS 项目不能一键套模板后直接变成交付标准已就绪。

M2D 不做：

- 真实 NAS 文件操作。
- 文件正文读取。
- Hermes 新能力。
- BIM 引擎接入。
- 自动挂接、自动审核、自动整改。
- 客户交付准备。

## M3E 收口状态

最近完成批次：

`M3E：预览与转换产物对象化`

当前分支：

`codex/m3e-preview-artifacts-object-storage`

M3E 已完成：

- 接入预览产物 / 转换产物与对象存储的关系。
- PDF / 图片等浏览器原生预览文件可形成对象化 preview artifact。
- DWG / RVT / Office 等文件只形成需转换 placeholder，不伪造可预览。
- file-access 仍是唯一受控访问入口。

M3E 已确认未做：

- Hermes 正文问答。
- documents / chunks / 语义索引。
- 真实 CAD / BIM 转换。
- DWG / RVT 深层内容解析。

## M3G-2 收口状态

最近完成批次：

`M3G-2：105 项目历史文件对象化上传灰度`

本批已正式收口。

完成情况：

- 105 首批历史文件已对象化。
- 对象化文件通过受控 `file-access` 读取。
- 未对象化文件继续以 `NAS_ONLY` 走 NAS 链路。
- NAS 原项目资料未移动、未删除、不改名。

当前读取策略：

- 已有 active object version 的文件，优先通过对象存储读取。
- 没有 active object version 的文件，继续走原 NAS 台账链路。
- 标记 `OBJECT_STORED` 但对象副本不可读时 fail-closed，不静默回退 NAS。

M3G-2 完成后，后续进入多项目分批对象化前必须先规划容量、速率、并发、失败重试和运维观察窗口。

## M3G-3 收口状态

最近完成批次：

`M3G-3：多真实项目分批对象化策略与任务中心增强`

本批已正式收口。

完成情况：

- 多项目对象化盘点已增强，能够区分真实项目与测试 / 样例项目。
- 多项目 dry-run 能按项目生成计划，并回显文件数、容量、并发、限速、单项目和总量限制。
- dry-run 不创建真实迁移任务、不复制文件、不修改 NAS。
- 文件服务页已提供“多项目对象化规划”入口。

M3G-3 收口后，再评估是否进入 `M3G-4：受控多项目小批对象化执行`。

下一步候选：

`M3G-4：受控多项目小批对象化执行`

M4A 语义证据契约继续后置，不直接进入向量库写入或 Hermes 正文问答。

## 后续路线

M2E 收口后再评估：

1. `M2F：数据管家客户版工作区深化`
   - 模型集成、管理对象、事项、任务、导出列表、文件服务等工作区收束。
2. `M2G：内部局域网试运行反馈闭环`
   - 员工真实使用、权限、NAS 写入灰度、交付包草案、bug 修复。
3. `8B-0：BIM 引擎选型与接入前置评估`
   - 厂商、格式、部署、授权、转换产物、Viewer 能力矩阵。
4. `8B / 8C`
   - 仅在 8B-0 通过且用户确认后进入真实 BIM 引擎接入。
5. `9A`
   - 仅在平台本体、数据管家、交付包、BIM 路线和部署运维方案都稳定后启动。
