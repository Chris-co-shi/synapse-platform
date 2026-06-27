# Synapse Audit 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Audit 是 V1 P0 的操作日志、安全日志和审计日志服务。Framework `synapse-audit` 只提供审计技术契约和适配能力。

Audit 主要消费各来源服务通过本地 Outbox + RocketMQ 可靠发送的结构化事件。

## 2. 子模块边界

- `synapse-audit-api`：保存稳定查询、事件和错误契约。
- `synapse-audit-client`：提供调用适配，只依赖 api。
- `synapse-audit-server`：承载事件消费、幂等持久化、查询和启动入口。
- Audit 启动类必须位于 `com.indigo.synapse.audit` 根包，禁止放入 `bootstrap` 子包。

## 3. Delivery Semantics

- V1 使用 At-least-once delivery，不承诺 Exactly-once。
- 每个事件必须包含稳定 `eventId` 和 `eventVersion`。
- Audit 必须使用 `eventId` 唯一约束或等价机制幂等去重。
- 重复消息不能产生重复审计记录。
- 消费成功后再确认消息。
- 消费失败执行有限重试；超过上限进入死信或人工补偿。
- 禁止静默丢弃失败事件。

## 4. 边界

Audit 负责：

- 操作日志、安全日志和审计日志持久化；
- 事件幂等和版本兼容；
- 失败、重试、死信和补偿状态可见性；
- 管理端查询和检索；
- 记录主体、动作、对象、结果、时间和 traceId。

Audit 不负责：

- 认证和授权判断；
- 反向调用 IAM 或 Resource 决定请求是否允许；
- 修改来源服务业务状态；
- 采集 stdout/stderr 普通运行日志；
- 与来源服务共享数据库事务。

## 5. 安全要求

- Audit 作为受保护服务，独立验证 Opaque Access Token 的 Redis 授权快照。
- 查询操作必须执行明确 permission。
- 禁止信任 Gateway 注入的身份和权限 Header。
- 禁止保存完整 Access Token、Refresh Token、密码、私钥、Client Secret 或 GatewayProof Secret。
- 敏感字段需要按日志规范脱敏。

## 6. 允许依赖

client/server -> api；server 可按需使用当前 Framework 正式模块、RocketMQ Client 和持久化组件。

## 7. 禁止事项

- 禁止 api/client 放置持久化实现。
- 禁止跨服务数据库访问。
- 禁止 server 依赖自己的 client 或其他 server。
- 禁止用同步 Audit 写入作为所有业务操作的硬依赖。
- 禁止因为消费重复而产生重复副作用。

## 8. 验证命令

```bash
mvn -pl synapse-audit-platform -am test
```

测试至少覆盖：

- 重复投递幂等；
- 不同事件版本兼容；
- 消费失败重试；
- 死信和人工重投；
- RocketMQ 暂停与恢复；
- 查询权限；
- 敏感信息不落日志。

## 9. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [服务边界](../docs/v1/01-architecture/service-boundary.md)
- [安全架构](../docs/v1/01-architecture/security-architecture.md)
- [通信架构](../docs/v1/01-architecture/communication-architecture.md)
- [ADR-004](../docs/v1/99-adr/ADR-004-service-communication-and-outbox.md)
