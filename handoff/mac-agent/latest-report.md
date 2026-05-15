# Mac 端项目迁移最终收尾报告

执行日期：2026-05-08  
项目路径：`/Users/vc/Documents/数字化交付平台`

## 1. 本轮结论

- JDK 21 已补齐并可用。
- 后端已从 Docker 兜底运行升级为本机原生 JDK + Maven Wrapper 可构建、可启动。
- `bash scripts/dev/start-backend.sh` 已能原生启动后端。
- `curl -fsS http://localhost:8080/actuator/health` 已返回 `{"status":"UP"}`。
- 旧机器路径残留已做最小清理；剩余仅是 mac-agent 交接文件中的诊断命令/检查项文本，无运行风险。
- `handoff/mac-agent` 缺失的交接文件已补齐。
- 当前可正式认定新 Mac 迁移完成，具备原生继续开发条件。

## 2. JDK 21 安装与验证

初始检查：

- `java -version`：失败，提示无法定位 Java Runtime。
- `/usr/libexec/java_home -V`：失败，未发现 Java Runtime。
- `JAVA_HOME`：空。
- `brew --version`：不可用，本机未安装 Homebrew。

处理方式：

- 通过 Adoptium GitHub Release 下载 `OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.11_10.tar.gz`。
- 安装到用户级目录：
  - `/Users/vc/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`
- 已写入：
  - `~/.zshrc`
  - `~/.zprofile`

最终验证：

```text
openjdk version "21.0.11" 2026-04-21 LTS
OpenJDK Runtime Environment Temurin-21.0.11+10
OpenJDK 64-Bit Server VM Temurin-21.0.11+10
```

`/usr/libexec/java_home -V` 可识别：

```text
21.0.11 (arm64) "Eclipse Adoptium" - "OpenJDK 21.0.11"
```

`JAVA_HOME`：

```text
/Users/vc/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
```

## 3. 原生 Maven Wrapper 验证

执行目录：`backend`

命令：

```bash
./mvnw -v
./mvnw -pl delivery-app -am -DskipTests package
```

结果：

- `./mvnw -v` 成功。
- Maven 使用 Java `21.0.11`。
- `./mvnw -pl delivery-app -am -DskipTests package` 成功。
- 8 个 Maven 模块全部 `SUCCESS`。
- `delivery-app` 完成 Spring Boot repackage。

结论：

- Maven Wrapper 无问题。
- JDK/JAVA_HOME 环境已满足原生后端开发。
- 当前不再需要 Docker 作为唯一可用后端构建方式。

## 4. 原生后端启动与健康检查

执行命令：

```bash
bash scripts/dev/start-backend.sh
curl -fsS http://localhost:8080/actuator/health
```

结果：

- `start-backend.sh` 成功走原生 `backend/mvnw`。
- Spring Boot 使用 Java `21.0.11` 启动。
- Tomcat 监听 `8080`。
- Flyway 校验 `V1-V7` 成功。
- 当前 schema 版本：`7`。
- health 返回：

```json
{"status":"UP"}
```

验证结束后已通过 `Ctrl-C` 停止原生后端进程，`8080` 已释放。

## 5. 基础设施状态

本轮复用已启动的 Docker 基础设施：

- `delivery-mysql`：running / healthy，端口 `3306`
- `delivery-redis`：running，端口 `16379`
- `delivery-minio`：running，端口 `19000/19001`

说明：

- Docker 仍用于本地 MySQL/Redis/MinIO。
- 后端应用本身已经可以原生运行。

## 6. 旧机器路径残留扫描与处理

执行过：

```bash
rg -n '/Users/Weishengsu/' .
rg -n '/Users/Weishengsu/' ~/.codex/config.toml
rg -n '/Users/Weishengsu/' handoff docs scripts backend frontend infra 2>/dev/null
```

处理结果：

- `README.md`、`docs/**`、`scripts/README.md`、`frontend/README.md`、`infra/sql/README.md`、`handoff/**` 中会误导后续开发的旧绝对路径已替换为当前路径。
- `~/.codex/config.toml` 未发现旧项目 trust 路径。
- `~/.codex/config.toml` 已存在当前项目：

```toml
[projects."/Users/vc/Documents/数字化交付平台"]
trust_level = "trusted"
```

当前剩余情况：

- `handoff/mac-agent/repository-migration-prompt.md` 和 `handoff/mac-agent/migration-checklist.md` 中保留了旧路径字符串作为“扫描命令/检查项”，这是诊断说明，不是运行路径。
- 不存在会影响构建、启动、脚本或后续 agent 接手的旧机器路径风险。

## 7. 已修正的路径/文档问题

本轮做了以下最小修正：

- 将仓库文档和 handoff 中的旧机器绝对路径更新为当前路径。
- 修复 `docs/README.md` 中重复的 `09-windows-dev-migration.md` 条目。
- 保持业务代码不变。

## 8. mac-agent 交接文件完善结果

已补齐并更新：

- `handoff/mac-agent/environment-setup-prompt.md`
- `handoff/mac-agent/repository-migration-prompt.md`
- `handoff/mac-agent/migration-checklist.md`
- `handoff/mac-agent/latest-report.md`

这些文件已与当前新 Mac 的真实状态对齐，可供后续接手人直接使用。

## 9. 当前是否具备原生继续开发条件

结论：具备。

依据：

- JDK 21 可用。
- `JAVA_HOME` 已配置。
- 原生 Maven Wrapper 构建通过。
- 原生 `start-backend.sh` 启动通过。
- 后端 health 通过。
- Docker 不再是唯一可用后端运行方式。
- 旧路径残留无运行风险。
- mac-agent 交接文件已补齐。

## 10. 剩余阻塞项

无必须由用户手动处理的阻塞项。

非阻塞提醒：

- 本机未安装 Homebrew，但当前 JDK 21 已通过用户级 Temurin 安装补齐，不影响开发。
- Node 当前为 `v22.15.0`，不是文档推荐的 Node 20 LTS；上一轮前端构建已通过，暂不阻塞。
- Docker 仍需用于本地 MySQL/Redis/MinIO。

## 11. 最终迁移结论

可以正式认定迁移完成。

当前新 Mac 已具备数字化交付平台的原生继续开发条件，后续可以按 `handoff/dev-agent/current-prompt.md` 继续推进一期 BIM 资产管理试点。
