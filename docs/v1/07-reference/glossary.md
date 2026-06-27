# Synapse Platform 术语表

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 产品、架构、开发、测试、实施、运维和交付人员 |
| Purpose | 统一 Synapse Platform 产品与架构文档中的核心术语 |
| Scope | `docs/v1/**` |
| Status | Draft |

## Synapse Platform

企业数字化平台，帮助企业构建统一的数字化基础设施，并为业务系统提供统一身份、权限、入口、治理与共享平台能力。

## Enterprise Digital Foundation

企业数字化基础设施。指支撑多个业务系统共同运行、接入、治理和演进的身份、安全、入口、日志、审计、监控、任务、集成及其他共享能力。

## Synapse Framework

Synapse 技术体系的通用开发框架，为 Platform 与业务系统提供 Web、Security、OAuth2、Data、Messaging、Audit、Observability 和 AutoConfiguration 等技术能力。

## Business System

业务系统，例如 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 或第三方业务系统。业务系统拥有自己的领域模型、数据库、业务规则和发布边界。

## Control Plane

控制面。负责身份、权限、配置、入口、安全策略、审计、任务调度、集成配置和平台运维等管理能力。

## Business Data Plane

业务数据面。负责真实业务数据处理和业务流程执行，由各业务系统自行掌握。Synapse Platform 不接管业务系统核心数据面。

## IAM

Identity and Access Management。身份与访问管理，负责主体、凭据、认证、OAuth 2.0 / OIDC、Token、Client、角色和权限等能力。

## RBAC

Role-Based Access Control。基于角色的访问控制模型，通过用户、角色和权限之间的关系实现授权管理。

## OAuth 2.0

授权框架，用于 Client、Authorization Server、Resource Server 和资源拥有者之间的授权交互。

## OIDC

OpenID Connect。建立在 OAuth 2.0 之上的身份认证协议，用于标准化用户身份信息表达。

## Gateway

平台统一入口，负责路由、基础认证校验、Header 清理、可信入口证明、基础流量治理和入口观测，不承担业务资源授权。

## GatewayProof

Synapse Gateway 向下游签发的可信入口证明，用于证明请求经过可信 Gateway。它不能替代 Access Token，也不能证明调用方具备业务权限。

## Resource Server

资源服务。独立验证 Access Token，并执行接口权限、资源权限、数据权限和业务规则。

## Platform Client

面向业务系统或第三方系统的接入 SDK / Client，封装调用 Platform 能力所需的协议、鉴权、错误处理和兼容策略。

## API First

先定义 API 契约，再实现服务内部逻辑，用于稳定跨服务和跨系统边界。

## Contract First

先确认跨模块或跨系统契约，再实现或生成适配代码。禁止用内部 Entity 替代跨边界契约。

## ADR

Architecture Decision Record。架构决策记录，用于记录重要设计问题的背景、备选方案、最终决策及其影响。

## Architecture Freeze

架构冻结。表示当前版本的核心产品边界、服务边界、安全模型、部署模型和关键数据流已经形成基线。冻结后的重大变更必须通过 ADR 或正式变更记录处理。

## Self-hosted

由企业在自有基础设施或受控环境中部署和运行产品，而不是必须依赖第三方 SaaS 服务。

## V1

Synapse Platform 第一阶段产品文档和交付基线。具体功能、部署和验收范围必须由正式产品范围及发布文档确认。
