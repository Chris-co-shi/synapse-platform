# Synapse Platform V1 数据库与持久化规范

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、后端研发、测试、数据库维护和代码审查人员 |
| Purpose | 统一 V1 的实体基类、主键、命名、逻辑删除、乐观锁和迁移规则 |
| Scope | Platform 各微服务的 PostgreSQL、Flyway 和 MyBatis-Plus 持久化实现 |
| Status | Accepted |

本文是 Platform 数据库开发规范。总体数据边界以 [`../01-architecture/data-architecture.md`](../01-architecture/data-architecture.md) 为准。

## 1. Framework Entity Hierarchy

Platform 使用 Framework `synapse-mybatis-plus` 已提供的实体基类，不在 Platform 中重复定义公共持久化基类。

```text
IdEntity
  └── CreatedEntity
       └── MutableEntity
            └── VersionedEntity
                 └── ManagedEntity
```

| Base Class | Standard Fields | Capability |
| --- | --- | --- |
| `IdEntity` | `id` | String 主键与 `ASSIGN_ID` |
| `CreatedEntity` | `createdAt`, `createdBy` | 创建审计 |
| `MutableEntity` | `updatedAt`, `updatedBy` | 修改审计 |
| `VersionedEntity` | `revision` | MyBatis-Plus 乐观锁 |
| `ManagedEntity` | `deleted` | 乐观锁 + 逻辑删除 |

规则：

- 只有 `infrastructure.persistence.entity` 下的 MyBatis-Plus Entity 可以继承这些基类；
- Domain Model、API DTO、Command、Query 和 Event DTO 不得继承持久化实体基类；
- Entity 按实际能力选择最浅的基类，禁止所有表机械继承 `ManagedEntity`；
- 关系表、日志、Outbox、Inbox、会话和执行记录可以完全不继承这些基类；
- Platform 不复制 Framework 已提供的相同字段和注解；
- Framework 基类变化属于跨仓库契约变化，需要同步验证 Platform。

## 2. Primary Key

V1 默认实体主键：

```text
Java type: String
PostgreSQL type: varchar(19)
MyBatis-Plus: IdType.ASSIGN_ID
```

Framework `IdEntity` 已定义 `String id` 和 `@TableId(type = IdType.ASSIGN_ID)`。

规则：

- 对外 API 中数据库 ID 始终按字符串传输；
- 禁止前端把 ID 转换为 JavaScript Number；
- 默认不依赖 PostgreSQL 自增序列生成业务实体 ID；
- 数据库主键、业务编码、事件 ID、Token 和 traceId 是不同概念；
- `permission_code`、字典编码和应用编码使用独立业务字段，不充当数据库主键；
- 特殊表需要联合主键或其他主键策略时，必须在模块设计中说明，不为迁就基类强制增加无意义 ID。

## 3. Audit Fields

标准字段：

| Java | PostgreSQL | Meaning |
| --- | --- | --- |
| `createdAt` | `created_at` | UTC 创建时间 |
| `createdBy` | `created_by` | 创建主体标识 |
| `updatedAt` | `updated_at` | UTC 最后修改时间 |
| `updatedBy` | `updated_by` | 最后修改主体标识 |

时间字段使用 `Instant`，数据库使用 `timestamptz`。

审计字段由 Framework MyBatis-Plus 适配基于 `OperationContext` 填充。没有可信操作主体时，不得伪造普通用户；后台任务需要显式建立 SYSTEM 或 CLIENT 操作上下文。

## 4. Database Naming

数据库标识统一使用未加双引号的 `lower_snake_case`。

禁止：

- 驼峰表名和字段名；
- 依赖双引号保留大小写；
- 使用数据库关键字；
- 无统一含义的缩写；
- 在独立 Schema 内重复服务名称前缀。

### 4.1 Schema and Table

服务归属由 Schema 表达，表名使用单数业务名，不再重复服务前缀。

推荐：

```text
iam.user_account
iam.user_credential
iam.role
iam.user_role
iam.role_permission

resource.application
resource.resource_node
resource.permission_definition

config.i18n_resource
config.dictionary_type
config.dictionary_item
```

不推荐：

```text
iam.iam_user
iam.iam_role
resource.resource_resource
```

现有表若仍使用服务前缀，不得只修改 `@TableName`。必须通过正式 Flyway migration、兼容评估和升级测试完成迁移。

### 4.2 Columns

标准字段：

| Meaning | Column |
| --- | --- |
| 主键 | `id` |
| 关联标识 | `<entity>_id` |
| 创建时间 | `created_at` |
| 创建人 | `created_by` |
| 更新时间 | `updated_at` |
| 更新人 | `updated_by` |
| 技术乐观锁 | `revision` |
| 逻辑删除 | `deleted` |
| 状态 | `status` |
| 启用状态 | `enabled` |

布尔列使用肯定语义，例如 `enabled`、`assignable`、`system_builtin`。不统一使用 `is_` 前缀。

### 4.3 Constraints and Indexes

