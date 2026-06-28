# Gateway-IAM 本地认证闭环 Compose

本目录提供只面向本地开发和真实链路测试的 Compose 编排。它不替代 `deploy/docker/gateway`
中的单 Gateway 部署方案，也不管理生产 Secret。

## 服务

| 服务 | 说明 |
| --- | --- |
| `postgres` | PostgreSQL 17，持久化卷与 healthcheck |
| `redis` | Redis AOF，`maxmemory-policy noeviction` |
| `nacos` | 本地 standalone Nacos |
| `iam` | `synapse-iam-server`，启动后执行 Flyway 和可选 bootstrap |
| `gateway` | `synapse-gateway-platform`，通过 Nacos 发现 IAM |

PostgreSQL、Redis 和 Nacos 默认只暴露在 Compose 网络内，避免占用宿主机常见端口。
Gateway 和 IAM 会分别映射到 `GATEWAY_HOST_PORT` 与 `IAM_HOST_PORT`，默认使用 `18080`
和 `18100`，供脚本从宿主机执行链路请求。

## 使用

先生成本地未跟踪 `.env`：

```bash
scripts/docker/prepare-local-auth-loop-env.sh
```

再执行真实链路测试：

```bash
scripts/test-gateway-iam-auth-loop.sh
```

脚本会在需要时打包 JAR、构建本地镜像、启动 Compose、执行链路测试，并清理包含 Token 的临时文件。

## 安全

- `.env` 已被 Git 忽略，权限应为 `600`。
- `.env.example` 只保留占位符和开发示例，不包含真实密码或 GatewayProof Secret。
- 本目录生成的 Secret 仅用于本机联调，不能复制到 beta/prd。
