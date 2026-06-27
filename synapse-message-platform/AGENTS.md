# Synapse Message 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Message 承载平台消息服务；Framework 当前技术模块名称为 `synapse-messaging`。

## 2. 子模块边界

- api 保存稳定消息服务契约。
- client 提供调用适配，只依赖 api。
- server 承载启动入口和平台消息实现并允许依赖 api。
- Message 启动类必须位于 `com.indigo.synapse.message` 根包，禁止放入 `bootstrap` 子包。

## 3. 允许依赖

client/server -> api；server 可使用 Framework `synapse-messaging` 技术契约。

## 4. 禁止事项

禁止继续引用旧 `synapse-mq`；禁止 api/client 包含消息存储实现；禁止 server 依赖自己的 client 或其他 server。

## 5. 验证命令

```bash
mvn -pl synapse-message-platform -am test
```

## 6. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [服务边界](../docs/v1/01-architecture/service-boundary.md)
- [安全架构](../docs/v1/01-architecture/security-architecture.md)
- [通信架构](../docs/v1/01-architecture/communication-architecture.md)
