# 开发 Agent 当前任务：M2B 受控 NAS 写操作真实项目灰度试运行与安全开关

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

## 0. 当前路线

M2A 已收口，且 M2A 真实使用 bug 已修复：项目工作台路由项目可直接执行 NAS 操作，不要求 JWT 当前项目先切换。

当前 active 批次：

`M2B：受控 NAS 写操作真实项目灰度试运行与安全开关`

完成承诺固定为：

`<promise>MAINLINE_M2B_NAS_WRITE_TRIAL_COMPLETE</promise>`

本批不是继续扩展完整 NAS CRUD，而是把 M2A 的受控写能力放进真实项目灰度试运行边界中。

## 1. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/m2b-nas-write-trial-plan.md`
5. `handoff/main-agent/m2a-controlled-nas-write-closure.md`
6. `handoff/main-agent/m2a-current-project-context-bugfix-report.md`
7. `handoff/dev-agent/latest-report.md`
8. `handoff/test-agent/latest-report.md`

重点检查：

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/**`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `scripts/dev/check-m2a-controlled-nas-write.sh`

## 2. 本批目标

完成真实项目灰度试运行底座：

1. 项目级 NAS 写操作灰度开关。
2. 项目内允许写入目录范围限制。
3. 可操作角色 / 账号边界。
4. 文件管理页展示灰度状态、可写范围、风险提示和禁用原因。
5. M2A 写接口在执行前检查灰度开关和目录范围。
6. 操作记录和审计继续可查。
7. M2B 专项脚本覆盖开启 / 关闭灰度、范围内允许、范围外拒绝。

## 3. 允许开发

允许：

- 追加 Flyway 迁移增加灰度配置表，或复用现有配置表实现项目级开关。
- 新增只读查询接口返回当前项目 NAS 写灰度状态。
- 在 M2A 既有写接口前增加灰度校验。
- 在文件管理页展示灰度状态和可操作目录范围。
- 新增 `scripts/dev/check-m2b-nas-write-trial.sh`。

## 4. 禁止事项

禁止：

1. 永久物理删除。
2. 跨项目移动。
3. 批量删除 / 批量移动 / 批量重命名。
4. Agent / Hermes / 自动流程触发 NAS 写操作。
5. 前端传入或展示真实 NAS 绝对路径。
6. 文件正文读取。
7. BIM 轻量化、构件解析、索引。
8. UX1 前端视觉专项改造。
9. 为 105 / 503 或某个项目写死逻辑。
10. 修改 `docs/**`，除非主 agent 单独授权。

## 5. 默认策略

如果没有真实项目灰度参数，本批默认实现为：

- 灰度开关默认关闭。
- 未开启灰度时，真实项目页面写按钮禁用，写接口返回清晰业务错误。
- 专项脚本使用临时项目或显式配置的安全目录，不默认操作真实业务目录。
- 只有开启灰度并命中允许相对目录时，才允许执行 M2A 已允许的写操作。

## 6. 建议接口

可按现有风格调整，但至少需要能支持：

- 查询项目 NAS 写灰度状态。
- 管理员开启 / 关闭项目灰度。
- 配置允许写入的项目内相对目录。
- 写操作前服务端校验灰度状态与目录范围。

所有接口必须统一响应、返回 `traceId`、纳入 OpenAPI、不返回真实 NAS 路径。

## 7. 前端要求

文件管理页必须展示：

- 当前项目是否开启真实 NAS 写入灰度。
- 当前允许写入的项目内相对目录。
- 当前用户为什么可以 / 不可以操作。
- 未开启灰度时，写按钮禁用并说明原因。
- 执行写操作前继续保留 M2A 风险确认。

## 8. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-m2a-controlled-nas-write.sh
bash scripts/dev/check-m1f-employee-access-control.sh
bash scripts/dev/check-m1e-file-task-continuity.sh
bash scripts/dev/check-m1d-standard-delivery-loop.sh
bash scripts/dev/check-m1c-real-project-masterdata.sh
git diff --check
```

浏览器自测至少覆盖：

- 真实项目文件管理页灰度关闭状态。
- 灰度关闭时写按钮禁用。
- 灰度开启后，仅允许目录内可以写。
- 允许目录外操作被拒绝。
- 操作记录和隔离区仍不泄露真实 NAS 路径。

## 9. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 Git 分支和基线 commit。
2. 是否确认 M2B 当前 active。
3. 灰度开关设计。
4. 允许目录范围设计。
5. 权限边界。
6. 前端灰度状态展示。
7. 修改文件清单。
8. 新增 / 修改 API 清单。
9. 自测命令结果。
10. P0 / P1 / P2 列表。
11. 是否建议进入 M2B 测试验收。
