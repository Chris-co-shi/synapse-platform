# Synapse Platform 产品定义

> **Build Your Enterprise Digital Foundation.**
>
> 构建企业数字化基础设施。

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 产品负责人、企业客户、售前、架构师、开发、测试、实施、运维与交付人员 |
| Purpose | 定义 Synapse Platform 的产品定位、价值、用户、范围与当前阶段 |
| Scope | Synapse Platform 产品级定义；不包含模块实现、接口参数或数据库设计 |
| Status | Draft |

本文档是 Synapse Platform 产品定义的单一事实来源。README 仅引用和摘要本文档，不应形成另一套产品口径。

## 1. Product Overview

Synapse Platform 是一个 **企业数字化平台（Enterprise Digital Platform）**。

它帮助两类企业建立统一的数字化基础设施：

1. 尚未形成数字化基座，准备建设多个业务系统的企业；
2. 已经上线多套系统，但身份、权限、入口、日志、监控和公共能力分散、重复且标准不一致的企业。

Synapse Platform 通过统一身份与权限、统一入口、统一治理和共享平台能力，对企业数字化系统进行统一收口，并为后续整合与持续演进提供稳定基座。

## 2. Why Synapse Platform Exists

企业数字化建设通常会经历两个阶段性问题。

### 2.1 从零建设时重复造轮子

当企业分别建设 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 或其他系统时，每套系统可能重复实现：

- 用户与身份认证；
- 角色与权限；
- 系统入口；
- 日志与审计；
- 监控与运行状态；
- 定时任务与分布式任务；
- 文件、消息、配置和集成基础能力。

重复建设会增加成本，也会导致标准、体验和安全策略不一致。

### 2.2 已有系统难以统一治理

企业已经存在多套系统时，常见问题包括：

- 多套账号和权限体系彼此割裂；
- 系统入口分散；
- 日志、审计和监控标准不统一；
- 系统接入方式不一致；
- 运维、升级与故障排查成本高；
- 后续新增系统继续复制旧问题。

Synapse Platform 的存在意义，是降低这些复杂度，而不是再增加一套难以维护的孤立系统。

## 3. Product Philosophy

> **Technology should simplify enterprise digitalization, not complicate it.**
>
> 技术应该降低企业数字化的复杂度，而不是增加复杂度。

该理念约束产品设计、架构、用户体验、部署、运维和交付：

- 不为展示技术而增加不必要的复杂度；
- 不通过堆叠功能掩盖边界不清；
- 不要求企业为了使用平台而重建所有既有系统；
- 优先提供直观、可理解、可部署和可维护的能力。

## 4. Product Positioning

Synapse Platform 的产品定位是：

> 为企业 IT 提供统一身份、安全、入口、治理与共享平台能力，帮助企业构建统一的数字化基础设施，并对分散的业务系统进行统一接入、统一治理和逐步整合。

Synapse Platform 是一个完整的平台产品，不是单纯的开发框架、微服务脚手架或 Spring Cloud Demo。

平台优先服务制造业数字化场景，但产品定义不绑定某一个业务领域。

## 5. Core Product Value

| Value | Meaning |
| --- | --- |
| **Unified** | 统一企业数字化基础设施，减少身份、权限、入口和公共能力的重复建设 |
| **Open** | 当前采用开源策略，保持开放、可扩展和可审查；具体开源许可证仍待确认 |
| **Self-hosted** | 以本地化和企业私有化部署为核心产品特征，企业能够掌握系统和数据 |
| **Simple** | 产品界面、操作、部署与维护应直观、易理解，避免不必要的使用门槛 |
| **Evolvable** | 支持企业从初始建设到多系统治理和整合的持续演进，而不是一次性方案 |

## 6. Target Customers

Synapse Platform 面向需要建设或治理企业数字化系统的组织，重点包括：

- 正在启动数字化建设的企业；
- 已经拥有多套分散业务系统、需要统一收口的企业；
- 需要私有化部署和自主控制平台能力的企业；
- 需要为多个业务系统提供统一基础设施的企业 IT 或数字化部门。

