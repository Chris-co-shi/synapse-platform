# Synapse Platform V1 范围

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 产品负责人、架构师、开发、测试、实施、运维和交付人员 |
| Purpose | 明确 V1 的交付目标、优先级、范围边界和完成标准 |
| Scope | Synapse Platform 第一个可运行开源版本 |
| Status | Accepted |

本文档是 Synapse Platform V1 范围的单一事实来源。

## 1. Product Goal

V1 交付一个：

> **可运行、可测试、文档完整、能够通过 Docker Compose 安装和启动的开源版本。**

平台先完成自身闭环。MES、WMS、QMS、EMS、MOM、ERP 周边、IoT 和第三方业务系统接入，不作为 V1 完成条件。

## 2. Priority Model

| Priority | Meaning |
| --- | --- |
| `P0` | 核心闭环和发布阻断项；未完成则 V1 不成立 |
| `P1` | V1 正式范围中的低优先级能力；V1.0 发布前完成最小范围 |
| `P2` | 仅提供基础能力，不作为完整产品交付 |
| `Out of Scope` | 明确不进入 V1 |

## 3. P0 Core Scope

### 3.1 Gateway

- 平台统一外部入口；
- 基础路由、Token 检查、Header 清理和 traceId 传播；
- 透传 Authorization Bearer Token；
- 不向下游注入用户、角色或权限 Header；
- 不执行具体资源授权和数据权限判断。

### 3.2 IAM

- 平台管理员初始化；
- 用户、组织和角色管理；
- 用户角色关系；
- 角色权限码关系；
- 登录、刷新、退出和当前用户；
- OAuth 2.0 / OpenID Connect；
- Access Token、Refresh Token 和 Client；
- 计算用户最终角色和权限集合。

IAM 不管理应用、菜单、按钮和 API 资源目录。

### 3.3 Resource Center

`synapse-resource-platform` 是 V1 P0 独立微服务，负责：

- 平台应用；
- 菜单和页面资源；
- 按钮和操作资源；
- API 资源目录；
- 权限码定义、状态、分类和描述；
- 资源与权限码关联；
- 角色授权页面所需的资源树；
- 权限码有效性校验；
- 根据已验证 Token 生成当前用户导航。

Resource 不保存用户角色关系和角色权限关系，不签发 Token，也不替其他服务执行接口权限。

### 3.4 RBAC and Runtime Authorization

- Resource 定义“有什么资源和权限”；
- IAM 保存“谁拥有什么权限”；
- 两者通过稳定、全局唯一的 `permission_code` 关联；
- 每个受保护服务独立验证 Access Token；
- 每个服务执行自身接口权限和资源授权；
- 前端菜单和按钮过滤不能替代服务端鉴权；
- 运行时不得为每个请求同步调用 IAM 和 Resource 完成鉴权。

### 3.5 Audit

V1 必须提供：

- 操作日志；
- 安全日志；
- 审计日志；
- 关键资源变更、角色授权、权限回收和访问拒绝记录；
- 管理端日志查询能力。

### 3.6 Management Console

管理端前端必须支持：

- 管理员登录；
- 用户和角色管理；
- 应用、菜单、按钮、API 与权限码管理；
- 用户角色分配；
- 角色权限分配；
- 当前用户导航；
- 操作日志、安全日志和审计日志查询；
- 基础平台运行状态查看。

### 3.7 Docker Compose

Docker Compose 是 V1 正式支持的部署方式，必须提供：

- 可执行的部署编排；
- 环境变量和配置说明；
- 数据初始化与 Flyway 迁移；
- 安装、启动、停止和重启步骤；
- 健康检查和运行验证；
- 持久化与常见故障排查说明。

VM 和 Kubernetes 不作为 V1 发布阻断项。

## 4. P1 Scope

### File

形成文件上传、下载、元数据和存储适配的最小闭环。

### Message

形成消息、模板、发送记录和最小发送链路。

### Task

形成任务定义、调度、执行记录和基本失败处理闭环。

File、Message、Task 在 P0 闭环稳定后进入主要开发，但应在 V1.0 发布前完成经确认的最小范围。

## 5. P2 Foundation

### Monitor

仅提供服务健康、基础指标和运行状态，不建设完整监控或 APM 产品。

### Config

仅满足平台自身运行配置，不建设独立完整的企业配置中心产品。

## 6. Out of Scope

V1 不包含：

- 外部业务系统真实接入验证；
- Integration 完整能力；
- Workflow、MDM 和 Report；
- 低代码、完整 BPM 和完整 BI；
- AI Agent 平台；
- 复杂多租户、多园区和多 Region；
- 完整高可用和灾备；
- Service Mesh；
- Kubernetes 正式交付；
- 商业版和企业版划分。

## 7. Self-contained Acceptance Flow

```text
Docker Compose 启动平台
  -> 初始化管理员
  -> Resource 注册应用、菜单、按钮、API 和权限码
  -> IAM 创建角色并授予权限码
  -> IAM 为用户分配角色
  -> 用户登录并获得 Token
  -> Resource 返回过滤后的当前用户导航
  -> 用户通过 Gateway 访问受保护平台接口
  -> 有权限请求成功，无权限请求被拒绝
  -> 刷新 Token 并完成退出
  -> Audit 查询资源、授权、安全和访问记录
  -> 查看平台基础运行状态
```

File、Message、Task 分别需要具备经模块设计确认的最小自闭环。

## 8. Definition of Done

V1 只有在以下条件全部满足后才视为完成：

### Functionality

- P0 能力全部可用；
- P1 能力完成确认后的最小范围；
- 平台自身闭环完整跑通；
- 不依赖外部业务系统才能运行。

### Testing

- 核心单元测试和集成测试通过；
- 身份、Token、RBAC、资源目录和访问拒绝测试通过；
- 操作日志、安全日志和审计日志测试通过；
- Docker Compose 安装与启动验证通过；
- 测试结果可追溯。

### Documentation

- 产品定义、V1 范围和总体架构；
- 服务边界、安全架构与关键 ADR；
- 开发、接口和数据规范；
- 核心模块设计；
- 测试策略和验收说明；
- Docker Compose 部署文档；
- 安装、初始化、运维和故障排查说明；
- 版本说明。

### Installation and Operation

- 新环境可依据文档完成安装；
- 必需服务和依赖可启动；
- 初始化流程可重复执行或有明确保护；
- 平台能够完成基础健康验证；
- 常见问题有排查依据。

## 9. Scope Change Rule

新增能力进入 V1 前，必须确认：

1. 是否是平台自身闭环不可缺少的能力；
2. 是否阻塞可运行开源版本发布；
3. 是否明确最小交付范围；
4. 是否具备测试和文档验收方式；
5. 是否会导致范围失控。

影响 P0、P1、完成标准或明确排除项的变更，必须更新本文档，并在必要时新增 ADR。
