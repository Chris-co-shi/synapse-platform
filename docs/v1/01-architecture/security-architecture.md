# Synapse Platform V1 安全架构

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、安全负责人、核心研发、测试、实施和运维人员 |
| Purpose | 定义 V1 的认证、Token、授权快照、可信入口、会话撤销和安全审计基线 |
| Scope | Management Console、Gateway、IAM、Resource、Audit、Config 及所有受保护微服务 |
| Status | Accepted |

本文定义 V1 必须达到的目标架构，不代表相关能力已经实现。实际完成状态必须由源码、自动化测试和发布记录证明。

## 1. Security Principles

- 默认拒绝，显式放行；
- Gateway 是统一入口，但不是唯一安全执行点；
- IAM 是身份、会话和授权快照的事实源；
- Resource 定义资源与权限目录；
- 每个受保护服务独立验证 Access Token；
- 每个服务执行自身接口、资源和数据权限；
- Management Console 和内部网络均不视为可信；
- Token、密码、私钥和 Client Secret 不进入日志；
- Redis 授权快照、GatewayProof 和权限检查失败时默认拒绝，只有明确的短时只读降级例外。

## 2. Trust Boundaries

```text
Browser / External Client
  ↓ HTTPS
Gateway
  ↓ Opaque Bearer Token + GatewayProof
IAM / Resource / Audit / Config / File / Message / Task
  ↓
PostgreSQL / Redis / Nacos / RocketMQ
```

V1 Docker Compose 默认只对外暴露 Management Console 和 Gateway 所需端口。平台微服务端口原则上只在内部网络可达。

内部网络隔离不能替代 Token 验证、GatewayProof 验证或服务端授权。

## 3. Access Token Model

### 3.1 Opaque Access Token

V1 使用 IAM 生成的高熵 Opaque Access Token。

Token 本身只是不可预测的 Bearer 字符串，不携带用户、角色、权限、菜单、路由或其他业务信息。

IAM 对原始 Token 执行安全摘要，并在 Redis 中保存授权快照。原始 Token 不写入数据库、Redis value、日志或审计 payload。

授权快照至少表达：

- token identifier 或 token hash；
- subject id；
- principal type；
- client id；
- session id；
- roles；
- permissions；
- issuer；
- audiences；
- authorization version；
- issued at；
- expires at；
- active / revoked 状态。

Redis key 的具体命名属于实现细节，由 IAM 和 Framework 设计确认。

### 3.2 Validation

Gateway 和每个受保护服务通过 Framework 统一适配验证 Access Token：

```text
Bearer Token
  -> 计算安全摘要
  -> 从 Redis 查询授权快照
  -> 校验状态、时间、issuer、audience 和 principal type
  -> 构建 Authentication 与 authorities
  -> 执行权限检查
```

V1 不在每个请求中调用 IAM introspection 接口，也不直接查询 IAM 数据库。

Redis 是运行时授权快照的权威来源。Framework 负责统一 Token 摘要、快照读取、校验和主体转换协议，Platform 不在各服务中重复实现。

### 3.3 User and Client Tokens

V1 至少区分：

- `USER`：用户通过管理端登录获得 Access Token；
- `CLIENT`：平台内部服务或可信机器客户端通过 Client Credentials 获得 Access Token。

用户 Token 和 Client Token 使用相同的 Opaque Token + Redis 快照机制，通过 `principal_type` 区分。

Client Token 不伪装成用户 Token，也不能默认继承平台管理员权限。

### 3.4 TTL

V1 默认值：

| Item | Default | Reason |
| --- | ---: | --- |
| Access Token | 15 分钟 | 限制 Bearer Token 泄露窗口，同时避免管理端过于频繁刷新 |
| Refresh Token 空闲有效期 | 7 天 | 兼顾管理端使用体验和长期凭据暴露风险 |
| 最大会话时长 | 30 天 | 即使持续刷新，也要求用户周期性重新认证 |
| GatewayProof 有效窗口 | 60 秒 | 容忍有限网络与时钟偏差，同时控制重放窗口 |
| Redis 故障本地降级窗口 | 30 秒 | 只覆盖短暂抖动，不把旧授权长期当作有效依据 |

所有值必须可配置，但生产环境修改需要经过安全评审。

## 4. Authorization Snapshot

Resource 是权限码定义的事实来源；IAM 是用户、角色和角色权限关系的事实来源。

用户登录、刷新或授权关系发生变化时，IAM 计算新的授权快照并写入 Redis。

运行时授权依据 Redis 快照中的权限码，而不是在每个请求中查询 IAM 或 Resource。

授权快照不包含：

- 菜单树；
- 页面路由；
- 按钮展示元数据；
- 权限说明文本；
- 密码、手机号、私钥或 Client Secret。

Resource 根据已验证主体的权限码生成当前用户导航，但菜单过滤不能代替服务端权限校验。

## 5. Refresh Token and Session

V1 使用高熵 Opaque Refresh Token。

Refresh Token：

