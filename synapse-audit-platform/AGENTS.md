# Synapse Audit 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Audit 承载平台审计运行时服务；Framework `synapse-audit` 只提供审计技术契约和适配能力。

## 2. 子模块边界

- api 保存稳定审计调用契约。
- client 提供调用适配，只依赖 api。
- server 承载启动入口与平台审计实现并允许依赖 api。
- Audit 启动类必须位于 `com.indigo.synapse.audit` 根包，禁止放入 `bootstrap` 子包。

## 3. 允许依赖

client/server -> api；server 可按需使用当前 Framework 正式模块。

## 4. 禁止事项

禁止 api/client 放置持久化实现，禁止跨服务数据库访问，禁止 server 依赖自己的 client 或其他 server。

## 5. 验证命令

```bash
mvn -pl synapse-audit-platform -am test
```

## 6. 相关文档

- [模块边界与服务设计](../docs/02-模块边界与服务设计.md)
