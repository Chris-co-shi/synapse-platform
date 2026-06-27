# Synapse IAM 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

IAM 承载平台身份、认证、授权关系、会话、Opaque Token、Client 和 Redis 授权快照生命周期。Framework OAuth2/Security 只提供通用技术能力。

## 2. 子模块边界

- `synapse-iam-api`：稳定调用契约，不暴露数据库 Entity。
- `synapse-iam-client`：IAM 调用适配，只依赖对应 api。
- `synapse-iam-server`：IAM 启动入口和业务实现，允许使用 WebMVC 与数据访问能力。
- IAM 启动类必须位于 `com.indigo.synapse.iam` 根包，禁止放入 `bootstrap` 子包。

## 3. V1 安全模型

- Access Token 使用高熵 Opaque Token。
- IAM 对 Token 计算安全摘要，并在 Redis 保存授权快照。
- 原始 Access Token 不写入数据库、Redis value、日志或审计 payload。
- Refresh Token 使用 Opaque Token，数据库只保存安全摘要。
- Refresh Token 每次刷新必须 rotation，并检测旧 Token 重放。
- 用户和 Client Credentials 使用统一的 Opaque Token + Redis 快照机制，通过 principal type 区分。
- RS256 只用于 OIDC ID Token，不用于 Access Token。
- IAM 负责授权快照创建、更新、撤销和过期，不允许其他服务写入快照。
- 用户、角色或权限变化后必须更新或撤销相关快照。
- Access Token 默认 15 分钟，Refresh Token 空闲 7 天，会话最长 30 天；修改默认值需要安全评审。

## 4. Resource 协作

- IAM 只保存稳定 `permission_code`，不保存 Resource 内部主键。
- 角色授权前同步调用 Resource 批量校验权限码。
- 提交必须包含 `catalogVersion`。
- 单批最多校验 1000 个权限码，超过时分批；全部批次成功后才能保存授权关系。
- Resource 不可用、版本冲突或权限无效时，授权修改失败关闭。
- 运行时接口鉴权不得同步调用 Resource。

## 5. 允许依赖

client/server 可依赖对应 api；server 可按 Framework BOM 引用必要技术模块。

IAM 可以通过正式 Resource Client 执行权限码批量校验，但禁止依赖 Resource Server 或访问 Resource 数据库。

## 6. 禁止事项

- api/client 禁止包含持久化实现。
- client 禁止依赖 server 或绕过服务接口访问数据库。
- server 禁止依赖自己的 client 或其他平台 server。
- 不把 IAM 业务反向写入 Framework。
- 不签发 JWT Access Token。
- 不在 Token 中携带菜单、按钮或页面路由。
- 不在日志中记录密码、Access Token、Refresh Token、私钥或 Client Secret。
- 不传播或信任客户端可伪造的用户、角色和权限 Header。
- 不使用 Noop 或仅声明依赖冒充真实会话撤销、重放检测或授权快照能力。
- 新增公开类型和方法必须提供完整中文 Javadoc；配置属性必须提供中文说明和 Configuration Metadata。

## 7. 可靠审计

登录、刷新、退出、重放检测、用户状态、角色授权、权限回收、Client 和凭据变化必须在本地事务中写入 Outbox，由 RocketMQ 可靠发送给 Audit。

Audit 或 RocketMQ 短时不可用不能导致事件静默丢失。

## 8. 验证命令

```bash
mvn -pl :synapse-iam-api,:synapse-iam-client,:synapse-iam-server -am test
```

测试至少覆盖：

- Opaque Access Token 和 Redis 授权快照；
- Refresh Token rotation、并发刷新和 reuse detection；
- 用户与 Client principal type；
- Resource 批量校验和 `catalogVersion` 冲突；
- 授权变化后的快照更新与撤销；
- Redis 故障策略；
- Outbox 与业务事务原子性。

## 9. 必读文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [服务边界](../docs/v1/01-architecture/service-boundary.md)
- [安全架构](../docs/v1/01-architecture/security-architecture.md)
- [通信架构](../docs/v1/01-architecture/communication-architecture.md)
- [ADR-003](../docs/v1/99-adr/ADR-003-token-session-and-trust-boundary.md)
- [ADR-004](../docs/v1/99-adr/ADR-004-service-communication-and-outbox.md)
