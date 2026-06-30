# Synapse Platform 仓库差距分析

本文记录当前仓库、host-local 测试结论与 [V1 架构基线](../00-product/v1-baseline.md) 的差距。它是迁移清单，不代表计划能力已经实现。

## 总体判断

当前已确认实现的是自定义 IAM 登录 API + Opaque Token 会话体系：`/auth/login`、`/auth/refresh`、`/auth/logout`、`/auth/me`，Opaque Access Token，Opaque Refresh Token，Redis 授权快照，Gateway 与 IAM 独立验证快照，GatewayProof，Refresh rotation，并发 refresh 控制，logout，以及 Redis 快照缺失 401、Redis 不可用 503。

当前尚未实现标准 OAuth 2.0 Authorization Server 协议入口，不能宣称已经完整支持 `/oauth2/token`、`grant_type`、Client Authentication、Client Credentials、Authorization Code、PKCE、OAuth2 标准错误响应、OIDC Discovery、ID Token 或 UserInfo。

最新 host-local 认证闭环结论应记录为 `Passed with Known Limitations`，不能写成完全 Passed。已知限制是 Refresh Token reuse detection 尚未完成 token family 整体撤销：旧 Refresh Token 重放请求本身返回 401，但 successor Refresh Token 仍可继续 refresh，同一个 token family 中仍存在 ACTIVE session。

## 差距表

| 编号 | 当前事实 | 目标 | 成本 | 决策 |
| --- | --- | --- | --- | --- |
| P-001 | 当前为自定义 `/auth/*` 登录 API + Opaque Token 会话体系 | 标准 OAuth2/OIDC 协议入口仍需建设 | L | NOW |
| P-002 | 当前实现 Opaque Access Token 与 Redis 授权快照 | V1 接受 Opaque Access Token，不迁移为 JWT Access Token | M | NOW |
| P-003 | GatewayProof 当前存在于实现和测试中 | 文档必须明确它是当前实现能力，不得写成已取消实现事实 | S | NOW |
| P-004 | Gateway 默认配置 13 个服务路由 | 只保留实际部署路由 | S | NOW |
| P-005 | Gateway 端口为 20000 | 统一为 8080 | S | NOW |
| P-006 | OAuth2 标准 Token Endpoint、grant_type、Client Authentication 未实现 | 接入 Spring Authorization Server | L | NOW |
| P-007 | Client Credentials 标准链路未实现 | 实现标准 Client Credentials | M | NOW |
| P-008 | Resource 被定义为 V1 P0 | V1 的 Resource/Scope/Permission 最小模型先收口在 IAM | M | NOW |
| P-009 | 13 个一级模块都进入根 reactor | 仓库可保留骨架，但 V1 部署不得把骨架视为交付 | M | NEXT |
| P-010 | IAM V1 包含复杂组织中心 | 当前只保留用户、Client、Role、Permission | M | NOW |
| P-011 | 完整 Audit/Outbox/RocketMQ 是登录闭环前置条件 | 完整审计服务延期，基础安全审计保留 | L | NEXT |
| P-012 | 文档默认 MES/WMS 接入统一快照 | 外部业务系统默认黑盒 | S | NOW |
| P-013 | Platform 要求 Maven 3.9.x，Framework 当前支持 3.8.6 | 两仓库统一构建基线 | S | NOW |
| P-014 | Refresh Token reuse detection 未撤销整个 token family | 旧 Token 重放后撤销 family、successor 和 ACTIVE session | M | NOW |
| P-015 | 普通用户注册和 OAuth Client 注册边界易混淆 | 用户由 bootstrap/管理员创建，OAuth Client 由管理员登记，V1 不做公开 Dynamic Client Registration | S | NOW |

## Framework 对接差距

| 编号 | 当前事实 | 决策 |
| --- | --- | --- |
| F-001 | `synapse-security` 包含 GatewayProof 生产能力 | 本任务只修正文档，不修改 Framework |
| F-002 | Resource Server 存在 JWT/Opaque 两类历史口径 | 文档按当前 Opaque + Redis 快照和后续 OAuth2 计划拆分 |
| F-003 | WebMVC/WebFlux 已具备资源服务能力 | 保留 |
| F-004 | WebFlux 可由 Gateway 自定义安全链 | 保留并继续测试 Gateway 行为 |
| F-005 | denylist 已存在但分布式撤销未闭环 | 默认关闭，不阻塞 V1 |

## 实施顺序

1. 修复 Refresh Token reuse detection 的 token family 整体撤销。
2. 重新执行 host-local 真实链路测试。
3. 建设 OAuth Client 模型和持久化。
4. 接入 Spring Authorization Server。
5. 实现 Client Credentials。
6. 实现 Authorization Code + PKCE。
7. 接入现有 Refresh Session。
8. 实现最小 OIDC 闭环。

## 延期模块

Resource、Audit、Config、File、Message、Task、Workflow、Integration、MDM、Report、Monitor 可以保留目录，但必须标为 NEXT/LATER，不作为当前 V1 完成条件。是否移出默认 reactor，在代码迁移任务中决定。

## 高风险事项

- 把自定义 `/auth/login` 写成标准 OAuth2 Token Endpoint；
- 把 host-local 测试写成完全 Passed；
- 把 reuse detection 写成已完成；
- 把计划中的 OAuth2/OIDC 写成已实现；
- 在未修复 family 撤销前继续允许 successor Refresh Token 使用；
- 把延期模块继续描述为 V1 已交付。

后续 Codex 每个任务只能处理一个 Gap，并同步更新本文状态。
