# Synapse Platform V1 服务边界

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、核心研发、测试、实施和运维人员 |
| Purpose | 明确 V1 各独立微服务的职责、数据所有权和协作边界 |
| Scope | Gateway、IAM、Resource、Audit、Config、File、Message、Task 及基础能力 |
| Status | Accepted |

## 1. Boundary Principle

```text
Resource  -> 定义有什么资源和权限
IAM       -> 决定谁拥有什么权限，并生成授权快照
各服务     -> 验证快照并执行当前请求权限
Audit     -> 记录授权与访问过程
Config    -> 管理平台级国际化和字典
```

服务之间不共享数据库主键、Entity、Mapper 或 Repository。

## 2. Gateway

Gateway 是平台统一外部入口，负责：

- 路由和基础流量治理；
- Opaque Access Token 授权快照验证；
- 不可信 Header 清理；
- Authorization Bearer Token 原样透传；
- GatewayProof 签发；
- traceId 和请求上下文传播；
- 入口访问记录。

Gateway 不负责：

- 菜单和权限目录管理；
- 角色授权；
- 业务 permission 和数据权限判断；
- 向下游注入用户、角色或权限 Header；
- 代替下游服务验证 Token；
- `gateway:admin` 等业务权限例外。

## 3. IAM

IAM 是身份、认证、会话和授权关系中心，负责：

- 用户、组织和主体管理；
- 角色管理；
- 用户与角色关系；
- 角色与权限码关系；
- 登录、刷新、退出和当前用户；
- OAuth 2.0 / OpenID Connect；
- Opaque Access Token 和 Refresh Token；
- Client 与 Client Credentials；
- 计算用户或 Client 的角色和权限；
- 在 Redis 创建、更新和撤销授权快照；
- Refresh Token rotation 和重放检测。

IAM 不负责：

- 应用、菜单、页面和按钮树；
- API 资源目录；
- 权限码的业务含义和展示元数据；
- 其他服务的资源归属或数据权限判断；
- 直接读取 Resource 数据库。

IAM 保存的授权关系使用稳定 `permission_code`，不使用 Resource 内部主键。

## 4. Resource

`synapse-resource-platform` 是 V1 P0 独立微服务，定位为统一资源与权限目录中心。

Resource 负责：

- 平台应用定义；
- 菜单和页面资源；
- 按钮和操作资源；
- API 资源目录；
- 权限码定义、描述、状态和分类；
- 菜单、按钮、API 与权限码关联；
- 按应用和节点提供可懒加载的授权资源树；
- 提供 `catalogVersion`；
- 批量校验权限码是否存在、启用、可分配且属于目标应用；
- 根据已验证授权快照生成当前用户导航。

Resource 不负责：

- 用户、组织和凭据管理；
- 用户角色关系；
- 角色权限关系持久化；
- Token 签发；
- 替其他服务执行接口权限和数据权限；
- 保存 MES、WMS 等业务资源数据。

## 5. IAM and Resource Association

两个服务使用全局唯一、可读、稳定的权限码关联：

```text
{domain}:{resource}:{action}
```

Resource 是权限码定义的事实来源；IAM 是角色被授予哪些权限码的事实来源。

### Role Grant Flow

```text
管理端 -> Resource 按应用/节点加载资源树和 catalogVersion
管理端 -> IAM 提交 role + selected permissionCodes + catalogVersion
IAM    -> Resource 分批批量校验权限码
IAM    -> 全部校验成功后保存角色与权限关系
IAM    -> 更新相关 Redis 授权快照
Outbox -> RocketMQ -> Audit 记录授权变更
```

单批最多校验 1000 个权限码。Resource 不可用、版本冲突或任一权限码无效时，IAM 不保存新的授权关系。

### Login and Refresh Flow

```text
用户登录或刷新 IAM
  -> IAM 查询用户角色和权限码
  -> IAM 计算最终授权集合
  -> IAM 把授权快照写入 Redis
  -> IAM 返回 Opaque Access Token
```

