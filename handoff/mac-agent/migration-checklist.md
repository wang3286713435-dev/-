# 新 Mac 迁移检查清单

## 1. 仓库与远端

- [ ] 当前路径为项目根目录。
- [ ] `git status -sb` 无意外变更。
- [ ] `git fetch --all --prune` 成功。
- [ ] 本地与 `origin/main` 无意外 ahead/behind。
- [ ] `git fsck --full` 无仓库损坏。
- [ ] 无已跟踪文件缺失。
- [ ] 无损坏软链接。

## 2. 文档与 handoff

- [ ] `README.md` 可读且指向当前阶段。
- [ ] `docs/07-complete-delivery-prd.md` 存在。
- [ ] `docs/08-acceptance-and-agent-integration.md` 存在。
- [ ] `handoff/main-agent/status.md` 存在。
- [ ] `handoff/dev-agent/current-prompt.md` 存在。
- [ ] `handoff/test-agent/current-prompt.md` 存在。
- [ ] `handoff/mac-agent/latest-report.md` 存在。
- [ ] 旧机器路径 `/Users/Weishengsu/` 已扫描并处理。

## 3. 工具环境

- [ ] Command Line Tools 可用。
- [ ] Git 可用。
- [ ] Docker Desktop 可用。
- [ ] Node/Corepack 可用。
- [ ] JDK 21 可用。
- [ ] `JAVA_HOME` 指向 JDK 21。

## 4. 构建与启动

- [ ] `cd frontend && corepack pnpm install` 成功。
- [ ] `cd frontend && corepack pnpm build` 成功。
- [ ] `cd backend && ./mvnw -v` 显示 Java 21。
- [ ] `cd backend && ./mvnw -pl delivery-app -am -DskipTests package` 成功。
- [ ] `bash scripts/dev/bootstrap-infra.sh` 成功。
- [ ] `bash scripts/dev/start-backend.sh` 可原生启动。
- [ ] `curl -fsS http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`。

## 5. 业务链路回归

- [ ] `bash scripts/dev/check-minimal-chain.sh http://localhost:8080 platform.admin Admin@123 2` 通过。
- [ ] `bash scripts/dev/check-master-data-chain.sh http://localhost:8080 platform.admin Admin@123 2` 通过。
- [ ] `bash scripts/dev/check-deliverable-standard-chain.sh http://localhost:8080 platform.admin Admin@123 2` 通过。
- [ ] `bash scripts/dev/check-mvp-chain.sh http://localhost:8080 platform.admin Admin@123 2` 通过。

## 6. 迁移完成判断

可认定迁移完成的条件：

- [ ] 不再依赖 Docker 作为唯一后端运行方式。
- [ ] 原生 JDK/Maven Wrapper 构建通过。
- [ ] 原生后端启动通过。
- [ ] 旧路径残留无执行风险。
- [ ] mac-agent 报告已更新。
