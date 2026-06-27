# Synapse Platform V1 通信架构

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、核心研发、测试、实施和运维人员 |
| Purpose | 定义 V1 的同步调用、身份传播、事件投递、依赖降级和通信可靠性规则 |
| Scope | Gateway、IAM、Resource、Audit、Config、File、Message、Task 及基础设施 |
| Status | Accepted |

本文定义 V1 服务通信基线。具体端点、Topic、超时数值和重试次数由模块设计确认，但不得违反本文边界。

## 1. Communication Principles

- 需要即时结果的查询和命令使用同步 HTTP API；
- 允许最终一致、需要可靠投递或不应阻塞主流程的事件使用 Outbox + RocketMQ；
- 用户请求传播原始用户 Access Token；
- 后台任务和纯服务调用使用 Client Credentials Access Token；
- 服务身份不得伪装成用户身份；
- V1 不引入 Token Exchange；
- 服务不得通过共享数据库或跨 Schema SQL 完成通信；
- 非幂等命令不得无条件自动重试；
- 不使用分布式大事务作为默认一致性方案。

## 2. Identity Propagation

### 2.1 User-initiated Calls

当同步调用需要保留用户主体和权限时，调用链传播原始用户 Opaque Access Token：

```text
Management Console
  -> Gateway
  -> Service A
  -> Service B
```

Service A 调用 Service B 前不得把用户身份转换为可伪造 Header，也不得自行生成“代理用户”Token。

下游服务必须独立验证 Token 授权快照，并执行自身权限和资源授权。

### 2.2 Service Calls

以下场景使用 Client Credentials：

- 后台任务；
- Outbox 投递器；
- 定时处理；
- 不代表具体用户的系统维护；
- 纯服务间调用。

Client Token 与用户 Token 使用相同的 Opaque Access Token + Redis 快照验证机制，并通过 `principal_type=CLIENT` 区分。

Client 只能获得明确配置的权限，不能默认继承平台管理员权限。

### 2.3 No Token Exchange in V1

V1 不支持用户 Token 交换为下游 Token。确实需要保留用户语义时传播用户 Token；不需要用户语义时使用服务自己的 Client Token。

## 3. Synchronous HTTP

同步 HTTP 适用于：

- 管理端查询和命令；
- 登录、刷新和退出；
- IAM 对 Resource 的权限码批量校验；
- 必须立即确认结果的服务操作。

每个同步 Client 必须定义：

- 连接和响应超时；
- 统一错误码；
- 身份与 traceId 传播；
- 是否允许重试；
- 下游不可用时的失败或降级行为。

超时具体数值在模块设计中确定。调用链必须保持有限，禁止形成无边界的串行依赖。

## 4. Retry and Idempotency

- 只读、幂等查询可以执行有限重试；
- 非幂等命令默认不自动重试；
- 需要重试的命令必须具备业务幂等键或等价防重机制；
- 认证、授权变更、Token 轮换等安全操作不能依赖客户端盲目重试；
- 重试必须有次数和时间上限，禁止无限重试；
- 超时后结果未知的命令必须支持状态查询或幂等重放。

## 5. IAM and Resource Validation

### 5.1 Resource Tree Loading

角色授权页面不一次加载所有应用的完整资源树。

Resource 应支持：

- 按应用加载；
- 按模块或父节点懒加载；
- 只返回授权页面所需字段；
- 缓存资源目录；
- 返回目录版本 `catalogVersion`。

### 5.2 Grant Submission

管理端只提交已选择的权限码和目录版本：

```json
{
  "catalogVersion": 27,
  "permissionCodes": [
    "iam:user:read",
    "iam:user:create"
  ]
}
```

IAM 同步调用 Resource 批量校验：

- 权限码是否存在；
- 是否启用；
- 是否允许分配；
- 是否属于目标应用；
- `catalogVersion` 是否仍然有效。

单批最多校验 1000 个权限码。超过时分批校验，但必须在全部批次校验成功后，才在 IAM 本地事务中保存完整授权关系。

Resource 必须基于权限码唯一索引执行集合查询，禁止逐个远程调用或逐条 SQL 校验。

### 5.3 Failure Rule

Resource 不可用、目录版本冲突或任一权限码无效时，IAM 拒绝新增或修改授权关系。

已有用户登录和接口鉴权不依赖 Resource 在线，因为 IAM 已保存合法权限码，运行时从 Redis 授权快照读取权限。

## 6. Outbox and RocketMQ

RocketMQ 是 V1 Docker Compose 默认基础设施。

关键审计和跨服务事件采用：

```text
Local Business Transaction
  -> Business Data
  -> Local Outbox Event
  -> Commit
  -> Outbox Publisher
  -> RocketMQ
  -> Consumer
  -> Idempotent Persistence
```

业务数据和 Outbox 记录必须在同一服务、同一数据库事务内提交。

