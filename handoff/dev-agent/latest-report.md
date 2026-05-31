# 开发 Agent 报告：M3G-8-F1 M3G-7R 回归脚本饱和环境修复

时间：2026-06-01 CST

## 1. 本轮目标

本轮执行 `M3G-8-F1：M3G-7R 回归脚本饱和环境修复`。

目标是修复测试 agent 发现的 M3G-7R 脚本 P1：当非 105 项目对象化已接近饱和，或本机只剩 0/1 个可读可执行样本项目时，脚本不能再硬性要求“至少推进 2 个项目”后失败；应改为验证饱和/候选不足解释、只读 dry-run、`confirmed=false` 防写保护、已有 `OBJECT_STORED` 样本可读、NAS 原文件未变化和禁出字段扫描。

## 2. 改动文件列表

- `scripts/dev/check-m3g7r-all-project-objectification-run.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `backend/**`、`frontend/**`、数据库迁移或 `docs/**`。

## 3. 脚本修复内容

- M3G-7R 脚本新增双分支：
  - 强验证分支：当可读执行样本覆盖至少 2 个非 105 真实项目时，继续执行原有小批量对象化验证。
  - 饱和/候选不足/环境不足分支：当可读执行样本不足 2 个项目时，不执行真实迁移，只验证接口解释、安全保护和已有对象读取。
- 饱和分支新增校验：
  - `projects` 队列必须返回 `queueStatus / queueReason / riskMessages` 等可解释字段。
  - dry-run 必须是只读，不创建迁移任务或对象版本。
  - 105 必须保持 `COMPLETED`。
  - 95 / 98 / 99 不能进入可执行队列。
  - `start confirmed=false` 必须被拒绝。
  - 分支执行前后迁移任务数、对象版本数不增加。
  - 抽取已有 `OBJECT_STORED` 样本，通过受控 file-access 读取，并验证 NAS 原文件 size/mtime 未变化。
  - 响应继续执行 forbidden-field scan。
- 保留原强路径，不把真实接口错误吞掉；只在样本不足时进入安全分支。

## 4. 为什么这次默认没有执行迁移

当前本机环境下，M3G-7R dry-run 能规划多个非 105 项目，但脚本实际检查 NAS 源文件可读性时，只有 projectId=504 可读样本满足条件；另一个 dry-run 项目的 NAS 源文件在本机不可读。

因此本轮默认脚本进入“候选不足/环境不足”分支，没有执行真实迁移。这符合本批修复目标：不为了通过脚本去强行迁移，也不伪造项目或文件。

## 5. 安全边界

- 未移动、删除、重命名、覆盖真实 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未启用 parser / writer / indexing。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未创建假项目或假资产绕过测试。
- 未扩大对象化迁移数量。
- 未暴露真实 NAS 路径、`storage_uri`、对象 key、bucket、SQL、raw row、secret/token/password 真值。

## 6. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3G-7R 饱和分支强制短验（MAX_PROJECTS=1 MAX_FILES=2）        PASS=20 FAIL=0
M3G-7R 默认专项脚本                                          PASS=23 FAIL=0
M3G-8 对象优先读取 / fallback 回归                           PASS=7 FAIL=0
M3G-6S-F1 对象版本 / NAS 侧 MinIO 对齐回归                   PASS=13 FAIL=0
M3G-6T 105 工程树交付映射回归                                PASS=30 FAIL=0
Phase2 Batch4 文件访问安全回归                               PASS=18 FAIL=0
git diff --check                                             PASS
```

已执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
MAX_PROJECTS=1 MAX_FILES=2 bash scripts/dev/check-m3g7r-all-project-objectification-run.sh
bash scripts/dev/check-m3g7r-all-project-objectification-run.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-m3g6s-f1-object-version-minio-alignment.sh
bash scripts/dev/check-m3g6t-105-engineering-tree-delivery-mapping.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 7. 服务状态

- 后端保持运行：`http://127.0.0.1:8080`
- 前端 dev server 保持运行：`http://127.0.0.1:5173`
- 后端当前使用 NAS 侧 MinIO 环境，readiness 为 `NAS_SIDE_MINIO / READY`。

## 8. 未完成事项和风险

- 本批只修复 M3G-7R 回归脚本，不修改后端队列算法和迁移执行逻辑。
- 当前本机仍存在“部分真实项目 NAS 源文件不可读”的环境现象；脚本已将它识别为候选/环境不足，不会误执行迁移。
- M3G-6S-F1 完整性脚本会两次验证 105 的 2928 个对象版本，耗时较长，但本轮已通过。
