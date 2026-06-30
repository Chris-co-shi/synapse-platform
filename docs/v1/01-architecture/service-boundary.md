# Synapse Platform V1 服务边界

## 1. 当前 V1 边界

```text
Gateway -> 入口认证和路由
IAM     -> 身份、Token、授权关系和管理 API
Service -> 独立 Token 验证和最终授权
Adapter -> 异构外部系统协议转换
```

## 2. Gateway

负责：

- WebFlux 路由；
- Opaque Access Token 授权快照验证；
- Redis 快照缺失返回 401；
- Redis 不可用返回 503；
- Header 清理；
- Bearer Token 转发；
- 当前 GatewayProof 处理；
- Trace 和基础流量治理。

不负责：

- Role、Permission 或数据权限；
- 可信身份 Header；
- 数据库访问；
- 替代下游 Token 验证。

## 3. IAM

负责：

- 用户和凭据；
- OAuth Client 和凭据轮换；
- Role、Permission、用户直接授权、Client Permission；
- Resource Identifier 与 Scope 的最小配置；
- 自定义 `/auth/login`、`/auth/refresh`、`/auth/logout`、`/auth/me`；
- Opaque Access Token；
- Redis 授权快照；
- Opaque Refresh Token 和 Session；
- rotation 和 reuse detection；
- 基础安全审计。

OAuth2/OIDC、标准 `/oauth2/token`、Client Authentication、Client Credentials、Authorization Code + PKCE、OIDC Discovery、ID Token 和 UserInfo 当前未实现，属于 V1 后续计划。

不负责：

- MES/WMS 等业务数据；
- 业务数据范围和资源归属；
- 外部系统协议转换；
- 复杂组织中心和多租户；
- 企业全部外部连接配置。

## 4. Resource Server

每个目标服务必须：

1. 验证 Opaque Access Token 授权快照；
2. 识别 USER 或 CLIENT；
3. 执行自身功能权限；
4. 执行自身数据权限和领域规则；
5. 写入稳定审计主体。

Framework 可以提供官方实现，但不是接入前提。

## 5. Resource 模块

`synapse-resource-platform` 当前调整为 NEXT 候选。

V1 不依赖独立 Resource 服务，不建设 Manifest、目录版本、授权快照或 IAM Catalog Projection。Resource/Scope/Permission 的最小模型先由 IAM 管理。

目录保留不代表服务已经进入 V1 部署和验收。

## 6. Audit、Config、File、Message、Task

这些模块可以继续独立演进，但当前均不阻塞身份与访问 V1。

基础安全审计先在 IAM 内形成可测试记录；完整 Audit 服务、Outbox 和 RocketMQ 可靠闭环进入 NEXT。

## 7. External Adapter

外部系统不支持 Synapse 标准协议时，使用项目级 Adapter / Anti-Corruption Layer：

```text
Legacy System <-> Adapter <-> Synapse API
Synapse Service <-> Adapter <-> External API
```

Adapter 不进入 IAM 领域；在出现多个重复场景前，不抽象为通用 Integration Platform。

## 8. 工程分层

除 Gateway 外，领域模块可以采用：

```text
synapse-xxx-platform
├── synapse-xxx-api
├── synapse-xxx-client
└── synapse-xxx-server
```

- api 不依赖 client/server；
- client 不依赖 server；
- server 不依赖自己的 client 或其他领域 server；
- 禁止跨服务数据库访问和共享持久化类型。

## 9. 当前数据所有权

| 组件 | 当前 V1 数据 |
| --- | --- |
| IAM | 用户、Client、Role、Permission、授权关系、Session、Refresh Token 摘要；OAuth2/OIDC JWK 元数据为后续计划 |
| Gateway | 无业务数据库 |
| Resource / Audit / Config / File / Message / Task | 非当前 V1 完成前置条件 |

## 10. 变更规则

新增独立服务前必须证明：

- 有至少一个真实消费者；
- 不能合理放在现有边界；
- 独立部署收益大于数据一致性和运维成本；
- 已明确 NOW/NEXT/LATER；
- 有可验收的端到端场景。
