# Host-local 认证闭环测试状态

## 文档地位

本文记录当前 host-local 认证链路的最新已确认测试结论，只描述测试事实和已知限制，不代表 OAuth2/OIDC 标准协议已经实现。

## 结论

当前认证闭环状态：`Passed with Known Limitations`。

已知限制：

`Refresh Token reuse detection 尚未完成 token family 整体撤销，successor Refresh Token 在旧 Token 重放后仍可使用。`

因此当前结果不能写成完全 `Passed`。

## 已覆盖事实

当前已确认实现并通过 host-local 真实链路验证的能力：

- 自定义管理端认证接口：`/auth/login`、`/auth/refresh`、`/auth/logout`、`/auth/me`；
- Opaque Access Token；
- Opaque Refresh Token；
- Redis 授权快照；
- Gateway 与 IAM 独立验证快照；
- GatewayProof；
- Refresh rotation；
- 并发 refresh 控制；
- logout；
- Redis 快照缺失返回 401；
- Redis 不可用返回 503。

## 已知缺陷

旧 Refresh Token 被重复使用时：

- 重放请求本身返回 401；
- successor Refresh Token 仍能继续 refresh；
- 同一个 token family 中仍存在 ACTIVE session；
- family 没有被整体撤销。

该缺陷修复前，reuse detection 只能标记为部分实现。

## OAuth2/OIDC 状态

当前 Token 发放属于：

```text
自定义 IAM 登录 API + Opaque Token 会话体系
```

当前尚未实现标准 OAuth 2.0 Authorization Server 协议入口，不能宣称完整支持：

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

## 后续验证顺序

1. 修复 Refresh Token reuse detection 的 token family 整体撤销。
2. 重新执行 host-local 真实链路测试。
3. 建设 OAuth Client 模型和持久化。
4. 接入 Spring Authorization Server。
5. 实现 Client Credentials。
6. 实现 Authorization Code + PKCE。
7. 接入现有 Refresh Session。
8. 实现最小 OIDC 闭环。