- 明文只返回合法客户端；
- IAM 数据库只保存安全摘要；
- 绑定用户、Client、会话和过期时间；
- 每次成功刷新执行 rotation；
- 旧 Token 再次使用视为重放；
- 重放后撤销对应 token family 和会话；
- 并发刷新必须通过原子事务保证只有一次成功。

会话至少表达：

- session id；
- refresh token family id；
- subject id；
- client id；
- refresh token hash；
- issued / idle expires / absolute expires time；
- active / revoked / expired / reuse-detected 状态；
- replaced token reference；
- revoke reason 和时间。

## 6. Login, Refresh and Logout

### 6.1 Login

```text
Client -> Gateway -> IAM
  -> GatewayProof verification
  -> credential and account status verification
  -> role and permission calculation
  -> create Redis authorization snapshot
  -> issue Opaque Access Token + Opaque Refresh Token
  -> write reliable security audit Outbox event
```

登录失败对外使用统一错误，不泄露账号是否存在、禁用、锁定或密码错误。

失败策略：

- 15 分钟统计窗口；
- 连续失败 5 次后锁定 15 分钟；
- 管理员可以手动解锁；
- 按账号和来源 IP 双维度限流；
- 所有失败进入安全审计事件。

### 6.2 Refresh

```text
Client -> Gateway -> IAM
  -> validate GatewayProof
  -> hash and validate Refresh Token
  -> validate session and client
  -> detect reuse
  -> rotate Refresh Token
  -> recalculate roles and permissions
  -> replace Redis authorization snapshot
  -> issue new Opaque Access Token
  -> write reliable security audit event
```

刷新后旧 Access Token 快照应删除或标记无效。普通权限调整可以通过更新相关授权快照立即生效，不必等待 Token 自然过期。

### 6.3 Logout

登出至少执行：

- 撤销当前 Refresh Token 会话；
- 删除或禁用当前 Access Token 授权快照；
- 清理相应的本地授权缓存；
- 记录退出和撤销事件。

“退出当前会话”和“退出全部会话”是不同操作。

## 7. Immediate Revocation

以下变化必须立即更新或删除相关 Redis 授权快照：

- 用户被禁用或锁定；
- 角色或权限发生变化；
- 管理员或敏感角色被回收；
- 凭据泄露或主动强制下线；
- Refresh Token 重放；
- Client 被禁用；
- 安全人员执行全会话撤销。

如果存在本地应急缓存，撤销在异常情况下可能存在最多 30 秒的低风险只读窗口。写操作和敏感操作不得使用该降级缓存。

## 8. Redis Availability and Recovery

Redis 是 P0 安全基础设施。

Docker Compose 至少需要：

- 开启 AOF 持久化；
- 使用持久化 Volume；
- 配置 `maxmemory-policy=noeviction`；
- 配置 healthcheck 和自动重启；
- 提供数据目录、恢复和故障排查说明；
- 禁止授权快照因内存淘汰被静默删除。

客户端应使用有限次数快速重连和明确超时，不能无限等待或无限重试。

### 8.1 Normal Mode

Redis 正常时，每个受保护请求以 Redis 快照作为权威验证来源。

服务可以把最近一次成功验证的快照写入本地 Caffeine 缓存，但该缓存只用于 Redis 故障时的有限降级，不作为正常授权来源。

### 8.2 Degraded Mode

Redis 不可用时：

| Request Type | Behavior |
| --- | --- |
| 已缓存、低风险、只读 GET | 距离最近一次 Redis 成功验证不超过 30 秒时允许 |
| 未缓存 Token | 返回 503 |
| 登录、刷新、退出 | 返回 503 |
| 用户、角色、权限、资源和 Config 修改 | 返回 503 |
| 文件上传、任务执行和其他写操作 | 返回 503 |
| 管理与敏感接口 | 返回 503 |

30 秒窗口结束后，所有受保护请求失败关闭。

服务不能把 Redis 故障误判为 401 或 403；基础设施不可用应返回统一 503，并记录安全和运行告警。

### 8.3 Recovery

Redis 恢复后：

- 客户端自动恢复连接；
- 立即恢复 Redis 权威校验；
- 清空或重新校验故障期间使用的本地缓存；
- 检查 Redis 数据和 TTL 是否完整；
- 验证登录、刷新、撤销和权限修改链路；
- 记录故障持续时间和降级请求数量。

## 9. Gateway Security Boundary

Gateway 负责：

- 提取 Opaque Bearer Token；
- 从 Redis 验证授权快照；
- 维护最小公开路径白名单；
- 清理外部 `X-Synapse-Gateway-*` Header；
- 原样透传 Authorization Bearer Token；
- 签发 GatewayProof；
- 传播 traceId；
- 返回入口级 401 或 Redis 故障 503。

Gateway 不负责：

- 查询用户、角色、菜单或 IAM 数据库；
- 执行业务 permission 判断；
- 注入或信任用户、角色和权限 Header；
- 代替下游验证 Token；
- 保留 `gateway:admin` 权限例外。

Actuator 和运维端点通过网络隔离、暴露控制和运维安全措施保护。

## 10. GatewayProof

