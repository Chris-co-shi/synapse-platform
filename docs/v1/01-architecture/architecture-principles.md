# Synapse Platform 架构原则

## 1. 平台不承载业务领域

Platform 不是 MES、WMS、QMS、ERP 或 IoT 业务系统。生产订单、库存、检验、设备和其他领域模型属于业务系统。

## 2. 外部系统默认黑盒

未明确证明代码、发布和安全配置由 Synapse 团队控制的系统，默认按外部黑盒处理。

不能因为系统名称是 MES 或 WMS，就假设它会使用 Framework、Synapse IAM、JWT、Manifest 或统一权限模型。

## 3. 标准协议优先

跨组织和跨技术栈接入优先使用 OAuth 2.0、OpenID Connect、JWT、JWK 和标准 HTTP 契约。

Synapse 扩展必须可选，不能成为第三方最低接入门槛。

## 4. Framework 与 Platform 单向依赖

Platform 可以依赖 Framework；Framework 不得依赖 Platform。

Framework 提供技术能力，Platform 承载可启动服务、业务数据和管理流程。

## 5. 服务自治

服务拥有自己的数据、迁移、发布和运行边界。禁止跨服务数据库查询、跨服务外键以及共享 Entity、Mapper、Repository。

## 6. 默认安全

- 默认拒绝，显式放行；
- Gateway 和下游服务都验证 JWT；
- Gateway 不是业务授权中心；
- 不信任网络位置、身份 Header 或 GatewayProof；
- Token 和认证材料不进入日志。

## 7. 简单优先

不为架构完整感提前引入微服务、事件投影、Manifest、版本树或通用适配平台。

只有真实消费者、明确上线阻断或安全风险，才进入当前迭代。

## 8. 成本与迭代必须显式

每个重要需求必须给出：

- 真实消费者；
- 不做的后果；
- 成本 S/M/L/XL；
- NOW/NEXT/LATER/REJECTED；
- 本次明确不做内容。

## 9. 先闭环，再扩展

V1 优先完成：用户登录、服务调用、第三方 Client Credentials、JWT 验证、权限和审计主体。

File、Message、Task、Resource Catalog、Integration、Workflow、MDM、Report 等能力必须独立证明产品价值后再进入迭代。

## 10. 适配发生在边界

Synapse 调用外部系统时遵守对方协议；外部系统调用 Synapse 时遵守 Synapse API 的标准 OAuth2 契约。

协议不兼容时使用 Adapter / Anti-Corruption Layer，不能把 IAM 或 Gateway 变成万能协议转换器。

## 11. 目标与事实分离

设计文档可以描述目标状态，但必须同时维护 Gap Analysis。没有源码、测试和部署证明的能力不得写成已完成。

## 12. 可部署与可运维

Java、数据库、中间件、端口、配置和部署方式必须有可重复验证。未经实际验证的数据库、Kubernetes、高可用或灾备方案不能成为当前交付承诺。
