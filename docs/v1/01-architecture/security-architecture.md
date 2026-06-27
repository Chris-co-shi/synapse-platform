# Synapse Platform V1 安全架构

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、安全负责人、核心研发、测试、实施和运维人员 |
| Purpose | 定义 V1 的认证、Token、授权执行、可信入口、会话撤销和安全审计基线 |
| Scope | Management Console、Gateway、IAM、Resource、Audit 及所有受保护微服务 |
| Status | Review |

本文描述 V1 必须达到的目标架构，不代表所有能力已经实现。实际完成状态必须由源码、自动化测试和发布记录证明。

## 1. Security Principles

V1 遵循以下原则：

- 默认拒绝，显式放行；
- Gateway 是统一入口，但不是唯一安全执行点；
- IAM 是身份和授权事实源，但不是所有权限执行点；
- Resource 定义资源与权限目录，不保存主体授权关系；
- 每个受保护服务独立验证 Access Token；
- 每个服务执行自己的接口、资源和数据权限；
- Management Console 和内部网络均不视为可信；
- Token、密码、私钥和 Client Secret 不进入日志；
- 签名、claim、撤销、GatewayProof 和权限检查失败时默认拒绝。

## 2. Trust Boundaries

```text
Browser / External Client
  ↓ HTTPS
Gateway
  ↓ Bearer Token + GatewayProof
IAM / Resource / Audit / File / Message / Task
  ↓
PostgreSQL / Redis / Nacos / RocketMQ
```

V1 Docker Compose 默认只对外暴露 Management Console 和 Gateway 所需端口。平台微服务端口原则上只在内部网络可达。

内部网络隔离不能替代 Token 验证、GatewayProof 验证或服务端授权。

## 3. Token Model

### 3.1 Access Token

V1 使用 IAM 签发的非对称签名 JWT Access Token。

Access Token：

- 用于 Gateway 和各 Resource Server 的请求认证；
- 不在 IAM 业务数据库中逐条持久化；
- 使用唯一 `jti` 支持撤销；
- 使用稳定且不可变的 `sub` 表达主体；
- 必须验证签名、有效期、issuer、audience、token type 和 required claims；
- 使用较短有效期，具体 TTL 在 IAM 详细设计中确定。

概念性 claim 示例：

```json
{
  "iss": "https://iam.example",
  "sub": "1900000000000000001",
  "aud": ["synapse-platform"],
  "iat": 0,
  "nbf": 0,
  "exp": 0,
  "jti": "unique-token-id",
  "token_type": "ACCESS_TOKEN",
  "principal_type": "USER",
  "client_id": "synapse-admin",
  "roles": ["PLATFORM_ADMIN"],
  "permissions": [
    "iam:user:read",
    "resource:menu:read"
  ]
}
```

该示例只表达语义。实际 claim 名称、集合格式和 authority 映射以 Synapse Framework 的常量、validator 和 converter 为协议事实来源。

Access Token 不携带：

- 菜单树；
- 页面路由；
- 按钮元数据；
- 资源描述；
- 密码、手机号、私钥或 Client Secret；
- 完成认证与授权不需要的个人信息。

### 3.2 Roles and Permissions

V1 在用户 Access Token 中携带签发时的角色和权限码快照。

- Resource 是权限码定义的事实来源；
- IAM 是角色权限关系的事实来源；
- 各服务根据已验证 Token 中的权限码执行授权；
- Framework 权限检查不在运行时查询 IAM 数据库；
- 角色只用于组织授权，接口安全最终使用权限码表达。

当权限数量超过可接受的 Token 体积时，后续可以演进为 `authorization_version` 与 Redis 授权快照，但不作为 V1 初始方案。

### 3.3 Principal Types

V1 至少区分：

- `USER`：用户通过管理端或交互式认证获得 Token；
- `CLIENT`：平台服务或可信客户端通过受支持的客户端授权获得 Token。

Client Token 不伪装成用户 Token。需要保留用户上下文的同步调用可以传播原始用户 Bearer Token；纯服务调用使用独立 Client 身份。

