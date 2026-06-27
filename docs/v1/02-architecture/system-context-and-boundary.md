# System Context and Boundary：系统上下文与边界

## 1. 系统上下文

Synapse Platform 位于企业数字化体系的公共能力层，为多个自治业务系统提供统一的平台能力。

```text
企业用户 / 管理员 / 开发团队 / 第三方系统
        ↓
入口层：LB / WAF / Reverse Proxy / Gateway
        ↓
Synapse Platform
        ├── IAM
        ├── Audit
        ├── Monitor
        ├── Task
        ├── File
        ├── Message
        ├── Config
        └── Integration
        ↓
自治业务系统
        ├── MES
        ├── WMS
        ├── QMS
        ├── EMS
        ├── MOM
        ├── ERP 周边
        ├── IoT / Edge
        └── 第三方业务系统
```

Platform 不位于某个业务系统内部，也不是所有业务系统的父工程。它是企业 IT 公共能力基座。

## 2. 三层边界

Synapse 体系分为三层：

```text
Business Systems
  MES / WMS / QMS / EMS / MOM / ERP 周边 / IoT / 第三方系统

Synapse Platform
  Gateway / IAM / Audit / Monitor / Task / File / Message / Config / Integration

Synapse Framework
  Web / Security / OAuth2 / Data / Messaging / Audit / Observability / AutoConfiguration
```

### 2.1 Framework 边界

Framework 只提供技术能力：

- WebMVC / WebFlux 基础支撑；
- Security 技术抽象；
- OAuth2 技术支持；
- Data 与 ORM 辅助；
- Messaging 技术契约；
- Audit 事件契约；
- Observability 支撑；
- AutoConfiguration。

Framework 禁止：

- IAM 业务；
- Gateway 服务；
- 业务 Controller；
- 业务 Entity / Mapper / Repository；
- Platform 数据库模型；
- 具体业务流程。

### 2.2 Platform 边界

Platform 提供可运行的平台服务。

Platform 负责组合 Framework 技术能力，并向业务系统提供公共平台能力。

Platform 禁止：

- 承载 MES / WMS / QMS / EMS / MOM 领域模型；
- 共享业务系统数据库；
- 让业务系统通过 Platform Server 内部类接入；
- 把所有业务系统放入 Platform Maven reactor；
- 使用统一大杂烩 common/api 替代明确契约。

### 2.3 Business Systems 边界

业务系统保持自治。

业务系统可以：

- 依赖 Synapse Framework；
- 使用 Synapse Platform Client；
- 调用 Platform API；
- 接收 Platform 事件；
- 通过 Gateway 暴露入口；
- 使用 IAM Token 与权限模型。

业务系统不应该：

- 直接访问 Platform 数据库；
- 依赖 Platform Server 模块；
- 把自己的业务 Entity 放入 Platform；
- 让 Platform 替自己执行业务规则。

## 3. 控制面与数据面

### 3.1 控制面

Synapse Platform 是企业数字化系统的控制面。

控制面包括：

- 用户、组织、角色、权限；
- OAuth2 Client；
- Gateway 路由和入口策略；
- 审计策略；
- 任务调度策略；
- 消息模板；
- 文件存储策略；
- 集成连接器配置；
- 平台运行状态与监控入口。

### 3.2 数据面

业务数据面由业务系统自己掌握。

MES 的生产订单、WMS 的库存、QMS 的检验单、EMS 的能耗数据、ERP 的业务单据不进入 Platform 核心数据库。

Platform 可以记录平台操作日志、审计日志、任务执行日志、文件元数据、消息记录、集成调用记录，但这些是平台运行数据，不是业务系统领域数据。

## 4. 接入模式

业务系统接入 Platform 的方式包括：

1. Gateway 统一入口；
2. OAuth2/OIDC 登录认证；
3. Bearer Token 与 Resource Server 验证；
4. RBAC 权限码；
5. Platform Client / SDK；
6. OpenAPI / Contract；
7. MQ / Event；
8. Webhook；
9. Connector / Adapter；
10. 日志、审计、Trace 接入。

禁止通过共享数据库、共享 Entity、共享 Mapper、复制 Platform 内部代码来完成接入。

## 5. V1 最小闭环

V1 最小闭环为：

```text
Admin 初始化平台资源
  ↓
创建用户、组织、角色、权限
  ↓
用户登录 IAM
  ↓
IAM 签发 Access Token / Refresh Token
  ↓
Client 请求 Gateway
  ↓
Gateway 校验 Token、清理 Header、签发可信入口证明
  ↓
下游服务独立验证 Token 和入口证明
  ↓
下游服务执行权限兜底
  ↓
Audit / Log / Trace 记录完整链路
```

该闭环成立后，Platform 才具备扩展 File、Message、Task、Integration、Monitor 等能力的基础。

## 6. 边界判断规则

判断一个能力是否应该进入 Platform：

| 问题 | 是 | 否 |
| --- | --- | --- |
| 是否多个业务系统都会复用？ | 倾向 Platform | 倾向业务系统 |
| 是否不依赖具体业务流程？ | 倾向 Platform | 倾向业务系统 |
| 是否属于身份、安全、入口、审计、监控、任务、消息、文件、集成等公共能力？ | 倾向 Platform | 倾向业务系统 |
| 是否需要读取业务系统私有数据库才能完成核心逻辑？ | 倾向业务系统 | 可考虑 Platform |
| 是否会导致 Platform 承载业务领域模型？ | 不进入 Platform | 可考虑 Platform |

## 7. 当前决策

Synapse Platform v1 采用企业平台基座定位，先完成控制面与公共能力闭环。

业务系统保持自治，Platform 通过 API、Client、事件、Webhook、Connector 与 Gateway 接入业务系统。
