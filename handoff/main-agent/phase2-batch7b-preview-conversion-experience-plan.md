# 二期 7B 规划：文件预览转换体验增强

## 1. 批次定位

7B 是 7A 之后的窄批次。

7A 已完成：

- 文件预览策略统一。
- 只读交付包导出预检查。
- 文档/图纸交付页展示预检查统计与明细。

7B 不做真实转换引擎，不做 BIM 轻量化，不做真实打包下载。

本批只解决一个问题：

> 用户已经能看到“可预览 / 需转换 / 暂不支持”等状态，但不同页面的状态表达、下一步提示和交付阻塞解释仍不够统一，不利于客户理解。

## 2. 本批目标

- 在数据管家、文件详情、文件管理、文档交付、图纸交付中统一预览状态展示。
- 把 Office、CAD、BIM 等文件的“需转换 / 转换中 / 转换失败 / 暂不支持 / 仅下载”表达清楚。
- 保持 7A 共享预览策略为唯一口径，不在前端或工作中心再散落一套格式判断。
- 在交付包预检查中更清楚展示“预览转换状态”和“交付导出状态”的区别。
- 给后续真实转换服务预留任务状态接口或 UI 壳，但本批不执行真实转换。
- 继续保证真实 NAS 路径、`storageUri`、raw row、SQL 不出现在新增响应或页面中。

## 3. 推荐技术设计

### 3.1 后端预览策略增强

复用 7A 已新增的：

- `delivery-shared` 中的 `FilePreviewPolicy`
- `PreviewDecision`

建议补强：

- 让 `PreviewDecision` 输出更稳定的用户可读字段，例如：
  - `previewStatus`
  - `previewMode`
  - `previewAvailable`
  - `conversionStatus`
  - `conversionRequired`
  - `downloadOnly`
  - `statusLabel`
  - `actionHint`
  - `riskLevel`
- 如果当前字段已经足够，也可以不扩 DTO，只在前端做统一映射；但映射必须集中到一个工具或组件里，不能散落在多个页面。

状态口径建议：

- PDF / 图片：`AVAILABLE / BROWSER_NATIVE / NOT_REQUIRED`
- Office：`NEEDS_CONVERSION / OFFICE_CONVERSION / NOT_STARTED`
- CAD：`NEEDS_CONVERSION / CAD_CONVERSION / NOT_STARTED`
- BIM：`NEEDS_CONVERSION / BIM_LIGHTWEIGHT / NOT_STARTED`
- 压缩包：`UNSUPPORTED / DOWNLOAD_ONLY / NOT_REQUIRED`
- 未知格式：`UNSUPPORTED / NONE / NOT_REQUIRED`

注意：

- 7B 可以增加“转换任务壳”或“状态查询壳”，但默认状态只能来自元数据和策略判断。
- 不允许读取文件正文。
- 不允许检查真实 NAS 文件内容。
- 不允许调用外部转换引擎。

### 3.2 前端统一展示组件

建议新增或整理一个通用展示能力，例如：

- `PreviewStatusTag.vue`
- `PreviewStatusSummary.vue`
- `previewStatus.ts`

用于统一：

- 标签颜色。
- 状态中文名。
- 用户可理解说明。
- 下一步提示。
- “能不能点预览”与“能不能下载”的差异提示。

必须覆盖页面：

- 数据管家项目详情文件管理区。
- 资产目录详情抽屉。
- 文档交付页。
- 图纸交付页。
- 交付包预检查明细。

### 3.3 预览弹窗体验增强

已有预览能力必须保持：

- PDF / 图片可以走现有受控预览票据。
- 下载权限继续独立于预览权限。
- 访问票据继续短时、受控、审计。

本批增强：

- 对 Office/CAD/BIM 不要只显示“不可预览”。
- 应显示明确原因和后续状态：
  - `需要转换后预览`
  - `当前未接入转换服务`
  - `后续可接入 Office/CAD/BIM 转换或轻量化服务`
- 如果用户点了不可预览文件，应显示业务提示，不应白屏或打开同一个下载页面冒充预览。

### 3.4 交付包预检查表达增强

交付包预检查中必须明确：

- `导出状态` 表示文件是否能纳入未来交付包。
- `预览状态` 表示文件是否能在线查看。
- “需要转换预览”不等于“不能交付导出”。
- 缺失、待审、驳回才是当前交付包准备的主要阻塞。

建议在预检查区域增加轻量说明：

- `预览转换状态只影响在线查看体验，不代表文件不能作为原始文件交付。`

## 4. 明确不做

- 不接入真实 Office 转换服务。
- 不接入真实 CAD 转换服务。
- 不接入真实 BIM 轻量化服务。
- 不生成真实预览缓存文件。
- 不生成真实 ZIP / 交付包。
- 不批量下载。
- 不读取 PDF / Office / DWG / RVT 正文。
- 不写 NAS。
- 不写 OpenSearch、Qdrant、MinIO、documents/chunks。
- 不做 Hermes 写操作、selective indexing 或正文问答。

## 5. 验收标准

- 数据管家与工作中心对同一文件类型展示一致的预览状态。
- PDF / 图片显示可预览。
- Office 显示需 Office 转换。
- CAD 显示需 CAD 转换。
- BIM 显示需 BIM 轻量化。
- 压缩包和未知格式显示暂不支持或仅下载。
- 不可预览文件点击预览时给出清晰业务提示，不白屏、不误下载、不伪装成已预览。
- 文档/图纸交付预检查继续可用，并明确区分预览状态与导出状态。
- 4R 文件访问安全、6B 批量挂接、7A 导出预检查均不回归。
- 新增响应和页面不泄露真实 NAS 路径或底层存储字段。

## 6. 建议回归脚本

新增：

`scripts/dev/check-phase2-batch7b-preview-conversion-experience.sh`

建议覆盖：

- 创建或复用安全 mock/minio 文件元数据，分别覆盖 PDF、图片、Office、DWG、RVT、ZIP、未知格式。
- 调用文件预览接口，验证各格式状态。
- 调用交付包预检查接口，验证行级预览状态仍使用统一口径。
- 扫描响应不得包含 `/Volumes`、`smb://`、`nas://`、`storage_path`、`storageUri`、`raw row`、`SQL`。
- 回归：
  - `check-phase2-batch7a-preview-export-precheck.sh`
  - `check-phase2-batch6b-delivery-package.sh`
  - `check-phase2-batch4-file-access.sh`

## 7. 当前裁决

可以进入 7B 开发。

开发前必须再次强调：

- 7B 是“转换体验增强”，不是“转换能力上线”。
- 真实转换服务、BIM 轻量化、正文抽取和 Hermes 索引都留到后续批次。
