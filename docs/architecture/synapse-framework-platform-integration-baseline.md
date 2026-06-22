# Synapse Framework → Synapse Platform 当前接入条件与决策基线

> 用途：将本文作为 Synapse Platform 架构讨论、Codex 仓库评审和接入实施的前置条件输入。  
> 文档日期：2026-06-21  
> Framework 仓库：`Chris-co-shi/synapse-framework`  
> 当前基线：`main` 已包含 Messaging 原子幂等与 Audit 事务语义重构。  
> 当前版本：`0.1.0-SNAPSHOT`

---

## 1. 结论

Synapse Framework 已经达到 Synapse Platform 的开发接入基线。

成熟度判断：

- Platform 开发准备度：8/10，可以立即开始。
- Platform 生产发布准备度：6.5/10，暂不应标记为生产就绪。
- Platform 不需要等待 Framework 所有后续重构全部完成。
- Framework 和 Platform 应双轨推进：
  - Framework 继续完成 Cache、Result/i18n、兼容 API 清理和真实基础设施验证。
  - Platform 同时建设 Gateway、IAM、基础业务服务及生产级 Adapter。

Platform 应将 Framework 视为“技术能力与扩展端口提供者”，而不是业务平台服务实现。

---

## 2. Framework 与 Platform 的边界

### 2.1 Framework 负责

Framework 负责提供：

- 通用上下文模型。
- WebMVC/WebFlux 技术接入。
- 统一异常与响应基础契约。
- Security 与 OAuth2 Resource Server 基础能力。
- JWT、Token、Client Credentials 等协议级能力。
- Data、MyBatis-Plus、Datasource 基础设施。
- Cache 抽象和通用能力。
- Broker 中立的 Messaging 模型与消费编排。
- 原子消费幂等 SPI。
- Audit Event、AOP、脱敏及事务语义。
- Observability 与 Resilience 通用能力。
- Spring Boot AutoConfiguration。
- 模块边界、架构门禁和配置元数据。

### 2.2 Platform 负责

Platform 负责实现：

- 可启动的 Gateway 服务。
- IAM 用户、角色、组织、菜单、客户端和授权管理。
- Gateway 路由、限流、黑白名单和策略管理。
- Redis、数据库和 Broker 的生产 Adapter。
- Outbox 表、Migration、扫描投递、集群抢占、重试和死信。
- 审计存储、查询、报表和归档。
- 配置中心的数据模型、发布、审批和刷新。
- 国际化资源中心的数据模型、版本、发布和缓存刷新。
- 文件中心、预览、OCR 和存储策略。
- 消息模板、短信、邮件和站内信。
- 服务级 Flyway Migration。
- 生产配置、监控后端、告警和运维能力。

### 2.3 禁止反向放入 Framework 的能力

不得把以下能力重新塞入 Framework：

- IAM 用户、角色、菜单、组织领域模型。
- Platform Gateway 可启动服务。
- Gateway 路由管理。
- 配置发布和审批。
- 文件记录、预览、OCR。
- 审计查询、报表和存储中心。
- 消息模板、短信、邮件和站内信。
- Platform 业务表和业务 Migration。
- 具体 Broker、数据库、Redis 产品级部署逻辑。

---

## 3. Platform 工程约束

### 3.1 Maven 结构

Platform 不创建独立的 `synapse-platform-bom` 模块。

版本统一放在 Platform 根工程 `pom.xml`：

```text
synapse-platform/
├── pom.xml
├── synapse-gateway/
├── synapse-iam-server/
├── synapse-message-server/
├── synapse-file-server/
├── synapse-task-server/
└── ...
```

根 POM 应：

- 作为 Parent。
- 在 `dependencyManagement` 中导入 Framework BOM。
- 管理 Platform 自己的第三方依赖版本。
- 管理所有 Platform 子模块。
- 固定 Framework 版本，不允许各服务自行声明 Framework 版本。

当前 Framework 仍为 `0.1.0-SNAPSHOT`。开发阶段可以使用 SNAPSHOT，但必须固定到明确构建产物或提交基线，避免不可重复构建。

### 3.2 服务端口

当前冻结端口：

- Gateway：8080
- IAM：8100
- Message：8200
- File：8300
- Task：8400

同类服务按百位段扩展，例如 IAM 使用 81xx，Message 使用 82xx。

### 3.3 服务依赖原则

每个服务只引入实际需要的 Framework 模块。

禁止：

