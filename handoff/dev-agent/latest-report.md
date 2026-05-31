# 开发报告：8C-GD4-F3 构件级能力诚实兜底

时间：2026-06-01 CST

## 0. 结论

已按主 agent 要求完成平台侧诚实兜底：当前 10 个 RVT 试点模型仍保留葛兰岱尔整体模型预览、拖动、缩放、主视角、整模显示、距离/面积测量；但构件拾取、构件隐藏/定位、构件爆炸这类依赖构件索引的功能，在当前轻量化产物未声明 `componentIndexAvailable / featureIndexAvailable / featurePickingAvailable` 前不再假装可用。

当前页面会明确提示：

```text
当前仅支持整体模型预览
当前轻量化产物未包含构件索引，暂不支持构件拾取、构件隐藏/定位和模型爆炸。
```

这避免用户反复点击模型后只看到“未拾取到构件”，也避免爆炸按钮给出无效果操作。

## 1. 改动文件

- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.css`
- `handoff/dev-agent/latest-report.md`

## 2. 具体改动

- Viewer iframe 内部新增构件级能力判断：只有后端/Viewer ticket 明确返回 `componentIndexAvailable=true`、`featureIndexAvailable=true` 或 `featurePickingAvailable=true`，才开放构件拾取、爆炸、构件隐藏/定位、构件面积/体积测量。
- Viewer 加载后通过 `postMessage` 向 BIM 协同页回传能力状态，父页面据此禁用爆炸、构件隐藏、定位等按钮。
- BIM 协同页新增能力提示卡，说明当前模型只支持整体预览，构件级能力等待葛兰岱尔输出构件索引。
- 保留整体模型操作：主视角、整模显示/隐藏、距离测量、面积测量、清除。

## 3. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS
8C-GD1 十个 RVT 试点脚本 PASS=11 FAIL=0
phase2 batch4 file-access 安全脚本 PASS=18 FAIL=0
git diff --check PASS
5188 前端页面 HTTP 200
```

## 4. 当前边界

- 未改后端、数据库、转换任务、M3 对象存储、NAS/MinIO 或 Hermes。
- 未新增构件属性库、构件索引、语义索引或文件正文读取。
- 当前不能收口为“构件级 BIM Viewer 已完成”；只能收口为“葛兰岱尔整体模型预览 + 构件级能力待引擎产物支持”。
- 下一步需等待葛兰岱尔团队确认 RVT 转换如何生成构件索引、RevitId 映射和爆炸分组。

---

# 开发报告：8C-GD4-F2 Viewer 工具实测修复

时间：2026-05-31 20:19 CST

## 0. 最新结论

已针对用户实测反馈修复：`点击构件一直提示未拾取到构件`、`爆炸不可用`、`显示不可用/不稳定`。

本轮只改葛兰岱尔 Viewer 前端调用口径，不改后端、不改转换任务、不影响 M3 对象存储主线。

- 明确判断：本轮问题优先判定为平台 Viewer 对接层问题，而不是“引擎一定无法读取构件”。理由是：模型已经能渲染，说明几何数据可读；引擎文档和 demo 明确存在 `Feature.getByEvent`、`Model.blow`、`Model.setVisible` 等能力；平台此前没有默认注册引擎原生 `LEFT_CLICK` 拾取事件，且爆炸传参不是 demo 的精确口径。
- 构件拾取：改回葛兰岱尔 demo 的原始坐标口径，只传 `clientX/clientY`，不再叠加 iframe 偏移、多候选坐标或 page/screen 坐标，避免 SDK 拾取点偏移。
- 构件拾取：新增默认注册引擎原生 `Public.event({ event: 'LEFT_CLICK' })`，让引擎先返回自己的点击位置，再调用 `Feature.getByEvent`；手工 pointerup 拾取仅作为引擎事件不可用时的兜底，避免双重触发。
- 构件拾取等待：拾取回调超时从 `700ms` 收敛到 `320ms`，更贴近 demo 的快速失败逻辑，避免误点后长时间卡住。
- 模型爆炸：`Model.blow` 改为 demo 已验证参数：`{ tag, type, value }`，不再混传 `mode / blowType / amplitude / scale`。
- 爆炸模式切换：球面/线性切换时先把上一种爆炸复原到 `0`，再做新模式 720ms 平滑过渡。
- 构件显示/隐藏：`Feature.setVisible` 改为 demo 口径：`{ featureIds, visible, tag }`；隐藏前先清除高亮，重新显示后恢复选中高亮。
- 整模显示/隐藏仍走 `Model.setVisible({ tag, visible })`，并保持 Viewer 渲染刷新。

