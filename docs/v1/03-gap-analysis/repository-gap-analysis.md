# Synapse Platform 仓库差距分析

本文记录 2026-06-29 `main` 分支代码与 [V1 架构基线](../00-product/v1-baseline.md) 的差距。它是迁移清单，不代表能力已经实现。

## 总体判断

现有仓库以 Opaque Access Token、Redis 授权快照、GatewayProof、独立 Resource/Audit/Config 等服务为 V1 核心；新基线收口为 JWT Access Token、Opaque Refresh Token、Gateway Authentication Only 和 IAM 身份访问闭环。因此需要明确迁移，不能继续混用两套安全模型。

## 差距表

| 编号 | 当前事实 | 目标 | 成本 | 决策 |
| --- | --- | --- | --- | --- |
| P-001 | V1 文档以 Opaque Access Token 和 Redis 快照为核心 | JWT Access Token | L | NOW |
| P-002 | Gateway 配置和文档启用 GatewayProof | 完全取消 GatewayProof | S | NOW |
| P-003 | Gateway 使用单一 `synapse-platform` Audience | 按路由验证目标 Audience | M | NOW |
| P-004 | Gateway 默认配置 13 个服务路由 | 只保留实际部署路由 | S | NOW |
| P-005 | Gateway 端口为 20000 | 统一为 8080 | S | NOW |
| P-006 | IAM 规则明确禁止 JWT Access Token | IAM 使用 RS256 JWT Access Token | M | NOW |
| P-007 | JWT Resource Server 已具备 issuer/audience/time 校验基础 | 继续复用并补充标准 Claim Profile | M | NOW |
| P-008 | Resource 被定义为 V1 P0 | V1 的 Resource/Scope/Permission 最小模型先收口在 IAM | M | NOW |
| P-009 | 13 个一级模块都进入根 reactor | 仓库可保留骨架，但 V1 部署不得把骨架视为交付 | M | NEXT |
| P-010 | IAM V1 包含复杂组织中心 | 当前只保留用户、Client、Role、Permission | M | NOW |
| P-011 | 完整 Audit/Outbox/RocketMQ 是登录闭环前置条件 | 完整审计服务延期，基础安全审计保留 | L | NEXT |
| P-012 | 文档默认 MES/WMS 接入统一快照 | 外部业务系统默认黑盒 | S | NOW |
| P-013 | Platform 要求 Maven 3.9.x，Framework 当前支持 3.8.6 | 两仓库统一构建基线 | S | NOW |

## Framework 对接差距

| 编号 | 当前事实 | 决策 |
| --- | --- | --- |
| F-001 | `synapse-security` 包含 GatewayProof 生产能力 | NOW：删除 |
| F-002 | Resource Server 从 JWT 映射 roles、permissions、tenant | NOW：移除默认映射 |
| F-003 | WebMVC/WebFlux 已具备 JWT 验证和主体桥接 | 保留 |
| F-004 | WebFlux 可由 Gateway 自定义安全链 | 保留并增加 Authentication Only 测试 |
| F-005 | denylist 已存在但分布式撤销未闭环 | 默认关闭，不阻塞 V1 |

## 实施顺序

1. **文档与契约**：统一 V1 scope、安全模型和模块边界。
2. **Framework**：删除 GatewayProof，调整 JWT Claim 与 Authority 映射。
3. **IAM**：完成 SAS/OIDC、JWT、JWK、Refresh rotation、USER/CLIENT 和 RBAC。
4. **Gateway**：删除 proof，改为 8080，按路由验证 Audience。
5. **验收**：用户登录、服务调用、第三方 Client Credentials 三条链路端到端通过。

## 延期模块

Resource、Audit、Config、File、Message、Task、Workflow、Integration、MDM、Report、Monitor 可以保留目录，但必须标为 NEXT/LATER，不作为当前 V1 完成条件。是否移出默认 reactor，在代码迁移任务中决定。

## 高风险事项

- 同时保留 Opaque 和 JWT 两套 Access Token 路径；
- 文档取消 GatewayProof，但配置或代码仍启用；
- Gateway 使用宽泛 Audience；
- JWT 继续携带 roles/permissions；
- 把延期模块继续描述为 V1 已交付。

后续 Codex 每个任务只能处理一个 Gap，并同步更新本文状态。
