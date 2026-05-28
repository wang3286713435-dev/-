# 开发报告：8C-GD1-F2 嵌入平台 BIM 协同管理预留接口

时间：2026-05-28 CST

## 0. 最新结论

本轮把葛兰岱尔 Viewer 从“独立预览页”抽成可复用组件，并嵌入到平台现有的 `BIM 协同管理` 页面下方模型预览区。用户进入 BIM 协同管理后，可以在 105 RVT 试点模型列表中选择模型，并直接在页面内看到葛兰岱尔真实 Viewer，而不是跳到孤立页面或只看到灰色占位。

本轮仍保持边界：未改后端转换任务、未改 M3 对象存储主线、未触碰真实 NAS 文件、未暴露 token / NAS 路径 / bucket / object key。

## 1. 改动文件

- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `frontend/src/modules/visualization/pages/GlandarModelPreviewPage.vue`
- `frontend/src/modules/visualization/pages/DigitalTwinDashboardPage.vue`
- `handoff/dev-agent/latest-report.md`

## 2. 实现说明

- 新增 `GlandarViewerCanvas.vue`，统一封装 viewer ticket 获取、Glendale 脚本加载、模型加载、鼠标旋转/平移/缩放、构件选择、适配/主视角/截图/测量工具。
- `GlandarModelPreviewPage.vue` 改为复用该组件，保留原独立大窗口预览入口和刷新 Viewer 能力。
- `DigitalTwinDashboardPage.vue` 在“105 RVT 试点模型”区域增加“下方预览”操作和内嵌 `葛兰岱尔真实模型预览` 区域。
- 旧的“大窗口”入口保留，当前项目内嵌预览和独立预览共用同一套平台 viewer ticket 接口。

## 3. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS（18088）
8C-GD1 专项脚本 PASS=10 FAIL=0
Phase2 batch4 文件访问安全回归 PASS=18 FAIL=0
viewer ticket 禁出字段扫描 PASS
10 个 RVT 试点模型状态接口 PASS（ready=10）
git diff --check PASS
git diff --cached --check PASS
```

补充说明：当前主线工作区仍在 M3G 分支且有未收口改动，因此本轮未直接合入主线。为避免分支污染，8B-GD 分支先完成提交/推送；待 M3G 合回 `main` 后，再将本分支合入 `main` 并用常规 `5173/8080` 入口预览。

## 4. 已知边界

- 本轮只实现平台内嵌 Viewer 复用，不新增转换能力。
- 当前只覆盖 105 项目 10 个 RVT 试点模型。
- 不做构件级业务联动、图模联动或全量模型轻量化承诺。
- Safari 本地登录在 `5187` 端口受 CORS 限制，已改用 `5173` 常规入口验证接口链路；合回主线后不再需要临时端口。

---

# 开发报告：8C-GD1-F1 复用葛兰岱尔 Demo 三维视图交互层

时间：2026-05-28 CST

## 0. 最新结论

本轮只修平台内葛兰岱尔 Viewer 交互层，未改后端转换任务、未改对象存储主线、未触碰真实 NAS 文件。

已将引擎 demo 中验证过的鼠标三维交互逻辑迁入平台 Viewer：

- BIM 模式：左键拖动旋转、右键拖动平移、滚轮缩放。
- 预留 CAD 模式映射：左键平移、右键旋转、滚轮缩放。
- Viewer 加载后会主动重建引擎鼠标事件、恢复 camera controls、绑定 canvas 事件。
- 页面卸载、刷新、重新加载 Viewer 时会清理旧事件，避免重复绑定。
- 增加构件选择、定位、隐藏的最小工具入口，但不做业务联动、不承诺图模联动。

## 1. 改动文件

- `frontend/src/modules/visualization/pages/GlandarModelPreviewPage.vue`
- `handoff/dev-agent/latest-report.md`

## 2. 实现说明

本轮从葛兰岱尔 demo 的 `viewerTools.js` / `app.js` 中抽取并平台化以下逻辑：

- `navigationBindings`
- `panTruckDelta`
- `pointerPickIntent`
- `screenToEnginePickPosition`
- `requestViewerRender`
- `updateCameraControls`
- `applyMouseNavigation`
- `reconnectCameraControls`
- pointer / wheel / contextmenu 事件处理
- 最小构件拾取与高亮入口

保留平台安全边界：

- Viewer ticket 仍由平台后端签发。
- 前端不接触葛兰岱尔 token。
- 页面不展示 NAS 路径、`storage_uri`、bucket、object key、SQL 或 raw row。
- 不新增 Hermes、BIM 构件业务联动、parser、indexing、上传或转换能力。

## 3. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS（18088）
8C-GD1 专项脚本 PASS=10 FAIL=0
Phase2 batch4 文件访问安全回归 PASS=18 FAIL=0
git diff --check PASS
git diff --cached --check PASS
```