## 0.1 追加诊断结论：当前 10 个 RVT 轻量化产物疑似缺少构件索引

用户复测后仍出现“未拾取到构件”，并且模型爆炸无效果。已进一步检查 105 项目 10 个试点模型的葛兰岱尔输出文件：

```text
1257 root.glt 3869 bytes -> root.bin 10075 bytes，未发现 feature/property/batch/revit/json/index
1261 root.glt 3993 bytes -> root.bin 10820 bytes，未发现 feature/property/batch/revit/json/index
1264 root.glt 4064 bytes -> root.bin 11259 bytes，未发现 feature/property/batch/revit/json/index
3730 root.glt 4081 bytes -> root.bin 11216 bytes，未发现 feature/property/batch/revit/json/index
1258 root.glt 4030 bytes -> root.bin 11191 bytes，未发现 feature/property/batch/revit/json/index
1251 root.glt 4077 bytes -> root.bin 11199 bytes，未发现 feature/property/batch/revit/json/index
1259 root.glt 3865 bytes -> root.bin 10073 bytes，未发现 feature/property/batch/revit/json/index
1262 root.glt 3853 bytes -> root.bin 10080 bytes，未发现 feature/property/batch/revit/json/index
3729 root.glt 4086 bytes -> root.bin 11166 bytes，未发现 feature/property/batch/revit/json/index
1243 root.glt 4089 bytes -> root.bin 11150 bytes，未发现 feature/property/batch/revit/json/index
```

这说明当前转换产物能被 Viewer 渲染为模型外形，但从可见文件结构看，没有构件拾取、RevitId、batchId 或属性索引文件。若 SDK 的构件拾取和爆炸依赖这类构件分组/索引，则当前失败更可能是“转换产物未生成构件级数据”，不是平台按钮没接上。

当前更准确的边界判断：

- 平台侧已经按引擎文档和 demo 完成 `Feature.getByEvent`、`Model.blow`、`Model.setVisible`、`Feature.setVisible` 的调用口径修正。
- 10 个试点 `root.glt` 产物体积均只有约 3.8~4.1KB，内部只有一个 `root.bin`，不像完整 BIM 构件级轻量化产物。
- 因此，构件拾取、爆炸、构件级显隐不能作为当前批次收口条件，必须先请葛兰岱尔确认：当前转换配置是否启用了构件级导出、构件拾取索引、RevitId 映射和爆炸分组。

建议发给引擎团队确认的问题：

1. 当前 `root.glt` 只包含 `root.bin` 是否正常？
2. 构件拾取需要哪些输出文件或索引？
3. `Feature.getByEvent` 对当前 RVT 产物是否应该返回 feature？
4. `Model.blow` 是否要求模型按构件/楼层/族拆分，当前 10 个试点产物是否满足？
5. 如需启用构件索引/属性导出/爆炸分组，应调整 Station 哪些转换参数？

## 1. 改动文件

- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS
8C-GD1 十个 RVT 试点脚本 PASS=11 FAIL=0
phase2 batch4 file-access 安全脚本 PASS=18 FAIL=0
git diff --check PASS
```

## 3. 需要人工复核

自动化能确认接口、构建和安全边界；构件拾取、爆炸、显示/隐藏最终依赖葛兰岱尔 Viewer SDK 在浏览器里的真实交互。请在当前 Viewer 页面重新点一次模型实体，并分别试：

1. 单击模型构件是否出现构件信息浮窗。
2. `爆炸 -> 应用 / 复原` 是否有模型爆炸效果。
3. `显示` 是否能隐藏/恢复整个模型。
4. 选中构件后 `隐藏` 是否能隐藏/恢复该构件。

如果以上仍失败，则下一步不再继续盲改平台前端，而应请引擎团队确认当前 `root.glt` 是否包含可拾取的 feature index / RevitId / batchId 映射，以及 `Model.blow` 对当前 RVT 转换产物是否生效。平台侧已按文档和 demo 口径完成调用。

## 4. 当前边界

- 未改后端、数据库、M3 对象存储、NAS/MinIO、Hermes 或转换任务。
- 未新增模型转换、构件属性库、语义索引或正文读取能力。
- 5188 前端和 18088 后端服务保持运行，方便继续验收。

---

# 开发报告：8C-GD4-F1 Viewer 功能栏重制

时间：2026-05-31 CST

## 0. 最新结论

已按用户给定的按钮语义重制 BIM 协同页下方模型功能栏。本轮只改葛兰岱尔可视化分支前端，不影响 M3 对象存储主线，不改后端转换任务。

- 下方工具栏收敛为：`主视角 / 爆炸 / 显示 / 隐藏 / 定位 / 距离 / 面积 / 清除`。
- `主视角` 调用 Viewer 内 `Camera.transitionsView` + `Model.zoomTo` 回到主视角。
- `爆炸` 增加模型爆炸面板，支持 `球面爆炸 / 线性爆炸`，幅度 `0 ~ 1`，应用与复原都按 720ms 平滑过渡调用 `Model.blow`。
- `显示` 调用 `Model.setVisible({ tag, visible })`，在隐藏/显示整个模型之间切换。
- `隐藏` 改为当前选中构件的显示/隐藏切换，调用 `Feature.setVisible({ featureIds, visible, tag })`，不再只做一次性隐藏。
- `定位` 继续基于当前选中构件调用 `Feature.zoomTo({ featureIds, batchId, tag })`。
- `距离 / 面积` 分别调用 `Measurement.distance` 与 `Measurement.area`；`清除` 专注清理测量状态，先退出测量再清除测量痕迹。
- iframe 内嵌 Viewer 保持只渲染模型画布，工具由 BIM 协同页底部统一控制；点击模型背景未拾取到构件时仍会清除当前构件选择。

## 1. 改动文件

- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.css`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS
8C-GD1 十个 RVT 试点脚本 PASS=11 FAIL=0
phase2 batch4 file-access 安全脚本 PASS=18 FAIL=0
git diff --check PASS
```

## 3. 当前边界

- 本轮没有改后端、数据库、M3 对象存储、NAS/MinIO、Hermes 或转换任务。
- 本轮没有新增模型转换、没有读取模型正文、没有写构件属性库或语义索引。
- `Model.blow` 的实际爆炸参数仍由葛兰岱尔 SDK 解释；平台侧已按 `type/mode/blowType/value/amplitude/scale` 做兼容传参，并保持业务侧按钮语义稳定。
- 5188 前端和 18088 后端服务保持运行，方便继续验收。

---

# 开发报告：8C-GD4 BIM 协同接入工程主数据与文件归属主线

时间：2026-05-29 CST

## 0. 最新结论

已根据用户反馈把 BIM 协同页从“独立适配大屏”收回到平台真实业务主线。本轮重点不是继续加 Viewer 功能，而是让 BIM 协同直接消费当前已经录入和治理好的工程主数据、专业字段、文件归属和交付状态。

- BIM 协同页额外加载工程主数据状态、部位树、文件归属覆盖率和文件归属树。
- 新增 `工程树` 页签，展示工程部位、文件归属覆盖、交付标准状态和重点工程节点文件数量。
- `综合驾驶舱` 的项目概览从“模型/对象/文件”改为“工程部位 / 交付标准 / 文件归属”，让用户先看到项目结构是否可用于交付。
- `设备设施` 页签保留，但明确绑定管理对象台账、专业字段和工程部位绑定，不再把它当成独立演示模块。
- `房屋空间 / 专业系统 / 告警监控 / 工单巡检保养` 继续只展示平台真实数据或真实空状态，并给出对应维护入口。
- 葛兰岱尔 Viewer 仍只是模型预览窗口，不主导业务结构。

## 1. 改动文件

- `frontend/src/modules/visualization/pages/DigitalTwinDashboardPage.vue`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.css`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS
8C-GD1 十个 RVT 试点脚本 PASS=11 FAIL=0
phase2 batch4 file-access 安全脚本 PASS=18 FAIL=0
git diff --check PASS
```

## 3. 当前边界

- 本轮没有修改后端、数据库、M3 对象存储、NAS/MinIO、Hermes 或转换任务。
- 本轮没有新增模型范围，没有读取 RVT/DWG/PDF 正文，没有写语义索引。
- BIM 协同页现在只是把已有工程主数据、文件归属和轻量化模型进行可视化组合；正式文件归属确认、交付挂接、审核整改仍回到原平台业务页面执行。
- 5188 前端和 18088 后端服务保持运行，方便继续验收。

---

# 开发报告：8C-GD3 BIM 协同页真实数据页签与大屏信息完整性修复

时间：2026-05-29 CST

## 0. 最新结论

已按 8C-GD3 要求把 BIM 协同管理页页签收束为“平台当前真实数据驱动”。本轮仍只在葛兰岱尔可视化独立分支开发，不影响 M3 对象存储主线。

- `综合驾驶舱` 继续基于平台 `digital-twin-dashboard` 数据展示资产、模型、轻量化、交付缺失、质量风险和最近动态。
- `BIM 场景` 保持为当前选中轻量化模型和葛兰岱尔 Viewer 操作区，不混入其它说明内容。
- `设备设施` 绑定管理对象与运营摘要数据；无对象时显示真实空状态，并提供“去管理对象登记 / 去文件管理 / 去工程部位”入口。
- `房屋空间` 绑定工程部位和空间对象口径；无空间对象时提示需要维护工程部位或对象绑定。
- `告警监控` 改成平台真实治理告警：质量风险、扫描失败、缺元数据、待审核、整改待闭环，不伪装 IoT 告警。
- `轻量化模型列表` 展示已轻量化 RVT，支持打开预览和跳转模型文件管理。
- `专业系统` 绑定 SYSTEM 类型管理对象和专业分布；无数据时提示登记系统对象或补充对象专业。
- `工单巡检保养` 映射为平台整改、审核、扫描治理任务；不展示虚构巡检保养工单。
- 所有空状态都补充“缺什么数据、去哪里维护”的真实业务动作。

## 1. 改动文件

- `frontend/src/modules/visualization/bim-collab/types.ts`
- `frontend/src/modules/visualization/bim-collab/mapDashboardToBimCollab.ts`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.css`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS
8C-GD1 十个 RVT 试点脚本 PASS=11 FAIL=0
phase2 batch4 file-access 安全脚本 PASS=18 FAIL=0
git diff --check PASS
```

## 3. 当前边界

- 本轮没有新增后端写接口，没有修改 NAS / MinIO / Hermes / 对象存储主线。
- 本轮没有接入真实 IoT / CMMS 系统；`告警监控` 和 `工单巡检保养` 只映射平台已有质量风险、整改、审核和扫描治理任务。
- 页签动作只跳转到平台真实业务页面，不返回真实 NAS 路径、`storage_uri`、bucket、object key、token、SQL 或 raw row。
- 5188 前端和 18088 后端服务保持运行，方便继续验收。

---

# 开发报告：8C-GD2 葛兰岱尔 Viewer 操作增强与构件信息浮窗

时间：2026-05-29 CST

## 0. 最新结论

已根据葛兰岱尔 3D BIM/GIS 轻量化渲染引擎 API 文档补齐平台内 Viewer 操作层。本轮只改可视化独立分支，不改 M3 对象存储主线、不改后端转换任务。

- 嵌入式 Viewer 增加同源 `postMessage` 指令桥，BIM 协同页按钮可以直接控制 iframe 内模型。
- 接入构件拾取：左键短按模型构件后调用 `Feature.getByEvent`，选中后高亮并回传外层大屏。
- 新增构件信息浮窗：展示 `featureId / revitId / batchId / 模型文件 / 平台文件ID`，并标注当前不是完整 BIM 属性接口。
- 新增构件操作：定位、隐藏、恢复隐藏、构件表面积、构件体积。
- 新增 Viewer 工具：距离测量、面积测量、漫游启动/停止、剖切、截图、清除。
- 嵌入态背景改用文档支持的 `Color` 模式，继续按深色/浅色主题设置背景色，避免 SDK 回退灰色默认背景。
- BIM 协同页底部工具栏重排为真实操作按钮：选择构件、漫游、停止漫游、距离、面积、构件测量、剖切、隐藏、恢复隐藏、截图、清除。

## 1. 改动文件

- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `frontend/src/modules/visualization/pages/GlandarModelPreviewPage.vue`
- `frontend/src/modules/visualization/pages/DigitalTwinDashboardPage.vue`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.css`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS
8C-GD1 十个 RVT 试点脚本 PASS=11 FAIL=0
phase2 batch4 file-access 安全脚本 PASS=18 FAIL=0
git diff --check PASS
Chrome 短验 PASS：5188 BIM 协同页可打开，模型工具栏显示新操作，嵌入 iframe 使用 glandar-viewer-embed
```

## 3. 当前边界

- 构件浮窗当前只展示引擎拾取接口返回的基础信息；完整族、类型、材质、楼层等属性需要葛兰岱尔提供属性查询接口或后续构件索引。
- 本轮不新增模型转换范围，不读 RVT 正文，不做 Hermes 问答，不写对象存储或语义索引。
- 5188 前端服务和 18088 后端服务保持运行，方便继续验收。

---

# 开发报告：8C-GD1-F9 追加 Viewer 背景主题修复

时间：2026-05-29 CST

## 0. 最新结论

根据用户反馈“背景还是水泥色，轻量化仅渲染模型本体”，本轮继续修复嵌入式葛兰岱尔 Viewer 的背景呈现：

- 引擎初始化背景色改为跟随 BI 大屏主题：深色为蓝黑大屏底色，浅色为浅蓝数据中台底色。
- 嵌入态创建引擎时不再使用 HDR 环境背景，避免 SDK 自带环境色覆盖平台大屏背景。
- 加入运行时背景同步：Viewer 创建、模型加载和视角适配后，都会尝试覆盖葛兰岱尔 renderer / scene 的清屏色。
- 嵌入态不再额外把网格覆盖到模型 canvas 上，避免模型预览看起来像一块灰色水泥画布。
- 大屏容器继续负责网格、光晕和主题背景；葛兰岱尔 Viewer 只负责渲染模型本体和鼠标交互。
- 不改后端、不改转换任务、不影响 M3 对象存储主线。

## 1. 改动文件

- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
git diff --check PASS
```

