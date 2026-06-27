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
8. [通信架构](01-architecture/communication-architecture.md)
9. [文档规范](02-specification/documentation-rules.md)
10. [术语表](07-reference/glossary.md)
11. [ADR-001：产品定位与边界](99-adr/ADR-001-platform-positioning.md)
12. [ADR-002：IAM 与 Resource 服务边界](99-adr/ADR-002-iam-resource-boundary.md)
13. [ADR-003：Token、会话与可信入口模型](99-adr/ADR-003-token-session-and-trust-boundary.md)
14. [ADR-004：服务通信、Outbox 与 RocketMQ](99-adr/ADR-004-service-communication-and-outbox.md)

## V1 Baseline

V1 交付一个可运行的开源版本。

- P0：Gateway、IAM、Resource、Audit、RBAC、OAuth 2.0 / OIDC、服务端鉴权、管理端前端和 Docker Compose；
- P1：Config、File、Message、Task；
- 基础能力：Actuator、健康检查和管理端基础状态；
- V1 不包含：Integration、Workflow、MDM、Report、AI Agent 平台、独立 Monitor 和 Kubernetes 正式交付。

完成标准：功能可用、测试通过、文档完整、可通过 Docker Compose 安装部署，并完成平台自身闭环。

## Authorization Boundary

```text
Resource  -> 定义资源与权限目录
IAM       -> 管理主体、授权关系和 Redis 授权快照
各服务     -> 独立验证快照并执行权限
Audit     -> 记录资源、授权和访问行为
Config    -> 管理国际化资源和字典
```

Gateway 透传 Bearer Token，不向下游注入用户、角色或权限 Header。

## Security Baseline

- Access Token 使用高熵 Opaque Token；
- Redis 保存授权快照，Gateway 和各服务分别验证；
- Refresh Token 使用摘要存储、rotation 和重放检测；
- 用户与 Client Credentials 使用统一 Opaque Token 模型；
- RS256 只用于 OIDC ID Token；
- Access Token 默认 15 分钟，Refresh Token 空闲 7 天，会话最长 30 天；
- Redis 故障仅允许已缓存低风险读取最多 30 秒；
- Access Token 仅保存在前端内存，Refresh Token 保存在 `sessionStorage`；
- Token、密码和密钥材料禁止进入日志。

## Communication Baseline

- 用户同步调用传播原始用户 Token；
- 后台任务和纯服务调用使用 Client Credentials；
- IAM 同步批量校验 Resource 权限码；
- 资源树按应用和节点懒加载，并使用 `catalogVersion`；
- 关键事件使用本地 Outbox + RocketMQ；
- 采用 At-least-once + `eventId` 幂等；
- RocketMQ 是 V1 默认基础设施；
- 非幂等命令默认不自动重试。

## Architecture Baseline

- 保持独立微服务形态；
- Gateway、IAM、Resource、Audit 为 P0；
- Config、File、Message、Task 为 P1；
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

产品范围、总体架构、服务边界、安全架构和通信架构已经确认。下一步进入数据架构设计。
