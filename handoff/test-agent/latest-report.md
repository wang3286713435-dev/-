# M1C 工程主数据真实项目落地验收报告

生成时间：2026-05-20 16:42 CST

## 1. 测试结论

结论：通过。

本轮只验收 `M1C：工程主数据真实项目落地`。M1C 作为 M1B 后的平台本体稳定批次，重点验证真实 NAS 项目的接入评估、草案预览、人工确认闸门、幂等应用和工程主数据页面落地体验。

当前未发现 P0 / P1 / P2。建议主 agent 判定 M1C 收口。

## 2. 当前分支与工作区

- 当前分支：`codex/platform-m1c-real-masterdata`
- 当前提交：`0e73eae`
- 工作区状态：存在开发 agent 本轮未提交改动和临时运行文件；测试 agent 未修改业务代码，未修改 `docs/**`，仅更新本测试报告。
- 新增脚本：`scripts/dev/check-m1c-real-project-masterdata.sh` 已存在并通过验收。

## 3. 必读文件

已阅读并按要求参考：

- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/main-agent/m1b-project-workbench-usability-closure.md`
- `handoff/main-agent/m1c-real-project-masterdata-plan.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

## 4. 必跑命令结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 后端健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- M1C 专项脚本：`bash scripts/dev/check-m1c-real-project-masterdata.sh`，通过，`PASS=14 FAIL=0`。
- 文件访问安全回归：`bash scripts/dev/check-phase2-batch4-file-access.sh`，通过，`PASS=18 FAIL=0`。
- 6A 项目初始化回归：`bash scripts/dev/check-phase2-batch6a-project-initialization.sh`，通过，输出 `phase2 batch6a project initialization ok`。
- 6B 交付包回归：`bash scripts/dev/check-phase2-batch6b-delivery-package.sh`，通过，`PASS=17 FAIL=0`。
- 7A 预览 / 导出预检查回归：`bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`，通过，`PASS=18 FAIL=0`。
- 8A BIM 轻量化 Mock 适配层回归：`bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`，通过，`PASS=11 FAIL=0`。
- 空白字符检查：`git diff --check`，通过。

## 5. API 合同抽查

结果：通过。

`GET /api/master-data/projects/503/onboarding/assessment`：

- 项目编码：`105`。
- `assetSource=NAS_REAL_PILOT`。
- `realNasProject=true`。
- `assetCatalogOnly=true`。
- `evidenceMode=catalog_only`。
- 文件数量：`2927`。
- 路径映射数量：`1`。
- 扫描记录数量：`1`。
- 主要扩展名：`DWG / PDF / RVT`。
- 主要专业线索包含：`ARCHITECTURE / ELECTRICAL / FIRE_PROTECTION / GAS / GENERAL / HVAC / INTELLIGENT / PLUMBING`。
- 目录线索已脱敏为 `项目内目录线索已脱敏`。

`GET /api/master-data/projects/506/onboarding/assessment`：

- 项目编码：`93`。
- `assetSource=NAS_REAL_PILOT`。
- `realNasProject=true`。
- `assetCatalogOnly=true`。
- `evidenceMode=catalog_only`。
- 文件数量：`5912`。
- 路径映射数量：`1`。
- 扫描记录数量：`2`。
- 主要扩展名：`DWG / IFC / PDF / RVT`。
- 主要专业线索包含：`ARCHITECTURE / ELECTRICAL / FIRE_PROTECTION / GAS / GENERAL / HVAC / INTELLIGENT / PLUMBING`。
- 目录线索已脱敏为 `项目内目录线索已脱敏`。

草案预览接口：

- 503 与 506 的 preview 均返回 `dryRun=true`、`confirmedRequired=true`、`nasTouched=false`、`contentRead=false`、`assetCatalogOnly=true`、`evidenceMode=catalog_only`。
- `draftItems` 均包含 `evidenceMode`、`evidenceSource`、`confidenceLevel`、`riskHint`、`pendingConfirmation=true`。
- 506 草案同时包含 `TEMPLATE_SKELETON`、`CATALOG_FILE_KIND_CLUE`、`CATALOG_DIRECTORY_CLUE`，满足“模板骨架 + 资产目录线索并存”要求。

