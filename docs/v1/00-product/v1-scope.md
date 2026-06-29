# Synapse Platform V1 范围

## 文档地位

本文定义当前 V1 的交付范围和完成标准。详细架构以 [V1 架构基线](v1-baseline.md) 为准。

## 1. V1 目标

V1 交付 **Synapse Identity & Access Foundation**，先完成平台自身认证、调用、授权和审计闭环。

V1 不以真实 MES、WMS、QMS、SAP 或其他遗留系统改造为完成条件。

## 2. P0：必须完成

### Gateway

- Spring Cloud Gateway + WebFlux；
- 使用 `synapse-oauth2-resource-server-webflux`；
- JWT 签名、Issuer、时间、Token 类型和必要 Claim 校验；
- 按路由校验唯一 Audience；
- 最小公开路径、Header 清理和 Bearer Token 转发；
- 不执行 Role、Permission 或数据权限；
- 不使用 GatewayProof。

### IAM

- 用户与凭据；
- OAuth Client 与凭据轮换；
- Role、Permission、用户直接授权和 Client Permission；
- OAuth 2.0 / OpenID Connect；
- Authorization Code + PKCE；
- Client Credentials；
- RS256 JWT Access Token；
- JWK Set 与 `kid`；
- Opaque Refresh Token；
- Refresh rotation、并发刷新和 reuse detection；
- 登录、刷新、退出、当前主体；
- 基础 Introspection 和 Token Revocation；
- 基础安全审计。

### Resource Server

- WebMVC 与 WebFlux 目标服务独立验证 JWT；
- USER 和 CLIENT 主体映射；
- 功能权限允许与拒绝；
- 业务数据权限由资源所在系统自行负责；
- 审计字段保存稳定 `principal_type + principal_id`。

### 第三方调用

- 支持标准 OAuth2 Client Credentials；
- 第三方不需要 Framework、Manifest、授权快照或 RocketMQ；
- 不兼容的遗留系统通过项目级 Adapter 接入。

### 运行基线

- Java 21；
- PostgreSQL 17；
- Redis；
- Nacos；
- 可重复执行的数据库迁移；
- 最小 Docker Compose 或等价部署验证。

## 3. Token 基线

### Access Token

- JWT；
- RS256；
- `typ=at+jwt`；
- Header 包含 `kid`；
- USER TTL 10 分钟；
- CLIENT TTL 5 分钟；
- Clock skew 60 秒；
- 单 Audience；
- Scope 为空格分隔字符串；
- 不携带 roles、permissions、菜单、数据范围和 `tenant_id`；
- USER 包含 `sid`，CLIENT 不包含。

### Refresh Token

- Opaque；
- 数据库保存安全摘要；
- rotation；
- reuse detection；
- Client Credentials 不返回 Refresh Token。

## 4. P1 / NEXT

- 外部 OIDC 或 SAML 身份联合；
- 身份映射；
- 管理端完善；
- File、Message、Task 独立能力；
- 根据真实项目建设 Legacy Adapter；
- 评估 Resource Catalog 是否需要独立服务；
- 更完整的审计服务。

## 5. LATER

- Resource Manifest；
- `authz_ver` 和 Authorization Snapshot；
- Revocation Feed 与 MQ 撤销投影；
- 多租户；
- Integration Platform；
- DPoP、mTLS 和 FAPI 高安全 Profile；
- Workflow、MDM、Report、Monitor 产品化。

## 6. REJECTED

- GatewayProof；
- Gateway 注入可信用户、角色或权限 Header；
- 强制第三方使用 Framework；
- 强制外部系统接受 Synapse JWT；
- 强制外部系统维护 Manifest；
- 把所有 MES/WMS/QMS 视为 Synapse 原生系统；
- 通过共享数据库完成跨系统集成；
- 为未知厂商提前建设万能适配平台。

## 7. 三条验收链路

### 用户登录

```text
Client -> IAM -> JWT + Refresh Token -> Gateway -> IAM protected API
```

### 服务调用

```text
Service Client -> Client Credentials -> JWT -> protected API
```

### 第三方调用

```text
Third-party Client -> standard token endpoint -> Bearer JWT -> Synapse API
```

## 8. Definition of Done

- 三条链路端到端通过；
- Gateway 与目标服务均验证 JWT；
- Audience 跨服务复用被拒绝；
- USER / CLIENT 权限和审计主体正确；
- Refresh rotation、并发和 reuse detection 测试通过；
- PostgreSQL 17 空库迁移通过；
- 最小部署可重复启动；
- 文档不再把延期能力描述成已实现能力。

任何影响 P0、Token Profile 或完成标准的变化，必须更新本文和 Gap Analysis。