- 创建包含全部 Framework 依赖的万能 starter。
- 所有服务无差别引入 Security、Data、Messaging、Audit、Cache。
- Platform 自己复制 Framework 的 Result、Trace、Context 或异常模型。
- 在业务模块中直接依赖 Framework 内部实现类。
- 绕过 Framework BOM 手工覆盖内部模块版本。

---

## 4. 当前能力接入判断

| Platform 能力 | 当前 Framework 支撑 | 接入判断 |
|---|---|---|
| Gateway | WebFlux、异常、Trace、Resource Server、Gateway Proof 扩展点 | 可立即开发；路由和网关策略由 Platform 实现 |
| IAM | Security、OAuth2 Core、JWT、Resource Server | 可立即开发；用户、角色、客户端、登录流程由 Platform 实现 |
| MVC 业务服务 | WebMVC、Data、Datasource、Cache、Security | 可立即开发 |
| 配置中心 | Config Client/Resolver/Parser 抽象 | 可设计和开发服务端；避免锁死当前客户端抽象 |
| 国际化资源中心 | 当前存在 Loader/Resolver 类能力 | 可建设服务端模型；Framework Web 端最终接入待 Result/i18n 批次稳定 |
| 消息基础设施 | Envelope、Dispatcher、Stream Adapter、原子幂等 SPI | 可立即开发；生产 Store、Binder、Outbox 由 Platform 实现 |
| 审计中心 | Audit Event、AOP、脱敏、事务策略、Messaging 发布 | 采集侧可用；存储、检索、报表由 Platform 实现 |
| 文件中心 | Framework 无文件业务模块 | Platform 直接建设 |
| 可观测性 | Micrometer 基础能力 | 可接入；监控后端和统一标签策略由 Platform 配置 |
| 韧性 | Resilience4j 基础能力 | 可接入；策略参数和场景边界由 Platform 决定 |
| 报表平台 | Framework 不提供业务报表服务 | 由 Platform 建设 `synapse-report-service` |

---

## 5. 安全与上下文接入规则

### 5.1 身份信任边界

普通 HTTP Header 不得建立以下身份：

- actor
- tenant
- initiator
- client identity
- roles
- permissions

Gateway 转发 Bearer Token 后，下游服务必须作为独立 Resource Server 验证 Token。

标准链路：

```text
Gateway
→ 转发 Bearer Token
→ 下游 Resource Server 独立验签
→ CurrentPrincipalContext
→ 可信 OperationContext
```

禁止通过以下普通 Header 直接恢复认证身份：

```text
X-User-Id
X-Tenant-Id
X-Initiator-Id
X-Roles
X-Permissions
```

这些 Header 即使由 Gateway 写入，也不能天然视为可信身份载体。

### 5.2 initiator 规则

当前没有可信签名载体时：

```text
initiator = actor
```

Platform 后续负责可信 initiator 协议：

- Gateway 签名。
- key rotation。
- audience。
- timestamp。
- nonce。
- Redis replay protection。
- 服务间继续传播。
- actor 不允许被覆盖。
- tenant 不一致必须拒绝。

Framework 可以提供验证 SPI，但密钥、签发和重放保护由 Platform 负责。

### 5.3 技术上下文与身份上下文

必须区分：

- 身份上下文：来自已验证的 `OperationContext`。
- 技术上下文：traceId、requestId、method、path、clientIp 等。

向消息、审计和响应输出上下文时，必须通过显式 Enricher/Factory 合成。

不得通过旧 Header Snapshot 或普通 Header 恢复身份。

---

## 6. Messaging 接入规则

Framework 当前已采用原子幂等模型：

```text
claim
→ handle
→ complete / release
```

幂等键必须包含：

```text
consumerId
+ handlerId
+ messageType
+ eventId 或 messageId
```

状态语义：

- `ACQUIRED`：当前消费者取得处理权。
- `PROCESSING`：其他消费者正在处理，应返回重试。
- `COMPLETED`：已经完成，应视为重复消息。
- 失败或非成功结果需要 release。
- complete/release 必须校验 claimId 所有权。
- claim 必须有租约，节点故障后允许接管。

Framework 不承诺 Exactly-once。

Platform 必须继续保证：

- 业务 Handler 自身幂等。
- 或将消费记录和业务修改放入同一本地事务。
- 多实例下的 Store 使用共享数据库或 Redis。
- handlerId 使用稳定业务标识，不能使用 Java 类名。

### 6.1 Platform 必须实现

至少需要：

- `MessageIdempotencyStore`
- `OutboxStore`
- 数据库 Outbox 表
- Flyway Migration
- 多实例 claim/lease
- Outbox 扫描器
- 集群抢占
- 重试策略
- 失败状态
- 死信或人工处置
- 运维查询
- Spring Cloud Stream Binder 或其他 Broker Adapter

