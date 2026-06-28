# Synapse Platform AI 协作规范

本文档约束 `synapse-platform` 整个仓库。修改模块前必须同时阅读：

1. 本文件。
2. 目标一级模块目录中的 `AGENTS.md`。
3. 模块规则引用的专题文档。

规则优先级：当前用户任务要求 > 距离目标文件最近的 `AGENTS.md` > 上级 `AGENTS.md` > 通用文档。冲突不得静默处理，最终报告必须说明。

## 0. 事实优先与执行底线

- 所有分析、设计、实现和报告必须以当前工作分支的真实源码、配置、POM、migration、测试和已接受文档为依据。
- 禁止虚构不存在的类、接口、Bean、模块、配置项、数据库表字段、接口路径、测试基础设施或执行结果。
- 禁止把目标架构、历史讨论、旧文档或提示词描述成当前已实现事实。
- 禁止使用空实现、Noop、Mock、固定返回值、TODO 或仅声明依赖冒充功能完成。
- 不确定时先搜索仓库和必要的相邻 Framework 源码，不得凭经验推断。
- 最终报告必须区分：`已确认事实`、`设计决策`、`尚未确认`、`未完成`。

遇到以下情况必须停止对应部分并向用户确认唯一决策点：文档与源码直接冲突、Platform 与 Framework 边界不清、找不到任务指定对象、需要修改 Framework、需要新增 Maven 模块或核心生产依赖、需要改变公开 API/错误语义/安全语义、需要修改已发布 migration、需要作出影响后续模块的架构选择、测试无法执行或结果异常且原因不明。

## 0.1 Framework 复用检查

Platform 默认只修改本仓库。未经明确授权，禁止修改 `../synapse-framework`、新增 Framework 模块、改变 Framework 公共 API 或把 Platform 业务语义反向写入 Framework。

实现 Web、安全、OAuth2、缓存、Redis、GatewayProof、上下文、异常、数据访问、审计、消息或时间相关功能前，必须先检查 Framework 是否已有可复用能力，并记录：

```text
功能：
Platform 目标模块：
Framework 检查模块：
找到的真实能力：
证据文件：
可直接复用部分：
需要 Platform 自行实现部分：
是否存在边界冲突：
```

检查至少覆盖对应模块、手册或设计文档、自动配置类、配置属性、公共接口、默认实现、测试、自动配置导入文件和 Platform 实际依赖。Framework 有基础能力时优先复用；Framework 只有底层能力时，Platform 可以在自身职责内完成业务适配，但必须说明边界。

## 0.2 编码前输出要求

涉及代码、配置、数据库、测试或架构文档修改前，必须先输出：

1. 本次任务边界：目标、预计修改范围、明确不修改内容、验证命令。
2. 编码前自查结果：已阅读的根/模块 `AGENTS.md`、任务相关设计/接口/数据库/测试文档。
3. 当前事实矩阵：能力、当前真实实现、Framework 支持、Platform 缺口、本次实现方式、证据。
4. Framework 复用清单：Platform 功能、Framework 模块、复用类型/Bean、是否直接可用、限制。
5. 预计修改清单：新增、修改、不修改、migration、配置、测试文件；尚未定位的路径标记为“待源码定位”。
6. 冲突清单：只列真实冲突，不把“没有现成完整实现”直接视为阻断。

## 1. 项目定位

Synapse Platform 是基于 Java 21、Spring Boot、Spring Cloud 和 Spring Cloud Alibaba 的企业级微服务平台。

Platform 只能通过 Maven 依赖复用相邻 `../synapse-framework`，禁止修改 Framework 或让 Framework 反向依赖 Platform。Framework 能力以其当前根 POM、`synapse-bom`、源码和模块手册为准。

## 2. 一级模块

仓库包含 Gateway、IAM、Resource、Config、Audit、File、Message、Task、Workflow、Integration、MDM、Report、Monitor 共 13 个一级模块。

模块存在不代表进入 V1。V1 范围以 [`docs/v1/00-product/v1-scope.md`](docs/v1/00-product/v1-scope.md) 为准。

Gateway 不拆分。其余模块采用：

```text
synapse-xxx-platform
├── synapse-xxx-api
├── synapse-xxx-client
└── synapse-xxx-server
```

## 3. 通用分层边界

- `api`：稳定跨服务契约，不依赖 client/server，不包含数据库实现。
- `client`：调用适配，允许依赖对应 api，禁止依赖 server 或直接访问服务数据库。
- `server`：启动入口与模块实现，允许依赖对应 api，禁止依赖自己的 client 或其他模块 server。
- 跨服务调用通过 api/client 或消息契约完成，禁止共享业务 Entity、Mapper、Repository。

## 4. 依赖与基线

- Java 21，Maven 3.9.x。
- 根工程 import `com.indigo.synapse:synapse-bom`；官方组件 BOM 可由 Platform 直接 import。
- 禁止循环依赖、跨服务数据库访问和 Framework -> Platform 依赖。
- 不得引用已从当前 Framework 删除或更名的 artifact。
- 新增生产依赖前必须说明必要性、替代方案和影响范围。

## 5. 配置与凭据

