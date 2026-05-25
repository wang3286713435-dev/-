# 测试 Agent 报告：M2H-F1 P1 文件夹名称二次点击折叠复验

时间：2026-05-25 CST

## 1. 验收结论

结论：通过。

本轮只复验上一轮 M2H-F1 的 P1：`点击左侧文件夹名称第一次能展开并进入目录，第二次点击同一名称不能折叠`。

复验结果：

- P1 已关闭。
- 文件夹名称点击和三角点击均可控制展开 / 折叠。
- 两种操作方式未再互相打架。
- 当前激活目录本身不再被强制展开到无法合上。
- 进入深层目录后，祖先目录仍保持展开；当前目录自身可手动折叠。
- 项目名包装目录 `105-启航华居项目` 未回归。
- 历史 URL `fileDir=105-启航华居项目` 可正常归一回项目根目录。
- 未发现 raw path 泄露或真实 NAS 写操作。

当前无 P0 / P1。建议主 agent 进入 M2H-F1 收口判断。

## 2. P0 / P1 / P2

P0：无。

P1：无。上一轮“文件夹名称二次点击不能折叠”的 P1 已修复。

P2：

- `scripts/dev/check-m2h-f1-real-nas-directory-completeness.sh` 当前仍是未跟踪文件。该问题不影响本轮 P1 关闭，但建议主 agent 收口提交时纳入 Git，避免后续回归脚本丢失。
- 前端构建仍有既有 Vite chunk size warning，不影响本轮验收。

## 3. 必跑命令结果

```text
corepack pnpm --dir frontend build
结果：PASS，仅既有 Vite chunk size warning

curl -fsS http://127.0.0.1:8080/actuator/health
结果：PASS，{"status":"UP"}

bash scripts/dev/check-m2h-f1-real-nas-directory-completeness.sh
结果：PASS=11 FAIL=0

bash scripts/dev/check-m2h-windows-file-manager.sh
结果：PASS=53 FAIL=0

git diff --check
结果：PASS
```

## 4. 静态只读检查

已检查：

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `frontend/src/modules/data-steward/components/DirectoryTreeNodeItem.vue`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m2h-f1-real-nas-directory-completeness.sh`

确认点：

- `DirectoryTreeNodeItem.vue` 中点击文件夹名称会触发 `toggle-expand`，并继续选择当前目录。
- `AssetProjectFileBrowser.vue` 的 `rememberExpandedAncestors()` 只自动展开祖先目录，不再把当前目录自身强行加入展开集合。
- M2H-F1 专项脚本已包含“选择目录时只自动展开祖先，不把当前目录强行加回展开集合”的断言。

## 5. 浏览器复验结果

页面：

```text
http://127.0.0.1:5173/data-steward/assets/503?tab=files
```

账号：

```text
platform.admin / Admin@123
```

### A. 文件夹名称点击折叠

测试目录：`01_文件收发`

结果：

- 初始状态下可见 `01_文件收发`，左侧三角显示 `展开目录`。
- 第一次点击文件夹名称后，目录展开，三角显示 `折叠目录`，子目录可见：`01_图纸`、`02_模型`、`03_文档`。
- 第二次点击同一文件夹名称后，目录成功折叠，三角显示 `展开目录`，子目录隐藏。
- 第三次点击文件夹名称后，目录再次展开。
- 点击三角可折叠。
- 三角折叠后，再点击文件夹名称可重新展开。

判定：通过。不再出现“只有点三角才能折叠”的问题。

### B. 祖先自动展开

测试路径：`01_文件收发 / 01_图纸`

结果：

- 点击 `01_图纸` 后，URL 进入 `fileDir=01_文件收发/01_图纸`。
- 祖先目录 `01_文件收发` 保持展开。
- 当前目录 `01_图纸` 可继续手动折叠，折叠后祖先仍保持展开。

判定：通过。

### C. 项目名包装目录

历史 URL：

```text
http://127.0.0.1:5173/data-steward/assets/503?tab=files&fileDir=105-%E5%90%AF%E8%88%AA%E5%8D%8E%E5%B1%85%E9%A1%B9%E7%9B%AE
```

结果：

- 页面未白屏。
- URL 自动归一为 `/data-steward/assets/503?tab=files`。
- 文件管理器回到项目根目录口径。
- 左侧未出现假的一级目录 `105-启航华居项目`。
- 根目录仍可见 `00_工作进度` 至 `07_浏览动画`。
- 右侧显示 `当前文件夹：9 个文件夹 / 2 个文件`。

判定：通过。

## 6. 安全边界

未发现：

- 真实 NAS 文件上传、移动、重命名、删除。
- 文件正文读取。
- PDF / Office / DWG / RVT / IFC 内容解析。
- parser / writer / indexing / 向量库能力。
- Hermes 写库、自动挂接、自动治理。
- raw NAS path、`/Volumes`、`smb://`、`nas://`、`storage_path`、`storage_uri`、SQL、raw row、token、secret、password 泄露。
- `docs/**` 改动。

## 7. Git 状态提醒

当前状态：

```text
?? scripts/dev/check-m2h-f1-real-nas-directory-completeness.sh
```

建议收口提交时纳入该脚本；本轮不因该 P2 阻断 P1 关闭。

## 8. 最终建议

M2H-F1 P1 复验通过，当前无 P0 / P1。建议主 agent 进入 M2H-F1 收口判断，并在提交前把 `scripts/dev/check-m2h-f1-real-nas-directory-completeness.sh` 纳入 Git。
