# 开发 Agent 首轮 Prompt

以下内容为主 agent 发给开发 agent 的首轮任务 prompt。

```text
你负责卓羽智能数据中台 v1 第一阶段的基础工程搭建。你不是唯一会修改仓库的人，代码库里还有主 agent 和测试 agent；不要回退他人的改动，只在你被授权的目录内工作，并且如果发现新变更，要基于现状继续实现。

你的任务目标不是实现完整业务，而是完成第一阶段最小可运行骨架，为后续 master-data、data-steward、work-center 开发提供稳定底座。

你只能修改以下目录：
- backend/**
- frontend/**
- infra/**
- scripts/**

不要修改：
- docs/**

必须遵守的基线：
- 后端：Java 21、Spring Boot 3.3.x、Maven 多模块、模块化单体
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router
- 基础设施：MySQL 8、Redis 7、MinIO、本地 docker-compose
- 认证：本地账号密码 + JWT
- 默认部署策略：单租户私有化部署，多项目隔离

必须参考这些文档：
- docs/05-phase1-dev-baseline.md
- docs/06-phase1-backlog-and-readiness.md
- docs/03-architecture-and-system-design.md
- docs/agents/00-session-governance.md

本轮具体任务：

1. 初始化后端 Maven 多模块工程，至少包含：
   - backend/pom.xml
   - backend/delivery-app
   - backend/delivery-shared
   - backend/delivery-core
   - backend/delivery-master-data
   - backend/delivery-data-steward
   - backend/delivery-work-center
   - backend/delivery-visualization-adapter

2. 在 delivery-app 中提供最小 Spring Boot 可启动应用。

3. 在 delivery-core 中落最小公共底座：
   - 健康检查接口
   - 登录接口
   - 刷新 token 接口
   - 当前用户接口
   - 项目切换接口
   - 统一返回结构
   - 全局异常处理
   - traceId 处理

4. 初始化数据库迁移机制：
   - 接入 Flyway
   - 建立最小 core 表结构脚本，至少覆盖用户、角色、项目、用户项目角色关系
   - 写一份样板初始化数据

5. 初始化前端工程，至少包含：
   - 登录页
   - 主框架布局
   - 顶部项目切换
   - 左侧菜单
   - 一个首页占位页
   - 登录态持久化

6. 初始化 infra：
   - docker-compose.yml
   - MySQL、Redis、MinIO 服务
   - 最小环境变量说明

7. 初始化 scripts：
   - 本地启动说明脚本或 README
   - 常用启动命令说明

实现要求：
- 所有接口命名遵循 docs/05-phase1-dev-baseline.md 的 API 规范
- 不要引入与当前阶段无关的复杂框架
- 不要提前实现 master-data、data-steward、work-center 具体业务
- 能跑通一条最小链路：启动后端、启动前端、登录、获取当前用户、切换项目、打开首页

完成后必须按以下格式回报：
1. 修改文件列表
2. 完成了哪些能力
3. 如何启动与验证
4. 还缺什么
5. 风险点
```
