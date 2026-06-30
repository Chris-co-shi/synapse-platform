# ADR-005：数据库、Redis 快照与时间模型

## Status

Accepted

## Context

Synapse Platform V1 需要明确：

- Platform 实际推荐和验证的数据库；
- Framework 数据库兼容与 Platform 正式支持之间的边界；
- Redis 授权快照彻底丢失后的恢复行为；
- 跨时区真实时间点、业务日期和日期查询的统一语义。

## Decision

### Database

PostgreSQL 17 是 V1 的默认、推荐和实际验证数据库。

V1 的 Docker Compose、Flyway migration、集成测试和发布验收以 PostgreSQL 17 为目标。

Framework 已提供部分其他数据库兼容能力，但不能据此宣称 Platform 已正式支持其他数据库。其他数据库必须完成 Platform 级迁移、方言、测试和部署验证后，才能进入支持矩阵。

### Service Isolation

V1 可以共用一个 PostgreSQL 实例，但每个服务必须使用独立 Schema、数据库账号和 Flyway 历史。

禁止跨 Schema SQL、跨服务数据库外键以及共享 Entity、Mapper 和 Repository。

### Redis Authorization Snapshot

IAM 是 Opaque Access Token 授权快照的唯一写入者。Gateway 和其他受保护服务只读验证。

PostgreSQL 不维护 Access Token 快照的完整镜像。

Redis 授权快照彻底丢失后：

- 所有现有 Access Token 立即失效；
- 有效 Refresh Token 可以生成新的 Access Token 和授权快照；
- Refresh Token 不可用时重新登录；
- 后续标准 Client Credentials 客户端重新申请 Token；
- 不从 PostgreSQL 自动重建全部活动 Access Token。

### Time and Time Zone

- 真实时间点使用 UTC 语义；
- Java 使用 `Instant`；
- PostgreSQL 推荐使用 `timestamptz`；
- 业务日期使用 `LocalDate` / `date`；
- 业务时区使用 IANA `ZoneId` 并显式保存；
- 展示按明确 ZoneId 转换；
- 日期查询转换为 UTC Instant 半开区间 `[start, end)`；
- 禁止依赖服务器默认时区；
- `LocalDateTime` 不用于跨时区真实时间点。

## Alternatives

### PostgreSQL 17 作为唯一支持数据库

边界最清晰，但会忽略 Framework 已经存在的初步兼容基础，也会过早排除未来验证其他数据库的可能，因此不采用“唯一支持”表述。

### 宣称 Framework 兼容的数据库都被 Platform 支持

缺少 Platform migration、部署和集成测试依据，属于未经验证的承诺，因此禁止采用。

### PostgreSQL 保存完整 Access Token 快照镜像

可以在 Redis 丢失后重建，但会产生双写、一致性和撤销语义复杂度，因此不采用。

### 使用服务器默认时区和 LocalDateTime

会在容器、部署地区和夏令时变化下产生歧义，不满足跨区域平台的长期演进要求，因此禁止采用。

## Consequences

正向影响：

- 数据库支持口径真实、可验证；
- Redis 恢复行为简单且安全；
- 不维护双份 Access Token 运行状态；
- 时间存储、查询和展示语义统一；
- 服务未来可以迁移到独立数据库实例。

代价：

- Redis 快照完全丢失会使现有 Access Token 全部失效；
- 其他数据库不能仅凭 Framework 兼容声明进入支持矩阵；
- 所有日期查询必须显式解析 ZoneId；
- 模块设计和测试必须覆盖时区转换及夏令时边界。

## References

- [`../01-architecture/data-architecture.md`](../01-architecture/data-architecture.md)
- [`../01-architecture/security-architecture.md`](../01-architecture/security-architecture.md)
- [`../01-architecture/overall-architecture.md`](../01-architecture/overall-architecture.md)