浏览器短验：

- 打开 `http://127.0.0.1:5173/visualization/glandar-viewer?projectId=503&jobId=7...`。
- 模型可见，不是灰屏。
- 左键拖动后模型视角变化。
- 右键拖动和滚轮缩放无控制台错误。
- “适配 / 主视角 / 截图 / 距离 / 清除”点击无报错。
- 刷新 Viewer 后再次拖动仍正常，没有事件重复绑定导致的异常。

## 4. 已知边界

- 本轮只把 demo 的基础三维交互迁入平台 Vue Viewer。
- 构件选择、定位、隐藏仍是 Viewer 层最小能力，不代表已经完成业务级构件管理。
- 本轮不扩展 DWG / IFC / NWD / NWC，不影响 10 个 RVT 试点范围。
- 本轮不进入 M3 / M3G 对象存储主线。

---

# 开发报告：8C-GD1 10 个 RVT 轻量化预览试点

时间：2026-05-28 CST

## 0.1 灰屏问题修复（2026-05-28 18:02 CST）

用户反馈：进入已轻量化模型预览后中间区域为灰色，看不到模型。

已定位并修复两个问题：

1. Viewer 页面使用 query 中的 `projectId=503`，但打开时登录态可能仍停留在其他项目，导致后端返回“请先切换到目标项目”。现已在 Viewer 页面加载前自动切换到目标项目上下文。
2. 葛兰岱尔引擎初始化时页面容器没有固定可计算高度，并且打开了引擎帧率小窗，导致引擎生成的 canvas 尺寸异常。现已按 demo 口径关闭 `frameRate`，并固定 viewer 容器尺寸。

复验结果：

- `http://127.0.0.1:5173/visualization/glandar-viewer?projectId=503&jobId=7...` 可直接打开。
- 页面状态显示 `已轻量化 / Viewer 可打开`。
- 中间视窗已渲染出模型构件，不再是灰色空白。
- canvas 尺寸恢复为正常视窗大小。
- 前端构建通过。

## 0. 最新结论

8C-GD1 已在独立葛兰岱尔分支完成平台内 10 个 RVT 试点模型闭环：

- 试点项目固定为 105 项目，本地 `projectId=503`。
- 试点文件固定为：`1257, 1261, 1264, 3730, 1258, 1251, 1259, 1262, 3729, 1243`。
- 10 个试点 RVT 已全部达到 `READY / 已轻量化 / viewerAvailable=true`，均可签发受控 viewer ticket。
- 新增平台内葛兰岱尔 viewer 页面，迁移 demo 的 Glendale 脚本加载、`root.glt` 加载和基础查看工具栏。
- 文件管理器中 RVT 文件可显示轻量化状态；已轻量化文件双击进入平台 viewer，未转换试点文件可提交转换。
- BIM 协同管理页新增“105 RVT 试点模型”区域，展示 10 个模型状态、刷新状态和打开预览入口。

本批仍保持边界：

- 未修改主 M 系列工作区。
- 未修改 `docs/**`。
- 未新增 Hermes、parser、documents/chunks、Qdrant、OpenSearch。
- 未移动、删除、重命名真实 NAS 文件。
- 前端不接触葛兰岱尔 token。
- API / 前端响应禁出字段扫描通过，未暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object_key、secret、SQL、raw row。

## 1. 改动文件

- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/controller/VisualizationAdapterController.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/dto/VisualizationDtos.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/LightweightJobRepository.java`
- `frontend/src/modules/visualization/api/visualization.ts`
- `frontend/src/modules/visualization/pages/GlandarModelPreviewPage.vue`
- `frontend/src/modules/visualization/pages/DigitalTwinDashboardPage.vue`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/router/index.ts`
- `scripts/dev/check-8c-gd1-ten-rvt-platform-preview.sh`
- `handoff/dev-agent/latest-report.md`

## 2. 后端实现

新增平台语义试点接口：

```text
GET  /api/visualization-adapter/projects/{projectId}/glandar/rvt-pilot-files
POST /api/visualization-adapter/projects/{projectId}/glandar/rvt-pilot-files:submit
```

说明：

- 只允许 `projectId=503` 的固定 10 个 RVT 试点文件进入真实葛兰岱尔转换路径。
- 试点清单返回文件名、平台文件 ID、assetUuid、轻量化状态、进度、viewer 可用性、失败原因和用户动作提示。
- 批量提交接口仍走现有权限校验、文件校验、StorageService 受控读取和葛兰岱尔 adapter。
- `GLANDAR` 未配置或不可用时返回业务化阻断，不返回 500。

## 3. 前端实现

新增平台内 viewer 页面：

```text
/visualization/glandar-viewer?projectId=503&jobId={jobId}&fileName={name}
```

页面行为：

- 先向平台后端申请 viewer ticket。
- 从平台返回的 `engineStaticBase` 加载 Glendale 脚本。
- 使用受控 `modelAccessAddress` 加载 `root.glt`。
- 提供适配视图、主视图、截图、距离测量、清空测量等基础工具。
- 加载失败时显示业务化错误，不暴露 token 或底层存储信息。

文件管理器：

- RVT / BIM 文件优先读取 10 个试点模型状态。
- 已轻量化：显示 `已轻量化`，双击或右键打开平台 viewer。
- 未转换试点：提示可提交转换，并调用平台转换接口。
- 非试点模型：保留模型预览占位，不伪装真实可预览。

BIM 协同管理：

- 新增“105 RVT 试点模型”区域。
- 展示 10 个模型的状态、进度、文件 ID、大小、操作建议和打开预览入口。
- 可刷新单个 job 状态，可提交 10 个试点转换。

## 4. 验证结果

已通过：

```text
后端构建 PASS
前端构建 PASS（仅既有 Vite chunk warning）
健康检查 PASS（18088 / GLANDAR）
8C-GD1 专项脚本 PASS=10 FAIL=0（要求 10 个 RVT 全部 READY）
8C-GD1 提交未转换样本路径 PASS=10 FAIL=0
Phase2 batch4 文件访问安全回归 PASS=18 FAIL=0
8A Mock 回归 PASS=11 FAIL=0（独立 MOCK 进程 18089）
git diff --check PASS
git diff --cached --check PASS
前端 viewer 路由加载 PASS（5187）
```

专项脚本：

```text
scripts/dev/check-8c-gd1-ten-rvt-platform-preview.sh
```

本批额外验证：

- `fileId=1257,1261,1264,3730,1258,1251,1259,1262,3729,1243` 已全部轻量化并可签发 viewer ticket。
- 试点任务 jobId 范围：`7-16`，均为 `READY / 100%`。
- OpenAPI 已包含 RVT 试点清单接口。
- viewer ticket 返回 Glendale 脚本地址和 `root.glt` 模型入口。
- 禁出字段扫描通过。

## 5. 风险与后续

当前可以宣称：

- 105 项目已有平台内葛兰岱尔 10 个 RVT 试点模型清单。
- 10 个试点 RVT 均可在平台中以“已轻量化”状态打开 viewer。
- 文件管理器与 BIM 协同管理页已经接入真实 viewer 入口。

仍需注意：

- 本批只做 10 个 RVT 试点，不等于全项目模型都已轻量化。
- 本批不做 DWG / IFC / NWD / NWC。
- 本批不做构件级定位、高亮、剖切、图模联动等业务联动。
- 大文件分片上传仍依赖葛兰岱尔 Station 后续稳定合并逻辑。

