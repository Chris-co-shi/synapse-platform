# Synapse Platform

> **Build a standard, reliable and integrable enterprise foundation.**

Synapse Platform 是基于 Java 21、Spring Boot、Spring Cloud、OAuth 2.0 和 OpenID Connect 的企业级公共能力平台。

当前 V1 聚焦 **Identity & Access Foundation**：

- 用户使用 Authorization Code + PKCE；
- 服务和第三方使用 Client Credentials；
- Access Token 使用 RS256 JWT；
- Refresh Token 使用 Opaque Token；
- Gateway 只做认证与路由 Audience 校验；
- 下游服务独立验证 JWT 并执行权限；
- 审计主体区分 USER 与 CLIENT。

外部 MES、WMS、SAP 和遗留系统默认按黑盒处理。它们不被强制使用 Framework、Synapse JWT、Manifest 或集中权限模型；协议不兼容时通过项目级 Adapter 处理。

## Start Here

1. [V1 架构基线](docs/v1/00-product/v1-baseline.md)
2. [V1 范围](docs/v1/00-product/v1-scope.md)
3. [仓库差距分析](docs/v1/03-gap-analysis/repository-gap-analysis.md)
4. [架构原则](docs/v1/01-architecture/architecture-principles.md)
5. [系统上下文](docs/v1/01-architecture/system-context-and-boundary.md)
6. [安全架构](docs/v1/01-architecture/security-architecture.md)
7. [文档首页](docs/v1/README.md)

仓库中的 Opaque Access Token、Redis 授权快照和 GatewayProof 属于待迁移代码事实，不代表目标架构。

所有新增能力必须明确标记为 `NOW`、`NEXT`、`LATER` 或 `REJECTED`。