---

## 7. Audit 接入规则

当前 Framework 已完成成功审计与失败审计事务语义拆分。

### 7.1 成功审计

支持：

- `BEST_EFFORT`
- `TRANSACTIONAL_OUTBOX`

`TRANSACTIONAL_OUTBOX` 语义：

```text
业务数据修改
+ Audit Outbox append
处于同一本地事务
```

事务提交时同时提交，事务回滚时同时回滚。

### 7.2 失败审计

支持：

- `NONE`
- `BEST_EFFORT_AFTER_ROLLBACK`
- `REQUIRES_NEW_AFTER_ROLLBACK`
- `EXTERNAL_SINK`

失败审计仅在业务事务回滚完成后执行。

失败审计异常：

- 不得覆盖原始业务异常。
- 只能作为 suppressed error 或日志记录。

### 7.3 Advisor 顺序

Framework 已固定：

```text
Security(-200)
→ Transaction(0)
→ Audit(200)
→ business method
```

Platform 不需要手工配置：

```java
@EnableTransactionManagement(order = 0)
```

可靠 Audit 条件不满足时应启动快速失败，不能静默退化为事务外可靠发布。

### 7.4 Platform 必须实现

- 本地 `OutboxStore`
- 失败审计外部 Sink（采用 `EXTERNAL_SINK` 时）
- 审计事件持久化
- 查询 API
- 权限控制
- 报表与导出
- 保留、归档和清理策略

---

## 8. 时间与时区规则

所有 Platform 服务必须遵循：

- 数据库真实时间统一存 UTC。
- Java 真实时间点统一使用 `Instant`。
- 业务日期单独使用 `LocalDate`。
- 业务发生地时区单独存 IANA `ZoneId`，例如 `business_zone_id`。
- 展示时间按查询人的 `ZoneId` 转换。
- 日期查询必须先明确 `TimeQueryScope`。
- 日期条件转换为 UTC `Instant` 半开区间查询。
- 禁止使用服务器默认时区解释业务时间。
- 禁止使用 `LocalDateTime` 表示跨时区真实时间点。

---

## 9. 当前必须由 Platform 提供的共享实现

多实例部署前至少需要共享实现：

- `GatewayProofReplayStore`
- `TokenDenylistPort`
- `AuthorizedClientTokenStore`
- `MessageIdempotencyStore`
- `OutboxStore`

建议技术实现：

- Replay、Denylist、Authorized Client：Redis。
- Message Idempotency：Redis 或业务数据库，按事务一致性要求选择。
- Outbox：各服务本地 PostgreSQL 表，不建立中央 Outbox 数据库。
- Outbox Dispatcher：每个服务本地运行，支持集群抢占。
- Audit External Sink：由 Audit Platform 服务或专门 Adapter 提供。

这些实现属于 Platform Adapter，不应进入 Framework 通用模块。

---

## 10. 暂缓深度绑定的 Framework 能力

以下能力仍会继续重构，Platform 可以使用，但必须通过本地 Port 或封装隔离：

### 10.1 Cache

后续仍需完成：

- ObjectMapper 修复。
- 静态锁移除。
- `SingleFlightCoordinator`。
- 集群级 Redis 验证。

Platform 不应直接依赖 Cache 内部锁实现。

### 10.2 Result 与国际化

后续目标：

- Result 退化为被动数据模型。
- 由 `ResultFactory` 统一组装 trace、time 和 message。
- 国际化进入 WebMVC/WebFlux 最终展示边界。
- 使用 Spring `MessageSource`。
- 独立 `synapse-i18n` 模块最终移除。
- Platform 国际化资源中心未来提供远程缓存型 `MessageSource`。

当前 Platform 不应大规模绑定旧 I18n Loader/Resolver API。

### 10.3 兼容 API

0.1.0 发布前仍会清理：

- Deprecated API。
- 重复 UUID/Trace 生成器。
- 旧 Datasource Router/Failover 接口。
- 当前版本尚未发布却保留的兼容层。

Platform 新代码禁止主动使用 `@Deprecated` API。

---

## 11. 推荐的第一阶段 Platform 接入

第一阶段建议只建设：

1. Platform 根 Parent POM 和 dependencyManagement。
2. `synapse-gateway`。
3. `synapse-iam-server`。
4. 一个 MVC 验证服务。
5. Redis 共享 Store Adapter。
6. PostgreSQL Outbox Adapter。
7. 一个真实 Broker Binder。
8. 一条端到端链路。
9. 服务工程规范和测试基线。

