# 开发 Agent 报告：M3D 真实 NAS 小范围灰度镜像

时间：2026-05-26 CST

## 1. 本轮目标

本轮按 `M3D：真实 NAS 小范围灰度镜像` 执行。

目标是在 M3C 任务中心已收口的基础上，选择 105 / projectId=503 的少量真实业务文件，执行受控对象存储镜像灰度，验证真实 NAS 源文件读取、checksum、MinIO 镜像、对象版本、storage-status、file-access、幂等和禁出字段扫描。

本轮不是全量 NAS 搬迁，不做目录一键迁移，不做 parser / indexing / BIM 轻量化，不做 Hermes 正文问答。

## 2. 改动文件清单

- `scripts/dev/check-m3d-real-nas-object-mirror-gray.sh`
- `handoff/dev-agent/latest-report.md`

本轮未修改后端业务代码，未修改前端业务页面，未新增数据库迁移，未修改仓库 `docs/**`。

## 3. 是否复用 M3C

是。

M3D 复用了 M3C 已有能力：

- `POST /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/storage-migration-tasks/{taskId}`
- `GET /api/data-steward/assets/files/{fileId}/storage-status`
- `POST /api/data-steward/assets/files/{fileId}/access-tickets`

现有 M3C 能力已经足够完成真实 NAS 小范围灰度，因此本轮只新增 M3D 专项脚本，没有新增危险全量执行接口。

## 4. 灰度样本选择规则

脚本只从 105 / projectId=503 中显式选择少量真实业务文件：

- 项目必须为 503。
- 文件必须未删除。
- 文件必须有 NAS 存储引用。
- 本地 NAS 源文件必须存在且可读。
- 文件大小必须大于 0 且不超过 10MB。
- 覆盖 PDF、DWG、RVT / 模型类；如果没有合格模型文件则记录 `SKIPPED_NO_ELIGIBLE_MODEL_SAMPLE`，不把模型缺失作为 P0。

本轮实际覆盖：

| 类型 | fileId | assetUuid | 大小 | 结果 |
| --- | ---: | --- | ---: | --- |
| PDF | 993 | `72f19096-58b2-11f1-aae9-1e851063bccc` | 1452359 | `OBJECT_STORED` |
| DWG | 935 | `72f0e8c1-58b2-11f1-aae9-1e851063bccc` | 291104 | `OBJECT_STORED` |
| RVT / MODEL | 1257 | `72f4838a-58b2-11f1-aae9-1e851063bccc` | 10014720 | `OBJECT_STORED` |

未在报告中记录或暴露真实 NAS 路径。

## 5. 灰度执行结果

首次真实灰度执行：

- 灰度任务：`taskId=57`
- 成功数：3
- 失败数：0
- 跳过数：0
- 覆盖类型：PDF / DWG / RVT
- checksum 覆盖率：3/3

脚本修正后复跑验证：

- 验证任务：`taskId=59`
- 成功数：0
- 跳过数：3
- 失败数：0
- 原因：三份样本已由首次灰度进入 `OBJECT_STORED`，复跑按 M3C 幂等策略返回 `ALREADY_STORED` / `SKIPPED`。

幂等复验：

- 复跑任务：`taskId=60`
- 全部样本再次跳过。
- active 对象版本数量未重复污染。

## 6. OBJECT_STORED 与 file-access 验证

三份真实样本均已验证：

- `storage-status.storageState = OBJECT_STORED`
- `storage-status.objectStored = true`
- `checksumAvailable = true`
- 受控 `DOWNLOAD` access ticket 可读取对象镜像。
- API 响应未返回底层路径或对象定位。

## 7. NAS 原文件保护

脚本在迁移前后对真实 NAS 源文件做了只读校验：

- 原文件仍存在。
- 原文件仍可读。
- size 未变化。
- mtime 未变化。

本轮没有移动、删除、改名、覆盖或写入真实 NAS 文件。

## 8. 禁出字段扫描

M3D 专项脚本对迁移创建、任务详情、storage-status、access ticket、幂等复跑响应执行 forbidden-field scan。

未发现以下真值泄露：

- `/Volumes`
- `/Users`
- `nas://`
- `smb://`
- `storage_path`
- `storage_uri`
- `storagePath`
- `storageUri`
- `bucket`
- `object_key`
- `objectKey`
- raw DB row
- SQL
- secret / password / token

说明：脚本内部为了选择真实样本会读取数据库中的存储引用并在本机做只读存在性校验，但不输出真实路径，不通过 API 或前端暴露。

## 9. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3D 真实 NAS 小范围灰度镜像专项                              PASS=19 FAIL=0
M3C 对象存储迁移任务中心回归                                 PASS=9 FAIL=0
M3C-1 asset UUID / storage status 回归                        PASS=15 FAIL=0
M3B 对象存储镜像迁移回归                                     PASS=11 FAIL=0
M3A StorageService 回归                                      PASS=8 FAIL=0
M2J 105 归属复核回归                                         PASS=6 FAIL=0
M2I 105 文件归属治理回归                                     PASS=8 FAIL=0
M2H Windows 文件管理器回归                                   PASS=53 FAIL=0
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
bash scripts/dev/check-m3d-real-nas-object-mirror-gray.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3c1-asset-uuid-storage-status.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m2j-105-ownership-review.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 10. Git 跟踪状态

新增专项脚本已纳入 Git 跟踪：

- `scripts/dev/check-m3d-real-nas-object-mirror-gray.sh`

未把 `.claude/**`、`CLAUDE.md`、`tmp/**` 纳入本轮交付。

## 11. 服务状态

- 后端运行中：`http://127.0.0.1:8080`
- 前端 dev server 运行中：`http://127.0.0.1:5173`

按用户要求，本轮完成后未关闭项目服务。

## 12. 已知风险与 M3E 建议

- M3D 当前仍是同步小范围灰度，适合验证真实链路；如果后续扩大范围，需要异步 worker、速率限制、失败队列和灰度白名单。
- 当前 file-access 已能读取对象镜像，但预览 / 转换产物尚未对象化。
- M3E 建议进入 `预览与转换产物对象化`：先做 PDF / 图片 / Office / CAD / BIM 产物关系和受控访问，不要直接进入 Hermes 正文问答。
