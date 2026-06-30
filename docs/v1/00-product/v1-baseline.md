# Synapse Platform V1 架构基线

## 1. 文档地位

本文是 `synapse-platform` 当前 V1 的最高优先级架构基线。

当 README、模块 AGENTS、旧 ADR、旧设计文档与本文冲突时，以本文和当前源码事实为准；冲突必须进入 Gap Analysis，禁止静默保留两套口径。

## 2. V1 产品目标

V1 不是企业全部数字化系统的统一改造平台，也不是完整 IAM、集成平台或低代码平台。

V1 的目标是交付 **Synapse Identity & Access Foundation**：

- Synapse 自身用户能够使用标准 OAuth 2.0 / OpenID Connect 登录；
- Synapse 服务和第三方调用方能够使用 Client Credentials 调用 Synapse API；
- Gateway 与目标 Resource Server 都能独立验证 Opaque Access Token 授权快照；
- USER 与 CLIENT 主体能够执行功能权限检查并写入稳定审计主体；
- Refresh Token 支持 rotation、撤销和 reuse detection；
- 所有能力可在 PostgreSQL 17、Redis、Nacos 的基础环境中运行、测试和部署。

当前已确认实现的是自定义 IAM 登录 API + Opaque Token 会话体系。OAuth2/OIDC 是 V1 必须完成范围，但当前状态为 `Planned / Not Implemented`，不得把计划能力写成已实现。

## 3. 真实企业环境假设

企业中的 MES、WMS、QMS、LIMS、ERP、AGV、IoT 等系统默认按外部黑盒处理，除非明确证明其代码、发布和安全配置由 Synapse 团队控制。

外部系统可能由多年前的不知名厂商使用 Java、.NET 或其他技术实现，可能只支持 Basic、API Key、SOAP、文件、数据库或私有协议。

因此：

1. 不强制外部系统使用 Synapse Framework；
2. 不强制外部系统接受 Synapse 私有 Token 或 GatewayProof；
3. 不强制外部系统发布 Resource Manifest；
4. 不尝试在 V1 统一其内部角色、权限和数据范围；
5. 不兼容时由项目级 Adapter / Anti-Corruption Layer 适配；
6. 重复出现多个同类适配场景后，才评估 Integration Platform。

## 4. V1 必须闭环的三条链路

### 4.1 用户登录闭环

```text
Web / Management Client
  -> Authorization Code + PKCE（计划）
IAM
  -> Opaque Access Token + Opaque Refresh Token
Gateway
  -> Redis 授权快照验证
Target Resource Server
  -> 再次验证授权快照 + 功能权限检查 + 审计主体
```

当前已实现自定义 `/auth/login`、`/auth/refresh`、`/auth/logout`、`/auth/me`。验收要求：登录、刷新、当前主体、退出、无 Token 返回 401、无权限返回 403、USER 审计字段正确。

### 4.2 服务调用闭环

```text
Platform Service / Worker
  -> Client Credentials（计划）
IAM
  -> CLIENT Opaque Access Token
Target Resource Server
  -> 授权快照验证 + Client 权限检查 + CLIENT 审计主体
```

验收要求：每个调用方使用独立 Client，只能申请允许的 Resource 和 Scope；CLIENT 不伪装成 USER。

### 4.3 第三方调用 Synapse 闭环

```text
SAP / ERP / Legacy Adapter / Partner System
  -> OAuth 2.0 Client Credentials（计划）
IAM
  -> standard Bearer Token
Synapse API
```

第三方只需支持标准 Token Endpoint 和 Bearer Header，不要求理解 GatewayProof、Redis 授权快照、RocketMQ 或 Framework。该标准入口尚未实现。

## 5. V1 安全协议

### 5.1 标准层

- OAuth 2.0；
- OpenID Connect；
- Authorization Code + PKCE S256；
- Client Credentials；
- OAuth 2.0 Security Best Current Practice；
- Resource Indicators；
- Authorization Server Metadata；
- JWK Set；
- Token Introspection；
- Token Revocation。

### 5.2 Token Profile

Access Token：

- Opaque；
- 高熵不可预测；
- IAM 对 Token 计算安全摘要；
- Redis 保存授权快照；
- 不携带 roles、permissions、菜单或数据范围；
- USER 与 CLIENT 通过 principal type 区分；
- 单租户 V1 不包含 `tenant_id`。

Refresh Token：

- Opaque；
- 数据库只保存安全摘要；
- 每次刷新 rotation；
- 旧 Token 重用触发 family / session 撤销和安全审计。

当前已知缺陷：旧 Refresh Token 重放请求本身返回 401，但 successor Refresh Token 仍可继续 refresh，同一 token family 中仍存在 ACTIVE session，family 尚未整体撤销。

## 6. Gateway 边界

