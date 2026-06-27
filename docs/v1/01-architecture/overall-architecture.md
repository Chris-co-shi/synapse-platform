# Synapse Platform V1 总体架构

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、核心研发、测试、实施和运维人员 |
| Purpose | 定义 V1 的总体运行结构、部署单元与数据隔离基线 |
| Scope | Management Console、Gateway、平台微服务及基础设施 |
| Status | Accepted |

产品范围以 [`../00-product/v1-scope.md`](../00-product/v1-scope.md) 为准。

## 1. Architecture Decision

Synapse Platform V1 继续采用当前规划的 **独立微服务架构**，不改为模块化单体。

```text
Browser
  ↓
Management Console
  ↓
Gateway
  ├── IAM                 P0
  ├── Audit               P0
  ├── Resource Service    P0 security execution point
  ├── File                P1
  ├── Message             P1
  └── Task                P1

Infrastructure
  ├── PostgreSQL
  ├── Redis
  ├── Nacos
  └── RocketMQ when required by service design
```

Monitor 在 V1 只提供基础健康状态和运行可见性。Config 只满足平台自身配置需要。Integration 不进入 V1。

## 2. Deployment Units

每个微服务必须：

- 可以独立启动；
- 可以独立构建应用制品和容器镜像；
- 拥有独立配置和健康检查；
- 可以独立停止、重启和升级；
- 不依赖其他服务的内部实现；
- 不通过共享数据库表完成服务协作。

V1 不要求多副本、高可用、自动扩缩容、Service Mesh 或多 Region。

## 3. Service Responsibilities

| Service | Responsibility |
| --- | --- |
| Gateway | 统一入口、路由、基础令牌检查、Header 清理、Trace 传播和基础流量治理 |
| IAM | 身份、认证、OAuth 2.0 / OIDC、RBAC 和授权事实 |
| Audit | 操作日志、安全日志和审计日志 |
| Resource Service | 独立验证令牌并执行服务端权限兜底 |
| File | 文件元数据和基础文件能力 |
| Message | 基础消息、模板和发送记录 |
| Task | 任务调度和执行记录 |

Gateway 不读取业务数据库，也不执行资源归属和数据范围判断。

IAM 是身份与授权事实源，但不是所有权限执行点。资源服务必须独立验证 Token，并执行自己的接口权限和资源授权。

## 4. Engineering Structure

平台服务继续采用：

```text
synapse-xxx-platform
├── synapse-xxx-api
├── synapse-xxx-client
└── synapse-xxx-server
```

- `api`：跨服务稳定契约；
- `client`：调用适配和 SDK；
- `server`：可启动服务实现。

其他服务或业务系统不得依赖另一个服务的 `server` 模块。

## 5. Service Communication

同步查询和需要即时返回的命令，通过正式 API / Client 完成。

允许最终一致、异步处理或削峰的场景，可以使用事件或消息。是否使用 RocketMQ 由具体模块设计确认，不把所有服务交互默认改造成消息通信。

定时、延迟和后台任务由 Task 能力承载。P0 服务在 Task 尚未完成时，不得形成启动级硬依赖。

禁止通过共享数据库、共享 Entity、共享 Mapper 或跨服务 Repository 完成集成。

## 6. Data Ownership

每个服务管理自己的数据：

| Service | Data Ownership |
| --- | --- |
| IAM | 用户、角色、权限、凭据、Client 和令牌相关状态 |
| Audit | 操作日志、安全事件和审计记录 |
| File | 文件元数据、存储定位和文件状态 |
| Message | 消息、模板和发送记录 |
| Task | 任务定义、调度信息和执行记录 |

跨服务需要信息时，应通过 Token Claim、正式 API、Client、事件或经确认的本地快照获取。

## 7. PostgreSQL Isolation

V1 为降低 Docker Compose 部署复杂度，允许所有服务共用一个 PostgreSQL 实例。

```text
One PostgreSQL Instance
  ├── iam schema       -> iam account
  ├── audit schema     -> audit account
  ├── file schema      -> file account
  ├── message schema   -> message account
  └── task schema      -> task account
```

必须遵守：

- 每个服务使用独立 Schema；
- 每个服务使用独立数据库账号；
- 每个账号只拥有本服务 Schema 所需权限；
- 每个服务维护独立 Flyway migration；
- 每个 Schema 维护独立 `flyway_schema_history`；
- 禁止跨 Schema 直接查询和写入；
- 禁止跨服务数据库外键；
- 禁止共享 Entity、Mapper 和 Repository。

单实例独立 Schema 是 V1 的部署简化策略，不得阻碍未来把某个服务迁移到独立数据库实例。

## 8. Docker Compose Baseline

V1 Docker Compose 至少编排：

- Management Console；
- Gateway；
- IAM；
- Audit；
- P0 受保护资源端；
- PostgreSQL；
- Redis；
- Nacos；
- 已完成的 P1 服务及其必要依赖。

RocketMQ 是否默认启动，由 Audit、Message、Task 的最终设计决定。

Compose 需要提供网络、环境变量、持久化卷、健康检查、初始化方式和基本运行验证说明。

## 9. Architecture Constraints

V1 明确禁止：

- 用模块化单体替代当前微服务规划；
- 服务之间直接访问对方数据表；
- 使用分布式大事务作为默认方案；
- 所有交互强制事件化；
- 因为请求经过 Gateway 就跳过下游鉴权；
- 把 Kubernetes 作为 V1 必交付方式。

## 10. Follow-up Documents

下一阶段继续补充：

1. `service-boundary.md`；
2. `security-architecture.md`；
3. `communication-architecture.md`；
4. `data-architecture.md`；
5. Docker Compose 部署设计；
6. Gateway、IAM、Audit 等模块设计。
