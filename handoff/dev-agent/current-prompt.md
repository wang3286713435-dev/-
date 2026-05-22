# 开发 Agent 当前任务：M2G 真实 NAS 文件管理器灰度完善

你是数字化交付平台开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

## 0. 当前批次

`M2G：真实 NAS 文件管理器灰度完善`

本批目标是把当前平台最核心的文件管理能力打磨成“员工愿意日常使用的文件管理器”。

产品形态：

`类 Windows / macOS 文件管理器 + 工程资料字段`

不是网盘，不是 BIM 引擎，不是 Hermes 自动治理。

## 1. 必须先阅读

- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/m2g-real-nas-file-manager-polish-plan.md`
- `handoff/main-agent/m2b-nas-write-trial-plan.md`
- `handoff/main-agent/lightweight-test-strategy.md`
- `handoff/main-agent/digital-twin-pr-compatibility-check.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/latest-report.md`

## 2. 必查代码

至少检查：

- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/components/DirectoryTreePanel.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/application/ControlledNasApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/controller/ControlledNasController.java`
- `scripts/dev/check-m2b-nas-write-trial.sh`

## 3. 本轮目标

围绕文件管理器完成以下体验和稳定性优化：

1. 新建文件夹不再出现“请先切换到当前项目”这类项目上下文误判。
2. 所有文件管理器 NAS 写操作优先按路由 `projectId` 执行，前端不因全局 currentProject 不一致直接误判。
3. 上传 / 新建 / 重命名 / 移动 / 删除到回收站 / 恢复形成顺畅单项闭环。
4. 操作成功后刷新当前文件列表，必要时刷新目录树。
5. 当前目录可写状态和不可写原因更清楚。
6. 回收站、操作记录、失败原因使用业务语言，不暴露真实 NAS 路径。
7. 移动目标尽量使用目录选择或明确目录输入校验，不让用户盲填真实路径。

## 4. 允许修改范围

允许：

- `frontend/**` 中与文件管理器体验直接相关的修改。
- `backend/delivery-data-steward/**` 中与受控 NAS 文件管理器 bug 直接相关的最小修复。
- `scripts/dev/**` 新增或增强 M2G 专项脚本。
- `handoff/dev-agent/latest-report.md`。

## 5. 禁止事项

禁止：

- 修改 `docs/**`。
- 永久删除作为普通员工能力开放。
- 绕过灰度开关、项目权限、角色权限或审计。
- 直接暴露真实 NAS 绝对路径。
- 读取 PDF / Office / DWG / RVT / IFC 正文。
- 新增 Hermes 自动操作。
- 新增 BIM 轻量化或构件解析。
- 新增 parser / writer / indexing / 向量库。
- 修改数字孪生主线能力。

## 6. 推荐实现重点

### A. 项目上下文修复

- 文件管理页内的 NAS 写操作必须使用路由项目 ID。
- 如果全局 currentProject 与路由项目不一致，不要让前端先报“请先切换到当前项目”。
- 如需提示，只能提示“正在使用当前项目工作台项目执行操作”。
- 后端仍必须按 JWT 和项目权限校验。

### B. 文件管理器操作体验

顶部工具栏建议保留常用操作：

- 上传文件。
- 新建文件夹。
- 刷新。
- 回收站。
- 操作记录。

文件和目录的重命名、移动、删除建议放到“更多操作”或目录操作里，不要把顶部堆满。

当前目录区域建议显示：

- 当前目录。
- 可写 / 不可写。
- 不可写原因。

### C. 操作后刷新

以下操作成功后必须刷新必要视图：

- 上传文件：刷新当前文件列表。
- 新建文件夹：刷新目录树；如合适，可进入新文件夹或保持当前目录但新目录可见。
- 重命名文件：刷新当前文件列表。
- 移动文件：刷新当前文件列表和目标目录相关状态。
- 删除到回收站：刷新当前文件列表和回收站。
- 恢复：刷新目录树、当前文件列表和回收站。
- 重命名 / 移动 / 删除当前目录：当前目录如果失效，应回到上级或项目根目录。

### D. 回收站

- 用户可见命名统一为“回收站”。
- 不显示“隔离区”。
- 不开放永久删除。
- 恢复冲突时给出清楚提示：原位置已有同名文件或文件夹，需要人工处理。

### E. M2G 专项脚本

新增：

`scripts/dev/check-m2g-nas-file-manager-polish.sh`

脚本必须使用隔离测试项目或隔离临时目录，不得写真实业务目录。

至少验证：

1. 默认无灰度配置时写操作被拒绝。
2. 开启灰度后允许目录内：
   - 新建文件夹。
   - 上传文件。
   - 重命名文件。
   - 移动文件。
   - 删除到回收站。
   - 恢复。
3. 目录操作：
   - 新建目录。
   - 重命名目录。
   - 移动目录。
   - 删除到回收站。
   - 恢复。
4. 查看者不能写。
5. 越过允许范围的写操作被拒绝。
6. 操作记录和回收站响应不泄露真实路径。
7. 审计日志有记录。

## 7. 自测要求

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2g-nas-file-manager-polish.sh
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如果你修改了核心文件列表或目录树交互，请手动打开一个真实项目文件管理页做短验：

- 文件管理页能打开。
- 目录树、文件表、工具栏不白屏。
- 按钮状态和当前目录可写提示清楚。
- 不出现页面级横向溢出。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前文件管理器问题审计。
2. 修复了哪些体验和上下文问题。
3. 是否新增 M2G 专项脚本。
4. 上传 / 新建 / 重命名 / 移动 / 删除到回收站 / 恢复各自状态。
5. 是否触碰真实业务目录。
6. 是否泄露真实 NAS 路径。
7. 是否新增 Hermes / BIM / parser / indexing 能力。
8. 自测结果。
9. 已知风险和未做项。

## 9. 完成定义

只有同时满足以下条件，才能标记完成：

- 新建文件夹不再出现项目上下文误判。
- 隔离目录内完整文件管理器写操作链路通过。
- 当前目录可写状态清楚。
- 操作成功后视图能刷新。
- 回收站命名统一且恢复可用。
- 操作记录和失败原因脱敏。
- M2B 和文件访问安全回归通过。
- 未新增 Hermes / BIM / parser / indexing。