人工确认闸门：

- `POST /api/master-data/projects/506/onboarding/apply` 传 `confirmed=false` 返回 HTTP 400，错误码 `REAL_PROJECT_ONBOARDING_CONFIRM_REQUIRED`。
- M1C 专项脚本使用隔离 smoke 项目验证了 `confirmed=true` 后可应用草案，重复应用表现为跳过已有项，不覆盖已有主数据，并产生 `masterdata.initialization.template-apply` 审计事件。

## 6. 105/503 与 93/506 验收

结果：通过。

- `105 / 503 / 启航华居项目`：接入评估、草案预览、目录线索脱敏、catalog-only 证据和风险字段均通过。
- `93 / 506 / 中建八局国交酒店项目`：接入评估、草案预览、模板骨架与资产目录线索并存、未确认 apply 拒绝均通过。

未发现为 `105 / 503` 写死导致 `93 / 506` 不适用的问题。

## 7. 前端页面验收

结果：通过。

浏览器已验证：

- `/data-steward/assets/503/master-data/initialization`
  - 点击 `预览草案` 后可见 `M1C 真实项目接入`、`真实 NAS 项目`、主要扩展名、主要专业线索、主要目录线索、`草案证据与风险`、`catalog_only`。
- `/data-steward/assets/506/master-data/initialization`
  - 点击 `预览草案` 后可见 `M1C 真实项目接入`、`真实 NAS 项目`、`模板骨架`、`草案证据与风险`、`catalog_only`、风险提示和待人工确认。
- `/data-steward/assets/503/master-data/sections`
  - 页面正常进入，可见“如果部位树来自接入草案”的复核提醒，提示项目负责人按真实楼栋、楼层、系统和专业范围复核。
- `/data-steward/assets/503/master-data/node-types`
  - 页面正常进入，可见“如果节点类型来自接入草案，它只是标准配置建议。锁定前请确认这些类型确实适用于当前真实项目。”提醒。
- `/data-steward/assets/503/master-data/deliverable-standard`
  - 页面正常进入，可见“如果交付物标准来自接入草案”的复核提醒。
- `/data-steward/assets/503/work/document-delivery`
  - 页面正常进入，显示 `文档交付` 与 `启航华居项目`。
- `/data-steward/assets/503/work/drawing-delivery`
  - 页面正常进入，显示 `图纸交付` 与 `启航华居项目`。

上述页面未出现 403 / 404 / 500。

## 8. 安全边界

结果：通过。

API 响应和前端可见文本未发现：

- raw `storage_path`
- `storage_uri`
- `storageUri`
- 真实 NAS 路径
- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- SQL
- raw DB row
- secret / token / password

本轮未触发：

- 真实 NAS 文件创建、移动、删除、重命名、上传。
- PDF / Office / DWG / RVT / IFC 正文读取。
- parser / writer / indexing。
- Hermes 自动审批、自动整改、自动挂接或自动删除。
- G4 / Hermes 新能力开发。
- 8B / 8C / 9A 能力推进。

说明：M1C 专项脚本和 6A 回归脚本会创建隔离 smoke 项目和目录级测试数据，用于验证确认应用与幂等性；本轮未触碰真实 NAS 文件。

## 9. 回答验收问题

- M1C 是否通过：通过。
- 105/503 与 93/506 是否都通过：都通过。
- 是否发现 raw path 或敏感字段残留：未发现。
- 是否发现未确认也能 apply：未发现；`confirmed=false` 已被拒绝。
- 是否发现重复应用覆盖已有主数据：未发现；专项脚本验证重复应用跳过已有项。
- 是否发现页面缺失草案证据 / 风险 / 复核提醒：未发现阻塞缺失。节点类型页提醒文案不使用“复核”二字，但已明确“草案只是建议，锁定前确认是否适用于当前真实项目”，判定满足要求。
- 是否发现 G4 / Hermes / 8B / 9A 边界被突破：未发现。
- 是否建议主 agent 判定 M1C 收口：建议收口。
