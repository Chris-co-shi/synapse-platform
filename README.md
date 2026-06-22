# Synapse Platform

Synapse Platform 是基于 Java 21、Spring Boot、Spring Cloud 和 Spring Cloud Alibaba 的企业级微服务平台。它通过 Maven 复用独立的 Synapse Framework，并承载 Gateway、IAM、文件、消息等可启动平台服务。

## 当前状态

- 已建立 13 个一级平台模块。
- Gateway 已实现 Reactive JWT 入口认证、明确白名单和默认保护策略。
- Gateway 会始终清理外部 `X-Synapse-Gateway-*` Header，并按 Framework 协议为最终下游请求签发 GatewayProof。
- Gateway 提供 Java 21 非 root 镜像、Docker Compose 部署、健康检查和失败回滚。
- 其他平台模块仍处于不同建设阶段；全仓 POM 已移除当前 Framework 不再提供的 artifact，并按现有代码需要直接声明 Spring Cloud Alibaba 组件。

## 模块概览

| 模块 | 定位 |
| --- | --- |
| `synapse-gateway-platform` | 统一入口、Reactive JWT、路由、GatewayProof |
| `synapse-iam-platform` | 认证、主体、授权模型和 Token 签发 |
| `synapse-resource-platform` | 平台资源能力 |
| `synapse-config-platform` | 平台配置服务 |
| `synapse-audit-platform` | 平台审计服务 |
| `synapse-file-platform` | 平台文件服务 |
| `synapse-message-platform` | 平台消息服务 |
| `synapse-task-platform` | 平台任务服务 |
| `synapse-workflow-platform` | 平台工作流服务 |
| `synapse-integration-platform` | 外部系统集成 |
| `synapse-mdm-platform` | 主数据管理 |
| `synapse-report-platform` | 平台报表服务 |
| `synapse-monitor-platform` | 平台监控服务 |

除 Gateway 外，每个一级模块包含 `api`、`client`、`server`：api 保存稳定契约，client 提供调用适配，server 承载启动入口与模块实现。

## Framework 依赖事实

当前事实以本地 `../synapse-framework` 根 POM、`synapse-bom`、源码和模块手册为准。

- 根 POM import `com.indigo.synapse:synapse-bom:0.1.0-SNAPSHOT`。
- Platform 应用按实际需要直接依赖 Spring Cloud Gateway、LoadBalancer、OpenFeign、Nacos Discovery/Config 等官方组件，不通过 Framework 聚合模块间接获取。
- Framework 当前已删除 `synapse-cloud` 和 `synapse-file`。
- 原 `synapse-mq` 已更名为 `synapse-messaging`。
- Framework `synapse-messaging` 提供消息技术契约；Platform `synapse-message-platform` 是独立的业务消息服务，两者职责不同。
- Gateway 使用 Framework 的 `synapse-webflux`、`synapse-security` 和 `synapse-oauth2-resource-server-webflux`。

构建前应确保当前 Framework 构件可由本地 Maven 仓库或已配置的远程仓库解析。若使用 GitHub Packages，请在 Maven `settings.xml` 中配置访问凭据；凭据不得提交到本仓库。

## Gateway 快速验证

全仓构建基线：

```bash
mvn validate
mvn clean test
```

Gateway 定向验证可使用模块 POM：

```bash
mvn -f synapse-gateway-platform/pom.xml clean test
mvn -f synapse-gateway-platform/pom.xml dependency:tree
```

本地运行需要可访问 IAM JWK 和 Nacos：

```bash
SPRING_PROFILES_ACTIVE=dev \
NACOS_SERVER_ADDR=127.0.0.1:8848 \
IAM_ISSUER_URI=http://127.0.0.1:20001 \
IAM_JWK_SET_URI=http://127.0.0.1:20001/oauth2/jwks \
mvn -f synapse-gateway-platform/pom.xml spring-boot:run
```

开发环境默认关闭 GatewayProof 签发，但仍清理所有外部 Gateway Header。beta/prd 默认开启，必须安全注入至少 32 字节 secret。

Gateway Docker 镜像使用 Java 21 JRE、非 root 用户和最小运行 context。构建及 Compose 部署入口：

```bash
./scripts/docker/build-gateway-image.sh \
  --repository registry.example.com/synapse/synapse-gateway \
  --tag 0.1.0 \
  --platform linux/amd64

./scripts/docker/deploy-gateway.sh \
  --repository registry.example.com/synapse/synapse-gateway \
  --tag 0.1.0 \
  --env-file deploy/docker/gateway/.env
```

完整配置、回滚和单实例限制见 [Gateway Docker 部署](deploy/docker/gateway/README.md)。

## 开发规范

- [仓库级开发规则](AGENTS.md)
- [Gateway 模块规则](synapse-gateway-platform/AGENTS.md)
- [IAM 模块规则](synapse-iam-platform/AGENTS.md)
- [Resource 模块规则](synapse-resource-platform/AGENTS.md)
- [Config 模块规则](synapse-config-platform/AGENTS.md)
- [Audit 模块规则](synapse-audit-platform/AGENTS.md)
- [File 模块规则](synapse-file-platform/AGENTS.md)
- [Message 模块规则](synapse-message-platform/AGENTS.md)
- [Task 模块规则](synapse-task-platform/AGENTS.md)
- [Workflow 模块规则](synapse-workflow-platform/AGENTS.md)
- [Integration 模块规则](synapse-integration-platform/AGENTS.md)
- [MDM 模块规则](synapse-mdm-platform/AGENTS.md)
- [Report 模块规则](synapse-report-platform/AGENTS.md)
- [Monitor 模块规则](synapse-monitor-platform/AGENTS.md)

## 架构与部署文档

- [产品设计](docs/01-产品设计.md)
- [模块边界与服务设计](docs/02-模块边界与服务设计.md)
- [IAM 架构与阶段规划](docs/iam.md)
- [Gateway 设计与安全模型](docs/gateway.md)
- [Gateway Docker 部署](deploy/docker/gateway/README.md)

## 安全说明

禁止提交真实密码、Token、GatewayProof secret、Registry 凭据、私钥或 `.env`。GatewayProof 不能替代 JWT；下游 Resource Server 必须独立验证二者。