## 4. Refresh Token and Session Model

V1 使用高熵、不可预测的 Opaque Refresh Token。

Refresh Token：

- 明文只返回给合法客户端；
- IAM 持久化层只保存安全摘要；
- 绑定用户、Client、会话和过期时间；
- 每次成功刷新后执行 rotation；
- 旧 Refresh Token 再次使用时视为重放；
- 重放检测后撤销对应会话族，并记录安全事件。

会话至少需要表达：

- session id；
- refresh token family id；
- subject id；
- client id；
- token hash；
- issued / expires time；
- active / revoked / expired / reuse-detected 状态；
- replaced token reference；
- revoke reason 和时间。

刷新操作必须是原子事务，避免并发刷新同时成功。

## 5. Login, Refresh and Logout

### 5.1 Login

```text
Client -> Gateway -> IAM
  -> GatewayProof verification
  -> credential and account status verification
  -> role and permission snapshot calculation
  -> Access Token + Opaque Refresh Token issuance
  -> security audit
```

登录失败对外返回统一错误，不泄露账号是否存在、禁用、锁定或密码错误等内部差异。

登录入口必须支持限流、失败计数、临时锁定和安全审计。具体阈值由 IAM 设计确认。

### 5.2 Refresh

```text
Client -> Gateway -> IAM
  -> validate GatewayProof
  -> hash and validate Refresh Token
  -> validate session and client
  -> detect reuse
  -> rotate Refresh Token
  -> recalculate current roles and permissions
  -> issue new Access Token
  -> security audit
```

刷新时重新计算当前授权，因此普通角色或权限变更最迟在下一次刷新后生效。

### 5.3 Logout

登出至少执行：

- 撤销当前 Refresh Token 会话；
- 将当前 Access Token `jti` 加入 denylist，保留至 Token 原始过期时间；
- 记录退出和撤销事件。

“退出当前会话”和“退出全部会话”应作为不同操作设计。

## 6. Immediate Revocation

普通授权调整可以在重新登录或刷新 Token 后生效。

以下高风险变化必须支持立即撤销：

- 用户被禁用或锁定；
- 管理员或敏感角色被回收；
- 凭据泄露或主动强制下线；
- Refresh Token 重放；
- Client 被禁用；
- 安全人员执行全会话撤销。

立即撤销基于：

- Access Token `jti` denylist；
- Refresh Token 会话状态；
- 用户或 Client 的会话级撤销。

Gateway 和所有受保护服务必须执行一致的 denylist 检查。撤销存储不可用时，生产环境默认 fail closed。

## 7. Gateway Security Boundary

Gateway 负责：

- 提取和验证 Bearer Access Token；
- 最小公开路径白名单；
- 清理所有外部 `X-Synapse-Gateway-*` Header；
- 原样透传 Authorization Bearer Token；
- 签发 GatewayProof；
- 传播 traceId；
- 返回统一入口级 401。

Gateway 不负责：

- 查询用户、角色或菜单；
- 执行业务 permission 判断；
- 传播或信任 `X-User-Id`、`X-Roles`、`X-Permissions` 等身份 Header；
- 代替下游验证 Token；
- 代替下游返回业务授权 403。

公开路径仅表示 Gateway 不要求用户 Access Token，IAM 仍需验证 GatewayProof、Client 和端点自身安全约束。

## 8. GatewayProof

JWT 证明调用主体身份；GatewayProof 证明请求经过可信 Gateway。两者不能互相替代。

GatewayProof 使用 Framework 定义的协议，至少绑定：

- Gateway 与可信 Route 标识；
- 时间戳；
- nonce；
- HTTP method；
- 路由改写后的最终 path；
- 规范化 query；
- Bearer Token 指纹。

下游服务验证：

- 签名；
- 时间窗口；
- route audience；
- path、query 和 method；
- Token 指纹绑定；
- nonce 重放。

GatewayProof 使用的 Secret 必须通过环境变量或 Secret 管理注入，不能写入仓库、镜像、普通配置或日志。

