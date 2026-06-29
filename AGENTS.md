# Synapse Platform AI 协作规范

修改本仓库前必须阅读：

1. 本文件；
2. `docs/v1/00-product/v1-baseline.md`；
3. `docs/v1/03-gap-analysis/repository-gap-analysis.md`；
4. 目标模块最近的 `AGENTS.md`；
5. 相关源码、测试和设计。

规则优先级：当前用户要求 > V1 架构基线 > 本文件 > 最近模块规则 > 其他文档。旧模块规则与基线冲突时，以基线为准并记录 Gap。

## 项目定位

Platform 是可运行产品，可以依赖 Framework；Framework 不得反向依赖 Platform。服务之间禁止共享数据库、Entity、Mapper、Repository 或 server 模块。

当前 V1 只交付 Identity & Access Foundation。仓库中存在模块不代表进入 V1。

## V1 NOW

- Gateway 与 IAM；
- OAuth 2.0 / OpenID Connect；
- Authorization Code + PKCE；
- Client Credentials；
- RS256 JWT Access Token；
- Opaque Refresh Token rotation 和 reuse detection；
- USER / CLIENT 主体；
- Role / Permission / Client Permission；
- Gateway 和下游服务独立验证 JWT；
- 基础安全审计；
- PostgreSQL 17、Redis、Nacos 最小运行闭环。

其余平台模块当前均不是 V1 完成前置条件。

## 安全硬约束

- 不再新增 Opaque Access Token 路径；
- Gateway 保留 WebFlux Resource Server，运行在 Authentication Only 模式；
- GatewayProof 已取消；
- Gateway 不注入可信身份或权限 Header；
- 下游服务必须独立验证 JWT；
- JWT 不携带 roles、permissions、菜单、数据范围或虚假租户；
- USER 与 CLIENT 不得相互伪装；
- 认证材料不得进入日志或仓库。

## 企业集成边界

MES、WMS、SAP 和遗留系统默认按外部黑盒处理。不强制使用 Framework、Synapse JWT、Manifest 或集中权限模型；协议不兼容时使用项目级 Adapter。没有多个重复场景时，不建设通用 Integration Platform。

## 架构准入

每个需求开始前必须说明：

1. 对应哪条 V1 闭环；
2. 当前真实消费者；
3. 不做的后果；
4. 成本 S/M/L/XL；
5. NOW/NEXT/LATER/REJECTED；
6. 本次明确不做内容。

没有真实消费者或只提升架构完整感的能力，默认不进入 NOW。

## 工程约束

- Gateway 只使用 WebFlux，禁止 MVC、Servlet、数据库和其他平台 server 依赖；
- `api` 不依赖 client/server；`client` 不依赖 server；`server` 不依赖自己的 client 或其他领域 server；
- 当前单租户，不新增 TenantContext 或租户拦截器；
- 时间点使用 `Instant`，业务日期使用 `LocalDate`，时区使用显式 `ZoneId`；
- 每个任务只处理一个 Gap，不顺带实现 Manifest、授权快照、Revocation Feed、多租户或 Integration Platform；
- 不删除测试、降低断言或绕过安全校验；
- 未经授权不得直接合并到 `main`。

## 验证与报告

```bash
mvn validate
mvn clean test
git diff --check
```

完成报告必须列出文件变化、Gap 决策、验证结果、未完成事项和风险。
