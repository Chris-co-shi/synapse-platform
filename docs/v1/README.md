# Synapse Platform Documentation v1

本目录用于支撑 Synapse Platform V1 的产品、架构、规范、设计、测试、部署和交付。

## 权威阅读顺序

1. [V1 架构基线](00-product/v1-baseline.md)
2. [V1 范围与完成标准](00-product/v1-scope.md)
3. [仓库差距分析](03-gap-analysis/repository-gap-analysis.md)
4. [架构原则](01-architecture/architecture-principles.md)
5. [系统上下文与边界](01-architecture/system-context-and-boundary.md)
6. [总体架构](01-architecture/overall-architecture.md)
7. [服务边界](01-architecture/service-boundary.md)
8. [安全架构](01-architecture/security-architecture.md)
9. [通信架构](01-architecture/communication-architecture.md)
10. [数据架构](01-architecture/data-architecture.md)
11. [数据库规范](02-specification/database-conventions.md)
12. [Host-local 认证闭环测试状态](04-testing/host-local-auth-loop.md)

## 当前 V1

V1 交付 Identity & Access Foundation：

- OAuth 2.0 / OpenID Connect；
- Authorization Code + PKCE；
- Client Credentials；
- Opaque Access Token；
- Opaque Refresh Token；
- Redis 授权快照；
- Gateway 与 IAM 独立验证快照；
- USER / CLIENT 审计主体；
- 第三方标准 OAuth2 调用。

当前认证闭环的最新 host-local 状态为 `Passed with Known Limitations`。已知限制是 Refresh Token reuse detection 尚未完成 token family 整体撤销，旧 Refresh Token 重放后 successor Refresh Token 仍可继续 refresh。

当前已实现的是自定义 IAM 登录 API + Opaque Token 会话体系，不是标准 OAuth2 Authorization Server。OAuth2/OIDC、标准 Token Endpoint、Authorization Code + PKCE、Client Credentials、OIDC Discovery、ID Token 和 UserInfo 仍为计划实现。

Resource Manifest、独立 Resource Catalog、Revocation Feed、多租户、完整 Integration Platform 和其余平台产品能力不进入当前 NOW。

## 文档纪律

- 当前事实以源码、POM、配置和测试为准；
- 目标状态以 `v1-baseline.md` 为准；
- 差异必须记录在 Gap Analysis；
- 计划能力不得写成已实现能力；
- 旧 ADR 与新基线冲突时，应新增 supersede 说明或重写，不得并存两套有效口径。
