# ADR-004：服务通信、Outbox 与 RocketMQ

## Status

Accepted

## Context

V1 需要同时满足：

- 用户请求中的主体和权限可以跨服务传播；
- 后台任务和纯服务调用拥有独立机器身份；
- IAM 与 Resource 在角色授权时保持权限目录一致；
- Audit 故障不能阻塞登录和核心业务；
- 审计事件不能静默丢失；
- 微服务之间不使用共享数据库或分布式大事务。

## Decision

### Identity Propagation

用户发起的同步调用传播原始用户 Opaque Access Token。

后台任务、Outbox 投递器、定时处理和纯服务调用目标使用 Client Credentials Opaque Access Token。当前标准 Client Credentials 尚未实现，该句描述 V1 目标。

服务身份不得伪装成用户身份。V1 不引入 Token Exchange。

### Synchronous HTTP

需要立即返回结果的查询和命令使用正式 HTTP API / Client。

IAM 保存角色权限关系前，同步调用 Resource 批量校验权限码。Resource 不可用、目录版本冲突或任一权限无效时，授权修改失败关闭。

资源树按应用和节点懒加载。提交时只提交已选择的权限码和 `catalogVersion`。单批最多校验 1000 个权限码，超过后分批校验，全部成功后才保存授权关系。

### Audit and Outbox

关键审计和跨服务事件使用本地 Outbox + RocketMQ。

业务数据与 Outbox 记录在同一服务、同一数据库事务中提交。RocketMQ 或 Audit 短时不可用时，主业务可以成功，事件保留在 Outbox，恢复后继续投递。

V1 采用：

> At-least-once delivery + eventId idempotency

不承诺 Exactly-once。

消费者必须按 `eventId` 去重。失败事件进入有限重试、死信或人工补偿流程，禁止静默删除。

### Infrastructure

RocketMQ 是 V1 Docker Compose 默认基础设施。

P0 登录和接口鉴权不得对 RocketMQ 形成同步运行时硬依赖。

### Retry

只读或幂等请求可以有限重试。非幂等命令默认不自动重试；确需重试时必须有幂等键或等价防重机制。

## Alternatives

### Audit Synchronous HTTP

能够立即感知写入结果，但 Audit 故障会扩散到登录、授权和业务操作，因此不采用为默认方案。

### Direct RocketMQ without Outbox

实现简单，但业务事务成功后消息发送失败会产生不可恢复的审计缺口，因此不采用。

### Exactly-once Delivery

会显著增加系统复杂度，且跨数据库和 MQ 难以形成真实端到端 Exactly-once。V1 采用可验证的 At-least-once 与消费者幂等。

### Shared Database Integration

会破坏服务数据所有权和独立演进能力，因此禁止采用。

## Consequences

正向影响：

- 用户和服务身份传播规则清晰；
- IAM 与 Resource 授权目录保持一致；
- Audit 和 RocketMQ 故障不会直接阻塞核心业务；
- 审计事件可以追踪、重试和补偿；
- 消费者可以安全处理重复消息。

代价：

- 每个产生可靠事件的服务需要 Outbox 表和投递器；
- 需要积压、重试、死信和人工补偿管理；
- 需要消费者幂等和事件版本治理；
- Resource 大树需要懒加载、版本控制和批量校验实现。

## References

- [`../01-architecture/communication-architecture.md`](../01-architecture/communication-architecture.md)
- [`../01-architecture/security-architecture.md`](../01-architecture/security-architecture.md)
- [`../01-architecture/service-boundary.md`](../01-architecture/service-boundary.md)
