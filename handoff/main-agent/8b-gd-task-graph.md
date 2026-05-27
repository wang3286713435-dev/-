# 8B-GD 葛兰岱尔轻量化引擎任务图

更新时间：2026-05-27

```text
[x] 8B-GD0：接口文档评审与握手
    [x] 创建独立 worktree / 分支
    [x] 收集 Station API / Web / Token / 上传 / 查询 / Viewer 文档
    [x] 明确首轮链路：平台后端分片上传 Station API
    [x] 明确安全边界：Station 不直连 NAS / MinIO 底层目录

[ ] 8B-GD1：平台侧葛兰岱尔适配骨架
    [ ] 默认 MOCK，不影响现有 8A
    [ ] 配置项与 GLANDAR provider 开关
    [ ] Station API client 骨架，不执行真实上传
    [ ] lightweight job / viewer ticket 平台接口骨架
    [ ] GLANDAR 未配置或 token 缺失时返回业务化不可用，不 500
    [ ] 响应禁出字段扫描
    [ ] 8A / file-access 回归

[ ] 8B-GD2：105 RVT PoC 转换闭环
    [ ] 选定 105 项目 1-3 个 RVT 样本
    [ ] 平台校验权限
    [ ] 平台通过 StorageService 读取文件流
    [ ] 分片上传 Station SplitUploadFile
    [ ] 保存 lightweightName 映射
    [ ] 查询 Station query-model-info
    [ ] status=100 后生成 viewer ticket
    [ ] 不暴露 token / raw path / bucket / object key

[ ] 8C-GD：Viewer 嵌入与业务联动
    [ ] 前端模型预览页加载 Glendale engine
    [ ] 打开 root.glt
    [ ] 项目上下文保持
    [ ] 基础工具：视角、鼠标模式、剖切、测量
    [ ] 后续构件定位 / 高亮 / 图模联动排期
```

## 当前 active

`8B-GD1：平台侧葛兰岱尔适配骨架`

## 当前关键裁决

- 不等待 M3G 才能做 8B-GD1，但必须在独立 worktree 开发。
- 8B-GD1 不做真实转换，只补平台侧接口骨架和配置边界。
- 不新增数据库迁移，避免与 M3G 主线迁移号冲突；持久化任务表放到 8B-GD2 或等 M3G 收口后再裁决。
- Station Token 不进入前端、handoff、Git、日志。

