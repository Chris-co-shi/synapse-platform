# Synapse IAM 架构与阶段规划

本文是 `synapse-iam-platform` 的架构事实、职责边界和后续任务入口。当前实现状态以仓库源码、POM 和配置为准；本文描述的后续能力在对应阶段完成验收前均不可视为可用功能。

## 1. IAM 定位

IAM 是 Synapse Platform 的身份与访问管理服务，负责：

- 用户身份认证、用户状态检查与凭据校验；
- Access Token 签发和 Refresh Token 生命周期管理；
- JWK 公钥发布以及 issuer、audience 管理；
- OAuth2 Client 管理；
- 角色、权限关系维护和 Token 授权快照装载；
- Token 撤销、认证审计和安全审计。

IAM 当前不负责菜单前端渲染、工作流、业务资源权限判断、数据权限执行，也不承载文件、消息、任务等领域业务。Gateway 路由和通用安全/OAuth2 技术实现分别属于 Gateway 与 Synapse Framework，不进入 IAM 业务代码。

## 2. 模块结构

```text
synapse-iam-platform
├── synapse-iam-api
├── synapse-iam-client
└── synapse-iam-server
```

- `synapse-iam-api`：保存稳定的跨服务调用契约，不暴露数据库 Entity，不依赖 client 或 server。
- `synapse-iam-client`：保存 IAM 调用适配；只允许依赖 IAM API，不依赖 IAM Server，不直接访问 IAM 数据库。是否引入 OpenFeign 必须由真实调用需求决定。
- `synapse-iam-server`：承载 IAM 启动入口、认证与授权业务、持久化、Token 签发和安全配置；允许依赖 IAM API，禁止依赖自己的 Client 或其他平台 Server。

IAM 启动类必须位于 `com.indigo.synapse.iam` 领域根包。API 和 Client 不得包含生产启动类。

## 3. Framework 复用边界

Framework 只提供通用技术能力，不提供用户、角色、权限、登录、Registered Client 或可启动 IAM 服务。IAM 通过 Maven 单向复用 Framework，禁止把 IAM 业务反向放入 Framework。

根据后续阶段的真实需求，IAM 可以评估复用：

| Framework 模块 | 可复用的技术能力 | 当前 IAM POM 状态 |
| --- | --- | --- |
| `synapse-security` | 主体上下文、权限检查、PasswordEncoder、GatewayProof 协议 | 已声明 |
| `synapse-webmvc` | Servlet WebMVC 通用支撑 | 已声明 |
| `synapse-data` | ORM 无关数据语义 | 已声明 |
| `synapse-mybatis-plus` | MyBatis-Plus 工程增强 | 未声明，待 IAM-02 选型 |
| `synapse-datasource` | 数据源治理 | 未声明，待 IAM-02 选型 |
| `synapse-cache` | 缓存、锁、限流和幂等技术能力 | 已声明，但尚无 IAM 业务实现 |
| `synapse-audit` | 审计事件技术契约 | 已声明，但尚无 IAM 审计业务实现 |
| `synapse-oauth2-core` | claim、token type、validator 和 denylist 契约 | 通过 OAuth2 模块间接复用 |
| `synapse-oauth2-authorization-server-support` | RSAKey、JWKSource、JwtEncoder 和 JWT 签发技术支持 | 已声明 |
| `synapse-oauth2-resource-server-webmvc` | 下游 JWT 验证、主体与 authority 映射 | 已声明 |

依赖被声明不代表业务能力已经启用或形成闭环。特别是 `synapse-oauth2-authorization-server-support` 不是完整 OAuth2 Authorization Server，不提供登录、Token、JWK、discovery、Refresh Token 或 Client 管理端点。

## 4. Token 模型原则

本节只固化跨签发端与验证端必须一致的约束，不在当前阶段最终确定未经实现和测试验证的完整 claim schema。

Access Token 后续至少需要处理：

```text
iss  sub  aud  iat  nbf  exp  jti
token_type  principal_type  client_id
主体标识  scope  roles  permissions
```

设计规则：

- claim 名称、`token_type`、`principal_type` 和集合读取规则必须以 Framework 当前常量、validator 和 converter 为准；
- 当前 Framework 使用 `token_type=ACCESS_TOKEN`，主体类型区分 `USER` 与 `CLIENT`；
- `sub` 使用稳定且不可变的主体标识，不能使用会变更的展示名称；
- `jti` 必须唯一，为后续撤销和 denylist 提供稳定键；
- `iss`、`aud`、有效期和允许时钟偏差必须由签发端与 Gateway、下游 Resource Server 协同配置；
- Gateway 只校验 Token 合法性，不执行角色、权限或业务资源判断；
- 下游服务重新验证 Bearer Token，并执行接口、资源和数据权限；
- roles、permissions 是签发时快照，权限变化与存量 Token 的一致性策略在 IAM-07/IAM-08 确定；
- Token 不保存密码、手机号、私钥、Client Secret 或其他完成认证与授权不需要的敏感信息；
- Bearer Token 原样转发给下游，不能用可伪造身份 Header 替代。

