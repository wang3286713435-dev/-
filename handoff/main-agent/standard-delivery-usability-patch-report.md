# 标准驱动交付流程新手可用性补丁报告

完成时间：2026-05-14 16:05 CST

## 1. 处理结论

已按测试 agent 最新报告中的 P1 建议完成轻量修复。

本轮没有进入二期批次四规划，只处理标准驱动交付流程里的新手理解问题。

## 2. 修复范围

页面范围：

- `/master-data/sections`
- `/master-data/node-types`
- `/master-data/deliverable-standard`
- `/work/document-delivery`
- `/work/drawing-delivery`

代码范围：

- `frontend/src/modules/master-data/components/StandardStatusPanel.vue`
- `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
- `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
- `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
- `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
- `frontend/src/styles/index.css`

## 3. 修复内容

- 增加三步主数据配置引导：
  - 第 1 步：工程管理部位树。
  - 第 2 步：节点类型。
  - 第 3 步：交付物标准。
- `StandardStatusPanel` 增加下一步提示，告诉用户现在缺什么以及应该先做什么。
- 工程管理部位树页面补充根节点、下级节点和编码的业务解释。
- 节点类型页面补充锁定含义、锁定后果和适用层级解释。
- 交付物标准页面补充四块配置关系和推荐顺序。
- 文档交付、图纸交付页面补充交付执行解释。
- 交付标准未就绪时补充修复入口：
  - 去配置交付物标准。
  - 检查节点类型。
- 将“缺失项”解释为标准要求存在但尚未选择文件完成交付的条目。
- 将“去挂接”调整为“选择文件补交”，降低系统内部术语感。
- 在补交弹窗中补充字段说明。

## 4. 未处理项说明

- 未清理样板项目里的历史测试命名数据，原因是这属于数据治理或样板数据整理，不应混入本轮前端可用性补丁。
- 未补齐样板项目缺失的文档类交付标准，原因是本轮目标是让页面告诉用户缺什么和去哪补，不改变样板数据本身。
- 未做标准配置向导、内置帮助中心、行业模板或客户版完整引导，这些保留到后续客户版体验设计。

## 5. 验证结果

- 前端构建通过。
- 后端健康检查通过，返回 `UP`。
- 五个目标页面路由均返回 HTTP 200。
- `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
- `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
- `git diff --check` 通过。

## 6. 当前裁决

主 agent 判断本轮 P1 已完成开发修复，可交给测试 agent 做短回归。

短回归重点：

- 五个页面是否能让新手理解当前页作用。
- 用户是否能看懂标准配置顺序。
- 前置条件缺失时是否知道去哪修。
- 缺失项到选择文件补交是否自然。
- 交付页没有重新出现横向溢出。
- 批次二和批次三核心链路不回归。
