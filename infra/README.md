# Infra

本目录提供本地开发依赖环境与基础运行配置。

## 内容

- `docker-compose.yml`: MySQL 8、Redis 7、MinIO
- `.env.example`: 默认环境变量
- `nginx/default.conf`: 反向代理样板配置
- `sql/README.md`: SQL 管理说明

## 本地启动

```bash
cd infra
docker compose --env-file .env.example up -d
```

Windows PowerShell 也可直接使用：

```powershell
.\scripts\dev\bootstrap-infra.ps1
```
