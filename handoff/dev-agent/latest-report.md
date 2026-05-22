# 开发 Agent 报告：M2E P1 修复 selectedDraftItemIds 契约

时间：2026-05-22

## 1. 本轮目标

本轮执行 `M2E P1 修复 selectedDraftItemIds 契约`。

测试 agent 指出：前端支持逐项勾选草案项并向后端发送 `selectedDraftItemIds`，但后端实际忽略该字段；空选择或不存在 ID 仍返回 OK，并按全量规则处理。此行为会误导用户以为“只采纳所选项”，实际却全量生成 / 跳过。

本轮采用方案 A：让 `selectedDraftItemIds` 真正生效。

## 2. 读取的关键文档

- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/m2e-real-project-masterdata-confirmation-plan.md`

## 3. 改动文件列表

本轮修改：

- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/application/ProjectInitializationApplicationService.java`
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `scripts/dev/check-m2e-real-project-masterdata-confirmation.sh`
- `handoff/dev-agent/latest-report.md`

沿用 M2E 既有文件：

- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/controller/ProjectInitializationController.java`
- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/dto/InitializationDtos.java`
- `frontend/src/modules/master-data/api/masterData.ts`

工作区仍存在主 agent / 其他批次留下的 handoff、`frontend/src/styles/index.css`、`.claude/**`、`CLAUDE.md`、`tmp/**` 等改动或未跟踪文件；本轮未回退这些内容。

## 4. 后端修复

`POST /api/master-data/projects/{projectId}/onboarding/confirm` 现在会读取并校验 `selectedDraftItemIds`：

- 空选择或未传选择：拒绝，错误码 `REAL_PROJECT_DRAFT_SELECTION_REQUIRED`，提示“请至少选择一个草案项”。
- 不存在或已失效 ID：拒绝，错误码 `REAL_PROJECT_DRAFT_SELECTION_INVALID`。
- 合法选择：只处理所选草案项及必要依赖项。

草案 ID 规则与前端保持一致：

```text
${category}:${name}:${evidenceSource}
```

后端会基于 `onboardingDraftItems(snapshot, templatePreview)` 重新生成合法草案项集合，再做校验，避免信任前端随意传入的 ID。

## 5. 过滤与依赖处理

已实现的选择过滤语义：

- 选择专业候选：只生成 / 跳过对应专业部位，并自动补项目根节点作为依赖。
- 选择项目级 / 专业级 / 文件类型级交付对象：按所选目标维度生成必要的部位、节点类型、交付定义、交付类型、属性或目录模板。
- 选择交付类型候选：只生成 / 跳过对应交付类型及其交付定义、节点类型和核心属性依赖。
- 选择交付定义 / 类型 / 属性 / 目录模板参考项：只处理对应正式规则及必要依赖。
- 依赖项在 `generatedItems.source` 中带 `DEPENDENCY+...`，不再静默全量生成。

`generatedItems` 现在只返回本次选择项及必要依赖项的 create / skip 结果。合法小选择已验证不会再返回全量 40 项。

## 6. 前端修复

- 初始化页草案表格保留勾选列，但不再默认暗中提交“资产线索项”。
- 用户未勾选草案项时，前端先提示“请至少选择一个要采纳的草案项”。
- 二次确认文案明确：后端只会处理勾选项及必要依赖项。
- 已确认项目的顶部文案从“工程主数据未确认”改为“工程主数据已生成，仍需人工复核和维护”。
- 顶部下一步入口补充“查看节点类型”。

## 7. 脚本修复

`scripts/dev/check-m2e-real-project-masterdata-confirmation.sh` 已补充断言：

- 空 `selectedDraftItemIds=[]` 被拒绝。
- 不存在 ID 被拒绝。
- 从后端 preview 生成合法草案 ID 后再确认。
- 合法小选择只返回所选草案项及必要依赖，`generatedItems` 数量大于 0 且小于 40。

## 8. 数据库迁移

本轮未新增数据库迁移，未修改旧迁移。

## 9. 安全边界

- 是否触碰真实 NAS 文件：否。
- 是否读取 PDF / Office / DWG / RVT / IFC 正文：否。
- 是否新增真实 NAS 写能力：否。
- 是否接入 Hermes 新能力：否。
- 是否接入 BIM 引擎 / parser / writer / indexing：否。
- 是否自动挂接 / 自动审核 / 自动整改 / 自动交付：否。
- 是否修改 `docs/**`：否。
- 是否暴露 `storage_path` / `storage_uri` / raw NAS path / raw DB row / SQL / secret：脚本 forbidden-field scan 未发现。

## 10. 自测结果

```text
cd backend && ./mvnw -pl delivery-master-data -am -DskipTests compile      PASS
cd backend && ./mvnw -pl delivery-app -am -DskipTests package              PASS
corepack pnpm --dir frontend build                                        PASS（仅既有 Vite chunk size 警告）
curl -fsS http://127.0.0.1:8080/actuator/health                           PASS：{"status":"UP"}
bash scripts/dev/check-m2e-real-project-masterdata-confirmation.sh         PASS=11 FAIL=0
bash scripts/dev/check-m2d-real-project-masterdata-onboarding.sh           PASS=8 FAIL=0
bash scripts/dev/check-m2c-delivery-package-archive.sh                    PASS=11 FAIL=0
bash scripts/dev/check-m1c-real-project-masterdata.sh                     PASS=14 FAIL=0
git diff --check                                                          PASS
```

后端已用最新构建包重启，当前 `127.0.0.1:8080` 健康检查为 UP。

## 11. P1 修复结论

测试 agent 的 P1 条件已不再成立：

- 页面不再允许用户空选后提交。
- 后端不再接受空选择。
- 后端不再接受不存在的草案 ID。
- 后端不再忽略 `selectedDraftItemIds`。
- 合法小选择不会被全量处理成 40 个生成 / 跳过项。

## 12. 已知风险

- 105 当前已处于 M2E 人工确认后状态，脚本验证的是幂等状态和选择契约；没有清理真实 105 数据重新复测 first-run。
- 当前选择过滤覆盖 M2E 最小可用规则，不代表已经支持 BIM 构件级、图纸解析级或文件正文级规则确认。
- 工作区仍有非本轮交付的未跟踪文件和既有改动，主 agent 收口提交时需确认归属。

## 13. 是否建议进入测试复核

建议测试 agent 复核 M2E P1 修复。

重点复核：

1. 空 `selectedDraftItemIds` 是否拒绝。
2. 无效 `selectedDraftItemIds` 是否拒绝。
3. 合法小选择是否只返回所选草案项及必要依赖。
4. 初始化页已确认状态文案和“查看节点类型”入口是否到位。
5. 未触碰真实 NAS、未读取正文、未新增 Hermes / BIM / parser / indexing。