5188 前端服务保持运行，便于用户刷新验收。

---

# 开发报告：8C-GD1-F9 追加 BIM 协同入口减负

时间：2026-05-29 CST

## 0. 最新结论

根据用户在 `http://127.0.0.1:5188/bim-collaboration` 的现场反馈，本轮继续收敛 BIM 协同页首屏：

- 已移除页面上方大块 `BIM协同管理亮点功能` 模块，避免占用首屏并遮挡大屏视觉优先级。
- 保留“项目选择”能力，但改成轻量项目选择条，放到 `BIM协同管理平台` 大屏模块正上方。
- 用户进入页面后，首屏优先看到 BIM 协同可视化大屏，而不是说明性卡片。
- 项目选择仍可打开项目面板，不删除项目切换能力，不破坏原项目上下文。

## 1. 改动文件

- `frontend/src/modules/visualization/pages/DigitalTwinPortalPage.vue`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
git diff --check PASS
浏览器短验 PASS：5188 页面顶部说明大卡已移除，项目选择条已移动到 BIM 协同大屏上方
```

## 3. 当前边界

- 本轮只改 BIM 协同页前端信息层级，不改后端、不改转换任务、不改 M3 对象存储主线。
- 本轮没有新增模型范围、没有新增 NAS / MinIO / Hermes 能力。
- 5188 前端预览服务保持运行，便于用户继续验收。

---

# 开发报告：8C-GD1-F9 BIM 协同页模型画布与轻量化模型信息重排

时间：2026-05-29 CST

## 0. 最新结论

本轮只处理葛兰岱尔可视化分支的 BIM 协同页展示，不改后端转换任务、不改 M3 对象存储主线。

- 已移除主视觉中的 `GLANDAR RVT PILOT / 105 项目 10 个 RVT 试点模型` 说明板块。
- 综合驾驶舱的“项目概览”改为展示模型文件总数、已轻量化数量、待轻量化数量。
- 顶部模块 `建筑能效` 已改为 `轻量化模型列表`。
- `轻量化模型列表` 展示 10 个已轻量化模型名称、文件 ID、大小、版本、状态，并提供“打开预览”和“文件管理定位”。
- 综合驾驶舱继续保留左右两侧和下方 BI 数据区，模型在中间展示并自旋。
- BIM 场景仍是聚焦模型模式，不自动轮播、不自旋。
- 内嵌 Viewer 背景增加主题化网格与光晕，浅色主题不再只是水泥灰底，深色主题保持大屏蓝黑风格。

## 1. 改动文件

- `frontend/src/modules/visualization/pages/DigitalTwinDashboardPage.vue`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.css`
- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS（8080）
8C-GD1 专项脚本 PASS=11 FAIL=0（使用 18088 葛兰岱尔分支后端）
file-access 安全回归 PASS=18 FAIL=0
git diff --check PASS
浏览器短验 PASS：5188 上试点说明板块消失，轻量化模型列表可见，项目概览显示 198 / 10 / 188
```

## 3. 当前边界

- 本轮没有新增转换能力，没有扩大模型范围。
- 本轮没有修改 NAS / MinIO / Hermes 主线。
- 构件拾取仍作为独立 SDK 对齐问题，不在 F9 内声明解决。
- `engineSessionKey` 仍是隔离分支 PoC 通道，合入主线前应改为葛兰岱尔短时 Viewer Session。

---

# 开发报告：8C-GD1-F8 嵌入 Viewer 体验修复

时间：2026-05-29 CST

## 0. 最新结论

本轮按用户反馈修复 BIM 协同页内嵌葛兰岱尔 Viewer 的展示逻辑：

- 关闭自动模型轮播，不再每隔数秒自动切换模型。
- 修正模型选择口径：不是“只展示全楼层模型”，而是默认把“全楼层 / 完整度最高”的模型排在首位，同时保留全部已轻量化模型，可在“展示模型”下拉框手动切换。
- 综合驾驶舱保留左右两侧和下方 BI 大屏数据区，中间模型自动自旋展示。
- BIM 场景不自旋，切换后只保留聚焦模型视图，由鼠标拖动、缩放、平移控制。
- 嵌入 Viewer 路由改为裸 Viewer 壳层，避免 iframe 内再次渲染整个平台左侧菜单和顶部栏。
- 3D 场景背景跟随 BIM 协同主题：深色主题为深蓝大屏背景，浅色主题为浅色中台背景。
- 恢复页面主题切换按钮，用“切换浅色 / 切换深色”替代不明显的 switch。

## 1. 改动文件

- `frontend/src/modules/visualization/pages/DigitalTwinDashboardPage.vue`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `frontend/src/modules/visualization/pages/GlandarModelPreviewPage.vue`
- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `frontend/src/router/index.ts`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS（8080）
git diff --check PASS
5188 前端服务仍在运行
```

