# Synapse Platform Documentation v1

> **Build Your Enterprise Digital Foundation.**

本目录用于支撑 Synapse Platform 的产品、架构、规范、设计、测试、部署和交付。

## Start Here

1. [产品定义](00-product/product.md)
2. [V1 范围与完成标准](00-product/v1-scope.md)
3. [架构原则](01-architecture/architecture-principles.md)
4. [总体架构](01-architecture/overall-architecture.md)
5. [系统上下文与边界](01-architecture/system-context-and-boundary.md)
6. [服务边界](01-architecture/service-boundary.md)
7. [安全架构](01-architecture/security-architecture.md)
8. [文档规范](02-specification/documentation-rules.md)
9. [术语表](07-reference/glossary.md)
10. [ADR-001：产品定位与边界](99-adr/ADR-001-platform-positioning.md)
11. [ADR-002：IAM 与 Resource 服务边界](99-adr/ADR-002-iam-resource-boundary.md)
12. [ADR-003：Token、会话与可信入口模型](99-adr/ADR-003-token-session-and-trust-boundary.md)

## V1 Baseline

V1 交付一个可运行的开源版本。

- P0：Gateway、IAM、Resource、Audit、RBAC、OAuth 2.0 / OIDC、服务端鉴权、管理端前端和 Docker Compose；
- P1：File、Message、Task；
- 基础能力：Monitor、Config；
- V1 不包含：Integration、Workflow、MDM、Report、AI Agent 平台和 Kubernetes 正式交付。

完成标准：功能可用、测试通过、文档完整、可通过 Docker Compose 安装部署，并完成平台自身闭环。

## Authorization Boundary

```text
Resource  -> 定义资源与权限目录
IAM       -> 管理主体与授权关系
各服务     -> 独立验证 Token 并执行权限
Audit     -> 记录资源、授权和访问行为
```

Gateway 只处理统一入口并透传 Bearer Token，不向下游注入用户、角色或权限 Header。

## Security Baseline

- Access Token 使用非对称签名 JWT；
- Refresh Token 使用 Opaque Token、摘要存储、rotation 和重放检测；
- Access Token 携带角色和权限码快照，不携带菜单或路由元数据；
- Gateway 和所有受保护服务独立验证 Token；
- GatewayProof 证明请求经过可信 Gateway，但不替代 JWT 或业务授权；
- 高风险变更通过会话撤销和 `jti` denylist 立即生效；
- Token、密码、私钥和 Client Secret 禁止进入日志。

## Architecture Baseline

- 保持独立微服务形态；
- Gateway、IAM、Resource、Audit 为 P0 独立服务；
- File、Message、Task 为 P1 独立服务；
- V1 可共用一个 PostgreSQL 实例；
- 每个服务使用独立 Schema、数据库账号和 Flyway migration；
- 禁止跨 Schema 访问和跨服务共享数据模型。

## Structure

```text
docs/v1
├── 00-product
├── 01-architecture
├── 02-specification
├── 03-design
├── 04-testing
├── 05-deployment
├── 06-delivery
├── 07-reference
└── 99-adr
```

## Principles

- 未确认的信息不得写成事实；
- 产品范围先于架构与实现；
- 同类信息只维护一个权威来源；
- 文档必须服务开发、测试、部署、运维和交付；
- 根 README 是 GitHub 门面，应现代、简洁、直观。

## Current Focus

安全架构基线已经形成，仍有 TTL、issuer/audience、密钥来源和浏览器 Token 保存策略等细节待模块设计确认。下一步进入通信架构设计。
