# Agent Handoff 工作台

本目录是卓羽智能数据中台三会话协作的交接区。

重要规则：

- 主 agent 只在这里准备任务、验收口径和交接说明。
- 开发 agent 和测试 agent 必须作为长期独立会话运行，不由主 agent 创建子 agent。
- 每个 agent 只读自己的 `current-prompt.md` 开始工作。
- 每轮完成后，把交付报告写入自己的 `latest-report.md`。
- 不把关键项目上下文只留在聊天记录里。

## 目录

- `main-agent/`：主 agent 状态、派工记录和裁决记录
- `dev-agent/`：开发 agent 当前任务、交付报告模板和结果
- `test-agent/`：测试 agent 当前任务、测试矩阵和结果
- `windows-agent/`：Windows 端 Codex 项目迁移、环境检查和可运行性复核交接区

## 推荐流程

1. 主 agent 更新 `dev-agent/current-prompt.md`。
2. 用户打开开发 agent 长期会话，让它读取该文件并执行。
3. 开发 agent 完成后写入 `dev-agent/latest-report.md`。
4. 主 agent 审阅代码和报告，必要时更新 `test-agent/current-prompt.md`。
5. 用户打开测试 agent 长期会话，让它读取测试任务并执行。
6. 测试 agent 完成后写入 `test-agent/latest-report.md`。
7. 主 agent 根据结果决定返工、验收或进入下一轮。

Windows 端交接流程：

1. 主 agent 更新 `windows-agent/current-prompt.md`。
2. 用户将项目源码迁移到 Windows，不迁移会话记录。
3. 用户在 Windows 端 Codex 打开项目后，让其先阅读 `windows-agent/current-prompt.md`。
4. Windows 端 Codex 完成检查后，把结果写入 `windows-agent/latest-report.md`。
