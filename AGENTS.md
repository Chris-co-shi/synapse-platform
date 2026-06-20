# AGENTS.md

本文档约束 Codex / AI 编码助手在 `synapse-platform` 仓库中的行为。

## 1. 项目定位

`Synapse Platform` 是基于 Java 21、Spring Boot、Spring Cloud、Spring Cloud Alibaba 构建的企业级微服务平台。

`Synapse Framework` 是独立技术基座项目，Platform 只能通过 Maven 依赖使用 Framework，禁止修改或反向依赖 Framework。

当前阶段只创建 Java 后端微服务骨架，不实现具体业务功能。

## 2. 当前平台模块

一级平台模块共 13 个：

```text
synapse-gateway-platform
synapse-iam-platform
synapse-resource-platform
synapse-config-platform
synapse-audit-platform
synapse-file-platform
synapse-message-platform
synapse-task-platform
synapse-workflow-platform
synapse-integration-platform
synapse-mdm-platform
synapse-report-platform
synapse-monitor-platform
```

Gateway 不拆分 api / client / server。

除 Gateway 外，每个平台能力采用：

```text
synapse-xxx-platform
├── synapse-xxx-api
├── synapse-xxx-client
└── synapse-xxx-server
```

## 3. 职责边界

`*-api`：平台微服务内部调用契约。本次只允许 POM 和源码目录，不创建 DTO、事件、错误码或接口类型。

`*-client`：面向业务系统或第三方系统的 SDK。本次只允许 POM 和源码目录，不创建 Feign Client 或自动配置。

`*-server`：可独立启动的 Java 微服务。本次只允许启动类、配置文件、Flyway 目录占位，不创建 Controller、Entity、Mapper、Repository、Service、Migration 或业务代码。

## 4. 依赖规则

允许：

```text
client -> 对应 api
server -> 对应 api
platform service -> synapse-framework
```

禁止：

```text
api -> client
api -> server
client -> server
server -> 自己的 client
server -> 其他服务的 server
framework -> platform
跨服务共享 Entity / Mapper / Repository
跨服务直接访问数据库
循环依赖
```

根工程只 import `com.indigo.synapse:synapse-bom`。Spring Boot / Spring Cloud / Spring Cloud Alibaba 版本由 `synapse-bom` 管理。

## 5. Framework 复用规则

使用 Framework 能力前必须以本地 `../synapse-framework` 的根 POM、`synapse-bom`、模块手册和配置属性类为准。

只允许使用已进入 Framework 根 POM reactor 且由 `synapse-bom` 管理的模块。

禁止凭空创建 Framework 配置项。当前已核实可用配置项包括：

```text
synapse.cloud.*
synapse.time.default-zone
synapse.config.values
synapse.i18n.*
synapse.security.*
synapse.oauth2.*
synapse.file.local-root
```

`synapse-webmvc`、`synapse-webflux` 当前没有需要统一生成的复杂外部属性，禁止创建 `synapse.webmvc.*` 或 `synapse.webflux.*`。

## 6. Gateway 边界

`synapse-gateway-platform` 使用 Spring Cloud Gateway / WebFlux / Reactor。

Gateway 禁止依赖：

```text
synapse-webmvc
synapse-data
spring-boot-starter-web
spring-webmvc
Servlet API
任何服务的 Entity / Mapper / server 模块
```

Gateway 不访问数据库，不承载 IAM 业务逻辑。

## 7. 包名与服务名

Server 根包：

```text
com.indigo.synapse.{module}
```

启动类包：

```text
com.indigo.synapse.{module}.bootstrap
```

服务名必须与可启动 artifactId 一致，例如：

```text
synapse-iam-server
synapse-resource-server
synapse-gateway-platform
```

## 8. 配置规则

每个可启动服务必须包含：

```text
application.yml
application-dev.yml
application-beta.yml
application-prd.yml
```

Spring Boot 3 / Spring Cloud 配置加载使用 ConfigData 机制：

```text
spring.config.import=optional:nacos:...
```

禁止新增 `bootstrap.yml` 或 `spring-cloud-starter-bootstrap`，除非后续明确切换到旧 bootstrap 上下文模型。

必须支持环境变量：

```text
NACOS_SERVER_ADDR
NACOS_NAMESPACE
NACOS_GROUP
NACOS_USERNAME
NACOS_PASSWORD
SPRING_PROFILES_ACTIVE
SERVER_PORT
```

配置中不得提交真实密码、Token、Secret、私钥或个人电脑路径。

## 9. 禁止创建

```text
synapse-platform-bom
synapse-platform-common
synapse-platform-api
synapse-i18n-platform
synapse-notification-platform
synapse-import-export-platform
synapse-admin-web
任何业务系统模块
```

## 10. 验证

提交前至少运行：

```bash
mvn validate
mvn clean test
mvn -pl synapse-gateway-platform dependency:tree
```

Gateway 依赖树必须确认不存在 `synapse-webmvc` 和 `synapse-data`。