Token 本身不携带角色、权限、菜单、按钮或路由。

### Navigation Flow

```text
Management Console
  -> Gateway
  -> Resource /me/navigation
  -> Gateway 和 Resource 分别验证 Redis 授权快照
  -> Resource 按权限码过滤菜单与按钮
  -> 返回导航树
```

### API Authorization Flow

```text
Management Console
  -> Gateway 验证快照并透传 Bearer Token
  -> 目标服务再次验证 Redis 授权快照
  -> 目标服务检查 permission_code 和资源权限
  -> 允许或拒绝请求
  -> 可靠审计事件写入本地 Outbox
```

运行时不得为每个请求同步调用 IAM 和 Resource 查询权限。

## 6. Audit

Audit 负责：

- 操作日志；
- 登录、刷新、退出和访问拒绝等安全日志；
- 角色授权、权限回收、资源和 Config 变更等审计日志；
- 记录主体、动作、对象、结果、时间和 traceId；
- 消费 RocketMQ 审计事件；
- 按 `eventId` 幂等保存；
- 为管理端提供检索和查看能力。

Audit 不负责授权判断，也不反向调用 IAM 或 Resource 决定请求是否允许。

## 7. Config

`synapse-config-platform` 是 V1 P1 独立微服务，管理平台级业务配置，不替代 Nacos。

V1 最小闭环：

- 国际化资源；
- 字典类型；
- 字典项。

Config 变更必须遵循平台审计要求。查询缓存和变更失效属于实现支撑，不扩展为完整企业配置中心产品。

Config 不管理：

- 数据库、Redis、Nacos 或 RocketMQ 技术连接配置；
- GatewayProof Secret；
- RSA 私钥；
- 业务系统私有领域配置。

## 8. File, Message and Task

File、Message、Task 为 V1 P1 独立微服务：

- File 拥有文件元数据和存储状态；
- Message 拥有消息、模板和发送记录；
- Task 拥有任务定义、调度和执行记录。

它们都必须独立验证 Opaque Access Token 授权快照，并执行自身接口权限。

## 9. Monitor

V1 不建设独立 Monitor 微服务。

基础运行可见性由 Actuator、服务健康检查、Docker Compose healthcheck 和管理端基础状态展示组成。

## 10. Data Ownership

| Service | Owned Data |
| --- | --- |
| IAM | 用户、组织、角色、授权关系、凭据、Client、Refresh Token 会话和授权快照生命周期 |
| Resource | 应用、菜单、按钮、API 资源、权限码定义及资源关联 |
| Audit | 操作日志、安全日志和审计记录 |
| Config | 国际化资源、字典类型和字典项 |
| File | 文件元数据和存储状态 |
| Message | 消息、模板和发送记录 |
| Task | 任务定义、调度和执行记录 |

Redis 中的 Access Token 授权快照由 IAM 负责生命周期管理，Gateway 和其他服务只读验证。

禁止跨服务数据库外键和跨 Schema SQL 关联。

## 11. Resource Lifecycle

权限码发布后原则上保持稳定。废弃权限时应：

- 标记为禁用或废弃，而不是直接物理删除；
- 停止在资源树和导航中展示；
- 清理 IAM 中相应授权关系；
- 更新或撤销相关 Redis 授权快照；
- 通过 Outbox 记录资源和授权影响。

## 12. V1 Self-contained Verification

```text
Resource 注册平台资源与权限码
  -> IAM 创建角色并批量校验、授予权限码
  -> IAM 为用户分配角色
  -> 用户登录获得 Opaque Access Token
  -> Redis 保存授权快照
  -> Resource 返回过滤后的导航
  -> 各服务验证快照并执行权限
  -> Config 完成国际化和字典最小闭环
  -> Audit 幂等消费并保存可靠事件
```
