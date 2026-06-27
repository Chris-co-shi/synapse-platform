# Synapse Platform V1 服务边界

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、核心研发、测试、实施和运维人员 |
| Purpose | 明确 V1 各独立微服务的职责、数据所有权和协作边界 |
| Scope | Gateway、IAM、Resource、Audit、File、Message、Task 及基础能力 |
| Status | Accepted |

## 1. Boundary Principle

Synapse Platform 按“资源定义、授权关系、权限执行、行为追溯”拆分职责：

```text
Resource  -> 定义有什么资源和权限
IAM       -> 决定谁拥有什么权限
各服务     -> 执行当前请求是否允许
Audit     -> 记录授权与访问过程
```

服务之间通过稳定权限码关联，不共享数据库主键、Entity、Mapper 或 Repository。

## 2. Gateway

Gateway 是平台唯一外部业务入口，负责：

- 路由和基础流量治理；
- Access Token 基础校验；
- 不可信 Header 清理；
- Authorization Bearer Token 透传；
- traceId 和请求上下文传播；
- 入口访问记录。

Gateway 不负责：

- 菜单和权限目录管理；
- 角色授权；
- 业务资源权限和数据权限判断；
- 向下游注入用户、角色或权限 Header；
- 代替下游服务验证 Token。

下游服务只信任经过密码学验证的 Token，不信任 Gateway 注入的身份信息。

## 3. IAM

IAM 是身份、认证和授权关系中心，负责：

- 用户、组织和主体管理；
- 角色管理；
- 用户与角色关系；
- 角色与权限码关系；
- 登录、刷新、退出和当前用户；
- OAuth 2.0 / OpenID Connect；
- Access Token、Refresh Token 和 Client；
- 计算用户最终角色和权限集合；
- 敏感授权变更后的会话撤销策略。

IAM 不负责：

- 应用、菜单、页面和按钮树；
- API 资源目录；
- 权限码的业务含义和展示元数据；
- 其他服务的资源归属或数据权限判断；
- 直接读取 Resource 数据库。

IAM 保存的授权关系使用稳定的 `permission_code`，不使用 Resource 服务内部主键。

## 4. Resource

`synapse-resource-platform` 是 V1 P0 独立微服务，定位为统一资源与权限目录中心，不是通用业务数据服务。

Resource 负责：

- 平台应用定义；
- 菜单和页面资源；
- 按钮和操作资源；
- API 资源目录；
- 权限码定义、描述、状态和分类；
- 菜单、按钮、API 与权限码的关联；
- 向角色授权页面提供可选择的资源树；
- 校验权限码是否存在、启用且可分配；
- 根据已验证 Token 中的权限集合生成当前用户导航。

Resource 不负责：

- 用户、组织和凭据管理；
- 用户角色关系；
- 角色权限关系持久化；
- Token 签发；
- 替其他服务执行接口权限和数据权限；
- 保存 MES、WMS 等业务资源数据。

## 5. IAM and Resource Association

### 5.1 Stable Association Key

两个服务使用全局唯一、可读、稳定的权限码关联：

```text
{domain}:{resource}:{action}
```

例如：

```text
iam:user:read
iam:user:create
iam:role:grant
resource:menu:update
audit:security-log:read
```

Resource 是权限码定义的事实来源；IAM 是角色被授予哪些权限码的事实来源。

### 5.2 Role Grant Flow

```text
管理端 -> Resource 获取资源与权限树
管理端 -> IAM 提交 role + permissionCodes
IAM    -> Resource 批量校验权限码
IAM    -> 保存角色与权限码关系
Audit  -> 记录授权变更
```

IAM 调用 Resource 失败时，不应保存未经校验的新授权关系。具体失败和重试策略由模块设计确认。

### 5.3 Login Flow

```text
用户登录 IAM
  -> IAM 查询用户角色
  -> IAM 查询角色权限码
  -> IAM 计算最终权限集合
  -> IAM 签发 Access Token
```

V1 可以在 Access Token 中携带角色与权限码。菜单树、按钮元数据和前端路由不进入 Token。

### 5.4 Navigation Flow

```text
Management Console
  -> Gateway
  -> Resource /me/navigation
  -> Resource 验证 Token
  -> 按权限码过滤启用的菜单与按钮
  -> 返回导航树
```

前端菜单和按钮过滤只改善用户体验，不能代替服务端鉴权。

### 5.5 API Authorization Flow

```text
Management Console
  -> Gateway 进行入口处理并透传 Bearer Token
  -> 目标服务独立验证 Token
  -> 目标服务检查所需 permission_code
  -> 允许或拒绝请求
  -> Audit 记录关键结果
```

运行时鉴权不得在每个请求中同步调用 IAM 和 Resource。主要依据应是经过验证的 Token 或经确认的授权快照。

## 6. Audit

Audit 负责：

- 操作日志；
- 登录、刷新、退出、访问拒绝等安全日志；
- 角色授权、权限回收、资源变更等审计日志；
- 记录主体、动作、对象、结果、时间和 traceId；
- 为管理端提供检索和查看能力。

Audit 不负责授权判断，也不反向调用 IAM 或 Resource 决定请求是否允许。

## 7. File, Message and Task

File、Message、Task 为 V1 P1 独立微服务：

- File 拥有文件元数据和存储状态；
- Message 拥有消息、模板和发送记录；
- Task 拥有任务定义、调度和执行记录。

它们都必须作为 OAuth2 Resource Server 独立验证 Token，并执行自身接口权限。

## 8. Data Ownership

| Service | Owned Data |
| --- | --- |
| IAM | 用户、组织、角色、用户角色关系、角色权限码关系、凭据、Client 和 Token 状态 |
| Resource | 应用、菜单、按钮、API 资源、权限码定义及资源关联 |
| Audit | 操作日志、安全日志和审计记录 |
| File | 文件元数据和存储状态 |
| Message | 消息、模板和发送记录 |
| Task | 任务定义、调度和执行记录 |

禁止跨服务数据库外键和跨 Schema SQL 关联。

## 9. Resource Lifecycle

权限码发布后原则上保持稳定。废弃权限时应：

- 标记为禁用或废弃，而不是直接物理删除；
- 停止在资源树和导航中展示；
- 清理或失效 IAM 中相应授权关系；
- 记录资源变更和授权影响；
- 对高风险权限回收触发必要的会话撤销。

## 10. V1 Self-contained Verification

平台自身至少通过以下链路验证边界：

```text
Resource 注册平台应用、菜单、按钮、API 和权限码
  -> IAM 创建角色并授予权限码
  -> IAM 为用户分配角色
  -> 用户登录获得 Token
  -> Resource 返回过滤后的导航
  -> 各平台服务执行权限校验
  -> Audit 记录资源、授权和访问行为
```

该链路完成后，再考虑 MES、WMS 等外部业务系统接入。