Gateway 负责：

- 验证 Opaque Access Token 授权快照；
- Redis 快照缺失时返回 401；
- Redis 不可用时返回 503；
- 最小公开路径控制；
- 清理外部可伪造的身份和权限 Header；
- 原样转发 `Authorization: Bearer`；
- 当前实现包含 GatewayProof；
- 路由、限流、熔断和 Trace 传播。

Gateway 不负责：

- 可信用户 Header 注入；
- Role、Permission 或数据权限判断；
- 替代下游 Resource Server 验证 Token。

## 7. IAM V1 边界

IAM 当前集中承载：

- 用户与凭据；
- OAuth Client 与凭据轮换；
- Role、Permission 和授权关系；
- Resource Identifier 与 Scope 的最小注册；
- 自定义 `/auth/*` 登录、刷新、退出和当前主体接口；
- Opaque Access Token 签发和 Redis 授权快照；
- Opaque Refresh Token、Session、rotation 和 reuse detection；
- 登录、授权与安全审计事件。

Authorization Server / OIDC Provider、标准 `/oauth2/token`、Client Authentication、Client Credentials、Authorization Code + PKCE、OIDC Discovery、ID Token 和 UserInfo 当前未实现，仍属于 V1 计划范围。

V1 不要求独立 Resource Catalog 微服务。`synapse-resource-platform` 保留为 NEXT 候选，不作为 V1 运行闭环依赖。

## 8. 功能授权

V1 使用 Allow-only 模型：

- USER：Role Permission 与用户直接 Permission 的并集；
- CLIENT：Client 直接 Permission；
- 最终权限受 Audience 与请求 Scope 限制；
- Scope 是 Client 的粗粒度访问边界，不自动授予 Permission；
- 不支持显式 DENY；
- 不支持 CLIENT -> ROLE；
- 不在 IAM 中实现业务数据权限。

业务系统的数据范围、资源归属和领域规则由资源所在系统执行。

## 9. 当前迭代取舍

### NOW

- 自定义 IAM 登录 API + Opaque Token 会话体系；
- Opaque Access Token、Redis 授权快照；
- Opaque Refresh Token rotation / reuse detection；
- USER / CLIENT 主体；
- Role / Permission / Client Permission；
- Gateway 与 IAM 独立验证授权快照；
- 审计主体与基础安全审计；
- 单租户；
- PostgreSQL 17、Redis、Nacos 的最小部署闭环。

### NEXT

- 修复 Refresh Token reuse detection 的 token family 整体撤销；
- 重新执行 host-local 真实链路测试；
- OAuth Client 模型和持久化；
- Spring Authorization Server；
- Client Credentials；
- Authorization Code + PKCE；
- 接入现有 Refresh Session；
- 最小 OIDC Discovery、ID Token 和 UserInfo 闭环；
- 外部 OIDC / SAML IdP 联合；
- 身份映射；
-独立 Resource Catalog 的重新评估；
- File / Message / Task 的独立产品闭环；
- 明确项目驱动的 Legacy Adapter；
- 更完整的管理端。

### LATER

- Resource Manifest as Code；
- Authorization Snapshot / `authz_ver` 扩展；
- Revocation Feed / MQ 撤销投影；
- Integration Platform；
- 多租户；
- DPoP、mTLS、FAPI 高安全 Profile；
- Workflow、MDM、Report、Monitor 产品化。

### REJECTED

- Gateway 注入可信身份或权限 Header；
- 强制第三方使用 Framework；
- 强制第三方接受 Synapse 私有扩展；
- 强制第三方维护 Manifest；
- 把所有 MES/WMS/QMS 视为 Synapse 原生系统；
- 在 V1 建设万能企业集成注册中心；
- 通过共享数据库完成跨系统集成。

## 10. 架构准入规则

任何新需求在进入开发前必须回答：

1. 它对应当前哪一条闭环？
2. 当前真实消费者是谁？
3. 不做会阻断上线、安全或数据正确性吗？
4. 成本是 S、M、L 还是 XL？
5. 结论是 NOW、NEXT、LATER 还是 REJECTED？

没有真实消费者、只提升架构完整感的能力，默认不进入 NOW。

## 11. 完成标准

V1 完成必须由代码、自动化测试和部署验证证明：

- 用户登录、刷新、退出和当前主体链路通过；
- Client Credentials 链路通过；
- Opaque Access Token 与 Redis 授权快照校验通过；
- Gateway 与下游独立验证通过；
- 权限允许和拒绝测试通过；
- USER / CLIENT 审计主体正确；
- Refresh rotation、并发刷新和 reuse detection 的 token family 整体撤销通过；
- 第三方可只依据标准 OAuth2 文档完成 Client Credentials 调用；
- Docker Compose 或等价最小部署可重复启动和验证。