## 5. JWK 与密钥原则

- JWT 使用非对称 RSA 密钥签发；IAM 持有私钥，Gateway 和下游只获取公钥/JWK。
- 私钥、密钥口令和真实 Secret 不得写入 Git、镜像、日志、异常或普通配置文件。
- 本地开发、自动化测试、beta 和生产密钥必须隔离；开发临时密钥不能用于生产。
- 每把签名密钥必须有稳定且非默认的 key ID，后续设计需要支持旧公钥保留和密钥轮换窗口。
- 生产私钥应由受控 Secret/KMS/HSM 或受限外部存储注入，不直接明文保存在普通业务表。
- JWK 响应只能发布验证所需的公钥材料，不能暴露私钥参数。
- `/oauth2/jwks` 当前尚未实现，不能描述为可用端点。

## 6. 权限边界

```text
IAM      -> 维护用户、角色、权限关系，并向 Token 装载授权快照
Gateway  -> 验证 Token 合法性和可信入口证明，不判断业务权限
下游服务 -> 验证 Token，并执行接口、资源、归属和数据权限
```

Gateway 与下游只传播 Bearer Token，不传播或信任客户端可伪造的 `X-User-Id`、`X-Roles`、`X-Permissions` 等身份权限 Header。Framework 的默认权限检查只读取已经验证的 Token 权限快照，不查询 IAM 数据库，也不根据角色临时推导权限。

## 7. 数据库设计原则

IAM 计划使用 PostgreSQL，并由 Flyway 管理 schema 演进；当前任务不创建表或 migration。IAM-02 必须先确定数据访问技术栈，再增加必要依赖。

- 表名、字段名统一使用小写 snake_case，并采用稳定的 IAM 领域前缀。
- 主键策略必须兼顾跨环境唯一性、索引局部性和对外不可枚举性；选型在 IAM-02 记录并验证。
- 可并发修改的聚合使用乐观锁；审计字段记录创建、修改时间和操作主体。
- 软删除只用于确有恢复、留痕或引用完整性需求的数据，不替代状态机和审计记录。
- 规范化后的用户名、邮箱等登录标识建立数据库唯一约束，不能只依赖应用层检查。
- 密码只保存 PasswordEncoder 产生的安全哈希，不保存明文、可逆密文或日志副本。
- Refresh Token 使用高熵随机值时，持久化层只保存其安全摘要，并支持轮换与重放检测。
- 私钥不得明文保存在普通业务表；密钥引用和轮换元数据与密钥材料分离。
- Entity、Mapper、Repository 只存在于 Server，不通过 IAM API DTO 暴露，也不供其他服务共享数据库访问。

## 8. 安全原则

- 密码编码和校验复用 Framework PasswordEncoder；具体算法参数和迁移策略在 IAM-03 验证。
- 登录失败对外使用一致响应，不能泄露账号是否存在、账户状态或密码校验细节。
- 登录入口必须具备限流、失败次数控制、锁定窗口和审计能力。
- Token、密码、私钥、Client Secret、Refresh Token 和完整认证材料禁止写入日志。
- Refresh Token 必须轮换，并检测旧 Token 重放；并发刷新和异常恢复策略必须有测试。
- Access Token 撤销依赖 `jti` 和真实 denylist/revocation 实现；Framework Noop 端口不代表生产撤销能力。
- 所有节点必须同步系统时间，并对 JWT 时钟偏差设置有限、明确的容忍窗口。
- 签名、claim 校验、密钥加载、撤销检查和审计失败默认 fail closed，不能降级为匿名或放行。
- 所有新增公开类型和公开方法必须提供完整中文 Javadoc；配置属性必须提供中文说明和 Configuration Metadata。

## 9. 当前实际状态

### 已完成

- IAM `api`、`client`、`server` Maven 工程骨架；
- 位于 `com.indigo.synapse.iam` 根包的服务启动类；
- Framework WebMVC、Security、OAuth2 Resource Server、JWT 签发支持、Cache、Audit 和 Data 技术依赖声明；
- Nacos、Actuator、环境 profile、PostgreSQL 驱动和开发数据源配置骨架；
- `db/migration` 目录占位。

上述“依赖声明”和“配置骨架”不等同于业务已经实现。当前数据源配置尚未由完整数据访问依赖闭环支撑，Flyway 也没有依赖或 migration。

### 尚未完成

- 用户、凭据、角色、权限和 OAuth2 Client 领域模型；
- 数据库表、Flyway migration 和持久化实现；
- 用户登录、密码认证和账户状态策略；
- Access Token、Refresh Token 和会话生命周期；
- RSA 生产密钥装配、JWK 端点和密钥轮换；
- issuer、audience 和完整 claim 签发策略；
- 权限快照装载；
- Token 撤销和真实 denylist；
- 认证与安全审计业务；
- OAuth2 Token、discovery 和 Client 管理端点。

当前 `/test` 仅为连通性占位接口，不是 IAM 认证能力。现有配置也不能作为上述端点可用的证据。

