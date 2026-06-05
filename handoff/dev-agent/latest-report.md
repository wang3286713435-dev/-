# 8C-GD-F4-F3 开发报告：轻量化引擎接入方式复核与爆炸参数对齐

## 1. 本轮目标

- 继续围绕用户目标复核：当前平台是否按葛兰岱尔文档和两个 demo 的方式接入轻量化 Viewer。
- 在已修复构件拾取的基础上，补齐模型爆炸参数与 demo 的一致性。

## 2. 接入方式复核

已对照：

- `/Users/vc/Desktop/Demo0604.zip`
- `/Users/vc/Desktop/rvt-preview-platform-embed-fix-20260604-164257.zip`
- 葛兰岱尔 BIM 模型服务文档中的构件属性接口说明。

当前平台接入口径如下：

- Viewer 初始化：已使用 `GlendaleEngine(...)`，并对齐 demo 的客户端渲染参数：
  - `logarithmicDepthBuffer: true`
  - `renderMode: 1`
  - `mappingMode: 3`
  - `sitePath` 指向平台本地 `/glandar-engine/`
  - `secretKey` 只从后端受控 ticket 注入，不在前端配置或日志明文输出。
- 模型加载：通过 `Model.add({ url, tag, flyTo, flyto })` 加载 Station 输出的 `root.glt`，并兼容 `flyTo / flyto`。
- 构件拾取：已按 demo 的成功链路接入：
  - `Public.event({ event: 'LEFT_CLICK' })`
  - `Feature.getByEvent({ position, callback })`
  - DOM 短点击兜底
  - canvas 矩形坐标换算
  - callback / Promise / direct object 三类返回兼容。
- 构件高亮：拾取后优先调用 `Feature.setColor`，否则降级到 `Feature.highlight`。
- 构件属性：平台后端代理调用葛兰岱尔 `property-data-by-externalid`，前端不直连 Station，也不暴露 token。
- 模型爆炸：平台使用 `Model.blow`，并保留多 tag payload 兼容；本轮补齐 Demo0604 使用的 `showAxis: true`。

结论：当前 Viewer 接入已经从“只显示模型”升级为“按 demo 与文档主链路接入构件拾取、构件属性代理和模型爆炸”。剩余后续增强不是接入错误，而是产品层增强，例如更精细的爆炸模式 UI、多模型联动爆炸和构件属性完整字段展示。

## 3. 本轮修复内容

- `GlandarViewerCanvas.vue`
  - `buildExplosionPayloads(...)` 增加 `showAxis: true`，对齐 Demo0604 中 `api.Model.blow({ tag, type, showAxis: true, value })` 的调用方式。
- `check-8c-gd-f4-component-pick-blow-properties.sh`
  - 增加模型爆炸参数 `showAxis: true` 静态断言。

## 4. 验证结果

```text
前端构建 PASS（仅保留既有 Vite chunk warning）
8C-GD-F4 专项 PASS=22 FAIL=0
浏览器构件拾取实测 PASS
浏览器模型爆炸入口实测 PASS
```

浏览器实测：

- 地址：`http://127.0.0.1:5173/bim-collaboration?projectId=503&preview=glandar`
- 点击模型实体后，已返回构件 `glandar-29^5156379` / Revit ID `5156379`。
- 点击“爆炸”后，Viewer 打开葛兰岱尔模型爆炸控制面板，可选择爆炸类型与幅度；本轮已补齐 demo 使用的 `showAxis` 参数。

## 5. 边界确认

- 未修改后端业务逻辑。
- 未新增数据库迁移。
- 未修改 `docs/**`。
- 未触碰真实 NAS / MinIO 文件。
- 未读取 BIM 模型正文或写入语义索引。
- 未新增 Hermes、parser、documents/chunks、Qdrant、OpenSearch 能力。

# 8C-GD-F4-F2 开发报告：复用 demo Worker 资源修复真实构件拾取

