# Glossary：架构术语表

## Synapse Platform

面向制造业数字化系统的企业级数字化平台基座，提供统一身份、权限、入口、日志、审计、监控、任务、文件、消息、集成与部署支撑。

## Synapse Framework

Synapse 技术体系的统一开发框架，提供 Web、Security、OAuth2、Data、Messaging、Audit、Observability、AutoConfiguration 等技术能力。

## Business System

业务系统，例如 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 或第三方业务系统。业务系统拥有自己的领域模型、数据库和发布边界。

## Control Plane

控制面。负责身份、权限、配置、入口、安全策略、审计、任务调度、集成配置和平台运维等管理能力。

## Data Plane

数据面。负责真实业务数据处理和业务流程执行。Synapse Platform 不接管业务系统数据面。

## IAM

Identity and Access Management。负责身份认证、主体、凭据、OAuth2/OIDC、RBAC、Token、Client 与授权快照。

## RBAC

Role-Based Access Control。基于角色的访问控制模型，通常由用户、组织、角色、权限、资源等组成。

## OAuth2

授权框架，用于 Client、Resource Owner、Authorization Server、Resource Server 之间的授权协议。

## OIDC

OpenID Connect。基于 OAuth2 的身份认证协议，用于标准化用户身份信息表达。

## Gateway

平台统一入口，负责路由、认证前置、Header 清理、可信入口证明、限流与入口观测，不承担业务授权。

## GatewayProof

Synapse Gateway 向下游签发的可信入口证明，用于证明请求经过可信 Gateway。它不能替代 JWT，也不能证明业务权限。

## Resource Server

资源服务。独立验证 Access Token，并执行接口权限、资源权限、数据权限与业务规则。

## Platform Client

面向业务系统或第三方系统的接入 SDK / Client，封装调用 Platform 能力的协议、鉴权、错误处理与降级策略。

## API First

先定义 API 契约，再实现服务内部逻辑。用于稳定跨系统边界。

## Contract First

先冻结跨模块或跨系统契约，再生成或实现适配代码。禁止用内部 Entity 替代契约。

## ADR

Architecture Decision Record。架构决策记录，用于记录重要设计问题的背景、备选方案、最终决策与影响。

## Architecture Freeze

架构冻结。表示当前阶段核心定位、边界、安全模型、部署模型和服务边界已经形成基线。冻结后变更必须通过 ADR。
