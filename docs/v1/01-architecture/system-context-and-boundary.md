# Synapse Platform 系统上下文与边界

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、核心研发、测试、实施和运维人员 |
| Purpose | 说明 Synapse Platform 在企业数字化体系中的位置、职责和边界 |
| Scope | Platform 与企业用户、业务系统、外部系统及 Synapse Framework 的关系 |
| Status | Draft |

## 1. System Context

Synapse Platform 位于企业数字化体系的公共能力层，为多个自治业务系统提供统一的平台能力。

```text
企业用户 / 平台管理员 / 开发团队 / 第三方系统
        ↓
入口层：LB / WAF / Reverse Proxy / Gateway
        ↓
Synapse Platform
        ├── IAM
        ├── Audit / Log
        ├── Monitor
        ├── Task
        ├── File
        ├── Message
        ├── Config
        └── Integration
        ↓
自治业务系统
        ├── MES
        ├── WMS
        ├── QMS
        ├── EMS
        ├── MOM
        ├── ERP 周边
        ├── IoT / Edge
        └── 第三方业务系统
```

Platform 不位于某个业务系统内部，也不是所有业务系统的父工程。它是企业 IT 的公共数字化基座。

## 2. Platform Boundary

Synapse Platform 负责提供可运行的平台能力，包括：

- 统一身份和访问管理；
- OAuth 2.0 / OpenID Connect 基础能力；
- RBAC 权限模型；
- 统一入口；
- 日志、审计、监控与可观测性；
- 分布式任务和调度；
- 文件、消息、配置等共享服务；
- 外部系统接入和集成支撑；
- 平台部署、运维和交付基线。

具体能力是否进入 V1，必须由产品范围或模块设计单独确认。

Platform 不负责：

- 承载 MES、WMS、QMS、EMS、MOM 等业务领域模型；
- 保存业务系统核心领域数据；
- 替业务系统执行业务流程和业务规则；
- 让业务系统依赖 Platform Server 内部实现；
- 通过共享数据库完成系统集成；
- 把所有业务系统纳入同一个 Platform 工程或发布单元。

## 3. Relationship with Synapse Framework

Synapse Framework 提供通用技术能力，Synapse Platform 组合这些技术能力形成可运行的平台产品。

```text
Synapse Platform -> may depend on Synapse Framework
Synapse Framework -> must not depend on Synapse Platform
```

Framework 不承载 Platform 业务；Platform 不把自己的业务模型反向下沉到 Framework。

## 4. Relationship with Business Systems

业务系统保持自治，并拥有自己的：

- 领域模型；
- 数据库；
- 业务规则；
- 业务流程；
- 发布节奏；
- 运行边界。

业务系统可以通过以下方式接入 Platform：

- Gateway；
- OAuth 2.0 / OIDC；
- Access Token 与 Resource Server；
- 权限码和授权信息；
- Platform Client / SDK；
- OpenAPI / Contract；
- MQ / Event；
- Webhook；
- Connector / Adapter；
- 日志、审计和 Trace 接入。

禁止通过共享数据库、共享 Entity、共享 Mapper 或复制 Platform 内部实现完成接入。

## 5. Control Plane and Business Data Plane

Synapse Platform 提供企业数字化公共控制能力，例如：

- 用户、角色和权限；
- OAuth2 Client 与令牌；
- Gateway 路由和入口策略；
- 审计策略；
- 任务调度策略；
- 消息模板；
- 文件存储策略；
- 集成连接器配置；
- 平台运行状态和监控入口。

真实业务数据面由业务系统掌握。

MES 的生产订单、WMS 的库存、QMS 的检验单、EMS 的能耗数据和 ERP 的业务单据不进入 Platform 核心数据库。

Platform 可以保存平台自身的运行数据，例如操作日志、审计日志、任务执行记录、文件元数据、消息记录和集成调用记录。这些数据属于平台运行域，不属于业务系统领域数据。

## 6. V1 Foundation Loop

当前确认的 V1 基础闭环为：

```text
平台初始化身份与权限资源
  ↓
用户登录 IAM
  ↓
IAM 完成令牌签发与刷新
  ↓
Client 通过统一入口访问资源服务
  ↓
资源服务完成令牌验证与权限兜底
  ↓
日志、审计和 Trace 记录关键链路
```

该闭环是当前产品目标，不表示所有环节已经完成。完成状态必须由测试和发布记录证明。

## 7. Boundary Decision Checklist

判断一个能力是否应该进入 Platform：

| Question | Platform Signal | Business Signal |
| --- | --- | --- |
| 是否会被多个业务系统复用？ | 是 | 否 |
| 是否不依赖具体业务流程？ | 是 | 否 |
| 是否属于身份、安全、入口、日志、审计、监控、任务、消息、文件或集成等公共能力？ | 是 | 否 |
| 是否需要读取业务系统私有数据库才能完成核心逻辑？ | 否 | 是 |
| 是否会让 Platform 承载业务领域模型？ | 否 | 是 |

只满足“技术上可以复用”不足以进入 Platform；还必须具备明确的企业产品价值和长期治理必要性。
