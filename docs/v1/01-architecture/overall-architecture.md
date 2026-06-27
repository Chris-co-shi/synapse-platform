# Synapse Platform V1 总体架构

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、核心研发、测试、实施和运维人员 |
| Purpose | 定义 V1 的总体运行结构、部署单元与数据隔离基线 |
| Scope | Management Console、Gateway、平台微服务及基础设施 |
| Status | Accepted |

相关文档：

- [V1 范围](../00-product/v1-scope.md)
- [服务边界](service-boundary.md)
- [安全架构](security-architecture.md)
- [通信架构](communication-architecture.md)

## 1. Runtime Topology

V1 保持独立微服务架构。

```text
Browser -> Management Console -> Gateway

P0 Services
  ├── IAM
  ├── Resource
  └── Audit

P1 Services
  ├── Config
  ├── File
  ├── Message
  └── Task

Infrastructure
  ├── PostgreSQL
  ├── Redis
  ├── Nacos
  └── RocketMQ
```

Redis 是 Opaque Access Token 授权快照的 P0 安全依赖。RocketMQ 是 Outbox 和 Audit 可靠闭环的默认基础设施。

V1 不建设独立 Monitor 微服务。基础状态由 Actuator、服务健康检查、Compose healthcheck 和管理端展示提供。

## 2. Service Summary

| Service | Responsibility |
| --- | --- |
| Gateway | 统一入口、Token 快照验证、路由、Header 清理和 GatewayProof |
| IAM | 身份、认证、授权关系、Token、Client 和 Redis 快照生命周期 |
| Resource | 应用、菜单、按钮、API、权限码、资源树和导航 |
| Audit | 操作日志、安全日志、审计日志和幂等事件消费 |
| Config | 国际化资源、字典类型和字典项 |
| File | 文件元数据和基础文件能力 |
| Message | 消息、模板和发送记录 |
| Task | 任务调度和执行记录 |

Gateway 不注入用户、角色或权限 Header。Gateway 和下游服务分别验证 Redis 授权快照。

## 3. Engineering Structure

```text
synapse-xxx-platform
├── synapse-xxx-api
├── synapse-xxx-client
└── synapse-xxx-server
```

其他服务和业务系统不得依赖另一个服务的 `server` 模块。

## 4. Communication Baseline

- 即时查询和命令使用 HTTP API / Client；
- 用户调用传播原始用户 Token；
- 后台和纯服务调用使用 Client Credentials；
- IAM 保存授权前同步批量校验 Resource 权限码；
- 关键事件使用本地 Outbox + RocketMQ；
- 事件采用 At-least-once + `eventId` 幂等；
- 非幂等命令默认不自动重试；
- 不使用共享数据库或分布式大事务完成服务集成。

## 5. Data Ownership

| Service | Owned Data |
| --- | --- |
| IAM | 用户、组织、角色、授权关系、Client 和会话 |
| Resource | 应用、菜单、按钮、API 和权限码目录 |
| Audit | 操作日志、安全日志和审计记录 |
| Config | 国际化资源和字典 |
| File | 文件元数据和存储状态 |
| Message | 消息、模板和发送记录 |
| Task | 任务定义、调度和执行记录 |

Redis 授权快照由 IAM 管理生命周期，其他服务只读验证。

## 6. PostgreSQL Isolation

V1 允许服务共用一个 PostgreSQL 实例，但每个服务必须使用：

- 独立 Schema；
- 独立数据库账号；
- 独立 Flyway migration；
- 独立 `flyway_schema_history`。

Schema 包括：`iam`、`resource`、`audit`、`config`、`file`、`message` 和 `task`。

禁止跨 Schema 查询、跨服务数据库外键以及共享 Entity、Mapper、Repository。

## 7. Docker Compose

V1 默认编排：

- Management Console、Gateway；
- IAM、Resource、Audit；
- Config；
- 已完成的 File、Message、Task；
- PostgreSQL、Redis、Nacos、RocketMQ。

Redis 需要持久化、healthcheck 和禁止淘汰授权快照。RocketMQ 需要支持 Outbox 积压恢复。

## 8. Degradation

- IAM 故障：新登录和刷新不可用，已有有效快照继续验证；
- Resource 故障：导航和授权修改不可用，已有快照鉴权继续；
- Audit/RocketMQ 故障：主流程继续，事件保留在 Outbox；
- Redis 故障：只允许已缓存的低风险读取最多 30 秒，其余失败关闭；
- Config 故障：维护不可用，可使用已缓存国际化和字典；
- P1 其他服务故障：不影响核心身份和权限闭环。

## 9. Constraints

V1 禁止：

- 改为模块化单体；
- 服务直接访问其他服务数据；
- 默认使用分布式大事务；
- 所有交互强制事件化；
- Gateway 通过后下游跳过认证；
- Gateway 注入身份权限 Header；
- 每请求同步调用 IAM 查询 Token；
- 把 Kubernetes 作为 V1 必交付方式。

## 10. Next Documents

1. `data-architecture.md`；
2. API、事件、日志和数据库规范；
3. Docker Compose 部署设计；
4. 各服务详细设计。
