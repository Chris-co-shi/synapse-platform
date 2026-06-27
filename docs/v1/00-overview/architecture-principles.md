# Architecture Principles：Synapse Platform 架构原则

本文是 Synapse Platform v1 的架构宪法。ADR 记录具体决策，本文记录所有决策必须服从的长期原则。

## P-001 平台不承载业务领域

Synapse Platform 是企业基座，不是 MES、WMS、QMS、EMS、MOM、ERP 或 IoT 业务系统。

Platform 可以提供业务系统需要的公共能力，但不能把具体业务流程、业务规则、业务实体或业务数据库收入平台核心。

## P-002 业务系统自治，平台能力复用

业务系统拥有自己的领域模型、数据库、业务流程、发布节奏和运行边界。

Platform 提供统一身份、权限、入口、日志、审计、监控、任务、文件、消息、集成等公共能力。

业务系统通过 API、Client、事件、Webhook 或连接器接入 Platform，而不是直接共享 Platform 数据库。

## P-003 Framework 与 Platform 解耦

Synapse Framework 是技术底座，Synapse Platform 是平台服务集合。

Framework 不允许包含 IAM 业务、Gateway 服务、数据库业务模型、Controller 或任何 Platform 业务代码。

Platform 可以依赖 Framework；Framework 不能反向依赖 Platform。

## P-004 控制面优先于功能堆叠

Synapse Platform 的核心价值是控制面与公共能力，而不是功能数量。

优先保证身份、安全、入口、审计、日志、运维与接入闭环，再扩展消息、文件、任务、集成、报表、工作流等能力。

## P-005 API First / Contract First

跨服务、跨系统、跨团队的交互必须先稳定契约，再实现内部代码。

`*-api` 保存稳定契约，`*-client` 保存调用适配，`*-server` 承载实现。禁止通过共享 Entity、Mapper、Repository 或数据库来替代契约。

## P-006 默认安全，显式放行

平台安全默认 fail closed。

除明确白名单外，入口默认需要认证；下游服务必须独立验证 Token 与可信入口证明；Gateway 认证成功不能成为下游跳过鉴权的理由。

## P-007 Gateway 是入口，不是业务授权中心

Gateway 负责统一入口、路由、Token 基础验证、Header 清理、可信入口证明与基础流量治理。

Gateway 不查询业务数据库，不执行业务资源权限，不承担组织、租户、数据范围或资源归属判断。

## P-008 IAM 是身份与授权源，不是所有权限执行点

IAM 负责主体、凭据、Client、Token、角色、权限与授权快照。

接口权限、资源权限、数据权限与资源归属判断由资源所在服务兜底执行。

## P-009 可部署性是一等架构属性

Synapse Platform 必须从设计阶段同时考虑：

- 本地开发；
- Docker Compose；
- 单机 / VM 私有化；
- Kubernetes；
- 多环境 dev / beta / prd；
- 企业内网、DMZ、反向代理、负载均衡与边缘接入。

部署不是编码后的附属文档，而是架构设计的一部分。

## P-010 可观测性是一等架构属性

所有关键平台能力必须支持 traceId、操作主体、请求入口、服务调用、异步任务和审计日志的串联。

日志、审计、指标、健康检查和运行状态不是后补能力，而是平台可运维性的基础。

## P-011 私有化优先，云原生兼容

Synapse Platform 面向企业制造业场景，必须优先支持私有化部署。

同时，平台设计不能和特定服务器、特定路径、特定中间件实例、特定云厂商绑定，必须兼容云原生演进。

## P-012 先冻结边界，再扩大能力

在 Architecture Freeze 前，可以调整定位、边界、模块与部署模型。

Freeze 后，任何影响平台定位、服务边界、安全模型、部署模型、网络边界或核心数据流的修改，必须通过 ADR 记录。不能以“代码已经写了”为理由反向推翻架构。