## 10. 阶段计划

### IAM-02：数据库基础模型与 Flyway

- 目标：建立可演进的 IAM 持久化基线。
- 主要产物：数据模型、技术选型记录、Flyway migration、约束与索引、Repository/Mapper 及数据库测试。
- 明确不做：登录、Token、JWK、Refresh Token 业务端点。
- 前置依赖：本设计文档和当前 Maven 基线通过。
- 验收条件：migration 可在空库和受支持升级路径执行，约束与并发规则有测试，Entity 不泄漏到 API。

### IAM-03：用户凭据与密码认证

- 目标：建立用户状态检查和安全密码认证闭环。
- 主要产物：用户与凭据领域逻辑、PasswordEncoder 接入、失败计数/锁定策略、认证审计事件和单元测试。
- 明确不做：Token/JWK、角色权限装载、管理后台。
- 前置依赖：IAM-02 数据模型和事务边界完成。
- 验收条件：成功、错误密码、未知用户、禁用、锁定和并发失败场景均有测试，外部错误不泄露账号信息。

### IAM-04：RSA/JWK/issuer 基础设施

- 目标：建立可安全配置和轮换的非对称签名基础设施。
- 主要产物：环境隔离的密钥提供适配、issuer 配置、只含公钥的 JWK 输出、key ID/轮换设计和安全测试。
- 明确不做：用户 Token 端点、Refresh Token、OAuth2 Client 管理。
- 前置依赖：Framework Authorization Server Support 契约保持兼容。
- 验收条件：生产禁止生成临时密钥，私钥不进入仓库/响应/日志，旧公钥保留策略可验证。

### IAM-05：Access Token 签发

- 目标：为已认证主体签发与 Gateway、WebMVC、WebFlux Resource Server 兼容的 Access Token。
- 主要产物：受约束的 claim builder、签发服务、issuer/audience/TTL 策略和跨技术栈契约测试。
- 明确不做：Refresh Token、完整角色权限后台、数据权限。
- 前置依赖：IAM-03 认证结果和 IAM-04 签名基础设施。
- 验收条件：标准时间 claim、`jti`、token/principal type 和主体字段完整，错误配置 fail closed，Gateway 与下游能够一致验证。

### IAM-06：Refresh Token 与会话生命周期

- 目标：支持安全续期、会话终止和 Refresh Token 重放检测。
- 主要产物：会话模型、Refresh Token 摘要存储、轮换、过期、并发刷新和重放处理。
- 明确不做：角色管理后台、SSO、MFA、第三方登录。
- 前置依赖：IAM-05 Access Token 签发闭环。
- 验收条件：明文不落库不落日志，旧 Token 重放会终止相应安全边界，生命周期场景有集成测试。

### IAM-07：角色和权限 claim 装载

- 目标：把 IAM 授权关系转换为 Framework 可识别的 Token 权限快照。
- 主要产物：角色/权限查询、规范化与去重、claim 装载、权限变更一致性策略和测试。
- 明确不做：菜单渲染、ABAC、组织与数据权限执行。
- 前置依赖：IAM-02 授权数据模型和 IAM-05 签发流程。
- 验收条件：scope/role/permission 与 Framework authority converter 一致，下游权限检查覆盖允许与拒绝场景。

### IAM-08：Token 撤销与 denylist

- 目标：让 Access Token、会话和 Refresh Token 可以可靠撤销。
- 主要产物：真实 `TokenDenylistPort` 适配、按 `jti` 撤销、过期清理、会话级撤销和 Resource Server 集成测试。
- 明确不做：用 Noop 实现伪装生产能力，不修改 Framework 业务边界。
- 前置依赖：IAM-05、IAM-06 的 token/session 标识稳定。
- 验收条件：撤销后 Gateway/下游按既定策略拒绝 Token，存储不可用时行为明确且默认 fail closed。

### IAM-09：OAuth2 Client 管理

- 目标：管理可信客户端、授权方式、scope 和 Client Secret 生命周期。
- 主要产物：Registered Client 模型与持久化、Secret 编码/轮换、协议端点选型记录和授权方式测试。
- 明确不做：社交登录、LDAP、第三方 OAuth 登录或未经决策的自定义 grant。
- 前置依赖：IAM-04 签名基础设施和 IAM-05/IAM-06 Token 生命周期。
- 验收条件：Secret 不明文存储/返回/记录，客户端状态、授权方式和 scope 均被服务端强制校验。

### IAM-10：安全审计、测试和文档收口

- 目标：形成可交付、可运维和可追溯的 IAM 安全闭环。
- 主要产物：登录/签发/刷新/撤销审计、威胁场景测试、API/配置/运维文档和故障排查手册。
- 明确不做：组织、菜单、ABAC、MFA、SSO 或前端页面扩展。
- 前置依赖：IAM-02 至 IAM-09 的已选能力完成。
- 验收条件：关键失败路径、敏感信息保护、时间偏差、重放、撤销和密钥轮换均有自动化验证，文档不夸大当前能力。

