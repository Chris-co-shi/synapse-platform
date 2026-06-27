# Synapse Platform 产品定义

> **Build Your Enterprise Digital Foundation.**
>
> 构建企业数字化基础设施。

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 产品负责人、企业客户、售前、架构师、开发、测试、实施、运维与交付人员 |
| Purpose | 定义 Synapse Platform 的产品定位、价值、用户和边界 |
| Scope | 产品级定义；V1 具体范围见 `v1-scope.md` |
| Status | Draft |

本文档是产品定义的单一事实来源。V1 的交付范围和完成标准以 [V1 Scope](v1-scope.md) 为准。

## Product Overview

Synapse Platform 是一个 **企业数字化平台（Enterprise Digital Platform）**。

它面向正在启动数字化建设，或者已经拥有多套分散系统的企业，通过统一身份、权限、入口、治理与共享平台能力，帮助企业构建统一的数字化基础设施。

## Why It Exists

企业在分别建设 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 和其他系统时，往往会重复建设账号、权限、入口、日志、监控、任务、文件和消息等基础能力。

Synapse Platform 的存在意义，是减少重复建设，对分散系统进行统一收口和治理，并为后续整合与持续演进提供稳定基座。

## Product Philosophy

> **Technology should simplify enterprise digitalization, not complicate it.**
>
> 技术应该降低企业数字化的复杂度，而不是增加复杂度。

平台不为展示技术而增加不必要的复杂度，也不要求企业为了使用平台而重建所有既有系统。

## Product Positioning

Synapse Platform 是完整的平台产品，不是开发框架、微服务脚手架或 Spring Cloud Demo。

平台优先服务制造业数字化场景，但产品定义不绑定某一个业务领域。

## Core Values

| Value | Meaning |
| --- | --- |
| **Unified** | 统一身份、权限、入口和公共平台能力 |
| **Open** | 当前采用开源策略，保持开放和可扩展 |
| **Self-hosted** | 支持企业自主和私有化部署 |
| **Simple** | 产品界面、操作、部署和维护应直观易懂 |
| **Evolvable** | 支持企业持续演进，而不是一次性建设 |

## Target Customers and Users

目标客户是需要建设或治理数字化系统的企业。

直接用户包括：

- 企业 IT 和数字化团队；
- 平台管理员与安全管理员；
- 企业研发团队；
- 实施和运维团队；
- 系统集成人员。

MES 操作员、仓库管理员和质检员等业务岗位通常是业务系统用户，不是 Platform 的直接用户。

## V1 Product Baseline

V1 交付一个可运行、可测试、文档完整、能够通过 Docker Compose 安装部署的开源版本。

V1 核心包括：

- Gateway；
- IAM；
- RBAC；
- OAuth 2.0 / OpenID Connect；
- Resource Server 安全；
- 操作日志、安全日志和审计日志；
- 平台管理端前端；
- Docker Compose 部署。

File、Message 和 Task 属于低优先级 V1 范围。Monitor 和 Config 在 V1 仅保留基础能力。Integration 不进入 V1。

平台先完成自身闭环，再考虑真实业务系统接入。详细定义见 [V1 Scope](v1-scope.md)。

## Product Boundary

Synapse Platform 不替代业务系统，也不承载具体业务领域。

它不是：

- ERP、MES、WMS、QMS、EMS 或 MOM；
- IoT 业务系统；
- 低代码平台；
- 完整 BI 或 BPM 产品；
- AI Agent 平台；
- 仅提供认证的权限平台；
- 仅提供依赖包的开发框架。

业务系统继续拥有自己的领域模型、业务流程、业务规则、数据库和发布节奏。

## Deployment and Delivery

Docker Compose 是 V1 正式支持的部署方式。

VM 和 Kubernetes 不作为 V1 正式交付阻断项。云原生和更多部署方式属于后续演进方向，在正式验证前不得描述为已交付能力。

## Open-source and Commercial Strategy

当前：

- 以开源项目方式建设；
- 希望允许企业免费使用和自主部署；
- 具体开源许可证仍待确认。

未来：

- 希望具备商业化交付能力；
- 商业化时间、版本形态、服务范围和商业模式尚未确定；
- 当前不承诺 Community Edition 或 Enterprise Edition 等版本划分。

## Current Stage

```text
Product Definition and V1 Scope
  -> Overall Architecture
  -> Specification
  -> Module Design
  -> Development
  -> Testing
  -> Docker Compose Deployment
  -> Open-source Release
```

## Decisions Still Required

以下信息尚未确认：

- 重点面向的企业规模；
- 官方支持的数据库范围；
- V1 国际化范围；
- 浏览器兼容范围；
- 开源许可证；
- 版本发布节奏；
- VM、Kubernetes 等后续部署方式的正式支持级别。