## 3. 当前边界

- 本轮只修 Viewer 嵌入体验和展示策略，不改转换任务、不改 M3 对象存储主线。
- 构件拾取仍需继续和葛兰岱尔 SDK 对齐；本轮不把“未拾取到构件”强行伪装为已解决。
- 后续合入主线前，`engineSessionKey` 这类 PoC 通道仍需替换为引擎方短时 Viewer Session。

---

# 开发报告：8C-GD1-F7 构件拾取复核与 5188 预览状态

时间：2026-05-29 CST

## 0. 最新结论

本轮针对用户反馈“5188 端口点击模型无法选中构件”继续排查：

- 已确认前端模型可加载、旋转/缩放链路和 Viewer 页面本身不是唯一问题。
- 已将默认构件拾取收回到葛兰岱尔 demo 的单一路径：左键短按只走平台 `pointerup -> Feature.getByEvent`，不再额外注册一层引擎 `LEFT_CLICK` 监听，避免“双重拾取后把已选构件清空”。
- 已保留坐标兜底候选，兼容嵌入 iframe 和独立 Viewer 两种场景。
- 前端构建通过，后端构建通过，`git diff --check` 通过。

## 1. 当前阻塞

真实 8B-GD 后端 `18088` 当前无法完成 GLANDAR 模式验证，因为本地安全环境里没有可继承的 `GLANDAR_TOKEN`。