企业规模的明确优先级尚未确认，不在本文档中推断。

## 7. Target Users

Synapse Platform 的直接用户主要包括：

- 企业 IT 团队；
- 平台管理员；
- 安全管理员；
- 企业研发团队；
- 实施团队；
- 运维团队；
- 系统集成人员。

MES 操作员、仓库管理员、质检员等业务岗位通常是业务系统用户，不是 Synapse Platform 的直接目标用户。

## 8. Product Capability Model

### 8.1 V1 必须形成的基础闭环

当前已经确认的 V1 基础目标包括：

- RBAC 权限模型；
- OAuth 2.0 / OpenID Connect 基础闭环；
- 统一入口与平台访问闭环；
- 登录、令牌、刷新、退出和当前用户等身份闭环；
- 日志与审计闭环。

这些是当前产品目标，不等同于已经全部完成。实际完成状态应由测试、发布记录和版本说明证明。

### 8.2 平台能力方向

在基础闭环之上，Synapse Platform 可以逐步提供：

- 统一身份与访问管理；
- 统一入口与流量治理；
- 日志、审计、监控与可观测性；
- 分布式任务与调度；
- 文件、消息和配置等共享能力；
- 外部系统接入与集成能力；
- 面向业务系统的标准 API、Client、事件和连接器。

具体能力是否进入 V1，必须在产品范围或模块设计中单独确认，不能因为出现在能力方向中就视为交付承诺。

### 8.3 AI Agent

AI Agent 可以在未来参与平台运维、知识检索、配置辅助、故障分析或系统协同，但当前不作为 Synapse Platform 的核心产品定义，也不自动构成 V1 交付范围。

## 9. Product Boundary

Synapse Platform 不替代业务系统，也不承载具体业务领域。

它不是：

- ERP；
- MES；
- WMS；
- QMS；
- EMS；
- MOM；
- IoT 业务系统；
- 低代码平台；
- 完整 BI 产品；
- 完整 BPM 产品；
- 仅提供认证的权限平台；
- 仅提供工程依赖的开发框架。

业务系统继续拥有自己的领域模型、业务流程、业务规则、数据库和发布节奏。Synapse Platform 提供公共能力和统一治理基础。

## 10. Deployment and Delivery Positioning

Synapse Platform 以可自主部署为重要产品特征：

- 支持企业本地化和私有化部署是明确方向；
- 支持云原生演进是明确方向；
- 产品需要形成安装、配置、升级、备份、恢复、运维和故障排查文档；
- 具体支持的 Docker、VM、Kubernetes 组合及其 V1 交付级别仍待确认。

在部署矩阵确认前，不应把所有部署方式描述为已完成或已验证。

## 11. Open-source and Commercial Strategy

当前策略：

- Synapse Platform 目前作为开源项目建设；
- 产品希望允许企业免费使用和自主部署；
- 具体开源许可证尚待确认。

长期策略：

- 未来希望具备商业化交付能力；
- 商业化时间、版本形态、服务范围和商业模式尚未确定；
- 在正式决策前，不使用 Community Edition、Enterprise Edition 等版本承诺。

## 12. Current Stage

Synapse Platform 当前处于产品与架构基线建设阶段：

```text
Product Definition
  -> Architecture
  -> Specification
  -> Module Design
  -> Development
  -> Testing
  -> Deployment
  -> Delivery
```

现阶段的重点是形成可以指导开发、测试、部署和交付的产品文档体系，并逐步确认 V1 范围。

## 13. Decisions Still Required

以下信息尚未确认，不得在其他文档中自行推断：

- 重点面向的企业规模；
- V1 正式支持的部署矩阵；
- 官方支持的数据库范围；
- V1 国际化范围；
- 浏览器兼容范围；
- 开源许可证；
- 产品版本与商业版本策略；
- V1 发布与验收计划。

这些问题将在后续产品或交付文档中逐项讨论和确认。
