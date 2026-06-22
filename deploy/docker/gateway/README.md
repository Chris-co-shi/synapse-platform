# Synapse Gateway Docker 部署

本目录提供 Linux Server、Docker Engine 和 Docker Compose v2 的单 Gateway 实例部署方案。它不是
Kubernetes 部署，也不提供 Kubernetes 式滚动更新。

## 1. 部署模型与前置条件

- Linux Server、Docker Engine、Docker Compose v2。
- 构建机使用 Java 21、Maven 3.8.6+、Docker buildx；运行服务器不需要安装 Java。
- 服务器可访问 OCI Registry、Nacos、IAM issuer/JWK，且已完成 Registry 登录。
- 服务器时间同步；GatewayProof 验签窗口依赖准确时钟。
- 根据流量和 `JAVA_TOOL_OPTIONS` 预留足够内存及磁盘。
- Platform 依赖的 Framework 构件已能由 Maven 本地仓库或配置的远程仓库解析。

应用无状态，不挂载源码、Maven settings、SSH key、Registry 凭据或 Docker socket。配置从环境文件注入，
日志写入 stdout/stderr。

## 2. 构建镜像

默认构建并执行 Gateway 测试，目标平台为 `linux/amd64`：

```bash
MVN_BIN=/path/to/mvn \
./scripts/docker/build-gateway-image.sh \
  --repository registry.example.com/synapse/synapse-gateway \
  --tag 0.1.0 \
  --platform linux/amd64
```

脚本执行 `mvn -pl synapse-gateway-platform -am clean package`，确认唯一 Spring Boot 可执行 JAR，复制到
临时最小 Docker context，再执行 buildx。`*.original`、sources、javadoc 和 tests JAR 不会被选中。

只有调用者明确指定时才跳过测试：

```bash
./scripts/docker/build-gateway-image.sh \
  --repository registry.example.com/synapse/synapse-gateway \
  --tag 0.1.0-test \
  --skip-tests
```

脚本会醒目标记跳过测试和 dirty workspace，但不会修改、暂存或提交工作区。

### 2.1 Runtime 镜像

- 基础镜像：`eclipse-temurin:21.0.11_10-jre-jammy`，并固定多架构 manifest digest；Java 21 JRE、Ubuntu Jammy/glibc。
- 非 root 用户：`synapse`，固定 UID/GID `10001`。
- 工作目录：`/opt/synapse`；JAR：`/opt/synapse/application.jar`。
- 入口：exec form `java -jar /opt/synapse/application.jar`。
- 基础镜像自带 `curl`，用于 Compose readiness 探测；不额外安装软件，也不包含 Maven、Git、JDK 或源码。
- OCI labels 包含 title、description、version、revision、created、source。

`JAVA_TOOL_OPTIONS` 由 JVM 原生读取，不在镜像中固定堆、GC、时区、代理或调试参数。

## 3. 多架构与 Push

Apple Silicon 通常为 `linux/arm64`，常见 Linux 服务器为 `linux/amd64`。直接按本机构架构构建可能导致服务器
出现 `exec format error`，应始终明确目标平台。

单平台本地构建使用 buildx `--load`；多平台镜像不能同时 `--load`，必须推送到 Registry：

```bash
./scripts/docker/build-gateway-image.sh \
  --repository registry.example.com/synapse/synapse-gateway \
  --tag 0.1.0 \
  --platform linux/amd64,linux/arm64 \
  --push
```

单平台也可使用 `--push`。脚本不会执行 `docker login`、读取密码或保存 Registry Token；运维人员必须预先登录。
生产 tag 必须变化且不可变，不得重复覆盖或使用 `latest`。

## 4. 首次部署

将仓库中的 `deploy/docker/gateway` 和 `scripts/docker` 保持相对目录复制到服务器，然后：

```bash
cp deploy/docker/gateway/.env.example deploy/docker/gateway/.env
chmod 600 deploy/docker/gateway/.env
```

编辑 `.env`，填写镜像、Nacos、IAM、audience 和 GatewayProof。至少 32 UTF-8 字节的
`GATEWAY_PROOF_SECRET` 必须通过服务器受限文件或后续 Secret 管理系统注入，不得写入镜像、命令参数或 Git。

确认 Registry 登录和可配置 Docker 网络后解析 Compose：

```bash
docker compose \
  --env-file deploy/docker/gateway/.env \
  --file deploy/docker/gateway/docker-compose.yml \
  config --quiet
```

执行部署：

```bash
./scripts/docker/deploy-gateway.sh \
  --repository registry.example.com/synapse/synapse-gateway \
  --tag 0.1.0 \
  --env-file deploy/docker/gateway/.env
```

脚本验证 Docker/Compose、必填配置和 GatewayProof secret，拉取目标镜像，仅更新 `gateway` 服务，轮询容器
健康状态，成功后原子更新 `.release/current`。不会打印 `.env`、完整 inspect 环境或 secret。

## 5. 健康检查与优雅停机