- 旧 `18088` 进程已经退出，无法从旧进程环境继承 token。
- 已尝试读取系统环境，未发现可用 token。
- 临时将 `5188` 前端连接到当前可用 `8080` 后端后，登录可用，但该后端不是完整 8B-GD 试点运行态，不能用于验证 10 个 RVT 试点模型构件拾取。

因此本轮代码已修，但“真实点击选中构件”还需要用户重新用安全方式启动 `18088` 的 GLANDAR 后端后继续验收。

## 2. 改动文件

- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/dto/VisualizationDtos.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `frontend/src/modules/visualization/api/visualization.ts`
- `handoff/dev-agent/latest-report.md`

## 3. 安全说明

为验证 demo 的 `secretKey` 初始化前置条件，本分支临时增加了 `engineSessionKey` 字段作为 PoC 通道。该做法只能用于当前隔离分支验证，不应直接合入主线生产形态。

主线合并前建议改为：由葛兰岱尔提供短时 Viewer Session Key / Viewer Ticket，平台后端签发短时票据，前端不接触长期 Station Token。

## 4. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端构建 PASS
git diff --check PASS
```

待继续验证：

```text
18088 使用 GLANDAR 安全配置启动
5188 连接 18088 后打开 BIM 协同 Viewer
左键短按模型构件能显示“已选中构件”并高亮
```

---

# 开发报告：8C-GD1-F6 左键构件拾取与高亮

时间：2026-05-29 CST

## 0. 最新结论

本轮按用户要求复用葛兰岱尔 demo 中的构件拾取逻辑，让平台内嵌 Viewer 支持左键短按选中构件：

- 左键拖动仍用于旋转模型。
- 左键短按不移动时触发 `Feature.getByEvent` 拾取构件。
- 选中构件后优先使用 demo 同款 `Feature.setColor` 将构件标为黄色。
- 再次选择会先清除旧构件高亮。
- 点击空处会取消当前选择。
- 嵌入模式下只显示一个轻量“已选中构件”浮层，不把 demo 的侧边工具面板搬进预览框。

## 1. 改动文件

- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `handoff/dev-agent/latest-report.md`

## 2. 复用来源

参考并迁入了引擎团队交接包中的核心逻辑：

- `/Users/vc/Downloads/rvt-preview-test-platform-demo-20260528-133459/rvt-preview-test/public/app.js`
  - `pickFeatureFromPointer`
  - `selectFeature`
  - `clearSelectedFeature`
  - `clearFeatureHighlight`
- `/Users/vc/Downloads/rvt-preview-test-platform-demo-20260528-133459/rvt-preview-test/public/viewerTools.js`
  - `pointerPickIntent`
  - `screenToEnginePickPosition`

## 3. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS（18088）
8C-GD1 专项脚本 PASS=10 FAIL=0
git diff --check PASS
```