- Spring Boot 3 使用 ConfigData，不新增 `bootstrap.yml`。
- 配置支持环境变量或外部配置中心注入，应用保持无状态。
- 禁止提交密码、Token、Secret、私钥、真实 `.env` 或个人绝对路径。
- 禁止在日志、异常或诊断命令中输出认证材料和完整环境变量。

## 6. 启动类约定

- Gateway 和各领域 server 的启动类必须位于对应领域 Java 根包。
- 禁止将生产启动类放入 `bootstrap`、`boot`、`launcher` 或 `startup` 子包。
- `api` 和 `client` 模块禁止存在生产启动类。

## 7. 数据与时间基线

- PostgreSQL 17 是 V1 默认、推荐和实际验证数据库。
- Framework 的其他数据库兼容不等于 Platform 已正式支持。
- 每个服务使用独立 Schema、账号和 Flyway 历史。
- 禁止跨 Schema SQL、跨服务数据库外键和共享业务持久化类型。
- 持久化 Entity 优先使用 Framework `synapse-mybatis-plus` 已提供的 `IdEntity`、`CreatedEntity`、`MutableEntity`、`VersionedEntity`、`ManagedEntity`。
- Entity 按实际生命周期选择满足需求的最浅基类，不统一强制 `ManagedEntity`。
- Domain Model、DTO、Command、Query 和 Event 禁止继承 MyBatis-Plus 实体基类。
- 默认主键为 Java `String`、PostgreSQL `varchar(19)`、MyBatis-Plus `ASSIGN_ID`。
- 技术乐观锁字段使用 `revision`；逻辑删除字段使用 `deleted`。
- 独立 Schema 内的新表不重复服务名称前缀；既有表更名必须通过 Flyway migration，禁止只改 `@TableName`。
- 真实时间点使用 UTC 语义与 Java `Instant`。
- 业务日期使用 `LocalDate`，业务时区使用显式 IANA `ZoneId`。
- 禁止依赖服务器默认时区解释业务时间。
- 日期区间转换为 UTC 半开区间 `[start, end)`。

详细规则见 [`docs/v1/02-specification/database-conventions.md`](docs/v1/02-specification/database-conventions.md)。

## 8. 修改原则

- 修改前先搜索并阅读相关规则、设计、接口和测试文档。
- 优先最小修改和现有结构，不进行未授权的大规模重构。
- 不删除测试、降低断言、绕过校验或把临时实现伪装成最终能力。
- 当前事实优先于旧 README、历史任务和规划描述。
- 代码说明、计划、总结和自查使用中文；命名遵循项目既有习惯。

## 9. 注释、文档与测试说明

- 新增或修改的核心代码必须提供准确、可维护的中文注释和 Javadoc；项目已有英文注释约定的局部可保持一致。
- public/protected API、配置属性、自动配置、Controller、Application Service、Domain Service、Repository/Adapter、Token/会话/权限/Gateway Filter/Security 组件、异常类型、DTO/Command/Query/Response 等必须说明职责、输入输出、关键安全语义、事务或外部存储行为、失败语义和不承担的职责。
- 复杂逻辑必须用行内注释解释“为什么这样做”，例如 Token rotation、reuse detection、并发控制、乐观锁、Redis 快照、GatewayProof、Header 清理、Reactive 调用链、401/403/503 映射、数据库与 Redis 一致性处理。
- 禁止无意义注释、过期注释、与代码不一致的注释，以及用 TODO/FIXME 或“临时、以后处理”等模糊表述替代必须完成的能力。
- 代码、配置、migration、测试和文档必须自洽。新增或修改 API、配置、数据库、Token、Redis Key、错误码、职责边界或 Framework 复用方式时，必须同步检查受影响文档。
- 文档必须区分已实现、部分实现、计划实现、明确未实现和不在本次范围；禁止把单元测试、Mock 验证、本地启动或代码存在夸大为集成测试、真实环境验证或功能闭环完成。
- 影响跨服务闭环、部署验证或复杂验收流程的任务，应新增或更新独立测试说明；路径和命名以仓库现有文档结构为准。

## 10. 最低验证

```bash
mvn validate
mvn clean test
git diff --check
```

模块规则可增加更严格验证。未执行的命令必须如实标记并说明原因。

## 11. 完成报告

必须列出修改、新增、删除文件，核心实现，验证命令和结果，未完成事项、技术债与风险点；涉及事实判断时必须按 `已确认事实`、`设计决策`、`尚未确认`、`未完成` 分类说明。未获用户明确授权不得自动提交。

## 12. 模块和专题索引

- [V1 文档首页](docs/v1/README.md)
- [V1 范围](docs/v1/00-product/v1-scope.md)
- [总体架构](docs/v1/01-architecture/overall-architecture.md)
- [服务边界](docs/v1/01-architecture/service-boundary.md)
- [安全架构](docs/v1/01-architecture/security-architecture.md)
- [通信架构](docs/v1/01-architecture/communication-architecture.md)
- [数据架构](docs/v1/01-architecture/data-architecture.md)
- [数据库与持久化规范](docs/v1/02-specification/database-conventions.md)
- [Gateway 模块规则](synapse-gateway-platform/AGENTS.md)
- [Gateway Docker 部署](deploy/docker/gateway/README.md)
- 其余模块规则位于对应一级模块目录的 `AGENTS.md`。
