# Synapse Platform V1 安全架构（当前基线）

## 核心决策

- Access Token 使用 RS256 JWT；Refresh Token 使用 Opaque Token。
- 用户使用 Authorization Code + PKCE；服务和第三方使用 Client Credentials。
- JWT 只包含身份、Client、Audience、Scope、时间和 USER Session 等最小 Claim。
- JWT 不包含 roles、permissions、菜单、数据范围或租户。
- Gateway 保留 WebFlux Resource Server，只做认证和路由 Audience 校验。
- GatewayProof 已取消，Gateway 不注入可信身份或权限 Header。
- 目标 Resource Server 必须再次验证 JWT，并执行自身功能权限和业务数据规则。
- 一个 Access Token 只面向一个 Audience。
- 第三方不需要 Framework，可以使用标准 JWT/JWK 或 Introspection。

## Token 默认值

| 项目 | 默认值 |
| --- | --- |
| USER Access Token | 10 分钟 |
| CLIENT Access Token | 5 分钟 |
| Clock skew | 60 秒 |
| JWT algorithm | RS256 |
| JWT type | `at+jwt` |

## Gateway

Gateway 负责签名、Issuer、时间、Token 类型和路由 Audience 校验，清理不可信 Header，并转发原始 Bearer Token。

Gateway 不执行 Role、Permission、数据权限或授权快照查询。

## Resource Server

目标服务必须独立验证 JWT，识别 USER / CLIENT，执行功能权限，并由业务系统自行执行数据范围和资源归属规则。

Token 无效返回 401；Token 有效但权限不足返回 403。

## 第三方

第三方可以只使用标准 Token Endpoint、Client Credentials、Bearer Header、JWT/JWK 或 Introspection，不要求引入 Framework 或 Synapse 私有扩展。

## 延期

Authorization Snapshot、`authz_ver`、Resource Manifest、Revocation Feed、多租户、外部 IdP 联合、DPoP、mTLS 和 FAPI 不进入当前 NOW。