GatewayProof 不用于任意服务间调用。非 Gateway 内部调用使用独立 Client Token 和正式服务契约。

## 9. Resource Server Enforcement

IAM、Resource、Audit、File、Message、Task 等受保护服务都作为 OAuth2 Resource Server：

1. 独立验证 JWT 签名和 claims；
2. 检查 Access Token denylist；
3. 对经 Gateway 的外部请求验证 GatewayProof；
4. 从已验证 Token 构建主体和 authorities；
5. 执行接口 permission；
6. 执行必要的资源归属和数据权限；
7. 记录关键允许、拒绝和异常结果。

缺失或无效 Token 返回 401；Token 有效但权限不足返回 403。

菜单隐藏、按钮禁用和前端路由守卫不能代替以上步骤。

## 10. Resource and IAM Authorization Flow

```text
Resource 注册资源和 permission_code
  -> IAM 为角色保存 permission_code
  -> 用户登录或刷新
  -> IAM 计算权限快照并签发 Access Token
  -> Resource 根据 Token 生成导航
  -> 目标服务根据 Token 权限码执行授权
  -> Audit 记录授权和访问过程
```

运行时每个业务请求不得同步调用 IAM 和 Resource 查询权限。

## 11. Key and JWK Management

- IAM 持有签名私钥；Gateway 和 Resource Server 只获取公钥；
- JWK 端点只发布验证所需的公钥材料；
- 每把密钥必须具有稳定且非默认的 `kid`；
- 本地、测试、beta 和生产密钥必须隔离；
- 生产环境禁止启动时临时生成长期使用的签名密钥；
- 轮换期间保留旧公钥，直到旧 Token 全部自然过期；
- 私钥不进入 Git、容器镜像、日志、异常和普通业务表。

具体签名算法、密钥来源和轮换周期由 IAM 详细设计确认。

## 12. Management Console Security

Management Console 是不可信客户端：

- 所有权限判断必须由服务端执行；
- Access Token 通过 Authorization Header 发送；
- V1 不采用基于 Cookie 的平台登录会话；
- Access Token 应尽量只保存在内存；
- Refresh Token 的浏览器持久化策略必须在前端安全设计中单独确认；
- 不得未经评审默认写入 `localStorage`；
- 禁止把 Token 输出到日志、埋点、URL、错误页面或前端状态快照。

Gateway 统一维护允许的 CORS Origin。生产环境不得使用无约束通配配置。

## 13. Security Audit Events

V1 至少记录：

- 登录成功与失败；
- 用户锁定、解锁、禁用和启用；
- Access Token 签发；
- Refresh 成功、失败和重放；
- 登出、会话撤销和 denylist；
- Client 创建、禁用和 Secret 轮换；
- 角色授权与权限回收；
- Resource 权限码新增、禁用和废弃；
- GatewayProof 校验失败和 nonce 重放；
- 401 和关键 403；
- 密钥轮换和异常密钥加载。

日志不得记录密码、完整 Token、Refresh Token、私钥、Client Secret、GatewayProof Secret 或完整签名材料。

## 14. Current Implementation Warning

以下能力只有在真实实现和测试完成后才能标记为可用：

- Access Token 签发完整闭环；
- Refresh Token 持久化、rotation 和 reuse detection；
- JWK 端点和密钥轮换；
- 角色与权限 claim 装载；
- 真实 Access Token denylist；
- 所有下游服务的 GatewayProof 与 nonce replay 验证；
- Client Credentials 服务身份；
- 管理端安全存储策略。

Framework 中的 Noop、默认实现或仅声明依赖，不构成生产能力。

## 15. Pending Detailed Decisions

以下细节进入 IAM、Gateway 和前端设计文档确认：

- Access Token TTL；
- Refresh Token TTL 和最大会话时长；
- issuer 和 audience 具体值；
- JWT 签名算法和生产密钥来源；
- 登录失败阈值与锁定窗口；
- denylist Redis key 与不可用策略细节；
- 浏览器 Refresh Token 保存和页面刷新恢复策略；
- Client Credentials 支持范围；
- CORS allowlist 和外部域名策略。
