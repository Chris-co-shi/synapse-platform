# Synapse Resource 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Resource 是 V1 P0 的资源与权限目录中心，负责应用、菜单、页面、按钮、API、权限码、授权资源树和当前用户导航。

Resource 不负责用户、角色、授权关系、Token 签发或其他服务的数据权限执行。

## 2. 子模块边界

- `synapse-resource-api`：保存稳定资源目录、批量校验和导航契约。
- `synapse-resource-client`：提供 Resource 调用适配，只依赖 api。
- `synapse-resource-server`：承载资源目录业务、持久化和启动入口。
- Resource 启动类必须位于 `com.indigo.synapse.resource` 根包，禁止放入 `bootstrap` 子包。

## 3. V1 核心规则

- 权限码使用稳定、全局唯一的 `{domain}:{resource}:{action}` 格式。
- Resource 是权限码定义的事实来源。
- IAM 只保存 `permission_code`，不得依赖 Resource 内部主键。
- 授权资源树必须支持按应用和父节点懒加载。
- Resource 必须提供 `catalogVersion`。
- IAM 授权前通过正式 Client 批量校验权限码。
- 单批最多校验 1000 个权限码。
- 批量校验必须基于唯一索引和集合查询，禁止逐个远程调用或逐条 SQL。
- 校验内容至少包括存在、启用、可分配、目标应用和目录版本。
- 当前用户导航根据已验证 Redis 授权快照中的权限码生成。
- 菜单和按钮过滤只改善体验，不能代替服务端鉴权。

## 4. 安全边界

- Resource 作为受保护服务，独立验证 Opaque Access Token 的 Redis 授权快照。
- 对经 Gateway 的请求验证 GatewayProof。
- 禁止依赖 Gateway 注入的用户、角色或权限 Header。
- 运行时导航和接口鉴权不得同步调用 IAM 查询权限。
- Redis 故障降级只能遵守安全架构中的 30 秒低风险只读边界。

## 5. 允许依赖

client -> api，server -> api，server 可按需使用 Framework 正式技术模块。

IAM 可以依赖 Resource Client；Resource 不反向依赖 IAM Server，也不访问 IAM 数据库。

## 6. 禁止事项

- 禁止 api -> client/server、client -> server。
- 禁止 server 依赖自己的 client 或其他平台 server。
- 禁止跨服务共享持久化类型或访问其他服务数据库。
- 禁止保存用户角色关系和角色权限关系。
- 禁止签发 Access Token 或 Refresh Token。
- 禁止把 MES、WMS 等业务资源数据纳入 Resource 目录模型。
- 权限码废弃时禁止直接物理删除并破坏历史审计。

## 7. 可靠审计

应用、菜单、API、权限码新增、修改、禁用和废弃必须在本地事务中写入 Outbox，并通过 RocketMQ 可靠发送给 Audit。

## 8. 验证命令

```bash
mvn -pl synapse-resource-platform -am test
```

测试至少覆盖：

- 资源树懒加载；
- 批量权限码校验；
- 1000 条边界和分批校验；
- `catalogVersion` 冲突；
- 导航过滤；
- Opaque Token 快照和 GatewayProof；
- Outbox 原子性。

## 9. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [服务边界](../docs/v1/01-architecture/service-boundary.md)
- [安全架构](../docs/v1/01-architecture/security-architecture.md)
- [通信架构](../docs/v1/01-architecture/communication-architecture.md)
- [ADR-002](../docs/v1/99-adr/ADR-002-iam-resource-boundary.md)
- [ADR-004](../docs/v1/99-adr/ADR-004-service-communication-and-outbox.md)
