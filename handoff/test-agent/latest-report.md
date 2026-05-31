# 测试 Agent 报告：M3G-8-F1 回归脚本饱和环境修复验收

时间：2026-06-01 05:37 CST

## 1. 结论

M3G-8-F1 验收通过。

本轮确认 `scripts/dev/check-m3g7r-all-project-objectification-run.sh` 已能正确处理当前“可读执行样本不足 / 对象化接近饱和”的环境：脚本没有继续强制要求推进 2 个项目，也没有静默跳过核心断言，而是进入候选不足 / 环境不足 / 饱和分支，验证队列解释、dry-run 只读、`confirmed=false` 防线、已有 `OBJECT_STORED` 样本 file-access 可读和 NAS 原文件未变化。

建议主 agent 继续收口 M3G-8。

## 2. P0 / P1 / P2

- P0：无。
- P1：无。上一轮 P1 “M3G-7R 回归脚本在饱和环境失败”已关闭。
- P2：既有前端 Vite chunk size warning；`.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项仍存在但未纳入 Git。

## 3. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `corepack pnpm --dir frontend build`：通过，仅既有 chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-m3g7r-all-project-objectification-run.sh`：通过，`PASS=23 FAIL=0`。
- `bash scripts/dev/check-m3g8-object-first-read-fallback.sh`：通过，`PASS=7 FAIL=0`。
- `bash scripts/dev/check-m3g6s-f1-object-version-minio-alignment.sh`：通过，`PASS=14 FAIL=0`。
- `bash scripts/dev/check-m3g6t-105-engineering-tree-delivery-mapping.sh`：通过，`PASS=30 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。
- `git diff --cached --check`：通过。

## 4. 当前脚本分支判断

本轮 `check-m3g7r-all-project-objectification-run.sh` 进入的是候选不足 / 环境不足 / 饱和分支，不是样本充足强执行分支。

关键输出：

- dry-run 至少覆盖 2 个非 105 真实项目。
- 实际可读执行样本只覆盖 `projectId=504` 1 个项目。
- 脚本输出：`执行样本仅覆盖 1 个项目，进入候选不足/环境不足校验`。
- 饱和分支确认项目队列和 dry-run 已说明当前候选不足、环境不足或对象化饱和。

这符合本批验收目标：样本不足时不再误判为失败，但必须有清晰解释和安全验证。

## 5. 是否执行真实对象化

未执行真实对象化迁移。

饱和分支明确验证：

- `start confirmed=false` 被拒绝。
- 分支执行前后迁移任务数未增加。
- 分支执行前后 active object version 数未增加。
- 输出：`饱和分支未执行迁移且未新增对象版本`。

## 6. dry-run / confirmed=false 防线结果

- dry-run 未创建迁移任务或对象版本。
- dry-run 满足硬上限。
- `confirmed=false` 的 start 请求被拒绝。
- 105 仍归入 `COMPLETED`，不会重复执行。
- 95 / 98 / 99 未进入可执行队列。

## 7. file-access 抽查结果

M3G-7R 饱和分支抽查已有 `OBJECT_STORED` 样本：

- `fileId=935` 可通过受控 file-access 读取。
- 读取后 NAS 原文件 size / mtime 未变化。

Phase2 Batch4 file-access 回归也通过：

- 管理员预览 / 下载通过。
- 查看者可预览但不能下载。
- 交付工程师可预览 / 下载。
- 无项目权限用户不能跨项目创建访问票据。
- catalog 列表与详情不暴露 NAS 路径。
- 路径失效文件拒绝清晰。
- OpenAPI 与审计检查通过。

## 8. M3G-8 主专项结果

`check-m3g8-object-first-read-fallback.sh` 通过，`PASS=7 FAIL=0`。

已确认：

- 读取策略接口返回对象优先和 fallback 显式状态。
- `OBJECT_STORED` 文件访问票据标记 `OBJECT_STORAGE` 且未 fallback。
- 对象存储读取响应头明确标记 `readSource / fallback`。
- `NAS_ONLY` 文件仍可受控读取且标记 `LEGACY_NAS`。
- 对象副本不可读时返回异常语义，不静默回退 NAS。
- 交付包导出预检查返回 `storageStatus / readSource`。

## 9. 其他回归结果

- M3G-6S-F1：通过，`PASS=14 FAIL=0`。本轮初始检查发现 105 有 `verified=2926 governance=2`，受控修复循环后变为 `verified=2928 governance=0`，最终确认 active object versions 可读、105 覆盖完整、active object version 未重复污染、file-access 可读、NAS 原文件抽样 size / mtime 未变化。
- M3G-6T：通过，`PASS=30 FAIL=0`。工程树草案仍不覆盖正式树，文件管理跳转字段安全，模型 / 图纸缺口保持 catalog-only，交付候选仍为 dry-run / confirmed 护栏。

## 10. 禁出字段与安全边界

本轮脚本内 forbidden-field scan 均通过，未发现以下敏感内容泄露：

- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `storageUri`
- `object_key`
- `objectKey`
- bucket 真值
- SQL / raw row
- token / secret / password

未发现：

- 真实 NAS 文件移动、删除、重命名或覆盖。
- 样本不足时强行执行无边界迁移。
- 对象读取失败静默 fallback 到 NAS 冒充成功。
- PDF / Office / DWG / RVT / IFC 正文读取。
- parser / writer / indexing / Hermes memory 写入。

## 11. Git 跟踪与范围检查

- `scripts/dev/check-m3g7r-all-project-objectification-run.sh` 已纳入 Git 跟踪，当前显示 `AM`，不是未跟踪 `??`。
- `git diff --check` 通过。
- `git diff --cached --check` 通过。
- 未发现 `docs/**` 变更。
- 非交付未跟踪项 `.claude/**`、`CLAUDE.md`、`tmp/**` 仅记录为 P2，不影响本批结论。

## 12. 是否建议主 agent 继续收口 M3G-8

建议继续收口 M3G-8。

上一轮阻塞 M3G-8 的唯一 P1 是 M3G-7R 回归脚本不适配饱和环境；本轮已复验脚本默认通过，并且没有以跳过核心断言或执行无边界迁移来掩盖问题。
