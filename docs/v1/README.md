# Synapse Platform Documentation v1

> **Build Your Enterprise Digital Foundation.**

Synapse Platform v1 文档用于支撑产品定义、架构设计、开发规范、模块设计、测试、部署、运维和企业交付。

当前文档状态：**Draft**。

## Start Here

1. [产品定义](00-product/product.md)
2. [架构原则](01-architecture/architecture-principles.md)
3. [系统上下文与边界](01-architecture/system-context-and-boundary.md)
4. [文档协作与编写规范](02-specification/documentation-rules.md)
5. [术语表](07-reference/glossary.md)
6. [ADR-001：产品定位与边界](99-adr/ADR-001-platform-positioning.md)

## Documentation Goal

本文档空间服务于一个目标：

> 把 Synapse Platform 设计成一个可以真实落地、可以测试、可以部署、可以交付、可以长期维护的企业级平台产品。

## Documentation Principles

- **真实性优先**：未经确认的信息不得写成事实。
- **产品先行**：先确认产品价值、范围和边界，再进入架构与实现。
- **文档驱动**：架构、规范和设计应先于大规模编码。
- **单一事实来源**：同类信息只维护在一个权威位置。
- **交付导向**：文档必须能够服务开发、测试、部署、运维和客户交付。
- **README 产品化**：根 README 是 GitHub 门面，应现代、简洁、直观。

详细规范见：[Documentation Rules](02-specification/documentation-rules.md)。

## Documentation Structure

```text
docs/v1
├── 00-product        # 产品定位、价值、用户、范围和路线图
├── 01-architecture   # 总体架构、边界、安全、网络和扩展性
├── 02-specification  # 开发、API、日志、数据库和文档规范
├── 03-design         # Gateway、IAM、Audit、Task 等模块设计
├── 04-testing        # 测试策略、验收、安全和性能测试
├── 05-deployment     # Docker、VM、Kubernetes、升级和灾备
├── 06-delivery       # 安装、初始化、运维、升级和故障排查
├── 07-reference      # 术语、端口、环境变量、权限和依赖
└── 99-adr            # 架构决策记录
```

未创建的目录代表后续文档分类，不代表对应能力已经设计完成或进入 V1。

## Audience

| Section | Primary Audience |
| --- | --- |
| `00-product` | 产品负责人、企业客户、售前、项目负责人 |
| `01-architecture` | 架构师、核心研发、测试和运维负责人 |
| `02-specification` | 开发人员、评审人员、AI 编码助手 |
| `03-design` | 模块负责人、开发和测试人员 |
| `04-testing` | 测试、开发和交付验收人员 |
| `05-deployment` | 运维、实施和平台管理员 |
| `06-delivery` | 客户实施、交付和运维团队 |
| `07-reference` | 所有维护者 |
| `99-adr` | 架构师、核心研发和长期维护者 |

## Writing Workflow

正式文档遵循：

```text
Discussion
  -> Confirm Facts and Unknowns
  -> Product / Architecture / Specification
  -> Review
  -> GitHub Commit
  -> Design
  -> Development
  -> Testing
  -> Deployment
  -> Delivery
```

编写前必须明确：

1. 文档读者；
2. 文档目的；
3. 已确认事实；
4. 不确定点；
5. 需要项目负责人确认的信息；
6. 只能标记为待定的内容。

## Current Focus

当前优先级：

1. 完成产品定义和 V1 范围确认；
2. 完成总体架构和服务边界；
3. 建立开发、测试、部署和交付基线；
4. 再进入各平台模块的详细设计。
