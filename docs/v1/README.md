# Synapse Platform Documentation v1

Synapse Platform v1 文档是平台产品文档体系，不只是架构说明。它用于支撑后续开发、测试、部署、交付与长期维护。

## Documentation Goal

本文档空间服务于一个目标：

> 把 Synapse Platform 设计成一个可以真实落地、可以测试、可以部署、可以交付、可以长期维护的企业级平台产品。

## Documentation Principles

- 真实性优先：未经确认的信息不得写成事实。
- 文档驱动开发：先产品、架构、规范和设计，再编码。
- README 产品化：根 README 是 GitHub 门面，现代、简洁、直观。
- 单一事实来源：同类信息只维护在一个权威位置。
- 企业交付标准：文档必须能服务开发、测试、部署、运维和交付。

详细规范见：[Documentation Rules](02-specification/documentation-rules.md)。

## Documentation Structure

```text
docs/v1
├── README.md
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

## Sections

| Section | Purpose | Primary Audience |
| --- | --- | --- |
| `00-product` | 产品定位、目标用户、产品范围、路线图 | 产品负责人、客户、售前、项目负责人 |
| `01-architecture` | 总体架构、模块边界、服务边界、安全、网络、部署、扩展性 | 架构师、核心研发 |
| `02-specification` | 开发规范、命名规范、API 规范、日志规范、数据库规范、文档规范 | 开发人员、代码评审人员、AI 编码助手 |
| `03-design` | Gateway、IAM、Audit、Task、Message、File、Integration 等模块设计 | 模块负责人、开发人员、测试人员 |
| `04-testing` | 测试策略、验收标准、安全测试、集成测试、性能测试 | 测试工程师、开发人员、交付验收人员 |
| `05-deployment` | Docker、Kubernetes、VM、升级、备份、灾备 | 运维工程师、实施人员 |
| `06-delivery` | 安装、初始化、运维、升级、故障排查、交付手册 | 客户实施、交付团队、运维人员 |
| `07-reference` | 术语、端口、环境变量、权限码、版本、依赖、OpenAPI | 所有维护者 |
| `99-adr` | 架构决策记录 | 架构师、核心研发、长期维护者 |

## Current Documents

当前已有文档会逐步迁移到新的产品文档结构中。

| Document | Current Location | Target Section |
| --- | --- | --- |
| Vision | `00-overview/vision.md` | `00-product` |
| Architecture Principles | `00-overview/architecture-principles.md` | `01-architecture` / `99-adr` |
| Glossary | `00-overview/glossary.md` | `07-reference` |
| Roadmap | `00-overview/roadmap.md` | `00-product` |
| ADR-001 Platform Positioning | `01-adr/ADR-001-platform-positioning.md` | `99-adr` |
| System Context and Boundary | `02-architecture/system-context-and-boundary.md` | `01-architecture` |
| Documentation Rules | `02-specification/documentation-rules.md` | `02-specification` |
| Architecture Freeze | `decisions/architecture-freeze.md` | `99-adr` / `07-reference` |

> 迁移完成前，旧目录中的文档仍作为 Draft 存在；迁移时不得改变已确认事实。

## Writing Workflow

每次编写正式文档前，必须先确认：

1. 文档读者；
2. 文档目的；
3. 已知事实；
4. 不确定点；
5. 需要确认的信息；
6. 只能标记为待定的内容。

## Status

Status: Draft

当前 `docs/v1` 处于产品文档体系重构阶段。后续会优先补齐 `00-product`、`01-architecture`、`02-specification` 三类文档，再进入模块设计、测试、部署和交付文档。
