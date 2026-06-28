# Synapse Platform V1 范围

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 产品负责人、架构师、开发、测试、实施、运维和交付人员 |
| Purpose | 明确 V1 的交付目标、优先级、范围边界和完成标准 |
| Scope | Synapse Platform 第一个可运行开源版本 |
| Status | Accepted |

本文档是 Synapse Platform V1 范围的单一事实来源。

## 1. Product Goal

V1 交付一个可运行、可测试、文档完整、能够通过 Docker Compose 安装和启动的开源版本。

平台先完成自身闭环。外部 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 和第三方业务系统接入，不作为 V1 完成条件。

## 2. Priority Model

| Priority | Meaning |
| --- | --- |
| `P0` | 核心闭环和发布阻断项 |
| `P1` | V1.0 发布前完成最小范围的低优先级能力 |
| `P2` | 仅提供基础能力，不作为独立产品交付 |
| `Out of Scope` | 明确不进入 V1 |

## 3. P0 Core Scope

### Gateway

- 统一入口、路由、Header 清理和 traceId 传播；
- 验证 Opaque Access Token 的 Redis 授权快照；
- 原样透传 Bearer Token并签发 GatewayProof；
- 不注入用户、角色或权限 Header；
- 不执行业务权限，不保留 `gateway:admin` 例外。

### IAM

- 用户、组织、角色和授权关系；
- 登录、刷新、退出和当前用户；
- OAuth 2.0 / OpenID Connect；
- Opaque Access Token、Opaque Refresh Token 和 Client；
- Client Credentials；
- Redis 授权快照创建、更新和撤销；
- Refresh Token rotation 与重放检测；
- 登录失败控制和安全审计。

### Resource

- 应用、菜单、页面、按钮、API 和权限码目录；
- 按应用和节点懒加载资源树；
- `catalogVersion`；
- 权限码批量校验；
- 根据已验证授权快照生成用户导航。

### Authorization

- Resource 定义资源和权限；
- IAM 保存授权关系并生成 Redis 快照；
- 两者通过稳定 `permission_code` 关联；
- Gateway 和每个服务独立验证快照；
- 每个服务执行自身权限；
- 运行时不为每个请求同步调用 IAM 或 Resource。

### Audit

- 操作日志、安全日志和审计日志；
- 关键资源、授权和访问事件；
- 本地 Outbox + RocketMQ 可靠传输；
- `eventId` 幂等消费；
- 管理端查询能力。

### Management Console

- 用户、角色、资源和授权管理；
- 当前用户导航；
- 日志查询和基础运行状态；
- Access Token 仅保存在内存；
- Refresh Token 保存在 `sessionStorage`；
- 不使用 Cookie 或 `localStorage` 保存平台登录凭据。

### Docker Compose

- 正式支持安装、启动、停止、重启和运行验证；
- 提供环境变量、初始化、迁移、持久化和故障排查说明；
- 默认编排 PostgreSQL 17、Redis、Nacos 和 RocketMQ；
- PostgreSQL 17 是 V1 推荐并实际验证的数据库；
- Redis 需要持久化、健康检查和禁止淘汰授权快照；
- RocketMQ 需要支持 Outbox 积压恢复。

Framework 的其他数据库兼容能力不等于 Platform 已正式验证支持。其他数据库不作为 V1 发布验收目标。

## 4. P1 Scope

### File

形成文件上传、下载、元数据和存储适配的最小闭环。

### Message

形成消息、模板、发送记录和最小发送链路。

### Task

形成任务定义、调度、执行记录和基本失败处理闭环。

### Config

`synapse-config-platform` 是 V1 P1 独立微服务。

V1 最小闭环只确认：

- 国际化资源；
- 字典类型；
- 字典项。

Config 不替代 Nacos。Config 变更需要进入平台审计体系。

## 5. P2 Foundation

V1 不建设独立 Monitor 微服务，只提供：

- Spring Boot Actuator；
- 服务健康检查；
- Docker Compose healthcheck；
- 管理端基础状态展示。

## 6. Required Infrastructure

V1 默认基础设施：

- PostgreSQL 17；
- Redis；
- Nacos；
- RocketMQ。

Redis 是授权快照的 P0 安全依赖。RocketMQ 是 Outbox 和 Audit 可靠闭环的默认基础设施。

## 7. Data and Time Baseline

- 每个服务使用独立 Schema、数据库账号和 Flyway 历史；
- 禁止跨 Schema SQL 和跨服务数据库外键；
- PostgreSQL 不保存 Access Token 授权快照完整镜像；
- Redis 快照彻底丢失后现有 Access Token 失效；
- 有效 Refresh Token 或重新登录可生成新快照；
- 真实时间点使用 UTC 语义和 Java `Instant`；
- 业务日期使用 `LocalDate`；
- 业务时区使用显式 IANA `ZoneId`；
- 日期查询转换为 UTC 半开区间 `[start, end)`；
- 禁止依赖服务器默认时区解释业务时间。

## 8. Out of Scope

- 外部业务系统真实接入验证；
- Integration、Workflow、MDM 和 Report；
- 低代码、完整 BPM 和完整 BI；
- AI Agent 平台；
- 复杂多租户、多园区和多 Region；
- 完整高可用和灾备；
- Service Mesh；
- Kubernetes 正式交付；
- Token Exchange；
- 独立 Monitor 产品；
- 未经过 Platform 级验证的其他数据库正式支持；
- 商业版和企业版划分。

## 9. Self-contained Acceptance Flow

```text
Docker Compose 启动平台与基础设施
  -> 初始化管理员
  -> Resource 注册资源和权限码
  -> IAM 批量校验并授予权限码
  -> 用户登录并获得 Opaque Token
  -> Redis 保存授权快照
  -> Resource 返回过滤后的导航
  -> Gateway 和目标服务分别验证快照
  -> 有权限请求成功，无权限请求被拒绝
  -> 刷新、退出和权限变化更新或撤销快照
  -> 关键事件通过 Outbox / RocketMQ 到达 Audit
  -> Config 完成国际化和字典最小闭环
  -> 查看日志和基础运行状态
```

## 10. Definition of Done

### Functionality

- P0 全部可用；
- P1 完成确认后的最小范围；
- 平台自身闭环完整跑通；
- 不依赖外部业务系统运行。

### Testing

- 核心单元和集成测试通过；
- PostgreSQL 17 空库初始化和版本升级测试通过；
- Opaque Token、Redis 快照、RBAC 和访问拒绝测试通过；
- Redis 快照丢失、Refresh Token 恢复和重新登录测试通过；
- Refresh Token rotation 和重放检测测试通过；
- Resource 批量校验和版本冲突测试通过；
- Outbox 原子性、重复投递和 Audit 幂等测试通过；
- UTC、ZoneId、日期半开区间和夏令时边界测试通过；
- Redis 故障 30 秒只读降级和恢复测试通过；
- RocketMQ/Audit 积压和恢复测试通过；
- Docker Compose 安装验证通过。

### Documentation and Operation

- 产品、架构、规范、设计、测试、部署和故障排查文档完整；
- 新环境可依据文档安装；
- 初始化流程可重复执行或有明确保护；
- PostgreSQL、Redis 和 RocketMQ 故障有恢复依据；
- 平台能够完成基础健康验证。

## 11. Scope Change Rule

影响 P0、P1、完成标准或排除项的变更，必须更新本文档，并在必要时新增 ADR。
