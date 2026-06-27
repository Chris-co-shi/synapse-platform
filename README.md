# Synapse Platform

> Enterprise Digital Platform Foundation for Manufacturing Systems.

Synapse Platform 是面向制造业数字化系统的企业级平台基座，为 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 与第三方业务系统提供统一身份、权限、入口、日志、审计、监控、任务、文件、消息、集成与部署支撑。

它不是具体业务系统，也不是 Spring Cloud Demo；它的目标是成为可以真实落地、支持私有化部署、支持云原生演进、可长期维护的平台产品。

---

## What is Synapse Platform?

```text
Business Systems
MES / WMS / QMS / EMS / MOM / ERP / IoT / External Systems
        ↑
        │  API / Client / Event / Webhook / Gateway
        │
Synapse Platform
Gateway / IAM / Audit / Monitor / Task / File / Message / Integration
        ↑
        │
Synapse Framework
Web / Security / OAuth2 / Data / Messaging / Observability
```

Synapse Platform 提供企业公共能力，但不承载业务领域模型。

---

## Core Capabilities

| Area | Capability |
| --- | --- |
| Identity | IAM, OAuth2, OIDC, Token, Client |
| Authorization | RBAC, Permission, Resource, Server-side Enforcement |
| Entry | Gateway, Routing, Trusted Entry, Header Sanitization |
| Traceability | Audit, Operation Log, Security Log, TraceId |
| Platform Services | Task, File, Message, Config, Monitor, Integration |
| Delivery | Docker, VM, Kubernetes, Private Deployment |

---

## Current Status

| Item | Status |
| --- | --- |
| Product Documentation | Draft |
| Architecture | Draft |
| Development | In Progress |
| Testing | To Be Defined |
| Deployment | To Be Defined |
| Delivery | To Be Defined |

Current branch focus:

```text
Product Documentation v1
```

The current goal is to establish product, architecture, specification, testing, deployment and delivery documentation before large-scale coding.

---

## Documentation

Start here:

- [Synapse Platform Documentation v1](docs/v1/README.md)
- [Documentation Rules](docs/v1/02-specification/documentation-rules.md)
- [Vision](docs/v1/00-overview/vision.md)
- [Architecture Principles](docs/v1/00-overview/architecture-principles.md)
- [ADR-001: Platform Positioning](docs/v1/01-adr/ADR-001-platform-positioning.md)
- [System Context and Boundary](docs/v1/02-architecture/system-context-and-boundary.md)

---

## Documentation Structure

```text
docs/v1
├── 00-product        # Product positioning, scope, users, roadmap
├── 01-architecture   # Overall architecture, module boundary, security, network
├── 02-specification  # Coding, API, logging, database, documentation standards
├── 03-design         # Gateway, IAM, Audit, Task, File, Message, Integration design
├── 04-testing        # Testing strategy, acceptance, security, performance
├── 05-deployment     # Docker, VM, Kubernetes, upgrade, backup, DR
├── 06-delivery       # Installation, operation, maintenance, troubleshooting
├── 07-reference      # Glossary, ports, env vars, permissions, dependencies
└── 99-adr            # Architecture Decision Records
```

---

## Boundary

Synapse Platform does not implement:

- MES production domain;
- WMS inventory domain;
- QMS quality domain;
- EMS energy domain;
- ERP core business domain;
- business system database;
- business-specific workflow and rules.

Business systems remain autonomous and integrate with Synapse Platform through contracts, clients, events, webhooks and gateway entry.

---

## Development Rules

- [Repository Rules](AGENTS.md)
- [Gateway Rules](synapse-gateway-platform/AGENTS.md)
- [IAM Rules](synapse-iam-platform/AGENTS.md)
- [Resource Rules](synapse-resource-platform/AGENTS.md)
- [Config Rules](synapse-config-platform/AGENTS.md)
- [Audit Rules](synapse-audit-platform/AGENTS.md)
- [File Rules](synapse-file-platform/AGENTS.md)
- [Message Rules](synapse-message-platform/AGENTS.md)
- [Task Rules](synapse-task-platform/AGENTS.md)
- [Workflow Rules](synapse-workflow-platform/AGENTS.md)
- [Integration Rules](synapse-integration-platform/AGENTS.md)
- [MDM Rules](synapse-mdm-platform/AGENTS.md)
- [Report Rules](synapse-report-platform/AGENTS.md)
- [Monitor Rules](synapse-monitor-platform/AGENTS.md)

---

## Security Notice

Never commit real passwords, tokens, GatewayProof secrets, registry credentials, private keys or `.env` files.

GatewayProof does not replace JWT. Gateway authentication must not be used as a reason for downstream services to skip token validation, resource authorization or data permission checks.
