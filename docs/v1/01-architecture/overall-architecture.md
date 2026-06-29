# Synapse Platform V1 总体架构

## 1. 运行拓扑

```text
Browser / Service / Third-party Client
              |
          HTTPS + OAuth2
              |
         Synapse Gateway
              |
         Synapse IAM API
              |
 PostgreSQL 17 / Redis / Nacos
```

V1 先完成身份与访问闭环，不要求 Resource、Audit、Config、File、Message、Task 等服务同时上线。

## 2. 服务职责

### Gateway

- WebFlux 统一入口；
- JWT 基础验证；
- 路由级 Audience 校验；
- Header 清理；
- Bearer Token 转发；
- 路由和基础流量治理。

Gateway 不执行 Permission，不生成 GatewayProof。

### IAM

- 用户、Client、Role、Permission 和授权关系；
- OAuth2 / OIDC；
- JWT Access Token；
- Opaque Refresh Token；
- Session、rotation 和 reuse detection；
- JWK、Introspection、Revocation；
- 基础安全审计。

### Target Resource Server

IAM 管理 API 自身以及后续进入 V1 的服务都必须独立验证 JWT，并执行自身功能权限和数据规则。

## 3. Token 结构

```text
Access Token: JWT / RS256 / single Audience
Refresh Token: Opaque / hashed persistence / rotation
```

Access Token 不包含 roles、permissions、菜单、数据范围或租户 Claim。

## 4. 通信基线

- 用户调用使用面向目标 Resource 的 JWT；
- 服务和第三方调用使用 Client Credentials；
- 跨 Resource 调用由调用方申请目标 Audience Token；
- V1 不无限转发用户 Token；
- 不使用共享数据库或可信身份 Header；
- 外部系统协议由项目级 Adapter 处理。

## 5. 数据所有权

V1 的 IAM Schema 保存身份、Client、Role、Permission、Session 和 Refresh Token 摘要。

Gateway 不访问数据库。其他延期模块的 Schema 不是当前 V1 完成条件。

## 6. 基础设施

- PostgreSQL 17：身份与授权事实；
- Redis：必要的会话、安全状态和短期缓存，不作为 Opaque Access Token 每请求权威；
- Nacos：配置和服务发现；
- RocketMQ：不作为当前登录闭环前置条件。

## 7. 可用性边界

- IAM 不可用时，新的登录、刷新和 Client Token 获取不可用；
- 已签发 JWT 在签名、时间和本地安全校验通过时可继续使用到过期；
- JWK 应按标准缓存；
- 不为尚未存在的全网实时撤销需求引入复杂投影。

## 8. 延期架构

Resource Catalog、Manifest、Authorization Snapshot、Revocation Feed、完整 Audit 服务、Integration Platform、多租户和高安全 Profile 均按真实需求进入后续迭代。

## 9. 约束

- 不同时维护两套 Access Token 主链路；
- 不把目录骨架写成已交付服务；
- 不把 Gateway 变成授权中心；
- 不强制外部系统采用 Synapse 私有扩展；
- 不在当前迭代顺带扩展平台模块数量。
