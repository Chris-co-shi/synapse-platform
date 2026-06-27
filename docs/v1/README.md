# Synapse Platform Documentation v1

> **Build Your Enterprise Digital Foundation.**

本目录用于支撑 Synapse Platform 的产品、架构、规范、设计、测试、部署和交付。

## Start Here

1. [产品定义](00-product/product.md)
2. [V1 范围与完成标准](00-product/v1-scope.md)
3. [架构原则](01-architecture/architecture-principles.md)
4. [系统上下文与边界](01-architecture/system-context-and-boundary.md)
5. [文档规范](02-specification/documentation-rules.md)
6. [术语表](07-reference/glossary.md)
7. [ADR-001：产品定位与边界](99-adr/ADR-001-platform-positioning.md)

## V1 Baseline

V1 交付一个可运行的开源版本。

- P0：Gateway、IAM、RBAC、OAuth 2.0 / OIDC、Resource Server 安全、操作日志、安全日志、审计日志、管理端前端、Docker Compose；
- P1：File、Message、Task；
- 基础能力：Monitor、Config；
- V1 不包含：Integration、Workflow、MDM、Report、AI Agent 平台和 Kubernetes 正式交付。

完成标准：功能可用、测试通过、文档完整、可通过 Docker Compose 安装部署，并完成平台自身闭环。

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

V1 产品范围已经确认。下一步进入总体架构设计和服务边界讨论。
