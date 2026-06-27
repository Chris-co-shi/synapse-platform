# Vision：Synapse Platform 项目愿景

## 1. 一句话定位

Synapse Platform 是面向制造业数字化系统的企业级数字化平台基座，为企业 IT 和业务系统提供统一身份、统一入口、权限、审计、日志、监控、任务、文件、消息、集成与部署支撑。

它不是 MES、WMS、QMS、EMS、MOM、ERP 周边或 IoT 业务系统本身，而是这些系统之上的公共能力抽象与控制面。

## 2. 项目目标

Synapse Platform 的目标是成为一个可以长期演进、真实落地、支持私有化与云原生部署的企业级平台产品。

核心目标：

1. 支持真实企业项目落地；
2. 支持制造业数字化系统接入；
3. 支持 Docker、VM、Kubernetes 与企业私有化部署；
4. 支持业务系统自治，而不是把所有业务塞入平台；
5. 沉淀统一的架构、工程、安全、部署与运维规范；
6. 作为长期维护的个人技术代表作，而不是一次性 Demo。

## 3. 使用对象

Synapse Platform 的主要用户不是一线业务操作员，而是企业数字化体系中的能力建设者与运维者：

- 企业 IT 团队；
- 平台管理员；
- 安全管理员；
- 运维团队；
- 企业内部开发团队；
- 第三方系统集成团队；
- 需要接入统一身份、权限、审计、日志、任务与集成能力的业务系统团队。

## 4. Synapse Platform 提供什么

Platform 提供企业公共能力：

- Gateway：统一入口、路由、认证前置、安全边界；
- IAM：身份认证、OAuth2/OIDC、RBAC、主体与权限模型；
- Audit：审计事件、操作日志、安全日志；
- Monitor：平台运行状态、服务健康、基础观测入口；
- Task：分布式任务、定时任务、异步任务追踪；
- Message：站内消息、通知、消息模板与发送记录；
- File：文件元数据、上传下载、存储适配；
- Integration：外部系统接入、Webhook、API 调用、连接器；
- Config：业务配置、字典、可管理配置资源；
- Workflow / Report / MDM：作为后续平台能力候选，不能在一期扩大边界。

## 5. Synapse Platform 不做什么

Platform 明确不承载具体业务域：

- 不实现 MES 生产业务；
- 不实现 WMS 库存业务；
- 不实现 QMS 质量业务；
- 不实现 EMS 能源业务；
- 不实现 ERP 财务、采购、销售、库存主流程；
- 不成为大一统业务数据库；
- 不把所有业务系统改造成 Platform 的子模块；
- 不以低代码、BI、AI 平台作为一期目标。

## 6. 与 Synapse Framework 的关系

Synapse Framework 是 Synapse 技术体系的统一开发框架，为 Platform 与未来业务系统提供一致的技术基础设施与开发规范。

Framework 提供技术能力：Web、Security、OAuth2、Data、Messaging、Audit、Observability、AutoConfiguration 等。

Platform 组合这些技术能力，形成可运行的平台服务。

边界原则：

```text
业务系统 -> 依赖 Framework，可接入 Platform 能力
Platform  -> 依赖 Framework，提供平台服务
Framework -> 不依赖 Platform，不包含平台业务
```

## 7. 一期目标

Synapse Platform v1 的第一期目标是完成企业平台最小闭环：

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

## 8. 长期愿景

长期看，Synapse Platform 应该演进为企业数字化系统的 Platform Product：

- 有清晰的控制面；
- 有稳定的平台 API；
- 有可接入的 SDK / Client；
- 有可观测、可部署、可审计、可扩展的运行体系；
- 能在不同企业项目中复用，而不是绑定某一个项目。
