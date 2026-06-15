# Post-UX4 待办：项目生命周期管理

状态：已完成，`PLM-1：项目生命周期管理 MVP` 已通过测试并由主 agent 收口
插入时机：`UX4` 已由用户确认收口后启动。
当前结果：已作为独立功能批次实现；未混入 UX4；未修改 `docs/**`；未扩展成完整 PLM 系统。

## 1. 背景

当前资产总览页已有“新建项目”入口，但现阶段只是提示，不会真正创建项目。

用户希望后续补齐：

- 添加项目。
- 删除项目。
- 删除项目需要二次确认。
- 仅超级管理员可删除项目。
- 添加项目必须通过对象存储管理。
- 创建项目时同步创建真实对象存储工作区和工程树根目录。

## 2. 推荐批次名称

`PLM-1：项目生命周期管理 MVP`

说明：`PLM` 指 Project Lifecycle Management，即项目从创建、授权、进入工作台，到归档/删除的生命周期管理。

## 3. 建议实现边界

### 添加项目

新增项目不是只写一条前端记录，而应完成一条真实项目初始化链：

```text
超级管理员创建项目
-> 写入 core_projects 项目台账
-> 创建对象存储项目工作区前缀
-> 创建工程树根节点
-> 给创建者授予 PROJECT_ADMIN
-> 写审计日志
-> 项目启动台可见
```

对象存储中的“目录”不是 NAS 那种真实文件夹，而是对象 key 前缀。建议平台创建一个受控占位对象，例如：

```text
projects/{projectUuid}/.keep
```

前端不得展示 bucket、object key、storage_uri 或真实 NAS 路径。

### 删除项目

删除项目第一阶段应做安全归档/软删除，不物理删除 MinIO 对象或 NAS 原文件。

建议流程：

```text
超级管理员发起删除
-> 前端二次确认，要求输入项目编码或项目名称
-> 后端校验 confirmed=true 和 confirmProjectCode
-> 项目标记为 ARCHIVED / DELETED
-> 默认项目列表隐藏
-> 项目授权失效或不再进入默认工作流
-> 写审计日志
-> 对象存储文件保留
```

物理清空对象存储应作为后续独立危险操作，不进入本批。

## 4. 建议接口

### 创建项目

```text
POST /api/data-steward/projects:lifecycle-create
```

建议请求字段：

- projectCode
- projectName
- industryType
- projectStage
- projectManagerName
- ownerOrgName

建议响应字段：

- projectId
- projectCode
- projectName
- storageWorkspaceStatus
- sectionRootNodeId
- grantedRole

### 归档/删除项目

```text
POST /api/data-steward/projects/{projectId}:archive
```

建议请求字段：

- confirmed
- confirmProjectCode
- reason

确认字段不匹配时必须拒绝。

## 5. 前端入口

- 资产总览页“新建项目”改为真实创建弹窗。
- 项目列表行内更多菜单增加“归档项目”。
- 只有超级管理员能看到归档/删除入口。
- 普通项目管理员、交付工程师、查看者看不到入口，后端也必须拒绝。
- 删除确认弹窗必须清楚提示：当前只是从平台工作流归档项目，不物理删除对象存储文件。

## 6. 验收标准

1. 只有 `admin` 超级管理员可创建真实项目工作区。
2. 新项目创建后，项目启动台可见。
3. 新项目创建后，对象存储工作区状态可用。
4. 新项目创建后，工程树存在根节点。
5. 创建者获得该项目 `PROJECT_ADMIN` 权限。
6. 删除/归档项目必须二次确认。
7. 非超级管理员删除项目返回无权限。
8. 删除/归档后，项目不再出现在默认项目列表。
9. 删除/归档不物理删除 MinIO 对象或 NAS 原文件。
10. 所有创建、归档操作写审计日志。
11. API 和前端不得泄露 bucket、object key、storage_uri、真实 NAS 路径、token、secret。
12. 前后端构建、健康检查、file-access、对象存储读取回归通过。

## 7. 暂不做

- 不做物理删除对象存储文件。
- 不做真实 NAS 项目目录删除。
- 不做批量删除项目。
- 不做跨租户组织架构。
- 不做客户级项目模板市场。
- 不修改 UX4 当前前端修复目标。
