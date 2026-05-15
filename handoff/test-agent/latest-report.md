# 二期批次四正式验收报告：文件预览与下载权限分离最小闭环

## 1. 总结论

结论：**不通过**。

本轮构建、健康检查、专项脚本、回归脚本、过期票据验证基本都通过，但人工复核发现一个明确 `P0`：

- 普通项目用户在 `/data-steward/catalog` 文件详情中仍能直接看到真实 `NAS` 路径。

这违反了本轮验收红线：

- `接口或页面暴露真实 NAS 路径给普通用户`

因此当前**不能收口二期批次四**。

---

## 2. P0 / P1 / P2 列表

### P0

1. **普通项目用户仍可在目录详情和目录详情接口中读取真实 NAS 路径**
   - 复现用户：
     - `phase2.viewer / Viewer@123`
     - `delivery.engineer / Engineer@123`
   - 复现接口：
     - `GET /api/data-steward/catalog/files/74846`
   - 实测返回：
     - `storagePathVisible = true`
     - `storagePath = "nas:///tmp/phase2-batch4-ui-1778812442.pdf"`
     - `storagePathVisibilityReason = "LOCAL_DEV_ADMIN"`
   - 页面复现：
     - `http://127.0.0.1:5173/data-steward/catalog`
     - 以 `phase2.viewer` 登录后打开 `批次四UI验证.pdf` 详情抽屉
     - 页面直接展示 `存储路径: nas:///tmp/phase2-batch4-ui-1778812442.pdf`
   - 判定：
     - 这是普通项目用户真实看到的页面数据，不是仅后台票据 URL。
     - 直接违反本轮目标“**不暴露真实 NAS 路径给普通用户**”。

### P1

无新增 `P1`。

### P2

1. 前端构建仍有既有 `Vite chunk size warning`。

---

## 3. 构建与健康检查结果

### 3.1 已执行

1. `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`
2. `corepack pnpm --dir frontend build`
3. `curl -fsS http://localhost:8080/actuator/health`
4. `git diff --check`

### 3.2 结果

- 后端构建：通过
- 前端构建：通过
- 健康检查：通过
  - 返回：`{"status":"UP"}`
- `git diff --check`：通过

补充：

- 前端构建仍出现既有 `Vite chunk size warning`，已记为 `P2`。

---

## 4. 专项脚本结果

### 4.1 必跑脚本

1. `bash scripts/dev/check-phase2-batch4-file-access.sh`
   - 结果：通过
   - 核心结论：
     - 管理员可预览
     - 管理员可下载
     - 查看者可预览但不可下载
     - 跨项目用户不可预览/下载
     - 路径失效文件错误清晰
     - OpenAPI 包含新增接口
     - 访问、拒绝、失败动作写审计

2. `bash scripts/dev/check-file-preview-shell.sh`
   - 结果：通过
   - 核心结论：
     - PDF 预览外壳可用
     - BIM 预览外壳元数据可用
     - 预览外壳本身不以真实 `storagePath` 作为访问入口

### 4.2 回归脚本

1. `bash scripts/dev/check-phase2-batch1-readonly-catalog.sh`
   - 结果：通过
   - `25/25 PASS`

2. `bash scripts/dev/check-phase2-batch2-standard-delivery.sh`
   - 最终结果：通过
   - `38/38 PASS`

3. `bash scripts/dev/check-phase2-batch3-review-rectification-report.sh`
   - 结果：通过
   - `52/52 PASS`

说明：

- `batch2` 脚本在与 `batch3` 回归并行跑时出现过一次共享样板数据干扰，顺序重跑后通过。
- 以顺序重跑结果为准，本轮未发现批次一/二/三主链回归。

---

## 5. 页面验收结果

### 5.1 `/data-steward/assets/1`

结果：部分通过

已确认：

- 页面可打开
- 页面级横向溢出为 `0`
- 页面正常显示“文件资产 / 扫描任务 / 路径映射”结构

补充说明：

- 当前样板环境下，该页本次打开时显示 `文件总数 0`，所以本页未能完成“真实文件详情抽屉里的预览/下载状态”人工点击验证。
- 对应的文件访问链路人工验证，本轮主要在 `/data-steward/catalog` 页面完成。

