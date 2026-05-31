# 葛兰岱尔轻量化引擎接口交接模板

> 用途：发给葛兰岱尔引擎团队填写。请不要在本文档中填写真实密钥、token、密码。

## 1. 服务信息

- 引擎服务名称：
- 引擎版本：
- 内网 base URL：
- health API：
- API 文档地址：
- 技术联系人：
- 支持时间窗口：

## 2. 认证与访问控制

- 认证方式：
- Header 名称：
- token / key 获取方式：
- token 有效期：
- 是否支持服务账号：
- 是否需要平台服务器 IP 白名单：
- 是否支持 HTTPS：
- 备注：

请通过安全渠道交付密钥，不要写在聊天、Git、handoff 文档里。

## 3. 健康检查 API

### Request

```bash
curl -fsS '<ENGINE_BASE_URL>/<HEALTH_PATH>'
```

### Response 示例

```json
{
  "status": "ok",
  "version": "example"
}
```

## 4. 提交转换任务 API

### Request

- method：
- path：
- content type：

```json
{
  "externalTaskId": "platform-task-id",
  "fileUrl": "platform short-lived download url",
  "fileName": "sample.rvt",
  "fileFormat": "RVT",
  "fileSizeBytes": 0,
  "checksum": "sha256-or-md5",
  "callbackUrl": "optional platform callback url"
}
```

### Response 示例

```json
{
  "taskId": "engine-task-id",
  "status": "queued"
}
```

## 5. 查询任务状态 API

### Request

- method：
- path：

```bash
curl -fsS '<ENGINE_BASE_URL>/<TASK_STATUS_PATH>/<TASK_ID>'
```

### Response 示例

```json
{
  "taskId": "engine-task-id",
  "status": "converting",
  "progress": 30,
  "errorCode": "",
  "errorMessage": ""
}
```

请填写完整状态枚举：

- queued：
- downloading：
- converting：
- ready：
- failed：
- canceled：
- 其他：

## 6. Viewer 入口 / Session API

### Request

- method：
- path：
- 是否支持 iframe：
- viewer token 有效期：

```json
{
  "taskId": "engine-task-id"
}
```

### Response 示例

```json
{
  "viewerUrl": "https://example/viewer/session",
  "expiresAt": "2026-05-27T12:00:00+08:00"
}
```

## 7. 回调 API

- 是否支持平台 callback：
- 回调签名方式：
- 回调失败重试策略：

### Callback 示例

```json
{
  "taskId": "engine-task-id",
  "externalTaskId": "platform-task-id",
  "status": "ready",
  "progress": 100,
  "viewerSessionId": "optional",
  "errorCode": "",
  "errorMessage": ""
}
```

## 8. 支持格式与限制

| 格式 | 是否支持 | 版本限制 | 最大文件 | 备注 |
| --- | --- | --- | --- | --- |
| RVT | 待填写 | 待填写 | 待填写 | PoC 优先 |
| IFC | 待填写 | 待填写 | 待填写 | 后续 |
| NWD/NWC | 待填写 | 待填写 | 待填写 | 后续 |
| DWG | 待填写 | 待填写 | 待填写 | 后续 |
| GLB/GLTF | 待填写 | 待填写 | 待填写 | 输出或输入待确认 |
| 3D Tiles | 待填写 | 待填写 | 待填写 | 输出或输入待确认 |

## 9. 并发、超时与错误码

- 最大并发转换数：
- 单任务建议超时：
- 下载超时：
- 转换失败是否可重试：
- 相同文件重复提交是否幂等：

| 错误码 | 含义 | 平台建议展示 |
| --- | --- | --- |
| 待填写 | 待填写 | 待填写 |

## 10. 产物与生命周期

- 产物由引擎保存还是平台保存：
- 产物保留时长：
- 是否提供清理 API：
- 是否支持重新转换：
- 是否支持查看转换日志：

## 11. 平台侧安全要求确认

请引擎团队确认：

- [ ] 引擎不直接访问 NAS 共享目录。
- [ ] 引擎不直接访问 MinIO 底层对象目录。
- [ ] 引擎只通过平台短时授权链接 / 票据取模型文件。
- [ ] 引擎不会在日志中输出完整文件下载 URL、token 或敏感路径。
- [ ] viewer URL / session 可设置过期时间。
- [ ] 引擎错误响应不会回显真实平台存储路径。