- 普通 health：`/actuator/health`。
- liveness：`/actuator/health/liveness`，用于判断进程是否仍应运行。
- readiness：`/actuator/health/readiness`，用于判断是否可接收流量，也是 Compose healthcheck 目标。
- healthcheck：间隔 10 秒、超时 5 秒、重试 12 次、启动缓冲 40 秒。
- Spring shutdown phase：30 秒；Compose `stop_grace_period`：40 秒。

健康端点可匿名访问，但仅暴露 `health,info`，不会公开 `env` 或 `configprops`。liveness 不绑定短期外部依赖，
避免 Nacos/IAM 短暂抖动造成无限重启；readiness 反映应用接流量能力。

手工检查：

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8080/actuator/health/liveness
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
docker compose --env-file deploy/docker/gateway/.env \
  -f deploy/docker/gateway/docker-compose.yml ps
docker compose --env-file deploy/docker/gateway/.env \
  -f deploy/docker/gateway/docker-compose.yml logs --tail 100 gateway
```

## 6. 更新与自动回滚

发布新 tag 后再次调用部署脚本。部署开始前，当前运行镜像写入 `.release/previous`；只有新容器进入
`healthy` 后才写入 `.release/current`。`unhealthy`、`exited`、`dead` 或超时会触发一次自动回滚。

回滚成功后，新版本部署命令仍返回非零，明确表示新版本发布失败。回滚失败不会递归回滚，也不会删除当前、
历史或失败镜像。可使用 `--no-auto-rollback` 禁用自动回滚，或用 `--timeout 300` 调整等待时间。

`.release` 只保存完整镜像引用，不保存环境变量或 secret，并已被 Git 忽略。

## 7. 手工回滚

回滚到 `.release/previous`：

```bash
./scripts/docker/rollback-gateway.sh \
  --env-file deploy/docker/gateway/.env
```

指定 tag 或完整镜像：

```bash
./scripts/docker/rollback-gateway.sh 0.0.9 \
  --env-file deploy/docker/gateway/.env

./scripts/docker/rollback-gateway.sh \
  --image registry.example.com/synapse/synapse-gateway:0.0.9 \
  --env-file deploy/docker/gateway/.env
```

回滚同样拉取镜像、等待 healthy 并更新 current。失败返回非零，保留容器和镜像用于排查。

## 8. 单实例限制

Docker Compose 单 Gateway 实例更新可能产生短暂中断，本方案没有伪装成 Kubernetes 式 RollingUpdate。
真正无中断需要两个以上 Gateway 实例配合上层负载均衡，或迁移 Kubernetes Deployment。

## 9. 配置与安全

- `.env` 必须权限受限且不得提交；`.env.example` 只包含占位值。
- `NACOS_PASSWORD`、`GATEWAY_PROOF_SECRET` 和 Registry 凭据不得出现在日志、label 或命令参数。
- 容器使用非 root、`no-new-privileges`、drop all capabilities 和独立 tmpfs `/tmp`。
- 不启用 privileged，不挂载 Docker socket、源码、Maven settings、SSH key 或业务数据。
- 日志使用 Docker `json-file`，单文件 100 MB、保留 5 个。
- 当前 Compose 使用服务器文件注入；迁移 Kubernetes 后应拆分为 ConfigMap 与 Secret。

## 10. 故障排查

| 现象 | 检查项 |
| --- | --- |
| Docker daemon 不可用 | Docker 服务、当前用户 socket 权限 |
| Compose config 失败 | `.env` 是否存在、必填变量和 Compose v2 版本 |
| 镜像 pull 失败 | Registry 地址、tag、网络、磁盘和 `docker login` |
| CPU 架构不匹配 | image platform 与服务器架构；重新指定 `linux/amd64` 或 `linux/arm64` |
| 端口冲突 | `GATEWAY_HOST_PORT` 是否被占用 |
| Nacos 无法连接 | 地址、namespace/group、认证、网络策略和 DNS |
| IAM/JWK 不可访问 | issuer/JWK URL、TLS、DNS、防火墙 |
| JWT 全部 401 | issuer、audience、JWK、Token 时间和必填 claim |
| GatewayProof 启动失败 | enabled、gateway-id、至少 32 字节 secret |
| readiness 失败 | 应用日志、端口、配置校验和外部依赖状态 |
| 容器 OOM Kill | Docker/主机 OOM 记录和 `JAVA_TOOL_OPTIONS` |
| Docker 磁盘不足 | `docker system df`；由运维按保留策略清理，脚本不会全局 prune |
| 时间漂移 | 主机 NTP/chrony 状态；GatewayProof 依赖准确时钟 |
| 回滚版本不存在 | `.release/previous` 与 Registry 中对应 tag |

## 11. Kubernetes 迁移映射

| Docker Compose | Kubernetes |
| --- | --- |
| image | Container image |
| `.env` | ConfigMap / Secret |
| ports | ContainerPort / Service |
| healthcheck | readinessProbe / livenessProbe |
| restart policy | Deployment controller |
| stop_grace_period | terminationGracePeriodSeconds |
| Docker network | Kubernetes Service networking |
| stdout/stderr | Container logging |
| deploy script | rollout / GitOps |
| `.release` | Deployment rollout history |

当前仓库不包含 Kubernetes YAML、Helm Chart、Ingress、Service 或 Argo CD 配置。
