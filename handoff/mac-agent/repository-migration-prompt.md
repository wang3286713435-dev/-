# 新 Mac 仓库接收 Prompt

本文档供后续新 Mac Codex 或开发者接收卓羽智能数据中台仓库时使用。

## 1. 接收目标

确认新 Mac 上的仓库与远端同步、文件完整、脚本可执行，并且不会被旧机器绝对路径误导。

## 2. 推荐路径

当前已验证路径：

```text
/Users/vc/Documents/数字化交付平台
```

如果迁移到另一台 Mac，建议使用不含空格、权限清晰的路径。中文项目目录当前已验证可用，但如果后续遇到工具兼容问题，可以迁移到英文路径。

## 3. 仓库检查

```bash
pwd
git rev-parse --show-toplevel
git status -sb
git remote -v
git fetch --all --prune
git rev-list --left-right --count origin/main...HEAD
git fsck --full
git ls-files --deleted
git diff --name-status --diff-filter=D
find . -type l ! -exec test -e {} \; -print
```

通过标准：

- 位于项目根目录。
- 本地与 `origin/main` 无意外 ahead/behind。
- 无已跟踪文件缺失。
- 无损坏软链接。

## 4. 必读文档

```text
README.md
docs/README.md
docs/07-complete-delivery-prd.md
docs/08-acceptance-and-agent-integration.md
handoff/main-agent/status.md
handoff/main-agent/backlog.md
handoff/dev-agent/current-prompt.md
handoff/test-agent/current-prompt.md
handoff/mac-agent/latest-report.md
```

## 5. 旧路径残留检查

```bash
rg -n '/Users/Weishengsu/' .
rg -n '/Users/Weishengsu/' ~/.codex/config.toml
rg -n '/Users/Weishengsu/' handoff docs scripts backend frontend infra 2>/dev/null
```

处理原则：

- 运行文档、README、handoff prompt 中的旧路径必须修正。
- 历史报告中不影响执行的旧路径可以记录，不必大范围清洗。
- `~/.codex/config.toml` 必须包含当前项目路径的 trusted 配置。

## 6. 当前项目阶段

当前项目处于：

```text
一期 BIM 资产管理试点
```

开发入口：

```text
handoff/dev-agent/current-prompt.md
```

测试入口：

```text
handoff/test-agent/current-prompt.md
```

不要回退到旧的 v1 MVP 开发口径。
