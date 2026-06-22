# Synapse IAM 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

IAM 承载平台认证、主体和授权模型以及 Token 签发业务。Framework OAuth2/Security 只提供技术能力。

## 2. 子模块边界

- `synapse-iam-api`：稳定调用契约，不暴露数据库 Entity。
- `synapse-iam-client`：IAM 调用适配，只依赖对应 api。
- `synapse-iam-server`：IAM 启动入口和业务实现，允许使用 WebMVC 与数据访问能力。
- IAM 启动类必须位于 `com.indigo.synapse.iam` 根包，禁止放入 `bootstrap` 子包。

修改 IAM 前必须阅读 [`docs/iam.md`](../docs/iam.md)，并以 Framework 当前 claim 常量、validator 和 Resource Server converter 为 Token 协议事实来源。

## 3. 允许依赖

client/server 可依赖对应 api；server 可按当前 Framework BOM 引用必要技术模块。

## 4. 禁止事项

- api/client 禁止包含持久化实现。
- client 禁止依赖 server 或绕过服务接口访问数据库。
- server 禁止依赖自己的 client 或其他平台 server。
- 不把 IAM 业务反向写入 Framework。
- 不在日志中记录密码、Token、私钥、Client Secret 或完整认证材料。
- 不传播或信任客户端可伪造的用户、角色和权限 Header。
- 新增公开类型和公开方法必须提供完整中文 Javadoc；配置属性必须提供中文说明和 Configuration Metadata。

## 5. 验证命令

```bash
mvn -pl :synapse-iam-api,:synapse-iam-client,:synapse-iam-server -am test
```

## 6. 必读文档

- [IAM 架构与阶段规划](../docs/iam.md)
- [Gateway 安全边界](../docs/gateway.md)
- [仓库模块边界](../docs/02-模块边界与服务设计.md)