## 1. 本轮目标

- 用户反馈：点击模型实体仍显示“未拾取到构件，请点击模型实体”。
- 本轮继续参考 `/Users/vc/Desktop/rvt-preview-platform-embed-fix-20260604-164257.zip` 和 `/Users/vc/Desktop/Demo0604.zip`，重点修复平台 Viewer 运行时构件拾取失败。
- 不扩展新 BIM 能力，不改 Hermes、对象存储、工程树或 NAS 写入。

## 2. 根因判断

- 平台模型能显示，但浏览器控制台反复出现 `batch texture worker unavailable`。
- 这说明葛兰岱尔引擎主体脚本已加载，但构件拾取相关 worker 没有正确加载，导致点击模型后无法返回构件。
- demo 中本地 `public/worker` 和 `public/third` 下包含可用的 `gleBatchTextureWorker.js`、`PickWorker.js`、`RaycastWorker.js` 等资源；平台此前没有完整提供这些本地静态资源。

## 3. 修复内容

- `frontend/public/glandar-engine/**`
  - 从 demo 补齐葛兰岱尔 Viewer 所需本地静态资源：
    - `worker/PickWorker.js`
    - `worker/RaycastWorker.js`
    - `worker/gleBatchTextureWorker.js`
    - `worker/gleEdgesWorker.js`
    - `third/worker/PickWorker.js`
    - `third/worker/RaycastWorker.js`
    - `third/worker/gleBatchTextureWorker.js`
    - `third/worker/gleEdgesWorker.js`
    - `third/draco_decoder.wasm`
    - `third/draco_wasm_wrapper.js`
    - `third/basis_transcoder.wasm`
    - `third/basis_transcoder.js`
- `GlandarViewerCanvas.vue`
  - 嵌入式 Viewer 的 `sitePath` 改为平台本地 `/glandar-engine/`，不再依赖 Station 缺失的静态资源目录。
  - Viewer 初始化参数对齐 demo：`logarithmicDepthBuffer: true`、`renderMode: 1`、`mappingMode: 3`、`flyTo: true`。
  - 增加 `installGlandarWorkerUrlBridge()`：当引擎内部尝试加载已知 worker 时，自动改到平台本地同名 worker，不影响其它 Worker。
  - 构件拾取结果继续兼容 `id / featureId / externalId / objectId / componentId` 等返回字段。
- `check-8c-gd-f4-component-pick-blow-properties.sh`
  - 补充 worker 静态资源、worker 地址桥接、本地引擎资源目录等断言。

## 4. 验证结果

```text
前端构建 PASS（仅保留既有 Vite chunk warning）
8C-GD-F4 专项 PASS=21 FAIL=0
浏览器实测 PASS
```

浏览器实测地址：

```text
http://127.0.0.1:5173/bim-collaboration?projectId=503&preview=glandar
```

实测结果：

- 页面显示 `GLANDAR / 真实 Viewer 可用 / READY`。
- 点击模型实体后，页面出现“选中构件”面板。
- 已返回构件 ID：`glandar-29^5156379`。
- 已返回 Revit ID：`5156379`。
- 页面不再显示“未拾取到构件，请点击模型实体”。
- 旧的 `batch texture worker unavailable` 没有继续出现。

## 5. 边界确认

- 未修改后端业务逻辑。
- 未新增数据库迁移。
- 未修改 `docs/**`。
- 未触碰真实 NAS / MinIO 文件。
- 未读取 BIM 模型正文或构件数据入库。
- 未新增 Hermes、parser、indexing、documents/chunks、Qdrant、OpenSearch 能力。

# 8C-GD-F4-F1 开发报告：构件拾取兜底修复

## 1. 本轮目标

- 当前返工：`8C-GD-F4-F1：复用 demo 构件拾取逻辑`。
- 用户问题：点击模型实体后仍提示“未拾取到构件，请点击模型实体”。
- 本轮重点只修构件拾取，不扩展新 BIM 能力，不改对象存储、工程树、Hermes 或 NAS 写入。