暂不优先建设：

- 完整配置中心。
- 完整消息中心。
- 完整审计查询中心。
- 文件中心复杂能力。
- 报表平台复杂设计。
- 低代码和工作流。

先通过真实链路验证 Framework 与 Platform 的组合方式。

---

## 12. 必须建立的端到端场景

### 场景一：认证与上下文

```text
Gateway
→ Bearer Token 转发
→ IAM/JWT 公钥验证
→ 下游 Resource Server 独立验签
→ CurrentPrincipalContext
→ OperationContext
→ Data 审计字段
```

### 场景二：成功事务审计

```text
业务数据写入
→ Audit Event
→ Transactional Outbox
→ 同事务提交
→ Dispatcher 投递 Broker
```

### 场景三：业务回滚

```text
业务异常
→ 业务数据回滚
→ 成功 Audit Outbox 回滚
→ AFTER_ROLLBACK 失败审计执行
```

### 场景四：消息重复

```text
同一 eventId 重复到达
→ 第一次 claim 成功
→ 后续 delivery 返回 PROCESSING 或 COMPLETED
→ Handler 不产生重复业务副作用
```

### 场景五：Broker 故障恢复

```text
Broker 不可用
→ Outbox 保留
→ 重试
→ 多实例竞争
→ 单实例成功投递
→ 状态完成
```

---

## 13. Codex 对 Platform 仓库的评审任务

Codex 应先读取：

1. 本文档。
2. 当前 `synapse-platform` 根 POM。
3. 所有 Platform 子模块 POM。
4. 当前 Framework BOM 和模块清单。
5. Platform 的 AGENTS.md、架构文档和现有代码。
6. 当前配置文件、AutoConfiguration 接入和测试。

本轮只做“现状评审和接入方案”，不要直接大规模改造代码。

### 13.1 必须回答

1. Platform 当前有哪些服务和公共模块。
2. 当前是否错误创建了独立 Platform BOM。
3. Framework BOM 是否已正确导入。
4. 各服务实际引入了哪些 Framework 模块。
5. 是否存在万能 starter 或全量依赖。
6. Gateway 当前是否遵守 WebFlux 与身份信任边界。
7. IAM 当前是否正确使用 OAuth2/JWT/Resource Server 能力。
8. 是否仍通过普通 Header 恢复用户、租户或 initiator。
9. 当前是否已有 Redis 共享 Store。
10. 当前是否已有 PostgreSQL Outbox Adapter。
11. 是否已有 Broker Binder 和真实端到端测试。
12. 当前 Audit 是否使用 Transactional Outbox。
13. 当前 Platform 使用了哪些待重构 API。
14. 哪些能力应立即接入，哪些应暂缓。
15. 给出按优先级排序的接入任务。

### 13.2 输出格式

仅输出：

1. 当前 Platform 工程事实。
2. Framework 接入覆盖矩阵。
3. 发现的边界违规。
4. 缺失的生产 Adapter。
5. 第一阶段接入方案。
6. 按服务拆分的依赖建议。
7. 端到端验证方案。
8. 风险与暂缓项。
9. 建议修改文件。
10. 建议提交拆分。

### 13.3 强制约束

- 不创建 `synapse-platform-bom`。
- 不在 Framework 创建 Gateway 服务。
- 不把 IAM、Gateway 路由、文件、配置发布、审计查询放入 Framework。
- 不信任普通身份 Header。
- 不使用服务器默认时区。
- 不使用 `LocalDateTime` 表示真实时间点。
- 不宣称 Exactly-once。
- 不绕过 Framework BOM。
- 不使用 Framework Deprecated API。
- 不在本轮直接实现所有缺失功能。
- 发现问题先给出证据、影响和建议，再决定是否修改。

---

## 14. 本轮 Codex 执行提示

请基于本文档和当前 `synapse-platform` 仓库，评审 Platform 现在应如何接入 Synapse Framework。

本轮目标不是重新设计 Framework，也不是立即完成全部 Platform 功能，而是：

- 确认当前工程事实。
- 识别错误依赖和边界违规。
- 确定第一阶段可落地的接入方案。
- 明确 Platform 必须实现的生产 Adapter。
- 给出按服务和提交拆分的实施计划。

不要根据模块名称猜测能力；必须以当前 POM、源码、AutoConfiguration、测试和文档为证据。

发现 Framework 与本文档不一致时，列出差异，不要自行修改 Framework。
