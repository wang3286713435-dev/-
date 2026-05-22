# MAIN-0 / M2C 启动与真实 NAS 灰度配置报告

## 1. 结论

- 主线已从 UX3 回到平台本体能力建设。
- 当前 active 批次已切换为：`M2C：交付包与档案目录能力`。
- 项目 `504 / 100 / 深圳市二十八高项目` 已开启真实 NAS 写入灰度。
- 用户可见命名已从 `隔离区` 收束为 `回收站`。

## 2. M2C 交接

已更新：

- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/m2c-delivery-package-archive-directory-plan.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/current-prompt.md`

M2C 目标：

- 生成交付包草案。
- 展示档案目录。
- 导出交付清单。
- 不复制、不移动、不打包真实 NAS 文件。

## 3. 真实 NAS 写入灰度

灰度项目：

- projectId：`504`
- 项目编码：`100`
- 项目名称：`深圳市二十八高项目`

灰度范围：

- 只允许相对目录：`平台试运行区`
- 只允许角色：`PROJECT_ADMIN`
- 只允许账号：`platform.admin`

已通过平台后端接口完成：

- 开启项目级写入灰度。
- 创建 `平台试运行区` 目录。
- 校验该目录下 `canWrite=true`。
- 项目根目录仍不可写，避免全项目放开。

## 4. 回收站命名

已修改用户可见文案：

- 前端文件管理页按钮、抽屉、提示、操作记录。
- 后端受控 NAS 写操作成功 / 失败提示。
- M2A / M2B 脚本输出。

未改动：

- 数据库表名。
- API 路径。
- DTO 字段名。
- 内部状态 `QUARANTINED`。

保留原因：

- 兼容既有接口、脚本、审计和数据表契约。

## 5. 验证结果

已通过：

- 后端构建：`./mvnw -pl delivery-app -am -DskipTests package`
- 前端构建：`corepack pnpm --dir frontend build`
- 健康检查：`http://127.0.0.1:8080/actuator/health -> UP`
- M2B 回归：`PASS=18 FAIL=0`
- M2A 回归：`PASS=21 FAIL=0`
- `git diff --check`

说明：

- 前端构建仍保留既有 Vite chunk size warning，不影响本轮结论。
- 本轮没有复制、移动、删除真实业务目录中的既有文件。
- 本轮只在项目 `504` 的 NAS 根目录下创建了受控灰度目录 `平台试运行区`。

## 6. 后续建议

下一步交给开发 agent 执行 `handoff/dev-agent/current-prompt.md` 中的 M2C。

测试 agent 使用 `handoff/test-agent/current-prompt.md` 做 M2C 验收。
