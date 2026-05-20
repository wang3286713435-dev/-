# 二期 G1-P2 小修复：测试数据自清理 / 隔离测试项目规划

时间：2026-05-19

## 1. 背景

`二期插入批次 G1：Agent 引导式交付治理 MVP` 已正式收口，当前无 P0/P1。

测试 agent 报告保留一个非阻塞 P2：`scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh` 和页面验收会留下 `PHASE2-G1-*` 元数据与测试挂接。连续复跑会影响样板项目缺失项、完整率和交付包预检查数字。

该问题不触碰真实 NAS，不阻塞 G1 收口，但会污染样板项目和演示口径，建议作为小修复先处理。

## 2. 本批目标

只处理 G1 验收脚本测试数据污染问题。

目标：

- G1 专项脚本复跑后不污染样板项目。
- 脚本创建的测试部位、节点类型、交付物标准、目录模板、mock 文件元数据和测试挂接可以清理或隔离。
- 脚本失败时尽量执行清理。
- 不改 G1 产品功能。
- 不碰真实 NAS。

## 3. 推荐方案

优先方案：`脚本自清理 + 可配置隔离项目`

实现要点：

- 在脚本中记录本轮创建的资源 ID。
- 使用 `trap cleanup EXIT`，脚本正常或失败退出都尽量清理。
- 优先通过已有 API 删除/软删除资源：
  - `DELETE /api/work-center/projects/{projectId}/delivery-bindings/{bindingId}`
  - `DELETE /api/data-steward/projects/{projectId}/file-resources/{fileId}`
  - `DELETE /api/master-data/projects/{projectId}/directory-templates/{templateId}`
  - 如交付物定义、交付物类型、交付物属性、部位、节点类型已有删除接口，则使用 API。
- 如果某些主数据没有删除接口，不能为测试脚本硬改业务删除能力；可改用隔离测试项目方案，或将资源标记为本轮专用且不再污染样板项目。
- 支持环境变量 `G1_TEST_PROJECT_ID`：
  - 设置时使用该项目作为隔离项目。
  - 未设置时仍可使用当前项目，但必须执行 best-effort cleanup。

可接受方案：

- 若清理主数据成本过高，创建或复用 `PHASE2-G1-SANDBOX` 测试项目，后续所有 G1 脚本默认在该项目运行。
- 但不应在每次脚本运行时不断创建新的项目。

## 4. 明确禁止

本批严禁：

- 修改 G1 产品功能。
- 修改 `docs/**`。
- 改数据库历史迁移。
- 为了脚本清理新增危险的生产删除能力。
- 硬删除真实业务数据。
- 触碰真实 NAS 文件。
- 开放真实 NAS 增删改查。
- 接入真实 BIM 引擎或真实轻量化。
- 读取文件正文。
- 写 Hermes memory / OpenSearch / Qdrant / MinIO documents/chunks。

## 5. 验收标准

测试 agent 至少验证：

1. `check-phase2-insert-g1-agent-delivery-governance.sh` 仍通过。
2. 脚本连续运行两次仍通过。
3. 第二次运行前后，样板项目中可见的 `PHASE2-G1-*` 残留不继续增长。
4. 脚本失败时 cleanup 仍尽量执行。
5. 8A、7A、6B、批次 3 回归仍通过。
6. 不触碰真实 NAS。
7. 不新增真实业务删除风险。

## 6. 收口口径

本批只是测试卫生修复。通过后仍保持暂停，不自动进入 8B 或其他主线开发。
