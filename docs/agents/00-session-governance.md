# 三会话协作规则

## 0. 会话模式

本项目采用三个长期独立会话协作：

- 主 agent 会话
- 开发 agent 会话
- 测试 agent 会话

主 agent 不通过“创建子 agent”的方式执行开发或测试任务。原因是子 agent 完成任务后上下文会关闭，不适合承载长期项目记忆。

正确方式：

1. 主 agent 维护需求、架构、任务 prompt 和交接文件。
2. 用户单独打开开发 agent 会话，把 `handoff/dev-agent/current-prompt.md` 作为入口。
3. 用户单独打开测试 agent 会话，把 `handoff/test-agent/current-prompt.md` 作为入口。
4. 开发 agent 和测试 agent 在各自长期会话中持续保留上下文。
5. 所有跨会话交接都通过 `handoff/` 目录沉淀，不依赖某一次聊天记录。

## 1. 角色定义

### 主 agent

职责：

- 维护需求、架构和实施文档
- 冻结任务范围和验收口径
- 编写开发 agent 与测试 agent 的 prompt
- 评审代码结果与测试结果
- 控制范围，避免跑偏

### 开发 agent

职责：

- 仅按主 agent 的任务 prompt 实现代码
- 仅修改被授权的目录
- 每次交付必须附变更说明、自测结果和未完成项

### 测试 agent

职责：

- 按验收口径执行功能测试、接口测试、回归测试
- 输出结构化缺陷与阻塞项
- 不擅自改业务代码

## 2. 写集边界

### 主 agent

默认写集：

- `docs/**`
- 根目录协作文件

### 开发 agent

默认写集：

- `backend/**`
- `frontend/**`
- `infra/**`
- `scripts/**`

除非主 agent 明确授权，否则开发 agent 不修改：

- `docs/**`

### 测试 agent

默认写集：

- 无

如需补测试资产，必须经主 agent 授权后修改：

- `backend/**/test/**`
- `frontend/**/__tests__/**`
- `scripts/**`

## 3. 通用交付格式

### 开发 agent 交付格式

开发 agent 每轮完成后，把报告写入：

- `handoff/dev-agent/latest-report.md`

同时在自己的会话中给出同样内容。

1. 修改文件
2. 新增或变更接口
3. 自测步骤与结果
4. 未完成项
5. 风险或需主 agent 裁决事项

### 测试 agent 交付格式

测试 agent 每轮完成后，把报告写入：

- `handoff/test-agent/latest-report.md`

同时在自己的会话中给出同样内容。

1. 测试范围
2. 执行环境
3. 用例结果
4. 缺陷列表
5. 阻塞项
6. 回归建议

## 4. 任务升级规则

以下情况必须回报主 agent，不得自行扩 scope：

- 需要跨模块修改
- 需要新增核心实体
- 需要改变 REST 契约
- 需要改变权限模型
- 需要改变数据库主约束

## 5. 第一阶段协作原则

- 先搭骨架，再做业务
- 先稳定 `core`，再进入 `master-data`
- 先确保可启动、可登录、可切项目、可审计，再追求页面丰富度
- 测试优先盯底座稳定性，不急着做花哨 UI 验证
