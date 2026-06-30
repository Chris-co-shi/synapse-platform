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

企业数字化基础设施。指支撑多个业务系统共同运行、接入、治理和演进的身份、安全、入口、配置、日志、审计、任务及其他共享能力。

## Synapse Framework

Synapse 技术体系的通用开发框架，为 Platform 与业务系统提供 Web、Security、OAuth2、Data、Messaging、Audit、Observability 和 AutoConfiguration 等技术能力。

## Business System

业务系统，例如 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 或第三方业务系统。业务系统拥有自己的领域模型、数据库、业务规则和发布边界。

## Control Plane

控制面。负责身份、权限、资源目录、配置、入口、安全策略、审计、任务调度和平台运维等管理能力。

## Business Data Plane

业务数据面。负责真实业务数据处理和业务流程执行，由各业务系统自行掌握。Synapse Platform 不接管业务系统核心数据面。

## IAM

Identity and Access Management。负责主体、凭据、认证、Opaque Token、Client、会话、角色授权关系和 Redis 授权快照生命周期。OAuth 2.0 / OIDC 是 V1 计划实现的标准协议能力。

## Resource Center

资源与权限目录中心。负责应用、菜单、页面、按钮、API、权限码定义、授权资源树和当前用户导航，不负责用户角色关系和 Token 签发。

## Config

平台级业务配置服务。V1 最小范围是国际化资源、字典类型和字典项。Config 不等同于 Nacos 技术配置中心。

## RBAC

Role-Based Access Control。基于角色的访问控制模型，通过用户、角色和权限之间的关系实现授权管理。

## OAuth 2.0

授权框架，用于 Client、Authorization Server、Resource Server 和资源拥有者之间的授权交互。

## OIDC

OpenID Connect。建立在 OAuth 2.0 之上的身份认证协议。当前尚未实现；后续需要签名的 ID Token 使用 RS256，Access Token 不使用 JWT。

## Opaque Access Token

高熵、不可预测的 Bearer 字符串。Token 本身不携带主体和权限信息，Gateway 与 Resource Server 使用其安全摘要从 Redis 获取授权快照。

## Opaque Refresh Token

用于续期会话的高熵随机字符串。IAM 数据库只保存安全摘要，并通过 rotation 和 reuse detection 防止旧 Token 重放。

## Authorization Snapshot

授权快照。由 IAM 写入 Redis，包含 Access Token 对应的主体、Client、会话、角色、权限、issuer、audience、有效期和状态。Gateway 和受保护服务只读验证。

## Client Credentials

OAuth 2.0 中用于机器客户端或服务身份的授权方式。当前标准 Client Credentials 尚未实现；V1 目标中的 Client Token 使用 Opaque Access Token + Redis 授权快照，并与用户身份明确区分。

## Gateway

平台统一入口，负责路由、Opaque Token 快照验证、Header 清理、Bearer Token 透传、GatewayProof、基础流量治理和入口观测，不承担业务权限判断。

## GatewayProof

Gateway 向下游签发的可信入口证明，用于证明请求经过可信 Gateway。它不能替代 Access Token，也不能证明调用方具备业务权限。

## Resource Server

受保护服务。独立验证 Opaque Access Token 的 Redis 授权快照，并执行接口权限、资源权限、数据权限和业务规则。

## Permission Code

稳定、全局唯一的权限标识，推荐格式为 `{domain}:{resource}:{action}`。Resource 定义权限码，IAM 使用权限码保存角色授权关系。

## Catalog Version

Resource 权限目录版本。角色授权提交时携带该版本，用于发现资源树加载后目录已经发生变化的并发冲突。

## Outbox

本地事件表模式。业务服务在本地业务事务中同时写入业务数据和待发送事件，提交后由投递器发送到 RocketMQ。

## At-least-once Delivery

至少投递一次。消息可能重复发送，因此消费者必须基于 `eventId` 幂等处理。V1 不承诺 Exactly-once。

## Nacos

V1 的技术配置与服务发现基础设施，不是平台级国际化和字典业务服务。

## Platform Client

面向平台服务或后续业务系统的接入 SDK / Client，封装协议、鉴权、错误处理和兼容策略。

## API First

先定义 API 契约，再实现服务内部逻辑，用于稳定跨服务和跨系统边界。

## Contract First

先确认跨模块或跨系统契约，再实现或生成适配代码。禁止用内部 Entity 替代跨边界契约。

## ADR

Architecture Decision Record。用于记录重要设计问题的背景、备选方案、最终决策及其影响。

## Architecture Freeze

架构冻结。表示当前版本的核心产品边界、服务边界、安全模型、部署模型和关键数据流已经形成基线。重大变更必须通过 ADR 或正式变更记录处理。

## Self-hosted

由企业在自有基础设施或受控环境中部署和运行产品，而不是必须依赖第三方 SaaS 服务。

## V1

Synapse Platform 第一阶段产品文档和交付基线。具体功能、部署和验收范围由正式产品范围及发布文档确认。
