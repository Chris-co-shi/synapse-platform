# Synapse Platform Architecture Sprint v1

本文档目录是 Synapse Platform 第一版架构设计空间，用于在进入大规模编码前完成平台定位、架构原则、ADR、部署、安全、集成与扩展性设计。

## 目标

Synapse Platform v1 的目标不是堆叠功能，而是先冻结企业级数字化平台的核心边界：

- Platform 是企业基座，不是业务系统。
- Framework 是技术底座，不承载平台业务。
- MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 与第三方系统保持自治。
- Platform 向业务系统提供身份、权限、入口、日志、审计、监控、任务、文件、消息、集成等公共能力。

## 文档结构

```text
docs/v1
├── README.md
├── 00-overview
│   ├── vision.md
│   ├── architecture-principles.md
│   ├── glossary.md
│   └── roadmap.md
├── 01-adr
│   └── ADR-001-platform-positioning.md
├── 02-architecture
│   └── system-context-and-boundary.md
├── 03-deployment
├── 04-network
├── 05-security
├── 06-integration
├── 07-scalability
├── diagrams
└── decisions
    └── architecture-freeze.md
```

## 阅读顺序

1. [Vision](00-overview/vision.md)
2. [Architecture Principles](00-overview/architecture-principles.md)
3. [Glossary](00-overview/glossary.md)
4. [ADR-001 Platform Positioning](01-adr/ADR-001-platform-positioning.md)
5. [System Context and Boundary](02-architecture/system-context-and-boundary.md)
6. [Roadmap](00-overview/roadmap.md)
7. [Architecture Freeze](decisions/architecture-freeze.md)

## 决策格式

所有重要架构问题统一按以下结构讨论和记录：

```text
Why
Alternatives
Decision
Impact
ADR
```

## 当前状态

Status: Architecture Sprint / Draft

本目录中的文档在 Architecture Freeze 前可以持续调整。冻结后，任何改变核心边界、部署模型、安全模型或服务边界的修改都必须新增或更新 ADR。
