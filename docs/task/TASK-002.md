# TASK-002 创建 Synapse Platform 完整 Java 微服务骨架

## 任务目标

在 TASK-001 基础上创建完整后端微服务骨架，只建立 Maven 聚合结构、API / Client / Server 分层、Spring Boot 启动类、Nacos Discovery / Config、Actuator、`dev / beta / prd` 配置和文档。

本任务不实现业务功能。

## 创建模块

- `synapse-gateway-platform`
- `synapse-iam-platform`
- `synapse-resource-platform`
- `synapse-config-platform`
- `synapse-audit-platform`
- `synapse-file-platform`
- `synapse-message-platform`
- `synapse-task-platform`
- `synapse-workflow-platform`
- `synapse-integration-platform`
- `synapse-mdm-platform`
- `synapse-report-platform`
- `synapse-monitor-platform`

除 Gateway 外，每个能力模块包含 `api / client / server` 三层。

## 迁移内容

- `synapse-iam-platform-api` 迁移为 `synapse-iam-api`。
- `synapse-iam-platform-client` 迁移为 `synapse-iam-client`。
- `synapse-iam-platform-server` 迁移为 `synapse-iam-server`。
- Message / File / Task 同步迁移为 `synapse-xxx-api/client/server`。
- Java 包名迁移为 `com.indigo.synapse.{module}`。
- 启动类最终统一位于领域根包 `com.indigo.synapse.{module}`；历史 `bootstrap` 目录规范已废止。

## 依赖方向

允许：

```text
client -> 对应 api
server -> 对应 api
platform service -> synapse-framework
```

禁止：

```text
api -> client/server
client -> server
server -> client
server -> 其他服务 server
```

## Framework 复用方式

已检查本地 `../synapse-framework`：

- 根 POM reactor
- `synapse-bom/pom.xml`
- `docs/modules/*.md`
- `@ConfigurationProperties` 类
- `AutoConfiguration.imports`

根工程只 import `com.indigo.synapse:synapse-bom`，Framework 依赖不在子模块重复声明版本。

## 配置属性来源

使用的 Framework 属性：

- `synapse.cloud.*`：`SynapseCloudProperties` / `SynapseFeignProperties`
- `synapse.time.default-zone`：`SynapseTimeProperties`
- `synapse.config.values`：`SynapseConfigProperties`
- `synapse.i18n.*`：`SynapseI18nProperties`
- `synapse.oauth2.*`：`SynapseOAuth2Properties`
- `synapse.file.local-root`：`SynapseFileProperties`

未创建 `synapse.webmvc.*`、`synapse.webflux.*` 等不存在的配置项。

## 环境配置

每个可启动服务均包含：

```text
application.yml
application-dev.yml
application-beta.yml
application-prd.yml
```

Nacos Config 按 Spring Boot 3 / Spring Cloud ConfigData 规范接入：

```text
spring.config.import=optional:nacos:...
```

不创建 `bootstrap.yml`，不引入 `spring-cloud-starter-bootstrap`。

支持：

- `NACOS_SERVER_ADDR`
- `NACOS_NAMESPACE`
- `NACOS_GROUP`
- `NACOS_USERNAME`
- `NACOS_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT`

配置中未写入真实密码、Token、Secret、私钥或个人电脑路径。

## 验证结果

待执行：

```bash
mvn validate
mvn clean test
mvn -pl synapse-gateway-platform dependency:tree
```

## 已知风险

- 当前只创建骨架，未实现真实业务契约、Feign Client、Controller、Entity、Mapper、Repository、Service 和 Migration。
- `application-prd.yml` 中 IAM OAuth2 设置 `production=true`，后续生产运行必须提供真实 `RSAKey` 和 `TokenDenylistPort`。
- Nacos 配置导入语法需要通过当前 Spring Cloud Alibaba 2025.0.0.0 依赖实际启动验证。
