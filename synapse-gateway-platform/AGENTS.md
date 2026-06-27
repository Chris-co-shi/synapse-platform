# Synapse Gateway 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Gateway 是平台统一外部入口，使用 Spring Cloud Gateway、WebFlux 和 Reactor，负责 Opaque Access Token 授权快照验证、静态路由、Header 清理、Bearer Token 透传和 GatewayProof 签发。

## 2. 强制边界

- 只能使用 Reactive 技术栈。
- 禁止 WebMVC、Servlet、数据库访问、Entity、Mapper、Repository。
- Redis 授权快照只能通过 Framework 统一 Opaque Token 适配访问，禁止在 Gateway 自行实现 IAM 业务查询。
- 禁止依赖任何其他平台模块的 server。
- 不承载 IAM 用户、角色、菜单、权限、会话或 Token 签发业务。
- GatewayProof 只证明请求经过可信 Gateway，不能替代 Access Token 验证和业务授权。
- 不传播可直接信任的用户、角色或权限 Header。
- Gateway 启动类位于 `com.indigo.synapse.gateway` 根包，禁止放入 `bootstrap` 子包。
- Gateway 只完成认证和入口安全，不判断业务 permission。
- 禁止配置 `hasAuthority`、`hasRole`、Route 权限 metadata 或 `gateway:admin` 例外。
- 下游服务负责接口、资源、组织、租户和数据权限。
- V1 默认只配置实际部署服务的路由，禁止保留指向未交付服务的活动默认路由。

## 3. 安全要求

- 转发前清理全部外部 `X-Synapse-Gateway-*` Header。
- GatewayProof Header、canonicalization、query、token hash、nonce 和 HMAC 必须复用 Framework 协议。
- GatewayProof 必须绑定路径改写后的最终下游请求。
- Opaque Access Token 原样通过 `Authorization` Header 透传。
- 禁止记录 Token、Token 摘要、Secret、canonical string 或完整环境变量。
- Redis 正常时以授权快照为权威来源；Redis 故障降级必须严格遵守安全架构中的 30 秒低风险只读边界。
- beta/prd 安全配置非法时必须启动失败。
- 新增安全代码必须包含完整中文 Javadoc 和对应测试。

## 4. 修改前必读

- [总体架构](../docs/v1/01-architecture/overall-architecture.md)
- [服务边界](../docs/v1/01-architecture/service-boundary.md)
- [安全架构](../docs/v1/01-architecture/security-architecture.md)
- [通信架构](../docs/v1/01-architecture/communication-architecture.md)
- 本地 Framework 的 WebFlux、Security 和 Opaque Resource Server 文档

## 5. 最低验证

```bash
mvn -f synapse-gateway-platform/pom.xml clean test
mvn -f synapse-gateway-platform/pom.xml dependency:tree
bash -n scripts/docker/*.sh
docker compose --env-file deploy/docker/gateway/.env.example -f deploy/docker/gateway/docker-compose.yml config
```

依赖树必须确认不存在 `synapse-webmvc`、`synapse-data`、`spring-webmvc` 和 Servlet API。

测试至少覆盖：

- Opaque Token 快照有效、失效和 Redis 故障；
- Header 清理；
- GatewayProof 最终路径绑定和 nonce 重放；
- 未配置路由默认不可用；
- Gateway 不执行 permission 判断。

## 6. 容器约束

- Dockerfile 只包含 Java Runtime 和已构建 JAR，禁止在运行镜像中加入 Maven、Git 或源码。
- 容器必须使用固定 UID/GID 的非 root 用户，日志写入 stdout/stderr，并支持 SIGTERM、readiness、liveness。
- 禁止把密码、Token、GatewayProof Secret、Registry 凭据或环境配置写入镜像。
- 生产镜像必须使用明确且不可变的 tag，禁止 `latest`。
