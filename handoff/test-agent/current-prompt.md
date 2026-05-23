# 测试 Agent 当前任务：M2H 目录直达子项返工极短验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

## 0. 当前背景

用户在 105 项目文件管理器中发现真实使用问题：

- 左侧文件夹显示不全，只看到 `01`、`03`、`05` 等部分文件夹。
- 根目录右侧混入了不属于根目录的深层文件。
- 文件管理器应像 Windows 资源管理器一样，右侧只显示当前目录的直接子文件夹和直接文件。

开发 agent 已完成 M2H 目录直达子项返工，报告写入：

- `handoff/dev-agent/latest-report.md`

本轮只做极短验收，不做全站巡检。

## 1. 必须先阅读

- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`

## 2. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2h-windows-file-manager.sh
git diff --check
```

建议补跑：

```bash
bash scripts/dev/check-m2g-nas-file-manager-polish.sh
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
```

## 3. 接口极短验收

必须抽查 `catalog/files` 的新旧语义：

1. `directOnly=true` 根目录：
   - 只返回项目根目录直接文件。
   - 不返回 `01/...`、`03/...`、`05/...` 等深层文件。
2. `directoryPath=某子目录&directOnly=true`：
   - 只返回该目录直接文件。
   - 不返回孙目录或更深层文件。
3. 默认 `directOnly=false` 或不传：
   - 旧递归语义仍保留，不能破坏其他目录检索页面。

可以优先信任 `scripts/dev/check-m2h-windows-file-manager.sh` 的隔离数据断言，但报告里需要写清楚脚本结果。

## 4. 浏览器极短验收

打开真实项目文件管理页，例如：

- `http://127.0.0.1:5173/data-steward/assets/503?tab=files`

或用户当前关注的 105 项目对应项目页。

必须验证：

1. 项目根目录右侧只显示根目录直接子文件夹和直接文件。
2. 根目录右侧不再铺满整个项目的深层文件。
3. 进入 `01_文件收发` 或任一真实子目录后，右侧只显示该目录直接子项。
4. 左侧目录树明显完整，不应只剩 `01`、`03`、`05` 等部分顶层目录。
5. 双击文件夹仍进入目录。
6. PDF / 图片受控预览不回归。
7. RVT / IFC / NWD / NWC / GLB / GLTF 等模型文件仍打开“模型预览占位”，不得伪装成真实 3D。
8. 页面不出现横向撑爆、白屏或卡死。

不要在真实业务目录执行上传、重命名、移动、移入回收站等写操作。

## 5. 边界检查

执行：

```bash
git diff --name-only
git status --short
```

本轮允许的交付改动范围：

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/CatalogController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/utils/directoryTree.ts`
- `scripts/dev/check-m2h-windows-file-manager.sh`
- `handoff/**`

如果发现 `docs/**`、数据库迁移、Hermes、真实 BIM、parser、indexing 或文件正文读取相关改动，按影响记 P1 或 P0。

`.claude/**`、`CLAUDE.md`、`tmp/**` 为既有非交付未跟踪项，不直接判失败，但报告中提醒收口时排除。

## 6. 判定标准

P0：

- 真实 NAS 路径泄露。
- 权限绕过。
- 测试在真实业务目录执行写操作。
- 新增真实 BIM 渲染、Hermes、parser、indexing、正文读取能力。

P1：

- 根目录右侧仍显示整个项目深层文件。
- 子目录右侧仍显示孙目录或更深层文件。
- 左侧目录树仍明显不完整。
- `directOnly=true` 破坏旧递归接口兼容。
- M2H 文件夹双击、PDF 预览、模型占位任一主交互回归。
- 后端构建、前端构建、M2H 脚本或 `git diff --check` 失败。

P2：

- 既有 Vite chunk warning。
- 非阻塞文案、间距或交互细节问题。

## 7. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 极短验收结论。
- P0 / P1 / P2。
- 必跑命令结果。
- M2H 脚本结果。
- `directOnly=true` 与旧递归兼容结果。
- 105 / 503 文件管理页浏览器短验结果。
- 是否发现真实路径或敏感字段泄露。
- 是否建议主 agent 收口 M2H 返工。