## 4. 当前边界

- 本轮只增强 Viewer 前端交互，不改后端、不改转换任务、不改 M3 对象存储主线。
- 不读取模型构件业务属性，不落库构件，不做图模联动。
- 不触碰真实 NAS 文件，不暴露 token / NAS 路径 / bucket / object key。
- 18088 后端和 5188 前端继续保持运行，方便继续验收。

---

# 开发报告：8C-GD1-F5 综合驾驶舱与 BIM 场景视图分离

时间：2026-05-29 CST

## 0. 最新结论

本轮按用户反馈修正 BIM 协同窗口的两个顶层模式：

- `综合驾驶舱` 恢复原 BI 大屏信息结构：左侧项目/进度/问题数据、右侧模型/构件/趋势数据、底部待办/动态/版本信息继续展示。
- `BIM 场景` 保持上轮模型聚焦模式：隐藏两侧统计栏和底部低频信息，只保留模型预览框与基础工具区。
- 葛兰岱尔已轻量化模型仍可在原“展示模型”下拉框中选择，并在模型预览区域内显示。

## 1. 改动文件

- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS（18088）
8C-GD1 专项脚本 PASS=10 FAIL=0
git diff --check PASS
```

## 3. 当前边界

- 本轮只修前端视图模式切换，不改后端、不改转换任务、不改 M3 对象存储主线。
- 不触碰真实 NAS 文件，不读取模型正文，不暴露 token / NAS 路径 / bucket / object key。
- 18088 后端和 5188 前端继续保持运行，方便继续验收。

---

# 开发报告：8C-GD1-F4 BIM 协同窗口聚焦式 Viewer 嵌入

时间：2026-05-29 CST

## 0. 最新结论

本轮在葛兰岱尔 Station 恢复后继续修正平台内嵌 Viewer：

- `BIM 协同管理 -> 平台内 BIM 协同窗口` 已直接复用平台预留的模型预览框展示葛兰岱尔模型。
- 嵌入 Viewer 不再渲染平台左侧菜单、顶部平台栏或 Viewer 侧边工具栏，只保留模型画布。
- 当 105 项目存在已轻量化 RVT 试点模型时，BIM 协同窗口自动进入单栏模型聚焦模式，隐藏两侧统计栏和底部低频信息。
- 模型选择仍保留在原“展示模型”下拉框中，用户可以在 10 个 RVT 试点模型之间切换。
- 鼠标旋转、平移、滚轮缩放等基础三维交互继续由 `GlandarViewerCanvas` 承载。

## 1. 改动文件

- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.css`
- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `frontend/src/modules/visualization/pages/GlandarModelPreviewPage.vue`
- `frontend/src/modules/visualization/pages/DigitalTwinDashboardPage.vue`
- `frontend/src/router/index.ts`
- `handoff/dev-agent/latest-report.md`