建议下一步：

1. 让测试 agent 按 8C-GD1 专项 prompt 做分支验收。
2. 验收通过后提交 `codex/8b-gd2-rvt-poc`。
3. 后续进入 `8C-GD2`，补齐 viewer 浏览器交互细节、状态轮询体验和更多 RVT 样本。

---

# 开发报告：8B-GD2 葛兰岱尔 RVT PoC 真实提交闭环

时间：2026-05-28 CST

## 0. 最新结论（2026-05-28 15:30 CST）

8B-GD2 已修复并跑通真实 RVT PoC：

- Station 侧诊断确认：此前不是 token 问题，平台确实打到了 `SplitUploadFile`，但分片合并阶段没有入库。
- 平台侧补充排查发现：Station 的 `upload-file` 现场实现实际需要 `multipart/form-data` 的 `file + input`，不是文档里的 raw binary body。
- 已将 64MB 以内小文件直传改成 multipart `file + input`；105 项目 RVT 样本 `fileId=1257` 已能真实提交、轮询到 READY，并签发受控 Viewer 入口。

最终专项结果：

```text
BASE_URL=http://127.0.0.1:18088 PROJECT_ID=503 MODEL_FILE_ID=1257 \
  POLL_SECONDS=900 POLL_INTERVAL=15 \
  bash scripts/dev/check-8b-gd2-glandar-rvt-poc.sh

PASS=11 FAIL=0
ALL PASS
```

本轮仍保持边界：

- 未输出或提交 token。
- 未修改主 M 系列工作区。
- 未修改 `docs/**`。
- 未新增 Hermes / parser / documents / chunks / Qdrant / OpenSearch。
- 未移动、删除、重命名真实 NAS 文件。
- API 响应禁出字段扫描通过，未暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object_key、secret、SQL、raw row。

## 1. 本轮目标

本轮执行 `8B-GD2：RVT PoC 转换闭环`，在独立分支 `codex/8b-gd2-rvt-poc` 中完成平台侧最小真实对接：

- 以 105 项目样本 RVT（本地项目 ID `503`，样本文件 ID `1257`）作为 PoC。
- 平台先校验项目上下文和文件权限，再通过 `StorageService` 受控读取模型流。
- 后端调用葛兰岱尔 Station 上传接口；大文件保留 `SplitUploadFile` 分片，小样本 RVT 走已验证的 multipart `upload-file`。
- 本地记录轻量化任务，支持查询状态和获取受控 Viewer 启动信息。
- 不让前端直连 Station API，不暴露 token、真实 NAS 路径、bucket、object key、storage_uri。

## 2. 改动文件

