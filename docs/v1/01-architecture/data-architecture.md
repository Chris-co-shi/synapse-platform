# Synapse Platform V1 数据架构

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、核心研发、测试、实施和运维人员 |
| Purpose | 定义 V1 的数据库、Redis、迁移、事务、一致性与时间时区基线 |
| Scope | PostgreSQL、Redis、Flyway、Outbox 及各服务数据边界 |
| Status | Accepted |

## 1. Database Positioning

PostgreSQL 17 是 Synapse Platform V1：

- Docker Compose 默认数据库；
- 推荐使用的数据库；
- 开发、迁移、集成测试和发布验收的实际验证目标。

Synapse Framework 已提供部分其他数据库兼容能力，但这不等于 Synapse Platform 已完成其他数据库的兼容验证。

V1 不宣称 PostgreSQL 17 是唯一可能运行的数据库，也不承诺其他数据库已被 Platform 正式支持。其他数据库只有完成迁移脚本、方言验证、集成测试和部署文档后，才能进入 Platform 支持矩阵。

## 2. Data Ownership

| Service | Owned Data |
| --- | --- |
| IAM | 用户、组织、角色、授权关系、凭据、Client、Refresh Token 会话和安全状态 |
| Resource | 应用、菜单、按钮、API、权限码、资源关系和目录版本 |
| Audit | 操作日志、安全日志、审计事件和消费幂等记录 |
| Config | 国际化资源、字典类型和字典项 |
| File | 文件元数据、存储定位和状态 |
| Message | 消息、模板、发送任务和发送记录 |
| Task | 任务定义、调度信息和执行记录 |

服务不得直接读取、修改或关联其他服务的数据表。跨服务关系只保存公开稳定标识，不建立跨服务数据库外键。

## 3. PostgreSQL Isolation

V1 允许所有服务共用一个 PostgreSQL 17 实例：

```text
PostgreSQL 17
  ├── iam schema
  ├── resource schema
  ├── audit schema
  ├── config schema
  ├── file schema
  ├── message schema
  └── task schema
```

必须满足：

- 每个服务独立 Schema；
- 每个服务独立数据库账号；
- 账号只拥有本服务 Schema 所需权限；
- 每个服务独立 Flyway migration 和 `flyway_schema_history`；
- 禁止跨 Schema SQL；
- 禁止跨服务数据库外键；
- 禁止共享 Entity、Mapper 和 Repository；
- 禁止通过视图或存储过程绕过服务边界。

共用实例只是 V1 部署简化策略，不阻碍未来迁移到独立数据库实例。

## 4. Database Portability

- 通用 SQL 能满足需求时优先使用通用 SQL；
- PostgreSQL 专有能力必须有明确收益；
- 专有 SQL、类型和索引封装在所属服务基础设施层；
- 不把数据库方言泄漏到跨服务 API 和领域模型；
- 其他数据库兼容必须由 Platform 实际测试证明；
- V1 不要求为未经验证的数据库维护多套 Flyway 脚本。

## 5. Flyway

- 各服务独立管理 migration 版本；
- 已进入共享环境的 migration 禁止修改；
- 修复通过新增 migration 完成；
- 初始化数据与结构迁移明确区分；
- 默认管理员、权限目录和基础字典初始化必须幂等或具备重复执行保护；
- 一个服务不得创建或修改其他服务 Schema 的业务对象；
- 发布前验证空库初始化和已有版本升级两条路径。

账号、Schema 和授权可由部署初始化脚本创建；业务表只由所属服务 Flyway 创建。

## 6. Transaction and Consistency

单个服务内部使用本地事务。跨服务不使用共享事务或默认依赖分布式大事务。

需要立即确认的操作使用同步 API，例如 IAM 保存角色授权前同步校验 Resource 权限码。

允许最终一致的变更使用：

```text
local transaction
  -> business data
  -> local outbox
  -> commit
  -> RocketMQ
  -> idempotent consumer
```

## 7. Outbox Data

Outbox 至少表达：

- `eventId`、`eventType`、`eventVersion`；
- `aggregateType`、`aggregateId`；
- `occurredAt`、`traceId`；
- `actorType`、`actorId`；
- payload、投递状态、重试次数和下次重试时间。

规则：

- `eventId` 全局唯一；
- 业务数据与 Outbox 同事务提交；
- 投递器使用原子 claim；
- 允许重复投递，消费者必须幂等；
- 未完成事件不得静默删除；
- 保留和清理周期由运维与模块设计确认。

## 8. Redis Ownership

