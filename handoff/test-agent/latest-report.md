# M1A 登录超时修复后极短复验报告

生成时间：2026-05-20 14:22 CST

## 1. 测试结论

结论：通过。

本轮只复验 `M1A：登录超时修复后极短回归`，重点检查上一轮阻塞项 `P1-M1A-LOGIN-TIMEOUT` 和两项路径脱敏 P1。真实浏览器 fresh login 已可进入 `/data-steward/assets`，未再出现 `timeout of 10000ms exceeded`；503 / 506 项目页面、工程主数据和工作中心抽查未见白屏、500 或明显横向撑爆；Catalog 和旧文件资源路径脱敏通过。

建议：M1A 登录超时 P1 可关闭，当前可建议主 agent 收口 M1A。仍不建议在本轮进入 G4 / 8B / 8C / 9A；Hermes 继续冻结。

## 2. 当前分支与提交

- 当前分支：`codex/platform-m1a-baseline-fixes`
- 当前提交：`0c97419`
- 工作区状态：存在开发 agent / 主 agent 本轮未提交改动和临时目录；测试 agent 未修改业务代码，未回退任何改动。

## 3. G4 暂停与 Hermes 冻结

结果：通过。

已按 prompt 阅读 M1A 相关 handoff、roadmap、Hermes freeze 文档。文档状态仍明确：G4 暂停，Hermes 冻结，本轮不进入 8B / 8C / 9A，不新增 Hermes Evidence / Memory / Orchestration 能力。

本轮测试未发现：

- 新增 Hermes 写入能力。
- 写 Hermes memory。
- 真实 BIM 轻量化。
- selective indexing。
- 真实 NAS 写操作。
- 文件正文抽取。

## 4. P0 / P1 / P2

P0：未发现。

P1：未发现。

- `P1-M1A-LOGIN-TIMEOUT`：已关闭。Chrome fresh login 从 `/login` 点击登录后约 2 秒进入 `/data-steward/assets`，未出现 `timeout of 10000ms exceeded`、白屏或 500。
- Catalog 详情路径脱敏 P1：已关闭。503 / 506 样本文件详情均为 `storagePath=null`、`storagePathVisible=false`。
- 旧文件资源路径脱敏 P1：已关闭。503 / 506 旧文件资源接口 `storageUri` 非空数量为 0；`/data-steward/files` 页面显示 `底层路径已隐藏`。

P2：

- 前端构建仍有既有 Vite chunk size warning，不影响构建通过。
- 浏览器抽查中 `/data-steward/assets` 默认真实项目统计显示为 0，但本轮目标是登录超时与路径脱敏极短复验，未作为本轮阻塞项展开。

## 5. 必跑命令结果

- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 chunk size warning。
- 后端健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- 文件访问安全脚本：`bash scripts/dev/check-phase2-batch4-file-access.sh`，通过，`PASS=18 FAIL=0`，输出 `phase2 batch4 file access check ok`。
- 格式检查：`git diff --check`，通过。

环境说明：

- 首次健康检查前后端服务不完整：后端 8080 和前端 5173 均曾未监听。
- 已按项目现有方式启动后端 `scripts/dev/start-backend.sh`，再启动前端 dev server `corepack pnpm --dir frontend dev --host 127.0.0.1` 后继续复验。

## 6. Fresh Login 复验

结果：通过。

浏览器：Google Chrome。

步骤与结果：

- 打开 `/login`，页面正常展示登录表单。
- 使用 `platform.admin / Admin@123` 登录。
- 点击登录后跳转到 `/data-steward/assets`，页面显示资产总览、左侧 `数据管家` 菜单、当前用户 `platform.admin`。
- 未出现 `timeout of 10000ms exceeded`。
- 未出现白屏、500 或页面卡死。

## 7. 503 页面抽查结果

样本：内部项目 ID `503`，业务编码 `105`，项目名 `启航华居项目`。

结果：通过。

已抽查：

- `/data-steward/assets/503`：项目工作台可打开，顶部项目工作台导航存在，显示 `启航华居项目 / 105`。
- `/data-steward/assets/503/master-data/initialization`：真实项目接入向导可打开，显示 catalog-only 评估、草案/待确认文案，不读取正文、不触碰 NAS 的说明存在。
- `/data-steward/assets/503/master-data/sections`：部位树页面可打开，项目导航持续存在，前置条件说明和操作顺序可见。
- `/data-steward/assets/503/work/document-delivery`：文档交付页面可打开，项目导航持续存在，说明、缺失项/已挂接入口可见。
- `/data-steward/assets/503/work/drawing-delivery`：图纸交付页面可打开，项目导航持续存在，说明、缺失项/已挂接入口可见。

未发现白屏、500、明显横向撑爆或项目上下文丢失。

## 8. 506 页面抽查结果

样本：内部项目 ID `506`，业务编码 `93`，项目名 `中建八局国交酒店项目`。

结果：通过。

已抽查 `/data-steward/assets/506`，页面进入 506 项目工作台子页，顶部项目工作台导航持续存在，显示 `中建八局国交酒店项目 / 93`，页面正常展示项目内导出/文件元数据视图，未见白屏、500 或项目上下文丢失。

## 9. 路径脱敏专项

结果：通过。

Catalog 文件详情：

- 503 样本 `fileId=3767`：`storagePath=null`、`storagePathVisible=false`。
- 506 样本 `fileId=24560`：`storagePath=null`、`storagePathVisible=false`。

旧文件资源接口：

- 503：`GET /api/data-steward/projects/503/file-resources?pageNo=1&pageSize=5` 返回 200，前 5 条 `storageUri` 非空数量为 0。
- 506：`GET /api/data-steward/projects/506/file-resources?pageNo=1&pageSize=5` 返回 200，前 5 条 `storageUri` 非空数量为 0。

旧文件资源页面：

- `/data-steward/files` 页面可见文件列表，路径状态列显示 `底层路径已隐藏`。

敏感字段检查：

- Catalog 列表和详情未发现 `/Volumes`、`smb://`、`nas://`、`storage_path`、`storage_uri`、raw DB row、SQL、secret、password 泄露。

## 10. 禁止项检查

结果：通过。

本轮未发现：

- 真实 NAS 增删改查。
- 文件移动、删除、重命名或上传。
- PDF / Office / DWG / RVT / IFC 正文读取。
- BIM 构件级解析。
- selective indexing。
- Hermes memory 写入。
- OpenSearch / Qdrant / MinIO documents/chunks 写入。
- Agent 自动审批、自动整改或自动创建真实交付结论。

## 11. 未解决问题与建议

未解决阻塞问题：无。

建议：

- `P1-M1A-LOGIN-TIMEOUT` 可关闭。
- M1A 可建议主 agent 收口。
- 可进入下一步 Git checkpoint / 主线平台开发判断，但仍应遵守 Hermes 冻结，不在本轮进入 G4 / 8B / 8C / 9A。
