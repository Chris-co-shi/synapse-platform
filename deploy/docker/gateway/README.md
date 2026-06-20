# Synapse Gateway Docker 部署

本目录提供 Linux Server + Docker Engine + Docker Compose v2 的单机或少量服务器部署方案。它不是 Kubernetes 部署，也不提供 Kubernetes 式 RollingUpdate。

## 1. 前置条件

- Linux 服务器，Docker Engine 与 `docker compose` v2。
- 可访问 OCI Registry、Nacos 和 IAM JWK 地址。
- 服务器时间已同步；GatewayProof 时间窗口依赖准确时钟。
- Registry 已完成登录，服务器部署目录包含本目录和 `scripts/docker`。
- beta/prd 使用至少 32 UTF-8 字节的 `GATEWAY_PROOF_SECRET`。

应用无状态，不挂载业务数据、源码、Maven settings 或 Docker socket。日志写入 stdout/stderr。

## 2. 构建镜像

构建机先安装本地 Framework，再由脚本构建 Gateway JAR 和镜像：

```bash
cd ../synapse-framework
mvn clean install
cd ../synapse-platform

./scripts/docker/build-gateway-image.sh \
  registry.example.com/synapse/synapse-gateway \
  0.1.0
```

脚本使用 Gateway 子 POM执行定向 Maven 构建，验证唯一 Spring Boot 可执行 JAR，再交给模块 Dockerfile。也可以手工执行：

```bash
mvn -f synapse-gateway-platform/pom.xml clean package
docker build \
  --platform linux/amd64 \
  --build-arg JAR_FILE=synapse-gateway-platform/target/synapse-gateway-platform-0.1.0-SNAPSHOT.jar \
  -f synapse-gateway-platform/Dockerfile \
  -t synapse-gateway-platform:test .
```

镜像使用 `eclipse-temurin:21-jre-alpine`、固定非 root UID/GID `10001` 和 `/opt/synapse/application.jar`。Alpine 自带 BusyBox `wget`，Compose 用它探测 readiness，无需为健康检查安装 curl。`JAVA_TOOL_OPTIONS` 由 JVM 原生读取；不要把堆大小、时区、代理或调试端口硬编码进镜像。

## 3. CPU 架构

脚本默认 `DOCKER_PLATFORM=linux/amd64`，适配常见 Linux AMD64 服务器。Apple Silicon 本地直接构建常产生 `linux/arm64`，不能部署到只支持 AMD64 的服务器。

```bash
DOCKER_PLATFORM=linux/amd64 ./scripts/docker/build-gateway-image.sh registry.example.com/synapse/synapse-gateway 0.1.0
```

ARM64 服务器可显式设置 `DOCKER_PLATFORM=linux/arm64`。生产 tag 必须明确且不可变，脚本拒绝 `latest`。

## 4. 发布镜像

脚本不处理登录和密码：

```bash
docker login registry.example.com
./scripts/docker/build-gateway-image.sh \
  registry.example.com/synapse/synapse-gateway 0.1.0 --push
```

Registry 凭据不得写入仓库、Dockerfile、构建参数或部署脚本。

## 5. 首次部署

```bash
cp deploy/docker/gateway/.env.example deploy/docker/gateway/.env
chmod 600 deploy/docker/gateway/.env
```

编辑 `.env`，至少配置镜像、Nacos、IAM issuer/JWK、audience 和 GatewayProof。真实 `.env` 已被 `.gitignore`/`.dockerignore` 排除。`GATEWAY_CONTAINER_PORT` 必须与 `SERVER_PORT` 一致。

先检查 Compose：

```bash
docker compose \
  --env-file deploy/docker/gateway/.env \
  -f deploy/docker/gateway/docker-compose.yml \
  config --quiet
```

部署：

```bash
./scripts/docker/deploy-gateway.sh \
  registry.example.com/synapse/synapse-gateway 0.1.0
```

脚本拉取并仅更新 `gateway` 服务，等待容器 `healthy`，成功后输出容器、镜像、健康状态和 UTC 部署时间。

