# Synapse IAM 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

IAM 承载平台认证、主体和授权模型以及 Token 签发业务。Framework OAuth2/Security 只提供技术能力。

## 2. 子模块边界

- `synapse-iam-api`：稳定调用契约，不暴露数据库 Entity。
- `synapse-iam-client`：IAM 调用适配，只依赖对应 api。
- `synapse-iam-server`：IAM 启动入口和业务实现，允许使用 WebMVC 与数据访问能力。
- IAM 启动类必须位于 `com.indigo.synapse.iam` 根包，禁止放入 `bootstrap` 子包。

## 3. 允许依赖

client/server 可依赖对应 api；server 可按当前 Framework BOM 引用必要技术模块。

## 4. 禁止事项

- api/client 禁止包含持久化实现。
- client 禁止依赖 server 或绕过服务接口访问数据库。
- server 禁止依赖自己的 client 或其他平台 server。
- 不把 IAM 业务反向写入 Framework。

## 5. 验证命令

```bash
mvn -pl synapse-iam-platform -am test
```

## 6. 相关文档

- [仓库模块边界](../docs/02-模块边界与服务设计.md)