- `backend/delivery-app/src/main/resources/db/migration/V31__8b_gd2_glandar_lightweight_jobs.sql`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarEngineSettings.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarStationClient.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/LightweightJobRepository.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/controller/VisualizationAdapterController.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/dto/VisualizationDtos.java`
- `scripts/dev/check-8b-gd2-glandar-rvt-poc.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`，未触碰主线 M 系列工作区，未新增 Hermes、语义索引、parser、documents/chunks 或真实 NAS 破坏性操作。

## 3. 后端实现

新增 `visualization_lightweight_jobs` 表，用于记录葛兰岱尔轻量化任务：

- 项目、模型文件、assetUuid、engineProvider、engineType。
- lightweightName、uniqueCode。
- 任务状态、进度、Viewer 地址、失败原因。
- Station 返回摘要 JSON。

新增 `GlandarStationClient`：

- 读取安全环境中的 Station API / Web base 和 token。
- 按文档生成 `input` JSON，`engineType=2`。
- 大文件通过 multipart form-data 调用 `SplitUploadFile`。
- 64MB 以内小文件通过 Station 现场可用的 multipart `upload-file` 提交，避免 `SplitUploadFile` 分片合并不入库问题。
- 查询 `query-model-info`，映射为平台状态：`RUNNING / READY / FAILED`。
- 不把 token 写入日志、响应、脚本或报告。

扩展平台接口：

```text
POST /api/visualization-adapter/projects/{projectId}/files/{fileId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

说明：

- 旧 model-integration 入口继续保留。
- 新 fileId 入口用于直接对文件管理器中的 RVT 样本发起 PoC。
- 重复点击同一个文件会复用已有可复用任务，不重复污染 active 任务。
- 当前 PoC 只开放 RVT，其他格式返回业务化不支持。

## 4. 当前真实对接状态

平台代码已能启动到 `GLANDAR` 模式，并能访问 Station 主机：

- `http://192.168.1.37:18086/` 可达。
- `http://192.168.1.37:18087/config.json` 可达。
- 后端已从安全环境读取到 token（未输出 token）。
- 平台接口可创建本地 lightweight job，并按失败原因记录。

补充复测（2026-05-28 12:25 CST）：用户替换新 token 后，Station 已不再返回 401，平台成功创建任务并持续查询到引擎运行态：

```text
任务 ID：4
状态：RUNNING
进度：30%
等待时间：900s
结果：超时仍未完成，未返回 status=100 或 modelAccessAddress
```

随后 Station 侧排查确认，该任务没有真正进入后台轻量化队列，根因表现为 `SplitUploadFile` 第 0 片文件名与后续分片不一致，导致合并/入库失败。

平台侧已改为：

- 继续保留大文件分片接口。
- 对 105 小样本 RVT 使用 Station 实测可用的 multipart `upload-file`。
- 强制重新提交时会将旧任务标记为 `SUPERSEDED`，避免复用旧失败任务。

最终真实转换结果：

```text
任务 ID：7
状态：READY
进度：100%
Viewer ticket：已受控签发
```

## 5. 验证结果

已通过：

```text
后端构建 PASS
健康检查 PASS（18088）
git diff --check PASS
Station API / Web 网络可达 PASS
禁出字段扫描 PASS
Phase2 batch4 文件访问安全回归 PASS=18 FAIL=0
```

GD2 专项脚本：

```text
scripts/dev/check-8b-gd2-glandar-rvt-poc.sh
```

当前执行结果：

```text
登录 PASS
切换 503 项目 PASS
创建/复用葛兰岱尔任务 PASS
响应禁出字段扫描 PASS
真实提交 PASS：Station 已接受任务
真实转换 PASS：READY 100%
Viewer ticket PASS：受控入口已签发
PASS=11 FAIL=0
```

说明：

- 8A Mock 回归在当前 `GLANDAR` 真实模式服务上不作为同进程验收项；此前 8B-GD1 已在默认 Mock 模式通过 8A 回归。
- 当前 8B-GD2 重点验收真实 Station PoC 与文件访问安全边界。

## 6. 风险与下一步

当前可以宣称：

- 平台侧葛兰岱尔真实对接代码已接入。
- 模型取用仍走平台受控读取链路。
- Station 主机网络可达。
- 105 项目 RVT 小样本已提交并转换完成。
- 平台已签发受控 Viewer 入口。

仍需注意：

- 本批只完成后端真实 PoC 与 Viewer ticket，不等于前端正式 Viewer 页面已完成。
- 大文件仍保留 `SplitUploadFile` 分片路径，但 Station 当前分片合并行为需引擎团队继续确认。
- 下一批建议进入 `8C-GD：Viewer 嵌入与业务联动`，把受控 viewer ticket 接入前端模型预览页。

下一步建议：

1. 安排测试 agent 对 `8B-GD2` 做专项验收。
2. 验收通过后提交 `codex/8b-gd2-rvt-poc`。
3. 后续从该分支或最新主线进入 `8C-GD`，开发前端 Viewer 嵌入。

---

# 开发报告：8B-GD1 葛兰岱尔轻量化引擎适配骨架

时间：2026-05-27 CST

## 1. 本轮目标

本轮执行 `8B-GD1：平台侧葛兰岱尔适配骨架`。

目标是在不影响 M 系列对象存储主线的前提下，为后续葛兰岱尔轻量化引擎 PoC 建立平台侧接口骨架：

- 保持默认 `MOCK` 模式，不改变现有 8A Mock 轻量化能力。
- 增加 `GLANDAR` provider 配置读取边界。
- 增加 lightweight job / viewer ticket 平台接口。
- 不调用葛兰岱尔 Station 上传接口。
- 不读取模型正文，不上传 RVT，不打开真实 Viewer。

## 2. 开发方式说明

本批原计划通过 CMUX Claude Code 开发。已尝试启动：

```bash
/Applications/cmux.app/Contents/Resources/bin/claude --permission-mode bypassPermissions --name 8B-GD1-GLANDAR-ADAPTER
```

Claude/CMUX 在读取上下文后因模型 API 报错中断：

```text
API Error: 400 The content[].thinking in the thinking mode must be passed back to the API.
```

已停止该 Claude 进程，随后由主 agent 按用户授权兜底实现。本轮仍保持独立 8B-GD worktree，不触碰主 M 系列工作区。

## 3. 改动文件

- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarEngineSettings.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/controller/VisualizationAdapterController.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/dto/VisualizationDtos.java`
- `scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh`
- `handoff/main-agent/8b-gd-task-graph.md`
- `handoff/dev-agent/latest-report.md`

未修改：

- `backend/**` 之外的业务后端模块
- `docs/**`
- 数据库迁移
- M3 对象存储模型
- Hermes / parser / indexing 相关代码

## 4. 后端实现

新增 `GlandarEngineSettings`：

- 读取 `BIM_ENGINE_PROVIDER` / `delivery.bim.engine.provider`。
- 读取 `GLANDAR_STATION_API_BASE`、`GLANDAR_STATION_WEB_BASE` 和安全凭据是否注入。
- 默认 provider 为 `MOCK`。
- `GLANDAR` 配置缺失时只返回业务化不可用原因，不抛 500。
- 不把凭据值返回给 API、前端或日志。

扩展现有轻量化状态：

- `lightweight-status` 默认返回 `engineMode=MOCK`。
- `lightweight-plan` 保持 `dryRun=true`，不创建任务。
- 原有 8A Mock 语义保持兼容。

新增平台侧接口：

```text
POST /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

接口行为：

- 校验当前项目上下文。
- 返回平台业务状态。
- MOCK 下不创建真实转换任务。
- GLANDAR 配置就绪时只返回“可进入 8B-GD2”的骨架状态，不真实上传模型。
- viewer ticket 在本批不签发真实入口。
- 响应不包含 Station API URL、Station Web URL、凭据、真实 NAS 路径、bucket、object key 或 SQL。

## 5. 禁止事项执行情况

本轮未执行：

- 葛兰岱尔 `SplitUploadFile` 分片上传。
- 葛兰岱尔 `query-model-info` 任务查询。
- RVT / IFC / DWG 模型正文读取。
- NAS / MinIO 底层路径直连。
- 轻量化产物写入。
- Viewer 真实跳转。
- Hermes memory / documents / chunks / Qdrant / OpenSearch 写入。

## 6. 验证结果

```text
后端构建                                           PASS
8B-GD1 专项脚本                                    PASS=16 FAIL=0
8A Mock 轻量化回归                                  PASS=11 FAIL=0
Phase2 batch4 文件访问安全回归                       PASS=18 FAIL=0
前端构建                                           PASS（仅既有 chunk size warning）
后端健康检查                                       PASS {"status":"UP"}
git diff --check                                   PASS
```

执行命令：

```bash
cd /Users/vc/Documents/数字化交付平台-8b-gd/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台-8b-gd
SERVER_PORT=18088 java -jar backend/delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-phase2-batch4-file-access.sh
corepack pnpm --dir frontend install --frozen-lockfile
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:18088/actuator/health
git diff --check
```

说明：

- 为避免干扰 M 系列主线，本轮临时后端运行在 `18088`。
- `frontend/node_modules` 在独立 worktree 初始不存在，已按锁文件安装依赖后完成前端构建。

## 7. 当前状态

`8B-GD1` 已完成开发自测，建议进入测试 agent 复核。

下一批建议：

- `8B-GD2：105 RVT PoC 转换闭环`
- 前置条件：测试 agent 收口 8B-GD1，并由引擎团队确认可用 Station Token、RVT 样本限制、任务查询状态码和 Viewer URL 拼接方式。