### 6.1 Delivery Semantics

V1 采用：

> **At-least-once delivery + eventId idempotency**

不承诺 Exactly-once。

每个事件至少包含：

- `eventId`；
- `eventType`；
- `eventVersion`；
- `aggregateType`；
- `aggregateId`；
- `occurredAt`；
- `traceId`；
- `actorType`；
- `actorId`；
- `payload`。

消费者使用 `eventId` 唯一约束或等价机制去重。重复投递不能产生重复审计记录或重复业务副作用。

### 6.2 Failure Handling

- RocketMQ 暂时不可用时，业务事务可以成功，事件保留在本地 Outbox；
- Outbox Publisher 在恢复后继续投递；
- 消费失败执行有限重试；
- 超过重试上限进入死信或人工补偿流程；
- 失败事件必须可查询、可重投、可追踪；
- 禁止静默删除失败审计事件。

Audit 服务短时不可用不得阻塞登录和核心业务，但审计事件必须先可靠写入来源服务的 Outbox。

## 7. Audit Communication

操作日志、安全日志和审计日志中的关键结构化事件通过 Outbox + RocketMQ 发送给 Audit。

普通应用运行日志继续输出 stdout/stderr，并由部署环境负责采集；运行日志不通过 Audit 业务事件接口逐条发送。

以下事件至少采用可靠审计事件：

- 登录、刷新、退出和重放检测；
- 用户状态变化；
- 角色授权和权限回收；
- Resource 权限码变化；
- Config 国际化和字典变化；
- Client 与凭据安全变更；
- 关键管理操作和访问拒绝。

## 8. Dependency and Degradation

| Dependency Failure | V1 Behavior |
| --- | --- |
| IAM unavailable | 登录、刷新和新 Token 签发不可用；已有有效 Access Token 可继续按 Redis 快照验证 |
| Resource unavailable | 导航、资源维护和新增授权不可用；已有 Token 的接口鉴权继续运行 |
| Audit unavailable | 主流程继续；来源服务 Outbox 保留事件，恢复后补投 |
| RocketMQ unavailable | 主流程继续；Outbox 累积，恢复后补投；积压必须可观测 |
| Redis unavailable | 按安全架构执行最多 30 秒的低风险只读缓存降级；其余请求失败关闭 |
| Nacos unavailable | 已启动实例使用已加载配置继续运行；新注册、发现变化和配置刷新受影响 |
| Config unavailable | 国际化和字典维护不可用；允许使用已缓存配置，写操作失败 |
| File/Message/Task unavailable | 不影响 IAM、Resource、Audit 和核心权限闭环 |

## 9. Config Boundary

Nacos 与 Config 是不同能力：

```text
Nacos
  -> 技术配置、服务发现和环境参数

synapse-config-platform
  -> 平台级业务配置
```

Config 是 V1 P1 独立微服务。V1 最小闭环只包括：

- 国际化资源；
- 字典类型；
- 字典项；
- 变更审计；
- 必要的查询缓存与变更失效。

Config 不保存数据库密码、Redis 密码、GatewayProof Secret、RSA 私钥或 Nacos 技术配置。

## 10. Monitor Boundary

V1 不建设独立 Monitor 微服务。

基础运行可见性由以下能力组成：

- Spring Boot Actuator；
- 各服务健康检查；
- Docker Compose healthcheck；
- 管理端基础状态展示；
- 必要的状态聚合适配。

Prometheus、Grafana、完整 APM 和告警中心不属于 V1 必交付范围。

## 11. Gateway Routes

V1 默认只配置实际部署且进入当前版本的服务路由。

范围外或尚未交付的服务不保留活动默认路由。后续服务上线时通过明确配置增加路由。

Gateway 不设置业务角色或权限规则，不保留 `gateway:admin` permission 例外。Actuator 和运维端点通过网络隔离、端点暴露控制和运维安全措施保护。

## 12. Versioning and Compatibility

- HTTP API 和事件契约必须显式版本化；
- 事件新增字段应保持向后兼容；
- 消费者不得假设未知字段不存在；
- 破坏性事件变更使用新的 `eventVersion` 或事件类型；
- API DTO 和事件 DTO 不暴露数据库 Entity；
- Client 与 Server 的兼容范围在发布说明中记录。

## 13. Verification

通信架构至少需要以下自动化验证：

- 用户 Token 在多级同步调用中的传播与独立验证；
- Client Token 不能获得未授权用户权限；
- Resource 大批量权限校验与目录版本冲突；
- Outbox 与业务事务原子性；
- RocketMQ 重复投递和消费者幂等；
- RocketMQ/Audit 故障后的积压与恢复；
- Redis 故障降级和 30 秒上限；
- 非 V1 服务路由默认不可用；
- 非幂等请求不会被基础 Client 自动重试。
