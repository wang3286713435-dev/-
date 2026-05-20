# G2-B Hermes 问答超时修复短回归报告

生成时间：2026-05-20 05:42 CST

## 1. 测试结论

结论：通过。

Hermes 前端问答超时 P1：已关闭。

是否建议收口 G2-B：建议收口。

是否建议进入 G2 整体收口和 Git checkpoint：建议主 agent 复核本报告与开发报告一致后，进入 G2 整体收口和 Git checkpoint。

## 2. P0 / P1 / P2

P0：无。

P1：无。上一轮发现的 `timeout of 10000ms exceeded` 已修复，浏览器实测 10 秒以上真实 Hermes 回答不再被前端 10 秒全局超时截断。

P2：无新增。

## 3. 静态检查

结果：通过。

- 全局 Axios 超时仍为 `10000ms`，位置：`frontend/src/app/http.ts`。
- `askHermes()` 单独配置 `timeout: HERMES_CHAT_TIMEOUT_MS`。
- `HERMES_CHAT_TIMEOUT_MS = 45_000`，位置：`frontend/src/modules/data-steward/api/dataSteward.ts`。
- 页面存在等待提示：`真实 Hermes 正在组织回答，可能需要 10-30 秒。平台未执行任何写操作。`
- 页面存在友好超时文案：`真实 Hermes 回答超时，请稍后重试；平台未执行任何写操作。`
- 前端未发现直连 Hermes 外部服务地址；前端仍通过平台后端 `/api/data-steward/chat`。

## 4. 必跑命令结果

- `corepack pnpm --dir frontend build`：通过，仅有既有 Vite chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh`：通过，`PASS=13 FAIL=0`。
- `bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`：通过，`PASS=11 FAIL=0`。
- `git diff --check`：通过。

## 5. 浏览器短回归

页面：`/data-steward/assets/503/work/agent-governance`

结果：通过。

已验证问题：

- `你好`：正常返回真实 Hermes 回答，无 `timeout of 10000ms exceeded`。
- `这个页面是干什么的？`：约 `10.9s` 返回，正常展示真实 Hermes 回答，无超时。
- `我下一步应该做什么？`：约 `16.2s` 返回，正常展示真实 Hermes 回答，无超时。
- `这个项目有哪些已登记文件？`：约 `9.9s` 返回，正常展示真实 Hermes 回答，无超时。

交互状态：

- 提交后页面显示等待提示：`真实 Hermes 正在组织回答，可能需要 10-30 秒。平台未执行任何写操作。`
- 等待期间输入框禁用。
- 等待期间 `问 Hermes` 提交按钮禁用。
- 页面未白屏。
- 页面未出现 500。
- 页面未出现 `timeout of 10000ms exceeded`。

回答来源：

- 页面显示 `Hermes真实回答 已接入`。
- 回答卡片显示真实 Hermes 回答。
- 未发现把平台本地兜底伪装成真实 Hermes 回答。

## 6. Missing Evidence 与安全边界

结果：通过。

补充提问：`请读取这个 RVT 模型里的构件参数、图层、正文和 BIM 构件信息`

实测结果：

- 正文 / DWG / RVT / BIM / 构件类问题返回 Missing Evidence / 缺少证据。
- 未编造正文、图层、构件参数或模型内部内容。
- 响应仍声明 catalog-only。
- 响应仍提示平台未执行写操作。

敏感信息检查：

- 未出现 `/Volumes`。
- 未出现 `smb://`。
- 未出现 `nas://`。
- 未出现 `storage_path` / `storage_uri`。
- 未出现 SQL。
- 未出现 raw row。
- 未出现 token / secret / password。

## 7. 收口建议

上一轮 Hermes 前端超时 P1 已关闭。

建议主 agent 收口 G2-B。

建议主 agent 在确认无其他未合并问题后，进入 G2 整体收口和 Git checkpoint。
