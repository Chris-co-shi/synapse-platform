# ADR-002：IAM 与 Resource 服务边界

## Status

Accepted

## Context

V1 需要同时解决两类问题：

1. 用户、角色、认证和授权关系；
2. 应用、菜单、按钮、API 和权限码目录。

如果全部放入 IAM，IAM 会逐步同时承担认证服务器、用户中心、角色中心、菜单中心、前端路由中心和 API 资源目录，边界会持续膨胀。

## Decision

`synapse-resource-platform` 作为 V1 P0 独立微服务。

职责划分：

```text
Resource  -> 定义有什么资源和权限
IAM       -> 决定谁拥有什么权限
各服务     -> 执行请求权限
Audit     -> 记录资源、授权和访问行为
```

Resource 负责：

- 应用、菜单、页面和按钮；
- API 资源目录；
- 权限码定义、状态和描述；
- 资源与权限码关联；
- 角色授权所需资源树；
- 当前用户导航。

IAM 负责：

- 用户、组织和角色；
- 用户角色关系；
- 角色权限码关系；
- 认证、OAuth 2.0 / OIDC 和 Token；
- 用户最终权限集合计算。

两个服务通过稳定、全局唯一的 `permission_code` 关联，不共享 Resource 内部主键。

## Runtime Rule

Gateway 只透传 Authorization Bearer Token，不向下游注入用户、角色或权限 Header。

每个受保护服务独立验证 Token，并根据权限码执行自身授权。运行时不为每个请求同步调用 IAM 和 Resource。

## Alternatives

### 全部放入 IAM

实现初期简单，但会让认证、授权关系、菜单和资源目录混合，导致 IAM 边界失控，因此不采用。

### Resource 同时保存角色授权关系

可以减少一次跨服务校验，但会造成用户角色体系与身份中心割裂，因此不采用。

### 仅在前端静态配置菜单

无法形成统一资源目录、动态导航和权限审计，不满足平台产品要求，因此不采用。

## Consequences

正向影响：

- IAM 聚焦身份、认证与授权关系；
- Resource 聚焦资源和权限目录；
- 菜单展示与服务端授权职责清晰；
- 后续业务系统可以复用同一权限目录模型。

代价：

- 角色授权时需要 Resource 提供资源树和权限码校验；
- 资源废弃与 IAM 授权关系需要协同处理；
- 必须维护稳定的权限码规范。

## References

- [`../01-architecture/service-boundary.md`](../01-architecture/service-boundary.md)
- [`../01-architecture/overall-architecture.md`](../01-architecture/overall-architecture.md)
- [`../00-product/v1-scope.md`](../00-product/v1-scope.md)
