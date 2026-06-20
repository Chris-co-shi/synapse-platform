# Synapse Platform

Synapse Platform 是独立 Java 微服务项目，基于 Synapse Framework 技术基座构建。

Platform 通过 Maven 依赖复用 Framework，根 POM 只 import `com.indigo.synapse:synapse-bom`。Spring Boot、Spring Cloud、Spring Cloud Alibaba 和 Framework 模块版本由 `synapse-bom` 管理。

当前 TASK-002 只创建完整后端微服务骨架，不实现任何业务功能。

## 模块结构

```text
synapse-platform
├── synapse-gateway-platform
├── synapse-iam-platform
│   ├── synapse-iam-api
│   ├── synapse-iam-client
│   └── synapse-iam-server
├── synapse-resource-platform
│   ├── synapse-resource-api
│   ├── synapse-resource-client
│   └── synapse-resource-server
├── synapse-config-platform
│   ├── synapse-config-api
│   ├── synapse-config-client
│   └── synapse-config-server
├── synapse-audit-platform
│   ├── synapse-audit-api
│   ├── synapse-audit-client
│   └── synapse-audit-server
├── synapse-file-platform
│   ├── synapse-file-api
│   ├── synapse-file-client
│   └── synapse-file-server
├── synapse-message-platform
│   ├── synapse-message-api
│   ├── synapse-message-client
│   └── synapse-message-server
├── synapse-task-platform
│   ├── synapse-task-api
│   ├── synapse-task-client
│   └── synapse-task-server
├── synapse-workflow-platform
│   ├── synapse-workflow-api
│   ├── synapse-workflow-client
│   └── synapse-workflow-server
├── synapse-integration-platform
│   ├── synapse-integration-api
│   ├── synapse-integration-client
│   └── synapse-integration-server
├── synapse-mdm-platform
│   ├── synapse-mdm-api
│   ├── synapse-mdm-client
│   └── synapse-mdm-server
├── synapse-report-platform
│   ├── synapse-report-api
│   ├── synapse-report-client
│   └── synapse-report-server
└── synapse-monitor-platform
    ├── synapse-monitor-api
    ├── synapse-monitor-client
    └── synapse-monitor-server
```

统计：

- 一级平台模块：13 个
- API 模块：12 个
- Client 模块：12 个
- Server 模块：12 个
- 可启动服务：13 个
- Maven reactor 项目：50 个，含根工程

## 技术边界

- Gateway 使用 Spring Cloud Gateway / WebFlux。
- 普通 Server 使用 WebMVC。
- 所有可启动服务接入 Nacos Discovery、Nacos Config 和 Actuator。
- Nacos Config 按 Spring Boot 3 / Spring Cloud ConfigData 规范通过 `spring.config.import` 加载，不使用 `bootstrap.yml`。
- 每个可启动服务包含 `dev / beta / prd` 三套环境覆盖配置。
- 每个普通 Server 预留独立 Flyway 目录，但不创建 migration。

## Framework 配置属性

本项目只使用本地 Framework 已核实存在的配置属性：

```text
synapse.cloud.*
synapse.time.default-zone
synapse.config.values
synapse.i18n.*
synapse.oauth2.*
synapse.file.local-root
```

配置属性来源：

- `../synapse-framework/synapse-cloud/.../SynapseCloudProperties.java`
- `../synapse-framework/synapse-cloud/.../SynapseFeignProperties.java`
- `../synapse-framework/synapse-time/.../SynapseTimeProperties.java`
- `../synapse-framework/synapse-config/.../SynapseConfigProperties.java`
- `../synapse-framework/synapse-i18n/.../SynapseI18nProperties.java`
- `../synapse-framework/synapse-oauth2/.../SynapseOAuth2Properties.java`
- `../synapse-framework/synapse-file/.../SynapseFileProperties.java`

## 禁止项

不创建：

- `synapse-platform-bom`
- `synapse-platform-common`
- 统一 `synapse-platform-api`
- `synapse-i18n-platform`
- `synapse-notification-platform`
- `synapse-import-export-platform`
- `synapse-admin-web`
- 业务系统模块

## 验证

```bash
mvn validate
mvn clean test
mvn -pl synapse-gateway-platform dependency:tree
```
