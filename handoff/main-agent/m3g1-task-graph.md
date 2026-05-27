# M3G-1 任务图：NAS 侧 MinIO readiness、对象化盘点与 dry-run

更新时间：2026-05-27

## 批次定位

`M3G-1` 是 `M3G：NAS 侧 MinIO 对象存储接管真实项目文件` 的第一个子批次。

本批只做就绪检查、盘点和 dry-run，不执行真实历史文件对象化迁移。

## 当前结论

- NAS 地址 `192.168.1.181` 已知为现有项目资料 NAS。
- 2026-05-27 非侵入式探测结果：
  - 初次探测：`192.168.1.181:9000/9001` 未发现 MinIO 响应。
  - 用户已在 Synology NAS 上通过 Container Manager 启动 MinIO。
  - 复测结果：`192.168.1.181:9000/minio/health/ready` 可达。
  - MinIO Console `9001` 可达。
  - bucket `zy-datahub-assets-prod` 已创建。
- 平台后端已由用户在本机终端临时注入 NAS 侧 MinIO 环境变量并重启，健康检查为 `UP`。
- M3G-1 已完成开发、P1 修复和测试 agent 复验，当前正式收口。
- 这代表平台具备 NAS 侧 MinIO readiness、全项目对象化盘点和单项目 dry-run 能力；但这仍不等于历史项目文件已经对象化。

## 任务图

- [x] 0. 冻结 M3G-1 边界：只做 readiness / inventory / dry-run。
- [x] 1. 输出开发 agent prompt。
- [x] 2. 输出测试 agent prompt。
- [x] 3. 输出 NAS 侧 MinIO 配置交接清单。
- [x] 4. NAS 侧 MinIO 服务部署完成。
- [x] 5. 平台已临时注入 NAS 侧 MinIO endpoint / access key / bucket 并启动。
- [x] 6. 平台 readiness 能区分本机 MinIO 与 NAS 侧 MinIO。
- [x] 7. 全项目对象化覆盖率盘点 API 可用。
- [x] 8. 单项目对象化 dry-run 计划 API 可用。
- [x] 9. 前端可查看对象存储就绪状态、覆盖率和 dry-run 结果。
- [x] 10. M3G-1 专项脚本通过。
- [x] 11. M3F / M3E / M3C / file-access 回归通过。
- [x] 12. 主 agent 审计通过并收口 M3G-1。

## M3G-1 完成定义

M3G-1 通过时，平台应能回答：

1. 当前对象存储是否是真正的 NAS 侧 MinIO。
2. 当前所有真实项目对象化覆盖率是多少。
3. 如果选择某项目、目录、文件类型、文件大小范围做对象化，会选中多少文件、多少容量、有哪些风险。

M3G-1 不验收：

- 历史文件真实批量迁移。
- 全量 NAS 搬迁。
- Hermes 正文问答。
- documents / chunks / Qdrant / OpenSearch。
- 文件正文读取。
- 真实 BIM 引擎。

## 红线

- 不移动、删除、重命名、覆盖真实 NAS 原项目文件。
- 不把本机 Docker MinIO 说成 NAS 侧 MinIO。
- 不在 API、前端、日志、审计中暴露真实 NAS 路径、bucket、object key、storage URI、SQL、raw row、token、secret。
- 不让 Hermes 直接访问 NAS、MinIO、MySQL 或向量库。
- 不修改 `docs/**`。