经过验证的 Opaque Access Token 授权快照证明当前 Bearer Token 对应的主体和权限；GatewayProof 证明请求经过可信 Gateway。两者不能互相替代。

GatewayProof 使用 Framework 协议，至少绑定：

- Gateway 与可信 Route 标识；
- 时间戳；
- nonce；
- HTTP method；
- 路由改写后的最终 path；
- 规范化 query；
- Bearer Token 指纹。

下游验证签名、时间窗口、route audience、path、query、method、Token 指纹和 nonce 重放。

GatewayProof Secret 只能通过环境变量或 Secret 管理注入，不得写入仓库、镜像、普通配置或日志。

GatewayProof 不用于任意服务间调用。非 Gateway 内部调用使用 Client Credentials Token 和正式服务契约。

## 11. Resource Server Enforcement

IAM、Resource、Audit、Config、File、Message、Task 等受保护服务都必须：

1. 提取 Opaque Access Token；
2. 通过 Framework 查询并验证 Redis 授权快照；
3. 对经 Gateway 的外部请求验证 GatewayProof；
4. 构建主体和 authorities；
5. 执行接口 permission；
6. 执行必要的资源归属和数据权限；
7. 记录关键允许、拒绝和异常结果。

缺失或无效 Token 返回 401；Token 有效但权限不足返回 403；Redis 等安全基础设施不可用返回 503。

## 12. OIDC and RS256

RS256 不用于 V1 Access Token。

RS256 用于需要签名的 OIDC ID Token：

- IAM 持有 RSA 私钥；
- JWK 端点只发布公钥；
- 每把密钥具有稳定且非默认的 `kid`；
- 开发、测试、beta 和生产密钥相互隔离；
- 开发环境挂载独立开发密钥；
- 生产环境通过 Docker Secret、受限文件挂载或外部 Secret 系统注入；
- 生产禁止启动时生成临时长期密钥；
- 轮换期间保留旧公钥，直到旧 ID Token 全部过期；
- 私钥不进入 Git、镜像、日志、异常和普通业务表。

环境 issuer 使用每个环境唯一的 IAM 外部地址。V1 统一 audience 为 `synapse-platform`。

## 13. Client Credentials

V1 支持 Client Credentials，但范围限定为：

- 平台内部服务；
- 受信任机器客户端；
- 管理员显式创建和授权的 Client。

不提供任意第三方自助注册。

Client Secret 只保存安全哈希；Client Token 使用 Opaque Access Token + Redis 快照，并标记 `principal_type=CLIENT`。

## 14. Management Console Security

Management Console 是不可信客户端：

- Access Token 只保存在内存；
- Refresh Token 保存在 `sessionStorage`；
- 不使用 Cookie 登录会话；
- 不使用 `localStorage` 保存 Token；
- 页面刷新可以从 `sessionStorage` 恢复会话；
- 关闭标签页后需要重新登录；
- Token 不进入 URL、日志、埋点、错误页面或状态快照；
- 必须使用严格 CSP、依赖治理、输出编码和 XSS 防护。

在不使用 HttpOnly Cookie 的浏览器应用中，Refresh Token 无法完全规避 XSS 风险，因此前端安全控制属于 V1 验收内容。

## 15. CORS

CORS 由 Gateway 统一配置：

- 生产环境只允许明确的 Management Console Origin；
- 开发环境只允许明确配置的 localhost Origin；
- 禁止 `*`；
- 允许 Header 至少包含 `Authorization`、`Content-Type` 和 `X-Request-Id`；
- 不启用 credentials，因为 V1 不使用 Cookie；
- 下游服务不单独开放宽松 CORS。

## 16. Security Audit Events

V1 至少可靠记录：

- 登录成功与失败；
- 用户锁定、解锁、禁用和启用；
- Access Token 与 Refresh Token 签发；
- Refresh 成功、失败和重放；
- 登出和会话撤销；
- Client 创建、禁用和 Secret 轮换；
- 角色授权与权限回收；
- Resource 权限码新增、禁用和废弃；
- Config 国际化和字典变更；
- GatewayProof 校验失败和 nonce 重放；
- 401、关键 403 和 Redis 降级；
- ID Token 密钥轮换和异常密钥加载。

安全事件通过本地 Outbox + RocketMQ 可靠发送给 Audit。日志不得记录完整 Token、Refresh Token、密码、私钥、Client Secret、GatewayProof Secret 或完整签名材料。

## 17. Current Implementation Warning

以下能力只有在真实实现和测试完成后才能标记为可用：

- Opaque Access Token 生成和 Redis 快照；
- Framework Opaque Token Resource Server 适配；
- Refresh Token 持久化、rotation 和 reuse detection；
- 授权变更后的快照更新和撤销；
- Redis 故障 30 秒只读降级；
- OIDC ID Token、JWK 和密钥轮换；
- 所有下游服务的 GatewayProof 与 nonce replay 验证；
- Client Credentials；
- Management Console Token 存储策略；
- 安全 Outbox 与 Audit 消费闭环。

Noop、默认实现、仅声明依赖或设计文档，不构成生产能力。