## 2. 验证结果

已通过：

```text
后端构建 PASS
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS（18088）
8C-GD1 专项脚本 PASS=10 FAIL=0
Phase2 batch4 文件访问安全回归 PASS=18 FAIL=0
git diff --check PASS
```

引擎输出复核：

```text
GET 105 基准模型 root.glt -> HTTP 200
10 个 RVT 试点模型均为 READY 且可签发安全 viewer ticket
```

浏览器短验：

- `http://127.0.0.1:5188/bim-collaboration?projectId=503&preview=glandar` 可打开。
- 105 项目 10 个 RVT 试点模型可见。
- 下方 BIM 协同窗口中，模型显示在原预览框内，不再嵌套整个平台页面。
- 当前 5188 前端已代理到 18088 的 8B-GD 后端，方便继续预览本分支。

## 3. 当前边界

- 本轮只修葛兰岱尔模型预览嵌入体验，不改 M3 对象存储主线。
- 未触碰真实 NAS 文件，未新增 Hermes、parser、documents/chunks、Qdrant、OpenSearch。
- 前端不接触葛兰岱尔 token，不暴露 NAS 路径、`storage_uri`、bucket、object key、SQL 或 raw row。
- 18088 后端和 5188 前端均保留运行，方便用户继续验收。

---

# 开发报告：8C-GD1-F3 BIM 协同窗口嵌入修正

时间：2026-05-29 CST

## 0. 最新结论

本轮按用户反馈继续修正葛兰岱尔 Viewer 嵌入方式：BIM 协同管理下方模型预览区不再嵌入整套平台壳层，不再显示左侧平台菜单、顶部平台栏或 Viewer 侧边工具栏，只保留模型画布本身，并保留鼠标旋转、平移、缩放等基础交互。

当前平台嵌入层已收窄，但真实模型仍无法显示的直接原因不是平台壳层，而是葛兰岱尔 Station 模型输出/API 服务当前异常：`http://192.168.1.37:18086/Tools/output/model/.../root.glt` 返回 `HTTP 500.30 - ASP.NET Core app failed to start`。也就是说前端拿到的 `modelAccessAddress` 不可访问，Viewer 只能看到空画布/灰底，需引擎工作站恢复 `18086` 模型输出服务后才能真实渲染。

## 1. 改动文件

- `frontend/src/modules/visualization/bim-collab/BimCollaborationIsland.tsx`
- `frontend/src/modules/visualization/components/GlandarViewerCanvas.vue`
- `handoff/dev-agent/latest-report.md`

## 2. 实现说明

- 嵌入式葛兰岱尔模型存在时，停止 BIM 协同窗口的自动模型轮播，避免 iframe 每 5.2 秒重载导致 Viewer 还没完成渲染就被切走。
- Viewer 组件在模型加载后增加延迟适配视角和渲染刷新，主动执行主视角、模型适配、camera controls 重连和 request render。
- 嵌入模式下 Viewer 画布强制填满父容器，隐藏工具栏，避免侧边按钮和平台壳层压住模型区域。
- 加载引擎前先校验 `root.glt` 是否可访问；如果 Station 输出服务返回 500 / HTML 错误页，不再灰屏假加载，而是在模型框中显示业务化错误。

## 3. 验证结果

已通过：

```text
前端构建 PASS（仅既有 Vite chunk warning）
后端健康检查 PASS（18088）
git diff --check PASS
```

外部阻塞复核：

```text
GET http://192.168.1.37:18086/api/app/site-related/job-execution-status -> HTTP 500
GET http://192.168.1.37:18086/Tools/output/model/5441790901012277939/root.glt -> HTTP 500
错误页：HTTP Error 500.30 - ASP.NET Core app failed to start
```

## 4. 当前边界

- 本轮未改后端转换任务、未改数据库、未改 M3 对象存储主线。
- 本轮未触碰真实 NAS 文件、未读取模型正文、未暴露 token / NAS 路径 / bucket / object key。
- 平台侧下一步应在 Station `18086` 恢复后重新刷新 BIM 协同页验证真实模型渲染；如果仍不显示，再继续查 Viewer 初始化或模型输出目录映射。

---

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
