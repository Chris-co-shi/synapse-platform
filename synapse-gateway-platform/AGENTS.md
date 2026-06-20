# Synapse Gateway 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Gateway 是平台统一外部入口，负责 Reactive JWT 认证、静态路由和可信入口证明，使用 Spring Cloud Gateway、WebFlux 和 Reactor。

## 2. 强制边界

- 只能使用 Reactive 技术栈。
- 禁止 WebMVC、Servlet、数据库访问、Entity、Mapper、Repository。
- 禁止依赖任何其他平台模块的 server。
- 不承载 IAM 用户、角色、菜单、权限或 Token 签发业务。
- JWT 负责身份认证；GatewayProof 只证明请求经过可信 Gateway，不能替代 JWT。
- 不传播可直接信任的用户、角色或权限 Header。

## 3. 安全要求

- 转发前清理全部外部 `X-Synapse-Gateway-*` Header。
- GatewayProof 必须绑定路径改写后的最终下游请求。
- 禁止记录 Token、secret、canonical string 或完整环境变量。
- beta/prd 开启证明时，配置非法必须启动失败。
- 新增安全代码必须包含完整中文 Javadoc 和对应测试。

## 4. 修改前必读

- [Gateway 设计与安全模型](../docs/gateway.md)
- [Gateway Docker 部署](../deploy/docker/gateway/README.md)
- 本地 Framework 的 `docs/modules/synapse-webflux.md`、`synapse-security.md` 和 `synapse-oauth2-resource-server-webflux.md`

## 5. 最低验证

```bash
mvn -f synapse-gateway-platform/pom.xml clean test
mvn -f synapse-gateway-platform/pom.xml dependency:tree
bash -n scripts/docker/*.sh
docker compose --env-file deploy/docker/gateway/.env.example -f deploy/docker/gateway/docker-compose.yml config
```

依赖树必须确认不存在 `synapse-webmvc`、`synapse-data`、`spring-webmvc` 和 Servlet API。
