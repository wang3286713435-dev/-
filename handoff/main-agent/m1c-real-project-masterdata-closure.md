# M1C：工程主数据真实项目落地收口报告

更新时间：2026-05-20

## 1. 主 Agent 结论

`M1C：工程主数据真实项目落地` 可以正式收口。

测试 agent 已完成专项验收，当前未发现 P0 / P1 / P2。

M1C 已把真实项目接入从“模板演示”推进到“资产证据 -> 接入评估 -> 草案预览 -> 人工确认 -> 工程主数据可维护”的平台本体流程。

## 2. 收口依据

已确认通过：

- 105 / 503 / 启航华居项目接入评估与草案预览通过。
- 93 / 506 / 中建八局国交酒店项目接入评估与草案预览通过。
- 两个真实项目均为 `NAS_REAL_PILOT`，且 `realNasProject=true`。
- assessment 展示资产登记、文件类型统计、主数据状态、缺口和下一步动作。
- preview 展示 `catalog_only`、证据来源、置信度、风险提示和人工确认要求。
- 未确认 apply 被拒绝。
- 隔离 smoke 项目确认 apply 后可应用，重复应用会跳过已有项，不覆盖已有主数据。
- 草案应用审计事件存在。
- 应用后可继续进入部位树、节点类型、交付物标准、文档交付和图纸交付。

## 3. 安全与路线边界

已确认未发生：

- 真实 NAS 文件创建、移动、删除、重命名、上传。
- PDF / Office / DWG / RVT / IFC 正文读取。
- parser / writer / indexing。
- Hermes 新能力开发。
- G4 继续开发。
- 8B / 8C / 9A 推进。
- Agent 自动治理。

API 响应和前端可见文本未发现：

- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `raw row`
- `SQL`
- `token`
- `secret`
- `password`

## 4. 验证结果

测试 agent 已执行并通过：

- 后端构建。
- 前端构建，仅保留既有 Vite chunk size warning。
- 后端健康检查。
- `scripts/dev/check-m1c-real-project-masterdata.sh`，`PASS=14 FAIL=0`。
- `scripts/dev/check-phase2-batch4-file-access.sh`，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch6a-project-initialization.sh`。
- `scripts/dev/check-phase2-batch6b-delivery-package.sh`，`PASS=17 FAIL=0`。
- `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`，`PASS=11 FAIL=0`。
- `git diff --check`。

## 5. 残余说明

- M1C 不代表平台已经能自动识别真实楼栋、楼层、系统或 BIM 构件。
- M1C 不代表进入真实 BIM 轻量化、构件级解析或客户交付准备。
- 真实工程主数据仍需项目负责人复核。
- `catalog metadata` 仍不能替代 PDF/Office 正文、DWG 图层/标题栏、RVT Family/Type/构件参数证据。
- 前端 Vite chunk size warning 是既有非阻塞 P2，不影响 M1C 收口。

## 6. 下一步建议

下一批建议进入：

`M1D：标准驱动交付闭环强化`

M1D 目标应围绕员工不依赖 Hermes，完成一次标准驱动交付闭环：

- 应交项。
- 缺失项。
- 文件挂接。
- 审核。
- 整改。
- 复审。
- 完整率刷新。
- 交付包预检查。

M1D 启动前需要用户再次确认。
