# Synapse Platform V1 总体架构

## 1. 运行拓扑

```text
Browser / Service / Third-party Client
              |
          HTTPS + Bearer Token
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
- Opaque Access Token 授权快照验证；
- Redis 快照缺失返回 401；
- Redis 不可用返回 503；
- Header 清理；
- Bearer Token 转发；
- 当前 GatewayProof 处理；
- 路由和基础流量治理。

Gateway 不执行 Permission，不注入可信身份或权限 Header。

### IAM

- 用户、Client、Role、Permission 和授权关系；
- 自定义 `/auth/login`、`/auth/refresh`、`/auth/logout`、`/auth/me`；
- Opaque Access Token；
- Redis 授权快照；
- Opaque Refresh Token；
- Session、rotation 和 reuse detection；
- 基础安全审计。

OAuth2/OIDC 标准入口、Client Credentials、Authorization Code + PKCE、OIDC Discovery、ID Token 和 UserInfo 当前未实现。

### Target Resource Server

IAM 管理 API 自身以及后续进入 V1 的服务都必须独立验证 Opaque Access Token 授权快照，并执行自身功能权限和数据规则。

## 3. Token 结构

```text
Access Token: Opaque / Redis authorization snapshot
Refresh Token: Opaque / hashed persistence / rotation
```

Access Token 本身不包含 roles、permissions、菜单、数据范围或租户 Claim。

## 4. 通信基线

- 当前用户调用使用 Opaque Bearer Token；
- 服务和第三方 Client Credentials 为计划实现；
- V1 不无限转发用户 Token；
- 不使用共享数据库或可信身份 Header；
- 外部系统协议由项目级 Adapter 处理。

## 5. 数据所有权

V1 的 IAM Schema 保存身份、Client、Role、Permission、Session 和 Refresh Token 摘要。

Gateway 不访问数据库。其他延期模块的 Schema 不是当前 V1 完成条件。

## 6. 基础设施

- PostgreSQL 17：身份与授权事实；
- Redis：授权快照、必要的会话安全状态和短期缓存；
- Nacos：配置和服务发现；
- RocketMQ：不作为当前登录闭环前置条件。

## 7. 可用性边界

- IAM 不可用时，新的登录、刷新和 Client Token 获取不可用；
- Redis 正常时，已签发 Opaque Access Token 依赖授权快照继续验证；
- Redis 不可用时，当前 host-local 结论为返回 503；
- 不为尚未存在的全网实时撤销需求引入复杂投影。

## 8. 延期架构

Resource Catalog、Manifest、Authorization Snapshot、Revocation Feed、完整 Audit 服务、Integration Platform、多租户和高安全 Profile 均按真实需求进入后续迭代。

## 9. 约束

- 不同时维护两套 Access Token 主链路；
- 不把目录骨架写成已交付服务；
- 不把 Gateway 变成授权中心；
- 不强制外部系统采用 Synapse 私有扩展；
- 不在当前迭代顺带扩展平台模块数量。
