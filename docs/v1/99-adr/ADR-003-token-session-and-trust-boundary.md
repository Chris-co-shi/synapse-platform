# ADR-003：Token、会话与可信入口模型

## Status

Accepted

> 2026-06-30 状态补充：当前实现符合本文的 Opaque Access Token、Redis 授权快照、GatewayProof 和 Opaque Refresh Token rotation 方向，但 reuse detection 仍存在已知缺陷，尚未完成 token family 整体撤销。OAuth2/OIDC 标准入口仍为计划实现。

## Context

Synapse Platform V1 需要同时满足：

- 管理端用户和平台 Client 的统一认证；
- 权限变化与会话撤销快速生效；
- 避免把大量角色、权限和资源元数据写入 Token；
- Gateway 与所有下游服务独立执行认证；
- Refresh Token 安全续期和重放检测；
- 防止客户端伪造 Gateway 身份信息；
- Redis 短暂故障时提供有限、明确的降级边界。

## Decision

### Opaque Access Token

V1 使用高熵 Opaque Access Token，不使用 JWT Access Token。

Token 本身只是不透明 Bearer 字符串。IAM 对 Token 计算安全摘要，并在 Redis 中保存授权快照。

授权快照至少包含主体、Client、会话、角色、权限、issuer、audience、授权版本、签发时间、过期时间和状态。

Gateway 和所有 Resource Server 通过 Framework 统一适配直接查询 Redis 快照，不在每个请求中同步调用 IAM introspection 接口，也不查询 IAM 数据库。

用户和 Client Credentials 使用同一套 Opaque Token + Redis 快照机制，并通过 `principal_type=USER/CLIENT` 区分。

### Token Lifetime

V1 默认值：

- Access Token：15 分钟；
- Refresh Token 空闲有效期：7 天；
- 最大会话时长：30 天；
- GatewayProof 有效窗口：60 秒；
- Redis 故障本地降级窗口：30 秒。

这些值可配置，但生产修改需要安全评审。

### Refresh Token

V1 使用高熵 Opaque Refresh Token：

- IAM 数据库只保存安全摘要；
- 每次刷新执行 rotation；
- 目标要求：旧 Token 重放会撤销对应 token family 和会话；
- 刷新重新计算角色和权限，并替换 Redis 授权快照；
- 并发刷新通过原子事务保证只有一次成功。

当前已知缺陷：旧 Refresh Token 重放请求本身返回 401，但 successor Refresh Token 仍可继续 refresh，同一个 token family 中仍存在 ACTIVE session，family 尚未整体撤销。因此 reuse detection 当前只能标记为部分实现。

### Revocation

Access Token 通过删除或禁用 Redis 授权快照立即撤销，不使用 JWT `jti` denylist。

用户禁用、角色权限变化、敏感权限回收、Client 禁用、Refresh Token 重放和安全人员强制下线都必须更新或删除相关快照。

### Redis Failure

Redis 是 P0 安全基础设施。正常请求以 Redis 快照为权威来源。

服务可以保存最近一次 Redis 成功验证的快照，但只用于故障降级：

- 已缓存、低风险、只读 GET 最多继续 30 秒；
- 登录、刷新、退出、写操作、管理操作和敏感操作立即返回 503；
- 未缓存 Token 返回 503；
- 30 秒后所有受保护请求失败关闭。

Docker Compose 中 Redis 开启 AOF、持久化 Volume、healthcheck、自动重启和 `noeviction` 策略。

### Gateway Boundary

Gateway：

- 验证 Opaque Access Token 的 Redis 授权快照；
- 原样透传 Bearer Token；
- 清理外部 GatewayProof Header；
- 签发 GatewayProof；
- 不注入用户、角色或权限 Header；
- 不执行普通业务权限；
- 不保留 `gateway:admin` permission 例外。

下游服务：

- 再次验证 Redis 授权快照；
- 验证 GatewayProof；
- 执行本服务权限和资源授权。

Opaque Token 授权快照证明主体与权限，GatewayProof 证明请求经过可信 Gateway，二者不能互相替代。

### OIDC and RS256

RS256 只用于计划中的 OIDC ID Token，不用于 Access Token。

OIDC Discovery、ID Token、UserInfo 和相关 JWK 生命周期当前未实现。实现后 IAM 管理 RSA 私钥和 `kid`，JWK 端点只发布公钥。开发、测试、beta 和生产密钥相互隔离，生产密钥通过 Docker Secret、受限挂载或外部 Secret 系统注入。

每个环境使用唯一 IAM issuer，V1 统一 audience 为 `synapse-platform`。

### Management Console

- Access Token 只保存在内存；
- Refresh Token 保存在 `sessionStorage`；
- 不使用 Cookie 或 `localStorage` 保存平台登录凭据；
- 页面刷新可以恢复会话；
- 关闭标签页后需要重新登录。

### Login and CORS

- 15 分钟内连续登录失败 5 次后锁定 15 分钟；
- 管理员可以手动解锁；
- 按账号和来源 IP 双维度限流；
- CORS 由 Gateway 维护明确 Origin 白名单；
- 禁止 `*`，且因为不使用 Cookie，不启用 credentials。

### Registration Boundary

V1 不提供普通用户公开自主注册。首个管理员通过受控 bootstrap 初始化，后续用户由管理员创建；邀请激活可在后续评估。

OAuth Client 需要管理员登记、修改、禁用和 Secret 轮换。V1 不实现公开 Dynamic Client Registration。

## Alternatives

### JWT Access Token

可以离线验证，但权限变化和会话撤销需要 denylist 或等待 Token 过期，且大量权限会增大 Token 体积，因此 V1 不采用。

### OAuth2 Introspection per Request

由每个服务同步调用 IAM 可以集中验证，但会让 IAM 成为所有请求的网络单点和性能瓶颈，因此不采用。

### JWT Refresh Token

不利于安全摘要存储、rotation 和重放检测，因此不采用。

### Gateway Identity Headers

容易被伪造或误信任，并诱导下游跳过 Token 验证，因此禁止采用。

### Redis Failure Immediate Full Shutdown

安全性最严格，但短暂 Redis 抖动会立即中断全部低风险读取。V1 采用最多 30 秒、只读且仅限已缓存 Token 的有限降级。

## Consequences

正向影响：

- 权限变化和会话撤销可以快速生效；
- Token 不暴露主体、角色和权限数据；
- 用户与服务身份使用统一验证模型；
- Gateway 与下游保持独立认证和授权；
- Redis 短暂故障有明确且受限的降级方案。

代价：

- Redis 成为 P0 运行时安全依赖；
- 每个受保护请求需要读取 Redis；
- Framework 需要提供统一 Opaque Token 快照适配；
- 必须建设 Redis 持久化、恢复、容量和故障演练；
- 30 秒只读降级存在明确接受的短暂旧授权窗口；
- 需要完整的跨服务安全集成测试。

## References

- [`../01-architecture/security-architecture.md`](../01-architecture/security-architecture.md)
- [`../01-architecture/communication-architecture.md`](../01-architecture/communication-architecture.md)
- [`../01-architecture/service-boundary.md`](../01-architecture/service-boundary.md)
- [`ADR-002-iam-resource-boundary.md`](ADR-002-iam-resource-boundary.md)