Redis 用于运行时和缓存数据，不作为跨服务共享业务数据库。

每类 Redis 数据必须有明确所有者、Key 命名空间、TTL 和故障策略。

### 8.1 Authorization Snapshot

IAM 是 Opaque Access Token 授权快照的唯一写入者和生命周期所有者。Gateway 与其他服务只读验证。

PostgreSQL 不维护 Access Token 授权快照的完整镜像。

Redis 授权快照彻底丢失时：

- 所有现有 Access Token 立即失效；
- 有效 Refresh Token 可重新生成 Access Token 和授权快照；
- Refresh Token 不可用时必须重新登录；
- Client Credentials 客户端重新申请 Access Token；
- 不从 PostgreSQL 自动重建全部正在使用的 Access Token；
- 不维护第二份完整 Access Token 快照。

### 8.2 Redis Persistence

V1 Docker Compose 至少提供：

- AOF；
- 持久化 Volume；
- healthcheck 和自动重启；
- `maxmemory-policy=noeviction`；
- 容量、备份和恢复说明。

这些措施用于降低丢失概率，但不改变“快照丢失后重新认证”的安全语义。

### 8.3 Cache

Config、Resource 等缓存由数据所属服务负责失效和重建。缓存不得成为业务数据唯一来源，不同服务必须使用独立命名空间。

## 9. Time and Time Zone

### 9.1 Core Rules

- 数据库真实时间统一使用 UTC 语义；
- Java 真实时间点使用 `Instant`；
- 业务日期使用 `LocalDate`；
- 业务时区使用显式 IANA `ZoneId`；
- 展示按查询用户或明确指定的 ZoneId 转换；
- 禁止使用服务器默认时区解释业务时间；
- 日期区间统一转换为 UTC Instant 半开区间 `[start, end)`。

### 9.2 PostgreSQL Mapping

| Meaning | Java | PostgreSQL |
| --- | --- | --- |
| 真实时间点 | `Instant` | `timestamptz` |
| 业务日期 | `LocalDate` | `date` |
| 业务时区 | `ZoneId` | `varchar`，保存 IANA Zone ID |

数据库连接、迁移和测试环境必须显式使用 UTC，不依赖操作系统或容器默认时区。

### 9.3 Date Query

按业务日期查询时，必须先确定日期和解释该日期的 ZoneId，再转换为：

```text
[startOfDay(zone), startOfNextDay(zone))
  -> UTC Instant range
```

禁止使用 `23:59:59.999` 表达日终。

### 9.4 LocalDateTime

`LocalDateTime` 不得用于 Token、审计、事件、日志、创建时间或更新时间等全局真实时间点。

只有明确表示“无时区本地墙上时间”的业务概念才允许使用，并必须说明语义。

## 10. Audit and Event Time

审计和事件至少区分：

- `occurredAt`：事件发生时间；
- `recordedAt`：Audit 持久化时间；
- 必要时增加 `receivedAt`。

均使用 UTC Instant。重试和补偿不得覆盖原始 `occurredAt`。

## 11. Query and Indexing

- 列表查询必须有稳定排序；
- 大数据量查询必须分页或使用游标；
- 唯一业务约束由数据库唯一索引保护；
- 外键只用于同一服务、同一 Schema 内的强一致关系；
- 索引依据实际查询和执行计划验证；
- `permission_code`、`event_id` 等唯一标识必须有唯一约束。

## 12. Sensitive Data

- 密码和 Client Secret 只保存安全哈希；
- Refresh Token 只保存安全摘要；
- 原始 Access Token 不持久化；
- 私钥、GatewayProof Secret 和基础设施密码不存入业务表；
- 日志和审计 payload 不记录完整 Token 或 Secret。

个人信息字段、加密和保留期限由模块设计和后续合规要求确认。

## 13. Backup and Recovery

V1 提供 PostgreSQL 备份恢复和 Redis AOF/Volume 恢复说明。

必须验证：

- PostgreSQL 恢复后 Flyway 状态正确；
- Redis 快照丢失后现有 Access Token 失效；
- Refresh Token 或重新登录可以恢复新快照；
- Outbox 未完成事件不会在恢复中静默丢失。

V1 不承诺完整高可用和跨地域灾备。

## 14. Pending Module Decisions

以下内容留待模块设计确认：

- 主键生成策略和字段类型；
- 表、列和索引命名规范；
- 逻辑删除和乐观锁适用范围；
- 数据保留、归档和分区策略；
- 个人信息加密与脱敏字段清单；
- 其他数据库进入 Platform 支持矩阵的验收标准和优先级。
