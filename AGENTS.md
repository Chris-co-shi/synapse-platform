# Synapse Platform AI 协作规范

本文档约束 `synapse-platform` 整个仓库。修改模块前必须同时阅读：

1. 本文件。
2. 目标一级模块目录中的 `AGENTS.md`。
3. 模块规则引用的专题文档。

规则优先级：当前用户任务要求 > 距离目标文件最近的 `AGENTS.md` > 上级 `AGENTS.md` > 通用文档。冲突不得静默处理，最终报告必须说明。

## 1. 项目定位

Synapse Platform 是基于 Java 21、Spring Boot、Spring Cloud 和 Spring Cloud Alibaba 的企业级微服务平台。

Platform 只能通过 Maven 依赖复用相邻 `../synapse-framework`，禁止修改 Framework 或让 Framework 反向依赖 Platform。Framework 能力以其当前根 POM、`synapse-bom`、源码和模块手册为准。

## 2. 一级模块

仓库包含 Gateway、IAM、Resource、Config、Audit、File、Message、Task、Workflow、Integration、MDM、Report、Monitor 共 13 个一级模块。

Gateway 不拆分。其余模块采用：

```text
synapse-xxx-platform
├── synapse-xxx-api
├── synapse-xxx-client
└── synapse-xxx-server
```

## 3. 通用分层边界

- `api`：稳定跨服务契约，不依赖 client/server，不包含数据库实现。
- `client`：调用适配，允许依赖对应 api，禁止依赖 server 或直接访问服务数据库。
- `server`：启动入口与模块实现，允许依赖对应 api，禁止依赖自己的 client 或其他模块 server。
- 跨服务调用通过 api/client 或消息契约完成，禁止共享 Entity、Mapper、Repository。

## 4. 依赖与基线

- Java 21，Maven 3.9.x。
- 根工程 import `com.indigo.synapse:synapse-bom`；官方组件 BOM 可由 Platform 直接 import。
- 禁止循环依赖、跨服务数据库访问和 Framework -> Platform 依赖。
- 不得引用已从当前 Framework 删除或更名的 artifact。
- 新增生产依赖前必须说明必要性、替代方案和影响范围。

## 5. 配置与凭据

- Spring Boot 3 使用 ConfigData，不新增 `bootstrap.yml`。
- 配置支持环境变量或外部配置中心注入，应用保持无状态。
- 禁止提交密码、Token、Secret、私钥、真实 `.env` 或个人绝对路径。
- 禁止在日志、异常或诊断命令中输出认证材料和完整环境变量。

## 6. 启动类约定

- Gateway 和各领域 server 的启动类必须位于对应领域 Java 根包。
- 禁止将生产启动类放入 `bootstrap`、`boot`、`launcher` 或 `startup` 子包。
- `api` 和 `client` 模块禁止存在生产启动类。

## 7. 修改原则

- 修改前先搜索并阅读相关规则、设计、接口和测试文档。
- 优先最小修改和现有结构，不进行未授权的大规模重构。
- 不删除测试、降低断言、绕过校验或把临时实现伪装成最终能力。
- 当前事实优先于旧 README、历史任务和规划描述。
- 代码说明、计划、总结和自查使用中文；命名遵循项目既有习惯。

## 8. 最低验证

```bash
mvn validate
mvn clean test
git diff --check
```

模块规则可增加更严格验证。未执行的命令必须如实标记并说明原因。

## 9. 完成报告

必须列出修改、新增、删除文件，核心实现，验证命令和结果，未完成事项、技术债与风险点。未获用户明确授权不得自动提交。

## 10. 模块和专题索引

- [Gateway 模块规则](synapse-gateway-platform/AGENTS.md)
- [Gateway 设计与安全模型](docs/gateway.md)
- [Gateway Docker 部署](deploy/docker/gateway/README.md)
- 其余模块规则位于对应一级模块目录的 `AGENTS.md`。
