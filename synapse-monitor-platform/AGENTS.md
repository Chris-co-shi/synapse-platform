# Synapse Monitor 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Monitor 承载平台监控运行时能力；Framework Observability 仅提供通用技术约定。

## 2. 子模块边界

- api 保存稳定监控调用契约。
- client 提供调用适配，只依赖 api。
- server 承载启动入口和监控实现并允许依赖 api。

## 3. 允许依赖

client/server -> api；server 可使用当前 Framework `synapse-observability` 等正式模块。

## 4. 禁止事项

禁止把平台监控后台写入 Framework；禁止跨层反向依赖、跨服务数据库访问和其他模块 server 依赖。

## 5. 验证命令

```bash
mvn -pl synapse-monitor-platform -am test
```

## 6. 相关文档

- [模块边界与服务设计](../docs/02-模块边界与服务设计.md)
