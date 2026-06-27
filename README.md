# Synapse Platform

Synapse Platform 是面向制造业数字化系统的企业级数字化平台基座，用于为 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 与第三方业务系统提供统一身份、权限、入口、日志、审计、监控、任务、文件、消息、集成与部署支撑。

它不是具体业务系统，也不是 Spring Cloud Demo。它的目标是成为一个可真实落地、支持私有化部署、支持云原生演进、能够长期维护的平台产品。

## 当前阶段

当前分支处于：

```text
Architecture Sprint v1
```

本阶段优先完成架构设计与 ADR，不进入 Controller、Service、Repository、Entity、Mapper、API 参数等实现细节。

目标流程：

```text
Architecture -> ADR -> Design -> Coding -> Testing
```

## 核心定位

```text
Synapse Platform = 企业基座 / 控制面 / 共享平台能力
Synapse Framework = 技术底座 / 通用开发框架
Business Systems  = 自治业务系统
```

Platform 提供公共能力，但不承载业务领域模型。

业务系统保持自治，通过 API、Client、事件、Webhook、Connector、Gateway 与统一身份权限体系接入 Platform。

## Architecture Sprint 文档

v1 架构文档入口：

- [Architecture Sprint v1](docs/v1/README.md)
- [Vision](docs/v1/00-overview/vision.md)
- [Architecture Principles](docs/v1/00-overview/architecture-principles.md)
- [Glossary](docs/v1/00-overview/glossary.md)
- [Roadmap](docs/v1/00-overview/roadmap.md)
- [ADR-001：Synapse Platform 项目定位与边界](docs/v1/01-adr/ADR-001-platform-positioning.md)
- [System Context and Boundary](docs/v1/02-architecture/system-context-and-boundary.md)
- [Architecture Freeze](docs/v1/decisions/architecture-freeze.md)

## V1 一期目标

一期目标是完成企业平台最小闭环：

```text
用户登录
  -> OAuth2/OIDC Token 签发
  -> Gateway 统一入口
  -> 下游资源服务独立验证
  -> RBAC 权限模型
  -> 服务端权限兜底
  -> 操作日志与安全审计
  -> 业务系统具备标准接入方式
```

一期必须完成：

- RBAC；
- OAuth2.0 / OIDC 基础闭环；
- Gateway 统一入口；
- IAM 登录、刷新、登出、当前用户；
- 日志与审计闭环；
- 平台接入规范；
- 基础部署口径。

## 边界原则

Platform 不负责：

- MES 生产业务；
- WMS 库存业务；
- QMS 质量业务；
- EMS 能源业务；
- MOM 制造运营业务；
- ERP 核心业务；
- 业务系统数据库；
- 业务系统领域模型；
- 业务系统流程编排。

Framework 不负责：

- IAM 业务；
- Gateway 服务；
- 业务 Controller；
- 业务 Entity / Mapper / Repository；
- Platform 数据库业务模型。

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

## 安全说明

禁止提交真实密码、Token、GatewayProof secret、Registry 凭据、私钥或 `.env`。

GatewayProof 不能替代 JWT；Gateway 认证成功不能成为下游服务跳过 Token 验证、资源权限或数据权限判断的理由。