## 2. 根因判断

- 已读取 `/Users/vc/Desktop/rvt-preview-platform-embed-fix-20260604-164257.zip` 中的 `public/app.js`、`public/viewerTools.js` 和 `test/viewerTools.test.js`。
- demo 的可用拾取链路有三个关键点：
  - 使用 `canvas.getBoundingClientRect()` 作为坐标换算基准。
  - `Feature.getByEvent` 同时兼容 callback、Promise 和直接返回对象。
  - 即使注册了葛兰岱尔 `LEFT_CLICK` 事件，DOM 层短点击仍会兜底尝试拾取。
- 平台上一版只在“引擎 LEFT_CLICK 没注册”时才走 DOM 兜底，所以当引擎事件坐标不兼容时，用户点击实体仍会得到“未拾取”。

## 3. 修复内容

- `GlandarViewerCanvas.vue`
  - 点击判断现在不再因为 `engineLeftClickPickRegistered=true` 而跳过 DOM 兜底。
  - 引擎左键事件先尝试；如果 180ms 内没有选中构件，再用 DOM 短点击坐标继续兜底。
  - 坐标换算优先使用真实 canvas 矩形，而不是外层 Viewer 容器矩形。
  - `getByEvent` 增加直接返回 feature object 的兼容处理。
  - 引擎事件失败时不提前弹“未拾取”，避免兜底成功前先出现误导提示。
- `check-8c-gd-f4-component-pick-blow-properties.sh`
  - 增加对 `waitForEnginePickResult` 和 canvas 坐标基准的静态断言。

## 4. 验证结果

```text
后端构建 PASS
前端构建 PASS（仅保留既有 Vite chunk warning）
8C-GD-F4 专项 PASS=14 FAIL=0
8C-GD 主链路回归 PASS=9 FAIL=0
Phase2 batch4 文件访问安全回归 PASS=18 FAIL=0
git diff --check PASS
git diff --cached --check PASS
```

## 5. 边界确认

- 未修改后端业务逻辑。
- 未新增数据库迁移。
- 未修改 `docs/**`。
- 未触碰真实 NAS / MinIO 文件。
- 未新增 Hermes、parser、indexing、documents/chunks、Qdrant、OpenSearch 能力。

## 6. 浏览器说明

- 已打开 `http://127.0.0.1:5173/bim-collaboration?projectId=503&preview=glandar`，页面显示 `GLANDAR / 真实 Viewer 可用 / READY`。
- 内置浏览器无法直接进入嵌入的 viewer iframe 地址做实体点选，因此最终仍建议由用户或测试 agent 在 5173 页面手动点击一次模型实体，确认不再出现“未拾取到构件”误提示。

# 8C-GD-F4 开发报告：构件拾取、模型爆炸与构件属性接口修复

## 1. 本轮目标

- 当前批次：`8C-GD-F4：葛兰岱尔 Viewer 构件交互兼容修复`。
- 用户问题：当前主线里的葛兰岱尔轻量化 Viewer 仍存在构件拾取不稳定、模型爆炸不可用、构件属性接口未接入平台的问题。
- 本轮参考：
  - `/Users/vc/Desktop/Demo0604.zip`
  - `/Users/vc/Desktop/rvt-preview-platform-embed-fix-20260604-164257.zip`
  - 葛兰岱尔 BIM 模型服务文档 4.2 构件属性接口。

## 2. 改动文件

- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarEngineSettings.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarStationClient.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/controller/VisualizationAdapterController.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/dto/VisualizationDtos.java`
- `frontend/src/modules/visualization/api/visualization.ts`
- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `scripts/dev/check-8c-gd-f4-component-pick-blow-properties.sh`
- `handoff/dev-agent/latest-report.md`

## 3. 修复内容

- 构件拾取：
  - 参考 demo 的做法，前端会同时尝试浏览器坐标、Viewer 容器相对坐标、Canvas 渲染坐标，避免坐标系不一致导致拾取不到构件。
  - `Feature.getByEvent` 同时兼容 callback 和 Promise 两种返回方式。
- 模型爆炸：
  - 参考 demo 的兼容策略，不再只用单一 `tag` 参数。
  - 会依次尝试 `tag`、`modelTag`、`modelTags`、`tags` 和无 tag payload，提升不同葛兰岱尔前端组件版本的兼容性。
- 构件属性：
  - 新增平台代理接口：`GET /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}/features/{featureId}/properties`。
  - 后端代理调用葛兰岱尔 `property-data-by-externalid`，前端不直连 Station。
  - 响应只返回脱敏后的属性分组、属性名和值，不返回 token、底层路径、raw station row。
  - 新提交转换任务已将 `dbPropertyType` 改为 `1`，让后续转换任务生成属性库。
- 配置兜底：
  - `GlandarEngineSettings` 现在优先读取正式 Spring 配置和环境变量，同时支持本机安全文件 `~/.zhuoyu-delivery/glandar.env` 作为开发兜底。
  - 该文件只在本机读取，不提交 Git，不在接口或报告里输出密钥。

## 4. 验证结果

```text
后端构建 PASS
前端构建 PASS（仅保留既有 Vite chunk warning）
后端健康检查 PASS {"status":"UP"}
8C-GD-F4 专项 PASS=14 FAIL=0
8C-GD 主链路回归 PASS=9 FAIL=0
Phase2 batch4 文件访问安全回归 PASS=18 FAIL=0
git diff --check PASS
git diff --cached --check PASS
```

## 5. 实测说明

- `8C-GD-F4` 专项已确认：
  - 前端存在多坐标拾取兼容逻辑。
  - 前端存在多 tag 模型爆炸兼容逻辑。
  - 后端已暴露受控构件属性接口。
  - READY Viewer ticket 已明确返回 `featurePickingAvailable / modelExplosionAvailable / componentPropertyAvailable`。
  - 构件属性代理接口可返回 OK，且未泄露底层路径或密钥。
- `8C-GD` 主链路回归中，非试点 RVT 可提交轻量化任务，当前返回 `RUNNING`。

## 6. 边界确认

- 未修改 `docs/**`。
- 未新增数据库迁移。
- 未触碰真实 NAS 文件。
- 未修改对象存储迁移逻辑。
- 未新增 Hermes、parser、indexing、documents/chunks、Qdrant、OpenSearch 能力。
- 未在接口、报告或脚本中输出葛兰岱尔 token。

## 7. 已知说明

- 新提交转换任务会生成属性库；历史 READY 模型如果当时未生成属性库，可能可以拾取和爆炸，但属性项为空或需要重转。
- 构件属性依赖葛兰岱尔返回的 `externalId`，当前平台只做平台代理和脱敏，不伪造属性。
- 后端和前端当前保持运行：
  - 后端：`http://127.0.0.1:8080`
  - 前端：`http://127.0.0.1:5173`

# UX4-F3 开发报告：顶部栏返回资产总览按钮

## 1. 本轮目标

- 当前批次：`UX4-F3：项目页顶部栏返回按钮定位`。
- 用户要求：在顶部栏原 `当前项目工作台` 位置放置返回按钮，替换原标签；项目标题和用户信息保留。
- 本轮只调整前端顶部栏交互，不改业务接口、不改后端、不改变项目工作台内容。

## 2. 改动文件

- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/styles/index.css`
- `handoff/dev-agent/latest-report.md`

## 3. 修复内容

- 项目路由内顶部栏左侧显示 `返回资产总览` 按钮。
- 点击按钮跳转到资产总览页 `data-steward-assets`。
- 非项目页继续显示原来的入口标签，例如 `平台主入口`、`BIM协同管理`、`管理中心`。
- 按钮样式复用原顶部标签的轻量蓝色胶囊视觉，保持顶部栏高度稳定。

## 4. 验证结果

- 前端构建：`corepack pnpm --dir frontend build` 通过，仅保留既有 Vite chunk size warning。
- 后端健康检查：`/actuator/health` 返回 `UP`。
- 格式检查：`git diff --check` 通过。

## 5. 边界确认

- 未修改 `backend/**`。
- 未修改数据库迁移。
- 未修改 `docs/**`。
- 未触碰真实 NAS / MinIO 文件。
- 未新增 Hermes、BIM、parser、documents/chunks、Qdrant、OpenSearch 能力。

# UX4-F2 开发报告：项目详情页重复工作台导航移除

## 1. 本轮目标

- 当前批次：`UX4-F2：项目详情页重复导航减负`。
- 用户问题：`/data-steward/assets/503` 顶部 `项目工作台导航` 与下方项目工作台模块内容重复，首屏信息密度过高。
- 本轮只删除用户标注的顶部重复模块，保留下方项目工作台模块；返回按钮暂不处理，等待用户后续指定位置。

## 2. 改动文件

- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `handoff/dev-agent/latest-report.md`

## 3. 修复内容

- 移除项目详情页顶部的 `ProjectWorkspaceNav` 渲染。
- 删除该页面中不再使用的 `ProjectWorkspaceNav` 导入。
- 未修改全局布局、侧边栏、旧路由兼容、项目工作台下方核心入口。

## 4. 验证结果

- 前端构建：`corepack pnpm --dir frontend build` 通过，仅保留既有 Vite chunk size warning。
- 后端健康检查：`/actuator/health` 返回 `UP`。
- 格式检查：`git diff --check` 通过。

## 5. 边界确认

- 未修改 `backend/**`。
- 未修改数据库迁移。
- 未修改 `docs/**`。
- 未触碰真实 NAS / MinIO 文件。
- 未新增 Hermes、BIM、parser、documents/chunks、Qdrant、OpenSearch 能力。

# UX4-F1 开发报告：工程树可视化项目文件统计修复

## 1. 本轮目标

- 当前批次：`UX4-F1：工程树可视化统计与项目口径修复`。
- 用户问题：`/data-steward/assets/505` 的工程树可视化页中，`项目文件` KPI 显示为 `0`，但该项目实际有文件，页面造成“项目没有文件”的误导。
- 本轮只修前端展示和兜底逻辑，不改后端、不改接口、不改数据库、不触碰 NAS / MinIO。

## 2. 根因分析

- 工程树可视化面板只依赖 `file-ownership/coverage` 的显示值，并且页面文案写死为 `105 试点资产`。
- 当项目还没有建立文件归属或页面加载状态短暂未同步时，员工看到的是一个孤立的 `0`，无法区分“项目没文件”和“项目还没建立工程树归属”。
- 实测 `projectId=505 / C塔` 后端数据正常：
  - `file-ownership/coverage.totalFiles = 5457`
  - `assets/statistics.fileCount = 5457`
  - `assignedFiles = 0`
- 因此本轮按前端 bug 修复：工程树面板补取项目资产统计作为兜底，并移除 105 写死文案。

## 3. 改动文件

- `frontend/src/modules/data-steward/components/FileOwnershipTreePanel.vue`
- `handoff/dev-agent/latest-report.md`

## 4. 修复内容

- 工程树 KPI 新增资产统计兜底：
  - 优先展示 `coverage.totalFiles`。
  - 如果归属统计总数为 0，则使用 `assets/statistics.fileCount`。
- `项目文件` 不再写死为 `105 试点资产`，改成当前项目业务口径：
  - 无登记文件：`当前项目暂无登记文件`
  - 有文件但未归属：`当前项目待建立工程树归属`
  - 已有归属：`当前项目资产`
- `已有归属` 覆盖率改为前端按展示总数重新计算，避免归属统计为 0 时页面解释不一致。
- `确认全部未归属文件` 按真实待归属数量启用/禁用，不再只看原始 coverage 值。

## 5. 验证结果

- 前端构建：`corepack pnpm --dir frontend build` 通过，仅保留既有 Vite chunk size warning。
- 后端健康检查：`/actuator/health` 返回 `UP`。
- 格式检查：`git diff --check` 通过。
- API 抽查：
  - `GET /api/data-steward/projects/505/file-ownership/coverage` 返回 `totalFiles=5457 / assignedFiles=0 / unassignedFiles=5457`。
  - `GET /api/data-steward/assets/statistics?projectId=505` 返回 `fileCount=5457`。

## 6. 边界确认

- 未修改 `backend/**`。
- 未新增数据库迁移。
- 未修改 `docs/**`。
- 未触碰真实 NAS 文件。
- 未执行对象化迁移。
- 未新增 Hermes、BIM、parser、documents/chunks、Qdrant、OpenSearch 能力。

## 7. 后续建议

- 如果测试 agent 仍在浏览器看到 `0`，优先刷新当前前端页面或确认 5173 是否来自当前项目目录。
- 后续 UX4 可继续把工程树页面做成更清楚的“待建立归属 / 已归属 / 待复核”业务视图，而不是只堆技术指标。

# 8C-GD-F3 开发报告：葛兰岱尔 Viewer 可用性口径修复

## 1. 本轮目标

- 当前批次：`8C-GD-F3：BIM 协同 / 葛兰岱尔 Viewer 兼容修复`。
- 用户问题：`/bim-collaboration?projectId=503&preview=glandar` 页面仍可能显示“Viewer 暂不可用”或“真实 Viewer 未接入”，即使 Station 已经有 READY 模型。
- 本轮只修葛兰岱尔 Viewer 状态口径和票据契约，不改对象存储、Hermes、NAS 写入、语义索引或工程主数据。

## 2. 根因分析

- 后端看板摘要只读取旧的模型集成元数据，没有统计 `visualization_lightweight_jobs` 中已经 READY 且可打开 Viewer 的葛兰岱尔轻量化任务。
- READY 任务签发 Viewer ticket 时，可能只返回 Station API 侧的 `modelAccessAddress`，但没有返回前端 Viewer 组件必须使用的 `engineStaticBase`。
- 在当前本机启动配置未显式注入 `GLANDAR_STATION_WEB_BASE` 时，后端没有从 Station 返回地址推导 Web 静态资源地址，导致前端合理地判断 Viewer 不可用。

## 3. 改动文件

- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/LightweightJobRepository.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `scripts/dev/check-8c-gd-mainline-full-glandar-render.sh`
- `handoff/dev-agent/latest-report.md`

## 4. 修复内容

- 新增 READY Viewer 任务计数：统计项目下 `READY + viewer_available + model_access_address` 的葛兰岱尔轻量化任务。
- BIM 协同综合驾驶舱现在会根据 READY Viewer 任务切换为真实 Viewer 口径：
  - `engineMode=GLANDAR`
  - `engineConnected=true`
  - `lightweightStatus=READY`
  - `viewerAvailable=true`
  - `statusLabel=真实 Viewer 可用`
- Viewer ticket 现在必须同时具备：
  - READY 状态
  - `viewerAvailable=true`
  - `modelAccessAddress`
  - `engineStaticBase`
- 若未配置 `GLANDAR_STATION_WEB_BASE`，后端会基于 Station API 地址安全推导 Web 静态资源地址，例如从 `:18086` 推导到 `:18087/static/ThreeJsEngine`。
- 脚本补充断言：READY ticket 必须返回 `engineStaticBase`，看板摘要不能继续停留在“真实 Viewer 未接入”。

## 5. 验证结果

- 后端构建：`./mvnw -pl delivery-app -am -DskipTests package` 通过。
- 后端健康检查：`/actuator/health` 返回 `UP`。
- 前端构建：`corepack pnpm --dir frontend build` 通过，仅保留既有 Vite chunk size warning。
- 葛兰岱尔专项：`EXPECT_GLANDAR_READY=true bash scripts/dev/check-8c-gd-mainline-full-glandar-render.sh` 通过，`PASS=9 FAIL=0`。
- 浏览器短验：打开 `http://127.0.0.1:5173/bim-collaboration?projectId=503&preview=glandar` 后，不再出现“Viewer 暂不可用”或“真实 Viewer 未接入”；页面显示 `GLANDAR / 真实 Viewer 可用 / 13 已发布 / READY`。
- `git diff --check` 和 `git diff --cached --check` 通过。

## 6. 边界确认

- 未修改 `docs/**`。
- 未新增数据库迁移。
- 未触碰真实 NAS 文件。
- 未修改对象存储迁移逻辑。
- 未新增 Hermes、parser、indexing 或文件正文读取能力。
- 未在接口、报告或脚本中输出葛兰岱尔 token。

## 7. 已知说明

- 当前修复让平台能正确识别和展示 Station 已经 READY 的真实 Viewer 能力。
- 后续若 Station Web 端口或静态资源路径改变，建议正式配置 `GLANDAR_STATION_WEB_BASE`，不要长期依赖端口推导。
- 后端当前按本机葛兰岱尔启动脚本运行，服务保持在 `http://127.0.0.1:8080`；前端保持在 `http://127.0.0.1:5173`。

# M3G-9 开发报告：全项目对象化覆盖率报告与 M3 收口依据

## 1. 本轮目标

- 当前批次：`M3G-9：全项目对象化覆盖率报告与 M3 收口`。
- 目标：基于真实 MySQL 台账和当前对象版本状态，生成全项目对象化覆盖率只读报告，说明 105 样板项目、非 105 项目状态分布和 M3 收口依据。
- 本轮不执行历史文件迁移，不读取文件正文，不触碰真实 NAS 原文件。

## 2. 改动文件

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `scripts/dev/check-m3g9-objectification-coverage-report.sh`
- `handoff/main-agent/m3-m5-storage-evidence-task-graph.md`
- `handoff/dev-agent/latest-report.md`

## 3. 新增 / 复用接口

- 新增只读接口：`GET /api/data-steward/storage-objectification-coverage`
- 返回统一 `ApiResponse`，包含 `traceId`。
- 返回内容包括：
  - `dryRun=true`
  - `reportCode=M3G-9`
  - `summary`
  - `closureAssessment`
  - `projects`
- 项目行包含：`projectId`、`projectCode`、`projectName`、项目分类、接入状态、文件数、对象化数、NAS_ONLY 数、失败/治理数、checksum 覆盖率、对象化覆盖率、最后对象化时间、读取策略、状态、失败原因分组、下一步建议。

## 4. 全项目覆盖率摘要

- totalProjects：`97`
- completedProjects：`1`
- partialProjects：`13`
- nasOnlyProjects：`2`
- failedOrGovernanceProjects：`0`
- excludedProjects：`81`
- totalFiles：`41214`
- objectStoredFiles：`2994`
- nasOnlyFiles：`38220`
- failedFiles：`0`
- overallObjectificationRate：`7.26%`
- totalSizeBytes：`317849650373`
- objectStoredSizeBytes：`35506773731`
- checksumCoverageRate：`10.72%`

## 5. 105 覆盖率结果

- projectId：`503`
- projectCode：`105`
- projectName：`启航华居项目`
- totalFiles：`2928`
- objectStoredCount：`2928`
- nasOnlyCount：`0`
- migrationFailedCount：`0`
- governanceCount：`0`
- checksumCoverageRate：`100.00%`
- objectificationCoverageRate：`100.00%`
- readStrategySummary：`OBJECT_FIRST`
- status：`COMPLETED`

## 6. 非 105 项目状态分布

- `PARTIAL=13`：已进入对象化主链路，但仍有 NAS_ONLY 剩余文件。
- `NAS_ONLY=2`：状态可解释，但不代表已对象化完成。
- `EXCLUDED=81`：非真实 NAS、测试/样例、无登记文件或不纳入 M3 主线完成口径的项目。
- `FAILED_NEEDS_GOVERNANCE=0`：当前覆盖率报告未发现迁移失败或不可读引用治理项目。

## 7. M3 收口判断依据

- 接口返回 `m3ClosureReady=true`，含义是：对象存储主链路已可用，105 完整样板已完成，非 105 项目状态可解释，当前没有失败/治理阻塞项。
- `PARTIAL` 和 `NAS_ONLY` 项目仍作为后续批量对象化计划，不被伪装成已完成。
- 最终是否宣布 M3 收口仍由主 agent / 测试 agent 判断。

## 8. 前端展示

- 在文件服务 / 对象存储页面新增“全项目对象化覆盖率”区域。
- 页面展示：
  - M3G-9 总覆盖率
  - 完成 / 部分 / NAS_ONLY / 需治理 / 容量 / checksum 汇总
  - 项目级状态表
  - 低频诊断信息折叠展示
- 前端不展示真实 NAS 路径、bucket、object key、`storage_uri` 或底层 SQL。

## 9. 禁出字段扫描

- `scripts/dev/check-m3g9-objectification-coverage-report.sh` 已扫描响应。
- 未发现：
  - `/Volumes`
  - `smb://`
  - `nas://`
  - `storage_path`
  - `storage_uri`
  - bucket
  - object key
  - raw row
  - SQL
  - token / secret / password 真值

## 10. 构建和脚本结果

- 后端构建：`./mvnw -pl delivery-app -am -DskipTests package` 通过。
- 前端构建：`corepack pnpm --dir frontend build` 通过。
- 健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health` 返回 `UP`。
- M3G-9 专项：`bash scripts/dev/check-m3g9-objectification-coverage-report.sh` 通过，`PASS=11 FAIL=0`。
- M3G-8 回归：`bash scripts/dev/check-m3g8-object-first-read-fallback.sh` 通过，`PASS=7 FAIL=0`。
- M3G-6S-F1 回归：`bash scripts/dev/check-m3g6s-f1-object-version-minio-alignment.sh` 通过，`PASS=13 FAIL=0`。
- 文件访问安全回归：`bash scripts/dev/check-phase2-batch4-file-access.sh` 通过，`PASS=18 FAIL=0`。

## 11. 环境说明

- 初次跑 M3G-8 时，后端以默认本机 MinIO 配置启动，导致找不到 NAS 侧可读对象样本。
- 后端随后按项目已有 `tmp/local-env/nas-minio.env` 启动，readiness 变为 `NAS_SIDE_MINIO / READY`。
- M3G-6S-F1 初始完整性为 `verified=2928`、`governance=0`，未触发实际修复循环。

## 12. 是否触碰 docs / 迁移 / NAS

- 是否修改 `docs/**`：否。
- 是否新增数据库迁移：否。
- 是否执行历史文件迁移：否。
- 是否移动、删除、重命名、覆盖真实 NAS 原文件：否。
- 是否读取 PDF / Office / DWG / RVT / IFC 正文：否。
- 是否写 documents / chunks / Qdrant / OpenSearch / Hermes memory：否。

## 13. 已知风险和后续建议

- M3G-9 只做覆盖率报告，不会自动对象化 `PARTIAL` / `NAS_ONLY` 项目。
- `PARTIAL=13` 和 `NAS_ONLY=2` 应作为后续分批对象化计划输入。
- `EXCLUDED=81` 需要由主 agent 判断是否长期排除、补登记，或进入后续治理批次。
- M3G-6S-F1 完整性核验会逐项探测对象副本，耗时明显长于普通 smoke。
