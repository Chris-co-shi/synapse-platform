<div align="center">

# Synapse Platform

### Build Your Enterprise Digital Foundation.

企业数字化平台，为企业提供统一身份、安全、入口、治理与共享平台能力。

**Open Source · Self-hosted · Unified · Simple · Evolvable**

[产品定义](docs/v1/00-product/product.md) · [架构文档](docs/v1/01-architecture/system-context-and-boundary.md) · [文档首页](docs/v1/README.md)

</div>

---

## Why Synapse Platform?

企业在数字化建设初期，或者已经拥有多套彼此割裂的系统时，常常需要重复建设身份、权限、入口、日志、监控和任务等基础能力。

Synapse Platform 希望帮助企业：

- 建立统一的数字化基础设施；
- 对分散系统进行统一接入和治理；
- 支持企业本地化与私有化部署；
- 以更直观、更简单的方式持续演进。

> 技术应该降低企业数字化的复杂度，而不是增加复杂度。

## Product Boundary

Synapse Platform 是完整的平台产品，但不是 ERP、MES、WMS、QMS、EMS、MOM 或其他业务系统。

业务系统继续拥有自己的领域模型、业务流程、数据库和发布节奏。Platform 提供公共能力和统一治理基础。

```text
Business Systems
MES / WMS / QMS / EMS / MOM / ERP / IoT / External Systems
        ↑
        │ API / Client / Event / Webhook / Gateway
        │
Synapse Platform
Identity / Access / Entry / Governance / Shared Platform Services
        ↑
        │
Synapse Framework
Web / Security / OAuth2 / Data / Messaging / Observability
```

## V1 Foundation

当前已经确认的 V1 基础目标：

- RBAC 权限模型；
- OAuth 2.0 / OpenID Connect 基础闭环；
- 统一入口与平台访问闭环；
- 身份登录、令牌、刷新和退出闭环；
- 日志与审计闭环。

这些是产品目标，不表示已经全部完成。实际完成状态以测试和发布记录为准。

## Current Status

| Area | Status |
| --- | --- |
| Product Definition | Draft |
| Architecture | Draft |
| Development | In Progress |
| Testing Baseline | To Be Defined |
| Deployment Matrix | To Be Confirmed |
| Delivery Baseline | To Be Defined |

当前分支 `docs/v1` 聚焦产品、架构、规范、测试、部署与交付文档建设。

## Documentation

| Document | Purpose |
| --- | --- |
| [Product](docs/v1/00-product/product.md) | 产品定位、价值、用户、边界和当前阶段 |
| [Architecture Principles](docs/v1/01-architecture/architecture-principles.md) | 平台架构必须遵守的长期原则 |
| [System Context & Boundary](docs/v1/01-architecture/system-context-and-boundary.md) | Platform 在企业数字化体系中的位置和边界 |
| [Documentation Rules](docs/v1/02-specification/documentation-rules.md) | 文档真实性、评审、维护和 GitHub 门面规范 |
| [Glossary](docs/v1/07-reference/glossary.md) | 产品与架构术语 |
| [ADR-001](docs/v1/99-adr/ADR-001-platform-positioning.md) | 产品定位与边界的决策记录 |
| [Documentation Home](docs/v1/README.md) | v1 产品文档导航 |

## Repository Rules

- [Repository Rules](AGENTS.md)
- [Gateway Rules](synapse-gateway-platform/AGENTS.md)
- [IAM Rules](synapse-iam-platform/AGENTS.md)
- [Resource Rules](synapse-resource-platform/AGENTS.md)
- [Audit Rules](synapse-audit-platform/AGENTS.md)
- [File Rules](synapse-file-platform/AGENTS.md)
- [Message Rules](synapse-message-platform/AGENTS.md)
- [Task Rules](synapse-task-platform/AGENTS.md)
- [Integration Rules](synapse-integration-platform/AGENTS.md)

## Security

不要提交真实密码、令牌、私钥、注册中心凭据、GatewayProof 密钥或 `.env` 文件。

Gateway 完成入口认证不能成为下游资源服务跳过令牌验证、资源授权或数据权限检查的理由。