```text
pk_<table>
uk_<table>__<columns>
idx_<table>__<columns>
fk_<table>__<column>
ck_<table>__<rule>
```

例如：

```text
pk_user_account
uk_user_account__normalized_username
idx_user_account__status
fk_user_role__user_id
ck_dictionary_item__status
```

唯一业务约束必须由数据库唯一约束保护，不能只依赖应用层预检查。

## 5. Optimistic Lock

继承 `VersionedEntity` 或 `ManagedEntity` 的实体使用：

```text
Java: Integer revision
PostgreSQL: revision integer not null default 0
```

`revision` 是技术并发控制字段，不是业务版本号。

业务修订版需要使用其他明确字段，例如 `document_revision`、`template_version` 或 `business_version`。

适合乐观锁的对象：

- 用户、角色等可并发编辑的聚合根；
- 应用、资源节点和权限定义；
- 国际化资源、字典、模板和任务定义。

通常不使用普通乐观锁：

- 关系表；
- 追加型日志；
- Outbox、Inbox 和消费幂等记录；
- 消息发送和任务执行记录；
- Access Token 授权快照。

更新影响行数为 0 且目标仍存在时，应转换为并发冲突，HTTP 层返回 `409 Conflict`。禁止静默覆盖或自动重试用户编辑。

Refresh Token rotation 等安全状态转换使用显式条件更新和受影响行数判断，不只依赖通用 `@Version`。

## 6. Logical Delete

继承 `ManagedEntity` 的实体使用：

```text
Java: Integer deleted
PostgreSQL: deleted smallint not null default 0
0 = active
1 = deleted
```

逻辑删除是显式选择的实体能力，不启用“所有表都必须逻辑删除”的业务规则。

适合逻辑删除：

- 需要恢复或保留历史标识的管理型主数据；
- 删除后仍需要支持历史审计引用的数据。

不使用逻辑删除：

- 用户角色、角色权限等关系表；
- Refresh Token 会话和安全状态记录；
- Audit、Outbox、Inbox 和消费幂等记录；
- 消息发送记录和任务执行记录；
- Redis 授权快照。

`DISABLED`、`DEPRECATED` 和 `DELETED` 是不同状态。停用、废弃不得通过逻辑删除代替。

安全标识和历史引用所使用的业务编码，逻辑删除后原则上不允许被新对象复用，除非模块设计明确证明不会破坏审计语义。

## 7. Entity Selection Guide

| Data Type | Recommended Base |
| --- | --- |
| 只需要 ID | `IdEntity` |
| 创建后不可修改的业务记录 | `CreatedEntity` |
| 可修改但不需要并发控制 | `MutableEntity` |
| 可并发编辑、不需要逻辑删除 | `VersionedEntity` |
| 可并发编辑且需要逻辑删除 | `ManagedEntity` |
| 关系表、日志、Outbox 等特殊模型 | 不强制继承，按表设计 |

选择基类必须以真实生命周期为依据，而不是为了减少几行字段定义。

## 8. Repository Boundary

- Entity 位于 `infrastructure.persistence.entity`；
- Mapper 位于 `infrastructure.persistence.mapper`；
- Repository Port 位于 `domain.repository`；
- Repository Adapter 位于 `infrastructure.persistence.repository`；
- Converter 位于 `infrastructure.persistence.converter`；
- Domain Model 不依赖 MyBatis-Plus；
- Controller 不直接依赖 Mapper；
- 默认不使用 `IService` / `ServiceImpl` 作为业务 Service 基类；
- Entity 禁止继承 MyBatis-Plus `Model<T>`；
- 外部排序字段必须经过白名单映射。

## 9. Flyway Naming and Rules

每个服务使用独立版本序列：

```text
V1__init_schema.sql
V2__create_user_account.sql
V3__create_role_and_permission.sql
```

规则：

- 已进入共享环境的 migration 禁止修改；
- 修复通过新增 migration 完成；
- 各服务版本号不需要全局对齐；
- migration 失败必须阻止服务启动；
- 发布前验证空库初始化和已有版本升级；
- 表名、字段名或 Schema 迁移必须提供兼容和回滚分析；
- 不通过手工数据库修改代替 migration。

## 10. Minimum Tests

涉及持久化实体或表结构的修改至少覆盖：

- insert 与审计字段填充；
- update 与审计字段刷新；
- `ASSIGN_ID` 字符串主键；
- 乐观锁成功和冲突；
- 逻辑删除查询隔离；
- 唯一约束冲突；
- Repository 与 Domain 转换；
- PostgreSQL 17 空库 migration；
- 从当前发布基线升级 migration。

只使用较浅基类的实体，只测试其实际拥有的能力，不虚构逻辑删除或乐观锁行为。

## 11. Current Migration Note

IAM 当前部分持久化实体仍使用 `iam_` 表名前缀。该现状不改变本规范，后续在 IAM 数据模型和 Flyway 设计中通过正式迁移收口，禁止直接改注解造成运行时表不存在。
