# Synapse Integration 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Integration 提供平台外部系统集成边界；当前不预设具体厂商协议或业务适配模型。

## 2. 子模块边界

- api 保存稳定集成契约。
- client 提供调用适配，只依赖 api。
- server 承载启动入口和集成实现并允许依赖 api。

## 3. 允许依赖

client/server -> api；server 可按批准方案使用官方协议组件和当前 Framework 模块。

## 4. 禁止事项

禁止在 api 暴露厂商实现细节；禁止已删除/更名 artifact、跨层反向依赖及其他模块 server 依赖。

## 5. 验证命令

```bash
mvn -pl synapse-integration-platform -am test
```

## 6. 相关文档

- [模块边界与服务设计](../docs/02-模块边界与服务设计.md)
