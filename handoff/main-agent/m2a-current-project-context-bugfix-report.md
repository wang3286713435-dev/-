# M2A 真实使用缺陷修复：新增文件夹误报请先切换到当前项目

## 1. 问题

用户在真实使用 M2A 文件管理时，点击 `新增文件夹` 后出现：

`请先切换到当前项目`

这是 M2A 收口后暴露出的真实使用缺陷。

## 2. 根因

M2A 受控 NAS 接口位于：

`/api/data-steward/projects/{projectId}/nas/**`

项目文件管理页面位于项目工作台路由：

`/data-steward/assets/{projectId}?tab=files`

这个页面使用路由中的项目 ID 作为项目上下文，不要求把全局 JWT 当前项目先切换到该项目。

但 `ControlledNasController` 在每个接口入口都调用了：

`ProjectContextApplicationService.requireCurrentProject(principal, projectId)`

这会强制要求：

`JWT currentProjectId == 路由 projectId`

因此，当用户有该项目权限、但全局当前项目尚未切到该项目时，后端会误报：

`CORE_PROJECT_CONTEXT_MISMATCH`

用户看到的就是“请先切换到当前项目”。

## 3. 修复

已修改：

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/controller/ControlledNasController.java`

修复方式：

- Controller 层只获取当前登录用户 principal。
- 不再强制 JWT 当前项目必须等于路由项目。
- 项目权限、项目角色、路径映射、写操作权限仍由 `ControlledNasApplicationService` 基于 `projectId` 做服务端校验。

这样既保留了权限安全，又符合项目工作台的路由上下文设计。

## 4. 回归保护

已增强：

- `scripts/dev/check-m2a-controlled-nas-write.sh`

新增断言：

- 管理员登录后，不先切换 JWT 当前项目。
- 直接调用 `/api/data-steward/projects/{projectId}/nas/directories` 创建文件夹。
- 预期成功。

该断言用于防止 M2A 再次退回到“必须先切换全局当前项目”的错误逻辑。

## 5. 验证结果

已执行：

- 后端构建：通过。
- `scripts/dev/check-m2a-controlled-nas-write.sh`：通过。

专项脚本结果：

`PASS=21 FAIL=0`

新增通过项：

`项目工作台路由项目可直接执行 NAS 操作，不要求 JWT 当前项目先切换`

## 6. 安全边界

本次修复没有放松以下安全边界：

- 未授权项目仍不可操作。
- 查看者仍不可写。
- 删除仍只进入隔离区。
- 永久删除仍未开放。
- 跨项目移动仍未开放。
- 前端和 API 响应仍不得泄露真实 NAS 路径。
- Agent / Hermes 仍不能触发 NAS 写操作。

## 7. 结论

该问题已修复。

用户在项目工作台里进入某个项目后，可以直接新建文件夹，不再需要先切换全局当前项目。
