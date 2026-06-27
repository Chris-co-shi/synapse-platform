# Synapse Platform 系统上下文与边界

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、核心研发、测试、实施和运维人员 |
| Purpose | 说明 Synapse Platform 在企业数字化体系中的位置、职责和边界 |
| Scope | Platform 与企业用户、业务系统、外部系统及 Synapse Framework 的关系 |
| Status | Accepted |

## 1. System Context

Synapse Platform 位于企业数字化体系的公共能力层，为多个自治业务系统提供统一平台能力。

```text
企业用户 / 平台管理员 / 开发团队
        ↓
Management Console / External Client
        ↓
Gateway
        ↓
Synapse Platform V1
        ├── IAM
        ├── Resource
        ├── Audit
        ├── Config
        ├── File
        ├── Message
        └── Task
        ↓
后续接入的自治业务系统
        ├── MES / WMS / QMS / EMS / MOM
        ├── ERP 周边
        ├── IoT / Edge
        └── 第三方业务系统
```

Gateway、IAM、Resource、Audit 是 V1 P0。Config、File、Message、Task 是 V1 P1。

独立 Monitor、Integration、Workflow、MDM 和 Report 不进入 V1，不能因为仓库存在模块骨架就描述为已交付能力。

Platform 不位于某个业务系统内部，也不是所有业务系统的父工程。它是企业 IT 的公共数字化基座。

## 2. Platform Boundary

V1 负责：

- 统一身份、认证和访问管理；
- OAuth 2.0 / OpenID Connect；
- Opaque Access Token 和 Redis 授权快照；
- RBAC 和资源权限目录；
- Gateway 统一入口；
- 操作日志、安全日志和审计日志；
- Config 国际化资源和字典；
- File、Message、Task 的最小平台能力；
- Docker Compose 部署、运行和故障排查基线。

长期能力方向可以包括监控、集成、工作流、主数据和报表，但进入具体版本前必须单独确认产品范围。

Platform 不负责：

- 承载 MES、WMS、QMS、EMS、MOM 等业务领域模型；
- 保存业务系统核心领域数据；
- 替业务系统执行业务流程和业务规则；
- 让业务系统依赖 Platform Server 内部实现；
- 通过共享数据库完成系统集成；
- 把所有业务系统纳入同一个 Platform 工程或发布单元。

## 3. Relationship with Synapse Framework

Synapse Framework 提供通用技术能力，Synapse Platform 组合这些能力形成可运行的平台产品。

```text
Synapse Platform -> may depend on Synapse Framework
Synapse Framework -> must not depend on Synapse Platform
```

Framework 可以提供 Opaque Token、Security、Outbox、Messaging 等技术抽象，但不承载用户、角色、资源目录、字典或其他 Platform 业务模型。

## 4. Relationship with Business Systems

业务系统保持自治，并拥有自己的：

- 领域模型；
- 数据库；
- 业务规则；
- 业务流程；
- 发布节奏；
- 运行边界。

V1 不以真实业务系统接入作为完成条件。平台自身闭环稳定后，业务系统可以通过以下方式接入：

- Gateway；
- OAuth 2.0 / OIDC；
- Opaque Access Token 与授权快照；
- 权限码；
- Platform Client / SDK；
- OpenAPI / Contract；
- MQ / Event；
- Webhook；
- Connector / Adapter；
- 日志、审计和 Trace 接入。

禁止通过共享数据库、共享 Entity、共享 Mapper 或复制 Platform 内部实现完成接入。

## 5. Control Plane and Business Data Plane

Synapse Platform 提供公共控制面能力：

- 用户、角色和授权关系；
- 应用、菜单、API 和权限码目录；
- Token、Client 和会话；
- Gateway 入口策略；
- 国际化和字典；
- 操作、安全和审计记录；
- 文件、消息和任务管理；
- 基础运行状态。

真实业务数据面由业务系统掌握。

生产订单、库存、检验单、能耗和 ERP 单据不进入 Platform 核心数据库。Platform 只保存自身运行与管理数据。

## 6. V1 Foundation Loop

```text
Resource 注册资源与权限码
  ↓
IAM 创建用户、角色和授权关系
  ↓
用户登录，IAM 创建 Redis 授权快照并返回 Opaque Token
  ↓
Client 经 Gateway 访问平台服务
  ↓
Gateway 和目标服务分别验证授权快照
  ↓
目标服务执行 permission 和资源授权
  ↓
关键事件写入 Outbox，经 RocketMQ 到达 Audit
  ↓
Config / File / Message / Task 完成各自最小闭环
```

完成状态必须由测试和发布记录证明，设计文档本身不代表能力已经实现。

## 7. Boundary Checklist

| Question | Platform Signal | Business Signal |
| --- | --- | --- |
| 是否会被多个业务系统复用？ | 是 | 否 |
| 是否不依赖具体业务流程？ | 是 | 否 |
| 是否属于企业公共控制或共享服务？ | 是 | 否 |
| 是否需要读取业务系统私有数据库才能完成核心逻辑？ | 否 | 是 |
| 是否会让 Platform 承载业务领域模型？ | 否 | 是 |

只满足“技术上可以复用”不足以进入 Platform；还必须具备明确产品价值并经过范围确认。
