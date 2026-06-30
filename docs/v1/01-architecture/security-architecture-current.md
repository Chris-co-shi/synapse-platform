# Synapse Platform V1 安全架构（当前实现状态）

## 核心决策

- 当前 Token 发放属于自定义 IAM 登录 API + Opaque Token 会话体系。
- 当前已实现 `/auth/login`、`/auth/refresh`、`/auth/logout`、`/auth/me`。
- Access Token 使用 Opaque Token；Refresh Token 使用 Opaque Token。
- IAM 在 Redis 保存授权快照。
- Gateway 与 IAM 独立验证授权快照。
- 当前实现包含 GatewayProof，Gateway 不注入可信身份或权限 Header。
- Redis 授权快照缺失返回 401；Redis 不可用返回 503。
- 当前 host-local 认证闭环状态为 `Passed with Known Limitations`。
- OAuth2/OIDC 标准协议入口为 `Planned / Not Implemented`。

## Token 默认值

| 项目 | 默认值 |
| --- | --- |
| Access Token | Opaque |
| Refresh Token | Opaque |
| 授权快照 | Redis |
| 当前测试结论 | `Passed with Known Limitations` |

## Gateway

Gateway 负责 Opaque Access Token 授权快照验证、清理不可信 Header、当前 GatewayProof 处理，并转发原始 Bearer Token。

Gateway 不执行 Role、Permission 或数据权限。

## Resource Server

目标服务必须独立验证 Opaque Access Token 授权快照，识别 USER / CLIENT，执行功能权限，并由业务系统自行执行数据范围和资源归属规则。

Token 无效返回 401；Token 有效但权限不足返回 403。

## 第三方

标准第三方接入尚未实现。后续目标是第三方可以只使用标准 Token Endpoint、Client Credentials 和 Bearer Header，不要求引入 Framework 或 Synapse 私有扩展。

## 已知缺陷

Refresh Token reuse detection 尚未完成 token family 整体撤销。旧 Refresh Token 重放请求本身返回 401，但 successor Refresh Token 仍能继续 refresh，同一个 token family 中仍存在 ACTIVE session。

## 未实现

- `/oauth2/token`；
- `grant_type`；
- Client Authentication；
- Client Credentials；
- Authorization Code；
- PKCE；
- OAuth2 标准错误响应；
- OIDC Discovery；
- ID Token；
- UserInfo。

## 延期

普通用户公开自主注册、公开 Dynamic Client Registration、Resource Manifest、Revocation Feed、多租户、外部 IdP 联合、DPoP、mTLS 和 FAPI 不进入当前 NOW。
