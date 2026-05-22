# 开发 Agent 报告：M2D 真实项目工程主数据接入草案增强

时间：2026-05-22

## 1. 本轮目标

本轮执行 `M2D：真实项目工程主数据接入草案增强`。

目标是让 105 真实项目在已接入 2928 个资产文件、但工程主数据和交付标准已清空的状态下，能够基于 catalog-only 目录证据生成可解释的接入评估和草案预览，同时避免真实 NAS 项目被“一键应用模板”误置为 ready。

## 2. 读取的关键文档

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/m2d-real-project-masterdata-onboarding-plan.md`
- `handoff/main-agent/project-105-template-reset-report.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

## 3. 改动文件列表

本轮新增 / 修改：

- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/application/ProjectInitializationApplicationService.java`
- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/dto/InitializationDtos.java`
- `frontend/src/modules/master-data/api/masterData.ts`
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `scripts/dev/check-m2d-real-project-masterdata-onboarding.sh`
- `handoff/dev-agent/latest-report.md`

工作区仍有 M2A/M2B/M2C 及主 agent 留下的其他未提交改动，本轮未回退、未改动这些无关文件。

## 4. 后端实现说明

- `GET /api/master-data/projects/{projectId}/onboarding/assessment` 增强为真实资产线索评估：
  - 返回文件总数、模型、图纸、文档、清单表格计数。
  - 返回文件类型、扩展名、专业分布。
  - 返回脱敏目录线索、治理风险、Missing Evidence。
  - 所有证据模式保持 `evidenceMode=catalog_only`。
- `GET /api/master-data/projects/{projectId}/onboarding/preview` 增强为真实资产候选草案：
  - 增加专业候选、交付类型候选、目标候选。
  - 每个草案项返回 `evidenceSource`、`confidenceLevel`、`riskHint`、`pendingConfirmation=true`、`fromRealAssetClue`、`fromTemplateSkeleton`。
  - 模板项保留为参考骨架，不当作真实项目已确认标准。
- 对真实 NAS 项目保护：
  - `initialization:apply-template` 对 `NAS_REAL*` 且非 smoke 项目返回明确错误，不直接应用模板。
  - `onboarding/apply` 对真实 NAS 项目只记录草案确认审计，返回 `draftApplied=false`、`templateResult=null`，不会把 `deliverableStandardReady` 改成 true。
  - M1C smoke 项目仍保留可应用模板能力，避免回归脚本失效。

## 5. 前端实现说明

- 初始化页顶部文案改为“真实资产已接入，工程主数据未确认”。
- 真实项目显示警示：不会通过一键模板直接变成就绪标准。
- 展示资产统计、文件类型分布、扩展名分布、专业分布、治理风险和 Missing Evidence。
- 模板区改为“参考草案模板 / 参考骨架”，降低模板一键应用误导。
- 真实 NAS 项目的主按钮改为“进入人工配置”，跳转到部位树，不调用真实应用模板接口。

## 6. 脚本说明

新增：

- `scripts/dev/check-m2d-real-project-masterdata-onboarding.sh`

覆盖：

- 管理员登录和切换 503/105。
- 标准状态保持未就绪。
- assessment 返回真实文件数量、DWG/PDF/RVT/XLSX 分布、专业分布、治理风险、Missing Evidence。
- preview 返回 dry-run、无 NAS 触碰、资产候选草案、模板骨架、Missing Evidence。
- 真实项目 direct apply-template 被拒绝。
- onboarding apply 不写正式标准，ready 仍为 false。
- forbidden-field scan 未发现 raw path / SQL / secret。

## 7. 数据库迁移

本轮未新增数据库迁移，未修改旧迁移。

## 8. 边界确认

- 是否触碰真实 NAS 文件：否。
- 是否读取 PDF / Office / DWG / RVT / IFC 正文：否。
- 是否做 parser / writer / indexing：否。
- 是否写 OpenSearch / Qdrant / MinIO documents/chunks：否。
- 是否修改 Hermes：否。
- 是否暴露 `storage_path` / `storage_uri` / raw NAS path / raw DB row / SQL / secret：未发现。
- 是否修改 `docs/**`：否。
- 是否扩大到 M2E 或后续批次：否。

说明：M2A/M2B 回归脚本会按既有设计使用隔离临时目录验证受控 NAS 写入能力；这不是 M2D 新增能力，也未触碰真实项目 NAS 文件。

## 9. 自测命令与结果

```text
cd backend && ./mvnw -pl delivery-master-data -am -DskipTests compile       PASS
cd backend && ./mvnw -pl delivery-app -am -DskipTests package              PASS
corepack pnpm --dir frontend build                                        PASS（仅既有 Vite chunk size 警告）
curl -fsS http://127.0.0.1:8080/actuator/health                           PASS：{"status":"UP"}
bash scripts/dev/check-m2d-real-project-masterdata-onboarding.sh           PASS=8 FAIL=0
bash scripts/dev/check-m2c-delivery-package-archive.sh                    PASS=11 FAIL=0
bash scripts/dev/check-m2b-nas-write-trial.sh                             PASS=18 FAIL=0
bash scripts/dev/check-m2a-controlled-nas-write.sh                        PASS=21 FAIL=0
bash scripts/dev/check-m1f-employee-access-control.sh                     PASS=20 FAIL=0
bash scripts/dev/check-m1e-file-task-continuity.sh                        PASS=10 FAIL=0
bash scripts/dev/check-m1d-standard-delivery-loop.sh                      PASS=29 FAIL=0
bash scripts/dev/check-m1c-real-project-masterdata.sh                     PASS=14 FAIL=0
git diff --check                                                          PASS
```

后端已用最新构建包重启，当前 `127.0.0.1:8080` 健康检查为 UP。

## 10. 已知风险

- 105 项目当前仍处于工程主数据未确认状态，这是本轮目标状态，不是故障。
- `documentFileCount` 为方便项目接入评估按 PDF/Office 线索统计，可能与文件原始 `file_kind=DRAWING` 有交叉；草案页面已明确这是 catalog-only 线索。
- 目录线索在真实路径不可安全展示时返回“项目内目录线索已脱敏”，避免泄露路径，但也会降低目录结构判断颗粒度。

## 11. 未完成事项

- 未自动生成正式部位树、节点类型、交付定义、交付类型、交付属性或目录模板。
- 未做真实交付标准应用。
- 未做文件正文解析、图纸解析、BIM 构件级识别。
- 未做生产发布。

## 12. 是否建议进入 M2D 测试验收

建议进入测试 agent 验收。

重点验收：

1. 503/105 初始化页显示真实资产已接入、工程主数据未确认。
2. assessment / preview 均只返回 catalog-only 证据和 Missing Evidence。
3. 真实项目不会通过模板应用直接 ready。
4. 文档 / 图纸交付页继续提示工程主数据未就绪。
5. 响应中无 raw NAS path、`storage_uri`、SQL、secret。
