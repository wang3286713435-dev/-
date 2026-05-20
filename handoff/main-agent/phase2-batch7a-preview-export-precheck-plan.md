# 二期 7A 规划：文件预览策略与交付包导出前置能力

## 1. 批次定位

7A 是 6B 之后的窄批次，不做真实打包下载，不做转换引擎，不做 BIM 轻量化。

本批只解决两个问题：

1. 交付场景里文件预览状态口径不统一。
2. 用户缺少“如果以后生成交付包，当前哪些文件能进包、哪些还阻塞”的只读预检查。

## 2. 本批目标

- 统一 PDF、图片、Office、CAD、BIM、归档包、未知格式的预览状态说明。
- 将现有数据管家文件预览策略抽成可复用策略，避免工作中心另写一套冲突规则。
- 新增只读交付包导出预检查接口。
- 前端在文档/图纸交付页展示“导出预检查”结果。
- 保持预览权限和下载权限分离。
- 所有新增响应禁止返回真实 NAS 路径、`storageUri`、`storage_path`、raw row、SQL。

## 3. 推荐技术设计

### 3.1 共享预览策略

在 `delivery-shared` 增加文件预览策略类，例如：

- `FilePreviewPolicy`
- `PreviewDecision`

由扩展名和文件类型推导：

- `AVAILABLE / BROWSER_NATIVE`：PDF、图片、SVG 等浏览器可原生打开格式。
- `NEEDS_CONVERSION / OFFICE_CONVERSION`：Office、WPS、表格、PPT。
- `NEEDS_CONVERSION / CAD_CONVERSION`：DWG、DXF、DGN。
- `NEEDS_CONVERSION / BIM_LIGHTWEIGHT`：RVT、IFC、NWD、NWC、GLB、GLTF、MODEL。
- `UNSUPPORTED / DOWNLOAD_ONLY`：ZIP、RAR、7Z。
- `UNSUPPORTED / NONE`：未知格式。

数据管家已有 `getFilePreview` 继续负责权限、票据、生命周期和文案；但格式判断尽量复用共享策略。

### 3.2 交付包导出预检查

新增接口建议：

```http
GET /api/work-center/projects/{projectId}/delivery-package/export-precheck?viewType=DOCUMENT&targetType=SECTION
```

响应字段建议：

- `projectId`
- `viewType`
- `targetType`
- `dryRun=true`
- `packageGenerated=false`
- `totalCount`
- `readyCount`
- `blockedCount`
- `missingCount`
- `pendingReviewCount`
- `rejectedCount`
- `conversionRequiredCount`
- `unsupportedPreviewCount`
- `rows`

每行建议包含：

- 交付定义/类型。
- 目标类型、目标名称。
- 文件 ID、文件名、文件类型、版本。
- 审核状态、准备状态。
- 预览状态、预览方式、转换状态、是否需要转换。
- `exportStatus`：`READY / MISSING / REVIEW_REQUIRED / REJECTED / NO_FILE / BLOCKED`。
- `blockReason`：用户可理解原因。

注意：

- 预检查不读取真实文件正文。
- 预检查不验证 NAS 物理可读性。
- 预检查不创建压缩包、不创建下载票据、不写 NAS。
- 文件是否能“进入交付包”的首要条件是：已挂接且审核通过。
- 预览是否需要转换只作为提示，不阻塞未来文件包导出。

### 3.3 前端交互

在 `DeliveryViewPanel.vue` 的交付包准备区域增加：

- `导出预检查` 按钮。
- 预检查说明：`只读检查，不生成文件包，不访问或复制 NAS 文件。`
- 统计卡片：可纳入导出、阻塞、缺失、待审、驳回、需转换预览、暂不支持预览。
- 明细表：交付定义、目标、文件、审核、预览状态、导出状态、阻塞原因。

继续保留：

- 6B 交付包准备视图。
- 批量挂接。
- 审核/驳回/整改。
- 文件选择远程分页。

## 4. 明确不做

- 不生成真实 ZIP/交付包。
- 不下载批量文件。
- 不新建、移动、删除、上传、改名 NAS 文件。
- 不读 PDF/Office/DWG/RVT 正文。
- 不做 BIM 轻量化转换。
- 不写 OpenSearch、Qdrant、MinIO、documents/chunks。
- 不让 Hermes 自动审核、自动整改或写库。

## 5. 验收标准

- 新增预检查接口出现在 OpenAPI。
- 接口按项目上下文校验权限。
- 响应包含 `dryRun=true` 和 `packageGenerated=false`。
- 已审核通过的挂接项为 `READY`。
- 缺失项为 `MISSING`。
- 待审项为 `REVIEW_REQUIRED`。
- 驳回项为 `REJECTED`。
- Office/CAD/BIM 文件显示需转换预览，但不阻塞交付包预检查。
- 所有新增响应不得包含真实 NAS 路径或存储字段。
- 前端能打开预检查面板，页面不横向撑爆。
- 6B、4R、6A 回归脚本继续通过。
