# ADR-001：Synapse Platform 项目定位与边界

## Status

Proposed

## Context

Synapse Platform 正在从代码驱动开发转向 Architecture Sprint。项目目标不是构建一个 Spring Cloud Demo，而是形成可真实落地、可私有化部署、可云原生演进、可长期维护的企业级数字化平台基座。

随着 Gateway、IAM、OAuth2、Resource Server、Client、业务系统接入、Integration、Edge Connector 等问题逐步浮现，平台边界需要先被清晰定义，否则后续会出现范围膨胀、服务边界混乱、Framework 与 Platform 互相污染、业务系统被平台吞并等问题。

## Why

需要先回答：Synapse Platform 到底是什么，以及它不是什么。

如果没有这个决策，后续每个模块都会产生边界争议：

- IAM 是否应该承载菜单、组织、业务资源、数据权限执行？
- Gateway 是否应该执行业务授权？
- Platform 是否应该包含 MES / WMS / QMS 等业务模块？
- Framework 是否可以放入 IAM、Gateway、Controller 或数据库模型？
- 业务系统是否必须成为 Platform 的子模块？
- Integration、Task、Message、File、Audit、Monitor 等能力应作为平台公共能力，还是业务系统内部能力？

ADR-001 的目标是冻结最高层边界，避免后续反复推翻。

## Alternatives

### 方案 A：Synapse Platform 作为统一业务平台

把 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 等业务系统都放入 Synapse Platform，形成一个大一统企业数字化平台。

优点：

- 初期看起来完整；
- 所有功能在一个项目中，集成成本低；
- 对 Demo 展示友好。

缺点：

- 平台会无限膨胀；
- 业务系统无法自治；
- 数据库和领域模型高度耦合；
- 任意业务变化都会影响平台发布；
- 长期维护风险极高。

不采用原因：这会把 Platform 变成 ERP/MES 大杂烩，违背企业级平台基座定位。

### 方案 B：Synapse Platform 只是微服务脚手架

把 Synapse Platform 定义为 Spring Cloud 工程模板，只提供网关、认证、基础依赖和样例服务。

优点：

- 边界简单；
- 实现成本低；
- 容易快速启动。

缺点：

- 不具备企业平台产品价值；
- 无法沉淀权限、审计、日志、任务、集成等公共能力；
- 更像 Demo 或 Starter，不足以成为长期代表作。

不采用原因：项目目标不是学习脚手架，而是真实企业基座。

### 方案 C：Synapse Platform 作为企业级数字化平台基座

Platform 提供统一身份、权限、入口、监控、日志、审计、任务、文件、消息、集成等公共能力。业务系统保持自治，通过标准协议和 SDK 接入平台。

优点：

- 平台边界清晰；
- 业务系统自治；
- 公共能力可复用；
- 支持私有化与云原生部署；
- 具备长期演进空间。

缺点：

- 前期架构设计成本较高；
- 需要严格治理边界；
- 对文档、ADR、规范和工程纪律要求更高。

采用原因：最符合项目长期目标和真实企业架构实践。

## Decision

Synapse Platform 定位为：

> 面向制造业数字化系统的企业级数字化平台基座，负责提供企业 IT 所需的控制面与共享平台能力，包括统一身份、OAuth2/OIDC、RBAC、统一入口、日志、审计、监控、任务、文件、消息、集成与部署支撑；但不承载具体业务系统的领域模型、业务流程和业务规则。

Synapse Platform 是 Platform Product，而不是业务系统，也不是简单脚手架。

业务系统包括但不限于 MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 与第三方业务系统。这些系统保持自治。

## Scope

### Platform 负责

- Gateway；
- IAM；
- RBAC；
- OAuth2/OIDC；
- Audit；
- Log；
- Monitor；
- Task；
- File；
- Message；
- Integration；
- Config / Dict；
- 平台接入规范；
- 平台部署与运维规范。

### Platform 不负责

- MES 生产业务；
- WMS 库存业务；
- QMS 质量业务；
- EMS 能源业务；
- MOM 制造运营业务；
- ERP 核心业务；
- 业务系统数据库；
- 业务系统领域模型；
- 业务系统流程编排；
- 低代码、完整 BI、完整 BPMN、AI 平台等非一期核心能力。

## Framework Relationship

Synapse Framework 是技术底座，向 Platform 和未来业务系统提供技术支持 jar。

Framework 只提供通用技术能力，不包含业务代码、Controller、IAM 业务、Gateway 服务或数据库业务模型。

依赖方向：

```text
业务系统 -> Framework
业务系统 -> Platform Client / API
Platform  -> Framework
Framework -> 不依赖 Platform
```

## Impact

该决策影响：

- Platform 模块边界；
- Framework 与 Platform 依赖方向；
- IAM、Gateway、Audit、Monitor、Task、File、Message、Integration 的职责划分；
- 业务系统接入方式；
- 文档结构；
- 后续 ADR 编写；
- Architecture Freeze 的判断标准。

## Consequences

正向影响：

- 平台定位稳定；
- 业务系统自治；
- 后续模块拆分有统一依据；
- 能支撑长期演进与作品集表达。

代价：

- 不能为了快速展示而把业务功能塞入 Platform；
- 每个新增模块都要证明自己是平台公共能力；
- 需要维护 ADR、架构文档和边界约束。

## Follow-ups

后续 ADR 至少包括：

- ADR-002：Framework 与 Platform 边界；
- ADR-003：Platform 服务边界与模块拆分；
- ADR-004：Gateway 作为统一入口与信任边界；
- ADR-005：IAM / OAuth2 / OIDC 安全模型；
- ADR-006：业务系统接入模型；
- ADR-007：部署模型；
- ADR-008：日志、审计与可观测性边界。
