# ADR-003：Token、会话与可信入口模型

## Status

Accepted

## Context

Synapse Platform V1 需要同时满足：

- 管理端用户登录和权限表达；
- Gateway 与下游服务的一致认证；
- Refresh Token 安全续期；
- 退出、禁用和敏感权限回收后的会话撤销；
- 防止客户端伪造 Gateway 身份信息；
- 服务之间保持独立授权执行。

## Decision

### Access Token

V1 使用 IAM 签发的非对称签名 JWT Access Token。

Access Token 至少表达标准时间和主体 claims、唯一 `jti`、token type、principal type、Client、角色和权限码快照。

实际 claim 名称与 authority 映射以 Synapse Framework 协议实现为准。

Access Token 不保存菜单、按钮、页面路由或资源展示元数据。

### Refresh Token

V1 使用高熵 Opaque Refresh Token：

- IAM 只保存安全摘要；
- 每次刷新执行 rotation；
- 旧 Token 重放会撤销对应会话族；
- 刷新时重新计算当前角色和权限；
- 并发刷新必须通过原子事务控制。

### Revocation

Access Token 通过 `jti` denylist 支持立即撤销，Refresh Token 通过 IAM 会话状态撤销。

用户禁用、敏感权限回收、Client 禁用、Refresh Token 重放和安全人员强制下线属于立即撤销场景。

Gateway 和所有受保护服务必须执行一致的 denylist 检查。

### Gateway Boundary

Gateway：

- 验证入口 Access Token；
- 原样透传 Bearer Token；
- 清理外部 GatewayProof Header；
- 签发 GatewayProof；
- 不注入用户、角色或权限 Header；
- 不执行普通业务权限。

下游服务：

- 再次验证 Access Token；
- 验证 GatewayProof；
- 执行本服务权限和资源授权。

JWT 证明主体身份，GatewayProof 证明请求经过可信 Gateway，二者不能互相替代。

### Runtime Authorization

运行时授权主要依据已验证 Access Token 或经确认的授权快照。

每个业务请求不得同步调用 IAM 和 Resource 查询权限。

## Alternatives

### Stateful Access Token

每次请求回查 IAM 会话可以立即反映权限变化，但会使 IAM 成为所有请求的运行时单点和性能瓶颈，因此不采用。

### JWT Refresh Token

无法方便实现安全摘要存储、rotation 和重放检测，因此 V1 不采用。

### Gateway 注入身份 Header

客户端和中间链路容易伪造或误信任，且会导致下游跳过 Token 验证，因此禁止采用。

### 只验证 JWT，不验证可信入口

无法证明请求是否经过统一 Gateway，也无法防止内部服务端口被绕过，因此 V1 保留 GatewayProof。

## Consequences

正向影响：

- Gateway 和下游拥有一致的认证协议；
- 下游服务保持独立授权能力；
- Refresh Token 可轮换并检测重放；
- 高风险变更可以立即撤销；
- 不依赖可伪造身份 Header。

代价：

- Gateway 和所有 Resource Server 都需要 denylist 与 GatewayProof 适配；
- Redis 或其他撤销存储成为安全基础设施；
- Token claim、TTL、密钥和浏览器存储策略必须严格治理；
- 需要完整的跨服务安全集成测试。

## References

- [`../01-architecture/security-architecture.md`](../01-architecture/security-architecture.md)
- [`../01-architecture/service-boundary.md`](../01-architecture/service-boundary.md)
- [`ADR-002-iam-resource-boundary.md`](ADR-002-iam-resource-boundary.md)
