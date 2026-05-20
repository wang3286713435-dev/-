# M1A 开发报告主 Agent 审计

时间：2026-05-20

## 审计结论

开发 agent 已完成 M1A 主线基线审计和两个路径脱敏 P1 修复，修复方向符合 M1A 范围。

本轮没有新增 Hermes 能力，没有继续 G4，没有进入 8B / 8C / 9A，没有触碰真实 NAS 写操作、正文抽取或真实 BIM 轻量化。

## 分支纠偏

开发 agent 首次执行时误在 `codex/hermes-alignment-0a-contract-freeze` 分支完成改动。

主 agent 已将本轮改动迁移到从 `main` 拉出的平台分支：

`codex/platform-m1a-baseline-fixes`

后续测试 agent 应在该分支执行 M1A 验收。

## 审计通过项

- `CatalogApplicationService` 已移除项目管理员查看 catalog raw path 的分支。
- catalog 文件详情统一按 catalog-only 策略隐藏底层路径。
- `FileResourceApplicationService` 对外响应清空 `storageUri`，同时保留内部 `requireFile(...)` 能力，避免破坏模型集成等内部调用。
- 前端旧文件资源页不再展示“存储地址”，改为“底层路径已隐藏”。
- TypeScript 类型已允许 `storageUri=null`。
- 未修改 `docs/**`。
- `git diff --check` 已通过。

## 需测试重点

测试 agent 需要重点复验：

1. 105 和至少一个非 105 真实 NAS 项目页面/API 可用。
2. catalog list/detail 不泄露 `nas://`、`smb://`、`/Volumes/`、`/Users/`、`storage_path/storage_uri`。
3. catalog detail 返回 `storagePath=null`、`storagePathVisible=false`。
4. file-resources API 返回 `storageUri=null` 或不展示真实值。
5. `/data-steward/files` 页面显示“底层路径已隐藏”。
6. 文件预览、下载权限、模型集成、6A、6B、7A、8A 不回归。

## 当前裁决

可以进入 M1A 测试 agent 验收。

在测试 agent 报告返回前，不建议提交或收口本轮改动。