### 5.2 `/data-steward/catalog`

结果：**不通过**

管理员视角已确认：

- 页面可打开
- 页面级横向溢出为 `0`
- 文件详情抽屉能展示：
  - 预览状态
  - 访问权限
  - 说明
  - `打开预览`
  - `下载文件`

查看者视角已确认：

- 页面可打开
- 页面级横向溢出为 `0`
- 文件详情抽屉中：
  - `打开预览` 可用
  - `下载文件` 为禁用状态
  - 访问权限文案为：
    - `当前账号可预览文件，但没有下载权限。`

但同时复现到 `P0`：

- 同一查看者详情抽屉里直接显示了真实 `storagePath`
- 页面展示内容：
  - `存储路径 nas:///tmp/phase2-batch4-ui-1778812442.pdf`

因此页面验收整体判定为：**不通过**

---

## 6. 权限分离验证结果

### 6.1 通过项

1. 预览权限与下载权限已分离
   - `phase2.viewer` 可预览
   - `phase2.viewer` 不可下载

2. 跨项目用户不可读取无权文件
   - 专项脚本通过

3. 访问票据到期后不可继续使用
   - 我手动创建了预览票据 `ticketId=18`
   - 将 `data_file_access_tickets.expires_at` 改到过去时间后再次访问
   - 返回：
     - HTTP `403`
     - `code = ASSET_FILE_ACCESS_TICKET_EXPIRED`
     - `message = 访问票据已过期`
   - 数据库中该票据状态也变为 `EXPIRED`

4. 路径失效文件不会假成功
   - 专项脚本已验证
   - 失效路径文件不能创建访问票据，错误码清晰

### 6.2 未通过项

1. 路径可见性与访问权限没有分离彻底
   - 虽然查看者不能下载，但仍能直接看到真实 NAS 路径
   - 这说明“下载受控”已经做到，但“路径隐藏”没有真正做到

---

## 7. NAS 路径隐藏验证结果

结论：**失败**

### 7.1 接口证据

使用 `phase2.viewer` 和 `delivery.engineer` 分别请求：

- `GET /api/data-steward/catalog/files/74846`

两次都返回：

- `storagePathVisible: true`
- `storagePath: "nas:///tmp/phase2-batch4-ui-1778812442.pdf"`
- `storagePathVisibilityReason: "LOCAL_DEV_ADMIN"`

说明：

- 当前并不是“管理员可见、普通用户隐藏”
- 而是“只要有项目权限，普通用户也直接可见”

### 7.2 页面证据

使用 `phase2.viewer` 登录：

- 打开 `http://127.0.0.1:5173/data-steward/catalog`
- 打开 `批次四UI验证.pdf` 详情抽屉

页面直接显示：

- `存储路径 nas:///tmp/phase2-batch4-ui-1778812442.pdf`

### 7.3 判定

这已满足当前 prompt 中的 `P0` 条件：

- `接口或页面暴露真实 NAS 路径给普通用户`

---

## 8. 审计验证结果

结果：通过

专项脚本已验证以下审计动作存在：

- `asset.file.preview.ticket.create`
- `asset.file.download.ticket.create`
- `asset.file.preview.open`
- `asset.file.download.open`
- `asset.file.access.denied`
- `asset.file.access.failed`

我本轮额外 spot check 也确认：

- 新建的 `ticketId=18` 已产生 `asset.file.preview.ticket.create`

当前未发现“预览/下载关键动作无审计”的问题。

---

## 9. 是否可以收口二期批次四

结论：**不可以**。

原因很明确：

- 文件访问票据、预览/下载权限分离、过期失效、审计这几条主链基本已成立；
- 但“普通用户不应看到真实 NAS 路径”这条红线当前仍然失守；
- 该问题同时出现在：
  - 页面详情抽屉
  - 目录详情接口返回

因此当前不建议主 agent 收口二期批次四。

---

## 10. 附注

1. 本轮人工页面验证使用了两条临时 UI 验证文件记录：
   - `74846`
   - `74847`
2. 测试结束后我已将这两条临时记录软删除，并删除 `/tmp` 下对应临时文件，避免继续污染样板环境。
