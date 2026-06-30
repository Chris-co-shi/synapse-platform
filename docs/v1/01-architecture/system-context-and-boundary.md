# Synapse Platform 系统上下文与边界

## 系统位置

当前 V1 的运行核心是 Gateway 与 IAM。其他模块目录可以保留，但不自动成为 V1 交付范围。

## 系统分类

1. **Synapse 原生系统**：代码、发布和安全配置由我们控制，可以使用 Framework。
2. **可控自研系统**：可以使用 Framework，也可以只使用标准 OAuth2 实现。
3. **外部遗留系统**：MES、WMS、SAP、LIMS、AGV 和旧 Java/.NET 系统默认属于此类。

外部系统不被强制接受 Synapse 私有协议。

## Platform V1 负责

- 用户与 Client 身份；
- OAuth2 / OIDC；
- Opaque Access Token 与 Redis 授权快照；
- Opaque Refresh Token；
- Role、Permission 和 Client Permission；
- Gateway 入口认证；
- 下游 Token 验证契约；
- 基础安全审计；
- 第三方标准 Client Credentials。

OAuth2/OIDC、标准 Client Credentials、OIDC Discovery、ID Token 和 UserInfo 当前未实现，仍属于 V1 计划范围。

## Platform V1 不负责

- MES/WMS/QMS 的业务模型；
- 外部系统内部角色和权限；
- 强制改造遗留登录；
- 企业所有外部连接的统一管理；
- 业务数据范围；
- 多租户；
- 万能集成平台。

## 接入方向

### 外部系统调用 Synapse

支持 OAuth2 时注册为 Confidential Client；不支持时使用项目级 Adapter。

### Synapse 调用外部系统

使用对方认可的认证和接口协议。连接与转换逻辑属于业务 Adapter，不属于 IAM。

### 身份联合

只有出现明确外部 IdP 和真实 SSO 需求时，才在 NEXT 引入 OIDC / SAML 联合。

## Framework 关系

```text
synapse-platform -> synapse-framework
synapse-framework -X-> synapse-platform
```

Framework 是 Java/Spring 官方适配器，不是第三方接入前提。

## V1 闭环

```text
User or Client
  -> IAM issues Opaque Access Token
  -> Gateway validates Redis authorization snapshot
  -> target API validates snapshot again
  -> permission decision and audit
```

当前实现包含 Opaque Access Token、Redis 授权快照和 GatewayProof。不得把该自定义认证闭环写成标准 OAuth2 Authorization Server。