## 6. 健康、日志与停机

- healthcheck：`/actuator/health/readiness`。
- liveness：可通过 `/actuator/health/liveness` 供外部监控使用。
- 启动缓冲：45 秒；间隔 15 秒；超时 5 秒；重试 5 次。
- Spring 优雅停机阶段：30 秒；Compose `stop_grace_period`：40 秒。
- 重启策略：`unless-stopped`。
- 日志：Docker `json-file`，单文件 100 MB，保留 5 个。
- 安全：非 root、`no-new-privileges`、删除全部 Linux capabilities，不使用 privileged。

查看非敏感状态和日志：

```bash
docker compose --env-file deploy/docker/gateway/.env -f deploy/docker/gateway/docker-compose.yml ps
docker logs --tail 100 synapse-gateway
```

日志中不得输出 Token、GatewayProof secret、canonical string 或完整环境变量。

## 7. 更新与自动回滚

再次执行部署脚本即可更新。脚本在 `.release/current` 和 `.release/previous` 中原子保存完整镜像引用，不保存凭据或环境变量。

健康检查失败时：

1. 输出容器的非敏感状态。
2. 如存在部署前镜像，调用回滚脚本执行一次回滚。
3. 返回非零退出码，保留失败镜像用于排查。

脚本不会执行 `docker system prune`、`compose down -v`，不会删除历史镜像或停止其他服务。

## 8. 手工回滚

回滚到记录的 previous：

```bash
./scripts/docker/rollback-gateway.sh
```

回滚到明确 tag 或完整镜像：

```bash
./scripts/docker/rollback-gateway.sh 0.1.0
./scripts/docker/rollback-gateway.sh registry.example.com/synapse/synapse-gateway:0.1.0
```

回滚同样等待 `healthy`，失败时返回非零，不递归回滚，也不删除当前失败镜像。

## 9. 单实例更新限制

Docker Compose 单实例替换容器会产生短暂中断。当前脚本没有伪装成零停机滚动发布。需要无中断更新时，应使用多实例配合上层负载均衡，或在后续任务迁移 Kubernetes Deployment。

## 10. 故障排查

| 现象 | 检查项 |
| --- | --- |
| 镜像拉取失败 | Registry 地址、tag、网络和 `docker login` 是否有效 |
| 容器启动失败 | `docker compose ps`、应用日志、JVM 内存与 CPU 架构 |
| readiness 失败 | `SERVER_PORT` 与容器端口、IAM JWK、Nacos、启动时间 |
| Nacos 不可用 | 地址、namespace、group、用户名密码和网络策略 |
| JWT 全部 401 | issuer、JWK、audience、时间同步和 Token 必填 claim |
| GatewayProof 启动失败 | enabled、gateway-id、至少 32 字节 secret |
| GatewayProof 验签失败 | 两端 secret/id、时钟、StripPrefix 后路径和 query |
| 端口冲突 | `GATEWAY_HOST_PORT` 是否被占用 |
| JVM 被杀死 | Docker/主机 OOM 记录，合理设置 `JAVA_TOOL_OPTIONS` |
| Mac 构建后 Linux 无法运行 | 检查镜像架构，使用 `DOCKER_PLATFORM=linux/amd64` 重建 |

## 11. Kubernetes 迁移映射

| 当前 Docker 配置 | 后续 Kubernetes 对应 |
| --- | --- |
| Docker image/tag | Pod container image |
| `.env` 普通配置 | ConfigMap |
| `.env` 敏感配置 | Secret |
| Compose healthcheck | readinessProbe；liveness 端点映射 livenessProbe |
| `restart` | Deployment controller |
| Compose service/network | Service 与集群网络 |
| 环境变量 | Pod `env` / `envFrom` |
| `stop_grace_period` | `terminationGracePeriodSeconds` |
| stdout/stderr | 容器日志采集 |
| 部署/回滚脚本 | Deployment rollout/rollback |

当前仓库没有 Kubernetes YAML、Helm Chart、Ingress 或 Service 清单。
