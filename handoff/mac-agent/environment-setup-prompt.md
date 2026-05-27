# 新 Mac 环境补齐 Prompt

本文档供后续接手人在新 Mac 上快速补齐卓羽智能数据中台的原生开发环境。

## 1. 当前目标

让新 Mac 具备“原生继续开发条件”，即：

- JDK 21 可用。
- Maven Wrapper 可原生构建后端。
- Docker 仅作为 MySQL/Redis/MinIO 基础设施，不再是唯一可用的后端运行方式。
- 前端可通过 `corepack pnpm` 安装、构建和启动。

## 2. 必备工具

检查：

```bash
xcode-select -p
git --version
docker --version
node -v
corepack --version
java -version
/usr/libexec/java_home -V
```

推荐版本：

- macOS Command Line Tools 已安装。
- Docker Desktop 可用。
- Node.js 20 LTS 或兼容版本。
- JDK 21。

## 3. JDK 21 安装

优先使用 Homebrew：

```bash
brew install --cask temurin@21
```

如果没有 Homebrew，也可以把 Temurin JDK 21 安装到用户级目录：

```bash
mkdir -p "$HOME/Library/Java/JavaVirtualMachines"
```

安装后配置：

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
echo "$JAVA_HOME"
```

建议写入 `~/.zshrc`：

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null)"
if [ -n "$JAVA_HOME" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
```

## 4. 项目启动顺序

仓库根目录：

```bash
bash scripts/dev/bootstrap-infra.sh
```

后端：

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
./mvnw -v
./mvnw -pl delivery-app -am -DskipTests package
cd ..
bash scripts/dev/start-backend.sh
```

健康检查：

```bash
curl -fsS http://localhost:8080/actuator/health
```

前端：

```bash
cd frontend
corepack pnpm install
corepack pnpm build
corepack pnpm dev --host 0.0.0.0
```

## 5. 通过标准

- `java -version` 显示 21。
- `/usr/libexec/java_home -V` 能列出 JDK 21。
- `./mvnw -v` 使用 JDK 21。
- `./mvnw -pl delivery-app -am -DskipTests package` 成功。
- `bash scripts/dev/start-backend.sh` 可原生启动后端。
- `curl -fsS http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`。
