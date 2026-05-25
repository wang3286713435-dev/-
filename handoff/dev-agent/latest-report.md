# 开发 Agent 报告：M2J 105 工程树人工复核与批量调整体验

时间：2026-05-25 CST

## 1. 本轮目标

本轮按 `M2J：105 工程树人工复核与批量调整体验` 执行。

目标是在 M2I 已完成“105 项目 2928 个文件全部进入工程树归属体系”的基础上，让员工可以在平台中查看节点文件、批量选择、批量改归属类型、批量移动工程节点、批量确认 / 驳回归属，并从文件管理器快速跳到所属工程节点。

本轮不移动、删除、重命名、上传或读取真实 NAS 文件；不接 Hermes 写库；不做 BIM/parser/indexing；不改变正式交付挂接规则。

## 2. 改动文件列表

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/ownership/FileOwnershipController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/ownership/FileOwnershipApplicationService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/FileOwnershipTreePanel.vue`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `scripts/dev/check-m2j-105-ownership-review.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`，未新增数据库迁移。

## 3. 后端实现

- 扩展 `GET /api/data-steward/projects/{projectId}/file-ownership/files`：
  - 新增 `ownershipType`
  - 新增 `reviewOnly`
  - 保留 `nodePath / status / page / pageSize`
- 新增批量复核接口：
  - `PUT /api/data-steward/projects/{projectId}/file-ownership/assignments:review`
- 请求字段：
  - `confirmed=true`
  - `fileIds`
  - `action=CONFIRM|REJECT|UPDATE_TYPE|MOVE_NODE|UPDATE_NODE_AND_TYPE`
  - 可选 `ownershipType / nodeKey / nodeLabel / nodePath / reason`
- 写入规则：
  - 必须 `confirmed=true`
  - 必须有项目写权限，沿用 `DELIVERY_ENGINEER / PROJECT_ADMIN`
  - 批量确认、驳回、改类型、移动节点均写入审计
  - 仅更新 `data_file_ownership_assignments` 目录级归属元数据
  - 不返回真实 NAS 路径、`storage_path`、`storage_uri`

## 4. 前端实现

- `FileOwnershipTreePanel.vue` 从 M2I 查看页升级为 M2J 人工复核页：
  - 左侧工程树保持节点浏览
  - 右侧节点文件表支持多选
  - 增加状态筛选、资料类型筛选、仅看待复核
  - 工具栏支持批量确认、批量驳回、批量改归属类型、批量移到其他工程节点
  - 批量写操作均弹出人工确认文案，明确“不移动、不删除、不重命名、不读取 NAS 文件”
- 资料类型支持：
  - 正式交付资料
  - 过程资料
  - 模型资料
  - 图纸收发
  - 参考归档
  - 待判定
- `AssetProjectFileBrowser.vue` 中“归属节点”改为可点击入口：
  - 已登记文件点击归属节点后跳转到工程树页并聚焦对应节点
  - 未登记文件仍显示“需扫描入库后治理”，不伪造归属入口
- `AssetProjectDetailPage.vue` 增加 `ownershipNode` 查询参数联动：
  - 支持从文件管理器跳到工程树节点
  - 保留项目上下文和旧页面结构

## 5. 审计与安全边界

- 新增审计动作：
  - `data.file-ownership.review-batch`
- 审计内容只记录动作、请求数量、更新数量、跳过数量、失败数量。
- 本轮未触碰真实 NAS 文件：
  - 未移动
  - 未删除
  - 未重命名
  - 未上传
  - 未读取文件正文
- 未暴露真实 NAS 路径、`/Volumes`、`smb://`、`nas://`、`storage_path`、`storage_uri`。
- 未让 Hermes 自动写库、自动挂接、自动审批或自动治理。
- 未把 105 全部文件标记成正式交付资料；正式交付仍由原有文档/图纸交付流程驱动。

## 6. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M2J 专项脚本                                                  PASS=6 FAIL=0
M2I 105 文件归属治理回归                                     PASS=8 FAIL=0
M2H-F1 真实 NAS 目录完整性回归                               PASS=11 FAIL=0
M2H Windows 文件管理器回归                                   PASS=53 FAIL=0
M2F 真实项目交付闭环回归                                     PASS=6 FAIL=0
Phase2 batch4 文件访问安全回归                               PASS=18 FAIL=0
git diff --check                                             PASS
```

执行命令：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2j-105-ownership-review.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-f1-real-nas-directory-completeness.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-m2f-real-project-delivery-loop.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 7. 已知风险与未完成事项

- M2J 当前复核对象是平台归属元数据，不等于正式交付挂接；后续如要把“人工确认的正式交付资料”转成应交项，需要单独设计，不应在本轮隐式完成。
- 批量移动节点使用现有工程树节点列表，不允许手写任意 NAS 路径；如果后续需要新建工程节点，应走工程主数据流程。
- 浏览器人工验收仍建议重点检查：
  - 节点文件多选
  - 批量确认 / 驳回
  - 批量改类型
  - 批量移动节点
  - 文件管理器归属节点跳转

## 8. 服务状态

- 后端已重启到本轮最新构建，运行在 `http://127.0.0.1:8080`
- 前端 dev server 保持运行，地址为 `http://127.0.0.1:5173`
