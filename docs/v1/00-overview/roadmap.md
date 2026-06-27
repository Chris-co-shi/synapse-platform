# Roadmap：Architecture Sprint 路线图

本文记录 Synapse Platform v1 Architecture Sprint 的讨论顺序与产物目标。

## Phase 1：Vision

目标：明确项目定位、产品定位、用户定位、一期目标和长期目标。

产物：

- Vision；
- Architecture Principles；
- Glossary；
- ADR-001 Platform Positioning。

## Phase 2：Architecture

目标：明确总体架构、模块划分、服务边界、控制面与数据面。

候选 ADR：

- ADR-002 Framework 与 Platform 边界；
- ADR-003 Platform 服务边界与模块拆分；
- ADR-004 控制面与数据面；
- ADR-005 API / Client / Server 契约模型。

## Phase 3：Deployment

目标：明确 Docker、Kubernetes、VM、企业私有化和 Edge 部署模型。

候选 ADR：

- ADR-006 部署模型；
- ADR-007 环境隔离与配置模型；
- ADR-008 私有化部署基线；
- ADR-009 Kubernetes 部署边界。

## Phase 4：Network

目标：明确 Gateway、WAF、LB、DMZ、Trust Boundary、Service Mesh 是否需要。

候选 ADR：

- ADR-010 Gateway 作为统一入口；
- ADR-011 网络信任边界；
- ADR-012 DMZ 与企业内网部署；
- ADR-013 Service Mesh 是否进入 v1。

## Phase 5：Security

目标：明确 IAM、OAuth2、OIDC、JWT、GatewayProof、Client、Resource Server 安全模型。

候选 ADR：

- ADR-014 IAM 职责边界；
- ADR-015 OAuth2/OIDC 协议模型；
- ADR-016 JWT 与 Refresh Token 策略；
- ADR-017 GatewayProof 与下游验证；
- ADR-018 RBAC 与权限执行边界。

## Phase 6：Integration

目标：明确 Connector、Adapter、External System、Webhook、MQ、File、API 的接入模型。

候选 ADR：

- ADR-019 业务系统接入模型；
- ADR-020 外部系统集成模型；
- ADR-021 Webhook 与回调模型；
- ADR-022 MQ 与事件驱动边界；
- ADR-023 File 平台能力边界。

## Phase 7：Scalability

目标：明确多租户、多园区、多 Region、高可用、灾备和水平扩展策略。

候选 ADR：

- ADR-024 多租户策略；
- ADR-025 多园区模型；
- ADR-026 高可用与灾备；
- ADR-027 水平扩展与无状态服务；
- ADR-028 数据隔离模型。

## Phase 8：Architecture Freeze

目标：冻结 v1 架构基线，进入 Design / Coding / Testing 阶段。

冻结条件：

1. Vision 已确认；
2. Architecture Principles 已确认；
3. 关键 ADR 已确认；
4. 服务边界已确认；
5. 安全模型已确认；
6. 部署模型已确认；
7. 网络边界已确认；
8. 一期范围与明确不做事项已确认。
